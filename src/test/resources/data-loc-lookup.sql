DELETE FROM users_roles;
DELETE FROM loan;
DELETE FROM photo;
DELETE FROM book;
DELETE FROM author;
DELETE FROM library;
DELETE FROM users;
DELETE FROM role;

INSERT INTO role (id, name) VALUES (1, 'USER');
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN');

INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp) VALUES (1, 'librarian', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'test-api-key-for-mock', '', '');
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp) VALUES (2, 'testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '', '', '');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1);

INSERT INTO library (id, name, hostname) VALUES (1, 'St. Martin de Porres', 'library.muczynskifamily.com');

INSERT INTO author (id, name) VALUES (1, 'Test Author');
INSERT INTO author (id, name) VALUES (2, 'Another Author');

-- Book with LOC number (should show in "View All" but not in "View Missing")
INSERT INTO book (id, title, publication_year, author_id, library_id, status, loc_number, status_reason, date_added_to_library)
VALUES (1, 'Book with LOC', 2023, 1, 1, 'ACTIVE', 'PS3505.H45 A6 1997', NULL, CURRENT_TIMESTAMP);

-- Books without LOC numbers (should show in both "View All" and "View Missing")
INSERT INTO book (id, title, publication_year, author_id, library_id, status, loc_number, status_reason, date_added_to_library)
VALUES (2, 'Book Missing LOC 1', 2022, 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP);

INSERT INTO book (id, title, publication_year, author_id, library_id, status, loc_number, status_reason, date_added_to_library)
VALUES (3, 'Book Missing LOC 2', 2021, 2, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP);
