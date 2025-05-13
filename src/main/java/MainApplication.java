import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import Enumerations.AccountStatus;
import Enumerations.BookItemStatus;
import Service.DatabaseConnector;
import model.Book;
import model.User;
import model.BookItem;
import model.BorrowingRecord;
import Service.*;

public class MainApplication {

    public static void main(String[] args) {
        // 0. Khởi tạo kết nối (DatabaseConnector sẽ tự làm khi gọi getInstance())
        System.out.println("--- Initializing Database Connection ---");
        DatabaseConnector dbInstance = DatabaseConnector.getInstance();
        if (dbInstance.getConnection() == null) {
            System.err.println("Failed to connect to the database. Exiting.");
            return;
        }
        System.out.println("Database connection appears to be successful.\n");

        // Khởi tạo các Service
        UserService userService = new UserService();
        AuthorService authorService = new AuthorService(); // BookService có thể dùng nó
        BookService bookService = new BookService();
        BookItemService bookItemService = new BookItemService();
        BorrowingRecordService borrowingRecordService = new BorrowingRecordService();

        // --- 1. User Operations ---
        System.out.println("--- Testing User Operations ---");
        testUserOperations(userService);

        // --- 2. Book and Author Operations ---
        System.out.println("\n--- Testing Book and Author Operations ---");
        testBookOperations(bookService, authorService, bookItemService);

        // --- 3. Borrowing Operations ---
        System.out.println("\n--- Testing Borrowing Operations ---");
        testBorrowingOperations(borrowingRecordService, userService, bookService, bookItemService);

        // --- 4. (Optional) Additional checks or cleanup ---
        // Ví dụ: Lấy lại tất cả sách để xem borrowedQuantity
        System.out.println("\n--- Final Book Status Check ---");
        List<Book> allBooksFinal = bookService.getAllBooks();
        for (Book b : allBooksFinal) {
            System.out.println("Book: " + b.getTitle() + ", Total: " + b.getQuantity() + ", Borrowed: " + b.getBorrowedQuantity());
            for (BookItem bi : b.getBookItems()) {
                System.out.println("  Item ID: " + bi.getBookItemId() + ", Status: " + bi.getStatus());
            }
        }


        // Đóng kết nối khi ứng dụng kết thúc
        System.out.println("\n--- Closing Database Connection ---");
        dbInstance.closeConnection();
        System.out.println("Application finished.");
    }

    public static void testUserOperations(UserService userService) {
        // Tạo User Admin
        String adminId = "admin-" + UUID.randomUUID().toString().substring(0, 8);
        User adminUser = new User(adminId, "Admin User", "admin01", "adminpass", "admin@example.com", "0123456780", AccountStatus.ACTIVE, true);
        if (userService.addUser(adminUser)) {
            System.out.println("Admin user added successfully: " + adminUser.getUsername());
        } else {
            System.out.println("Failed to add admin user.");
        }

        // Tạo User thường
        String memberId = "member-" + UUID.randomUUID().toString().substring(0, 8);
        User regularUser = new User(memberId, "Regular Member", "member01", "memberpass", "member@example.com", "0987654321", AccountStatus.ACTIVE, false);
        if (userService.addUser(regularUser)) {
            System.out.println("Regular user added successfully: " + regularUser.getUsername());
        } else {
            System.out.println("Failed to add regular user.");
        }

        // Lấy user theo ID
        User fetchedAdmin = userService.getUserById(adminId);
        if (fetchedAdmin != null) {
            System.out.println("Fetched admin by ID: " + fetchedAdmin.getName());
        }

        // Lấy user theo username
        User fetchedMember = userService.getUserByUsername("member01");
        if (fetchedMember != null) {
            System.out.println("Fetched member by Username: " + fetchedMember.getName());
        }

        // Cập nhật user
        if (fetchedMember != null) {
            fetchedMember.setPhone("1112223333");
            if (userService.updateUser(fetchedMember)) {
                System.out.println("Updated member's phone: " + fetchedMember.getPhone());
            }
        }

        // Lấy tất cả users
        System.out.println("All users in DB:");
        List<User> allUsers = userService.getAllUsers();
        for (User u : allUsers) {
            System.out.println("- " + u.getName() + " (" + u.getUsername() + "), IsAdmin: " + u.isAdmin());
        }

        // (Tùy chọn) Xóa user - Cẩn thận nếu user có borrowing records (sẽ bị RESTRICT)
        // if (userService.deleteUser(memberId)) {
        //     System.out.println("Deleted user: " + memberId);
        // }
    }

