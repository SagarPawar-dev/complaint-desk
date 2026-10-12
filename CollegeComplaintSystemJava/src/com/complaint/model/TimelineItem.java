package com.complaint.model;

import java.sql.Timestamp;

/**
 * Model class representing an event in a complaint's activity timeline.
 * Maps directly to the 'complaint_timeline' table.
 */
public class TimelineItem {
    private long id;
    private String complaintId;
    private String action;
    private String performedBy;
    private Integer userId;
    private String note;
    private String status;
    private Timestamp createdAt;

    public TimelineItem() {
    }

    public TimelineItem(long id, String complaintId, String action, String performedBy, Integer userId, String note, String status, Timestamp createdAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.action = action;
        this.performedBy = performedBy;
        this.userId = userId;
        this.note = note;
        this.status = status;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TimelineItem{" +
                "id=" + id +
                ", complaintId='" + complaintId + '\'' +
                ", action='" + action + '\'' +
                ", performedBy='" + performedBy + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
