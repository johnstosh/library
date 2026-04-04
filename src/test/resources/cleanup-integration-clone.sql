-- Cleanup clone integration test data
DELETE FROM book WHERE title LIKE 'Test Book with 3-Letter LOC%';
DELETE FROM author WHERE id = 999;
DELETE FROM library WHERE id = 999;
