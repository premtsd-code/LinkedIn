# Prompt for Claude — Phase 5: K3s Production
# Give this entire file to Claude in IntelliJ

---

You are helping me implement Phase 5 of my LinkedIn
microservices project — deploying to K3s Kubernetes
in production.

## Project Context

- Language: Java 17 (Amazon Corretto)
- Framework: Spring Boot 3.2.x
- Build: Maven (multi-module project)
- Package prefix: com.premtsd.linkedin
- Server: Hetzner (i7-8700, 64GB RAM, 2TB NVMe)
- Server IP: 195.201.195.25
- Server user: premtsd
- K3s version: v1.34.5 (already installed)
- DockerHub: premtsd18

## Port Strategy

### Development (Docker Compose) — already running
- Ports: 10000-10999
- All services healthy at http://195.201.195.25:10000

### Production (K3s NodePort) — to implement now
- Ports: 32000-32999
- K3s native NodePort range (no config needed)

## Production Port Map

| Port  | Service                  |
|-------|--------------------------|
| 32000 | api-gateway              |
| 32001 | discovery-server         |
| 32003 | notification-service     |
| 32100 | postgres                 |
| 32101 | neo4j browser            |
| 32102 | neo4j bolt               |
| 32103 | redis                    |
| 32201 | kafbat-ui                |
| 32300 | zipkin                   |
| 32301 | opensearch               |
| 32302 | opensearch-dashboards    |
| 32303 | opensearch-logstash      |

## Domains (premtsd.com)
- api.premtsd.com → api-gateway
- eureka.premtsd.com → discovery-server
- kafka.premtsd.com → kafbat-ui
- zipkin.premtsd.com → zipkin
- logs.premtsd.com → opensearch-dashboards

## Services to Deploy

| Service              | Image                              | Internal Port |
|----------------------|------------------------------------|---------------|
| api-gateway          | premtsd18/api-gateway:amd64        | 8080          |
| config-server        | premtsd18/config-server:amd64      | 8888          |
| discovery-server     | premtsd18/discovery-server:amd64   | 8761          |
| user-service         | premtsd18/user-service:amd64       | 8080          |
| post-service         | premtsd18/post-service:amd64       | 8080          |
| connections-service  | premtsd18/connections-service:amd64| 8080          |
| notification-service | premtsd18/notification-service:amd64| 8080         |
| uploader-service     | premtsd18/uploader-service:amd64   | 8080          |
| postgres             | postgres:15                        | 5432          |
| neo4j                | neo4j:5                            | 7474/7687     |
| redis                | bitnami/redis:latest               | 6379          |
| kafka                | bitnami/kafka:3.7                  | 9092          |
| kafbat-ui            | ghcr.io/kafbat/kafka-ui:latest     | 8080          |
| zipkin               | openzipkin/zipkin                  | 9411          |
| opensearch           | opensearchproject/opensearch:2.17.0| 9200          |
| opensearch-dashboards| opensearchproject/opensearch-dashboards:2.17.0 | 5601 |

---

## K8s Folder Structure to Create

```
k8s/
├── namespace.yaml
├── configmap.yaml
├── secrets.yaml
├── infrastructure/
│   ├── config-server.yaml
│   └── discovery-server.yaml
├── databases/
│   ├── postgres.yaml
│   ├── neo4j.yaml
│   └── redis.yaml
├── messaging/
│   ├── kafka.yaml
│   └── kafbat-ui.yaml
├── microservices/
│   ├── api-gateway.yaml
│   ├── user-service.yaml
│   ├── post-service.yaml
│   ├── connections-service.yaml
│   ├── notification-service.yaml
│   └── uploader-service.yaml
└── observability/
    ├── zipkin.yaml
    ├── opensearch.yaml
    └── opensearch-dashboards.yaml
```

---

## Task 1 — namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: linkedin-prod
  labels:
    app: linkedin
    environment: production
---
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
---
apiVersion: v1
kind: Namespace
metadata:
  name: logging
