package Service;

import model.Book;
import model.BookItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookService {
    private AuthorService authorService;
    private BookItemService bookItemService; // Sẽ cần BookItemDAO

    public BookService() {
        this.authorService = new AuthorService();
        this.bookItemService = new BookItemService(); // Khởi tạo
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnector.getInstance().getConnection();
    }

    // Thêm sách mới, bao gồm tác giả và các bản sao sách (book items)
    public boolean addBook(Book book) {
        String sqlBook = "INSERT INTO Books (id, title, publisher, publishedDate, printType, quantity, borrowedQuantity, isAvailable, imagePath) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlBookAuthor = "INSERT INTO Book_Authors (book_id, author_id) VALUES (?, ?)";

        Connection conn = null;
        boolean success = false;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Thêm thông tin cơ bản của sách vào bảng Books
            try (PreparedStatement pstmtBook = conn.prepareStatement(sqlBook)) {
                pstmtBook.setString(1, book.getId());
                pstmtBook.setString(2, book.getTitle());
                pstmtBook.setString(3, book.getPublisher());
                pstmtBook.setDate(4, book.getPublishedDate() != null ? java.sql.Date.valueOf(book.getPublishedDate()) : null);
                pstmtBook.setString(5, book.getPrintType());
                pstmtBook.setInt(6, book.getQuantity());         // Số lượng tổng thể
                pstmtBook.setInt(7, 0); // Ban đầu borrowedQuantity là 0
                pstmtBook.setBoolean(8, book.getQuantity() > 0); // isAvailable nếu quantity > 0
                pstmtBook.setString(9, book.getImagePath());
                pstmtBook.executeUpdate();
            }

            // 2. Xử lý tác giả và bảng Book_Authors
            if (book.getAuthors() != null) {
                for (String authorName : book.getAuthors()) {
                    int authorId = authorService.findAuthorByName(authorName); // Giả sử authorDAO đã được khởi tạo
                    if (authorId == -1) { // Nếu tác giả chưa có, thêm mới
                        // Đảm bảo addAuthor được thiết kế để hoạt động trong transaction nếu cần,
                        // hoặc nó tự quản lý transaction của riêng nó (ít ưu tiên hơn khi đã có transaction cha)
                        authorId = authorService.addAuthor(authorName);
                    }
                    if (authorId != -1) {
                        try (PreparedStatement pstmtBookAuthor = conn.prepareStatement(sqlBookAuthor)) {
                            pstmtBookAuthor.setString(1, book.getId());
                            pstmtBookAuthor.setInt(2, authorId);
                            pstmtBookAuthor.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Could not find or add author: " + authorName + " for book " + book.getId());
                    }
                }
            }

            // 3. Thêm 'book.getQuantity()' số lượng BookItems cho cuốn sách này.
            // Bất kỳ BookItem nào có thể đã tồn tại trong book.getBookItems() trước đó khi gọi hàm này
            // sẽ bị bỏ qua. Chúng ta sẽ tạo mới dựa trên book.getQuantity() để đảm bảo tính nhất quán.
            if (book.getQuantity() > 0) {
                List<BookItem> createdItemsForThisBook = new ArrayList<>(); // Để cập nhật lại đối tượng Book nếu cần
                for (int i = 0; i < book.getQuantity(); i++) {
                    // Constructor của BookItem(String bookId) sẽ tự động:
                    // - Tạo bookItemId bằng UUID
                    // - Gán bookId được truyền vào
                    // - Đặt status là AVAILABLE
                    BookItem newItem = new BookItem(book.getId());

                    // bookItemDAO.addBookItem(item, connection) để nó tham gia vào transaction hiện tại
                    if (!bookItemService.addBookItem(newItem, conn)) {
                        throw new SQLException("Failed to add BookItem " + (i + 1) + " of " + book.getQuantity() +
                                " for Book ID: " + book.getId());
                    }
                    createdItemsForThisBook.add(newItem);
                }
                // Tùy chọn: Cập nhật danh sách bookItems của đối tượng Book trong bộ nhớ
                // để phản ánh các BookItem thực sự đã được tạo và lưu.
                // Điều này hữu ích nếu bạn tiếp tục sử dụng đối tượng 'book' sau khi gọi hàm này.
                // book.setBookItems(createdItemsForThisBook);
            }

            conn.commit(); // Kết thúc transaction thành công
            success = true;
            System.out.println("Book added successfully with ID: " + book.getId() + " and " + book.getQuantity() + " book items.");

        } catch (SQLException e) {
            System.err.println("Error adding book with transaction for Book ID " + book.getId() + ": " + e.getMessage());
            e.printStackTrace(); // In chi tiết lỗi để debug
            if (conn != null) {
                try {
                    System.err.println("Transaction is being rolled back for Book ID " + book.getId());
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction for Book ID " + book.getId() + ": " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Khôi phục auto-commit
                    // Không đóng connection ở đây nếu nó được quản lý bởi DatabaseConnector Singleton
                    // hoặc được truyền từ bên ngoài và sẽ được quản lý ở đó.
                } catch (SQLException ex) {
                    System.err.println("Error resetting auto-commit for Book ID " + book.getId() + ": " + ex.getMessage());
                }
            }
        }
        return success;
    }


    public Book getBookById(String bookId) {
        String sql = "SELECT * FROM Books WHERE id = ?";
        Book book = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    book = new Book();
                    book.setId(rs.getString("id"));
                    book.setTitle(rs.getString("title"));
                    book.setPublisher(rs.getString("publisher"));
                    Date sqlDate = rs.getDate("publishedDate");
                    book.setPublishedDate(sqlDate != null ? sqlDate.toString() : null);
                    book.setPrintType(rs.getString("printType"));
                    book.setQuantity(rs.getInt("quantity"));
                    book.setBorrowedQuantity(rs.getInt("borrowedQuantity"));
                    book.setAvailable(rs.getBoolean("isAvailable"));
                    book.setImagePath(rs.getString("imagePath"));

                    // Lấy danh sách tác giả
                    book.setAuthors(authorService.getAuthorNamesByBookId(bookId));
                    // Lấy danh sách BookItems
                    book.setBookItems(bookItemService.getBookItemsByBookId(bookId));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book by ID: " + e.getMessage());
        }
        return book;
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id FROM Books"; // Lấy id rồi gọi getBookById để tận dụng logic load authors/items
        // Hoặc JOIN phức tạp để lấy hết 1 lần (hiệu quả hơn cho nhiều sách)
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Book book = getBookById(rs.getString("id")); // Tái sử dụng getBookById
                if (book != null) {
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all books: " + e.getMessage());
        }
        return books;
    }

    public boolean updateBook(Book book) {
        // Logic update phức tạp:
        // 1. Update bảng Books
        // 2. Update bảng Book_Authors (xóa cũ, thêm mới nếu có thay đổi)
        // 3. Update BookItems (nếu số lượng thay đổi, hoặc thông tin BookItem thay đổi)
        // Cần transaction
        String sqlUpdateBook = "UPDATE Books SET title=?, publisher=?, publishedDate=?, printType=?, quantity=?, borrowedQuantity=?, isAvailable=?, imagePath=? WHERE id=?";
        String sqlDeleteBookAuthors = "DELETE FROM Book_Authors WHERE book_id = ?";
        String sqlInsertBookAuthor = "INSERT INTO Book_Authors (book_id, author_id) VALUES (?, ?)";

        Connection conn = null;
        boolean success = false;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Update bảng Books
            try (PreparedStatement pstmtUpdateBook = conn.prepareStatement(sqlUpdateBook)) {
                pstmtUpdateBook.setString(1, book.getTitle());
                pstmtUpdateBook.setString(2, book.getPublisher());
                pstmtUpdateBook.setDate(3, book.getPublishedDate() != null ? java.sql.Date.valueOf(book.getPublishedDate()) : null);
                pstmtUpdateBook.setString(4, book.getPrintType());
                pstmtUpdateBook.setInt(5, book.getQuantity());
                pstmtUpdateBook.setInt(6, book.getBorrowedQuantity());
                pstmtUpdateBook.setBoolean(7, book.isAvailable());
                pstmtUpdateBook.setString(8, book.getImagePath());
                pstmtUpdateBook.setString(9, book.getId());
                pstmtUpdateBook.executeUpdate();
            }

            // 2. Update Book_Authors
            // Xóa tất cả liên kết tác giả cũ của sách này
            try (PreparedStatement pstmtDeleteBA = conn.prepareStatement(sqlDeleteBookAuthors)) {
                pstmtDeleteBA.setString(1, book.getId());
                pstmtDeleteBA.executeUpdate();
            }
            // Thêm lại liên kết tác giả mới
            if (book.getAuthors() != null) {
                for (String authorName : book.getAuthors()) {
                    int authorId = authorService.findAuthorByName(authorName);
                    if (authorId == -1) {
                        authorId = authorService.addAuthor(authorName); // Dùng transaction
                    }
                    if (authorId != -1) {
                        try (PreparedStatement pstmtInsertBA = conn.prepareStatement(sqlInsertBookAuthor)) {
                            pstmtInsertBA.setString(1, book.getId());
                            pstmtInsertBA.setInt(2, authorId);
                            pstmtInsertBA.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Could not find or add author during update: " + authorName);
                    }
                }
            }
            // 3. Xử lý BookItems:
            // Nếu quantity thay đổi, cần thêm/xóa BookItems.
            // Đây là phần phức tạp, tùy thuộc vào logic nghiệp vụ bạn muốn.
            // Ví dụ đơn giản: Nếu quantity mới < quantity cũ, có thể cần đánh dấu một số BookItem là "không dùng" hoặc xóa (nếu chưa mượn).
            // Nếu quantity mới > quantity cũ, tạo thêm BookItem.
            // Hiện tại, chúng ta chưa có logic chi tiết cho việc này trong update.
            // Bạn có thể cần một phương thức riêng để đồng bộ BookItems.

            conn.commit();
            success = true;
        } catch (SQLException e) {
            System.err.println("Error updating book with transaction: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* log */ }
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { /* log */ }
        }
        return success;
    }

    public boolean deleteBook(String bookId) {
        // ON DELETE CASCADE trên Book_Authors và BookItems sẽ xử lý các bảng con.
        // Tuy nhiên, ON DELETE RESTRICT trên BookItems trong BorrowingRecords sẽ ngăn xóa nếu sách đang được mượn.
        String sql = "DELETE FROM Books WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
            return false;
        }
    }

    // Tăng số lượng sách đang mượn
    public boolean incrementBorrowedQuantity(String bookId, Connection connPassed) throws SQLException {
        String sql = "UPDATE Books SET borrowedQuantity = borrowedQuantity + 1 WHERE id = ?";
        PreparedStatement pstmt = null;
        try {
            // Nếu connPassed khác null, nghĩa là đang trong transaction, dùng nó
            Connection conn = (connPassed != null) ? connPassed : getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } finally {
            if (pstmt != null) pstmt.close();
            // Không đóng connPassed ở đây
        }
    }

    // Giảm số lượng sách đang mượn
    public boolean decrementBorrowedQuantity(String bookId, Connection connPassed) throws SQLException {
        String sql = "UPDATE Books SET borrowedQuantity = borrowedQuantity - 1 WHERE id = ? AND borrowedQuantity > 0";
        PreparedStatement pstmt = null;
        try {
            Connection conn = (connPassed != null) ? connPassed : getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
}