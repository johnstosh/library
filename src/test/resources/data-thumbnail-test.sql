-- (c) Copyright 2025 by Muczynski
-- Test data for Thumbnail Persistence UI tests
-- Creates 5 books each with a photo record (with image_checksum so BookDto gets firstPhotoId/firstPhotoChecksum)

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
INSERT INTO role (id, name) VALUES (1, 'USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN') ON CONFLICT (name) DO NOTHING;

-- Insert a test library
INSERT INTO library (id, name, library_system_name) VALUES (1, 'St. Martin de Porres', 'Sacred Heart Library System');

-- Insert test users
-- Username: librarian, Password: password
-- The BCrypt hash below is BCrypt(SHA-256("password"))
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (1, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', '', '', '', 'local');

-- Assign roles
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2); -- librarian has LIBRARIAN role

-- Insert test author
INSERT INTO author (id, name) VALUES (1, 'Thumbnail Test Author');

-- Insert 5 test books with current timestamp
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, date_added_to_library, last_modified)
VALUES (1, 'Thumbnail Book One', 2023, 'Publisher A', 1, 1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, date_added_to_library, last_modified)
VALUES (2, 'Thumbnail Book Two', 2023, 'Publisher B', 1, 1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, date_added_to_library, last_modified)
VALUES (3, 'Thumbnail Book Three', 2023, 'Publisher C', 1, 1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, date_added_to_library, last_modified)
VALUES (4, 'Thumbnail Book Four', 2023, 'Publisher D', 1, 1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, date_added_to_library, last_modified)
VALUES (5, 'Thumbnail Book Five', 2023, 'Publisher E', 1, 1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert a photo for each book (with image_checksum so BookDto gets firstPhotoId/firstPhotoChecksum)
-- Image data is NULL since the Playwright test intercepts thumbnail requests with a mock PNG
INSERT INTO photo (id, book_id, content_type, image_checksum, photo_order)
VALUES (1, 1, 'image/png', 'checksum_book_1', 1);
INSERT INTO photo (id, book_id, content_type, image_checksum, photo_order)
VALUES (2, 2, 'image/png', 'checksum_book_2', 1);
INSERT INTO photo (id, book_id, content_type, image_checksum, photo_order)
VALUES (3, 3, 'image/png', 'checksum_book_3', 1);
INSERT INTO photo (id, book_id, content_type, image_checksum, photo_order)
VALUES (4, 4, 'image/png', 'checksum_book_4', 1);
INSERT INTO photo (id, book_id, content_type, image_checksum, photo_order)
VALUES (5, 5, 'image/png', 'checksum_book_5', 1);
