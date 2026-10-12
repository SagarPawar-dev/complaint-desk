package com.complaint.dao;

import com.complaint.model.Notification;
import com.complaint.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for 'notifications' table.
 * Handles notification retrieval, unread counts, and read/unread status updates.
 */
public class NotificationDAO {

    /**
     * Gets notifications for a specific student by user ID.
     */
    public List<Notification> getNotificationsForStudent(int studentId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, complaint_id, user_id, recipient_role, title, message, type, is_read, created_at " +
                     "FROM notifications " +
                     "WHERE user_id = ? AND recipient_role = 'STUDENT' " +
                     "ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
        }
        return list;
    }

    /**
     * Gets all admin notifications.
     */
    public List<Notification> getNotificationsForAdmin() throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, complaint_id, user_id, recipient_role, title, message, type, is_read, created_at " +
                     "FROM notifications " +
                     "WHERE recipient_role = 'ADMIN' " +
                     "ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToNotification(rs));
            }
        }
        return list;
    }

    /**
     * Returns the count of unread notifications for a specific user (student or admin).
     */
    public int getUnreadCount(int userId, String role) throws SQLException {
        String sql;
        if ("ADMIN".equalsIgnoreCase(role)) {
            sql = "SELECT COUNT(*) FROM notifications WHERE recipient_role = 'ADMIN' AND is_read = FALSE";
        } else {
            sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND recipient_role = 'STUDENT' AND is_read = FALSE";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!"ADMIN".equalsIgnoreCase(role)) {
                stmt.setInt(1, userId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Marks a single notification as read (idempotent operation).
     */
    public boolean markAsRead(long notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notificationId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Marks all notifications as read for a given user/role.
     */
    public int markAllAsRead(int userId, String role) throws SQLException {
        String sql;
        if ("ADMIN".equalsIgnoreCase(role)) {
            sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_role = 'ADMIN' AND is_read = FALSE";
        } else {
            sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND recipient_role = 'STUDENT' AND is_read = FALSE";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (!"ADMIN".equalsIgnoreCase(role)) {
                stmt.setInt(1, userId);
            }

            return stmt.executeUpdate();
        }
    }

    /**
     * Inserts a notification using an existing transaction Connection.
     */
    public void createNotification(Connection conn, Notification notif) throws SQLException {
        String sql = "INSERT INTO notifications (complaint_id, user_id, recipient_role, title, message, type, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, notif.getComplaintId());
            stmt.setInt(2, notif.getUserId());
            stmt.setString(3, notif.getRecipientRole());
            stmt.setString(4, notif.getTitle());
            stmt.setString(5, notif.getMessage());
            stmt.setString(6, notif.getType());
            stmt.executeUpdate();
        }
    }

    /**
     * Standalone insertion of a notification.
     */
    public void createNotification(Notification notif) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            createNotification(conn, notif);
        }
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getLong("id"),
                rs.getString("complaint_id"),
                rs.getInt("user_id"),
                rs.getString("recipient_role"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("type"),
                rs.getBoolean("is_read"),
                rs.getTimestamp("created_at")
        );
    }
}
