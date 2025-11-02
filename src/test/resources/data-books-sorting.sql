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

INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp) VALUES (1, 'librarian', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '', '', '');
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp) VALUES (2, 'testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '', '', '');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1);

INSERT INTO library (id, name, hostname) VALUES (1, 'St. Martin de Porres', 'library.muczynskifamily.com');

INSERT INTO author (id, name) VALUES (1, 'Test Author');

INSERT INTO book (id, title, publication_year, author_id, library_id, status, loc_number, status_reason) VALUES
(1, 'Animal Farm', 1945, 1, 1, 'ACTIVE', NULL, NULL),
(2, 'Brave New World', 1932, 1, 1, 'ACTIVE', NULL, NULL),
(3, 'The Color Purple', 1982, 1, 1, 'ACTIVE', NULL, NULL),
(4, 'The Great Gatsby', 1925, 1, 1, 'ACTIVE', NULL, NULL);
