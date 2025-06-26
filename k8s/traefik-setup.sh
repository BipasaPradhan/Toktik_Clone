#!/bin/bash
set -e

echo "==> Installing Traefik Ingress Controller..."

# Create namespace
kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -

# Add Helm repo
helm repo add traefik https://helm.traefik.io/traefik || true
helm repo update

# Install Traefik using DaemonSet + MetalLB-compatible LoadBalancer
helm upgrade --install traefik traefik/traefik \
  --namespace traefik \
  --set service.type=LoadBalancer \
  --set replicas=3

echo "Traefik installed with LoadBalancer."
