package com.complaint.servlet;

import com.complaint.dao.ComplaintDAO;
import com.complaint.model.Complaint;
import com.complaint.model.User;
import com.complaint.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Jakarta Servlet for Complaints API.
 * Endpoints:
 *   GET  /api/complaints              - List complaints (filtered by student for students, all for admins)
 *   GET  /api/complaints?id=CMP-XXXXX - View single complaint details + timeline (ownership enforced)
 *   POST /api/complaints              - Submit a new grievance (student session identity enforced)
 *   POST /api/complaints/status       - Update status with admin remark (Admin only)
 *   POST /api/complaints/response     - Post admin response to timeline (Admin only)
 */
public class ComplaintServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final transient ComplaintDAO complaintDAO = new ComplaintDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        User user = getAuthenticatedUser(request);
        if (user == null) {
            sendUnauthorized(response, out);
            return;
        }

        String complaintId = request.getParameter("id");
        if (complaintId == null) {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                complaintId = pathInfo.substring(1);
            }
        }

        try {
            // Case 1: Fetch single complaint by ID
            if (complaintId != null && !complaintId.trim().isEmpty()) {
                complaintId = complaintId.trim();
                Complaint complaint = complaintDAO.findById(complaintId);

                if (complaint == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Complaint not found: " + JsonUtil.escape(complaintId) + "\"}");
                    out.flush();
                    return;
                }

                // Strict Backend Ownership Check for Students
                if ("STUDENT".equalsIgnoreCase(user.getRole()) && complaint.getStudentId() != user.getId()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"success\":false,\"message\":\"Access denied. You can only view your own complaints.\"}");
                    out.flush();
                    return;
                }

                out.print(JsonUtil.toJson(complaint));
                out.flush();
                return;
            }

            // Case 2: List complaints
            List<Complaint> list;
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                // Admin can optionally filter by studentEmail query parameter if viewing user details
                String studentEmail = request.getParameter("studentEmail");
                if (studentEmail != null && !studentEmail.trim().isEmpty()) {
                    list = complaintDAO.getAllComplaints();
                    list.removeIf(c -> c.getStudentEmail() == null || !c.getStudentEmail().equalsIgnoreCase(studentEmail.trim()));
                } else {
                    list = complaintDAO.getAllComplaints();
                }
            } else {
                // Student isolation: strictly fetch complaints filed by this authenticated student ID
                list = complaintDAO.getComplaintsByStudent(user.getId());
            }

            out.print(JsonUtil.complaintListToJson(list));

        } catch (SQLException e) {
            System.err.println("[ComplaintServlet] Database error in doGet: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database query error: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        User user = getAuthenticatedUser(request);
        if (user == null) {
            sendUnauthorized(response, out);
            return;
        }

        String pathInfo = request.getPathInfo();
        String contentType = request.getContentType();
        String body = "";
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            body = readBody(request);
        } else if (request.getParameterMap().isEmpty()) {
            body = readBody(request);
        }

        try {
            // Sub-route 0: /api/complaints/delete or action=delete (Admin Only)
            String action = request.getParameter("action");
            if ((pathInfo != null && pathInfo.endsWith("/delete")) || "delete".equalsIgnoreCase(action)) {
                handleDelete(request, response, out, user, body);
                return;
            }

            // Sub-route 1: /api/complaints/status (Admin Only)
            if (pathInfo != null && pathInfo.endsWith("/status")) {
                if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                    sendForbidden(response, out, "Only administrators can update complaint statuses.");
                    return;
                }

                String id = getField(request, body, "id", "complaintId");
                String newStatus = getField(request, body, "status", "newStatus");
                String remark = getField(request, body, "remark", "adminRemark");

                if (id == null || id.trim().isEmpty() || newStatus == null || newStatus.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Missing required fields: id and status.\"}");
                    out.flush();
                    return;
                }

                String validatedStatus = normalizeStatus(newStatus);
                if (validatedStatus == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Invalid status value. Allowed: PENDING, SUBMITTED, IN PROGRESS, RESOLVED, CLOSED.\"}");
                    out.flush();
                    return;
                }

                boolean updated = complaintDAO.updateStatus(id.trim(), validatedStatus, remark, user.getFullName(), user.getId());
                if (updated) {
                    Complaint updatedComplaint = complaintDAO.findById(id.trim());
                    out.print("{\"success\":true,\"message\":\"Complaint status updated to " + JsonUtil.escape(validatedStatus) + "\",\"complaint\":" + JsonUtil.toJson(updatedComplaint) + "}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Complaint not found: " + JsonUtil.escape(id) + "\"}");
                }
                out.flush();
                return;
            }

            // Sub-route 2: /api/complaints/response (Admin Only)
            if (pathInfo != null && pathInfo.endsWith("/response")) {
                if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                    sendForbidden(response, out, "Only administrators can add remarks to complaints.");
                    return;
                }

                String id = getField(request, body, "id", "complaintId");
                String responseText = getField(request, body, "response", "responseText", "note");

                if (id == null || id.trim().isEmpty() || responseText == null || responseText.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Missing required fields: id and responseText.\"}");
                    out.flush();
                    return;
                }

                boolean added = complaintDAO.addAdminResponse(id.trim(), responseText.trim(), user.getFullName(), user.getId());
                if (added) {
                    Complaint updatedComplaint = complaintDAO.findById(id.trim());
                    out.print("{\"success\":true,\"message\":\"Response posted successfully.\",\"complaint\":" + JsonUtil.toJson(updatedComplaint) + "}");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"success\":false,\"message\":\"Complaint not found: " + JsonUtil.escape(id) + "\"}");
                }
                out.flush();
                return;
            }

            // Sub-route 3: Root POST - Submit a new grievance (Students)
            String title = getField(request, body, "title");
            String category = getField(request, body, "category");
            String priority = getField(request, body, "priority");
            String location = getField(request, body, "location");
            String description = getField(request, body, "description");

            if (title == null || title.trim().isEmpty() ||
                category == null || category.trim().isEmpty() ||
                location == null || location.trim().isEmpty() ||
                description == null || description.trim().isEmpty()) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"message\":\"All required fields must be provided (title, category, location, description).\"}");
                out.flush();
                return;
            }

            priority = normalizePriority(priority);

            Complaint newComplaint = new Complaint();
            newComplaint.setTitle(title.trim());
            newComplaint.setCategory(category.trim());
            newComplaint.setPriority(priority);
            newComplaint.setLocation(location.trim());
            newComplaint.setDescription(description.trim());
            // Enforce student identity from the server-side session
            newComplaint.setStudentId(user.getId());
            newComplaint.setStudentName(user.getFullName());
            newComplaint.setStudentEmail(user.getEmail());
            newComplaint.setStudentDept(user.getDepartment());

            Complaint created = complaintDAO.createComplaint(newComplaint, user.getFullName());

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(JsonUtil.toJson(created));

        } catch (SQLException e) {
            System.err.println("[ComplaintServlet] Database error in doPost: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Failed to process complaint: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        User user = getAuthenticatedUser(request);
        if (user == null) {
            sendUnauthorized(response, out);
            return;
        }

        String body = "";
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            body = readBody(request);
        } else if (request.getParameterMap().isEmpty()) {
            body = readBody(request);
        }

        handleDelete(request, response, out, user, body);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, PrintWriter out, User user, String body) {
        // Strict Server-Side Authorization: Only administrators can delete complaints
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            sendForbidden(response, out, "Access denied. Only administrators can delete complaints.");
            return;
        }

        String complaintId = getField(request, body, "id", "complaintId");
        if (complaintId == null || complaintId.trim().isEmpty()) {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                String sub = pathInfo.substring(1);
                if (sub.endsWith("/delete")) {
                    sub = sub.substring(0, sub.length() - 7);
                }
                if (!sub.isEmpty()) complaintId = sub;
            }
        }

        if (complaintId == null || complaintId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Complaint ID is required for deletion.\"}");
            out.flush();
            return;
        }

        try {
            boolean deleted = complaintDAO.deleteComplaint(complaintId.trim());
            if (deleted) {
                out.print("{\"success\":true,\"message\":\"Complaint " + JsonUtil.escape(complaintId.trim()) + " deleted successfully.\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\":false,\"message\":\"Complaint not found: " + JsonUtil.escape(complaintId.trim()) + "\"}");
            }
        } catch (SQLException e) {
            System.err.println("[ComplaintServlet] Error deleting complaint: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error deleting complaint: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, PrintWriter out) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        out.print("{\"success\":false,\"message\":\"Not authenticated. Please log in.\"}");
        out.flush();
    }

    private void sendForbidden(HttpServletResponse response, PrintWriter out, String message) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        out.print("{\"success\":false,\"message\":\"" + JsonUtil.escape(message) + "\"}");
        out.flush();
    }

    private String readBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getField(HttpServletRequest request, String body, String... fieldNames) {
        for (String name : fieldNames) {
            String param = request.getParameter(name);
            if (param != null && !param.trim().isEmpty()) {
                return param.trim();
            }
        }
        if (body != null && !body.trim().isEmpty()) {
            for (String name : fieldNames) {
                String val = JsonUtil.extractJsonField(body, name);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase();
        if ("PENDING".equals(s) || "SUBMITTED".equals(s) || "RESOLVED".equals(s) || "CLOSED".equals(s)) {
            return s;
        }
        if ("IN PROGRESS".equals(s) || "IN_PROGRESS".equals(s)) {
            return "IN PROGRESS";
        }
        if ("REJECTED".equals(s)) {
            return "CLOSED";
        }
        return null;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) return "Medium";
        String p = priority.trim();
        if ("Low".equalsIgnoreCase(p)) return "Low";
        if ("Medium".equalsIgnoreCase(p)) return "Medium";
        if ("High".equalsIgnoreCase(p)) return "High";
        if ("Urgent".equalsIgnoreCase(p)) return "Urgent";
        return "Medium";
    }
}

