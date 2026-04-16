import { Request, Response } from 'express';
import { pool } from '../db/pool';
import { CreateOrderPayload, OrderResponse } from '../types';

export async function createOrder(req: Request, res: Response): Promise<void> {
  const { customerName, productName, quantity, totalAmount } = req.body as CreateOrderPayload;

  if (!customerName || !productName || !quantity || totalAmount === undefined) {
    res.status(400).json({
      error: 'INVALID_PAYLOAD',
      message: 'Fields customerName, productName, quantity, and totalAmount are required.',
    });
    return;
  }

  if (typeof quantity !== 'number' || quantity <= 0) {
    res.status(400).json({
      error: 'INVALID_PAYLOAD',
      message: 'quantity must be a positive integer.',
    });
    return;
  }

  const transactionId = req.headers['x-transaction-id'] as string;

  const result = await pool.query(
    `INSERT INTO orders (transaction_id, customer_name, product_name, quantity, total_amount)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING *`,
    [transactionId, customerName, productName, quantity, totalAmount],
  );

  const row = result.rows[0] as Record<string, unknown>;

  const response: OrderResponse = {
    id: row['id'] as string,
    transactionId: row['transaction_id'] as string,
    customerName: row['customer_name'] as string,
    productName: row['product_name'] as string,
    quantity: row['quantity'] as number,
    totalAmount: parseFloat(row['total_amount'] as string),
    status: row['status'] as string,
    createdAt: (row['created_at'] as Date).toISOString(),
    source: 'processed',
  };

  res.status(201).json(response);
}
