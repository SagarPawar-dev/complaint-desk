package com.complaint.dao;

import com.complaint.model.User;
import com.complaint.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for 'users' table.
 * Handles database queries for student and admin accounts.
 */
public class UserDAO {

    /**
     * Finds a user by email address (case-insensitive).
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, department, role, status, created_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a user by their primary key ID.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, department, role, status, created_at " +
                     "FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns all registered users (for Admin User Management).
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, full_name, email, password_hash, department, role, status, created_at " +
                     "FROM users ORDER BY role DESC, full_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * Creates a new user record.
     */
    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, department, role, status) " +
                     "VALUES (?, LOWER(?), ?, ?, ?, ?) RETURNING id, created_at";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail().trim());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getDepartment());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus() != null ? user.getStatus() : "ACTIVE");

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * Helper method to map a ResultSet row to a User object.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("department"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getTimestamp("created_at")
        );
    }
}
