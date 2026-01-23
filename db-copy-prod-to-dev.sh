#!/bin/bash
# (c) Copyright 2025 by Muczynski
# db-copy-prod-to-dev.sh
# Copies database 'library' → 'library-dev' on Google Cloud SQL PostgreSQL
# Runs in a Docker container with Cloud SQL Auth Proxy for secure connection
# Uses pg_dump | pg_restore pipe (no intermediate file)

set -euo pipefail

# ── Configuration ───────────────────────────────────────────────────────────────
# Reuse variables from your environment (matching deploy.sh)
: "${GCP_PROJECT_ID:?Error: Missing required environment variable: GCP_PROJECT_ID}"
: "${DB_PASSWORD:?Error: Missing required environment variable: DB_PASSWORD}"
: "${GCP_REGION:=us-east1}"  # Default like deploy.sh

# Set Cloud SQL instance name (matching deploy.sh)
CLOUD_SQL_INSTANCE_NAME=scrabble-db

# Database names
SOURCE_DB="library"
TARGET_DB="library-dev"

# Docker image with postgres tools (pg_dump, pg_restore, psql)
DOCKER_IMAGE="postgres:16"

# ── Safety checks ───────────────────────────────────────────────────────────────
echo "This script will:"
echo "  1. DROP & recreate database '$TARGET_DB'"
echo "  2. Copy all data & schema from '$SOURCE_DB' into it"
echo ""
echo "Source database: $SOURCE_DB"
echo "Target database: $TARGET_DB"
echo "Cloud SQL instance: $GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME"
echo ""
read -p "Are you sure? (type YES to continue): " confirmation

if [[ "$confirmation" != "YES" ]]; then
  echo "Aborted."
  exit 1
fi

# ── Get access token ────────────────────────────────────────────────────────────
gcloud config set project "$GCP_PROJECT_ID" --quiet

echo "→ Fetching access token..."
ACCESS_TOKEN=$(gcloud auth print-access-token)
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: Failed to get access token. Ensure you're authenticated with gcloud."
  exit 1
fi

# ── Run in Docker ───────────────────────────────────────────────────────────────
# This runs a temporary container that:
# 1. Installs wget & ca-certificates (for HTTPS)
# 2. Downloads the Cloud SQL Proxy binary
# 3. Starts the proxy in background with --token
# 4. Runs the dump-restore commands

echo "Starting Docker container to perform the database copy..."

docker run --rm -it \
  --user root \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e GCP_REGION="$GCP_REGION" \
  -e ACCESS_TOKEN="$ACCESS_TOKEN" \
  -e SOURCE_DB="$SOURCE_DB" \
  -e TARGET_DB="$TARGET_DB" \
  -e CLOUD_SQL_INSTANCE_NAME="$CLOUD_SQL_INSTANCE_NAME" \
  --network host \
  "$DOCKER_IMAGE" bash -c '
    set -euo pipefail

    # Database connection settings (via Cloud SQL Proxy)
    DB_HOST="127.0.0.1"
    DB_PORT="5432"
    DB_USER="postgres"

    # Install wget & ca-certificates (needed for HTTPS download)
    echo "→ Installing wget & ca-certificates..."
    apt-get update -qq && apt-get install -y --no-install-recommends wget ca-certificates >/dev/null

    # Download Cloud SQL Auth Proxy
    echo "→ Downloading Cloud SQL Auth Proxy..."
    wget -q https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.15.2/cloud-sql-proxy.linux.amd64 -O /cloud_sql_proxy
    chmod +x /cloud_sql_proxy

    # Start proxy in background (using access token)
    echo "→ Starting Cloud SQL Auth Proxy..."
    /cloud_sql_proxy "$GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME" --port=5432 --token="$ACCESS_TOKEN" &
    PROXY_PID=$!

    # Wait for proxy to be ready (up to 30s)
    echo "→ Waiting for proxy to be ready..."
    for i in {1..30}; do
      if bash -c "exec 3<>/dev/tcp/127.0.0.1/5432" 2>/dev/null; then
        echo "  Proxy ready!"
        exec 3<&-
        break
      fi
      sleep 1
    done
    if ! kill -0 $PROXY_PID 2>/dev/null; then
      echo "Error: Proxy failed to start"
      exit 1
    fi

    # ── Dump & Restore ──────────────────────────────────────────────────────────
    echo "→ Dropping & recreating target database \"$TARGET_DB\"..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres <<EOF || true
DROP DATABASE IF EXISTS "$TARGET_DB";
CREATE DATABASE "$TARGET_DB";
EOF

    echo "→ Starting database copy (pg_dump → pg_restore pipe)..."
    PGPASSWORD="$DB_PASSWORD" pg_dump \
      --no-owner --no-privileges --verbose --format=custom \
      -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$SOURCE_DB" \
      | PGPASSWORD="$DB_PASSWORD" pg_restore \
          --verbose --no-owner --no-privileges \
          --clean --if-exists \
          -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TARGET_DB"

    echo ""
    echo "✓ Database copy complete! \"$TARGET_DB\" is now a fresh copy of \"$SOURCE_DB\"."

    # Cleanup
    kill $PROXY_PID 2>/dev/null || true
  '

echo ""
echo "Database copy completed successfully!"
