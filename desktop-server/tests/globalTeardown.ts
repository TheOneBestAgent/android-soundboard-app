import path from 'path';
import fs from 'fs';

export default async function globalTeardown() {
  console.log('Cleaning up test environment...');
  
  const projectRoot = path.join(__dirname, '..');
  const testDbPath = path.join(projectRoot, 'data', 'test.db');
  const testDbWalPath = path.join(projectRoot, 'data', 'test.db-wal');
  const testDbShmPath = path.join(projectRoot, 'data', 'test.db-shm');

  // Clean up test database files
  [testDbPath, testDbWalPath, testDbShmPath].forEach(filePath => {
    if (fs.existsSync(filePath)) {
      try {
        fs.unlinkSync(filePath);
      } catch (error) {
        console.warn(`Failed to clean up ${filePath}:`, error);
      }
    }
  });

  // Clean up test logs
  const testLogsDir = path.join(projectRoot, 'logs');
  if (fs.existsSync(testLogsDir)) {
    try {
      const logFiles = fs.readdirSync(testLogsDir);
      logFiles.forEach(file => {
        if (file.includes('test') || file.includes('error')) {
          fs.unlinkSync(path.join(testLogsDir, file));
        }
      });
    } catch (error) {
      console.warn('Failed to clean up test logs:', error);
    }
  }

  console.log('Test environment cleanup complete.');
}