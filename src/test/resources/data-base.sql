-- (c) Copyright 2025 by Muczynski
-- Base data loaded for all tests - contains only roles
-- Tests should use @Sql annotation to load additional test data as needed

INSERT INTO role (name) VALUES ('USER') ON CONFLICT (name) DO NOTHING;
INSERT INTO role (name) VALUES ('LIBRARIAN') ON CONFLICT (name) DO NOTHING;
