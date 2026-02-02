# (c) Copyright 2025 by Muczynski
#!/bin/bash

# deploy.sh
# Unified deployment script for Google Cloud Run
# Supports standard JVM, GraalVM native image, and redeploy-only modes

set -e

# ── Usage ─────────────────────────────────────────────────────────
usage() {
  cat <<EOF
Usage: ./deploy.sh <mode>

Modes:
  standard    Build and deploy using standard JVM (Dockerfile, 512Mi memory)
  graalvm     Build and deploy using GraalVM native image (Dockerfile-graalvm, 384Mi memory)
  redeploy    Redeploy the existing image without rebuilding (skip build and push)

Required environment variables:
  GCP_PROJECT_ID              Google Cloud project ID
  BRANCH_NAME                 Git branch name (main or dev)
  DB_PASSWORD                 PostgreSQL database password

Optional environment variables:
  GCP_REGION                  Defaults to us-east1
  BINARY_REPO_NAME            Artifact Registry repo, defaults to scrabble-game
  CLOUD_RUN_SERVICE_ACCOUNT   Service account name (without @... suffix)

Examples:
  ./deploy.sh standard
  ./deploy.sh graalvm
  ./deploy.sh redeploy
EOF
  exit 1
}

# ── Parse arguments ───────────────────────────────────────────────
MODE="${1:-}"
if [ -z "$MODE" ]; then
  usage
fi

case "$MODE" in
  standard|graalvm|redeploy) ;;
  *) echo "Error: Unknown mode '$MODE'"; echo ""; usage ;;
esac

# ── Validate required environment variables ───────────────────────
if [ -z "$GCP_PROJECT_ID" ]; then
  echo "Error: Missing required environment variable: GCP_PROJECT_ID"
  exit 1
fi

if [ -z "$BRANCH_NAME" ]; then
  echo "Error: Missing required environment variable: BRANCH_NAME"
  exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
  echo "Error: Missing required environment variable: DB_PASSWORD"
  exit 1
fi

if [ -z "$BINARY_REPO_NAME" ]; then
  export BINARY_REPO_NAME=scrabble-game
  echo "Info: Defaulting BINARY_REPO_NAME to ${BINARY_REPO_NAME}"
fi

if [ -z "$GCP_REGION" ]; then
  export GCP_REGION=us-east1
  echo "Warning: GCP_REGION not set. Defaulting to ${GCP_REGION}"
fi

# ── Extract names from build files ────────────────────────────────
BASE_SERVICE_NAME=$(grep "rootProject.name" settings.gradle | sed "s/.*rootProject.name = '\(.*\)'.*/\1/")
if [ -z "$BASE_SERVICE_NAME" ]; then
  echo "Error: Could not extract service name from settings.gradle"
  exit 1
fi

SERVICE_VERSION=$(grep "version = " build.gradle | sed "s/.*version = '\(.*\)'.*/\1/")
if [ -z "$SERVICE_VERSION" ]; then
  echo "Error: Could not extract version from build.gradle"
  exit 1
fi

# ── Determine names based on branch ──────────────────────────────
if [ "$BRANCH_NAME" = "main" ] || [ "$BRANCH_NAME" = "master" ]; then
  SERVICE_NAME="$BASE_SERVICE_NAME"
  DB_NAME="$BASE_SERVICE_NAME"
  IMAGE_NAME="${BASE_SERVICE_NAME}-main"
  echo "Info: Deploying to production (main branch): service=$SERVICE_NAME, db=$DB_NAME"
else
  SERVICE_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
  DB_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
  IMAGE_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
  echo "Info: Deploying branch '${BRANCH_NAME}': service=$SERVICE_NAME, db=$DB_NAME"
fi

# ── Mode-specific settings ────────────────────────────────────────
MEMORY="512Mi"
DOCKERFILE="Dockerfile"

if [ "$MODE" = "graalvm" ]; then
  IMAGE_NAME="${IMAGE_NAME}-graalvm"
  SERVICE_VERSION="${SERVICE_VERSION}-graalvm"
  MEMORY="384Mi"
  DOCKERFILE="Dockerfile-graalvm"
fi

IMAGE_TAG="us-east1-docker.pkg.dev/${GCP_PROJECT_ID}/${BINARY_REPO_NAME}/${IMAGE_NAME}:${SERVICE_VERSION}"

