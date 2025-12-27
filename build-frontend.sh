#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Build the React frontend and copy to Spring Boot static resources

set -e  # Exit on error

./gradlew clean

echo "================================="
echo "Building React Frontend"
echo "================================="

# Navigate to frontend directory
cd frontend

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
  echo "Installing dependencies..."
  npm install
fi

# Build the production bundle
echo "Building production bundle..."
npm run build

# Navigate back to root
cd ..

# Clear old static files
echo "Clearing old static files..."
rm -rf src/main/resources/static/*

# Copy new build to Spring Boot static resources
echo "Copying build to Spring Boot static resources..."
mkdir src/main/resources/static
cp -r frontend/dist/* src/main/resources/static/

echo "================================="
echo "Frontend build complete!"
echo "================================="
echo ""
echo "Build output copied to: src/main/resources/static/"
echo ""
echo "Next steps:"
echo "  - Run './gradlew bootRun' to start the application"
echo "  - Or run './gradlew build' to create a production JAR"
