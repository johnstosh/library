# (c) Copyright 2025 by Muczynski
#!/bin/bash

# docker-test-prod.sh
# Builds and runs the app Docker image locally, connected to the production
# Cloud SQL database (the "library" database on instance scrabble-db).
# Does not start a local PostgreSQL container.
#
# The container uses the prod profile. Hibernate ddl-auto stays "update", and
# the essay and alternate-title checks run, the same as a Cloud Run cold start.
# Those checks take an exclusive lock on the book table until they finish.
#
# Required environment variables:
#   GCP_PROJECT_ID    Google Cloud project ID
#   DB_PASSWORD       PostgreSQL password for the postgres user
#
# Optional:
#   GCP_REGION        Defaults to us-east1
#   BINARY_REPO_NAME  Artifact Registry repo, defaults to scrabble-game

# Exit on any error
set -euo pipefail

# Parse command line arguments
QUIET=false
while [[ $# -gt 0 ]]; do
  case $1 in
    -quiet|--quiet)
      QUIET=true
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [-quiet]"
      exit 1
      ;;
  esac
done

# Helper function for echo that respects quiet mode
log() {
  if [ "$QUIET" = false ]; then
    echo "$@"
  fi
}

if [ -z "${GCP_PROJECT_ID:-}" ]; then
  echo "Error: Missing GCP_PROJECT_ID environment variable."
  exit 1
fi

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "Error: Missing DB_PASSWORD environment variable."
  exit 1
fi

if [ -z "${BINARY_REPO_NAME:-}" ]; then
  BINARY_REPO_NAME=scrabble-game
  log "Info: Defaulting BINARY_REPO_NAME environment variable to ${BINARY_REPO_NAME}"
fi

if [ -z "${GCP_REGION:-}" ]; then
  GCP_REGION=us-east1
  log "Info: Defaulting GCP_REGION environment variable to ${GCP_REGION}"
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "Error: gcloud is required to authenticate the Cloud SQL Auth Proxy."
  exit 1
fi

# Extract SERVICE_NAME from settings.gradle
SERVICE_NAME=$(grep "rootProject.name" settings.gradle | sed "s/.*rootProject.name = '\(.*\)'.*/\1/")
if [ -z "$SERVICE_NAME" ]; then
  echo "Error: Could not extract service name from settings.gradle"
  exit 1
fi

# Production database name matches deploy.sh for the main branch.
DB_NAME="$SERVICE_NAME"
CLOUD_SQL_INSTANCE_NAME=scrabble-db
INSTANCE_CONNECTION_NAME="${GCP_PROJECT_ID}:${GCP_REGION}:${CLOUD_SQL_INSTANCE_NAME}"
PROXY_NAME=cloud-sql-proxy
PROXY_IMAGE=gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.15.2
IMAGE_TAG="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/${BINARY_REPO_NAME}/library:latest"

if ss -tuln | grep -q ":8080 "; then
  echo "Error: Port 8080 is already in use."
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -qx application; then
  echo "Error: A container named 'application' already exists. Stop it before running this script."
  exit 1
fi

cleanup() {
  docker stop "$PROXY_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

log "Creating Docker network 'app-network'..."
docker network create app-network 2>/dev/null || log "Docker network 'app-network' already exists."

# Build the frontend first
log "Building React frontend..."
./build-frontend.sh

# Build the Spring Boot JAR
log "Building Spring Boot JAR..."
./gradlew clean build -x test

# Build the Docker image
log "Building Docker image..."
docker build \
  -t "$IMAGE_TAG" .

log "Fetching a Cloud SQL access token..."
ACCESS_TOKEN=$(gcloud auth print-access-token)
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: gcloud auth print-access-token returned an empty token."
  exit 1
fi

log "Starting Cloud SQL Auth Proxy for ${INSTANCE_CONNECTION_NAME}..."
docker rm -f "$PROXY_NAME" >/dev/null 2>&1 || true
docker run -d --rm --name "$PROXY_NAME" \
  --network app-network \
  "$PROXY_IMAGE" \
  --address 0.0.0.0 \
  --port 5432 \
  --token "$ACCESS_TOKEN" \
  "$INSTANCE_CONNECTION_NAME" >/dev/null
unset ACCESS_TOKEN

log "Waiting for the Cloud SQL Auth Proxy..."
PROXY_READY=false
for _ in $(seq 1 40); do
  if docker logs "$PROXY_NAME" 2>&1 | grep -q "ready for new connections"; then
    PROXY_READY=true
    break
  fi
  if ! docker ps --format '{{.Names}}' | grep -qx "$PROXY_NAME"; then
    echo "Error: Cloud SQL Auth Proxy exited."
    docker logs "$PROXY_NAME" 2>&1 | tail -40
    exit 1
  fi
  sleep 1
done
if [ "$PROXY_READY" != true ]; then
  echo "Error: Cloud SQL Auth Proxy did not become ready."
  docker logs "$PROXY_NAME" 2>&1 | tail -40
  exit 1
fi

# The prod profile opens a Cloud Run unix socket. This process has no socket
# mount, so point the same profile at the proxy over the Docker network.
# sslmode=disable is correct: the proxy, not the JDBC driver, encrypts the
# Cloud SQL connection.
log "Running Docker image on http://localhost:8080 against production database ${DB_NAME}..."
log "Startup takes an exclusive lock on the book table, the same as a Cloud Run cold start."
docker run --rm --name application -p 8080:8080 \
  --network app-network \
  -e PORT=8080 \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e GCP_REGION="$GCP_REGION" \
  -e DB_NAME="$DB_NAME" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://${PROXY_NAME}:5432/${DB_NAME}?sslmode=disable" \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  -e APP_ENV=production \
  -e APP_EXTERNAL_BASE_URL=http://localhost:8080 \
  "$IMAGE_TAG"