echo "Mode: $MODE | Image: $IMAGE_TAG | Memory: $MEMORY"

# ── GCP setup ─────────────────────────────────────────────────────
gcloud config set project "$GCP_PROJECT_ID" --quiet
export CLOUD_SQL_INSTANCE_NAME=scrabble-db

# ── Build and push (skip for redeploy) ────────────────────────────
if [ "$MODE" != "redeploy" ]; then
  # Create Cloud SQL instance if it doesn't exist
  echo "Creating Cloud SQL instance..."
  gcloud sql instances create $CLOUD_SQL_INSTANCE_NAME \
    --database-version=POSTGRES_15 \
    --tier=db-f1-micro \
    --region="$GCP_REGION" \
    --quiet || echo "Instance already exists?"

  # Create the database
  echo "Creating database..."
  gcloud sql databases create ${DB_NAME} --instance=$CLOUD_SQL_INSTANCE_NAME --quiet || echo "Database already exists?"

  # Set the database password
  echo "Setting database password..."
  gcloud sql users set-password postgres \
    --instance=$CLOUD_SQL_INSTANCE_NAME \
    --password="$DB_PASSWORD" \
    --quiet || echo "Don't need to set the password today."

  # Configure Docker to use GCP Artifact Registry
  echo "Configuring Docker for Artifact Registry..."
  gcloud auth configure-docker us-east1-docker.pkg.dev --quiet

  # Build the frontend
  echo "Building React frontend..."
  ./build-frontend.sh

  # Build the Docker image
  echo "Building Docker image... version $SERVICE_VERSION"
  if [ "$MODE" = "graalvm" ]; then
    docker build -f "$DOCKERFILE" -t "$IMAGE_TAG" .
  else
    ./gradlew clean build -x test
    docker build -t "$IMAGE_TAG" .
  fi

  # Push the image with retry loop (up to 20 attempts)
  echo "Pushing image ${SERVICE_VERSION} to Artifact Registry..."
  PUSH_SUCCESS=false
  for attempt in $(seq 1 20); do
    echo "Push attempt $attempt of 20..."
    if docker push "$IMAGE_TAG"; then
      PUSH_SUCCESS=true
      echo "Push succeeded on attempt $attempt."
      break
    else
      echo "Push attempt $attempt failed."
      if [ "$attempt" -lt 20 ]; then
        echo "Retrying..."
      fi
    fi
  done

  if [ "$PUSH_SUCCESS" = false ]; then
    echo "Error: Docker push failed after 20 attempts."
    exit 1
  fi
fi

# ── Deploy to Cloud Run ──────────────────────────────────────────
echo "Deploying ${SERVICE_VERSION} to Cloud Run in $GCP_REGION..."

SERVICE_ACCOUNT_ARG=""
if [ -n "$CLOUD_RUN_SERVICE_ACCOUNT" ]; then
  SERVICE_ACCOUNT_EMAIL="$CLOUD_RUN_SERVICE_ACCOUNT@${GCP_PROJECT_ID}.iam.gserviceaccount.com"
  SERVICE_ACCOUNT_ARG="--service-account=$SERVICE_ACCOUNT_EMAIL"
  echo "Using service account: $SERVICE_ACCOUNT_EMAIL"
fi

gcloud run deploy "$SERVICE_NAME" \
  --image "$IMAGE_TAG" \
  --region "$GCP_REGION" \
  --platform managed \
  --allow-unauthenticated \
  --concurrency 10 \
  --min-instances 0 \
  --max-instances 1 \
  --memory "$MEMORY" \
  --cpu 1 \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID,GCP_REGION=$GCP_REGION,DB_NAME=$DB_NAME,DB_PASSWORD=$DB_PASSWORD,SPRING_PROFILES_ACTIVE=prod,APP_ENV=production,APP_EXTERNAL_BASE_URL=https://$SERVICE_NAME.muczynskifamily.com" \
  --add-cloudsql-instances="$GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME" \
  $SERVICE_ACCOUNT_ARG \
  --quiet

# Get the service URL
echo "Deployment ${SERVICE_VERSION} complete! Service URL:"
gcloud run services describe "$SERVICE_NAME" --region "$GCP_REGION" --format 'value(status.url)' --quiet
