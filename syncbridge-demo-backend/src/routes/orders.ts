import { Router } from 'express';
import { idempotencyMiddleware } from '../middleware/idempotency';
import { createOrder } from '../controllers/createOrder';
import { forceConflict } from '../controllers/forceConflict';

const router = Router();

// POST /api/orders/force-conflict must be registered BEFORE /api/orders
// to avoid Express matching /force-conflict as a dynamic segment.
router.post('/force-conflict', idempotencyMiddleware, forceConflict);
router.post('/', idempotencyMiddleware, createOrder);

export default router;
