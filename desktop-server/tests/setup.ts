import { beforeAll, afterAll } from '@jest/globals';
import { execSync } from 'child_process';
import path from 'path';
import fs from 'fs';

// Set test environment
process.env.NODE_ENV = 'test';
process.env.DATABASE_URL = 'file:./data/test.db';
process.env.LOG_LEVEL = 'error';

// Ensure test directories exist
beforeAll(async () => {
  const testDataDir = path.join(__dirname, '..', 'data');
  const testLogsDir = path.join(__dirname, '..', 'logs');
  const testUploadsDir = path.join(__dirname, '..', 'uploads', 'audio');

  // Create directories if they don't exist
  [testDataDir, testLogsDir, testUploadsDir].forEach(dir => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  });

  // Generate Prisma client for tests
  try {
    execSync('npx prisma generate', { 
      cwd: path.join(__dirname, '..'),
      stdio: 'pipe'
    });
  } catch (error) {
    console.warn('Prisma generate failed during test setup:', error);
  }

  // Run database migrations for tests
  try {
    execSync('npx prisma db push --force-reset', { 
      cwd: path.join(__dirname, '..'),
      stdio: 'pipe'
    });
  } catch (error) {
    console.warn('Database setup failed during test setup:', error);
  }
});

// Clean up after all tests
afterAll(async () => {
  // Clean up test database
  const testDbPath = path.join(__dirname, '..', 'data', 'test.db');
  if (fs.existsSync(testDbPath)) {
    try {
      fs.unlinkSync(testDbPath);
    } catch (error) {
      console.warn('Failed to clean up test database:', error);
    }
  }
});