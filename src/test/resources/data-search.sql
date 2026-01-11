-- (c) Copyright 2025 by Muczynski
-- Test data for search functionality
DELETE FROM users_roles;
DELETE FROM loan;
DELETE FROM photo;
DELETE FROM book;
DELETE FROM author;
DELETE FROM library;
DELETE FROM users;
DELETE FROM role;

INSERT INTO role (id, name) VALUES (1, 'USER');
INSERT INTO role (id, name) VALUES (2, 'LIBRARIAN');

INSERT INTO library (id, branch_name, library_system_name) VALUES (1, 'St. Martin de Porres', 'Sacred Heart Library System');
INSERT INTO library (id, branch_name, library_system_name) VALUES (2, 'Holy Family Library', 'Sacred Heart Library System');

-- Insert authors
INSERT INTO author (id, name, brief_biography, date_of_birth, date_of_death) VALUES
    (1, 'Thomas Aquinas', 'Medieval philosopher and theologian', '1225-01-01', '1274-03-07'),
    (2, 'Augustine of Hippo', 'Early Christian theologian and philosopher', '0354-11-13', '0430-08-28'),
    (3, 'John Paul II', 'Pope and philosopher', '1920-05-18', '2005-04-02'),
    (4, 'Teresa of Avila', 'Spanish mystic and Doctor of the Church', '1515-03-28', '1582-10-04'),
    (5, 'Francis of Assisi', 'Founder of the Franciscan order', '1181-09-26', '1226-10-03');

-- Insert books with various titles for searching
INSERT INTO book (id, title, publication_year, publisher, author_id, library_id, status, loc_number) VALUES
    (1, 'Summa Theologica', 1485, 'Catholic Press', 1, 1, 'ACTIVE', 'BX1749 .A6'),
    (2, 'Confessions', 397, 'Ancient Books', 2, 1, 'ACTIVE', 'BR65 .A9'),
    (3, 'City of God', 426, 'Ancient Books', 2, 1, 'ACTIVE', 'BR65 .A92'),
    (4, 'Theology of the Body', 1997, 'Pauline Books', 3, 2, 'ACTIVE', 'BT701 .J6'),
    (5, 'Interior Castle', 1577, 'Carmelite Press', 4, 1, 'ACTIVE', 'BX2179 .T4'),
    (6, 'The Way of Perfection', 1566, 'Carmelite Press', 4, 2, 'ACTIVE', 'BX2179 .T42'),
    (7, 'Little Flowers', 1476, 'Franciscan Press', 5, 1, 'WITHDRAWN', NULL),
    (8, 'Canticle of the Sun', 1224, 'Franciscan Press', 5, 1, 'ACTIVE', NULL);
