<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.DatabaseMetaData" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="com.complaint.util.DBConnection" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Database Connection Test - Complaint Desk</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; color: #0f172a; padding: 40px 20px; }
    .card { max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 28px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.08); border: 1px solid #e2e8f0; }
    h2 { margin-top: 0; color: #1e293b; font-size: 20px; }
    .badge-success { background: #dcfce7; color: #15803d; padding: 6px 12px; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block; margin-bottom: 16px; }
    .badge-error { background: #fee2e2; color: #b91c1c; padding: 6px 12px; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block; margin-bottom: 16px; }
    ul { list-style: none; padding: 0; margin: 16px 0; }
    li { padding: 8px 12px; background: #f1f5f9; border-radius: 6px; margin-bottom: 8px; font-size: 14px; }
    pre { background: #fef2f2; border: 1px solid #fecaca; color: #991b1b; padding: 14px; border-radius: 8px; font-size: 13px; overflow-x: auto; white-space: pre-wrap; }
    .back-link { display: inline-block; margin-top: 20px; color: #2563eb; text-decoration: none; font-size: 14px; font-weight: 500; }
  </style>
</head>
<%
  // Security Guard: Diagnostic endpoint restricted to authenticated Admin users only
  HttpSession userSession = request.getSession(false);
  com.complaint.model.User currentUser = (userSession != null) ? (com.complaint.model.User) userSession.getAttribute("user") : null;
  if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
      response.sendRedirect(request.getContextPath() + "/login.html");
      return;
  }
%>
<body>
  <div class="card">
    <h2>Supabase PostgreSQL Connection Status</h2>
    <%
      Connection conn = null;
      try {
        conn = DBConnection.getConnection();
        DatabaseMetaData meta = conn.getMetaData();

        // Query row counts
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        int userCount = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        stmt.close();
    %>
      <div class="badge-success">&#10003; Successfully Connected via Tomcat 10.1!</div>
      <ul>
        <li><strong>Database Engine:</strong> <%= meta.getDatabaseProductName() %> (<%= meta.getDatabaseProductVersion() %>)</li>
        <li><strong>JDBC Driver:</strong> <%= meta.getDriverName() %> <%= meta.getDriverVersion() %></li>
        <li><strong>Verified Table Data:</strong> Found <%= userCount %> registered user(s) in the 'users' table</li>
      </ul>
      <p style="color: #64748b; font-size: 13px; margin: 0;">JDBC and Supabase integration is functioning properly for Phase 3.</p>
    <%
      } catch (Exception e) {
    %>
      <div class="badge-error">&#10007; Connection Failed</div>
      <p style="font-size: 14px; color: #64748b; margin-top: 0;">Tomcat could not establish a connection using the current <code>WEB-INF/db.properties</code> settings.</p>
      <pre>Database connection could not be established. Please inspect Tomcat server logs for details.</pre>
    <%
      } finally {
        if (conn != null) {
          try { conn.close(); } catch (Exception ignored) {}
        }
      }
    %>
    <a href="admin/dashboard.html" class="back-link">&larr; Return to Admin Dashboard</a>
  </div>
</body>
</html>
