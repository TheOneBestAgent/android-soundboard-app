import { prisma } from '../models/index';
import { PrismaClientKnownRequestError } from '@prisma/client/runtime/library';

export class DatabaseUtils {
  /**
   * Test database connection
   */
  static async testConnection(): Promise<boolean> {
    try {
      await prisma.$queryRaw`SELECT 1`;
      return true;
    } catch (error) {
      console.error('Database connection test failed:', error);
      return false;
    }
  }

  /**
   * Get database health information
   */
  static async getHealthInfo(): Promise<{
    connected: boolean;
    version: string;
    tableCount: number;
    totalRecords: number;
  }> {
    try {
      const [versionResult, tableInfo] = await Promise.all([
        prisma.$queryRaw<[{ version: string }]>`SELECT sqlite_version() as version`,
        Promise.all([
          prisma.user.count(),
          prisma.soundButton.count(),
          prisma.soundboardGrid.count(),
          prisma.audioFile.count(),
        ]),
      ]);

      const [userCount, soundButtonCount, gridCount, audioFileCount] = tableInfo;
      const totalRecords = userCount + soundButtonCount + gridCount + audioFileCount;

      return {
        connected: true,
        version: versionResult[0].version,
        tableCount: 4, // users, sound_buttons, soundboard_grids, audio_files
        totalRecords,
      };
    } catch (error) {
      console.error('Failed to get database health info:', error);
      return {
        connected: false,
        version: 'unknown',
        tableCount: 0,
        totalRecords: 0,
      };
    }
  }

  /**
   * Execute database cleanup
   */
  static async cleanup(): Promise<void> {
    try {
      // Clean up orphaned records, optimize database, etc.
      await prisma.$executeRaw`VACUUM`;
      await prisma.$executeRaw`ANALYZE`;
      console.log('Database cleanup completed successfully');
    } catch (error) {
      console.error('Database cleanup failed:', error);
      throw error;
    }
  }

  /**
   * Get database size information
   */
  static async getDatabaseSize(): Promise<{
    sizeBytes: number;
    sizeFormatted: string;
  }> {
    try {
      const result = await prisma.$queryRaw<[{ page_count: number; page_size: number }]>`
        PRAGMA page_count, page_size
      `;
      
      const sizeBytes = result[0].page_count * result[0].page_size;
      const sizeFormatted = this.formatBytes(sizeBytes);
      
      return { sizeBytes, sizeFormatted };
    } catch (error) {
      console.error('Failed to get database size:', error);
      return { sizeBytes: 0, sizeFormatted: '0 B' };
    }
  }

  /**
   * Format bytes to human readable string
   */
  private static formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  }

  /**
   * Handle Prisma errors and return user-friendly messages
   */
  static handlePrismaError(error: unknown): string {
    if (error instanceof PrismaClientKnownRequestError) {
      switch (error.code) {
        case 'P2002':
          return 'A record with this information already exists.';
        case 'P2014':
          return 'The change you are trying to make would violate a required relation.';
        case 'P2003':
          return 'Foreign key constraint failed.';
        case 'P2025':
          return 'Record not found.';
        case 'P2016':
          return 'Query interpretation error.';
        case 'P2021':
          return 'The table does not exist in the current database.';
        case 'P2022':
          return 'The column does not exist in the current database.';
        default:
          return `Database error: ${error.message}`;
      }
    }
    
    if (error instanceof Error) {
      return error.message;
    }
    
    return 'An unknown database error occurred.';
  }

  /**
   * Execute transaction with retry logic
   */
  static async executeWithRetry<T>(
    operation: () => Promise<T>,
    maxRetries: number = 3,
    delay: number = 1000
  ): Promise<T> {
    let lastError: unknown;
    
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await operation();
      } catch (error) {
        lastError = error;
        
        if (attempt === maxRetries) {
          break;
        }
        
        console.warn(`Database operation failed (attempt ${attempt}/${maxRetries}):`, error);
        await new Promise(resolve => setTimeout(resolve, delay * attempt));
      }
    }
    
    throw lastError;
  }

  /**
   * Backup database to specified path
   */
  static async backup(backupPath: string): Promise<void> {
    try {
      await prisma.$executeRaw`VACUUM INTO ${backupPath}`;
      console.log(`Database backed up to: ${backupPath}`);
    } catch (error) {
      console.error('Database backup failed:', error);
      throw error;
    }
  }

  /**
   * Get table statistics
   */
  static async getTableStats(): Promise<Record<string, { count: number; size: string }>> {
    try {
      const [userCount, soundButtonCount, gridCount, audioFileCount] = await Promise.all([
        prisma.user.count(),
        prisma.soundButton.count(),
        prisma.soundboardGrid.count(),
        prisma.audioFile.count(),
      ]);

      return {
        users: { count: userCount, size: this.formatBytes(userCount * 100) }, // Estimated
        sound_buttons: { count: soundButtonCount, size: this.formatBytes(soundButtonCount * 200) },
        soundboard_grids: { count: gridCount, size: this.formatBytes(gridCount * 150) },
        audio_files: { count: audioFileCount, size: this.formatBytes(audioFileCount * 300) },
      };
    } catch (error) {
      console.error('Failed to get table statistics:', error);
      return {};
    }
  }
}

/**
 * Database connection middleware for Express
 */
export const databaseMiddleware = async (req: any, res: any, next: any) => {
  try {
    // Test connection before processing request
    const isConnected = await DatabaseUtils.testConnection();
    
    if (!isConnected) {
      return res.status(503).json({
        error: 'Database connection unavailable',
        message: 'The database is currently unavailable. Please try again later.',
      });
    }
    
    next();
  } catch (error) {
    console.error('Database middleware error:', error);
    res.status(500).json({
      error: 'Database error',
      message: DatabaseUtils.handlePrismaError(error),
    });
  }
};