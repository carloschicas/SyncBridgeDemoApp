export interface Order {
  id: string;
  transactionId: string;
  customerName: string;
  productName: string;
  quantity: number;
  totalAmount: number;
  status: string;
  createdAt: string;
}

export type IdempotencyStatus = 'PROCESSING' | 'SUCCESS' | 'CONFLICT';

export interface IdempotencyRecord {
  transactionId: string;
  status: IdempotencyStatus;
  responseStatus: number;
  responseBody: Record<string, unknown>;
  endpoint: string;
  clientTimestamp: number | null;
  attemptCount: number | null;
  createdAt: string;
  expiresAt: string;
}

export interface CreateOrderPayload {
  customerName: string;
  productName: string;
  quantity: number;
  totalAmount: number;
}

export interface OrderResponse extends Order {
  source: 'processed' | 'cached';
}

export interface ConflictResponse {
  error: string;
  message: string;
  offlineRef: string;
  source: 'processed' | 'cached';
}
