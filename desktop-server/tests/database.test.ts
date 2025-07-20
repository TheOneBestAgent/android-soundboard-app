import { describe, it, expect, beforeAll, afterAll, beforeEach } from '@jest/globals';
import { prisma } from '../src/models/index';
import { UserModel } from '../src/models/user';
import { SoundButtonModel } from '../src/models/soundButton';
import { AudioFileModel } from '../src/models/audioFile';
import { DatabaseUtils } from '../src/utils/database';

describe('Database Operations', () => {
  let testUserId: string;
  let testAudioFileId: string;
  let testSoundButtonId: string;

  beforeAll(async () => {
    // Ensure database is connected
    const isConnected = await DatabaseUtils.testConnection();
    expect(isConnected).toBe(true);
  });

  afterAll(async () => {
    // Clean up test data
    await prisma.soundButton.deleteMany({ where: { userId: testUserId } });
    await prisma.user.deleteMany({ where: { username: { startsWith: 'test_' } } });
    await prisma.audioFile.deleteMany({ where: { filename: { startsWith: 'test_' } } });
    await prisma.$disconnect();
  });

  beforeEach(async () => {
    // Clean up any existing test data
    await prisma.soundButton.deleteMany({ where: { user: { username: { startsWith: 'test_' } } } });
    await prisma.user.deleteMany({ where: { username: { startsWith: 'test_' } } });
    await prisma.audioFile.deleteMany({ where: { filename: { startsWith: 'test_' } } });
  });

  describe('User Model', () => {
    it('should create a new user', async () => {
      const userData = {
        username: 'test_user_1',
        email: 'test1@example.com',
      };

      const user = await UserModel.create(userData);
      testUserId = user.id;

      expect(user).toBeDefined();
      expect(user.username).toBe(userData.username);
      expect(user.email).toBe(userData.email);
      expect(user.id).toBeDefined();
    });

    it('should find user by ID', async () => {
      const userData = {
        username: 'test_user_2',
        email: 'test2@example.com',
      };

      const createdUser = await UserModel.create(userData);
      const foundUser = await UserModel.findById(createdUser.id);

      expect(foundUser).toBeDefined();
      expect(foundUser?.username).toBe(userData.username);
    });

    it('should find user by username', async () => {
      const userData = {
        username: 'test_user_3',
        email: 'test3@example.com',
      };

      await UserModel.create(userData);
      const foundUser = await UserModel.findByUsername(userData.username);

      expect(foundUser).toBeDefined();
      expect(foundUser?.username).toBe(userData.username);
    });

    it('should check if username exists', async () => {
      const userData = {
        username: 'test_user_4',
        email: 'test4@example.com',
      };

      await UserModel.create(userData);
      const exists = await UserModel.usernameExists(userData.username);
      const notExists = await UserModel.usernameExists('nonexistent_user');

      expect(exists).toBe(true);
      expect(notExists).toBe(false);
    });

    it('should update user', async () => {
      const userData = {
        username: 'test_user_5',
        email: 'test5@example.com',
      };

      const user = await UserModel.create(userData);
      const updatedUser = await UserModel.update(user.id, {
        email: 'updated5@example.com',
      });

      expect(updatedUser.email).toBe('updated5@example.com');
      expect(updatedUser.username).toBe(userData.username);
    });
  });

  describe('Audio File Model', () => {
    it('should create a new audio file', async () => {
      const audioData = {
        filename: 'test_audio.mp3',
        filePath: '/test/path/test_audio.mp3',
        fileSize: 1024,
        duration: 5.5,
        format: 'mp3',
      };

      const audioFile = await AudioFileModel.create(audioData);
      testAudioFileId = audioFile.id;

      expect(audioFile).toBeDefined();
      expect(audioFile.filename).toBe(audioData.filename);
      expect(audioFile.fileSize).toBe(audioData.fileSize);
      expect(audioFile.duration).toBe(audioData.duration);
    });

    it('should find audio file by file path', async () => {
      const audioData = {
        filename: 'test_audio_2.wav',
        filePath: '/test/path/test_audio_2.wav',
        fileSize: 2048,
        duration: 3.2,
        format: 'wav',
      };

      await AudioFileModel.create(audioData);
      const foundFile = await AudioFileModel.findByFilePath(audioData.filePath);

      expect(foundFile).toBeDefined();
      expect(foundFile?.filename).toBe(audioData.filename);
    });

    it('should search audio files by filename', async () => {
      const audioData1 = {
        filename: 'test_search_audio_1.mp3',
        filePath: '/test/path/test_search_audio_1.mp3',
        fileSize: 1024,
        duration: 2.0,
        format: 'mp3',
      };

      const audioData2 = {
        filename: 'test_search_audio_2.mp3',
        filePath: '/test/path/test_search_audio_2.mp3',
        fileSize: 1536,
        duration: 3.0,
        format: 'mp3',
      };

      await AudioFileModel.create(audioData1);
      await AudioFileModel.create(audioData2);
      
      const searchResults = await AudioFileModel.search('test_search');

      expect(searchResults).toHaveLength(2);
      expect(searchResults.some(f => f.filename === audioData1.filename)).toBe(true);
      expect(searchResults.some(f => f.filename === audioData2.filename)).toBe(true);
    });
  });

  describe('Sound Button Model', () => {
    beforeEach(async () => {
      // Create test user for sound button tests
      const user = await UserModel.create({
        username: 'test_sound_user',
        email: 'soundtest@example.com',
      });
      testUserId = user.id;
    });

    it('should create a new sound button', async () => {
      const buttonData = {
        user: { connect: { id: testUserId } },
        name: 'Test Button',
        filePath: '/test/path/button.mp3',
        duration: 2.5,
        volume: 0.8,
        pitch: 1.2,
      };

      const soundButton = await SoundButtonModel.create(buttonData);
      testSoundButtonId = soundButton.id;

      expect(soundButton).toBeDefined();
      expect(soundButton.name).toBe(buttonData.name);
      expect(soundButton.userId).toBe(testUserId);
      expect(soundButton.volume).toBe(buttonData.volume);
    });

    it('should find sound buttons by user ID', async () => {
      const buttonData1 = {
        user: { connect: { id: testUserId } },
        name: 'Test Button 1',
        filePath: '/test/path/button1.mp3',
        duration: 2.0,
      };

      const buttonData2 = {
        user: { connect: { id: testUserId } },
        name: 'Test Button 2',
        filePath: '/test/path/button2.mp3',
        duration: 3.0,
      };

      await SoundButtonModel.create(buttonData1);
      await SoundButtonModel.create(buttonData2);
      
      const userButtons = await SoundButtonModel.findByUserId(testUserId);

      expect(userButtons).toHaveLength(2);
      expect(userButtons.some(b => b.name === buttonData1.name)).toBe(true);
      expect(userButtons.some(b => b.name === buttonData2.name)).toBe(true);
    });

    it('should update sound button volume', async () => {
      const buttonData = {
        user: { connect: { id: testUserId } },
        name: 'Volume Test Button',
        filePath: '/test/path/volume_button.mp3',
        volume: 0.5,
      };

      const soundButton = await SoundButtonModel.create(buttonData);
      const updatedButton = await SoundButtonModel.updateVolume(soundButton.id, 0.9);

      expect(updatedButton.volume).toBe(0.9);
    });

    it('should search sound buttons by name', async () => {
      const buttonData = {
        user: { connect: { id: testUserId } },
        name: 'Searchable Button Name',
        filePath: '/test/path/searchable.mp3',
      };

      await SoundButtonModel.create(buttonData);
      const searchResults = await SoundButtonModel.search(testUserId, 'Searchable');

      expect(searchResults).toHaveLength(1);
      expect(searchResults[0].name).toBe(buttonData.name);
    });
  });

  describe('Database Utils', () => {
    it('should test database connection', async () => {
      const isConnected = await DatabaseUtils.testConnection();
      expect(isConnected).toBe(true);
    });

    it('should get database health info', async () => {
      const healthInfo = await DatabaseUtils.getHealthInfo();
      
      expect(healthInfo).toBeDefined();
      expect(healthInfo.connected).toBe(true);
      expect(healthInfo.version).toBeDefined();
      expect(healthInfo.tableCount).toBe(4);
      expect(typeof healthInfo.totalRecords).toBe('number');
    });

    it('should get database size', async () => {
      const sizeInfo = await DatabaseUtils.getDatabaseSize();
      
      expect(sizeInfo).toBeDefined();
      expect(typeof sizeInfo.sizeBytes).toBe('number');
      expect(typeof sizeInfo.sizeFormatted).toBe('string');
    });

    it('should handle Prisma errors', async () => {
      const error = new Error('Test error');
      const message = DatabaseUtils.handlePrismaError(error);
      
      expect(typeof message).toBe('string');
      expect(message).toBe('Test error');
    });
  });
});