// Engineering: LX Obsidian Labs
// Fail closed until an authoritative migration history is recovered.
console.error('Database changes disabled: migration history is unverified. See docs/DATABASE_RECOVERY.md.');
process.exitCode = 1;
