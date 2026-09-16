-- (c) Copyright 2025 by Muczynski
-- Insert test library if not exists
INSERT INTO library (id, name, library_system_name) VALUES (999, 'Test Library', 'Test System');

-- Insert test author if not exists
INSERT INTO author (id, name) VALUES (999, 'Test Author');

-- Print book without LOC (should be returned)
INSERT INTO book (id, title, loc_number, electronic_resource, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (999, 'Print Book Without LOC', NULL, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');

-- Print book with empty LOC (should be returned)
INSERT INTO book (id, title, loc_number, electronic_resource, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (998, 'Print Book With Empty LOC', '', false, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', 999, 999, 'ACTIVE');

-- Electronic resource without LOC (should be excluded)
INSERT INTO book (id, title, loc_number, electronic_resource, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (997, 'Electronic Resource Without LOC', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');
