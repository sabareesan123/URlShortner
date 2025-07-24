

package cx.telosa.urlshortener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:h2:./urlshortener_db;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static DatabaseService instance;

    private DatabaseService() {}
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load H2 JDBC driver", e);
            throw new RuntimeException("Failed to load H2 JDBC driver", e);
        }
    }
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users table
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL)";
            stmt.execute(createUserTableSql);
            logger.info("Users table created or already exists.");

            // URLs table
            String createUrlTableSql = "CREATE TABLE IF NOT EXISTS urls (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "long_url VARCHAR(2048) NOT NULL, " +
                    "short_code VARCHAR(10) UNIQUE, " +
                    "user_id INT, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            stmt.execute(createUrlTableSql);
            logger.info("URLs table created or already exists.");

        } catch (SQLException e) {
            logger.error("Database initialization failed", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<User> findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password")));
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return Optional.empty();
    }

    public long saveUrl(String longUrl, String shortCode, Integer userId) {
        String sql = "INSERT INTO urls (long_url, short_code, user_id) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, longUrl);
            pstmt.setString(2, shortCode);
            if (userId != null) {
                pstmt.setInt(3, userId);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.executeUpdate();
            
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                throw new SQLException("Creating URL failed, no ID obtained.");
            }
        } catch (SQLException e) {
            logger.error("Error saving URL", e);
            return -1;
        }
    }

     public Optional<String> getLongUrl(String shortCode) {
        String sql = "SELECT long_url FROM urls WHERE short_code = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shortCode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("long_url"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving long URL for short code: {}", shortCode, e);
        }
        return Optional.empty();
    }

    public boolean isShortCodeTaken(String shortCode) {
        return getLongUrl(shortCode).isPresent();
    }
}
