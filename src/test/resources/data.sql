INSERT INTO role (name) VALUES ('USER');
INSERT INTO role (name) VALUES ('LIBRARIAN');

INSERT INTO users (username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token, google_photos_token_expiry, google_client_secret, last_photo_timestamp) VALUES ('librarian', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '', '', '', '', '', '');
INSERT INTO users (username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token, google_photos_token_expiry, google_client_secret, last_photo_timestamp) VALUES ('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '', '', '', '', '', '');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1);

INSERT INTO library (name, hostname) VALUES ('St. Martin de Porres', 'library.muczynskifamily.com');

INSERT INTO author (name) VALUES ('Initial Author');

INSERT INTO book (title, publication_year, author_id, library_id, status) VALUES ('Initial Book', 2023, 1, 1, 'ACTIVE');

INSERT INTO loan (loan_date, return_date, book_id, user_id) VALUES (CURRENT_DATE, NULL, 1, 2);