    public static void testBookOperations(BookService bookService, AuthorService authorService, BookItemService bookItemService) {
        // Tạo sách 1
        String bookId1 = "ISBN-TEST-001";
        ArrayList authors = new ArrayList();
        authors.add("<NAME>");
        authors.add("adbbd");
        Book book1 = new Book(bookId1, "Lập Trình Với Java", authors,
                "NXB Tri Thức Việt", "2023-01-10", "Bìa mềm", 5, 1, "/img/java.jpg");
        // book1.setQuantity(5); // Constructor đã set initialQuantity
        // Các BookItem sẽ được tạo tự động trong BookService.addBook nếu Book.getBookItems() rỗng và quantity > 0

        if (bookService.addBook(book1)) {
            System.out.println("Book 1 added: " + book1.getTitle());
        } else {
            System.out.println("Failed to add Book 1.");
        }

        // Tạo sách 2
        String bookId2 = "ISBN-TEST-002";
        ArrayList authors2 = new ArrayList();
        authors2.add("<NAME>");
        authors2.add("adbbd");
        Book book2 = new Book(bookId2, "Cấu Trúc Dữ Liệu Và Giải Thuật",
                authors2, // "Trần Thị Thuật Toán" đã có
                "NXB Khoa Học", "2022-05-20", "Bìa cứng", 3, 0, "/img/dsa.jpg");

        if (bookService.addBook(book2)) {
            System.out.println("Book 2 added: " + book2.getTitle());
        } else {
            System.out.println("Failed to add Book 2.");
        }

        // Lấy sách theo ID và kiểm tra thông tin
        Book fetchedBook1 = bookService.getBookById(bookId1);
        if (fetchedBook1 != null) {
            System.out.println("\nFetched Book 1: " + fetchedBook1.getTitle());
            System.out.println("  Authors: " + String.join(", ", fetchedBook1.getAuthors()));
            System.out.println("  Publisher: " + fetchedBook1.getPublisher());
            System.out.println("  Total Quantity: " + fetchedBook1.getQuantity());
            System.out.println("  Book Items (" + fetchedBook1.getBookItems().size() + "):");
            for (BookItem item : fetchedBook1.getBookItems()) {
                System.out.println("    - Item ID: " + item.getBookItemId() + ", Status: " + item.getStatus());
            }
        }

        // Lấy tất cả sách
        System.out.println("\nAll books in library:");
        List<Book> allBooks = bookService.getAllBooks();
        for (Book b : allBooks) {
            System.out.println("- " + b.getTitle() + " (ID: " + b.getId() + "), Authors: " + String.join(", ", b.getAuthors()));
        }

        // Cập nhật sách (ví dụ: thêm tác giả mới, thay đổi NXB)
        if (fetchedBook1 != null) {
            List<String> updatedAuthors = new ArrayList<>(fetchedBook1.getAuthors());
            updatedAuthors.add("Phạm Văn Debug"); // Tác giả mới
            fetchedBook1.setAuthors(updatedAuthors);
            fetchedBook1.setPublisher("NXB Thế Giới Mới");
            fetchedBook1.setQuantity(7); // Tăng số lượng => Sẽ cần logic để tạo thêm BookItem nếu muốn.
            // BookService.updateBook hiện tại chưa xử lý tự động tạo/xóa BookItem khi quantity thay đổi.
            // Nó chỉ update trường quantity trong bảng Books.
            // Để đơn giản, ta sẽ không thay đổi quantity ở đây hoặc phải tự thêm/xóa BookItem.
            fetchedBook1.setQuantity(5); // Giữ nguyên quantity để tránh phức tạp việc quản lý BookItem trong test này

            if (bookService.updateBook(fetchedBook1)) {
                System.out.println("\nUpdated Book 1: " + fetchedBook1.getTitle());
                Book reFetchedBook1 = bookService.getBookById(bookId1); // Lấy lại để xem thay đổi
                System.out.println("  New Authors: " + String.join(", ", reFetchedBook1.getAuthors()));
                System.out.println("  New Publisher: " + reFetchedBook1.getPublisher());
            }
        }

        // (Tùy chọn) Xóa sách - Cẩn thận nếu sách có item đang được mượn (sẽ bị RESTRICT)
        // if (bookService.deleteBook(bookId2)) {
        //    System.out.println("Deleted book: " + bookId2);
        // }
    }

