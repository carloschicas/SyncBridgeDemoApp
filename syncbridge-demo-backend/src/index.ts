import { config } from './config/env';
import { createApp } from './app';
import { startTtlCleanup } from './jobs/ttlCleanup';

const app = createApp();

app.listen(config.port, () => {
  console.log('─────────────────────────────────────────────────────');
  console.log('  SyncBridge Demo Backend');
  console.log(`  Listening on http://0.0.0.0:${config.port}`);
  console.log(`  Environment: ${config.nodeEnv}`);
  console.log(`  Idempotency TTL: ${config.idempotencyTtlDays} days`);
  console.log('─────────────────────────────────────────────────────');

  startTtlCleanup(config.ttlCleanupIntervalMs);
});
