# (c) Copyright 2025 by Muczynski
#!/bin/bash

# deploy-graalvm.sh
# Deploys the application to Google Cloud Run using GraalVM-optimized Docker image

# Exit on any error
set -e

# Check if required environment variables are set
if [ -z "$GCP_PROJECT_ID" ]; then
  echo "Error: Missing required environment variable: GCP_PROJECT_ID"
  exit 1
fi

if [ -z "$BRANCH_NAME" ]; then
  echo "Error: Missing required environment variable: BRANCH_NAME"
  exit 1
fi

# Check if required environment variables are set
if [ -z "$BINARY_REPO_NAME" ]; then
  export BINARY_REPO_NAME
  BINARY_REPO_NAME=scrabble-game
  echo "Info: Defaulting BINARY_REPO_NAME environment variable to ${BINARY_REPO_NAME}"
fi

if [ -z "$DB_PASSWORD" ]; then
  echo "Error: Missing required environment variable: DB_PASSWORD"
  exit 1
fi

if [ -z "$GCP_REGION" ]; then
  export GCP_REGION=us-east1
  echo "Warning: GCP_REGION not set. Defaulting to ${GCP_REGION}"
fi

# Extract BASE_SERVICE_NAME from settings.gradle
BASE_SERVICE_NAME=$(grep "rootProject.name" settings.gradle | sed "s/.*rootProject.name = '\(.*\)'.*/\1/")
if [ -z "$BASE_SERVICE_NAME" ]; then
  echo "Error: Could not extract service name from settings.gradle"
  exit 1
fi

# ── Determine names based on branch ────────────────────────────────
# GraalVM deploys to a separate service with -graalvm suffix
if [ "$BRANCH_NAME" = "main" ] || [ "$BRANCH_NAME" = "master" ]; then
  SERVICE_NAME="${BASE_SERVICE_NAME}-graalvm"      # Cloud Run service: library-graalvm
  DB_NAME="$BASE_SERVICE_NAME"                     # Database: library
  IMAGE_NAME="${BASE_SERVICE_NAME}-main-graalvm"   # Docker image: library-main-graalvm
  echo "Info: Deploying GraalVM to production (main branch): service=$SERVICE_NAME, db=$DB_NAME, image=$IMAGE_NAME"
else
  SERVICE_NAME="${BASE_SERVICE_NAME}-graalvm"      # Cloud Run service: library-graalvm (shared)
  DB_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"    # Database: library-featureX
  IMAGE_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}-graalvm" # Docker image: library-featureX-graalvm
  echo "Info: Deploying GraalVM branch '${BRANCH_NAME}': service=$SERVICE_NAME, db=$DB_NAME, image=$IMAGE_NAME"
fi

gcloud config set project "$GCP_PROJECT_ID" --quiet

# Set Cloud SQL instance name
export CLOUD_SQL_INSTANCE_NAME=scrabble-db

# Extract SERVICE_VERSION from build.gradle
SERVICE_VERSION=$(grep "version = " build.gradle | sed "s/.*version = '\(.*\)'.*/\1/")
if [ -z "$SERVICE_VERSION" ]; then
  echo "Error: Could not extract version from build.gradle"
  exit 1
fi

# Append -graalvm to version for identification
SERVICE_VERSION="${SERVICE_VERSION}-graalvm"

# Create Cloud SQL instance if it doesn't exist (smallest tier for cost efficiency)
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

# Build the frontend first
echo "Building React frontend..."
./build-frontend.sh

# Build the Docker image using GraalVM Dockerfile
echo "Building GraalVM Docker image... version $SERVICE_VERSION"
docker build -f Dockerfile-graalvm -t us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/"$IMAGE_NAME":"$SERVICE_VERSION" .

# Push the image to Artifact Registry
echo "Pushing image ${SERVICE_VERSION} to Artifact Registry..."
docker push us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/"$IMAGE_NAME":"$SERVICE_VERSION"

# Deploy to Cloud Run with Cloud SQL connection and all env vars
# GraalVM images can use less memory, so we reduce the memory allocation
echo "Deploying ${SERVICE_VERSION} to Cloud Run in $GCP_REGION..."

# Initialize an empty variable for the service account argument
SERVICE_ACCOUNT_ARG=""

# Check if CLOUD_RUN_SERVICE_ACCOUNT is defined and not empty
if [ -n "$CLOUD_RUN_SERVICE_ACCOUNT" ]; then
  # If defined, construct the service account argument
  SERVICE_ACCOUNT_EMAIL="$CLOUD_RUN_SERVICE_ACCOUNT@${GCP_PROJECT_ID}.iam.gserviceaccount.com"
  SERVICE_ACCOUNT_ARG="--service-account=$SERVICE_ACCOUNT_EMAIL"
  echo "Using service account: $SERVICE_ACCOUNT_EMAIL"
fi

gcloud run deploy "$SERVICE_NAME" \
  --image us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/"$IMAGE_NAME":"$SERVICE_VERSION" \
  --region "$GCP_REGION" \
  --platform managed \
  --allow-unauthenticated \
  --concurrency 10 \
  --min-instances 0 \
  --max-instances 1 \
  --memory 384Mi \
  --cpu 1 \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID,GCP_REGION=$GCP_REGION,DB_NAME=$DB_NAME,DB_PASSWORD=$DB_PASSWORD,SPRING_PROFILES_ACTIVE=prod,APP_ENV=production,APP_EXTERNAL_BASE_URL=https://$SERVICE_NAME.muczynskifamily.com" \
  --add-cloudsql-instances="$GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME" \
  $SERVICE_ACCOUNT_ARG \
  --quiet

# Get the service URL
echo "Deployment ${SERVICE_VERSION} complete! Service URL:"
gcloud run services describe "$SERVICE_NAME" --region "$GCP_REGION" --format 'value(status.url)' --quiet
