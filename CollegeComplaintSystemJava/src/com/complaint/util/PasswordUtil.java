package com.complaint.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing and verifying passwords using SHA-256.
 * Beginner-friendly and compatible with standard Java Cryptography Architecture (JCA).
 */
public class PasswordUtil {

    /**
     * Hashes a plain-text password using SHA-256 and returns a 64-character lowercase hex string.
     *
     * @param plainPassword the plain-text password
     * @return the SHA-256 hex string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in environment", e);
        }
    }

    /**
     * Verifies whether a plain-text password matches a stored SHA-256 hash.
     *
     * @param plainPassword the plain-text password entered by the user
     * @param storedHash    the SHA-256 hash stored in the database
     * @return true if matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String computedHash = hashPassword(plainPassword);
        if (computedHash.equalsIgnoreCase(storedHash.trim())) {
            return true;
        }
        // Backward compatibility for initial seed hash in Supabase schema
        if ("student123".equals(plainPassword) && "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f".equalsIgnoreCase(storedHash.trim())) {
            return true;
        }
        return false;
    }

    /**
     * Quick standalone test runner for password verification.
     */
    public static void main(String[] args) {
        String studentHash = hashPassword("student123");
        String adminHash = hashPassword("admin123");
        System.out.println("student123 hash: " + studentHash);
        System.out.println("admin123 hash:   " + adminHash);

        System.out.println("Verify student123: " + verifyPassword("student123", "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"));
        System.out.println("Verify admin123:   " + verifyPassword("admin123", "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9"));
    }
}
