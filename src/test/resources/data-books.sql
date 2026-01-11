-- (c) Copyright 2025 by Muczynski
-- Test data for Books UI tests
-- Creates test books, authors, library, and users

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
INSERT INTO library (id, branch_name, library_system_name) VALUES (1, 'St. Martin de Porres', 'Sacred Heart Library System');

-- Insert test users
-- Username: librarian, Password: password
-- The BCrypt hash below is BCrypt(SHA-256("password"))
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (1, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', 'test-api-key-for-mock', '', '', 'local');

-- Username: testuser, Password: password
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (2, 'testuser', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', '', '', '', 'local');

-- Assign roles
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2); -- librarian has LIBRARIAN role
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1); -- testuser has USER role

-- Insert test author
INSERT INTO author (id, name) VALUES (1, 'Initial Author');

-- Insert test book with current timestamp for most-recent filter
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, status_reason, date_added_to_library, last_modified)
VALUES (1, 'Initial Book', 2023, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test loan
INSERT INTO loan (id, loan_date, due_date, return_date, book_id, user_id)
VALUES (1, CURRENT_DATE, CURRENT_DATE + 14, NULL, 1, 2);
