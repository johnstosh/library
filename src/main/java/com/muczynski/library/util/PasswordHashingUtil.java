/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for password hashing operations.
 * Provides SHA-256 hashing to match frontend implementation.
 */
public class PasswordHashingUtil {

    /**
     * Hash a password using SHA-256 algorithm.
     * This matches the frontend hashPassword() implementation in utils.js.
     *
     * @param password The plaintext password to hash
     * @return The SHA-256 hash as a 64-character hex string
     */
    public static String hashPasswordSHA256(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validate if a string is a valid SHA-256 hash (64 hex characters).
     *
     * @param hash The string to validate
     * @return true if the string is a valid SHA-256 hash format
     */
    public static boolean isValidSHA256Hash(String hash) {
        if (hash == null) {
            return false;
        }
        return hash.matches("^[a-f0-9]{64}$");
    }

    /**
     * Validate if a string is a BCrypt hash.
     *
     * @param hash The string to validate
     * @return true if the string is a BCrypt hash format
     */
    public static boolean isBCryptHash(String hash) {
        if (hash == null) {
            return false;
        }
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
