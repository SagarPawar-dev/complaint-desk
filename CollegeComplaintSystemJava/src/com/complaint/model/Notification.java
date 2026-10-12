package com.complaint.model;

import java.sql.Timestamp;

/**
 * Model class representing a user or admin alert notification.
 * Maps directly to the 'notifications' table.
 */
public class Notification {
    private long id;
    private String complaintId;
    private int userId;
    private String recipientRole; // "STUDENT" or "ADMIN"
    private String title;
    private String message;
    private String type;          // "NEW_SUBMISSION", "PENDING", "IN PROGRESS", "RESOLVED", "RESPONSE"
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {
    }

    public Notification(long id, String complaintId, int userId, String recipientRole, String title, String message, String type, boolean isRead, Timestamp createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.userId = userId;
        this.recipientRole = recipientRole;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", complaintId='" + complaintId + '\'' +
                ", recipientRole='" + recipientRole + '\'' +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
