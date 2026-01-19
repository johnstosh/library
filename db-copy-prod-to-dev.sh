#!/bin/bash
# db-copy-prod-to-dev.sh
# Orchestrates copying 'library' → 'library-dev' inside a Docker container
# Downloads & runs Cloud SQL Auth Proxy inside the container for secure connection
# Matches variable names & style from deploy.sh (reuses GCP_PROJECT_ID, DB_PASSWORD, GCP_REGION, etc.)
# Updated to install wget & dependencies inside container
# Uses access token for auth (workaround for Cloud Shell where ADC JSON isn't local)

set -euo pipefail

# ── Configuration ───────────────────────────────────────────────────────────────
# Reuse variables from your environment (matching deploy.sh)
: "${GCP_PROJECT_ID:?Error: Missing required environment variable: GCP_PROJECT_ID}"
: "${DB_PASSWORD:?Error: Missing required environment variable: DB_PASSWORD}"
: "${GCP_REGION:=us-east1}"  # Default like deploy.sh

# Set Cloud SQL instance name (matching deploy.sh)
CLOUD_SQL_INSTANCE_NAME=scrabble-db

# Construct full Cloud SQL connection name (as used in deploy.sh)
CLOUD_SQL_INSTANCE="$GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME"

# Docker image with postgres tools (pg_dump, pg_restore, psql)
DOCKER_IMAGE="postgres:16"

gcloud config set project "$GCP_PROJECT_ID" --quiet
# Get access token on host (for Cloud Shell compatibility)
echo "→ Fetching access token..."
ACCESS_TOKEN=$(gcloud auth print-access-token)
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: Failed to get access token. Ensure you're authenticated with gcloud."
  exit 1
fi

# ── Run in Docker ──────────────────────────────────────────────────────────────
# This runs a temporary container that:
# 1. Installs wget & ca-certificates (for HTTPS)
# 2. Downloads the Cloud SQL Proxy binary
# 3. Starts the proxy in background with --token
# 4. Runs the dump-restore.sh script (mount it from host)

echo "Starting Docker container to perform the database copy..."

docker run --rm -it \
  --user root \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e GCP_REGION="$GCP_REGION" \
  -e ACCESS_TOKEN="$ACCESS_TOKEN" \
  -v "$(pwd)/dump-restore.sh:/dump-restore.sh:ro" \
  --network host \
  "$DOCKER_IMAGE" bash -c '
    set -euo pipefail

    # Install wget & ca-certificates (needed for HTTPS download)
    echo "→ Installing wget & ca-certificates..."
    apt-get update -qq && apt-get install -y --no-install-recommends wget ca-certificates

    # Download Cloud SQL Auth Proxy (latest version as of 2026)
    echo "→ Downloading Cloud SQL Auth Proxy..."
    wget https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.15.2/cloud-sql-proxy.linux.amd64 -O /cloud_sql_proxy
    chmod +x /cloud_sql_proxy

    # Start proxy in background (using access token)
    echo "→ Starting Cloud SQL Auth Proxy..."
    /cloud_sql_proxy "$GCP_PROJECT_ID:$GCP_REGION:scrabble-db" --port=5432 --token="$ACCESS_TOKEN" &
    PROXY_PID=$!

    # Wait for proxy to be ready (up to 30s) using bash built-in
    for i in {1..30}; do
      if bash -c "exec 3<>/dev/tcp/127.0.0.1/5432" 2>/dev/null; then
        echo "Proxy ready!"
        exec 3<&-  # Close the file descriptor
        break
      fi
      sleep 1
    done
    if ! kill -0 $PROXY_PID 2>/dev/null; then
      echo "Error: Proxy failed to start"
      exit 1
    fi

    # Run the dump-restore script
    bash /dump-restore.sh

    # Cleanup
    kill $PROXY_PID
  '

echo "Database copy completed!"
