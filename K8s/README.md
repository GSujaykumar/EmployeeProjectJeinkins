# Kubernetes manifests — employee-management

Same stack as `docker-compose.yml`, running on Kubernetes.

## Folder layout

| File | Purpose |
|------|---------|
| `namespace.yaml` | Isolated namespace `employee-management` |
| `mysql-*.yaml` | MySQL + Secret + PVC |
| `redis-*.yml` | Redis |
| `employee-app-*.yaml` | Spring Boot API |
| `zipkin-*.yaml` | Tracing |
| `prometheus-*.yaml` | Metrics (ConfigMap + Deployment + Service) |
| `grafana-*.yaml` | Dashboards |

## Prerequisites

1. **kubectl** + **Minikube** (or Docker Desktop Kubernetes)
2. Docker image built locally:

```powershell
cd springboot-crud-project
mvn clean package -DskipTests
docker build -t employee-management:1.0 .
```

3. Load image into Minikube (required for local image):

```powershell
minikube image load employee-management:1.0
```

## Deploy (order matters)

```powershell
cd K8s
kubectl apply -f namespace.yaml
kubectl apply -f mysql-secret.yaml
kubectl apply -f mysql-pvc.yaml
kubectl apply -f mysql-deployment.yaml
kubectl apply -f mysql-service.yaml
kubectl apply -f redis-deployment.yml
kubectl apply -f redis-service.yml
kubectl apply -f zipkin-deployment.yaml
kubectl apply -f zipkin-service.yaml
kubectl wait --for=condition=available deployment/mysql -n employee-management --timeout=120s
kubectl apply -f employee-app-deployment.yaml
kubectl apply -f employee-app-service.yaml
kubectl apply -f prometheus-configmap.yaml
kubectl apply -f prometheus-deployment.yaml
kubectl apply -f prometheus-service.yaml
kubectl apply -f grafana-configmap.yaml
kubectl apply -f grafana-deployment.yaml
kubectl apply -f grafana-service.yaml
```

Or apply everything at once (after namespace):

```powershell
kubectl apply -f namespace.yaml
kubectl apply -f .
```

## Check status

```powershell
kubectl get all -n employee-management
kubectl logs -f deployment/employee-app -n employee-management
```

## Access URLs (Minikube)

```powershell
minikube service employee-app -n employee-management --url
minikube service prometheus -n employee-management --url
minikube service grafana -n employee-management --url
minikube service zipkin -n employee-management --url
```

Or use NodePorts directly: **30080** (app), **30090** (Prometheus), **30300** (Grafana), **30411** (Zipkin).

API auth: `user` / `User123`

## Service DNS (inside cluster)

| Hostname | Port | Used by |
|----------|------|---------|
| `mysql` | 3306 | Spring Boot JDBC |
| `redis` | 6379 | Spring Boot cache |
| `zipkin` | 9411 | Tracing |
| `employee-app` | 8089 | Prometheus scrape |
| `prometheus` | 9090 | Grafana |

## Delete everything

```powershell
kubectl delete namespace employee-management
```
