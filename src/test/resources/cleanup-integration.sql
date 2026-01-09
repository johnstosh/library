-- Cleanup test data
DELETE FROM book WHERE id IN (997, 998, 999);
DELETE FROM author WHERE id = 999;
DELETE FROM library WHERE id = 999;
