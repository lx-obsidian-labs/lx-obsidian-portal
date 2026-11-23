import { readFile } from 'node:fs/promises';
import { parseEnv } from 'node:util';

// Deployment credentials may live in .env.deploy; the project's .env takes precedence.
export async function loadEnvironment() {
  const values = { ...process.env };
  for (const file of ['../.env.deploy', '../.env', '../.env.local']) {
    try {
      const parsed = parseEnv(await readFile(new URL(file, import.meta.url), 'utf8'));
      // Accept either conventional key name; the explicitly supplied project key wins.
      if (!parsed.NVIDIA_API_KEY && parsed.NVIDIA_NIM_API_KEY) parsed.NVIDIA_API_KEY = parsed.NVIDIA_NIM_API_KEY;
      Object.assign(values, parsed);
      // Read the existing pasted-key format during migration; never print its contents.
      if (!values.NEWSDATA_API_KEY) {
        values.NEWSDATA_API_KEY = (await readFile(new URL(file, import.meta.url), 'utf8'))
          .match(/^NEWS DATA API KEY\s+(\S+)\s*$/m)?.[1];
      }
    }
    catch (error) { if (error.code !== 'ENOENT') throw error; }
  }
  return values;
}
