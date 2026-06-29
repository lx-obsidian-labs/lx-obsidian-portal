#!/bin/bash
# LX Obsidian Portal — VPS Deployment Script
# Usage: bash deploy.sh <server-ip> <ssh-user>

set -e

APP_DIR="/opt/lx-portal"
REPO_URL="https://github.com/YOUR_USER/YOUR_REPO.git"

if [ $# -lt 2 ]; then
  echo "Usage: bash deploy.sh <server-ip> <ssh-user>"
  echo "Example: bash deploy.sh 192.168.1.100 root"
  exit 1
fi

SERVER_IP=$1
SSH_USER=$2

echo "=== Building and deploying LX Portal ==="

# Build locally
echo "--- Installing dependencies ---"
npm install --production

# Copy to server
echo "--- Deploying to $SSH_USER@$SERVER_IP ---"
rsync -avz --delete \
  --exclude 'node_modules' \
  --exclude '.git' \
  --exclude '.env' \
  --exclude 'lx_portal.db' \
  --exclude '*.log' \
  --exclude 'node_modules' \
  --exclude '__pycache__' \
  --exclude '*.pyc' \
  ./ "$SSH_USER@$SERVER_IP:$APP_DIR"

# Install deps & restart on server
ssh "$SSH_USER@$SERVER_IP" bash << EOF
  cd $APP_DIR
  npm install --production
  
  # Setup systemd service if not exists
  if [ ! -f /etc/systemd/system/lx-portal.service ]; then
    cat > /tmp/lx-portal.service << 'SERVICEEOF'
[Unit]
Description=LX Obsidian Portal
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/node $APP_DIR/server.js
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
SERVICEEOF
    mv /tmp/lx-portal.service /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable lx-portal
  fi
  
  # Restart
  systemctl restart lx-portal
  echo "=== Deployment complete! ==="
  echo "Server running at http://$SERVER_IP:3000"
  echo "Dashboard at http://$SERVER_IP:3000/dashboard"
EOF
