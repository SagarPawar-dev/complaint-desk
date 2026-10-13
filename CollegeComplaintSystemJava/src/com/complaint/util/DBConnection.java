package com.complaint.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
 * Reads credentials securely from WEB-INF/db.properties or environment variables.
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
     * Loads database configuration from environment variables or db.properties.
     * Checks DB_URL / DATABASE_URL first, then falls back to db.properties.
     */
    private static synchronized void loadProperties() {
        // 1. Priority: Check Environment Variables (Render, Container, Cloud Deployments)
        String envUrl = getFirstEnv("DB_URL", "DATABASE_URL");
        String envUser = getFirstEnv("DB_USER", "DATABASE_USER", "DB_USERNAME");
        String envPassword = getFirstEnv("DB_PASSWORD", "DATABASE_PASSWORD", "DB_PASS");
        String envDriver = getFirstEnv("DB_DRIVER", "DATABASE_DRIVER");

        if (envUrl != null && !envUrl.trim().isEmpty()) {
            envUrl = envUrl.trim();

            // Auto-parse full URI format: postgresql://[user]:[password]@[host]:[port]/[database]
            // or postgres://... or jdbc:postgresql://...
            if (envUrl.startsWith("postgres://") || envUrl.startsWith("postgresql://") || envUrl.startsWith("jdbc:postgres://") || envUrl.startsWith("jdbc:postgresql://")) {
                try {
                    String rawUri = envUrl;
                    if (rawUri.startsWith("jdbc:")) {
                        rawUri = rawUri.substring(5);
                    }
                    if (rawUri.startsWith("postgres://")) {
                        rawUri = "postgresql://" + rawUri.substring(11);
                    }

                    URI uri = new URI(rawUri);
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isEmpty()) {
                        String[] parts = userInfo.split(":", 2);
                        if ((envUser == null || envUser.isEmpty()) && parts.length > 0) {
                            envUser = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name());
                        }
                        if ((envPassword == null || envPassword.isEmpty()) && parts.length > 1) {
                            envPassword = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
                        }
                    }

                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    if (path == null || path.isEmpty() || path.equals("/")) {
                        path = "/postgres";
                    }

                    String query = uri.getQuery();
                    StringBuilder jdbcBuilder = new StringBuilder();
                    jdbcBuilder.append("jdbc:postgresql://").append(uri.getHost()).append(":").append(port).append(path);
                    if (query != null && !query.isEmpty()) {
                        if (!query.contains("sslmode=")) {
                            jdbcBuilder.append("?").append(query).append("&sslmode=require");
                        } else {
                            jdbcBuilder.append("?").append(query);
                        }
                    } else {
                        jdbcBuilder.append("?sslmode=require");
                    }
                    envUrl = jdbcBuilder.toString();
                } catch (Exception ex) {
                    if (!envUrl.startsWith("jdbc:")) {
                        if (envUrl.startsWith("postgres://")) {
                            envUrl = "jdbc:postgresql://" + envUrl.substring(11);
                        } else {
                            envUrl = "jdbc:" + envUrl;
                        }
                    }
                    if (!envUrl.contains("sslmode=")) {
                        envUrl += (envUrl.contains("?") ? "&" : "?") + "sslmode=require";
                    }
                }
            } else if (!envUrl.startsWith("jdbc:")) {
                envUrl = "jdbc:postgresql://" + envUrl;
                if (!envUrl.contains("sslmode=")) {
                    envUrl += (envUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }
            }

            dbUrl = envUrl;
            dbUser = (envUser != null) ? envUser.trim() : "";
            dbPassword = (envPassword != null) ? envPassword.trim() : "";
            dbDriver = (envDriver != null && !envDriver.trim().isEmpty()) ? envDriver.trim() : "org.postgresql.Driver";

            try {
                Class.forName(dbDriver);
                isInitialized = true;
                System.out.println("[DBConnection] Configured database from environment variables.");
                System.out.println("[DBConnection] Target URL : " + dbUrl);
                System.out.println("[DBConnection] Target User: " + (dbUser.isEmpty() ? "(not set)" : dbUser));
                return;
            } catch (ClassNotFoundException e) {
                System.err.println("[DBConnection] ERROR: PostgreSQL JDBC Driver class not found: " + dbDriver);
                System.err.println("[DBConnection] Verify that 'postgresql-42.7.3.jar' is present in WEB-INF/lib.");
                return;
            }
        }

        // 2. Fallback: Load from db.properties for local development
        Properties props = new Properties();
        InputStream input = null;

        try {
            // A. Try loading from ClassLoader (Tomcat WEB-INF/classes or classpath)
            input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");
            if (input == null) {
                input = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties");
            }

            // B. Try locating WEB-INF/db.properties relative to compiled class location (Tomcat WebApp)
            if (input == null) {
                try {
                    java.net.URL location = DBConnection.class.getProtectionDomain().getCodeSource().getLocation();
                    if (location != null) {
                        File base = new File(location.toURI());
                        File parent = base.isDirectory() ? base.getParentFile() : base.getParentFile().getParentFile();
                        if (parent != null) {
                            File candidate = new File(parent, "db.properties");
                            if (candidate.exists() && candidate.isFile()) {
                                input = new FileInputStream(candidate);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // C. Fallback: Search common file paths for standalone main() execution
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
                System.err.println("[DBConnection] ERROR: Neither DB_URL environment variable nor 'db.properties' was found.");
                System.err.println("[DBConnection] Please provide DB_URL, DB_USER, DB_PASSWORD environment variables or WEB-INF/db.properties.");
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
            // Transient WAN network retry (common with cloud databases over public internet)
            try {
                Thread.sleep(250);
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } catch (Exception retryEx) {
                // Mask password from any logged diagnostic output
                String sanitizedUrl = dbUrl;
                System.err.println("[DBConnection] Connection failed: " + e.getMessage());
                System.err.println("[DBConnection] Attempted URL: " + sanitizedUrl);
                System.err.println("[DBConnection] Attempted User: " + dbUser);
                throw e;
            }
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
     * Helper to return the first non-empty environment variable value among candidate names.
     */
    private static String getFirstEnv(String... names) {
        for (String name : names) {
            String val = System.getenv(name);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return null;
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
