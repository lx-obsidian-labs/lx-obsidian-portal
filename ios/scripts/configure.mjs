import { readFile, writeFile } from 'node:fs/promises';
import { parseEnv } from 'node:util';

const root = new URL('../../', import.meta.url);
async function optional(path) {
  try { return await readFile(new URL(path, root), 'utf8'); }
  catch (error) { if (error.code === 'ENOENT') return ''; throw error; }
}
const dotenv = parseEnv(await optional('.env'));
const gradle = Object.fromEntries((await optional('gradle.properties')).split(/\r?\n/)
  .filter(line => /^\s*[A-Z_]+\s*=/.test(line))
  .map(line => { const split = line.indexOf('='); return [line.slice(0, split).trim(), line.slice(split + 1).trim()]; }));
function setting(name) {
  const value = (process.env[name] ?? dotenv[name] ?? gradle[name] ?? '').trim();
  // These are token values, never URLs or xcconfig expressions. Reject config injection.
  if (value && !/^[A-Za-z0-9._-]+$/.test(value)) throw new Error(`Invalid token format for ${name}.`);
  return `${name} = ${value}`;
}
await writeFile(new URL('ios/Config/Secrets.xcconfig', root),
  '// Generated local build configuration. Do not commit.\n' +
  ['TMDB_ACCESS_TOKEN', 'BACKUP_MOVIES_API_KEY'].map(setting).join('\n') + '\n');
console.log('iOS local configuration generated (key values hidden).');
await import('./icon.mjs');
