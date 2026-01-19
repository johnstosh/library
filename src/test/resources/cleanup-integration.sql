-- Cleanup test data
DELETE FROM book WHERE id IN (997, 998, 999);
DELETE FROM author WHERE id IN (998, 999);
DELETE FROM library WHERE id IN (998, 999);
