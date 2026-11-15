import { readFile } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';

// Transfer only the two client configuration keys via stdin, without printing their values.
const config = await readFile(new URL('../Config/Secrets.xcconfig', import.meta.url), 'utf8');
for (const name of ['TMDB_ACCESS_TOKEN', 'BACKUP_MOVIES_API_KEY']) {
  const value = config.split(/\r?\n/).find(line => line.startsWith(`${name} = `))?.slice(name.length + 3).trim();
  if (!value) { console.log(`${name}: unset`); continue; }
  if (!process.argv.includes('--apply')) { console.log(`${name}: configured locally; use --apply to set the Actions secret`); continue; }
  const result = spawnSync('gh', ['secret', 'set', name, '--repo', 'lx-obsidian-labs/lx-obsidian-portal'],
    { input: value, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] });
  if (result.error || result.status !== 0) throw new Error(`Could not set Actions secret ${name}. Check gh authentication and repository access.`);
  console.log(`${name}: Actions secret configured`);
}
