// Set required env vars before any module is loaded.
// This prevents config/env.ts from calling process.exit(1).
process.env['DATABASE_URL'] = 'postgresql://test:test@localhost:5432/test_db';
process.env['PORT'] = '3001';
process.env['NODE_ENV'] = 'test';
process.env['IDEMPOTENCY_TTL_DAYS'] = '30';
process.env['TTL_CLEANUP_INTERVAL_MS'] = '3600000';
