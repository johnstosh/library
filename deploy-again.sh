#!/bin/bash
# Temporary script: re-deploys the EXISTING image (skips build/push)
# to verify the gcloud run deploy command works without --max-request-body-size
set -e

# Reuse env setup from deploy.sh
if [ -z "$GCP_PROJECT_ID" ]; then echo "Error: GCP_PROJECT_ID not set"; exit 1; fi
if [ -z "$BRANCH_NAME" ]; then echo "Error: BRANCH_NAME not set"; exit 1; fi
if [ -z "$DB_PASSWORD" ]; then echo "Error: DB_PASSWORD not set"; exit 1; fi
if [ -z "$BINARY_REPO_NAME" ]; then export BINARY_REPO_NAME=scrabble-game; fi
if [ -z "$GCP_REGION" ]; then export GCP_REGION=us-east1; fi

BASE_SERVICE_NAME=$(grep "rootProject.name" settings.gradle | sed "s/.*rootProject.name = '\(.*\)'.*/\1/")
if [ "$BRANCH_NAME" = "main" ] || [ "$BRANCH_NAME" = "master" ]; then
  SERVICE_NAME="$BASE_SERVICE_NAME"
  DB_NAME="$BASE_SERVICE_NAME"
  IMAGE_NAME="${BASE_SERVICE_NAME}-main"
else
  SERVICE_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
  DB_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
  IMAGE_NAME="${BASE_SERVICE_NAME}-${BRANCH_NAME}"
fi

SERVICE_VERSION=$(grep "version = " build.gradle | sed "s/.*version = '\(.*\)'.*/\1/")
CLOUD_SQL_INSTANCE_NAME=scrabble-db

gcloud config set project "$GCP_PROJECT_ID" --quiet

SERVICE_ACCOUNT_ARG=""
if [ -n "$CLOUD_RUN_SERVICE_ACCOUNT" ]; then
  SERVICE_ACCOUNT_EMAIL="$CLOUD_RUN_SERVICE_ACCOUNT@${GCP_PROJECT_ID}.iam.gserviceaccount.com"
  SERVICE_ACCOUNT_ARG="--service-account=$SERVICE_ACCOUNT_EMAIL"
  echo "Using service account: $SERVICE_ACCOUNT_EMAIL"
fi

echo "=== Debug deploy: redeploying existing image $IMAGE_NAME:$SERVICE_VERSION ==="
echo "Service: $SERVICE_NAME | Region: $GCP_REGION"

gcloud run deploy "$SERVICE_NAME" \
  --image us-east1-docker.pkg.dev/"$GCP_PROJECT_ID"/${BINARY_REPO_NAME}/"$IMAGE_NAME":"$SERVICE_VERSION" \
  --region "$GCP_REGION" \
  --platform managed \
  --allow-unauthenticated \
  --concurrency 10 \
  --min-instances 0 \
  --max-instances 1 \
  --memory 512Mi \
  --cpu 1 \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID,GCP_REGION=$GCP_REGION,DB_NAME=$DB_NAME,DB_PASSWORD=$DB_PASSWORD,SPRING_PROFILES_ACTIVE=prod,APP_ENV=production,APP_EXTERNAL_BASE_URL=https://$SERVICE_NAME.muczynskifamily.com" \
  --add-cloudsql-instances="$GCP_PROJECT_ID:$GCP_REGION:$CLOUD_SQL_INSTANCE_NAME" \
  $SERVICE_ACCOUNT_ARG \
  --quiet

echo ""
echo "=== Deploy succeeded! Service URL: ==="
gcloud run services describe "$SERVICE_NAME" --region "$GCP_REGION" --format 'value(status.url)' --quiet
