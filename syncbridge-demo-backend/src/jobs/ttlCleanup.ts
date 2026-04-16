import { deleteExpired } from '../services/idempotencyLog';

async function cleanup(): Promise<void> {
  try {
    const deleted = await deleteExpired();
    if (deleted > 0) {
      console.log(`[${new Date().toISOString()}] [TTL-CLEANUP] Deleted ${deleted} expired idempotency records.`);
    }
  } catch (err) {
    console.error('[TTL-CLEANUP] Error during cleanup:', (err as Error).message);
  }
}

export function startTtlCleanup(intervalMs: number): void {
  // Run once immediately, then on every interval
  void cleanup();
  setInterval(() => { void cleanup(); }, intervalMs);
  console.log(`[${new Date().toISOString()}] [TTL-CLEANUP] Started — interval: ${intervalMs / 1000}s`);
}