    public static void testBorrowingOperations(BorrowingRecordService borrowingRecordService, UserService userService, BookService bookService, BookItemService bookItemService) {
        // Lấy một user và một book để thực hiện mượn
        User testUser = userService.getUserByUsername("member01"); // Sử dụng user đã tạo
        Book testBookToBorrow = bookService.getBookById("ISBN-TEST-001"); // Sử dụng sách đã tạo

        if (testUser == null || testBookToBorrow == null) {
            System.err.println("Cannot proceed with borrowing test: User or Book not found.");
            return;
        }

        System.out.println("\nUser for borrowing: " + testUser.getName());
        System.out.println("Book to borrow: " + testBookToBorrow.getTitle());

        // Tìm một BookItem AVAILABLE của sách này
        BookItem itemToBorrow = null;
        for (BookItem item : testBookToBorrow.getBookItems()) {
            if (item.getStatus() == BookItemStatus.AVAILABLE) {
                itemToBorrow = item;
                break;
            }
        }

        if (itemToBorrow == null) {
            System.err.println("No available items for book: " + testBookToBorrow.getTitle());
            // Có thể thử mượn sách khác hoặc thêm item
            // testBookToBorrow = bookService.getBookById("ISBN-TEST-002");
            // if (testBookToBorrow != null) {
            //    for (BookItem item : testBookToBorrow.getBookItems()) {
            //        if (item.getStatus() == BookItemStatus.AVAILABLE) {
            //            itemToBorrow = item;
            //            break;
            //        }
            //    }
            // }
            // if (itemToBorrow == null) {
            //     System.err.println("Still no available items. Borrowing test skipped for now.");
            //     return;
            // }
            return;
        }

        System.out.println("Attempting to borrow BookItem ID: " + itemToBorrow.getBookItemId());

        // 1. Tạo bản ghi mượn
        BorrowingRecord newRecord = new BorrowingRecord(itemToBorrow, testUser);
        // newRecord.setBorrowingRecordId(...); // ID tự tạo trong constructor
        if (borrowingRecordService.addBorrowingRecord(newRecord)) {
            System.out.println("Book '" + testBookToBorrow.getTitle() + "' (Item: " + itemToBorrow.getBookItemId() + ") borrowed successfully by " + testUser.getName());
            System.out.println("  Borrowing Record ID: " + newRecord.getBorrowingRecordId());
            System.out.println("  Due Date: " + newRecord.getDueDate());

            // Kiểm tra lại trạng thái BookItem và Book.borrowedQuantity
            BookItem updatedItem = bookItemService.getBookItemById(itemToBorrow.getBookItemId());
            Book updatedBook = bookService.getBookById(testBookToBorrow.getId());
            System.out.println("  BookItem status after borrow: " + (updatedItem != null ? updatedItem.getStatus() : "Not Found"));
            System.out.println("  Book borrowed quantity after borrow: " + (updatedBook != null ? updatedBook.getBorrowedQuantity() : "Not Found"));
        } else {
            System.err.println("Failed to borrow book.");
            return; // Không thể tiếp tục nếu mượn thất bại
        }

        // 2. Lấy bản ghi mượn theo ID
        BorrowingRecord fetchedRecord = borrowingRecordService.getBorrowingRecordById(newRecord.getBorrowingRecordId());
        if (fetchedRecord != null) {
            System.out.println("\nFetched Borrowing Record ID: " + fetchedRecord.getBorrowingRecordId());
            System.out.println("  User: " + fetchedRecord.getUser().getName());
            System.out.println("  Book Title: " + bookService.getBookById(fetchedRecord.getBookItem().getBookId()).getTitle());
            System.out.println("  BookItem ID: " + fetchedRecord.getBookItem().getBookItemId());
        }

        // 3. Lấy các bản ghi đang mượn của user
        System.out.println("\nActive borrowings for user " + testUser.getName() + ":");
        List<BorrowingRecord> activeRecords = borrowingRecordService.getActiveBorrowingRecordsByUserId(testUser.getId());
        if (activeRecords.isEmpty()) {
            System.out.println("  No active borrowings.");
        } else {
            for (BorrowingRecord br : activeRecords) {
                Book borrowedBookDetails = bookService.getBookById(br.getBookItem().getBookId());
                System.out.println("  - Record ID: " + br.getBorrowingRecordId() +
                        ", Book: " + (borrowedBookDetails != null ? borrowedBookDetails.getTitle() : "N/A") +
                        ", Due: " + br.getDueDate());
            }
        }


        // 4. Trả sách
        System.out.println("\nAttempting to return book for Record ID: " + newRecord.getBorrowingRecordId());
        if (borrowingRecordService.updateReturnDateAndStatus(newRecord.getBorrowingRecordId(), LocalDate.now())) {
            System.out.println("Book returned successfully for Record ID: " + newRecord.getBorrowingRecordId());

            // Kiểm tra lại trạng thái BookItem và Book.borrowedQuantity
            BookItem returnedItem = bookItemService.getBookItemById(itemToBorrow.getBookItemId());
            Book bookAfterReturn = bookService.getBookById(testBookToBorrow.getId());
            System.out.println("  BookItem status after return: " + (returnedItem != null ? returnedItem.getStatus() : "Not Found"));
            System.out.println("  Book borrowed quantity after return: " + (bookAfterReturn != null ? bookAfterReturn.getBorrowedQuantity() : "Not Found"));

            BorrowingRecord finalRecord = borrowingRecordService.getBorrowingRecordById(newRecord.getBorrowingRecordId());
            if (finalRecord != null) {
                System.out.println("  Actual return date on record: " + finalRecord.getReturnDate());
            }

        } else {
            System.err.println("Failed to return book.");
        }
    }
}