package com.complaint.dao;

import com.complaint.model.Complaint;
import com.complaint.model.Notification;
import com.complaint.model.TimelineItem;
import com.complaint.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for 'complaints' and 'complaint_timeline' tables.
 * Handles complaint creation, status updates, timeline tracking, and multi-table transactions.
 */
public class ComplaintDAO {

    /**
     * Finds a single complaint by its ID (e.g. "CMP-00001"),
     * including joined student profile info and chronological timeline history.
     */
    public Complaint findById(String complaintId) throws SQLException {
        String sql = "SELECT c.id, c.title, c.category, c.priority, c.location, c.description, c.status, " +
                     "       c.student_id, c.created_at, c.updated_at, " +
                     "       u.full_name AS student_name, u.email AS student_email, u.department AS student_dept " +
                     "FROM complaints c " +
                     "JOIN users u ON c.student_id = u.id " +
                     "WHERE c.id = ?";

        Complaint complaint = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, complaintId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    complaint = mapRowToComplaint(rs);
                }
            }

            if (complaint != null) {
                complaint.setTimeline(getTimelineForComplaint(conn, complaintId));
            }
        }
        return complaint;
    }

    /**
     * Retrieves all complaints for a specific student.
     */
    public List<Complaint> getComplaintsByStudent(int studentId) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.id, c.title, c.category, c.priority, c.location, c.description, c.status, " +
                     "       c.student_id, c.created_at, c.updated_at, " +
                     "       u.full_name AS student_name, u.email AS student_email, u.department AS student_dept " +
                     "FROM complaints c " +
                     "JOIN users u ON c.student_id = u.id " +
                     "WHERE c.student_id = ? " +
                     "ORDER BY c.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToComplaint(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all complaints across the institution (for Admin workspace).
     */
    public List<Complaint> getAllComplaints() throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT c.id, c.title, c.category, c.priority, c.location, c.description, c.status, " +
                     "       c.student_id, c.created_at, c.updated_at, " +
                     "       u.full_name AS student_name, u.email AS student_email, u.department AS student_dept " +
                     "FROM complaints c " +
                     "JOIN users u ON c.student_id = u.id " +
                     "ORDER BY c.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToComplaint(rs));
            }
        }
        return list;
    }

    /**
     * Atomically creates a complaint, adds the initial timeline entry,
     * and generates an Admin notification within a single database transaction.
     */
    public Complaint createComplaint(Complaint complaint, String studentName) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Generate next formatted ID (e.g. CMP-00003)
            String nextId = generateNextComplaintId(conn);
            complaint.setId(nextId);
            complaint.setStatus("PENDING");

            // 2. Insert into complaints table
            String insertComplaintSql =
                    "INSERT INTO complaints (id, title, category, priority, location, description, status, student_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                    "RETURNING created_at, updated_at";

            try (PreparedStatement stmt = conn.prepareStatement(insertComplaintSql)) {
                stmt.setString(1, nextId);
                stmt.setString(2, complaint.getTitle());
                stmt.setString(3, complaint.getCategory());
                stmt.setString(4, complaint.getPriority() != null ? complaint.getPriority() : "Medium");
                stmt.setString(5, complaint.getLocation() != null ? complaint.getLocation() : "Campus Premises");
                stmt.setString(6, complaint.getDescription());
                stmt.setInt(7, complaint.getStudentId());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        complaint.setCreatedAt(rs.getTimestamp("created_at"));
                        complaint.setUpdatedAt(rs.getTimestamp("updated_at"));
                    }
                }
            }

            // 3. Insert initial timeline entry
            String insertTimelineSql =
                    "INSERT INTO complaint_timeline (complaint_id, action, performed_by, user_id, note, status, created_at) " +
                    "VALUES (?, 'Complaint Submitted', ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt = conn.prepareStatement(insertTimelineSql)) {
                stmt.setString(1, nextId);
                stmt.setString(2, (studentName != null ? studentName : "Student") + " (Student)");
                stmt.setInt(3, complaint.getStudentId());
                stmt.setString(4, "Initial grievance submitted with " + complaint.getPriority() + " priority.");
                stmt.executeUpdate();
            }

            // 4. Notify Administrator
            int adminUserId = findAdminUserId(conn);
            if (adminUserId > 0) {
                Notification notif = new Notification();
                notif.setComplaintId(nextId);
                notif.setUserId(adminUserId);
                notif.setRecipientRole("ADMIN");
                notif.setTitle("New Complaint Submitted (" + nextId + ")");
                notif.setMessage((studentName != null ? studentName : "A student") + " submitted '" + complaint.getTitle() + "' in " + complaint.getLocation() + ".");
                notif.setType("NEW_SUBMISSION");

                NotificationDAO notifDAO = new NotificationDAO();
                notifDAO.createNotification(conn, notif);
            }

            conn.commit(); // Commit Transaction
            return complaint;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Atomically updates a complaint's status, logs the change to timeline,
     * and creates a Student notification within a single database transaction.
     */
    public boolean updateStatus(String complaintId, String newStatus, String adminRemark, String adminName, int adminUserId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Fetch current complaint details for timeline and student notification
            String fetchSql = "SELECT title, status, student_id FROM complaints WHERE id = ?";
            String title = "";
            String oldStatus = "";
            int studentId = 0;

            try (PreparedStatement stmt = conn.prepareStatement(fetchSql)) {
                stmt.setString(1, complaintId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        title = rs.getString("title");
                        oldStatus = rs.getString("status");
                        studentId = rs.getInt("student_id");
                    } else {
                        return false;
                    }
                }
            }

            // 2. Update status in complaints table
            String updateSql = "UPDATE complaints SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, newStatus);
                stmt.setString(2, complaintId);
                stmt.executeUpdate();
            }

            // 3. Log event into complaint_timeline
            String actionTitle = "Status Changed from " + oldStatus + " to " + newStatus;
            if ("RESOLVED".equalsIgnoreCase(newStatus) || "CLOSED".equalsIgnoreCase(newStatus)) {
                actionTitle = "Complaint Resolved & Closed";
            }

            String insertTimelineSql =
                    "INSERT INTO complaint_timeline (complaint_id, action, performed_by, user_id, note, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt = conn.prepareStatement(insertTimelineSql)) {
                stmt.setString(1, complaintId);
                stmt.setString(2, actionTitle);
                stmt.setString(3, adminName != null ? adminName : "System Administrator");
                stmt.setInt(4, adminUserId);
                stmt.setString(5, adminRemark != null && !adminRemark.trim().isEmpty() ? adminRemark.trim() : "Status updated by administrator.");
                stmt.setString(6, newStatus);
                stmt.executeUpdate();
            }

            // 4. Notify the student
            Notification notif = new Notification();
            notif.setComplaintId(complaintId);
            notif.setUserId(studentId);
            notif.setRecipientRole("STUDENT");
            notif.setTitle("Complaint Status Updated (" + complaintId + ")");
            notif.setMessage("Your complaint '" + title + "' status is now " + newStatus + "." +
                    (adminRemark != null && !adminRemark.trim().isEmpty() ? " Remark: \"" + adminRemark.trim() + "\"" : ""));
            notif.setType(newStatus);

            NotificationDAO notifDAO = new NotificationDAO();
            notifDAO.createNotification(conn, notif);

            conn.commit(); // Commit Transaction
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Atomically posts an Admin response to the complaint timeline
     * and sends a notification to the student within a single database transaction.
     */
    public boolean addAdminResponse(String complaintId, String responseText, String adminName, int adminUserId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Fetch current status & student ID
            String fetchSql = "SELECT title, status, student_id FROM complaints WHERE id = ?";
            String title = "";
            String currentStatus = "";
            int studentId = 0;

            try (PreparedStatement stmt = conn.prepareStatement(fetchSql)) {
                stmt.setString(1, complaintId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        title = rs.getString("title");
                        currentStatus = rs.getString("status");
                        studentId = rs.getInt("student_id");
                    } else {
                        return false;
                    }
                }
            }

            // 2. Insert into complaint_timeline
            String insertTimelineSql =
                    "INSERT INTO complaint_timeline (complaint_id, action, performed_by, user_id, note, status, created_at) " +
                    "VALUES (?, 'Admin Response', ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt = conn.prepareStatement(insertTimelineSql)) {
                stmt.setString(1, complaintId);
                stmt.setString(2, adminName != null ? adminName : "System Administrator");
                stmt.setInt(3, adminUserId);
                stmt.setString(4, responseText.trim());
                stmt.setString(5, currentStatus);
                stmt.executeUpdate();
            }

            // 3. Touch updated_at on complaint
            String touchSql = "UPDATE complaints SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(touchSql)) {
                stmt.setString(1, complaintId);
                stmt.executeUpdate();
            }

            // 4. Notify student
            Notification notif = new Notification();
            notif.setComplaintId(complaintId);
            notif.setUserId(studentId);
            notif.setRecipientRole("STUDENT");
            notif.setTitle("New Response on '" + title + "' (" + complaintId + ")");
            notif.setMessage("Admin added a response: \"" + (responseText.length() > 80 ? responseText.substring(0, 80) + "..." : responseText) + "\"");
            notif.setType("RESPONSE");

            NotificationDAO notifDAO = new NotificationDAO();
            notifDAO.createNotification(conn, notif);

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Calculates statistics (total, pending, progress, resolved) for dashboard stat cards.
     * If studentId is null, returns system-wide metrics for Admin.
     */
    public Map<String, Integer> getDashboardStats(Integer studentId) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("pending", 0);
        stats.put("progress", 0);
        stats.put("resolved", 0);

        String sql = "SELECT status, COUNT(*) AS count FROM complaints " +
                     (studentId != null ? "WHERE student_id = ? " : "") +
                     "GROUP BY status";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (studentId != null) {
                stmt.setInt(1, studentId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String status = rs.getString("status");
                    int count = rs.getInt("count");
                    total += count;

                    if ("PENDING".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status)) {
                        stats.put("pending", stats.get("pending") + count);
                    } else if ("IN PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
                        stats.put("progress", stats.get("progress") + count);
                    } else if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                        stats.put("resolved", stats.get("resolved") + count);
                    }
                }
                stats.put("total", total);
            }
        }
        return stats;
    }

    /**
     * Retrieves chronological timeline items for a complaint.
     */
    private List<TimelineItem> getTimelineForComplaint(Connection conn, String complaintId) throws SQLException {
        List<TimelineItem> list = new ArrayList<>();
        String sql = "SELECT id, complaint_id, action, performed_by, user_id, note, status, created_at " +
                     "FROM complaint_timeline " +
                     "WHERE complaint_id = ? " +
                     "ORDER BY created_at ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, complaintId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new TimelineItem(
                            rs.getLong("id"),
                            rs.getString("complaint_id"),
                            rs.getString("action"),
                            rs.getString("performed_by"),
                            rs.getObject("user_id") != null ? rs.getInt("user_id") : null,
                            rs.getString("note"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Generates the next sequential ID in CMP-0000X format.
     */
    private String generateNextComplaintId(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM complaints";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int count = rs.next() ? rs.getInt(1) : 0;
            int nextNumber = count + 1;
            return String.format("CMP-%05d", nextNumber);
        }
    }

    /**
     * Finds the primary Administrator user ID for notification routing.
     */
    private int findAdminUserId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id ASC LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 0;
    }

    private Complaint mapRowToComplaint(ResultSet rs) throws SQLException {
        Complaint c = new Complaint(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("location"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getInt("student_id"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
        c.setStudentName(rs.getString("student_name"));
        c.setStudentEmail(rs.getString("student_email"));
        c.setStudentDept(rs.getString("student_dept"));
        return c;
    }
}
