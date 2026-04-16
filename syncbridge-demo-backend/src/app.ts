import express, { Request, Response } from 'express';
import ordersRouter from './routes/orders';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const { version } = require('../package.json') as { version: string };

export function createApp(): express.Application {
  const app = express();

  app.use(express.json());

  // Request logger
  app.use((req: Request, _res: Response, next) => {
    if (req.path !== '/health') {
      console.log(`[${new Date().toISOString()}] → ${req.method} ${req.path}`);
    }
    next();
  });

  // Routes
  app.use('/api/orders', ordersRouter);

  // Health check (for Docker / load balancers)
  app.get('/health', (_req: Request, res: Response) => {
    res.json({ status: 'ok', version, timestamp: new Date().toISOString() });
  });

  // 404 handler
  app.use((_req: Request, res: Response) => {
    res.status(404).json({ error: 'NOT_FOUND' });
  });

  return app;
}
