-- (c) Copyright 2025 by Muczynski
-- Insert test library if not exists
INSERT INTO library (id, branch_name, library_system_name) VALUES (999, 'Test Library', 'Test System');

-- Insert test author if not exists
INSERT INTO author (id, name) VALUES (999, 'Test Author');

-- Insert test books with grokipedia_url (should not be returned by without-grokipedia filter)
INSERT INTO book (id, title, grokipedia_url, date_added_to_library, author_id, library_id, status)
VALUES (999, 'Book With Grokipedia URL 1', 'https://grokipedia.com/test1', CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');

INSERT INTO book (id, title, grokipedia_url, date_added_to_library, author_id, library_id, status)
VALUES (998, 'Book With Grokipedia URL 2', 'https://grokipedia.com/test2', CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');
