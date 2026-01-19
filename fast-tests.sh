#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Fast tests script - runs all tests except UI tests

# Find all test files excluding UI tests and build the --tests arguments
TEST_ARGS=$(find src/test/java -name "*Test.java" -not -path "*/ui/*" | \
  sed 's|src/test/java/||' | \
  sed 's|\.java$||' | \
  sed 's|/|.|g' | \
  sed 's/^/--tests /' | \
  tr '\n' ' ')

# Run the tests
./gradlew clean
./gradlew test $TEST_ARGS
