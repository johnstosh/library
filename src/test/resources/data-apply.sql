-- (c) Copyright 2025 by Muczynski
-- Test data for apply for card functionality
DELETE FROM applied;
DELETE FROM users_roles;
DELETE FROM users;
DELETE FROM role;

INSERT INTO role (id, name) VALUES (1, 'USER');
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN');

-- No pre-existing applications needed for basic apply for card test
-- Tests will create new applications through the UI
