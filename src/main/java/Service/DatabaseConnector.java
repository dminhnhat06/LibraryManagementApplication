package Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/librarydb";
    private static final String USER = "root";         // Sửa user CSDL
    private static final String PASS = "admin";     // Sửa mật khẩu CSDL

    private static DatabaseConnector instance;
    private Connection connection;

    private DatabaseConnector() {
        try {
            // 1. Đăng ký JDBC driver (Không bắt buộc với JDBC 4.0+ nhưng nên có)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Mở kết nối
            System.out.println("Connecting to database...");
            this.connection = DriverManager.getConnection(DB_URL, USER, PASS);
            if (this.connection != null) {
                System.out.println("Database connection established successfully!");
            } else {
                System.err.println("Failed to make connection!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
            // Cân nhắc throw một RuntimeException ở đây để ứng dụng dừng lại nếu không kết nối được
        } catch (SQLException e) {
            System.err.println("Connection Failed! Check output console");
            e.printStackTrace();
            // Cân nhắc throw một RuntimeException
        }
    }

    // Phương thức public static để lấy instance của lớp (Singleton)
    public static synchronized DatabaseConnector getInstance() {
        if (instance == null) {
            instance = new DatabaseConnector();
        } else {
            try {
                // Kiểm tra nếu kết nối đã đóng, thử kết nối lại
                if (instance.connection == null || instance.connection.isClosed()) {
                    System.out.println("Connection was closed. Reconnecting...");
                    instance = new DatabaseConnector(); // Thử kết nối lại
                }
            } catch (SQLException e) {
                System.err.println("Error checking connection status. Reconnecting...");
                instance = new DatabaseConnector(); // Thử kết nối lại
            }
        }
        return instance;
    }

    // Lấy đối tượng Connection
    public Connection getConnection() {
        // Có thể thêm kiểm tra null hoặc isClosed() ở đây nếu muốn an toàn hơn
        // và thử kết nối lại nếu cần, nhưng getInstance() đã làm điều đó rồi.
        if (this.connection == null) {
            System.err.println("Connection is null. Attempting to re-establish via getInstance().");
            // This should not happen if getInstance is used correctly, but as a safeguard:
            instance = new DatabaseConnector();
            return instance.connection;
        }
        return this.connection;
    }

    // Đóng kết nối (nên gọi khi ứng dụng kết thúc)
    public void closeConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection.");
            e.printStackTrace();
        }
    }

    // Optional: main method for quick testing
    public static void main(String[] args) {
        DatabaseConnector dbConnector = DatabaseConnector.getInstance();
        Connection conn = dbConnector.getConnection();
        if (conn != null) {
            System.out.println("Successfully got connection from getInstance().");
            // Thực hiện một truy vấn đơn giản để kiểm tra
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    System.out.println("Test query successful: " + rs.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnector.closeConnection();
        } else {
            System.out.println("Failed to get connection from getInstance().");
        }
    }
}