```

---

## Task 2 — configmap.yaml

Create ConfigMap with all shared configuration:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: linkedin-config
  namespace: linkedin-prod
data:
  SPRING_PROFILES_ACTIVE: "prod"
  EUREKA_URL: "http://discovery-server:8761/eureka"
  KAFKA_SERVERS: "kafka:9092"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  ZIPKIN_URL: "http://zipkin:9411"
  CONFIG_SERVER_URL: "http://config-server:8888"
  OPENSEARCH_HOST: "opensearch"
  OPENSEARCH_PORT: "9200"
  DB_URL: "jdbc:postgresql://postgres:5432/linkedinDB"
  DB_USER: "linkedin_user"
  NEO4J_URI: "bolt://neo4j:7687"
  NEO4J_USER: "neo4j"
```

---

## Task 3 — secrets.yaml

Create Secrets for sensitive data:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: linkedin-secrets
  namespace: linkedin-prod
type: Opaque
stringData:
  DB_PASSWORD: "linkedin_prod_password"
  REDIS_PASSWORD: "redis_prod_secret"
  NEO4J_PASSWORD: "neo4j_prod_password"
  JWT_SECRET: "linkedin-prod-jwt-secret-key-minimum-32-characters-long"
```

---

## Task 4 — Infrastructure Manifests

### infrastructure/config-server.yaml

Every manifest must follow this pattern:
- Deployment with 2 replicas
- Rolling update strategy (maxUnavailable: 0)
- Resource limits
- Liveness + Readiness probes
- Service (ClusterIP for internal, NodePort for external)
- HPA where applicable

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
  namespace: linkedin-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: config-server
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: config-server
    spec:
      containers:
      - name: config-server
        image: premtsd18/config-server:amd64
        ports:
        - containerPort: 8888
        envFrom:
        - configMapRef:
            name: linkedin-config
        - secretRef:
            name: linkedin-secrets
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8888
          initialDelaySeconds: 60
          periodSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8888
          initialDelaySeconds: 40
          periodSeconds: 5
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: config-server
  namespace: linkedin-prod
spec:
  selector:
    app: config-server
  ports:
  - port: 8888
    targetPort: 8888
```

### infrastructure/discovery-server.yaml

Same pattern — NodePort 32001 for external access:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: discovery-server
  namespace: linkedin-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: discovery-server
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: discovery-server
    spec:
      containers:
      - name: discovery-server
        image: premtsd18/discovery-server:amd64
        ports:
        - containerPort: 8761
        envFrom:
        - configMapRef:
            name: linkedin-config
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8761
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8761
          initialDelaySeconds: 40
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: discovery-server
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: discovery-server
  ports:
  - port: 8761
    targetPort: 8761
    nodePort: 32001
```

---

## Task 5 — Database Manifests

### databases/postgres.yaml

Use StatefulSet for databases (not Deployment):

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: linkedin-prod
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: linkedinDB
        - name: POSTGRES_USER
          value: linkedin_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: linkedin-secrets
              key: DB_PASSWORD
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - linkedin_user
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - linkedin_user
          initialDelaySeconds: 10
          periodSeconds: 5
  volumeClaimTemplates:
  - metadata:
      name: postgres-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
    nodePort: 32100
```

### databases/neo4j.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: neo4j
  namespace: linkedin-prod
spec:
  serviceName: neo4j
  replicas: 1
  selector:
    matchLabels:
      app: neo4j
  template:
    metadata:
      labels:
        app: neo4j
    spec:
      containers:
      - name: neo4j
        image: neo4j:5
        ports:
        - containerPort: 7474
          name: browser
        - containerPort: 7687
          name: bolt
        env:
        - name: NEO4J_AUTH
          valueFrom:
            secretKeyRef:
              name: linkedin-secrets
              key: NEO4J_PASSWORD
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        volumeMounts:
        - name: neo4j-data
          mountPath: /data
  volumeClaimTemplates:
  - metadata:
      name: neo4j-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: neo4j
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: neo4j
  ports:
  - name: browser
    port: 7474
    targetPort: 7474
    nodePort: 32101
  - name: bolt
    port: 7687
    targetPort: 7687
    nodePort: 32102
