#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Development script - runs frontend dev server and backend together

set -e  # Exit on error

echo "================================="
echo "Library Management System"
echo "Development Mode"
echo "================================="
echo ""
echo "This script runs:"
echo "  - React dev server on http://localhost:5173"
echo "  - Spring Boot API on http://localhost:8080"
echo ""
echo "The React dev server will proxy API requests to Spring Boot."
echo ""

# Check if frontend dependencies are installed
if [ ! -d "frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  cd frontend
  npm install
  cd ..
fi

echo "Starting Spring Boot backend..."
echo "(API will be available at http://localhost:8080/api)"
echo ""

# Start Spring Boot in the background
./gradlew bootRun &
BACKEND_PID=$!

# Wait a bit for backend to start
sleep 5

echo ""
echo "Starting React dev server..."
echo "(Frontend will be available at http://localhost:5173)"
echo ""

# Start frontend dev server
cd frontend
npm run dev &
FRONTEND_PID=$!

echo ""
echo "================================="
echo "Development servers started!"
echo "================================="
echo ""
echo "Frontend: http://localhost:5173"
echo "Backend:  http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop both servers"
echo ""

# Wait for user interrupt
trap "echo 'Stopping servers...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT

# Wait for both processes
wait
