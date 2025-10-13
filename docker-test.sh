#!/bin/bash

# docker-test.sh
# Builds and runs the app Docker image locally, with PostgreSQL setup

# Exit on any error
set -e

# Check if required environment variables are set
if [ -z "$GCP_PROJECT_ID" ]; then
  echo "Error: Missing GCP_PROJECT_ID environment variable."
  exit 1
fi

# Check if required environment variables are set
if [ -z "$BINARY_REPO_NAME" ]; then
  export BINARY_REPO_NAME
  BINARY_REPO_NAME=scrabble-game
  echo "Info: Defaulting BINARY_REPO_NAME environment variable to ${BINARY_REPO_NAME}"
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
echo "Creating Docker network 'app-network'..."
docker network create app-network 2>/dev/null || echo "Docker network 'app-network' already exists."

# Check if port 5432 is in use
echo "Checking if PostgreSQL is running on port 5432..."
if ss -tuln | grep -q ":5432"; then
  echo "Port 5432 is in use. Assuming PostgreSQL is already running. Ensure the database password is set to the expected value and is accessible at localhost:5432."
  echo "If using a local PostgreSQL instance, you may need to update SPRING_DATASOURCE_URL manually or stop the existing database to let this script start a new one."
else
  echo "Port 5432 is free. Starting PostgreSQL container..."
  docker run --rm -d --name local-db \
    --network app-network \
    -p 5432:5432 \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -e POSTGRES_DB=${SERVICE_NAME} \
    postgres:15
  echo "PostgreSQL container started."
fi

# Build the Spring Boot JAR
echo "Building Spring Boot JAR..."
./gradlew clean build -x test

# Build the Docker image with environment variables
echo "Building Docker image..."
docker build \
  --build-arg DB_PASSWORD="$DB_PASSWORD" \
  -t us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/library:latest .

# Run the Docker image locally with environment variables
echo "Running Docker image on http://localhost:8080..."
docker run --rm --name application -p 8080:8080 \
  --network app-network \
  -e PORT=8080 \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://local-db:5432/${SERVICE_NAME}" \
  us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/library:latest
