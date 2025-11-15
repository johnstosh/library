-- Migration to convert Photo.image from OID (Large Object) to BYTEA
-- This fixes the "Large Objects may not be used in auto-commit mode" error in PostgreSQL

-- IMPORTANT: This migration is only needed if you're running PostgreSQL and the
-- Photo.image column is currently of type OID. If you're using H2 or if the table
-- hasn't been created yet, this migration is not needed.

-- Step 1: Check current column type (for informational purposes)
-- Run this first to see what type the column currently is:
-- SELECT column_name, data_type, udt_name
-- FROM information_schema.columns
-- WHERE table_name = 'photo' AND column_name = 'image';

-- Expected output if migration is needed:
-- column_name | data_type | udt_name
-- image       | USER-DEFINED | oid

-- Step 2: Convert the column from OID to BYTEA
-- WARNING: This will convert all existing LOB data to BYTEA
-- Backup your database before running this!

BEGIN;

-- For PostgreSQL: Convert OID column to BYTEA
-- This assumes the column is currently named 'image' and is of type OID
DO $$
BEGIN
    -- Check if the column exists and is of type oid
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'photo'
        AND column_name = 'image'
        AND udt_name = 'oid'
    ) THEN
        -- Create a temporary column to hold the bytea data
        ALTER TABLE photo ADD COLUMN image_bytea BYTEA;

        -- Copy data from OID to BYTEA
        -- This reads each large object and stores it as bytea
        UPDATE photo SET image_bytea = lo_get(image)::bytea WHERE image IS NOT NULL;

        -- Drop the old OID column
        ALTER TABLE photo DROP COLUMN image;

        -- Rename the new column to the original name
        ALTER TABLE photo RENAME COLUMN image_bytea TO image;

        RAISE NOTICE 'Successfully converted Photo.image from OID to BYTEA';
    ELSE
        RAISE NOTICE 'Photo.image column is not of type OID, no migration needed';
    END IF;
END $$;

COMMIT;

-- Step 3: Verify the migration
-- Run this to confirm the column is now BYTEA:
-- SELECT column_name, data_type, udt_name
-- FROM information_schema.columns
-- WHERE table_name = 'photo' AND column_name = 'image';

-- Expected output after migration:
-- column_name | data_type | udt_name
-- image       | bytea     | bytea

-- Step 4: Clean up orphaned large objects (optional but recommended)
-- After migration, you may have orphaned large objects in PostgreSQL
-- This is optional but recommended to reclaim disk space:
--
-- SELECT lo_unlink(loid) FROM (
--     SELECT DISTINCT image AS loid FROM photo WHERE image IS NOT NULL
-- ) AS large_objects;

-- Note: Only run the cleanup if you're absolutely sure the migration succeeded
-- and you don't need to roll back.
