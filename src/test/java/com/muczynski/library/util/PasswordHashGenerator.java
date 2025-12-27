// (c) Copyright 2025 by Muczynski
package com.muczynski.library.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.MessageDigest;

/**
 * Utility to generate BCrypt hash of SHA-256 hashed password.
 * Run this to generate test password hashes.
 */
public class PasswordHashGenerator {
    public static void main(String[] args) throws Exception {
        String plainPassword = "password";

        // Step 1: SHA-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(plainPassword.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String sha256Hash = hexString.toString();

        System.out.println("Plain password: " + plainPassword);
        System.out.println("SHA-256 hash: " + sha256Hash);

        // Step 2: BCrypt the SHA-256 hash
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String bcryptHash = encoder.encode(sha256Hash);

        System.out.println("BCrypt(SHA-256) hash: " + bcryptHash);
        System.out.println();
        System.out.println("Use this in SQL:");
        System.out.println("'" + bcryptHash + "'");
    }
}
