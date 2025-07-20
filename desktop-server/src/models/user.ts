import { prisma, User, Prisma } from './index';

export class UserModel {
  /**
   * Create a new user
   */
  static async create(data: Prisma.UserCreateInput): Promise<User> {
    return prisma.user.create({
      data,
    });
  }

  /**
   * Find user by ID
   */
  static async findById(id: string): Promise<User | null> {
    return prisma.user.findUnique({
      where: { id },
      include: {
        soundButtons: true,
        soundboardGrids: true,
      },
    });
  }

  /**
   * Find user by username
   */
  static async findByUsername(username: string): Promise<User | null> {
    return prisma.user.findUnique({
      where: { username },
      include: {
        soundButtons: true,
        soundboardGrids: true,
      },
    });
  }

  /**
   * Find user by email
   */
  static async findByEmail(email: string): Promise<User | null> {
    return prisma.user.findUnique({
      where: { email },
    });
  }

  /**
   * Get all users with pagination
   */
  static async findMany(options: {
    skip?: number;
    take?: number;
    orderBy?: Prisma.UserOrderByWithRelationInput;
  } = {}): Promise<User[]> {
    const { skip = 0, take = 10, orderBy = { createdAt: 'desc' } } = options;
    
    return prisma.user.findMany({
      skip,
      take,
      orderBy,
      include: {
        _count: {
          select: {
            soundButtons: true,
            soundboardGrids: true,
          },
        },
      },
    });
  }

  /**
   * Update user
   */
  static async update(id: string, data: Prisma.UserUpdateInput): Promise<User> {
    return prisma.user.update({
      where: { id },
      data,
    });
  }

  /**
   * Delete user
   */
  static async delete(id: string): Promise<User> {
    return prisma.user.delete({
      where: { id },
    });
  }

  /**
   * Check if username exists
   */
  static async usernameExists(username: string): Promise<boolean> {
    const user = await prisma.user.findUnique({
      where: { username },
      select: { id: true },
    });
    return !!user;
  }

  /**
   * Check if email exists
   */
  static async emailExists(email: string): Promise<boolean> {
    const user = await prisma.user.findUnique({
      where: { email },
      select: { id: true },
    });
    return !!user;
  }

  /**
   * Get user statistics
   */
  static async getStats(userId: string): Promise<{
    soundButtonCount: number;
    soundboardGridCount: number;
    totalAudioDuration: number;
  }> {
    const [soundButtonCount, soundboardGridCount, soundButtons] = await Promise.all([
      prisma.soundButton.count({ where: { userId } }),
      prisma.soundboardGrid.count({ where: { userId } }),
      prisma.soundButton.findMany({
        where: { userId },
        select: { duration: true },
      }),
    ]);

    const totalAudioDuration = soundButtons.reduce(
      (total, button) => total + (button.duration || 0),
      0
    );

    return {
      soundButtonCount,
      soundboardGridCount,
      totalAudioDuration,
    };
  }
}