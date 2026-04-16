import request from 'supertest';
import { createApp } from '../../app';

jest.mock('../../db/pool', () => ({
  pool: { query: jest.fn(), on: jest.fn() },
}));

jest.mock('../../services/idempotencyLog');

import {
  claimTransaction,
  findTransaction,
  updateTransaction,
} from '../../services/idempotencyLog';

const mockClaimTransaction = claimTransaction as jest.MockedFunction<typeof claimTransaction>;
const mockFindTransaction = findTransaction as jest.MockedFunction<typeof findTransaction>;
const mockUpdateTransaction = updateTransaction as jest.MockedFunction<typeof updateTransaction>;

// ─── Constants ────────────────────────────────────────────────────────────────

const VALID_UUID = 'aaaa1111-e29b-41d4-a716-446655440000';

const VALID_HEADERS = {
  'x-transaction-id': VALID_UUID,
  'x-client-timestamp': '1713264000000',
  'x-attempt-count': '1',
};

const MOCK_CLAIMED_RECORD = {
  transactionId: VALID_UUID,
  status: 'PROCESSING' as const,
  responseStatus: 0,
  responseBody: {},
  endpoint: 'POST /force-conflict',
  clientTimestamp: 1713264000000,
  attemptCount: 1,
  createdAt: new Date().toISOString(),
  expiresAt: new Date().toISOString(),
};

const CACHED_CONFLICT_BODY = {
  error: 'OUT_OF_STOCK',
  message: 'El producto solicitado ya no está disponible.',
  offlineRef: VALID_UUID,
  source: 'processed',
};

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('POST /api/orders/force-conflict', () => {
  const app = createApp();

  describe('New transaction', () => {

    beforeEach(() => {
      mockClaimTransaction.mockResolvedValue(MOCK_CLAIMED_RECORD);
      mockUpdateTransaction.mockResolvedValue(undefined);
    });

    it('returns 409 with OUT_OF_STOCK error and source=processed', async () => {
      const res = await request(app)
        .post('/api/orders/force-conflict')
        .set(VALID_HEADERS)
        .send({ customerName: 'Beta Corp', productName: 'Agotado', quantity: 1, totalAmount: 10 });

      expect(res.status).toBe(409);
      expect(res.body).toMatchObject({
        error: 'OUT_OF_STOCK',
        message: 'El producto solicitado ya no está disponible.',
        source: 'processed',
      });
    });

    it('includes the X-Transaction-Id as offlineRef in the response', async () => {
      const res = await request(app)
        .post('/api/orders/force-conflict')
        .set(VALID_HEADERS)
        .send({});

      expect(res.body.offlineRef).toBe(VALID_UUID);
    });

    it('saves the 409 response to the idempotency log as CONFLICT', async () => {
      await request(app)
        .post('/api/orders/force-conflict')
        .set(VALID_HEADERS)
        .send({});

      await Promise.resolve(); // flush fire-and-forget

      expect(mockUpdateTransaction).toHaveBeenCalledWith(
        VALID_UUID,
        'CONFLICT',
        409,
        expect.objectContaining({ error: 'OUT_OF_STOCK' }),
      );
    });

  });

  describe('Duplicate transaction (idempotency)', () => {

    it('returns 409 with cached body and source=cached on retry', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue({
        ...MOCK_CLAIMED_RECORD,
        status: 'CONFLICT',
        responseStatus: 409,
        responseBody: CACHED_CONFLICT_BODY,
      });

      const res = await request(app)
        .post('/api/orders/force-conflict')
        .set({ ...VALID_HEADERS, 'x-attempt-count': '2' })
        .send({});

      expect(res.status).toBe(409);
      expect(res.body).toMatchObject({
        error: 'OUT_OF_STOCK',
        source: 'cached',
      });
    });

    it('returns the same offlineRef on every retry', async () => {
      mockClaimTransaction.mockResolvedValue(null);
      mockFindTransaction.mockResolvedValue({
        ...MOCK_CLAIMED_RECORD,
        status: 'CONFLICT',
        responseStatus: 409,
        responseBody: CACHED_CONFLICT_BODY,
      });

      const attempt2 = await request(app).post('/api/orders/force-conflict').set({ ...VALID_HEADERS, 'x-attempt-count': '2' }).send({});
      const attempt3 = await request(app).post('/api/orders/force-conflict').set({ ...VALID_HEADERS, 'x-attempt-count': '3' }).send({});

      expect(attempt2.body.offlineRef).toBe(VALID_UUID);
      expect(attempt3.body.offlineRef).toBe(VALID_UUID);
      expect(attempt2.status).toBe(409);
      expect(attempt3.status).toBe(409);
    });

  });

  describe('Missing or invalid X-Transaction-Id', () => {

    it('returns 400 without X-Transaction-Id header', async () => {
      const res = await request(app)
        .post('/api/orders/force-conflict')
        .set({ 'x-client-timestamp': '1713264000000' })
        .send({});

      expect(res.status).toBe(400);
      expect(res.body).toMatchObject({ error: 'MISSING_TRANSACTION_ID' });
    });

  });

});
