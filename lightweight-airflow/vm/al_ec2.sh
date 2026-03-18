#!/bin/bash
# Bootstrap script for Amazon Linux 2023
# Sets up lightweight Airflow for Onehouse SQL on a single EC2 instance
set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
# Path to the lightweight-airflow directory on this instance.
# Run this script from within that directory, or set AIRFLOW_DIR explicitly.
AIRFLOW_DIR="${AIRFLOW_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"

# Onehouse SQL — set to the internal ELB DNS or IP of the Spark Thrift Server
HIVE_HOST="${HIVE_HOST:-}"  # Set via env var: HIVE_HOST="<internal-elb-dns>" bash al_ec2.sh
HIVE_PORT="${HIVE_PORT:-10000}"
HIVE_USER="${HIVE_USER:-hive}"
HIVE_PASSWORD="${HIVE_PASSWORD:-hive}"

AIRFLOW_USER="${AIRFLOW_USER:-admin}"
AIRFLOW_PASSWORD="${AIRFLOW_PASSWORD:-admin}"
# Generate a random secret key if not provided
AIRFLOW_SECRET_KEY="${AIRFLOW_SECRET_KEY:-$(openssl rand -hex 32)}"
# ──────────────────────────────────────────────────────────────────────────────

echo "==> Installing Docker"
dnf update -y
dnf install -y docker

echo "==> Installing Docker Compose plugin"
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

echo "==> Installing Docker Buildx plugin"
BUILDX_VERSION=$(curl -s https://api.github.com/repos/docker/buildx/releases/latest | grep '"tag_name"' | cut -d'"' -f4)
curl -SL "https://github.com/docker/buildx/releases/download/${BUILDX_VERSION}/buildx-${BUILDX_VERSION}.linux-amd64" \
    -o /usr/local/lib/docker/cli-plugins/docker-buildx
chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx

echo "==> Starting Docker and enabling on boot"
systemctl enable --now docker
usermod -aG docker ec2-user
usermod -aG docker ssm-user

echo "==> Using AIRFLOW_DIR=$AIRFLOW_DIR"

echo "==> Writing .env"
cat > "$AIRFLOW_DIR/.env" <<EOF
AIRFLOW_USER=$AIRFLOW_USER
AIRFLOW_PASSWORD=$AIRFLOW_PASSWORD
AIRFLOW_SECRET_KEY=$AIRFLOW_SECRET_KEY

HIVE_HOST=$HIVE_HOST
HIVE_PORT=$HIVE_PORT
HIVE_USER=$HIVE_USER
HIVE_PASSWORD=$HIVE_PASSWORD
EOF

echo "==> Starting Airflow"
cd "$AIRFLOW_DIR"
mkdir -p logs plugins
chown -R 50000:0 logs plugins
docker compose up --build -d

echo ""
echo "✓ Done. Airflow is starting up."
echo "  UI will be available at http://$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4):8080"
echo "  Login: $AIRFLOW_USER / $AIRFLOW_PASSWORD"
echo ""
echo "  To follow startup logs: docker compose -f $AIRFLOW_DIR/docker-compose.yml logs -f"
