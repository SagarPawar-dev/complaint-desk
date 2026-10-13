package com.complaint.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Logout Servlet for terminating the user's server-side session.
 * Endpoint: /api/logout
 * Compatible with Jakarta EE 10 / Tomcat 10.1.
 */
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        String accept = request.getHeader("Accept");
        boolean isApiRequest = (accept != null && accept.contains("application/json"))
                || "POST".equalsIgnoreCase(request.getMethod());

        if (isApiRequest) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            out.print("{\"success\": true, \"message\": \"Logged out successfully.\"}");
            out.flush();
        } else {
            // Standard browser redirect
            response.sendRedirect(request.getContextPath() + "/login.html");
        }
    }
}
