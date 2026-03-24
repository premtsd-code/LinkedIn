#!/bin/bash
set -e

echo "Deploying LinkedIn to K3s Production..."

echo "Creating namespaces..."
kubectl apply -f k8s/namespace.yaml

echo "Creating configmap and secrets..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

echo "Deploying databases..."
kubectl apply -f k8s/databases/postgres.yaml
kubectl apply -f k8s/databases/neo4j.yaml
kubectl apply -f k8s/databases/redis.yaml

echo "Waiting for databases..."
kubectl rollout status statefulset/postgres -n linkedin-prod --timeout=3m
kubectl rollout status statefulset/redis -n linkedin-prod --timeout=3m

echo "Deploying messaging..."
kubectl apply -f k8s/messaging/kafka.yaml
sleep 30
kubectl apply -f k8s/messaging/kafbat-ui.yaml

echo "Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/config-server.yaml
kubectl rollout status deployment/config-server -n linkedin-prod --timeout=3m

kubectl apply -f k8s/infrastructure/discovery-server.yaml
kubectl rollout status deployment/discovery-server -n linkedin-prod --timeout=3m

echo "Deploying microservices..."
kubectl apply -f k8s/microservices/
kubectl rollout status deployment/api-gateway -n linkedin-prod --timeout=5m
kubectl rollout status deployment/user-service -n linkedin-prod --timeout=5m
kubectl rollout status deployment/post-service -n linkedin-prod --timeout=5m

echo "Deploying observability..."
kubectl apply -f k8s/observability/

echo "Applying network policies..."
kubectl apply -f k8s/network-policy.yaml

echo ""
echo "Deployment complete!"
echo ""
kubectl get pods -n linkedin-prod
echo ""
kubectl get services -n linkedin-prod
