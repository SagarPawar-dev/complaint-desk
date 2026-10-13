package com.complaint.servlet;

import com.complaint.dao.UserDAO;
import com.complaint.model.User;
import com.complaint.util.JsonUtil;
import com.complaint.util.PasswordUtil;

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
 * Jakarta Servlet for User Management & Profile Settings API.
 * Endpoints:
 *   GET  /api/users                 - List all institutional users (Admin only)
 *   GET  /api/users/me              - View currently authenticated profile
 *   POST /api/users/status          - Toggle user status ACTIVE/INACTIVE (Admin only)
 *   POST /api/users/profile         - Update profile information (name, department)
 *   POST /api/users/password        - Update user password
 */
public class UserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final transient UserDAO userDAO = new UserDAO();

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
            // Case 1: Current user profile /api/users/me
            if ("me".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/me"))) {
                User fresh = userDAO.findById(user.getId());
                out.print(JsonUtil.toJson(fresh != null ? fresh : user));
                out.flush();
                return;
            }

            // Case 2: List all users (Admin only)
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\":false,\"message\":\"Access denied. User management requires Admin role.\"}");
                out.flush();
                return;
            }

            List<User> list = userDAO.getAllUsers();
            out.print(JsonUtil.userListToJson(list));

        } catch (SQLException e) {
            System.err.println("[UserServlet] Database error in doGet: " + e.getMessage());
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
            // Sub-action 1: Toggle user status (Admin only)
            if ("status".equalsIgnoreCase(action) || "toggle-status".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/status"))) {
                if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"success\":false,\"message\":\"Admin privileges required.\"}");
                    out.flush();
                    return;
                }

                String idStr = getField(request, body, "id", "userId");
                String newStatus = getField(request, body, "status", "newStatus");

                if (idStr == null || newStatus == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Missing id or status parameter.\"}");
                    out.flush();
                    return;
                }

                // If ID is formatted like USR-001, strip prefix
                String raw = idStr.trim();
                if (raw.toUpperCase().startsWith("USR-")) {
                    raw = raw.substring(4).trim();
                }

                int targetUserId = Integer.parseInt(raw);
                boolean updated = userDAO.updateStatus(targetUserId, newStatus.trim().toUpperCase());
                out.print("{\"success\":" + updated + ",\"message\":\"User status updated to " + JsonUtil.escape(newStatus) + "\"}");
                out.flush();
                return;
            }

            // Sub-action 2: Change password
            if ("password".equalsIgnoreCase(action) || "change-password".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/password"))) {
                String oldPass = getField(request, body, "oldPassword", "currentPassword");
                String newPass = getField(request, body, "newPassword");

                if (oldPass == null || newPass == null || newPass.trim().length() < 4) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"New password must be at least 4 characters long.\"}");
                    out.flush();
                    return;
                }

                User fresh = userDAO.findById(user.getId());
                if (fresh == null || !PasswordUtil.verifyPassword(oldPass, fresh.getPasswordHash())) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"success\":false,\"message\":\"Current password is incorrect.\"}");
                    out.flush();
                    return;
                }

                String newHash = PasswordUtil.hashPassword(newPass.trim());
                boolean updated = userDAO.updatePassword(user.getId(), newHash);
                fresh.setPasswordHash(newHash);
                request.getSession().setAttribute("user", fresh);

                out.print("{\"success\":" + updated + ",\"message\":\"Password updated successfully.\"}");
                out.flush();
                return;
            }

            // Sub-action 3: Update profile details
            if ("profile".equalsIgnoreCase(action) || "update-profile".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.endsWith("/profile"))) {
                String fullName = getField(request, body, "fullName", "name");
                String dept = getField(request, body, "department", "dept");

                if (fullName == null || fullName.trim().isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\":false,\"message\":\"Name cannot be empty.\"}");
                    out.flush();
                    return;
                }

                if (dept == null) dept = user.getDepartment();

                boolean updated = userDAO.updateProfile(user.getId(), fullName.trim(), dept.trim());
                if (updated) {
                    User fresh = userDAO.findById(user.getId());
                    request.getSession().setAttribute("user", fresh);
                    request.getSession().setAttribute("userName", fresh.getFullName());
                    out.print("{\"success\":true,\"message\":\"Profile updated successfully.\",\"user\":" + JsonUtil.toJson(fresh) + "}");
                } else {
                    out.print("{\"success\":false,\"message\":\"Could not update profile.\"}");
                }
                out.flush();
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"No recognized action specified.\"}");

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Invalid user ID format.\"}");
        } catch (SQLException e) {
            System.err.println("[UserServlet] Database error in doPost: " + e.getMessage());
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
