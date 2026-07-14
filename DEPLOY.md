# LX Portal — Cloudflare Pages Deployment Guide

## Architecture

```
Static Assets (.html, .css, .js)
    └── Served by Cloudflare CDN
API Routes (functions/api/*.js)
    └── Cloudflare Pages Functions (Edge Workers)
Database (D1)
    └── Cloudflare's serverless SQLite
```

- **Static files**: HTML, CSS, JS, images → served directly by Cloudflare CDN
- **API routes**: All `/api/*` endpoints are handled by Pages Functions in `functions/api/`
- **Database**: Cloudflare D1 (SQLite-compatible), replaces the old `better-sqlite3` SQLite file

## Prerequisites

- [Node.js 18+](https://nodejs.org/)
- [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/) (installed via `npm install`)
- [Cloudflare account](https://dash.cloudflare.com/) with Pages & D1 enabled

## Setup

### 1. Login to Cloudflare

```bash
npx wrangler login
```

### 2. Create the D1 Database

```bash
npx wrangler d1 create lx-portal
```

This creates a D1 database and outputs a `database_id`. Copy this ID.

### 3. Configure wrangler.toml

Open `wrangler.toml` and uncomment/update the D1 binding:

```toml
[[d1_databases]]
binding = "DB"
database_name = "lx-portal"
database_id = "your-database-id-here"  # ← paste the ID from step 2
```

### 4. Run Database Migrations

```bash
# Create all tables
npx wrangler d1 execute lx-portal --file=migrations/0001_initial.sql

# Seed with sample data
npx wrangler d1 execute lx-portal --file=migrations/0002_seed.sql
```

### 5. Set Environment Variables

In the Cloudflare Dashboard → Pages → `lx-obsidian-portal` → **Settings** → **Environment variables** (production):

| Variable | Description |
|----------|-------------|
| `NVIDIA_API_KEY` | NVIDIA AI API key for chat (get at https://build.nvidia.com) |
| `JWT_SECRET` | JWT signing secret (generate: `openssl rand -hex 32`) |
| `CONTACT_EMAIL` | Email to receive contact form submissions |

## Local Development

### With Wrangler (recommended — uses D1 + Pages Functions)

```bash
npm run dev
```

This starts a local dev server at `http://localhost:8788` with live reload. It uses the Pages Functions and a local D1 simulation.

### With Node.js (legacy Express server)

```bash
npm start
```

This runs the old Express server at `http://localhost:3000` with SQLite. Only for local testing; the Express server is not used in production.

## Deploy

### One-command deploy

```bash
npm run deploy
```

### Full deploy with database setup

```bash
bash deploy.sh
```

### Automatic deploys (Git integration)

Connect your GitHub/GitLab repo in Cloudflare Dashboard → Pages → Create a project → Connect your repository. Cloudflare will auto-deploy on every push.

## Important Notes

- **`server.js` and `routes/`**: These are the legacy Express.js backend. The Cloudflare deployment uses Pages Functions (`functions/api/`) instead. Keep `server.js` only for local testing.
- **`db/`**: Contains the old SQLite database (`better-sqlite3`). Cloudflare uses D1. The schema is replicated in `migrations/0001_initial.sql`.
- **No build step**: The site is fully static HTML/CSS/JS. No bundler or build tool is needed.
- **Service Worker (`sw.js`)**: Used for offline caching. Works as-is on Cloudflare Pages.

## Troubleshooting

- **D1 binding errors**: Ensure `wrangler.toml` has the correct `database_id` and the binding name is `DB`.
- **CORS errors**: The `_middleware.js` file handles CORS for all API routes.
- **Missing environment variables**: Set them in the Cloudflare Dashboard under Pages → Settings → Environment variables.
