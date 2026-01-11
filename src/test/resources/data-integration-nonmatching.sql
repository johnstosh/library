-- Insert test library if not exists
INSERT INTO library (id, branch_name, library_system_name) VALUES (999, 'Test Library', 'Test System');

-- Insert test author if not exists
INSERT INTO author (id, name) VALUES (999, 'Test Author');

-- Insert test book without 3-letter LOC start
INSERT INTO book (id, title, loc_number, date_added_to_library, author_id, library_id, status)
VALUES (999, 'Test Book without 3-Letter LOC', 'PS3566.O5', CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');
