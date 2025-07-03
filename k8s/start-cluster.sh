#!/bin/bash
set -e

# === Config ===
MASTER_NAME="k8s-master"
WORKER_NAMES=("k8s-worker1" "k8s-worker2")
MASTER_CPUS=2
MASTER_MEM="3G"
MASTER_DISK="10G"
WORKER_CPUS=2
WORKER_MEM="4G"
WORKER_DISK="10G"

K8S_VERSION="1.28.15-00"
POD_NETWORK_CIDR="10.244.0.0/16"
CNI_VERSION="v1.4.0"

# === Step 0: Ensure SSH key and generate cloud-init.yaml ===
if [ ! -f ~/.ssh/id_rsa.pub ]; then
  echo "Generating SSH keypair..."
  ssh-keygen -t rsa -b 4096 -N "" -f ~/.ssh/id_rsa
fi

PUBKEY=$(cat ~/.ssh/id_rsa.pub)

cat > cloud-init.yaml <<EOF
users:
  - name: ubuntu
    ssh-authorized-keys:
      - $PUBKEY
    sudo: ['ALL=(ALL) NOPASSWD:ALL']
    shell: /bin/bash
EOF

# === Helper function: run command on multipass VM ===
run_on_node() {
  local NODE=$1
  shift
  multipass exec "$NODE" -- bash -c "$*"
}

# === Step 1: Launch multipass VMs ===
echo "[Step 1] Launching VMs..."
multipass launch --name "$MASTER_NAME" --cpus $MASTER_CPUS --memory $MASTER_MEM --disk $MASTER_DISK --cloud-init cloud-init.yaml || true

for w in "${WORKER_NAMES[@]}"; do
  multipass launch --name "$w" --cpus $WORKER_CPUS --memory $WORKER_MEM --disk $WORKER_DISK --cloud-init cloud-init.yaml || true
done
echo "VMs launched."

# === Step 2: Setup nodes ===
echo "[Step 2] Setting up nodes..."

setup_node() {
  local NODE=$1
  echo "Setting up $NODE..."

  # Determine architecture for CNI plugin
  ARCH=$(multipass exec "$NODE" -- uname -m)
  if [[ "$ARCH" == "aarch64" ]]; then
    CNI_PLUGINS_TGZ="cni-plugins-linux-arm64-${CNI_VERSION}.tgz"
  else
    CNI_PLUGINS_TGZ="cni-plugins-linux-amd64-${CNI_VERSION}.tgz"
  fi
  CNI_PLUGINS_URL="https://github.com/containernetworking/plugins/releases/download/${CNI_VERSION}/${CNI_PLUGINS_TGZ}"

  run_on_node "$NODE" "
    set -eux;

    sudo apt-get update &&
    sudo apt-get install -y apt-transport-https ca-certificates curl gpg software-properties-common containerd;

    sudo swapoff -a;
    sudo sed -i '/ swap / s/^/#/' /etc/fstab;

    sudo modprobe br_netfilter;
    echo 'br_netfilter' | sudo tee /etc/modules-load.d/k8s.conf;

    sudo tee /etc/sysctl.d/k8s.conf <<EOF
net.bridge.bridge-nf-call-iptables=1
net.bridge.bridge-nf-call-ip6tables=1
net.ipv4.ip_forward=1
EOF
    sudo sysctl --system;

    sudo mkdir -p /etc/containerd;
    containerd config default | sudo tee /etc/containerd/config.toml;
    sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml;
    sudo systemctl restart containerd;
    sudo systemctl enable containerd;

    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.28/deb/Release.key | gpg --dearmor | sudo tee /etc/apt/keyrings/kubernetes-apt-keyring.gpg >/dev/null;
    echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.28/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list;
    sudo apt-get update;
    sudo apt-get install -y kubelet kubeadm kubectl;
    sudo apt-mark hold kubelet kubeadm kubectl;
    sudo systemctl enable kubelet;
    sudo systemctl restart kubelet;

    # CNI plugins check and install if missing
    if [ ! -d /opt/cni/bin ] || [ \$(ls -A /opt/cni/bin | wc -l) -eq 0 ]; then
      echo 'Installing CNI plugins...'
      curl -L \"$CNI_PLUGINS_URL\" -o /tmp/$CNI_PLUGINS_TGZ
      sudo mkdir -p /opt/cni/bin
      sudo tar -C /opt/cni/bin -xzf /tmp/$CNI_PLUGINS_TGZ
      rm /tmp/$CNI_PLUGINS_TGZ
      sudo systemctl restart kubelet
    else
      echo 'CNI plugins already installed.'
    fi
  "
  echo "$NODE setup complete."
}

setup_node "$MASTER_NAME"
for w in "${WORKER_NAMES[@]}"; do
  setup_node "$w"
done

# === Step 3: Initialize Kubernetes on master ===
echo "[Step 3] Initializing Kubernetes master..."
kubeadm_init_output=$(multipass exec $MASTER_NAME -- sudo kubeadm init --pod-network-cidr=$POD_NETWORK_CIDR)

echo "$kubeadm_init_output"

join_command=$(echo "$kubeadm_init_output" | grep -A 2 "kubeadm join" | tail -n +1)

echo "Join command for workers:"
echo "$join_command"

# === Step 4: Setup kubectl on master ===
echo "[Step 4] Setup kubectl on master node..."
run_on_node $MASTER_NAME "
  mkdir -p /home/ubuntu/.kube;
  sudo cp /etc/kubernetes/admin.conf /home/ubuntu/.kube/config;
  sudo chown ubuntu:ubuntu /home/ubuntu/.kube/config;
"

# === Step 5: Join workers to cluster ===
echo "[Step 5] Joining worker nodes to cluster..."
for w in "${WORKER_NAMES[@]}"; do
  echo "Joining $w..."
  multipass exec $w -- sudo bash -c "$join_command --ignore-preflight-errors=all"
done

# === Step 6: Setup kubeconfig locally ===
echo "[Step 6] Copy kubeconfig locally..."
MASTER_IP=$(multipass info $MASTER_NAME | grep IPv4 | awk '{print $2}')
ssh ubuntu@$MASTER_IP 'sudo cat /etc/kubernetes/admin.conf' > ~/.kube/config
chmod 600 ~/.kube/config

# === Step 7: Install CoreDNS and Flannel ===
echo "[Step 7] Installing Flannel network plugin..."
kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/master/Documentation/kube-flannel.yml

echo "Cluster setup is complete. Nodes:"
kubectl get nodes

