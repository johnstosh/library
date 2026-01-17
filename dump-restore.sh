#!/bin/bash
# dump-restore.sh
# Copies database 'library' → 'library-dev' on Google Cloud SQL PostgreSQL
# Uses pg_dump | pg_restore pipe (no intermediate file)
# Important: run this script from a machine that can connect to your Cloud SQL instance
#            or inside a Docker container with postgres client tools
# Matches variable names & style from deploy.sh

set -euo pipefail

# ── Configuration ───────────────────────────────────────────────────────────────
# Reuse variables from your environment/script (matching deploy.sh)
: "${GCP_PROJECT_ID:?Error: Missing required environment variable: GCP_PROJECT_ID}"
: "${DB_PASSWORD:?Error: Missing required environment variable: DB_PASSWORD}"
: "${GCP_REGION:=us-east1}"  # Default like deploy.sh

# Set Cloud SQL instance name (matching deploy.sh)
CLOUD_SQL_INSTANCE_NAME=scrabble-db

# Most common values in Cloud SQL (change only if different)
DB_HOST="127.0.0.1"               # ← use this with Cloud SQL Auth Proxy
DB_PORT="5432"
DB_USER="postgres"                # or your Cloud SQL admin user

SOURCE_DB="library"
TARGET_DB="library-dev"

# Extra pg_dump options (customize if needed)
DUMP_OPTS="--no-owner --no-privileges --verbose --format=custom"
# You can add: --exclude-table-data='audit.*' --jobs=4  etc.

# ── Safety checks ───────────────────────────────────────────────────────────────
echo "This script will:"
echo "  1. DROP & recreate database '$TARGET_DB'"
echo "  2. Copy all data & schema from '$SOURCE_DB' into it"
echo ""
echo "Source connection:  $DB_USER@$DB_HOST:$DB_PORT / db=$SOURCE_DB"
echo "Target database:    $TARGET_DB"
echo ""
read -p "Are you sure? (type YES to continue): " confirmation

if [[ "$confirmation" != "YES" ]]; then
  echo "Aborted."
  exit 1
fi

# ── Main logic ──────────────────────────────────────────────────────────────────

echo "→ Dropping & recreating target database '$TARGET_DB'..."

PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres <<EOF || true
DROP DATABASE IF EXISTS "$TARGET_DB";
CREATE DATABASE "$TARGET_DB";
EOF

echo "→ Starting database copy (pg_dump → pg_restore pipe)..."

PGPASSWORD="$DB_PASSWORD" pg_dump $DUMP_OPTS \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$SOURCE_DB" \
  | PGPASSWORD="$DB_PASSWORD" pg_restore \
      --verbose --no-owner --no-privileges \
      --clean --if-exists \
      -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB"

echo ""
echo "Done! Database '$TARGET_DB' should now be a fresh copy of '$SOURCE_DB'."
echo "You can connect to it using the same credentials."
