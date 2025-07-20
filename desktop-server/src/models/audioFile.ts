import { prisma, AudioFile, Prisma } from './index';

export class AudioFileModel {
  /**
   * Create a new audio file record
   */
  static async create(data: Prisma.AudioFileCreateInput): Promise<AudioFile> {
    return prisma.audioFile.create({
      data,
    });
  }

  /**
   * Find audio file by ID
   */
  static async findById(id: string): Promise<AudioFile | null> {
    return prisma.audioFile.findUnique({
      where: { id },
    });
  }

  /**
   * Find audio file by file path
   */
  static async findByFilePath(filePath: string): Promise<AudioFile | null> {
    return prisma.audioFile.findFirst({
      where: { filePath },
    });
  }

  /**
   * Find audio file by filename
   */
  static async findByFilename(filename: string): Promise<AudioFile | null> {
    return prisma.audioFile.findFirst({
      where: { filename },
    });
  }

  /**
   * Get all audio files with pagination
   */
  static async findMany(options: {
    skip?: number;
    take?: number;
    orderBy?: Prisma.AudioFileOrderByWithRelationInput;
    format?: string;
  } = {}): Promise<AudioFile[]> {
    const { skip = 0, take = 50, orderBy = { createdAt: 'desc' }, format } = options;
    
    return prisma.audioFile.findMany({
      where: format ? { format } : undefined,
      skip,
      take,
      orderBy,
    });
  }

  /**
   * Search audio files by filename
   */
  static async search(query: string): Promise<AudioFile[]> {
    return prisma.audioFile.findMany({
      where: {
        filename: {
          contains: query,
          mode: 'insensitive',
        },
      },
      orderBy: {
        filename: 'asc',
      },
    });
  }

  /**
   * Update audio file
   */
  static async update(id: string, data: Prisma.AudioFileUpdateInput): Promise<AudioFile> {
    return prisma.audioFile.update({
      where: { id },
      data,
    });
  }

  /**
   * Delete audio file
   */
  static async delete(id: string): Promise<AudioFile> {
    return prisma.audioFile.delete({
      where: { id },
    });
  }

  /**
   * Bulk delete audio files
   */
  static async deleteMany(ids: string[]): Promise<{ count: number }> {
    return prisma.audioFile.deleteMany({
      where: {
        id: {
          in: ids,
        },
      },
    });
  }

  /**
   * Get audio files by format
   */
  static async findByFormat(format: string): Promise<AudioFile[]> {
    return prisma.audioFile.findMany({
      where: { format },
      orderBy: {
        filename: 'asc',
      },
    });
  }

  /**
   * Get audio file statistics
   */
  static async getStats(): Promise<{
    total: number;
    totalSize: number;
    totalDuration: number;
    averageSize: number;
    averageDuration: number;
    formatBreakdown: Record<string, number>;
  }> {
    const [count, aggregates, formatCounts] = await Promise.all([
      prisma.audioFile.count(),
      prisma.audioFile.aggregate({
        _sum: {
          fileSize: true,
          duration: true,
        },
        _avg: {
          fileSize: true,
          duration: true,
        },
      }),
      prisma.audioFile.groupBy({
        by: ['format'],
        _count: {
          format: true,
        },
      }),
    ]);

    const formatBreakdown = formatCounts.reduce(
      (acc, item) => {
        if (item.format) {
          acc[item.format] = item._count.format;
        }
        return acc;
      },
      {} as Record<string, number>
    );

    return {
      total: count,
      totalSize: aggregates._sum.fileSize || 0,
      totalDuration: aggregates._sum.duration || 0,
      averageSize: aggregates._avg.fileSize || 0,
      averageDuration: aggregates._avg.duration || 0,
      formatBreakdown,
    };
  }

  /**
   * Check if file exists in database
   */
  static async exists(filePath: string): Promise<boolean> {
    const file = await prisma.audioFile.findFirst({
      where: { filePath },
      select: { id: true },
    });
    return !!file;
  }

  /**
   * Get files larger than specified size
   */
  static async findLargeFiles(minSizeBytes: number): Promise<AudioFile[]> {
    return prisma.audioFile.findMany({
      where: {
        fileSize: {
          gte: minSizeBytes,
        },
      },
      orderBy: {
        fileSize: 'desc',
      },
    });
  }

  /**
   * Get files longer than specified duration
   */
  static async findLongFiles(minDurationSeconds: number): Promise<AudioFile[]> {
    return prisma.audioFile.findMany({
      where: {
        duration: {
          gte: minDurationSeconds,
        },
      },
      orderBy: {
        duration: 'desc',
      },
    });
  }
}