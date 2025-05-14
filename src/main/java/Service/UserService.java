package Service;

import Enumerations.AccountStatus;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection conn;

    public UserService() {
        // Lấy connection một lần khi DAO được tạo, hoặc mỗi lần gọi phương thức
        // Cách tốt hơn là truyền Connection vào constructor nếu quản lý bên ngoài
        // Hoặc dùng DatabaseConnector.getInstance().getConnection() trong mỗi phương thức
    }

    private Connection getConnection() throws SQLException {
        // Đảm bảo bạn có instance của DatabaseConnector và gọi getConnection()
        // Ví dụ: return DatabaseConnector.getInstance().getConnection();
        // Tạm thời để null để tránh lỗi biên dịch nếu chưa có DatabaseConnector
        conn = DatabaseConnector.getInstance().getConnection();
        if (conn == null || conn.isClosed()) {
            // Xử lý nếu không có kết nối hoặc kết nối đã đóng
            System.err.println("UserService: Database connection is not available or closed.");
            throw new SQLException("Database connection is not available.");
        }
        return conn;
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO Users (id, name, username, password, email, phone, status, isAdmin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getUsername());
            pstmt.setString(4, user.getPassword()); // Nhớ hash mật khẩu trước khi lưu
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());
            pstmt.setString(7, user.getStatus().name());
            pstmt.setBoolean(8, user.isAdmin());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            // e.printStackTrace(); // Để debug
            return false;
        }
    }

    public User getUserById(String userId) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        User user = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getString("id"));
                    user.setName(rs.getString("name"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password")); // Mật khẩu đã hash từ DB
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setStatus(AccountStatus.valueOf(rs.getString("status")));
                    user.setAdmin(rs.getBoolean("isAdmin"));
                    // borrowedBooks sẽ cần được load riêng nếu cần
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by ID: " + e.getMessage());
        }
        return user;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        User user = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User();
                    user.setId(rs.getString("id"));
                    user.setName(rs.getString("name"));
                    // ... (tương tự như getUserById)
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setStatus(AccountStatus.valueOf(rs.getString("status")));
                    user.setAdmin(rs.getBoolean("isAdmin"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by username: " + e.getMessage());
        }
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getString("id"));
                user.setName(rs.getString("name"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setStatus(AccountStatus.valueOf(rs.getString("status")));
                user.setAdmin(rs.getBoolean("isAdmin"));
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE Users SET name = ?, username = ?, password = ?, email = ?, phone = ?, status = ?, isAdmin = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword()); // Nếu mật khẩu thay đổi, phải hash lại
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getStatus().name());
            pstmt.setBoolean(7, user.isAdmin());
            pstmt.setString(8, user.getId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        // Cẩn thận: DB có thể có ràng buộc khóa ngoại (ví dụ: trong BorrowingRecords)
        // ON DELETE RESTRICT sẽ ngăn việc xóa nếu user có bản ghi mượn.
        String sql = "DELETE FROM Users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            // Nếu lỗi do khóa ngoại, e.getMessage() sẽ cho biết.
            return false;
        }
    }
    // Các phương thức khác: kiểm tra mật khẩu, thay đổi trạng thái, v.v.
}