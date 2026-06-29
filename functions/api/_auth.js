import { SignJWT, jwtVerify } from 'jose';
import bcrypt from 'bcryptjs';

const encoder = new TextEncoder();

function getSecret(context) {
  return (context && context.env && context.env.JWT_SECRET) || 'lx-obsidian-dev-secret-key-2024';
}

export async function createToken(user, secret) {
  const key = encoder.encode(secret);
  return await new SignJWT({
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role || 'customer'
  })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime('30d')
    .sign(key);
}

export async function verifyToken(token, secret) {
  try {
    const key = encoder.encode(secret);
    const { payload } = await jwtVerify(token, key);
    return payload;
  } catch {
    return null;
  }
}

export function getSecretFrom(context) {
  return getSecret(context);
}

export async function requireAuth(request, secret) {
  const header = request.headers.get('Authorization');
  if (!header || !header.startsWith('Bearer ')) return null;
  return await verifyToken(header.split(' ')[1], secret);
}

export async function hashPassword(password) {
  return bcrypt.hashSync(password, 10);
}

export async function comparePassword(password, hash) {
  return bcrypt.compareSync(password, hash);
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
    return new Response(null, {
      status: 204,
      headers: corsHeaders()
    });
  }
  return null;
}
