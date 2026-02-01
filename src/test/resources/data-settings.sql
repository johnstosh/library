-- Test data for Settings UI tests
-- Creates both USER and LIBRARIAN users for testing settings pages

-- Clean up existing data in correct order (respecting foreign keys)
DELETE FROM loan;
DELETE FROM photo;
DELETE FROM book;
DELETE FROM author;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM role;
DELETE FROM library;
DELETE FROM global_settings;

-- Insert roles
INSERT INTO role (id, name) VALUES (1, 'USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN') ON CONFLICT (name) DO NOTHING;

-- Insert a test library
INSERT INTO library (id, name, library_system_name) VALUES (1, 'Test Library', 'Test Library System');

-- Insert a regular USER
-- Username: testuser
-- Password: password
-- The BCrypt hash below is BCrypt(SHA-256("password"))
-- Frontend sends SHA-256("password"), backend stores BCrypt(SHA-256("password"))
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token,
                   google_photos_token_expiry, google_client_secret, google_photos_album_id, last_photo_timestamp,
                   sso_provider, sso_subject_id, email, library_card_design)
VALUES (1, 'testuser', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he',
        '', '', '', '', '', '', '', 'local', NULL, 'testuser@example.com', 'CLASSICAL_DEVOTION');

-- Insert a LIBRARIAN user
-- Username: librarian
-- Password: password
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token,
                   google_photos_token_expiry, google_client_secret, google_photos_album_id, last_photo_timestamp,
                   sso_provider, sso_subject_id, email, library_card_design)
VALUES (2, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he',
        '', '', '', '', '', '', '', 'local', NULL, 'librarian@example.com', 'CLASSICAL_DEVOTION');

-- Assign USER role to testuser
INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);

-- Assign LIBRARIAN role to librarian (LIBRARIAN includes USER privileges)
INSERT INTO users_roles (user_id, role_id) VALUES (2, 2);

-- Insert global settings (required for Global Settings page tests)
INSERT INTO global_settings (id, google_sso_client_id, google_sso_client_secret, google_client_id, google_client_secret, redirect_uri)
VALUES (1, '', '', '', '', 'http://localhost:8080/oauth2/callback');
