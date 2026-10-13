package com.complaint.servlet;

import com.complaint.dao.UserDAO;
import com.complaint.model.User;
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

/**
 * Authentication Servlet for handling User Login and Current Session checks.
 * Endpoint: /api/login
 * Compatible with Jakarta EE 10 / Tomcat 10.1.
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final transient UserDAO userDAO = new UserDAO();

    /**
     * GET /api/login
     * Returns the current authenticated session user (if any).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            out.print("{"
                    + "\"authenticated\": true,"
                    + "\"id\": " + user.getId() + ","
                    + "\"name\": \"" + escapeJson(user.getFullName()) + "\","
                    + "\"email\": \"" + escapeJson(user.getEmail()) + "\","
                    + "\"role\": \"" + escapeJson(user.getRole()) + "\","
                    + "\"department\": \"" + escapeJson(user.getDepartment()) + "\""
                    + "}");
        } else {
            out.print("{\"authenticated\": false}");
        }
        out.flush();
    }

    /**
     * POST /api/login
     * Authenticates credentials, creates a secure HttpSession, and returns user details.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // Fallback: If sent as JSON body payload
        if (email == null || password == null) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();
            email = extractJsonField(body, "email");
            password = extractJsonField(body, "password");
        }

        // 1. Validate non-empty inputs
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Email and password are required.\"}");
            out.flush();
            return;
        }

        email = email.trim().toLowerCase();

        try {
            // 2. Fetch user from Supabase database
            User user = userDAO.findByEmail(email);

            if (user == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\": false, \"message\": \"Invalid credentials. No account found with this email.\"}");
                out.flush();
                return;
            }

            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"success\": false, \"message\": \"Account is inactive. Please contact administration.\"}");
                out.flush();
                return;
            }

            // 3. Verify hashed password
            boolean isPasswordValid = PasswordUtil.verifyPassword(password, user.getPasswordHash());

            if (!isPasswordValid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\": false, \"message\": \"Invalid password. Please check your credentials.\"}");
                out.flush();
                return;
            }

            // 4. Create server-side session (Tomcat HttpSession)
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getFullName());
            session.setAttribute("userRole", user.getRole());

            // 5. Determine destination URL
            String redirectUrl = "ADMIN".equalsIgnoreCase(user.getRole())
                    ? "admin/dashboard.html"
                    : "student/dashboard.html";

            // 6. Return JSON response
            out.print("{"
                    + "\"success\": true,"
                    + "\"message\": \"Login successful.\","
                    + "\"redirectUrl\": \"" + redirectUrl + "\","
                    + "\"id\": " + user.getId() + ","
                    + "\"name\": \"" + escapeJson(user.getFullName()) + "\","
                    + "\"email\": \"" + escapeJson(user.getEmail()) + "\","
                    + "\"role\": \"" + escapeJson(user.getRole()) + "\","
                    + "\"department\": \"" + escapeJson(user.getDepartment()) + "\""
                    + "}");

        } catch (SQLException e) {
            System.err.println("[LoginServlet] Database error during authentication: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Database connection error during login. Please try again.\"}");
        }

        out.flush();
    }

    private String extractJsonField(String json, String field) {
        if (json == null) return null;
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
