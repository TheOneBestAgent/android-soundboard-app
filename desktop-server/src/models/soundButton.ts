import { prisma, SoundButton, Prisma } from './index';

export class SoundButtonModel {
  /**
   * Create a new sound button
   */
  static async create(data: Prisma.SoundButtonCreateInput): Promise<SoundButton> {
    return prisma.soundButton.create({
      data,
      include: {
        user: true,
      },
    });
  }

  /**
   * Find sound button by ID
   */
  static async findById(id: string): Promise<SoundButton | null> {
    return prisma.soundButton.findUnique({
      where: { id },
      include: {
        user: true,
      },
    });
  }

  /**
   * Find all sound buttons for a user
   */
  static async findByUserId(userId: string, options: {
    skip?: number;
    take?: number;
    orderBy?: Prisma.SoundButtonOrderByWithRelationInput;
  } = {}): Promise<SoundButton[]> {
    const { skip = 0, take = 50, orderBy = { createdAt: 'desc' } } = options;
    
    return prisma.soundButton.findMany({
      where: { userId },
      skip,
      take,
      orderBy,
      include: {
        user: true,
      },
    });
  }

  /**
   * Search sound buttons by name
   */
  static async search(userId: string, query: string): Promise<SoundButton[]> {
    return prisma.soundButton.findMany({
      where: {
        userId,
        name: {
          contains: query,
          mode: 'insensitive',
        },
      },
      include: {
        user: true,
      },
      orderBy: {
        name: 'asc',
      },
    });
  }

  /**
   * Update sound button
   */
  static async update(id: string, data: Prisma.SoundButtonUpdateInput): Promise<SoundButton> {
    return prisma.soundButton.update({
      where: { id },
      data,
      include: {
        user: true,
      },
    });
  }

  /**
   * Delete sound button
   */
  static async delete(id: string): Promise<SoundButton> {
    return prisma.soundButton.delete({
      where: { id },
    });
  }

  /**
   * Bulk delete sound buttons
   */
  static async deleteMany(ids: string[]): Promise<{ count: number }> {
    return prisma.soundButton.deleteMany({
      where: {
        id: {
          in: ids,
        },
      },
    });
  }

  /**
   * Update sound button volume
   */
  static async updateVolume(id: string, volume: number): Promise<SoundButton> {
    return prisma.soundButton.update({
      where: { id },
      data: { volume },
    });
  }

  /**
   * Update sound button pitch
   */
  static async updatePitch(id: string, pitch: number): Promise<SoundButton> {
    return prisma.soundButton.update({
      where: { id },
      data: { pitch },
    });
  }

  /**
   * Get sound buttons by file path
   */
  static async findByFilePath(filePath: string): Promise<SoundButton[]> {
    return prisma.soundButton.findMany({
      where: { filePath },
      include: {
        user: true,
      },
    });
  }

  /**
   * Get total duration for user's sound buttons
   */
  static async getTotalDuration(userId: string): Promise<number> {
    const result = await prisma.soundButton.aggregate({
      where: { userId },
      _sum: {
        duration: true,
      },
    });
    
    return result._sum.duration || 0;
  }

  /**
   * Get sound button statistics
   */
  static async getStats(userId: string): Promise<{
    total: number;
    totalDuration: number;
    averageDuration: number;
    averageVolume: number;
  }> {
    const [count, aggregates] = await Promise.all([
      prisma.soundButton.count({ where: { userId } }),
      prisma.soundButton.aggregate({
        where: { userId },
        _sum: {
          duration: true,
        },
        _avg: {
          duration: true,
          volume: true,
        },
      }),
    ]);

    return {
      total: count,
      totalDuration: aggregates._sum.duration || 0,
      averageDuration: aggregates._avg.duration || 0,
      averageVolume: aggregates._avg.volume || 0,
    };
  }
}