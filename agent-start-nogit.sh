# (c) Copyright 2025 by Muczynski
#!/bin/bash

if [ -z "$XAI_API_KEY" ]; then
  echo "Error: XAI_API_KEY environment variable is not set"
  exit 1
fi

AIDER_CMD="aider"
if ! command -v aider >/dev/null 2>&1; then
    LOCAL_AIDER="$HOME/.local/bin/aider"
    if [ -x "$LOCAL_AIDER" ]; then
        AIDER_CMD="$LOCAL_AIDER"
    else
        echo "Error: aider not found on PATH or in ~/.local/bin"
        exit 1
    fi
fi

$AIDER_CMD --model xai/grok-4-fast-reasoning --no-auto-commits --no-dirty-commits --auto-test  --dark-mode --pretty --no-auto-lint --edit-format diff
# --test-cmd "./gradlew clean test" 
