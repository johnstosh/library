-- Test data for Loans UI tests
-- Creates USER and LIBRARIAN users, books, and loans for testing

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
INSERT INTO library (id, name, hostname) VALUES (1, 'Test Library', 'test.example.com');

-- Insert authors
INSERT INTO author (id, name, bio, birth_year, death_year) VALUES (1, 'Test Author 1', 'Bio 1', 1950, NULL);
INSERT INTO author (id, name, bio, birth_year, death_year) VALUES (2, 'Test Author 2', 'Bio 2', 1960, NULL);

-- Insert books (3 active books for testing)
INSERT INTO book (id, title, publication_year, publisher, isbn, loc_call_number, author_id, library_id, status)
VALUES (1, 'Available Book 1', 2020, 'Publisher 1', '1234567890', '', 1, 1, 'ACTIVE');

INSERT INTO book (id, title, publication_year, publisher, isbn, loc_call_number, author_id, library_id, status)
VALUES (2, 'Available Book 2', 2021, 'Publisher 2', '1234567891', '', 2, 1, 'ACTIVE');

INSERT INTO book (id, title, publication_year, publisher, isbn, loc_call_number, author_id, library_id, status)
VALUES (3, 'Loaned Book', 2022, 'Publisher 3', '1234567892', '', 1, 1, 'ACTIVE');

-- Insert a regular USER
-- Username: testuser
-- Password: password
-- The BCrypt hash below is BCrypt(SHA-256("password"))
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

-- Insert another regular USER for testing librarian viewing all loans
-- Username: otheruser
-- Password: password
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token,
                   google_photos_token_expiry, google_client_secret, google_photos_album_id, last_photo_timestamp,
                   sso_provider, sso_subject_id, email, library_card_design)
VALUES (3, 'otheruser', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he',
        '', '', '', '', '', '', '', 'local', NULL, 'otheruser@example.com', 'CLASSICAL_DEVOTION');

-- Assign USER role to testuser
INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);

-- Assign LIBRARIAN role to librarian
INSERT INTO users_roles (user_id, role_id) VALUES (2, 2);

-- Assign USER role to otheruser
INSERT INTO users_roles (user_id, role_id) VALUES (3, 1);

-- Insert loans
-- testuser has 1 active loan and 1 returned loan
INSERT INTO loan (id, loan_date, due_date, return_date, book_id, user_id)
VALUES (1, CURRENT_DATE - 5, CURRENT_DATE + 9, NULL, 3, 1);

INSERT INTO loan (id, loan_date, due_date, return_date, book_id, user_id)
VALUES (2, CURRENT_DATE - 20, CURRENT_DATE - 6, CURRENT_DATE - 5, 1, 1);

-- otheruser has 1 active loan (librarian should see this but testuser should not)
INSERT INTO loan (id, loan_date, due_date, return_date, book_id, user_id)
VALUES (3, CURRENT_DATE - 3, CURRENT_DATE + 11, NULL, 2, 3);
