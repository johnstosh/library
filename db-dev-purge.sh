#!/bin/bash
# (c) Copyright 2025 by Muczynski
# db-dev-purge.sh
# Purges (drops and recreates) the 'library-dev' database on Google Cloud SQL PostgreSQL
# Runs in a Docker container with Cloud SQL Auth Proxy for secure connection

set -euo pipefail

# ── Configuration ───────────────────────────────────────────────────────────────
# Reuse variables from your environment (matching deploy.sh)
: "${GCP_PROJECT_ID:?Error: Missing required environment variable: GCP_PROJECT_ID}"
: "${DB_PASSWORD:?Error: Missing required environment variable: DB_PASSWORD}"
: "${GCP_REGION:=us-east1}"  # Default like deploy.sh

# Set Cloud SQL instance name (matching deploy.sh)
CLOUD_SQL_INSTANCE_NAME=scrabble-db

# Database to purge
TARGET_DB="library-dev"

# Docker image with postgres tools (psql)
DOCKER_IMAGE="postgres:16"

# ── Safety checks ───────────────────────────────────────────────────────────────
echo "WARNING: This script will PERMANENTLY DELETE all data in '$TARGET_DB'!"
echo ""
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
# 4. Drops and recreates the database

echo "Starting Docker container to purge the database..."

docker run --rm -it \
  --user root \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e GCP_REGION="$GCP_REGION" \
  -e ACCESS_TOKEN="$ACCESS_TOKEN" \
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

    # ── Drop & Recreate ──────────────────────────────────────────────────────────
    echo "→ Dropping & recreating database \"$TARGET_DB\"..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres <<EOF
DROP DATABASE IF EXISTS "$TARGET_DB";
CREATE DATABASE "$TARGET_DB";
EOF

    echo ""
    echo "✓ Database \"$TARGET_DB\" has been purged and recreated as empty."

    # Cleanup
    kill $PROXY_PID 2>/dev/null || true
  '

echo ""
echo "Database purge completed successfully!"
