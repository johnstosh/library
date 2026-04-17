-- (c) Copyright 2025 by Muczynski
-- Test data for apply for card functionality

-- Clean up existing data in correct order (respecting foreign keys)
DELETE FROM applied;
DELETE FROM loan;
DELETE FROM photo;
DELETE FROM book;
DELETE FROM author;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM role;
DELETE FROM library;

INSERT INTO role (id, name) VALUES (1, 'USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN') ON CONFLICT (name) DO NOTHING;

-- Insert a test library
INSERT INTO library (id, name, library_system_name) VALUES (1, 'Test Library', 'Test Library System');

-- Insert a test librarian user for admin tasks
-- Username: librarian, Password: password
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (1, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', '', '', '', 'local');

-- Assign LIBRARIAN role
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);

-- Reset sequences to avoid duplicate key violations when auto-generating IDs after explicit inserts
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));
SELECT setval('library_id_seq', (SELECT MAX(id) FROM library));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- No pre-existing applications needed for basic apply for card tests
-- Tests will create new applications through the UI
