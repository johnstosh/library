-- Test data for Login UI tests
-- Creates a librarian user for testing authentication

-- Clean up existing data in correct order (respecting foreign keys)
DELETE FROM loan;
DELETE FROM photo;
DELETE FROM book;
DELETE FROM author;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM role;
DELETE FROM library;

-- Insert roles
INSERT INTO role (id, name) VALUES (1, 'USER');
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN');

-- Insert a test library
INSERT INTO library (id, branch_name, library_system_name) VALUES (1, 'Test Library', 'Test Library System');

-- Insert a test librarian user
-- Username: librarian
-- Password: password
-- The BCrypt hash below is BCrypt(SHA-256("password"))
-- Frontend sends SHA-256("password"), backend stores BCrypt(SHA-256("password"))
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (1, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', '', '', '', 'local');

-- Assign LIBRARIAN role
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);
