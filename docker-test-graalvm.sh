# (c) Copyright 2025 by Muczynski
#!/bin/bash

# docker-test-graalvm.sh
# Builds and runs the GraalVM app Docker image locally, with PostgreSQL setup

# Exit on any error
set -e

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

# Check if required environment variables are set
if [ -z "$GCP_PROJECT_ID" ]; then
  echo "Error: Missing GCP_PROJECT_ID environment variable."
  exit 1
fi

# Check if required environment variables are set
if [ -z "$BINARY_REPO_NAME" ]; then
  export BINARY_REPO_NAME
  BINARY_REPO_NAME=scrabble-game
  log "Info: Defaulting BINARY_REPO_NAME environment variable to ${BINARY_REPO_NAME}"
fi


# Extract SERVICE_NAME from settings.gradle
SERVICE_NAME=$(grep "rootProject.name" settings.gradle | sed "s/.*rootProject.name = '\(.*\)'.*/\1/")
if [ -z "$SERVICE_NAME" ]; then
  echo "Error: Could not extract service name from settings.gradle"
  exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
  # Set fixed database password
  export DB_PASSWORD=-insert-password-here-
  echo "Error: Missing DB_PASSWORD environment variable. Using ${DB_PASSWORD}"
fi

# Create a Docker network for the application and database
log "Creating Docker network 'app-network'..."
docker network create app-network 2>/dev/null || log "Docker network 'app-network' already exists."

DB_HOST="local-db"
STARTED_CONTAINER=false

# Check if port 5432 is in use
log "Checking if PostgreSQL is running on port 5432..."
if ss -tuln | grep -q ":5432 "; then
  log "Port 5432 is in use. Assuming PostgreSQL is already running. Ensure the database password is set to the expected value and is accessible at localhost:5432."
  log "If using a local PostgreSQL instance, you may need to update SPRING_DATASOURCE_URL manually or stop the existing database to let this script start a new one."
else
  log "Port 5432 is free. Starting PostgreSQL container..."
  docker run --rm -d --name local-db \
    --network app-network \
    -p 5432:5432 \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -e POSTGRES_DB=${SERVICE_NAME} \
    -e POSTGRES_USER=postgres \
    postgres:15
  log "PostgreSQL container started."
  STARTED_CONTAINER=true
fi

# Build the frontend first
log "Building React frontend..."
./build-frontend.sh

# Build the Spring Boot JAR
log "Building Spring Boot JAR..."
./gradlew clean build -x test

# Build the GraalVM Docker image
log "Building GraalVM Docker image..."
docker build \
  -f Dockerfile-graalvm \
  -t us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/library-graalvm:latest .

# Run the Docker image locally with environment variables
log "Running GraalVM Docker image on http://localhost:8080..."
docker run --rm --name application -p 8080:8080 \
  --network app-network \
  -e PORT=8080 \
  -e GCP_PROJECT_ID="$GCP_PROJECT_ID" \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:5432/${SERVICE_NAME}" \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  -e SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect \
  -e SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION=true \
  -e APP_ENV=staging \
  -e APP_EXTERNAL_BASE_URL=https://8080-cs-913025961294-default.cs-us-central1-pits.cloudshell.dev \
  us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/library-graalvm:latest
