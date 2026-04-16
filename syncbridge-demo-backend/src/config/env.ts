function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    console.error(`[CONFIG] Missing required environment variable: ${name}`);
    process.exit(1);
  }
  return value;
}

export const config = {
  port: parseInt(process.env['PORT'] ?? '3000', 10),
  nodeEnv: process.env['NODE_ENV'] ?? 'development',
  databaseUrl: requireEnv('DATABASE_URL'),
  idempotencyTtlDays: parseInt(process.env['IDEMPOTENCY_TTL_DAYS'] ?? '30', 10),
  ttlCleanupIntervalMs: parseInt(process.env['TTL_CLEANUP_INTERVAL_MS'] ?? '3600000', 10),
};
