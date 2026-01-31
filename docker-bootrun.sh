# (c) Copyright 2025 by Muczynski
#!/bin/bash

# docker-bootrun.sh
# Starts a PostgreSQL container for local development with ./gradlew bootRun
# The container uses a Docker volume for data persistence across restarts.

set -e

CONTAINER_NAME="library-dev-db"
DB_NAME="library"
DB_USER="postgres"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
DB_PORT=5432
VOLUME_NAME="library-dev-pgdata"

# Export environment variables for ./gradlew bootRun
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}"
export SPRING_DATASOURCE_USERNAME="${DB_USER}"
export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"

# Check if container is already running
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "PostgreSQL container '${CONTAINER_NAME}' is already running on port ${DB_PORT}."
  echo "Run: ./gradlew bootRun"
  exit 0
fi

# Check if container exists but is stopped
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  echo "Starting existing PostgreSQL container '${CONTAINER_NAME}'..."
  docker start "${CONTAINER_NAME}"
  echo "PostgreSQL is running on port ${DB_PORT}."
  echo "Run: ./gradlew bootRun"
  exit 0
fi

# Check if port is in use
if ss -tuln | grep -q ":${DB_PORT} "; then
  echo "Port ${DB_PORT} is already in use. If PostgreSQL is already running, just run:"
  echo "  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME} SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} ./gradlew bootRun"
  exit 1
fi

echo "Starting new PostgreSQL container '${CONTAINER_NAME}'..."
docker run -d --name "${CONTAINER_NAME}" \
  -p ${DB_PORT}:5432 \
  -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
  -e POSTGRES_DB="${DB_NAME}" \
  -e POSTGRES_USER="${DB_USER}" \
  -v "${VOLUME_NAME}:/var/lib/postgresql/data" \
  postgres:16-alpine

echo "PostgreSQL is running on port ${DB_PORT} (database: ${DB_NAME}, user: ${DB_USER}, password: ${DB_PASSWORD})."
echo "Data is persisted in Docker volume '${VOLUME_NAME}'."
echo ""
echo "Run: ./gradlew bootRun"
echo ""
echo "Or to start database and app together:"
echo "  source ./docker-bootrun.sh && ./gradlew bootRun"
echo ""
echo "To stop:   docker stop ${CONTAINER_NAME}"
echo "To remove: docker rm ${CONTAINER_NAME}"
echo "To reset:  docker rm ${CONTAINER_NAME} && docker volume rm ${VOLUME_NAME}"
