#!/bin/bash
# LX Obsidian Portal — Cloudflare Pages Deployment Script
#
# Prerequisites:
#   1. Install wrangler: npm install -g wrangler
#   2. Login: wrangler login
#   3. Create D1 database: npx wrangler d1 create lx-portal
#   4. Update wrangler.toml with your D1 database_id
#   5. Set environment variables in Cloudflare Dashboard:
#      - NVIDIA_API_KEY
#      - JWT_SECRET (generate with: openssl rand -hex 32)
#      - CONTACT_EMAIL
#
# Usage:
#   bash deploy.sh          # Deploy to Cloudflare Pages
#   bash deploy.sh --seed   # Deploy + seed D1 database

set -e

echo "=== LX Obsidian Portal — Cloudflare Deploy ==="

# Install dependencies
echo "--- Installing production dependencies ---"
npm install --production

# Run database migrations
echo "--- Running D1 migrations ---"
npx wrangler d1 execute lx-portal --file=migrations/0001_initial.sql

# Seed database if flag is set
if [ "$1" = "--seed" ]; then
  echo "--- Seeding D1 database ---"
  npx wrangler d1 execute lx-portal --file=migrations/0002_seed.sql
fi

# Deploy to Cloudflare Pages
echo "--- Deploying to Cloudflare Pages ---"
npx wrangler pages deploy . --project-name lx-obsidian-portal

echo "=== Deployment complete! ==="
echo "Visit your site at https://lx-obsidian-portal.pages.dev"
echo ""
echo "To set a custom domain, go to Cloudflare Dashboard > Pages > lx-obsidian-portal > Custom domains"
