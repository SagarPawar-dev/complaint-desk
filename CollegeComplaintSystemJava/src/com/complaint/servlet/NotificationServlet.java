package com.complaint.servlet;

import com.complaint.dao.NotificationDAO;
import com.complaint.model.Notification;
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
 * Jakarta Servlet for Notifications API.
 * Endpoints:
 *   GET  /api/notifications              - List notifications for authenticated user
 *   GET  /api/notifications/unread-count - Get unread count for bell badge
 *   POST /api/notifications/read         - Mark single notification as read
 *   POST /api/notifications/read-all     - Mark all notifications as read for current user
 */
public class NotificationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final transient NotificationDAO notificationDAO = new NotificationDAO();

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

        String pathInfo = request.getPathInfo();
        String action = request.getParameter("action");

        try {
            // Unread count check (for top bell badge)
            if ("unread-count".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/unread-count"))) {
                int count = notificationDAO.getUnreadCount(user.getId(), user.getRole());
                out.print("{\"count\":" + count + "}");
                out.flush();
                return;
            }

            // List notifications
            List<Notification> list;
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                list = notificationDAO.getNotificationsForAdmin();
            } else {
                list = notificationDAO.getNotificationsForStudent(user.getId());
            }

            out.print(JsonUtil.notificationListToJson(list));

        } catch (SQLException e) {
            System.err.println("[NotificationServlet] Database error in doGet: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + JsonUtil.escape(e.getMessage()) + "\"}");
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
        String action = request.getParameter("action");
        String contentType = request.getContentType();
        String body = "";
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            body = readBody(request);
        } else if (request.getParameterMap().isEmpty()) {
            body = readBody(request);
        }

        try {
            // Case 1: Mark all as read
            if ("read-all".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/read-all"))) {
                int count = notificationDAO.markAllAsRead(user.getId(), user.getRole());
                out.print("{\"success\":true,\"updated\":" + count + "}");
                out.flush();
                return;
            }

            // Case 2: Mark single notification as read
            String idStr = getField(request, body, "id", "notificationId");
            if (idStr != null && !idStr.trim().isEmpty()) {
                // If ID is formatted like NOTIF-1, strip prefix
                String raw = idStr.trim();
                if (raw.toUpperCase().startsWith("NOTIF-")) {
                    raw = raw.substring(6).trim();
                }

                try {
                    long notificationId = Long.parseLong(raw);
                    boolean marked = notificationDAO.markAsRead(notificationId, user.getId(), user.getRole());
                    out.print("{\"success\":" + marked + "}");
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Invalid notification ID format.\"}");
                }
                out.flush();
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"No valid notification action specified.\"}");

        } catch (SQLException e) {
            System.err.println("[NotificationServlet] Database error in doPost: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + JsonUtil.escape(e.getMessage()) + "\"}");
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
        out.print("{\"success\":false,\"message\":\"Not authenticated\"}");
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
}
