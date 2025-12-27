// (c) Copyright 2025 by Muczynski
package com.muczynski.library.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.MessageDigest;

/**
 * Test to generate password hashes for test data.
 */
public class PasswordHashGeneratorTest {

    @Test
    void generatePasswordHash() throws Exception {
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
        System.out.println("Use this in data-login.sql:");
        System.out.println("'" + bcryptHash + "'");
    }
}