```

### databases/redis.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: linkedin-prod
spec:
  serviceName: redis
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: bitnami/redis:latest
        ports:
        - containerPort: 6379
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: linkedin-secrets
              key: REDIS_PASSWORD
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        volumeMounts:
        - name: redis-data
          mountPath: /bitnami/redis/data
  volumeClaimTemplates:
  - metadata:
      name: redis-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 2Gi
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
    nodePort: 32103
```

---

## Task 6 — Messaging Manifests

### messaging/kafka.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: linkedin-prod
spec:
  serviceName: kafka
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: bitnami/kafka:3.7
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_CFG_NODE_ID
          value: "0"
        - name: KAFKA_CFG_PROCESS_ROLES
          value: "controller,broker"
        - name: KAFKA_CFG_CONTROLLER_QUORUM_VOTERS
          value: "0@kafka:9093"
        - name: KAFKA_CFG_LISTENERS
          value: "PLAINTEXT://:9092,CONTROLLER://:9093"
        - name: KAFKA_CFG_ADVERTISED_LISTENERS
          value: "PLAINTEXT://kafka:9092"
        - name: KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP
          value: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT"
        - name: KAFKA_CFG_CONTROLLER_LISTENER_NAMES
          value: "CONTROLLER"
        - name: KAFKA_CFG_INTER_BROKER_LISTENER_NAME
          value: "PLAINTEXT"
        - name: KAFKA_HEAP_OPTS
          value: "-Xmx512m -Xms512m"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        volumeMounts:
        - name: kafka-data
          mountPath: /bitnami/kafka
  volumeClaimTemplates:
  - metadata:
      name: kafka-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: linkedin-prod
spec:
  selector:
    app: kafka
  ports:
  - port: 9092
    targetPort: 9092
```

### messaging/kafbat-ui.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafbat-ui
  namespace: linkedin-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafbat-ui
  template:
    metadata:
      labels:
        app: kafbat-ui
    spec:
      containers:
      - name: kafbat-ui
        image: ghcr.io/kafbat/kafka-ui:latest
        ports:
        - containerPort: 8080
        env:
        - name: KAFKA_CLUSTERS_0_NAME
          value: linkedin-prod
        - name: KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS
          value: kafka:9092
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: kafbat-ui
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: kafbat-ui
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 32201
```

---

## Task 7 — Microservice Manifests

### Template for ALL microservices:
- replicas: 2 (high availability)
- Rolling update (maxUnavailable: 0)
- Resource limits
- Liveness + Readiness probes at /actuator/health
- HPA (min:2, max:10, CPU:70%)
- envFrom configmap + secrets

### microservices/api-gateway.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: linkedin-prod
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: premtsd18/api-gateway:amd64
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: linkedin-config
        - secretRef:
            name: linkedin-secrets
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 5
          failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: api-gateway
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 32000
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: linkedin-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Create same pattern for ALL these services:

microservices/user-service.yaml
- image: premtsd18/user-service:amd64
- replicas: 2
- port: 8080
- NO nodePort (internal only — accessed via api-gateway)
- HPA min:2 max:10

microservices/post-service.yaml
- image: premtsd18/post-service:amd64
- replicas: 2
- port: 8080
- NO nodePort (internal only)
- HPA min:2 max:10

microservices/connections-service.yaml
- image: premtsd18/connections-service:amd64
- replicas: 2
- port: 8080
- NO nodePort (internal only)
- HPA min:2 max:10

microservices/notification-service.yaml
- image: premtsd18/notification-service:amd64
- replicas: 2
- port: 8080
- nodePort: 32003 (WebSocket needs direct access)
- HPA min:2 max:10

microservices/uploader-service.yaml
- image: premtsd18/uploader-service:amd64
- replicas: 2
- port: 8080
- NO nodePort (internal only)
- HPA min:2 max:10

---

## Task 8 — Observability Manifests

### observability/zipkin.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zipkin
  namespace: linkedin-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zipkin
  template:
    metadata:
      labels:
        app: zipkin
    spec:
      containers:
      - name: zipkin
        image: openzipkin/zipkin
        ports:
        - containerPort: 9411
        env:
        - name: JAVA_OPTS
          value: "-Xms256m -Xmx512m"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: zipkin
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: zipkin
  ports:
  - port: 9411
    targetPort: 9411
    nodePort: 32300
