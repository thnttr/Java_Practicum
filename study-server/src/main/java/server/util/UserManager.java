package server.util;

import server.models.UserSession;
import java.sql.*;
import java.util.*;

public class UserManager {
    
    public static Set<UserSession> loadUsers() {
        Set<UserSession> users = new HashSet<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT username, student_id, ip_address FROM users";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new UserSession(
                    rs.getString("username"),
                    rs.getString("student_id"),
                    rs.getString("ip_address")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return users;
    }
    
    private static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) DatabaseUtil.releaseConnection(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String username, String studentId, String ipAddress) {
        String sql = "INSERT INTO users (username, student_id, ip_address) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, studentId);
            pstmt.setString(3, ipAddress);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateUser(String studentId, String ipAddress) {
        String sql = "UPDATE users SET ip_address = ? WHERE student_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static UserSession getUserByStudentId(String studentId) {
        String sql = "SELECT username, student_id, ip_address FROM users";
        UserSession user = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if(rs.getFetchSize() == 1){
                user = new UserSession(
                    rs.getString("username"),
                    rs.getString("student_id"),
                    rs.getString("ip_address")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }
}