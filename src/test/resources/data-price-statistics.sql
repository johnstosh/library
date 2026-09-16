-- (c) Copyright 2025 by Muczynski
-- Books and AbeBooks listings for price-statistics footer tests.

DELETE FROM loan;
DELETE FROM photo;
DELETE FROM favorites;
DELETE FROM book_price;
DELETE FROM book;
DELETE FROM author;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM role;
DELETE FROM library;

INSERT INTO role (id, name) VALUES (1, 'USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN') ON CONFLICT (name) DO NOTHING;

INSERT INTO library (id, name, library_system_name) VALUES (1, 'St. Martin de Porres', 'Sacred Heart Library System');

INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, last_photo_timestamp, sso_provider)
VALUES (1, 'librarian', '$2a$10$8r2Q3l5gvhlkBNCv32DqI.TRbcvs6up4ATM46w4RgmE2dW3tKo6he', 'test-api-key-for-mock', '', '', 'local');
INSERT INTO users_roles (user_id, role_id) VALUES (1, 2);

INSERT INTO author (id, name) VALUES (1, 'Stats Author');

INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, status_reason, date_added_to_library, last_modified)
VALUES
    (1, 'Cheap Book', 2020, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Mid Book', 2021, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Pricey Book', 2022, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Expensive Book', 2023, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'Twenty Dollar Book', 2023, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'Unpriced Book', 2024, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'Failed Lookup Book', 2024, 'Test Publisher', 1, 1, 'ACTIVE', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO book_price (id, book_id, cover, price_dollars, shipping_dollars, lookup_error, looked_up_at, last_modified)
VALUES
    (1, 1, 'HARDCOVER', 10.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 'SOFTCOVER', 8.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 2, 'HARDCOVER', 25.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 2, 'SOFTCOVER', 30.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 3, 'HARDCOVER', 50.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 3, 'SOFTCOVER', 45.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 4, 'HARDCOVER', 100.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 4, 'SOFTCOVER', 90.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9, 5, 'HARDCOVER', 20.00, 0.00, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (10, 7, 'HARDCOVER', NULL, NULL, 'No matching listing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval('book_id_seq', (SELECT MAX(id) FROM book));
SELECT setval('author_id_seq', (SELECT MAX(id) FROM author));
SELECT setval('library_id_seq', (SELECT MAX(id) FROM library));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));
SELECT setval('book_price_id_seq', (SELECT MAX(id) FROM book_price));
