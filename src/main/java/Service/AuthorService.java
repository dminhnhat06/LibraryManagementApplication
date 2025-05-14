package Service;

import Service.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuthorService {

    private Connection getConnection() throws SQLException {
        return DatabaseConnector.getInstance().getConnection();
    }

    // Trả về author_id, hoặc -1 nếu không tìm thấy
    public int findAuthorByName(String name) {
        String sql = "SELECT author_id FROM Authors WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("author_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding author by name: " + name + " - " + e.getMessage());
        }
        return -1;
    }

    // Thêm tác giả mới và trả về ID được tạo, hoặc -1 nếu lỗi
    public int addAuthor(String name) {
        String sql = "INSERT INTO Authors (name) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            // Có thể do UNIQUE constraint nếu tên tác giả đã tồn tại
            if (e.getSQLState().equals("23000")) { // Mã lỗi cho vi phạm ràng buộc UNIQUE/PRIMARY KEY
                System.err.println("Author with name '" + name + "' already exists or other integrity constraint violation.");
                return findAuthorByName(name); // Trả về ID của tác giả đã tồn tại
            }
            System.err.println("Error adding author: " + name + " - " + e.getMessage());
        }
        return -1;
    }

    public List<String> getAuthorNamesByBookId(String bookId) {
        List<String> authorNames = new ArrayList<>();
        String sql = "SELECT a.name FROM Authors a " +
                "JOIN Book_Authors ba ON a.author_id = ba.author_id " +
                "WHERE ba.book_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    authorNames.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching author names for book ID " + bookId + ": " + e.getMessage());
        }
        return authorNames;
    }
}