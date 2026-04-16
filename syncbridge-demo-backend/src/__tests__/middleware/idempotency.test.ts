import { Request, Response, NextFunction } from 'express';
import { idempotencyMiddleware } from '../../middleware/idempotency';

// Mock the entire idempotencyLog service — no DB calls in unit tests
jest.mock('../../services/idempotencyLog');

import {
  claimTransaction,
  findTransaction,
  updateTransaction,
} from '../../services/idempotencyLog';

const mockClaimTransaction = claimTransaction as jest.MockedFunction<typeof claimTransaction>;
const mockFindTransaction = findTransaction as jest.MockedFunction<typeof findTransaction>;
const mockUpdateTransaction = updateTransaction as jest.MockedFunction<typeof updateTransaction>;

// ─── Helpers ──────────────────────────────────────────────────────────────────

const VALID_UUID = '550e8400-e29b-41d4-a716-446655440000';

function makeReq(headers: Record<string, string> = {}): Request {
  return {
    headers: {
      'x-transaction-id': VALID_UUID,
      'x-client-timestamp': '1713264000000',
      'x-attempt-count': '1',
      ...headers,
    },
    method: 'POST',
    path: '/api/orders',
    body: {},
  } as unknown as Request;
}

function makeRes(): { res: Response; statusCode: () => number; jsonBody: () => unknown } {
  let _statusCode = 200;
  let _jsonBody: unknown = undefined;
  let _retryAfter: string | undefined;

  const res = {
    get statusCode() { return _statusCode; },
    set statusCode(v: number) { _statusCode = v; },
    status: jest.fn(function (code: number) { _statusCode = code; return this; }),
    json: jest.fn(function (body: unknown) { _jsonBody = body; return this; }),
    header: jest.fn(function (_name: string, value: string) { _retryAfter = value; return this; }),
  } as unknown as Response;

  return {
    res,
    statusCode: () => _statusCode,
    jsonBody: () => _jsonBody,
  };
}

const MOCK_CLAIMED_RECORD = {
  transactionId: VALID_UUID,
  status: 'PROCESSING' as const,
  responseStatus: 0,
  responseBody: {},
  endpoint: 'POST /api/orders',
  clientTimestamp: 1713264000000,
  attemptCount: 1,
  createdAt: new Date().toISOString(),
  expiresAt: new Date().toISOString(),
};

const MOCK_SUCCESS_RECORD = {
  ...MOCK_CLAIMED_RECORD,
  status: 'SUCCESS' as const,
  responseStatus: 201,
  responseBody: { id: 'order-123', customerName: 'Acme', source: 'processed' },
};

const MOCK_CONFLICT_RECORD = {
  ...MOCK_CLAIMED_RECORD,
  status: 'CONFLICT' as const,
  responseStatus: 409,
  responseBody: { error: 'OUT_OF_STOCK', source: 'processed' },
};

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('idempotencyMiddleware', () => {

  describe('Header validation', () => {

    it('returns 400 when X-Transaction-Id header is missing', async () => {
      const req = makeReq({ 'x-transaction-id': '' });
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(400);
      expect(jsonBody()).toMatchObject({ error: 'MISSING_TRANSACTION_ID' });
      expect(next).not.toHaveBeenCalled();
    });

    it('returns 400 when X-Transaction-Id is not a valid UUID v4', async () => {
      const req = makeReq({ 'x-transaction-id': 'not-a-valid-uuid' });
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(400);
      expect(jsonBody()).toMatchObject({ error: 'MISSING_TRANSACTION_ID' });
      expect(next).not.toHaveBeenCalled();
    });

  });

  describe('New transaction (cache miss)', () => {

    it('calls next() and intercepts res.json() when transaction is new', async () => {
      mockClaimTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD);
      mockUpdateTransaction.mockResolvedValue(undefined);

      const req = makeReq();
      const { res } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(mockClaimTransaction).toHaveBeenCalledWith(
        VALID_UUID,
        'POST /api/orders',
        1713264000000,
        1,
        30,
      );
      expect(next).toHaveBeenCalledTimes(1);
    });

    it('calls updateTransaction with SUCCESS when controller responds 2xx', async () => {
      mockClaimTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD);
      mockUpdateTransaction.mockResolvedValue(undefined);

      const req = makeReq();
      const { res } = makeRes();
      const next = jest.fn().mockImplementation(() => {
        // Simulate controller calling res.status(201).json(body)
        (res.status as jest.Mock)(201);
        res.json({ id: 'order-123', source: 'processed' });
      }) as NextFunction;

      await idempotencyMiddleware(req, res, next);

      // Allow the fire-and-forget promise to resolve
      await Promise.resolve();

      expect(mockUpdateTransaction).toHaveBeenCalledWith(
        VALID_UUID,
        'SUCCESS',
        201,
        { id: 'order-123', source: 'processed' },
      );
    });

    it('calls updateTransaction with CONFLICT when controller responds 4xx/5xx', async () => {
      mockClaimTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD);
      mockUpdateTransaction.mockResolvedValue(undefined);

      const req = makeReq();
      const { res } = makeRes();
      const next = jest.fn().mockImplementation(() => {
        (res.status as jest.Mock)(409);
        res.json({ error: 'OUT_OF_STOCK', source: 'processed' });
      }) as NextFunction;

      await idempotencyMiddleware(req, res, next);

      await Promise.resolve();

      expect(mockUpdateTransaction).toHaveBeenCalledWith(
        VALID_UUID,
        'CONFLICT',
        409,
        { error: 'OUT_OF_STOCK', source: 'processed' },
      );
    });

  });

  describe('Duplicate transaction (cache hit)', () => {

    it('returns 200 with cached body and source=cached for a SUCCESS duplicate', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue(MOCK_SUCCESS_RECORD);

      const req = makeReq({ 'x-attempt-count': '2' });
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(200);
      expect(jsonBody()).toMatchObject({ id: 'order-123', source: 'cached' });
      expect(next).not.toHaveBeenCalled();
    });

    it('returns 409 with cached body and source=cached for a CONFLICT duplicate', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue(MOCK_CONFLICT_RECORD);

      const req = makeReq({ 'x-attempt-count': '2' });
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(409);
      expect(jsonBody()).toMatchObject({ error: 'OUT_OF_STOCK', source: 'cached' });
      expect(next).not.toHaveBeenCalled();
    });

    it('returns 409 with Retry-After: 2 when the same transaction is still PROCESSING', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD); // status = PROCESSING

      const req = makeReq();
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(409);
      expect(jsonBody()).toMatchObject({ error: 'CONCURRENT_REQUEST' });
      expect((res.header as jest.Mock)).toHaveBeenCalledWith('Retry-After', '2');
      expect(next).not.toHaveBeenCalled();
    });

  });

  describe('Defensive cases', () => {

    it('returns 500 when claim fails but no record is found (state unknown)', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue(null);

      const req = makeReq();
      const { res, statusCode, jsonBody } = makeRes();
      const next = jest.fn() as NextFunction;

      await idempotencyMiddleware(req, res, next);

      expect(statusCode()).toBe(500);
      expect(jsonBody()).toMatchObject({ error: 'IDEMPOTENCY_STATE_UNKNOWN' });
      expect(next).not.toHaveBeenCalled();
    });

  });

});
