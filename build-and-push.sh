#!/bin/bash
set -e

DOCKER_USERNAME="premtsd18"
PLATFORM="linux/amd64"

echo "Logging into DockerHub..."
docker login -u $DOCKER_USERNAME

SERVICES=(
  "config-server"
  "discovery-server"
  "api-gateway"
  "user-service"
  "post-service"
  "connections-service"
  "notification-service"
  "uploader-service"
)

echo "Building and pushing all services..."

for service in "${SERVICES[@]}"; do
  echo ""
  echo "Building $service..."

  docker build \
    --platform $PLATFORM \
    -t $DOCKER_USERNAME/$service:amd64 \
    -t $DOCKER_USERNAME/$service:latest \
    ./$service

  echo "Pushing $service..."
  docker push $DOCKER_USERNAME/$service:amd64
  docker push $DOCKER_USERNAME/$service:latest

  echo "$service done!"
done

echo ""
echo "All images built and pushed!"
echo ""
echo "Verify on DockerHub:"
for service in "${SERVICES[@]}"; do
  echo "  https://hub.docker.com/r/$DOCKER_USERNAME/$service"
done
