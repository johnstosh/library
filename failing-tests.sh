#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Script to run only the test classes that are currently failing

./gradlew test \
  --tests com.muczynski.library.controller.DeletionConflictTest \
  --tests com.muczynski.library.controller.GoogleOAuthControllerTest \
  --tests com.muczynski.library.domain.RandomBookTest
