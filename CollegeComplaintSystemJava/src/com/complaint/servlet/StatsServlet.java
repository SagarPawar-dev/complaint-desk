package com.complaint.servlet;

import com.complaint.dao.ComplaintDAO;
import com.complaint.model.User;
import com.complaint.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map;

/**
 * Jakarta Servlet for Dashboard Analytics & Stat Cards.
 * Endpoint: GET /api/stats
 * Derives real statistics directly from Supabase PostgreSQL.
 * Role-aware:
 *   - Students get counts for their own complaints
 *   - Admins get institution-wide complaint counts
 */
public class StatsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final transient ComplaintDAO complaintDAO = new ComplaintDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"Not authenticated\"}");
            out.flush();
            return;
        }

        try {
            Integer studentId = "STUDENT".equalsIgnoreCase(user.getRole()) ? user.getId() : null;
            Map<String, Integer> stats = complaintDAO.getDashboardStats(studentId);

            out.print("{"
                    + "\"total\":" + stats.getOrDefault("total", 0) + ","
                    + "\"pending\":" + stats.getOrDefault("pending", 0) + ","
                    + "\"progress\":" + stats.getOrDefault("progress", 0) + ","
                    + "\"resolved\":" + stats.getOrDefault("resolved", 0)
                    + "}");

        } catch (SQLException e) {
            System.err.println("[StatsServlet] Database error in doGet: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
        out.flush();
    }
}
