import { Request, Response, NextFunction } from 'express';
import { validate as uuidValidate } from 'uuid';
import { config } from '../config/env';
import { claimTransaction, findTransaction, updateTransaction } from '../services/idempotencyLog';

function formatLog(
  icon: string,
  label: string,
  method: string,
  path: string,
  txn: string,
  attempt: string | number,
  status: number,
): void {
  const ts = new Date().toISOString();
  const shortTxn = txn.split('-')[0];
  console.log(
    `[${ts}] ${icon} [${label.padEnd(9)}]  ${method} ${path.padEnd(35)}  txn=${shortTxn}  attempt=${attempt}  status=${status}`,
  );
}

export async function idempotencyMiddleware(
  req: Request,
  res: Response,
  next: NextFunction,
): Promise<void> {
  const transactionId = req.headers['x-transaction-id'] as string | undefined;
  const clientTimestamp = req.headers['x-client-timestamp'] as string | undefined;
  const attemptCount = req.headers['x-attempt-count'] as string | undefined;

  // Validate required header
  if (!transactionId || !uuidValidate(transactionId)) {
    res.status(400).json({
      error: 'MISSING_TRANSACTION_ID',
      message: 'Header X-Transaction-Id is required and must be a valid UUID v4.',
    });
    return;
  }

  const endpoint = `${req.method} ${req.path}`;
  const clientTs = clientTimestamp ? parseInt(clientTimestamp, 10) : null;
  const attempt = attemptCount ? parseInt(attemptCount, 10) : 1;

  // Step 1: Attempt atomic claim
  const claimed = await claimTransaction(
    transactionId,
    endpoint,
    clientTs,
    attempt,
    config.idempotencyTtlDays,
  );

  if (claimed !== null) {
    // New transaction — intercept res.json() to capture and persist the response
    const originalJson = res.json.bind(res) as (body: unknown) => Response;

    res.json = function (body: unknown): Response {
      const httpStatus = res.statusCode;
      const logStatus: 'SUCCESS' | 'CONFLICT' = httpStatus >= 200 && httpStatus < 300
        ? 'SUCCESS'
        : 'CONFLICT';

      formatLog('✅', 'PROCESSED', req.method, req.path, transactionId, attempt, httpStatus);

      // Persist result asynchronously (fire-and-forget — response already on the wire)
      updateTransaction(transactionId, logStatus, httpStatus, body as Record<string, unknown>).catch(
        (err: Error) => console.error('[IDEMPOTENCY] Failed to update log:', err.message),
      );

      return originalJson(body);
    };

    next();
    return;
  }

  // Step 2: transaction_id already exists — read the record
  const existing = await findTransaction(transactionId);

  if (!existing) {
    // Defensive: claim failed but record not found — should not happen
    res.status(500).json({ error: 'IDEMPOTENCY_STATE_UNKNOWN' });
    return;
  }

  if (existing.status === 'PROCESSING') {
    // Another concurrent request is still in-flight
    console.log(
      `[${new Date().toISOString()}] ⏳ [IN-FLIGHT]   ${req.method} ${req.path}  txn=${transactionId.split('-')[0]}  attempt=${attempt}`,
    );
    res
      .status(409)
      .header('Retry-After', '2')
      .json({
        error: 'CONCURRENT_REQUEST',
        message: 'Same transaction is currently being processed. Retry in 2 seconds.',
      });
    return;
  }

  // Replay cached response.
  // SUCCESS retries return 200 (not the original 201) per SyncBridge spec.
  // CONFLICT retries replay the exact stored status (409).
  const replayStatus = existing.status === 'SUCCESS' ? 200 : existing.responseStatus;
  const replayBody = { ...existing.responseBody, source: 'cached' };

  formatLog('♻️ ', 'CACHED', req.method, req.path, transactionId, attempt, replayStatus);

  res.status(replayStatus).json(replayBody);
}
