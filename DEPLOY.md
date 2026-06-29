# LX Portal Deployment Guide

## Prerequisites
- A VPS (Ubuntu 22.04+ recommended, 1GB RAM minimum)
- Node.js 18+ installed on the VPS
- Domain name pointing to your VPS IP
- PM2 or systemd for process management

## Quick Deploy

### 1. Prepare the VPS
```bash
ssh root@your-vps-ip
apt update && apt upgrade -y
apt install -y nginx nodejs npm certbot python3-certbot-nginx
```

### 2. Install Node.js 22
```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
apt install -y nodejs
node -v  # Should show v22.x
```

### 3. Clone & Setup
```bash
git clone https://github.com/YOUR_USER/lx-obsidian-portal.git /opt/lx-portal
cd /opt/lx-portal
cp .env.example .env
nano .env  # Set your NVIDIA_API_KEY and JWT_SECRET
npm install --production
```

### 4. Run the Server
```bash
node server.js
```

### 5. Setup PM2 (recommended for production)
```bash
npm install -g pm2
pm2 start server.js --name lx-portal
pm2 save
pm2 startup
```

### 6. Nginx Reverse Proxy (for port 80/443)
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

### 7. SSL with Certbot
```bash
certbot --nginx -d yourdomain.com
```

## Local Dev
```bash
node server.js
# Visit http://localhost:3000
# Dashboard at http://localhost:3000/dashboard
```

## Environment Variables
| Variable | Required | Description |
|----------|----------|-------------|
| `PORT` | No | Server port (default: 3000) |
| `NVIDIA_API_KEY` | No | NVIDIA AI API key for chat |
| `JWT_SECRET` | No | JWT signing secret |
| `CONTACT_EMAIL` | No | Contact form recipient |
