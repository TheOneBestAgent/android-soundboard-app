import { execSync } from 'child_process';
import path from 'path';
import fs from 'fs';

export default async function globalSetup() {
  console.log('Setting up test environment...');
  
  // Set test environment variables
  process.env.NODE_ENV = 'test';
  process.env.DATABASE_URL = 'file:./data/test.db';
  process.env.LOG_LEVEL = 'error';
  
  const projectRoot = path.join(__dirname, '..');
  const testDataDir = path.join(projectRoot, 'data');
  const testLogsDir = path.join(projectRoot, 'logs');
  const testUploadsDir = path.join(projectRoot, 'uploads', 'audio');

  // Create test directories
  [testDataDir, testLogsDir, testUploadsDir].forEach(dir => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  });

  // Clean up any existing test database
  const testDbPath = path.join(testDataDir, 'test.db');
  if (fs.existsSync(testDbPath)) {
    fs.unlinkSync(testDbPath);
  }

  try {
    // Generate Prisma client
    execSync('npx prisma generate', { 
      cwd: projectRoot,
      stdio: 'inherit'
    });

    // Create test database schema
    execSync('npx prisma db push --force-reset', { 
      cwd: projectRoot,
      stdio: 'inherit'
    });

    console.log('Test environment setup complete.');
  } catch (error) {
    console.error('Failed to setup test environment:', error);
    throw error;
  }
}