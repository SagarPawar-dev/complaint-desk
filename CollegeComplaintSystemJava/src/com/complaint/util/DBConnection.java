package com.complaint.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ==============================================================================
 * College Complaint Management System - Database Connection Utility
 * ==============================================================================
 * Manages JDBC connections to Supabase PostgreSQL.
 * Reads credentials securely from WEB-INF/db.properties.
 *
 * Beginner-friendly:
 * - Simple static methods
 * - Safe diagnostics without leaking passwords
 * - Supports both Tomcat runtime and standalone command-line testing
 * ==============================================================================
 */
public class DBConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbDriver;
    private static boolean isInitialized = false;

    // Static initialization block to load properties once
    static {
        loadProperties();
    }

    /**
     * Loads database configuration from db.properties.
     * Looks in the classpath first, then falls back to local file paths.
     */
    private static synchronized void loadProperties() {
        Properties props = new Properties();
        InputStream input = null;

        try {
            // 1. Try loading from ClassLoader (Tomcat WEB-INF/classes or classpath)
            input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");
            if (input == null) {
                input = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties");
            }

            // 2. Fallback: Search common file paths for standalone main() execution
            if (input == null) {
                String[] candidatePaths = {
                    "WEB-INF/db.properties",
                    "CollegeComplaintSystemJava/WEB-INF/db.properties",
                    "../WEB-INF/db.properties",
                    System.getProperty("user.dir") + "/WEB-INF/db.properties",
                    System.getProperty("user.dir") + "/CollegeComplaintSystemJava/WEB-INF/db.properties"
                };

                for (String path : candidatePaths) {
                    File file = new File(path);
                    if (file.exists() && file.isFile()) {
                        input = new FileInputStream(file);
                        break;
                    }
                }
            }

            if (input == null) {
                System.err.println("[DBConnection] ERROR: 'db.properties' could not be found.");
                System.err.println("[DBConnection] Please make sure WEB-INF/db.properties exists.");
                return;
            }

            props.load(input);

            dbDriver = props.getProperty("db.driver", "org.postgresql.Driver").trim();
            dbUrl = props.getProperty("db.url", "").trim();
            dbUser = props.getProperty("db.user", "").trim();
            dbPassword = props.getProperty("db.password", "").trim();

            // Validate driver class
            Class.forName(dbDriver);
            isInitialized = true;

        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] ERROR: PostgreSQL JDBC Driver class not found: " + dbDriver);
            System.err.println("[DBConnection] Verify that 'postgresql-42.7.3.jar' is present in WEB-INF/lib.");
        } catch (Exception e) {
            System.err.println("[DBConnection] ERROR loading 'db.properties': " + e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Establishes and returns a new JDBC database connection.
     *
     * @return java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            loadProperties();
            if (!isInitialized) {
                throw new SQLException("DBConnection is not initialized. Please verify WEB-INF/db.properties and driver jar.");
            }
        }

        // Safety check for unconfigured placeholder values
        if (dbUrl.contains("YOUR_SUPABASE_HOST") || dbUser.contains("YOUR_SUPABASE_DB_USER") || dbPassword.contains("YOUR_SUPABASE_DB_PASSWORD")) {
            throw new SQLException("WEB-INF/db.properties still contains placeholder values. Please edit db.properties with your actual Supabase credentials.");
        }

        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            // Mask password from any logged diagnostic output
            String sanitizedUrl = dbUrl;
            System.err.println("[DBConnection] Connection failed: " + e.getMessage());
            System.err.println("[DBConnection] Attempted URL: " + sanitizedUrl);
            System.err.println("[DBConnection] Attempted User: " + dbUser);
            throw e;
        }
    }

    /**
     * Closes a connection safely.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Standalone diagnostic test runner.
     * Run this class to test database connectivity before running the web app.
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" College Complaint System - JDBC Connection Test ");
        System.out.println("=================================================");

        try {
            System.out.println("[1/3] Loading database configuration...");
            if (!isInitialized) {
                System.err.println("FAILED: Configuration could not be initialized.");
                return;
            }
            System.out.println("      Driver : " + dbDriver);
            System.out.println("      URL    : " + dbUrl);
            System.out.println("      User   : " + dbUser);
            System.out.println("      Pass   : [PROTECTED - " + (dbPassword.isEmpty() ? "EMPTY" : "SET") + "]");

            System.out.println("[2/3] Attempting connection to Supabase PostgreSQL...");
            Connection conn = getConnection();

            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("[3/3] SUCCESS: Connected to database successfully!");
                System.out.println("      Database Product : " + meta.getDatabaseProductName());
                System.out.println("      Database Version : " + meta.getDatabaseProductVersion());
                System.out.println("      Driver Version   : " + meta.getDriverVersion());
                System.out.println("=================================================");
                System.out.println(" Database setup verified and ready for Step 3!   ");
                System.out.println("=================================================");
                conn.close();
            } else {
                System.err.println("FAILED: Connection returned null or closed.");
            }

        } catch (SQLException e) {
            System.err.println("\n-------------------------------------------------");
            System.err.println("TEST FAILED WITH SQL EXCEPTION:");
            System.err.println(e.getMessage());
            System.err.println("-------------------------------------------------");
            System.err.println("Troubleshooting suggestions:");
            System.err.println("1. Did you replace placeholder values in WEB-INF/db.properties?");
            System.err.println("2. Is the Supabase password correct?");
            System.err.println("3. Is '?sslmode=require' appended to the JDBC URL?");
            System.err.println("4. In Supabase, if direct port 5432 fails due to IPv6, use pooler port 6543.");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
