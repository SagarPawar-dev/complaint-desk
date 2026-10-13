package com.complaint.util;

import com.complaint.model.Complaint;
import com.complaint.model.Notification;
import com.complaint.model.TimelineItem;
import com.complaint.model.User;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight JSON utility for formatting and parsing data objects
 * across Jakarta Servlets without requiring external libraries.
 */
public class JsonUtil {

    private static final String DATE_FORMAT = "dd/MM/yyyy hh:mm a";

    /**
     * Escapes special characters for JSON string literals.
     */
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Formats a Timestamp to display format matching existing UI ("dd/MM/yyyy hh:mm a").
     */
    public static String formatDate(Timestamp ts) {
        if (ts == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return sdf.format(ts);
    }

    /**
     * Serializes a single Complaint to JSON.
     */
    public static String toJson(Complaint c) {
        if (c == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(c.getId())).append("\",");
        sb.append("\"title\":\"").append(escape(c.getTitle())).append("\",");
        sb.append("\"category\":\"").append(escape(c.getCategory())).append("\",");
        sb.append("\"priority\":\"").append(escape(c.getPriority())).append("\",");
        sb.append("\"location\":\"").append(escape(c.getLocation())).append("\",");
        sb.append("\"description\":\"").append(escape(c.getDescription())).append("\",");
        sb.append("\"status\":\"").append(escape(c.getStatus())).append("\",");
        sb.append("\"studentId\":").append(c.getStudentId()).append(",");
        sb.append("\"submittedBy\":\"").append(escape(c.getStudentEmail() != null ? c.getStudentEmail() : "")).append("\",");
        sb.append("\"studentName\":\"").append(escape(c.getStudentName() != null ? c.getStudentName() : "")).append("\",");
        sb.append("\"studentDept\":\"").append(escape(c.getStudentDept() != null ? c.getStudentDept() : "")).append("\",");
        sb.append("\"submittedAt\":\"").append(escape(formatDate(c.getCreatedAt()))).append("\",");
        sb.append("\"createdAt\":\"").append(escape(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")).append("\",");
        sb.append("\"updatedAt\":\"").append(escape(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "")).append("\",");
        sb.append("\"timeline\":").append(timelineListToJson(c.getTimeline()));
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a list of Complaints to a JSON array.
     */
    public static String complaintListToJson(List<Complaint> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializes a TimelineItem to JSON.
     */
    public static String toJson(TimelineItem t) {
        if (t == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(t.getId()).append(",");
        sb.append("\"complaintId\":\"").append(escape(t.getComplaintId())).append("\",");
        sb.append("\"action\":\"").append(escape(t.getAction())).append("\",");
        sb.append("\"by\":\"").append(escape(t.getPerformedBy())).append("\",");
        sb.append("\"performedBy\":\"").append(escape(t.getPerformedBy())).append("\",");
        sb.append("\"userId\":").append(t.getUserId() != null ? t.getUserId() : "null").append(",");
        sb.append("\"note\":\"").append(escape(t.getNote())).append("\",");
        sb.append("\"status\":\"").append(escape(t.getStatus())).append("\",");
        sb.append("\"time\":\"").append(escape(formatDate(t.getCreatedAt()))).append("\",");
        sb.append("\"createdAt\":\"").append(escape(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a list of TimelineItems to JSON.
     */
    public static String timelineListToJson(List<TimelineItem> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializes a single Notification to JSON.
     */
    public static String toJson(Notification n) {
        if (n == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(n.getId()).append(",");
        sb.append("\"complaintId\":\"").append(escape(n.getComplaintId())).append("\",");
        sb.append("\"userId\":").append(n.getUserId()).append(",");
        sb.append("\"recipientRole\":\"").append(escape(n.getRecipientRole())).append("\",");
        sb.append("\"title\":\"").append(escape(n.getTitle())).append("\",");
        sb.append("\"message\":\"").append(escape(n.getMessage())).append("\",");
        sb.append("\"type\":\"").append(escape(n.getType())).append("\",");
        sb.append("\"read\":").append(n.isRead()).append(",");
        sb.append("\"isRead\":").append(n.isRead()).append(",");
        sb.append("\"time\":\"").append(escape(formatDate(n.getCreatedAt()))).append("\",");
        sb.append("\"createdAt\":\"").append(escape(n.getCreatedAt() != null ? n.getCreatedAt().toString() : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a list of Notifications to JSON.
     */
    public static String notificationListToJson(List<Notification> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Serializes a User to JSON (excluding passwordHash for security).
     */
    public static String toJson(User u) {
        if (u == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"USR-").append(String.format("%03d", u.getId())).append("\",");
        sb.append("\"rawId\":").append(u.getId()).append(",");
        sb.append("\"name\":\"").append(escape(u.getFullName())).append("\",");
        sb.append("\"fullName\":\"").append(escape(u.getFullName())).append("\",");
        sb.append("\"email\":\"").append(escape(u.getEmail())).append("\",");
        sb.append("\"dept\":\"").append(escape(u.getDepartment())).append("\",");
        sb.append("\"department\":\"").append(escape(u.getDepartment())).append("\",");
        sb.append("\"role\":\"").append(escape(u.getRole())).append("\",");
        sb.append("\"status\":\"").append(escape(u.getStatus())).append("\",");
        sb.append("\"createdAt\":\"").append(escape(formatDate(u.getCreatedAt()))).append("\"");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes a list of Users to JSON.
     */
    public static String userListToJson(List<User> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extracts a string field from a simple JSON object string.
     */
    public static String extractJsonField(String json, String field) {
        if (json == null) return null;
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        // Also check if field is numeric/boolean or non-quoted
        String nonQuotedPattern = "\"" + field + "\"\\s*:\\s*([^,}\\]\\s]+)";
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(nonQuotedPattern);
        java.util.regex.Matcher m2 = p2.matcher(json);
        if (m2.find()) {
            return m2.group(1).trim();
        }
        return null;
    }
}
