import { pool } from '../db/pool';
import { IdempotencyRecord, IdempotencyStatus } from '../types';

function mapRow(row: Record<string, unknown>): IdempotencyRecord {
  return {
    transactionId: row['transaction_id'] as string,
    status: row['status'] as IdempotencyStatus,
    responseStatus: row['response_status'] as number,
    responseBody: row['response_body'] as Record<string, unknown>,
    endpoint: row['endpoint'] as string,
    clientTimestamp: row['client_timestamp'] as number | null,
    attemptCount: row['attempt_count'] as number | null,
    createdAt: row['created_at'] as string,
    expiresAt: row['expires_at'] as string,
  };
}

/**
 * Attempts to atomically claim a transaction slot.
 * Returns the newly inserted record if successful (new transaction),
 * or null if the transaction_id already exists (duplicate/concurrent).
 */
export async function claimTransaction(
  transactionId: string,
  endpoint: string,
  clientTimestamp: number | null,
  attemptCount: number | null,
  ttlDays: number,
): Promise<IdempotencyRecord | null> {
  const result = await pool.query(
    `INSERT INTO idempotency_log
       (transaction_id, status, response_status, response_body, endpoint, client_timestamp, attempt_count, expires_at)
     VALUES ($1, 'PROCESSING', 0, '{}', $2, $3, $4, NOW() + ($5 || ' days')::INTERVAL)
     ON CONFLICT (transaction_id) DO NOTHING
     RETURNING *`,
    [transactionId, endpoint, clientTimestamp, attemptCount, ttlDays],
  );

  if (result.rowCount === 1) {
    return mapRow(result.rows[0] as Record<string, unknown>);
  }
  return null;
}

/**
 * Finds an existing idempotency record by transaction_id.
 */
export async function findTransaction(transactionId: string): Promise<IdempotencyRecord | null> {
  const result = await pool.query(
    'SELECT * FROM idempotency_log WHERE transaction_id = $1',
    [transactionId],
  );

  if (result.rowCount === 0) return null;
  return mapRow(result.rows[0] as Record<string, unknown>);
}

/**
 * Updates a PROCESSING record with the final status and response.
 */
export async function updateTransaction(
  transactionId: string,
  status: IdempotencyStatus,
  responseStatus: number,
  responseBody: Record<string, unknown>,
): Promise<void> {
  await pool.query(
    `UPDATE idempotency_log
     SET status = $1, response_status = $2, response_body = $3
     WHERE transaction_id = $4`,
    [status, responseStatus, JSON.stringify(responseBody), transactionId],
  );
}

/**
 * Deletes all idempotency records past their TTL.
 * Called periodically by the TTL cleanup job.
 */
export async function deleteExpired(): Promise<number> {
  const result = await pool.query(
    'DELETE FROM idempotency_log WHERE expires_at < NOW()',
  );
  return result.rowCount ?? 0;
}
