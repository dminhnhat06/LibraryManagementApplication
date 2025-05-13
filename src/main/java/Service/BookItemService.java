package Service;

import Enumerations.BookItemStatus;
import Service.DatabaseConnector;
import model.BookItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookItemService {

    private Connection getConnection() throws SQLException {
        return DatabaseConnector.getInstance().getConnection();
    }

    // Thêm BookItem, có thể dùng trong transaction của BookDAO
    public boolean addBookItem(BookItem item, Connection conn) throws SQLException {
        String sql = "INSERT INTO BookItems (bookItemId, bookId, status) VALUES (?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            // Nếu conn khác null, nghĩa là đang trong transaction, dùng nó
            Connection currentConn = (conn != null) ? conn : getConnection();
            boolean autoCommitOriginal = currentConn.getAutoCommit();
            if (conn == null) currentConn.setAutoCommit(true); // Nếu không trong transaction, tự commit

            pstmt = currentConn.prepareStatement(sql);
            pstmt.setString(1, item.getBookItemId());
            pstmt.setString(2, item.getBookId());
            pstmt.setString(3, item.getStatus().name());
            int affectedRows = pstmt.executeUpdate();

            if (conn == null) { // Nếu không trong transaction, khôi phục lại autoCommit
                // currentConn.close(); // Chỉ đóng nếu DAO này tự mở
            }
            return affectedRows > 0;
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
            // Không đóng connection nếu nó được truyền vào (conn != null)
        }
    }
    public boolean addBookItem(BookItem item) { // Overload để dùng độc lập
        try {
            return addBookItem(item, null);
        } catch (SQLException e) {
            System.err.println("Error adding book item independently: " + e.getMessage());
            return false;
        }
    }


    public BookItem getBookItemById(String bookItemId) {
        String sql = "SELECT * FROM BookItems WHERE bookItemId = ?";
        BookItem item = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookItemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    item = new BookItem();
                    item.setBookItemId(rs.getString("bookItemId"));
                    item.setBookId(rs.getString("bookId"));
                    item.setStatus(BookItemStatus.valueOf(rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book item by ID: " + e.getMessage());
        }
        return item;
    }

    public List<BookItem> getBookItemsByBookId(String bookId) {
        List<BookItem> items = new ArrayList<>();
        String sql = "SELECT * FROM BookItems WHERE bookId = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BookItem item = new BookItem();
                    item.setBookItemId(rs.getString("bookItemId"));
                    item.setBookId(rs.getString("bookId"));
                    item.setStatus(BookItemStatus.valueOf(rs.getString("status")));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book items by book ID: " + e.getMessage());
        }
        return items;
    }

    public boolean updateBookItemStatus(String bookItemId, BookItemStatus status, Connection connPassed) throws SQLException {
        String sql = "UPDATE BookItems SET status = ? WHERE bookItemId = ?";
        PreparedStatement pstmt = null;
        try {
            Connection conn = (connPassed != null) ? connPassed : getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, status.name());
            pstmt.setString(2, bookItemId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    public boolean updateBookItemStatus(String bookItemId, BookItemStatus status) { // Overload
        try {
            return updateBookItemStatus(bookItemId, status, null);
        } catch(SQLException e) {
            System.err.println("Error updating book item status independently: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteBookItem(String bookItemId) {
        // Sẽ bị chặn bởi ON DELETE RESTRICT nếu BookItem này có trong BorrowingRecords
        String sql = "DELETE FROM BookItems WHERE bookItemId = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookItemId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting book item: " + e.getMessage());
            return false;
        }
    }
}