# (c) Copyright 2025 by Muczynski
#!/bin/bash

# docker-stop-testing.sh
# Stops and cleans up Docker resources used for local testing of the Scrabble game

# Exit on any error
set -e

# Check if required environment variable is set
if [ -z "$GCP_PROJECT_ID" ]; then
  echo "Error: Missing GCP_PROJECT_ID environment variable."
  exit 1
fi

echo "Stopping and cleaning up Docker resources for local testing..."

# Stop and remove the PostgreSQL container
echo "Stopping and removing PostgreSQL container 'local-db'..."
docker stop local-db 2>/dev/null || echo "No PostgreSQL container 'local-db' found."
docker rm local-db 2>/dev/null || true

# Stop and remove the application container
echo "Stopping and removing application container..."
# Find and stop containers running the application image
CONTAINER_IDS=$(docker ps -q)
if [ -n "$CONTAINER_IDS" ]; then
  docker stop $CONTAINER_IDS
  docker rm $CONTAINER_IDS
else
  echo "No application containers found."
fi

# Remove the Docker network
echo "Removing Docker network 'local-net'..."
docker network rm local-net 2>/dev/null || echo "No Docker network 'local-net' found."
docker image prune -f

echo "Remaining containers:"
docker container ls -a
echo "Cleanup complete."

