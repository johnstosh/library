-- (c) Copyright 2025 by Muczynski
-- Extra book for the Without LOC filter UI test: electronic resource, no call number.
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number, electronic_resource, date_added_to_library, last_modified)
VALUES (2, 'Electronic Resource Without LOC', 2023, 'Test Publisher', 1, 1, 'ACTIVE', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

SELECT setval('book_id_seq', (SELECT MAX(id) FROM book));