```

### observability/opensearch.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: opensearch
  namespace: linkedin-prod
spec:
  serviceName: opensearch
  replicas: 1
  selector:
    matchLabels:
      app: opensearch
  template:
    metadata:
      labels:
        app: opensearch
    spec:
      initContainers:
      - name: init-permissions
        image: busybox:latest
        command: ['sh', '-c',
          'chown -R 1000:1000 /usr/share/opensearch/data']
        volumeMounts:
        - name: opensearch-data
          mountPath: /usr/share/opensearch/data
      containers:
      - name: opensearch
        image: opensearchproject/opensearch:2.17.0
        ports:
        - containerPort: 9200
        env:
        - name: discovery.type
          value: single-node
        - name: DISABLE_SECURITY_PLUGIN
          value: "true"
        - name: OPENSEARCH_JAVA_OPTS
          value: "-Xms512m -Xmx512m"
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        volumeMounts:
        - name: opensearch-data
          mountPath: /usr/share/opensearch/data
  volumeClaimTemplates:
  - metadata:
      name: opensearch-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: opensearch
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: opensearch
  ports:
  - port: 9200
    targetPort: 9200
    nodePort: 32301
```

### observability/opensearch-dashboards.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: opensearch-dashboards
  namespace: linkedin-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: opensearch-dashboards
  template:
    metadata:
      labels:
        app: opensearch-dashboards
    spec:
      containers:
      - name: opensearch-dashboards
        image: opensearchproject/opensearch-dashboards:2.17.0
        ports:
        - containerPort: 5601
        env:
        - name: OPENSEARCH_HOSTS
          value: "http://opensearch:9200"
        - name: DISABLE_SECURITY_DASHBOARDS_PLUGIN
          value: "true"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: opensearch-dashboards
  namespace: linkedin-prod
spec:
  type: NodePort
  selector:
    app: opensearch-dashboards
  ports:
  - port: 5601
    targetPort: 5601
    nodePort: 32302
```

---

## Task 9 — Network Policies (Zero Trust)

Create network policies so pods can ONLY talk
to what they need:

```yaml
# Only api-gateway can receive external traffic
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-gateway-policy
  namespace: linkedin-prod
spec:
  podSelector:
    matchLabels:
      app: api-gateway
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - {}
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: user-service
  - to:
    - podSelector:
        matchLabels:
          app: post-service
  - to:
    - podSelector:
        matchLabels:
          app: connections-service
  - to:
    - podSelector:
        matchLabels:
          app: notification-service
  - to:
    - podSelector:
        matchLabels:
          app: uploader-service
  - to:
    - podSelector:
        matchLabels:
          app: discovery-server
```

---

## Task 10 — Deploy Script

Create a deploy script `k8s/deploy.sh`:

```bash
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
kubectl rollout status statefulset/postgres \
  -n linkedin-prod --timeout=3m
kubectl rollout status statefulset/redis \
  -n linkedin-prod --timeout=3m

echo "Deploying messaging..."
kubectl apply -f k8s/messaging/kafka.yaml
sleep 30
kubectl apply -f k8s/messaging/kafbat-ui.yaml

echo "Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/config-server.yaml
kubectl rollout status deployment/config-server \
  -n linkedin-prod --timeout=3m

kubectl apply -f k8s/infrastructure/discovery-server.yaml
kubectl rollout status deployment/discovery-server \
  -n linkedin-prod --timeout=3m

echo "Deploying microservices..."
kubectl apply -f k8s/microservices/
kubectl rollout status deployment/api-gateway \
  -n linkedin-prod --timeout=5m
kubectl rollout status deployment/user-service \
  -n linkedin-prod --timeout=5m
kubectl rollout status deployment/post-service \
  -n linkedin-prod --timeout=5m

echo "Deploying observability..."
kubectl apply -f k8s/observability/

