#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Scans a pre-built Docker image with Trivy for HIGH/CRITICAL CVEs.
# Usage: ./trivy-scan.sh <image>
# Exits non-zero if any findings are found.

set -e

IMAGE="${1:?Usage: trivy-scan.sh <image>}"

echo "Scanning ${IMAGE} with Trivy..."
IGNORE_ARGS=()
if [ -f .trivyignore ]; then
  IGNORE_ARGS=(
    -v "$(pwd)/.trivyignore:/.trivyignore:ro"
  )
fi
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  "${IGNORE_ARGS[@]}" \
  ghcr.io/aquasecurity/trivy:latest image \
  --severity HIGH,CRITICAL \
  --exit-code 1 \
  ${IGNORE_ARGS:+"--ignorefile" "/.trivyignore"} \
  "${IMAGE}"
