package com.complaint.filter;

import com.complaint.model.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Role-Based Authorization Filter.
 * Enforces role isolation:
 * - Only ADMIN users can access /admin/*
 * - Only STUDENT users can access /student/* (or redirected appropriately)
 * Compatible with Jakarta EE 10 / Tomcat 10.1.
 */
public class RoleAuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        // Note: AuthenticationFilter runs prior to this filter, but we guard just in case
        if (user == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            return;
        }

        String requestUri = httpRequest.getRequestURI().toLowerCase();
        String role = user.getRole();

        // 1. Guard Admin workspace
        if (requestUri.contains("/admin/")) {
            if (!"ADMIN".equalsIgnoreCase(role)) {
                // Unauthorized role access attempt: Student trying to open Admin pages
                System.err.println("[RoleSecurity] Blocked unauthorized attempt by " + user.getEmail() + " (" + role + ") to access: " + requestUri);
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/student/dashboard.html");
                return;
            }
        }

        // 2. Guard Student workspace
        if (requestUri.contains("/student/")) {
            if (!"STUDENT".equalsIgnoreCase(role)) {
                // Admin trying to open student pages directly -> redirect to Admin workspace
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/admin/dashboard.html");
                return;
            }
        }

        // Authorized for this role
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
