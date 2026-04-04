#!/bin/bash
# (c) Copyright 2025 by Muczynski
#
# Deletes build artifacts to free disk space or start fresh.
# Re-run './gradlew bootRun' and 'cd frontend && npm install' to restore.

set -e

echo "Cleaning build artifacts..."

# Gradle build output
if [ -d "./build" ]; then
    echo "  Removing ./build"
    rm -rf ./build
fi

# Frontend npm dependencies (restored by: cd frontend && npm install)
if [ -d "./frontend/node_modules" ]; then
    echo "  Removing ./frontend/node_modules"
    rm -rf ./frontend/node_modules
fi

# Frontend Vite dist output (if present)
if [ -d "./frontend/dist" ]; then
    echo "  Removing ./frontend/dist"
    rm -rf ./frontend/dist
fi

# Maven target output
if [ -d "./target" ]; then
    echo "  Removing ./target"
    rm -rf ./target
fi

echo "Done. To rebuild:"
echo "  Backend:  ./gradlew bootRun"
echo "  Frontend: cd frontend && npm install"
