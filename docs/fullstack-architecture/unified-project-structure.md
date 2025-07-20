# Unified Project Structure

```plaintext
android-soundboard-app/
├── .github/                    # CI/CD workflows
│   └── workflows/
│       ├── ci.yaml            # Continuous integration
│       ├── android-build.yaml # Android app build
│       ├── ios-build.yaml     # iOS app build
│       └── deploy.yaml        # Backend deployment
├── apps/
│   └── mobile/                # React Native application
│       ├── src/
│       │   ├── components/    # Reusable UI components
│       │   │   ├── common/    # Generic components
│       │   │   ├── audio/     # Audio-specific components
│       │   │   ├── soundboard/ # Soundboard components
│       │   │   ├── library/   # Library components
│       │   │   ├── streaming/ # Streaming components
│       │   │   ├── ai/        # AI processing components
│       │   │   └── settings/  # Settings components
│       │   ├── screens/       # Screen components
│       │   │   ├── SoundboardScreen/
│       │   │   ├── LibraryScreen/
│       │   │   ├── SettingsScreen/
│       │   │   ├── StreamingScreen/
│       │   │   └── AuthScreen/
│       │   ├── navigation/    # Navigation configuration
│       │   │   ├── RootNavigator.tsx
│       │   │   ├── AuthNavigator.tsx
│       │   │   └── MainNavigator.tsx
│       │   ├── services/      # API and native services
│       │   │   ├── api/       # API client services
│       │   │   ├── audio/     # Audio processing services
│       │   │   ├── ai/        # AI processing services
│       │   │   ├── streaming/ # Streaming services
│       │   │   └── storage/   # Local storage services
│       │   ├── stores/        # Zustand state stores
│       │   │   ├── authStore.ts
│       │   │   ├── soundboardStore.ts
│       │   │   ├── audioStore.ts
│       │   │   ├── libraryStore.ts
│       │   │   ├── streamingStore.ts
│       │   │   └── aiStore.ts
│       │   ├── hooks/         # Custom React hooks
│       │   │   ├── useAuth.ts
│       │   │   ├── useAudioPlayer.ts
│       │   │   ├── useAIProcessor.ts
│       │   │   ├── useStreaming.ts
│       │   │   └── useTheme.ts
│       │   ├── utils/         # Utility functions
│       │   │   ├── audio.ts
│       │   │   ├── validation.ts
│       │   │   ├── storage.ts
│       │   │   └── constants.ts
│       │   ├── types/         # TypeScript definitions
│       │   │   ├── audio.ts
│       │   │   ├── user.ts
│       │   │   ├── api.ts
│       │   │   └── navigation.ts
│       │   └── constants/     # App constants
│       │       ├── colors.ts
│       │       ├── dimensions.ts
│       │       └── config.ts
│       ├── android/           # Android-specific code
│       │   ├── app/src/main/java/com/soundboard/
│       │   │   ├── audio/     # Native audio modules
│       │   │   │   ├── AudioEngineModule.java
│       │   │   │   ├── AudioPlayerModule.java
│       │   │   │   └── AudioRecorderModule.java
│       │   │   ├── ai/        # OpenVINO integration
│       │   │   │   ├── OpenVINOModule.java
│       │   │   │   └── AIProcessorModule.java
│       │   │   ├── voicemeeter/ # VoiceMeeter bridge
│       │   │   │   └── VoiceMeeterModule.java
│       │   │   └── streaming/ # Streaming integration
│       │   │       └── StreamingModule.java
│       │   ├── app/src/main/cpp/ # Native C++ code
│       │   │   ├── audio/     # Audio processing
│       │   │   ├── ai/        # OpenVINO inference
│       │   │   └── jni/       # JNI bridges
│       │   └── build.gradle
│       ├── ios/               # iOS-specific code
│       │   ├── SoundboardApp/
│       │   │   ├── Audio/     # Native audio modules
│       │   │   │   ├── AudioEngineModule.h/m
│       │   │   │   ├── AudioPlayerModule.h/m
│       │   │   │   └── AudioRecorderModule.h/m
│       │   │   ├── AI/        # OpenVINO integration
│       │   │   │   ├── OpenVINOModule.h/m
│       │   │   │   └── AIProcessorModule.h/m
│       │   │   ├── VoiceMeeter/ # VoiceMeeter bridge
│       │   │   │   └── VoiceMeeterModule.h/m
│       │   │   └── Streaming/ # Streaming integration
│       │   │       └── StreamingModule.h/m
│       │   └── Podfile
│       ├── __tests__/         # Test files
│       │   ├── components/
│       │   ├── screens/
│       │   ├── services/
│       │   ├── stores/
│       │   └── utils/
│       ├── metro.config.js    # Metro bundler config
│       ├── babel.config.js    # Babel configuration
│       ├── react-native.config.js # RN configuration
│       └── package.json
├── desktop-server/           # Local Desktop Server
│   ├── src/
│   │   ├── main.ts           # Electron main process
│   │   ├── tray.ts           # System tray integration
│   │   ├── routes/           # API route handlers
│   │   │   ├── auth.ts
│   │   │   ├── soundboards.ts
│   │   │   ├── audio.ts
│   │   │   ├── library.ts
│   │   │   ├── streaming.ts
│   │   │   ├── ai.ts
│   │   │   └── index.ts
│   │   ├── controllers/      # Request controllers
│   │   │   ├── AuthController.ts
│   │   │   ├── SoundboardController.ts
│   │   │   ├── AudioController.ts
│   │   │   ├── LibraryController.ts
│   │   │   ├── StreamingController.ts
│   │   │   └── AIController.ts
│   │   ├── services/         # Business logic services
│   │   │   ├── AuthService.ts
│   │   │   ├── SoundboardService.ts
│   │   │   ├── AudioService.ts
│   │   │   ├── LibraryService.ts
│   │   │   ├── StreamingService.ts
│   │   │   ├── AIService.ts
│   │   │   └── TrayService.ts
│   │   ├── repositories/     # Data access layer
│   │   │   ├── UserRepository.ts
│   │   │   ├── SoundboardRepository.ts
│   │   │   ├── AudioRepository.ts
│   │   │   └── AIJobRepository.ts
│   │   ├── models/           # Database models
│   │   │   ├── User.ts
│   │   │   ├── Soundboard.ts
│   │   │   ├── SoundButton.ts
│   │   │   ├── AudioLibrary.ts
│   │   │   └── AIProcessingJob.ts
│   │   ├── middleware/       # Express middleware
│   │   │   ├── auth.ts
│   │   │   ├── validation.ts
│   │   │   ├── errorHandler.ts
│   │   │   ├── rateLimit.ts
│   │   │   ├── cors.ts
│   │   │   └── logging.ts
│   │   ├── audio/            # Audio engine integration
│   │   │   ├── engine.ts     # Web Audio API wrapper
│   │   │   ├── speaker.ts    # Node-speaker integration
│   │   │   ├── recorder.ts   # Audio recording
│   │   │   └── effects.ts    # Audio effects processing
│   │   ├── utils/            # Desktop server utilities
│   │   │   ├── database.ts   # SQLite utilities
│   │   │   ├── storage.ts    # Local file storage
│   │   │   ├── logger.ts
│   │   │   ├── errors.ts
│   │   │   ├── validation.ts
│   │   │   └── crypto.ts
│   │   ├── config/           # Configuration
│   │   │   ├── database.ts   # SQLite configuration
│   │   │   ├── audio.ts      # Audio engine config
│   │   │   ├── tray.ts       # System tray config
│   │   │   └── environment.ts
│   │   ├── websocket/        # WebSocket handlers
│   │   │   ├── streamingHandler.ts
│   │   │   ├── collaborationHandler.ts
│   │   │   └── aiProcessingHandler.ts
│   │   └── server.ts         # Express server entry
│   ├── tests/                # Desktop server tests
│   │   ├── unit/
│   │   ├── integration/
│   │   └── e2e/
│   ├── database/             # SQLite database
│   │   ├── schema.sql        # Database schema
│   │   ├── migrations/       # Migration scripts
│   │   └── seed.ts           # Seed data
│   ├── assets/               # Desktop app assets
│   │   ├── icons/            # System tray icons
│   │   └── sounds/           # Default sounds
│   ├── electron.config.js    # Electron configuration
│   └── package.json
├── packages/                  # Shared packages
│   ├── shared/               # Shared types and utilities
│   │   ├── src/
│   │   │   ├── types/        # Common TypeScript interfaces
│   │   │   │   ├── audio.ts
│   │   │   │   ├── user.ts
│   │   │   │   ├── api.ts
│   │   │   │   └── streaming.ts
│   │   │   ├── constants/    # Shared constants
│   │   │   │   ├── audio.ts
│   │   │   │   ├── api.ts
│   │   │   │   └── errors.ts
│   │   │   ├── utils/        # Shared utility functions
│   │   │   │   ├── validation.ts
│   │   │   │   ├── audio.ts
│   │   │   │   └── formatting.ts
│   │   │   └── api/          # API client and types
│   │   │       ├── client.ts
│   │   │       ├── types.ts
│   │   │       └── endpoints.ts
│   │   ├── tests/
│   │   └── package.json
│   ├── audio-engine/         # Audio processing core
│   │   ├── src/
│   │   │   ├── processors/   # Audio effect processors
│   │   │   │   ├── ReverbProcessor.ts
│   │   │   │   ├── EchoProcessor.ts
│   │   │   │   ├── DistortionProcessor.ts
│   │   │   │   └── FilterProcessor.ts
│   │   │   ├── streaming/    # Real-time streaming
│   │   │   │   ├── StreamManager.ts
│   │   │   │   ├── AudioBuffer.ts
│   │   │   │   └── Encoder.ts
│   │   │   ├── ai/           # AI audio processing
│   │   │   │   ├── NoiseReduction.ts
│   │   │   │   ├── VoiceEnhancement.ts
│   │   │   │   └── AutoMastering.ts
│   │   │   ├── formats/      # Audio format handlers
│   │   │   │   ├── MP3Handler.ts
│   │   │   │   ├── WAVHandler.ts
│   │   │   │   ├── FLACHandler.ts
│   │   │   │   └── OGGHandler.ts
│   │   │   └── core/         # Core audio engine
│   │   │       ├── AudioEngine.ts
│   │   │       ├── AudioContext.ts
│   │   │       └── AudioNode.ts
│   │   ├── tests/
│   │   └── package.json
│   └── config/               # Shared configuration
│       ├── eslint/
│       │   ├── base.js
│       │   ├── react-native.js
│       │   └── node.js
│       ├── typescript/
│       │   ├── base.json
│       │   ├── react-native.json
│       │   └── node.json
│       ├── jest/
│       │   ├── base.js
│       │   ├── react-native.js
│       │   └── node.js
│       └── prettier/
│           └── .prettierrc.js
├── deployment/               # Deployment and Build Configuration
│   ├── electron/             # Electron build configuration
│   │   ├── build/            # Build scripts and configs
│   │   │   ├── windows.js    # Windows build configuration
│   │   │   ├── macos.js      # macOS build configuration
│   │   │   ├── linux.js      # Linux build configuration
│   │   │   └── portable.js   # Portable build configuration
│   │   ├── installer/        # Installer configurations
│   │   │   ├── windows.nsh   # NSIS installer script
│   │   │   ├── macos.dmg     # DMG configuration
│   │   │   └── linux.deb     # Debian package config
│   │   ├── auto-updater/     # Auto-update configuration
│   │   │   ├── update-server.js
│   │   │   ├── signature.js
│   │   │   └── channels.json
│   │   └── electron-builder.json # Main build configuration
│   │   └── package.json
│   ├── docker/               # Docker configurations
│   │   ├── backend/
│   │   │   ├── Dockerfile
│   │   │   └── .dockerignore
│   │   └── nginx/
│   │       ├── Dockerfile
│   │       └── nginx.conf
│   └── scripts/              # Deployment scripts
│       ├── deploy.sh
│       ├── rollback.sh
│       └── migrate.sh
├── docs/                      # Documentation
│   ├── prd_rewrite_v0.4.md
│   ├── ui-brainstorming-session.md
│   ├── fullstack-architecture.md
│   ├── api-documentation.md
│   ├── deployment-guide.md
│   └── development-setup.md
├── scripts/                   # Build and utility scripts
│   ├── build.sh
│   ├── test.sh
│   ├── lint.sh
│   └── setup.sh
├── .env.example               # Environment template
├── .gitignore                 # Git ignore rules
├── package.json               # Root package.json
├── yarn.lock                  # Dependency lock file
├── lerna.json                 # Monorepo configuration
├── tsconfig.json              # TypeScript configuration
├── jest.config.js             # Jest configuration
├── .eslintrc.js               # ESLint configuration
├── .prettierrc                # Prettier configuration
└── README.md                  # Project documentation
```

---
