-- (c) Copyright 2025 by Muczynski
-- Insert test library if not exists
INSERT INTO library (id, name, library_system_name) VALUES (999, 'Test Library', 'Test System');

-- Insert test author if not exists
INSERT INTO author (id, name) VALUES (999, 'Test Author');

-- Insert test book without grokipedia_url (null)
INSERT INTO book (id, title, grokipedia_url, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (999, 'Book Without Grokipedia URL', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');

-- Insert test book with empty grokipedia_url (earlier date so it appears second)
INSERT INTO book (id, title, grokipedia_url, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (998, 'Book With Empty Grokipedia URL', '', DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), 999, 999, 'ACTIVE');

-- Insert test book with grokipedia_url (should not be returned)
INSERT INTO book (id, title, grokipedia_url, date_added_to_library, last_modified, author_id, library_id, status)
VALUES (997, 'Book With Grokipedia URL', 'https://grokipedia.com/test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999, 999, 'ACTIVE');
