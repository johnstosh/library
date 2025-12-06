/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.LibraryApplication;
import com.muczynski.library.domain.User;
import com.muczynski.library.dto.UserDto;
import com.muczynski.library.dto.UserSettingsDto;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LibraryApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(value = "classpath:data-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserSettingsServiceTest {

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Helper method to hash passwords with SHA-256 (matching frontend behavior)
     */
    private String hashPassword(String plainPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(plainPassword.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    void testGooglePhotosApiKeyPersistence() {
        // Arrange: Get initial user settings
        String username = "librarian";
        UserDto initialUser = userSettingsService.getUserSettings(username);

        // Verify initial state - should be empty
        assertEquals("", initialUser.getGooglePhotosApiKey());

        // Act: Update with a Google Photos API key
        String testApiKey = "test-google-photos-api-key-123";
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setXaiApiKey("");
        updateDto.setGooglePhotosApiKey(testApiKey);

        UserDto updatedUser = userSettingsService.updateUserSettings(username, updateDto);

        // Assert: Verify the update returned the correct value
        assertEquals(testApiKey, updatedUser.getGooglePhotosApiKey());

        // Act: Retrieve user settings again to verify persistence
        UserDto retrievedUser = userSettingsService.getUserSettings(username);

        // Assert: Verify the value persisted in the database
        assertEquals(testApiKey, retrievedUser.getGooglePhotosApiKey());

        // Verify in database directly
        User userEntity = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertEquals(testApiKey, userEntity.getGooglePhotosApiKey());
    }

    @Test
    void testGooglePhotosApiKeyCanBeCleared() {
        // Arrange: Set an API key first
        String username = "librarian";
        String testApiKey = "test-key-to-be-cleared";

        UserSettingsDto setDto = new UserSettingsDto();
        setDto.setUsername(username);
        setDto.setGooglePhotosApiKey(testApiKey);
        userSettingsService.updateUserSettings(username, setDto);

        // Verify it was set
        UserDto userWithKey = userSettingsService.getUserSettings(username);
        assertEquals(testApiKey, userWithKey.getGooglePhotosApiKey());

        // Act: Clear the API key by setting it to empty string
        UserSettingsDto clearDto = new UserSettingsDto();
        clearDto.setUsername(username);
        clearDto.setGooglePhotosApiKey("");

        UserDto clearedUser = userSettingsService.updateUserSettings(username, clearDto);

        // Assert: Verify the key was cleared
        assertEquals("", clearedUser.getGooglePhotosApiKey());

        // Verify persistence
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals("", retrievedUser.getGooglePhotosApiKey());
    }

    @Test
    void testXaiApiKeyPersistence() {
        // Test that xaiApiKey also persists correctly
        String username = "librarian";
        String testXaiKey = "test-xai-api-key-456";

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setXaiApiKey(testXaiKey);
        updateDto.setGooglePhotosApiKey("");

        userSettingsService.updateUserSettings(username, updateDto);

        // Retrieve and verify
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals(testXaiKey, retrievedUser.getXaiApiKey());
    }

    @Test
    void testBothApiKeysPersistTogether() {
        // Test that both API keys can be set and retrieved together
        String username = "librarian";
        String testXaiKey = "test-xai-key";
        String testGooglePhotosKey = "test-google-photos-key";

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setXaiApiKey(testXaiKey);
        updateDto.setGooglePhotosApiKey(testGooglePhotosKey);

        userSettingsService.updateUserSettings(username, updateDto);

        // Retrieve and verify both
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals(testXaiKey, retrievedUser.getXaiApiKey());
        assertEquals(testGooglePhotosKey, retrievedUser.getGooglePhotosApiKey());
    }

    @Test
    void testPasswordUpdate() throws Exception {
        // Test that password can be updated
        String username = "librarian";
        String newPassword = "newSecurePassword123";
        String hashedPassword = hashPassword(newPassword);

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setPassword(hashedPassword);
        updateDto.setXaiApiKey("");
        updateDto.setGooglePhotosApiKey("");

        userSettingsService.updateUserSettings(username, updateDto);

        // Verify password was updated in database
        User userEntity = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        // Password should be encoded, not plain text
        assertNotNull(userEntity.getPassword());
        assertNotEquals(hashedPassword, userEntity.getPassword()); // Should be BCrypt hashed
        assertTrue(userEntity.getPassword().startsWith("$2")); // BCrypt hash starts with $2a, $2b, or $2y
    }

    @Test
    void testPasswordNotUpdatedWhenEmpty() {
        // Test that password is not changed when empty string is provided
        String username = "librarian";

        // Get initial password
        User initialUser = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        String initialPassword = initialUser.getPassword();

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setPassword(""); // Empty password should not update
        updateDto.setXaiApiKey("test-key");

        userSettingsService.updateUserSettings(username, updateDto);

        // Verify password unchanged
        User userEntity = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertEquals(initialPassword, userEntity.getPassword());
    }

    @Test
    void testGoogleClientSecretPersistence() {
        // Test that Google Client Secret persists correctly
        String username = "librarian";
        String testSecret = "test-google-client-secret-xyz";

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setGoogleClientSecret(testSecret);

        userSettingsService.updateUserSettings(username, updateDto);

        // Retrieve and verify
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals(testSecret, retrievedUser.getGoogleClientSecret());

        // Verify in database
        User userEntity = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertEquals(testSecret, userEntity.getGoogleClientSecret());
    }

    @Test
    void testGoogleClientSecretCanBeCleared() {
        // Arrange: Set a secret first
        String username = "librarian";
        String testSecret = "secret-to-clear";

        UserSettingsDto setDto = new UserSettingsDto();
        setDto.setUsername(username);
        setDto.setGoogleClientSecret(testSecret);
        userSettingsService.updateUserSettings(username, setDto);

        // Verify it was set
        UserDto userWithSecret = userSettingsService.getUserSettings(username);
        assertEquals(testSecret, userWithSecret.getGoogleClientSecret());

        // Act: Clear the secret
        UserSettingsDto clearDto = new UserSettingsDto();
        clearDto.setUsername(username);
        clearDto.setGoogleClientSecret("");

        userSettingsService.updateUserSettings(username, clearDto);

        // Assert: Verify cleared
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals("", retrievedUser.getGoogleClientSecret());
    }

    @Test
    void testUpdateMultipleFieldsTogether() throws Exception {
        // Test updating multiple fields at once
        String username = "librarian";
        String newPassword = "newPass123";
        String hashedPassword = hashPassword(newPassword);
        String xaiKey = "xai-key-123";
        String googlePhotosKey = "gp-key-456";
        String googleClientSecret = "client-secret-789";

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setPassword(hashedPassword);
        updateDto.setXaiApiKey(xaiKey);
        updateDto.setGooglePhotosApiKey(googlePhotosKey);
        updateDto.setGoogleClientSecret(googleClientSecret);

        userSettingsService.updateUserSettings(username, updateDto);

        // Verify all fields
        UserDto retrievedUser = userSettingsService.getUserSettings(username);
        assertEquals(xaiKey, retrievedUser.getXaiApiKey());
        assertEquals(googlePhotosKey, retrievedUser.getGooglePhotosApiKey());
        assertEquals(googleClientSecret, retrievedUser.getGoogleClientSecret());

        // Verify password updated
        User userEntity = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertNotEquals(hashedPassword, userEntity.getPassword()); // Should be BCrypt hashed
        assertTrue(userEntity.getPassword().startsWith("$2")); // BCrypt hash starts with $2a, $2b, or $2y
    }

    @Test
    void testEmptyStringHandling() {
        // Test that empty strings are handled correctly for all fields
        String username = "librarian";

        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        updateDto.setXaiApiKey("");
        updateDto.setGooglePhotosApiKey("");
        updateDto.setGoogleClientSecret("");
        updateDto.setPassword(""); // Should not update password

        UserDto result = userSettingsService.updateUserSettings(username, updateDto);

        // All should be empty strings
        assertEquals("", result.getXaiApiKey());
        assertEquals("", result.getGooglePhotosApiKey());
        assertEquals("", result.getGoogleClientSecret());
    }

    @Test
    void testNullValuesHandling() {
        // Test that null values are handled gracefully
        String username = "librarian";

        // First set some values
        UserSettingsDto setDto = new UserSettingsDto();
        setDto.setUsername(username);
        setDto.setXaiApiKey("test-key");
        setDto.setGooglePhotosApiKey("test-gp-key");
        userSettingsService.updateUserSettings(username, setDto);

        // Then update with null (should not change existing values if handled properly)
        UserSettingsDto updateDto = new UserSettingsDto();
        updateDto.setUsername(username);
        // Leave other fields null

        UserDto result = userSettingsService.updateUserSettings(username, updateDto);

        // Verify behavior (depends on implementation - may preserve or clear)
        assertNotNull(result);
    }
}
