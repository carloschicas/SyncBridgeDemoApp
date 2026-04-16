import { Request, Response } from 'express';
import { ConflictResponse } from '../types';

export function forceConflict(req: Request, res: Response): void {
  const transactionId = req.headers['x-transaction-id'] as string;

  const response: ConflictResponse = {
    error: 'OUT_OF_STOCK',
    message: 'El producto solicitado ya no está disponible.',
    offlineRef: transactionId,
    source: 'processed',
  };

  res.status(409).json(response);
}
