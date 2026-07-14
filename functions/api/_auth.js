// Cloudflare Pages Functions — zero npm dependencies
// Uses native Web Crypto API (available in all Workers/Pages environments)

const encoder = new TextEncoder();
const decoder = new TextDecoder();

/* ---- Base64URL helpers ---- */

function base64url(input) {
  const str = typeof input === 'string' ? input : btoa(String.fromCharCode(...new Uint8Array(input)));
  return str.replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

function base64urlDecode(str) {
  const padded = str.replace(/-/g, '+').replace(/_/g, '/');
  const bin = atob(padded);
  return new Uint8Array([...bin].map(c => c.charCodeAt(0)));
}

/* ---- HMAC key from secret ---- */

async function getKey(secret) {
  return crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign', 'verify']
  );
}

/* ---- JWT Create ---- */

export async function createToken(user, secret) {
  const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = base64url(JSON.stringify({
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role || 'customer',
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 30 * 24 * 60 * 60
  }));
  const key = await getKey(secret);
  const sig = await crypto.subtle.sign('HMAC', key, encoder.encode(header + '.' + payload));
  return header + '.' + payload + '.' + base64url(sig);
}

/* ---- JWT Verify ---- */

export async function verifyToken(token, secret) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const key = await getKey(secret);
    const valid = await crypto.subtle.verify(
      'HMAC', key, base64urlDecode(parts[2]),
      encoder.encode(parts[0] + '.' + parts[1])
    );
    if (!valid) return null;
    const payload = JSON.parse(decoder.decode(base64urlDecode(parts[1])));
    if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) return null;
    return payload;
  } catch {
    return null;
  }
}

/* ---- Password hashing via PBKDF2-SHA256 ---- */

export async function hashPassword(password) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const key = await crypto.subtle.importKey('raw', encoder.encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', salt, iterations: 100000, hash: 'SHA-256' },
    key, 256
  );
  return base64url(salt) + '.' + base64url(bits);
}

export async function comparePassword(password, stored) {
  try {
    const [saltStr, hashStr] = stored.split('.');
    const salt = base64urlDecode(saltStr);
    const key = await crypto.subtle.importKey('raw', encoder.encode(password), 'PBKDF2', false, ['deriveBits']);
    const bits = await crypto.subtle.deriveBits(
      { name: 'PBKDF2', salt, iterations: 100000, hash: 'SHA-256' },
      key, 256
    );
    return base64url(bits) === hashStr;
  } catch {
    return false;
  }
}

/* ---- Helpers ---- */

function getSecret(context) {
  return (context && context.env && context.env.JWT_SECRET) || 'lx-obsidian-dev-secret-key-2024';
}

export function getSecretFrom(context) {
  return getSecret(context);
}

export async function requireAuth(request, secret) {
  const header = request.headers.get('Authorization');
  if (!header || !header.startsWith('Bearer ')) return null;
  return await verifyToken(header.split(' ')[1], secret);
}

export function uuid() {
  return crypto.randomUUID();
}

export function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    }
  });
}

export function error(msg, status = 400) {
  return json({ error: msg }, status);
}

export function corsHeaders(method = 'GET, POST, PUT, DELETE, OPTIONS') {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': method,
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  };
}

export function handleOptions(request) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: corsHeaders() });
  }
  return null;
}