echo "Deployment complete!"
kubectl get pods -n linkedin-prod
kubectl get services -n linkedin-prod
```

---

## Task 11 — Update GitHub Actions prod-ci-cd.yml

Update the deploy-prod job to use kubectl:

```yaml
  deploy-prod:
    name: Deploy to Production
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Setup kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBECONFIG }}" \
            > ~/.kube/config
          chmod 600 ~/.kube/config

      - name: Update configmap
        run: |
          kubectl apply -f k8s/configmap.yaml

      - name: Deploy changed services
        run: |
          services=(
            "api-gateway"
            "user-service"
            "post-service"
            "connections-service"
            "notification-service"
            "uploader-service"
            "config-server"
            "discovery-server"
          )
          for service in "${services[@]}"; do
            kubectl set image \
              deployment/$service \
              $service=premtsd18/$service:${{ github.sha }} \
              -n linkedin-prod \
              --ignore-not-found=true
          done

      - name: Wait for rollouts
        run: |
          for service in api-gateway user-service \
            post-service connections-service \
            notification-service uploader-service; do
            kubectl rollout status \
              deployment/$service \
              -n linkedin-prod \
              --timeout=5m || true
          done

      - name: Smoke test production
        run: |
          sleep 30
          curl -f \
            http://${{ secrets.HETZNER_IP }}:32000/actuator/health
          echo "Production deployment successful!"

      - name: Rollback on failure
        if: failure()
        run: |
          for service in api-gateway user-service \
            post-service connections-service \
            notification-service uploader-service; do
            kubectl rollout undo \
              deployment/$service \
              -n linkedin-prod || true
          done
```

---

## Deployment Rules

- Always use StatefulSet for databases (postgres, neo4j, redis, kafka)
- Always use Deployment for stateless services
- Replicas: 1 for infra (config, discovery, kafka)
- Replicas: 2 for microservices (HA)
- Replicas: 1 for observability (zipkin, opensearch)
- Always set resource requests AND limits
- Always add liveness AND readiness probes
- Always use rolling update (maxUnavailable: 0)
- Never expose DB ports externally in production (use ClusterIP)
  EXCEPT for direct DB access during development/debugging
- Use volumeClaimTemplates for persistent storage
- Use envFrom for configmap + secrets (cleaner than env)

---

## After Implementation — How to Deploy

Run these commands on Hetzner server:

```bash
cd ~/personal/linkedin

# Make deploy script executable
chmod +x k8s/deploy.sh

# Run deployment
./k8s/deploy.sh

# Verify everything running
kubectl get pods -n linkedin-prod
kubectl get services -n linkedin-prod

# Check specific pod logs
kubectl logs -f deployment/api-gateway \
  -n linkedin-prod

# Open K9s dashboard
k9s -n linkedin-prod
```

---

## Expected Output After Deployment

```
NAMESPACE      NAME                    READY  STATUS
linkedin-prod  api-gateway-xxx         2/2    Running
linkedin-prod  user-service-xxx        2/2    Running
linkedin-prod  post-service-xxx        2/2    Running
linkedin-prod  connections-service-xxx 2/2    Running
linkedin-prod  notification-service-xxx 2/2   Running
linkedin-prod  uploader-service-xxx    2/2    Running
linkedin-prod  config-server-xxx       1/1    Running
linkedin-prod  discovery-server-xxx    1/1    Running
linkedin-prod  postgres-0              1/1    Running
linkedin-prod  neo4j-0                 1/1    Running
linkedin-prod  redis-0                 1/1    Running
linkedin-prod  kafka-0                 1/1    Running
linkedin-prod  kafbat-ui-xxx           1/1    Running
linkedin-prod  zipkin-xxx              1/1    Running
linkedin-prod  opensearch-0            1/1    Running
linkedin-prod  opensearch-dashboards   1/1    Running
```

## Health Checks After Deploy

```bash
# API Gateway
curl http://195.201.195.25:32000/actuator/health

# Eureka
curl http://195.201.195.25:32001

# Kafka UI
curl http://195.201.195.25:32201

# Zipkin
curl http://195.201.195.25:32300

# OpenSearch
curl http://195.201.195.25:32301

# OpenSearch Dashboards
curl http://195.201.195.25:32302
```

Please implement all tasks above.
Start with Task 1 (namespace.yaml) and work through each task.
Ask me to share any existing files if needed.