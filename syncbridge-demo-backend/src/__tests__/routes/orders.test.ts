import request from 'supertest';
import { createApp } from '../../app';

// Mock DB pool — no real PostgreSQL connection needed
jest.mock('../../db/pool', () => ({
  pool: {
    query: jest.fn(),
    on: jest.fn(),
  },
}));

// Mock idempotency service — control cache hit/miss per test
jest.mock('../../services/idempotencyLog');

import { pool } from '../../db/pool';
import {
  claimTransaction,
  findTransaction,
  updateTransaction,
} from '../../services/idempotencyLog';

const mockQuery = pool.query as jest.MockedFunction<typeof pool.query>;
const mockClaimTransaction = claimTransaction as jest.MockedFunction<typeof claimTransaction>;
const mockFindTransaction = findTransaction as jest.MockedFunction<typeof findTransaction>;
const mockUpdateTransaction = updateTransaction as jest.MockedFunction<typeof updateTransaction>;

// ─── Constants ────────────────────────────────────────────────────────────────

const VALID_UUID = '550e8400-e29b-41d4-a716-446655440000';

const VALID_HEADERS = {
  'x-transaction-id': VALID_UUID,
  'x-client-timestamp': '1713264000000',
  'x-attempt-count': '1',
};

const VALID_BODY = {
  customerName: 'Distribuidora Acme',
  productName: 'Widget Pro X',
  quantity: 10,
  totalAmount: 299.90,
};

const MOCK_CLAIMED_RECORD = {
  transactionId: VALID_UUID,
  status: 'PROCESSING' as const,
  responseStatus: 0,
  responseBody: {},
  endpoint: 'POST /',
  clientTimestamp: 1713264000000,
  attemptCount: 1,
  createdAt: new Date().toISOString(),
  expiresAt: new Date().toISOString(),
};

const MOCK_DB_ROW = {
  id: 'order-uuid-123',
  transaction_id: VALID_UUID,
  customer_name: 'Distribuidora Acme',
  product_name: 'Widget Pro X',
  quantity: 10,
  total_amount: '299.90',
  status: 'CONFIRMED',
  created_at: new Date('2026-04-16T12:00:00Z'),
};

const CACHED_ORDER_BODY = {
  id: 'order-uuid-123',
  transactionId: VALID_UUID,
  customerName: 'Distribuidora Acme',
  productName: 'Widget Pro X',
  quantity: 10,
  totalAmount: 299.9,
  status: 'CONFIRMED',
  createdAt: '2026-04-16T12:00:00.000Z',
  source: 'processed',
};

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('POST /api/orders', () => {
  const app = createApp();

  describe('New transaction', () => {

    beforeEach(() => {
      mockClaimTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD);
      mockUpdateTransaction.mockResolvedValue(undefined);
      mockQuery.mockResolvedValue({ rows: [MOCK_DB_ROW], rowCount: 1 } as never);
    });

    it('creates an order and returns 201 with source=processed', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send(VALID_BODY);

      expect(res.status).toBe(201);
      expect(res.body).toMatchObject({
        id: 'order-uuid-123',
        customerName: 'Distribuidora Acme',
        productName: 'Widget Pro X',
        quantity: 10,
        totalAmount: 299.9,
        status: 'CONFIRMED',
        source: 'processed',
      });
      expect(res.body.transactionId).toBe(VALID_UUID);
      expect(res.body.createdAt).toBeDefined();
    });

    it('inserts the order into the DB with the correct parameters', async () => {
      await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send(VALID_BODY);

      expect(mockQuery).toHaveBeenCalledWith(
        expect.stringContaining('INSERT INTO orders'),
        [VALID_UUID, 'Distribuidora Acme', 'Widget Pro X', 10, 299.90],
      );
    });

    it('returns 400 when customerName is missing', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send({ ...VALID_BODY, customerName: undefined });

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'INVALID_PAYLOAD' });
    });

    it('returns 400 when quantity is zero', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send({ ...VALID_BODY, quantity: 0 });

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'INVALID_PAYLOAD' });
    });

    it('returns 400 when quantity is negative', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send({ ...VALID_BODY, quantity: -5 });

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'INVALID_PAYLOAD' });
    });

  });

  describe('Duplicate transaction (idempotency)', () => {

    it('returns 200 with cached body and source=cached on retry', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue({
        ...MOCK_CLAIMED_RECORD,
        status: 'SUCCESS',
        responseStatus: 201,
        responseBody: CACHED_ORDER_BODY,
      });

      const res = await request(app)
        .post('/api/orders')
        .set({ ...VALID_HEADERS, 'x-attempt-count': '2' })
        .send(VALID_BODY);

      expect(res.status).toBe(200);
      expect(res.body.source).toBe('cached');
      expect(res.body.id).toBe('order-uuid-123');
      // DB query must NOT be called on retries
      expect(mockQuery).not.toHaveBeenCalled();
    });

    it('returns the same order ID on every retry', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue({
        ...MOCK_CLAIMED_RECORD,
        status: 'SUCCESS',
        responseStatus: 201,
        responseBody: CACHED_ORDER_BODY,
      });

      const attempt2 = await request(app).post('/api/orders').set({ ...VALID_HEADERS, 'x-attempt-count': '2' }).send(VALID_BODY);
      const attempt3 = await request(app).post('/api/orders').set({ ...VALID_HEADERS, 'x-attempt-count': '3' }).send(VALID_BODY);

      expect(attempt2.body.id).toBe(attempt3.body.id);
      expect(attempt2.status).toBe(200);
      expect(attempt3.status).toBe(200);
    });

    it('returns 409 with Retry-After when the same transaction is still in-flight', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD); // PROCESSING

      const res = await request(app)
        .post('/api/orders')
        .set(VALID_HEADERS)
        .send(VALID_BODY);

      expect(res.status).toBe(409);
      expect(res.body).toMatchObject({ error: 'CONCURRENT_REQUEST' });
      expect(res.headers['retry-after']).toBe('2');
    });

  });

  describe('Missing or invalid X-Transaction-Id', () => {

    it('returns 400 when X-Transaction-Id header is absent', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set({ 'x-client-timestamp': '1713264000000', 'x-attempt-count': '1' })
        .send(VALID_BODY);

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'MISSING_TRANSACTION_ID' });
    });

    it('returns 400 when X-Transaction-Id is not a UUID', async () => {
      const res = await request(app)
        .post('/api/orders')
        .set({ ...VALID_HEADERS, 'x-transaction-id': 'plaintext-id' })
        .send(VALID_BODY);

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'MISSING_TRANSACTION_ID' });
    });

  });

});
