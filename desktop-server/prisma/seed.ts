import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Starting database seeding...');

  // Create test users
  const testUser = await prisma.user.upsert({
    where: { username: 'testuser' },
    update: {},
    create: {
      username: 'testuser',
      email: 'test@example.com',
    },
  });

  const demoUser = await prisma.user.upsert({
    where: { username: 'demo' },
    update: {},
    create: {
      username: 'demo',
      email: 'demo@example.com',
    },
  });

  console.log('✅ Created users:', { testUser: testUser.username, demoUser: demoUser.username });

  // Create sample audio files
  const audioFiles = await Promise.all([
    prisma.audioFile.upsert({
      where: { id: 'audio-1' },
      update: {},
      create: {
        id: 'audio-1',
        filename: 'airhorn.mp3',
        filePath: '/audio/samples/airhorn.mp3',
        fileSize: 45678,
        duration: 2.5,
        format: 'mp3',
      },
    }),
    prisma.audioFile.upsert({
      where: { id: 'audio-2' },
      update: {},
      create: {
        id: 'audio-2',
        filename: 'applause.wav',
        filePath: '/audio/samples/applause.wav',
        fileSize: 123456,
        duration: 5.2,
        format: 'wav',
      },
    }),
    prisma.audioFile.upsert({
      where: { id: 'audio-3' },
      update: {},
      create: {
        id: 'audio-3',
        filename: 'drumroll.mp3',
        filePath: '/audio/samples/drumroll.mp3',
        fileSize: 78901,
        duration: 3.8,
        format: 'mp3',
      },
    }),
  ]);

  console.log('✅ Created audio files:', audioFiles.length);

  // Create sample sound buttons
  const soundButtons = await Promise.all([
    prisma.soundButton.create({
      data: {
        userId: testUser.id,
        name: 'Air Horn',
        filePath: '/audio/samples/airhorn.mp3',
        duration: 2.5,
        volume: 0.8,
        pitch: 1.0,
      },
    }),
    prisma.soundButton.create({
      data: {
        userId: testUser.id,
        name: 'Applause',
        filePath: '/audio/samples/applause.wav',
        duration: 5.2,
        volume: 0.9,
        pitch: 1.0,
      },
    }),
    prisma.soundButton.create({
      data: {
        userId: demoUser.id,
        name: 'Drum Roll',
        filePath: '/audio/samples/drumroll.mp3',
        duration: 3.8,
        volume: 0.7,
        pitch: 1.2,
      },
    }),
  ]);

  console.log('✅ Created sound buttons:', soundButtons.length);

  // Create sample soundboard grids
  const grids = await Promise.all([
    prisma.soundboardGrid.create({
      data: {
        userId: testUser.id,
        name: 'Default Grid',
        layoutConfig: JSON.stringify({
          rows: 3,
          columns: 4,
          buttonSize: 'medium',
          theme: 'dark',
        }),
      },
    }),
    prisma.soundboardGrid.create({
      data: {
        userId: demoUser.id,
        name: 'Demo Grid',
        layoutConfig: JSON.stringify({
          rows: 2,
          columns: 3,
          buttonSize: 'large',
          theme: 'light',
        }),
      },
    }),
  ]);

  console.log('✅ Created soundboard grids:', grids.length);

  console.log('🎉 Database seeding completed successfully!');
}

main()
  .catch((e) => {
    console.error('❌ Error during seeding:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });