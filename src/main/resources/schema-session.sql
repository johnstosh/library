-- Spring Session JDBC Schema for PostgreSQL
-- This schema is automatically applied when the application starts
-- Fail fast if a previous Cloud Run revision still holds ACCESS EXCLUSIVE locks.
SET lock_timeout = '5s';
SET statement_timeout = '30s';

-- Drop the auto-generated Hibernate check constraint on library_card_design so new enum values are accepted.
ALTER TABLE IF EXISTS users DROP CONSTRAINT IF EXISTS users_library_card_design_check;

-- Hibernate ddl-auto=update does not widen CHECK constraints on enum columns. Widen book_status_check to include REQUESTED.
ALTER TABLE IF EXISTS book DROP CONSTRAINT IF EXISTS book_status_check;
ALTER TABLE book ADD CONSTRAINT book_status_check CHECK (status IN ('ACTIVE', 'LOST', 'WITHDRAWN', 'ON_ORDER', 'REQUESTED'));

-- Hibernate ddl-auto=update does not widen CHECK constraints on enum columns. Widen book_price_cover_check for all BookCoverType values (fixes #341).
ALTER TABLE IF EXISTS book_price DROP CONSTRAINT IF EXISTS book_price_cover_check;
ALTER TABLE book_price ADD CONSTRAINT book_price_cover_check CHECK (cover IN ('HARDCOVER', 'SOFTCOVER', 'LIBRARY_BINDING', 'OTHER', 'UNKNOWN'));

CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);

SET lock_timeout = DEFAULT;
SET statement_timeout = DEFAULT;
