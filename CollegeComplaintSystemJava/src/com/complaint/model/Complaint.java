package com.complaint.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a student complaint ticket.
 * Maps directly to the 'complaints' table.
 */
public class Complaint {
    private String id;          // e.g. "CMP-00001"
    private String title;
    private String category;
    private String priority;    // "Low", "Medium", "High", "Urgent"
    private String location;
    private String description;
    private String status;      // "PENDING", "SUBMITTED", "IN PROGRESS", "RESOLVED", "CLOSED"
    private int studentId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Joined helper fields for easy UI rendering
    private String studentName;
    private String studentEmail;
    private String studentDept;
    private List<TimelineItem> timeline = new ArrayList<>();

    public Complaint() {
    }

    public Complaint(String id, String title, String category, String priority, String location, String description, String status, int studentId, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.location = location;
        this.description = description;
        this.status = status;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentDept() {
        return studentDept;
    }

    public void setStudentDept(String studentDept) {
        this.studentDept = studentDept;
    }

    public List<TimelineItem> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineItem> timeline) {
        this.timeline = timeline;
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }
}
