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

INSERT INTO users (id, username, password) VALUES (1, 'librarian', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');
INSERT INTO users (id, username, password) VALUES (2, 'testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');

INSERT INTO users_roles (user_id, role_id) VALUES (1, 1);
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO users_roles (user_id, role_id) VALUES (2, 1);

INSERT INTO library (id, name, hostname) VALUES (1, 'St. Martin de Porres', 'library.muczynskifamily.com');

INSERT INTO author (id, name) VALUES (1, 'Initial Author');

INSERT INTO book (id, title, publication_year, author_id, library_id, status, loc_number, status_reason) VALUES (1, 'Initial Book', 2023, 1, 1, 'ACTIVE', NULL, NULL);

INSERT INTO loan (id, loan_date, due_date, return_date, book_id, user_id) VALUES (1, CURRENT_DATE, CURRENT_DATE + 14, NULL, 1, 2);
