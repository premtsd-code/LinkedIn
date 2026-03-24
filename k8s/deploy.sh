#!/bin/bash
set -e

echo "Deploying LinkedIn to K3s Production..."

# Function to wait for deployment
wait_for_deployment() {
  local name=$1
  local namespace=$2
  local timeout=${3:-5m}
  echo "Waiting for $name..."
  kubectl rollout status deployment/$name \
    -n $namespace --timeout=$timeout || {
    echo "WARNING: $name timed out, continuing..."
    kubectl describe deployment/$name -n $namespace | tail -10
    kubectl logs -l app=$name -n $namespace --tail=20 || true
  }
}

# Function to wait for statefulset
wait_for_statefulset() {
  local name=$1
  local namespace=$2
  local timeout=${3:-5m}
  echo "Waiting for $name..."
  kubectl rollout status statefulset/$name \
    -n $namespace --timeout=$timeout || {
    echo "WARNING: $name timed out, continuing..."
  }
}

echo "Creating namespaces..."
kubectl apply -f k8s/namespace.yaml

echo "Creating configmap and secrets..."
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

echo "Deploying databases..."
kubectl apply -f k8s/databases/postgres.yaml
kubectl apply -f k8s/databases/neo4j.yaml
kubectl apply -f k8s/databases/redis.yaml

echo "Waiting for databases (up to 5 min each)..."
wait_for_statefulset postgres linkedin-prod 5m
wait_for_statefulset redis linkedin-prod 5m

echo "Deploying messaging..."
kubectl apply -f k8s/messaging/kafka.yaml
echo "Waiting 30s for Kafka to initialize..."
sleep 30
kubectl apply -f k8s/messaging/kafbat-ui.yaml

echo "Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/config-server.yaml
wait_for_deployment config-server linkedin-prod 5m

kubectl apply -f k8s/infrastructure/discovery-server.yaml
wait_for_deployment discovery-server linkedin-prod 5m

echo "Deploying microservices..."
kubectl apply -f k8s/microservices/
echo "Waiting for microservices (up to 5 min)..."
wait_for_deployment api-gateway linkedin-prod 5m
wait_for_deployment user-service linkedin-prod 5m
wait_for_deployment post-service linkedin-prod 5m
wait_for_deployment connections-service linkedin-prod 5m
wait_for_deployment notification-service linkedin-prod 5m
wait_for_deployment uploader-service linkedin-prod 5m

echo "Deploying observability..."
kubectl apply -f k8s/observability/

echo "Applying network policies..."
kubectl apply -f k8s/network-policy.yaml

echo ""
echo "Deployment complete!"
echo ""
echo "Pod status:"
kubectl get pods -n linkedin-prod

echo ""
echo "Service ports:"
kubectl get services -n linkedin-prod

echo ""
echo "Health check:"
echo "API Gateway: http://195.201.195.25:32000/actuator/health"
