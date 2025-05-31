package server.util;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtil {
    private static final String DB_NAME = "study_server";
    private static String username;
    private static String password;
    private static final int MAX_POOL_SIZE = 10;
    private static final List<Connection> connectionPool = new ArrayList<>();
    private static boolean initialized = false;
    
    public static void initialize() {
        try {
            // 加载数据库配置
            loadDatabaseProperties();
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 创建数据库（如果不存在）
            createDatabaseIfNotExists();
            
            // 初始化连接池
            initializeConnectionPool();
            
            initialized = true;
            System.out.println("Database initialized successfully");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void createDatabaseIfNotExists() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306?useSSL=false",
                username, password)) {
            
            // 检查数据库是否存在
            ResultSet resultSet = conn.getMetaData().getCatalogs();
            boolean dbExists = false;
            while (resultSet.next()) {
                if (DB_NAME.equals(resultSet.getString(1))) {
                    dbExists = true;
                    break;
                }
            }
            resultSet.close();

            if (!dbExists) {
                // 创建数据库
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE DATABASE " + DB_NAME;
                    stmt.executeUpdate(sql);
                    System.out.println("Database created successfully: " + DB_NAME);
                }
            }

            // 创建用户表
            try (Connection dbConn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/" + DB_NAME,
                    username, password);
                 Statement stmt = dbConn.createStatement()) {
                
                String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(50) NOT NULL,
                        student_id VARCHAR(20) NOT NULL UNIQUE,
                        ip_address VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;
                stmt.execute(createTableSQL);
                System.out.println("Users table created/verified successfully");
            }
        }
    }

    private static void loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("src/main/resources/database.properties")) {
            props.load(input);
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
            
            if (username == null || password == null) {
                throw new IllegalStateException("Missing database credentials");
            }
        }
    }

    private static void initializeConnectionPool() throws SQLException {
        for (int i = 0; i < MAX_POOL_SIZE; i++) {
            connectionPool.add(createNewConnection());
        }
    }

    private static Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/" + DB_NAME,
            username,
            password
        );
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database not initialized");
        }

        // 获取可用连接
        if (!connectionPool.isEmpty()) {
            Connection conn = connectionPool.remove(0);
            if (conn != null && !conn.isClosed() && conn.isValid(1)) {
                return conn;
            }
        }
        
        // Create new connection if none available
        return createNewConnection();
    }

    public static synchronized void releaseConnection(Connection conn) {
        if (conn != null && connectionPool.size() < MAX_POOL_SIZE) {
            connectionPool.add(conn);
        } else {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void shutdown() {
        System.out.println("Shutting down database connections...");
        for (Connection conn : connectionPool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        connectionPool.clear();
        System.out.println("Database connections closed");
    }
}