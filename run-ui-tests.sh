#!/bin/bash

# Script to run Playwright UI tests in the official Playwright Docker image.
# This mounts the project directory and runs Gradle tests for LibraryUITest.
# Assumes Docker is installed and the project is in the current directory.
# The image includes Chromium and necessary dependencies for Playwright Java.

set -e  # Exit on any error

IMAGE="mcr.microsoft.com/playwright:v1.55.0-jammy"

echo "Running UI tests in Playwright Docker image: $IMAGE"

# Run the container: mount current dir as /workspace, run gradle test for UI tests
docker run --rm \
  -v $(pwd):/workspace \
  -w /workspace \
  -e GRADLE_OPTS="-Dorg.gradle.daemon=false" \
  $IMAGE \
  /bin/bash -c "
    apt-get update && apt-get install -y openjdk-17-jdk gradle &&
    cd /workspace &&
    ./gradlew test --tests '*LibraryUITest' --info
  "

echo "UI tests completed."
