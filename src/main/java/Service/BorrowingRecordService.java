package Service;

import Enumerations.BookItemStatus;
import model.BorrowingRecord;
import model.BookItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowingRecordService {
    private BookItemService bookItemService;
    private UserService userService;
    private BookService bookService; // Để cập nhật borrowedQuantity của Book

    public BorrowingRecordService() {
        this.bookItemService = new BookItemService();
        this.userService = new UserService();
        this.bookService = new BookService();
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConnector.getInstance().getConnection();
    }

    public boolean addBorrowingRecord(BorrowingRecord record) {
        String sql = "INSERT INTO BorrowingRecords (borrowingRecordId, bookItemId, userId, borrowDate, dueDate, returnDate) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        boolean success = false;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Thêm bản ghi mượn
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, record.getBorrowingRecordId());
                pstmt.setString(2, record.getBookItemId());
                pstmt.setString(3, record.getUserId());
                pstmt.setDate(4, java.sql.Date.valueOf(record.getBorrowDate()));
                pstmt.setDate(5, java.sql.Date.valueOf(record.getDueDate()));
                if (record.getReturnDate() != null) {
                    pstmt.setDate(6, java.sql.Date.valueOf(record.getReturnDate()));
                } else {
                    pstmt.setNull(6, Types.DATE);
                }
                pstmt.executeUpdate();
            }

            // 2. Cập nhật trạng thái BookItem thành BORROWED
            BookItem borrowedItem = bookItemService.getBookItemById(record.getBookItemId()); // Lấy bookId từ item
            if (borrowedItem == null) throw new SQLException("Book item not found for borrowing.");

            if (!bookItemService.updateBookItemStatus(record.getBookItemId(), BookItemStatus.BORROWED, conn)) {
                throw new SQLException("Failed to update book item status to BORROWED.");
            }

            // 3. Cập nhật (tăng) borrowedQuantity của Book
            if (!bookService.incrementBorrowedQuantity(borrowedItem.getBookId(), conn)) {
                throw new SQLException("Failed to increment borrowed quantity for book.");
            }


            conn.commit();
            success = true;

        } catch (SQLException e) {
            System.err.println("Error adding borrowing record: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* log */ }
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { /* log */ }
        }
        return success;
    }

    public BorrowingRecord getBorrowingRecordById(String recordId) {
        String sql = "SELECT * FROM BorrowingRecords WHERE borrowingRecordId = ?";
        BorrowingRecord record = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    record = new BorrowingRecord();
                    record.setBorrowingRecordId(rs.getString("borrowingRecordId"));
                    record.setBookItemId(rs.getString("bookItemId"));
                    record.setUserId(rs.getString("userId"));
                    record.setBorrowDate(rs.getDate("borrowDate").toLocalDate());
                    record.setDueDate(rs.getDate("dueDate").toLocalDate());
                    Date returnSqlDate = rs.getDate("returnDate");
                    if (returnSqlDate != null) {
                        record.setReturnDate(returnSqlDate.toLocalDate());
                    }
                    // Load đối tượng BookItem và User
                    record.setBookItem(bookItemService.getBookItemById(record.getBookItemId()));
                    record.setUser(userService.getUserById(record.getUserId()));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrowing record by ID: " + e.getMessage());
        }
        return record;
    }

    public boolean updateReturnDateAndStatus(String recordId, LocalDate returnDate) {
        String sqlUpdateRecord = "UPDATE BorrowingRecords SET returnDate = ? WHERE borrowingRecordId = ?";
        Connection conn = null;
        boolean success = false;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // Lấy thông tin record để biết bookItemId
            BorrowingRecord record = getBorrowingRecordById(recordId); // Cần lấy record này trong transaction
            if (record == null || record.getBookItem() == null) {
                throw new SQLException("Borrowing record or associated book item not found.");
            }

            // 1. Cập nhật ngày trả trong BorrowingRecords
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateRecord)) {
                pstmt.setDate(1, java.sql.Date.valueOf(returnDate));
                pstmt.setString(2, recordId);
                pstmt.executeUpdate();
            }

            // 2. Cập nhật trạng thái BookItem thành AVAILABLE
            if (!bookItemService.updateBookItemStatus(record.getBookItemId(), BookItemStatus.AVAILABLE, conn)) {
                throw new SQLException("Failed to update book item status to AVAILABLE.");
            }

            // 3. Cập nhật (giảm) borrowedQuantity của Book
            if(!bookService.decrementBorrowedQuantity(record.getBookItem().getBookId(), conn)){
                throw new SQLException("Failed to decrement borrowed quantity for book.");
            }

            conn.commit();
            success = true;

        } catch (SQLException e) {
            System.err.println("Error updating return date and status: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* log */ }
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { /* log */ }
        }
        return success;
    }

    public List<BorrowingRecord> getActiveBorrowingRecordsByUserId(String userId) {
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT borrowingRecordId FROM BorrowingRecords WHERE userId = ? AND returnDate IS NULL";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BorrowingRecord record = getBorrowingRecordById(rs.getString("borrowingRecordId"));
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active borrowing records by user ID: " + e.getMessage());
        }
        return records;
    }
    // Các phương thức khác: getAllBorrowingRecords, getOverdueRecords, ...
}