# Desktop Server - Soundboard Application

A Node.js/Express backend server with SQLite database for the soundboard application, providing REST APIs for managing users, sound buttons, audio files, and soundboard grids.

## Features

- **Database Management**: SQLite with Prisma ORM
- **RESTful APIs**: Express.js server with TypeScript
- **Audio File Management**: Upload, storage, and metadata handling
- **User Management**: User accounts and authentication
- **Sound Button Management**: Create, update, and organize sound buttons
- **Soundboard Grids**: Customizable grid layouts for sound buttons
- **Health Monitoring**: Database health checks and system monitoring
- **Testing**: Comprehensive test suite with Jest
- **Code Quality**: ESLint, Prettier, and TypeScript for code quality

## Prerequisites

- Node.js 18+ 
- npm or yarn
- SQLite3

## Installation

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Set up environment variables**:
   ```bash
   cp .env.example .env
   ```
   Edit `.env` with your configuration.

3. **Generate Prisma client**:
   ```bash
   npm run db:generate
   ```

4. **Set up database**:
   ```bash
   npm run db:migrate
   ```

5. **Seed database (optional)**:
   ```bash
   npm run db:seed
   ```

## Development

### Start Development Server
```bash
npm run dev
```

### Build for Production
```bash
npm run build
```

### Start Production Server
```bash
npm start
```

## Testing

### Run All Tests
```bash
npm test
```

### Run Tests in Watch Mode
```bash
npm run test:watch
```

### Generate Coverage Report
```bash
npm run test:coverage
```

## Code Quality

### Linting
```bash
npm run lint
npm run lint:fix
```

### Formatting
```bash
npm run format\```

### Type Checking
```bash
npm run type-check
```

## Database Management

### Generate Prisma Client
```bash
npm run db:generate
```

### Run Migrations
```bash
npm run db:migrate
```

### Reset Database
```bash
npm run db:reset
```

### Seed Database
```bash
npm run db:seed
```

### Open Prisma Studio
```bash
npm run db:studio
```

## API Endpoints

### Health Check
- `GET /health` - Server health status
- `GET /health/db` - Database health status

### Users
- `GET /api/users` - List all users
- `GET /api/users/:id` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/:id` - Update user
- `DELETE /api/users/:id` - Delete user

### Sound Buttons
- `GET /api/sound-buttons` - List sound buttons
- `GET /api/sound-buttons/:id` - Get sound button by ID
- `POST /api/sound-buttons` - Create sound button
- `PUT /api/sound-buttons/:id` - Update sound button
- `DELETE /api/sound-buttons/:id` - Delete sound button

### Audio Files
- `GET /api/audio-files` - List audio files
- `GET /api/audio-files/:id` - Get audio file by ID
- `POST /api/audio-files` - Upload audio file
- `DELETE /api/audio-files/:id` - Delete audio file

### Soundboard Grids
- `GET /api/soundboard-grids` - List soundboard grids
- `GET /api/soundboard-grids/:id` - Get grid by ID
- `POST /api/soundboard-grids` - Create grid
- `PUT /api/soundboard-grids/:id` - Update grid
- `DELETE /api/soundboard-grids/:id` - Delete grid

## Project Structure

```
desktop-server/
├── src/
│   ├── models/           # Database models and operations
│   │   ├── index.ts      # Prisma client setup
│   │   ├── user.ts       # User model operations
│   │   ├── soundButton.ts # Sound button operations
│   │   └── audioFile.ts  # Audio file operations
│   ├── utils/            # Utility functions
│   │   └── database.ts   # Database utilities
│   └── types/            # TypeScript type definitions
├── prisma/
│   ├── schema.prisma     # Database schema
│   └── seed.ts           # Database seeding script
├── tests/                # Test files
│   ├── database.test.ts  # Database operation tests
│   ├── setup.ts          # Test setup
│   ├── globalSetup.ts    # Global test setup
│   └── globalTeardown.ts # Global test cleanup
├── data/                 # SQLite database files
├── logs/                 # Application logs
├── uploads/              # Uploaded audio files
└── dist/                 # Compiled JavaScript output
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|----------|
| `DATABASE_URL` | SQLite database file path | `file:./data/soundboard.db` |
| `PORT` | Server port | `3001` |
| `NODE_ENV` | Environment mode | `development` |
| `LOG_LEVEL` | Logging level | `info` |
| `AUDIO_UPLOAD_PATH` | Audio file upload directory | `./uploads/audio` |
| `MAX_AUDIO_FILE_SIZE` | Maximum audio file size | `50MB` |
| `ALLOWED_AUDIO_FORMATS` | Allowed audio formats | `mp3,wav,ogg,m4a` |

## Contributing

1. Follow the existing code style and conventions
2. Write tests for new features
3. Run linting and formatting before committing
4. Ensure all tests pass
5. Update documentation as needed

## License

This project is part of the Android Soundboard Application.