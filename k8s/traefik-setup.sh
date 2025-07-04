#!/bin/bash
set -e

echo "==> Installing Traefik Ingress Controller..."

kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -

# Add Helm repo
helm repo add traefik https://helm.traefik.io/traefik || true
helm repo update

helm upgrade --install traefik-main traefik/traefik \
  --namespace traefik \
  --set service.type=LoadBalancer \
  --set replicas=1 \
  --set service.name=traefik-main

helm upgrade --install traefik-secondary traefik/traefik \
  --namespace traefik \
  --set service.type=LoadBalancer \
  --set replicas=1 \
  --set service.name=traefik-secondary

echo "Traefik installed with LoadBalancer."
