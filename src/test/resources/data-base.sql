-- (c) Copyright 2025 by Muczynski
-- Minimal base data for all tests - just roles for authentication

-- Insert roles without specifying ID to avoid conflicts with auto-increment
INSERT INTO role (name) VALUES ('USER');
INSERT INTO role (name) VALUES ('LIBRARIAN');
