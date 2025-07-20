# High-Level Architecture

## Technical Summary
The application follows a hybrid architecture combining:
- **Frontend:** React Native with TypeScript for cross-platform mobile development
- **Audio Engine:** Native modules with React Native Audio API for low-latency processing
- **AI Processing:** OpenVINO runtime integrated via native bridges
- **State Management:** Zustand for lightweight, performant state management
- **Desktop Server:** Electron-based local server for audio processing and data management
- **Database:** SQLite for local storage and user data
- **Real-time Communication:** WebSocket connections for live streaming features

## Platform and Infrastructure Choice

**Mobile Platform:**
- **Framework:** React Native 0.73+
- **Language:** TypeScript for type safety and developer experience
- **Build System:** Metro bundler with custom native module integration
- **Deployment:** Google Play Store (Android), Apple App Store (iOS)

**Desktop Server Infrastructure:**
- **Runtime:** Electron with Node.js backend for local server functionality
- **Database:** SQLite for user data and audio metadata
- **File Storage:** Local file system for audio files and user content
- **Audio Engine:** Web Audio API with Node-speaker for audio processing
- **System Integration:** System tray integration and VoiceMeeter control

**AI/ML Infrastructure:**
- **Framework:** OpenVINO 2023.3+ for cross-platform AI inference
- **Models:** Custom audio processing models (noise reduction, enhancement)
- **Deployment:** Embedded models in mobile app, local desktop processing for heavy operations

## Repository Structure
```
android-soundboard-app/
├── .github/                    # CI/CD workflows
│   └── workflows/
│       ├── ci.yaml
│       ├── android-build.yaml
│       └── ios-build.yaml
├── apps/
│   └── mobile/                 # React Native application
│       ├── src/
│       │   ├── components/     # Reusable UI components
│       │   ├── screens/        # Screen components
│       │   ├── navigation/     # Navigation configuration
│       │   ├── services/       # API and native module services
│       │   ├── stores/         # Zustand state stores
│       │   ├── hooks/          # Custom React hooks
│       │   ├── utils/          # Utility functions
│       │   ├── types/          # TypeScript type definitions
│       │   └── constants/      # App constants
│       ├── android/            # Android-specific code
│       │   ├── app/src/main/java/
│       │   │   └── com/soundboard/
│       │   │       ├── audio/  # Native audio modules
│       │   │       ├── ai/     # OpenVINO integration
│       │   │       └── voicemeeter/ # VoiceMeeter bridge
│       │   └── build.gradle
│       ├── ios/                # iOS-specific code
│       │   ├── SoundboardApp/
│       │   │   ├── Audio/      # Native audio modules
│       │   │   ├── AI/         # OpenVINO integration
│       │   │   └── VoiceMeeter/ # VoiceMeeter bridge
│       │   └── Podfile
│       ├── __tests__/          # Test files
│       └── package.json
├── packages/
│   ├── shared/                 # Shared types and utilities
│   │   ├── src/
│   │   │   ├── types/          # Common TypeScript interfaces
│   │   │   ├── constants/      # Shared constants
│   │   │   ├── utils/          # Shared utility functions
│   │   │   └── api/            # API client and types
│   │   └── package.json
│   ├── audio-engine/           # Audio processing core
│   │   ├── src/
│   │   │   ├── processors/     # Audio effect processors
│   │   │   ├── streaming/      # Real-time streaming
│   │   │   ├── ai/             # AI audio processing
│   │   │   └── formats/        # Audio format handlers
│   │   └── package.json
│   └── config/                 # Shared configuration
│       ├── eslint/
│       ├── typescript/
│       └── jest/
├── desktop-server/             # Electron desktop server
│   ├── src/
│   │   ├── main.ts             # Electron main process
│   │   ├── tray.ts             # System tray integration
│   │   ├── services/           # Desktop server services
│   │   │   ├── TrayService.ts  # System tray management
│   │   │   ├── AudioService.ts # Audio processing service
│   │   │   └── DatabaseService.ts # SQLite database service
│   │   ├── audio/              # Audio processing modules
│   │   │   ├── WebAudioEngine.ts # Web Audio API integration
│   │   │   └── NodeSpeaker.ts  # Node-speaker integration
│   │   ├── utils/              # Desktop utilities
│   │   ├── config/             # Configuration files
│   │   └── database/           # SQLite schema and migrations
│   │       ├── schema.sql      # Database schema
│   │       └── migrations/     # Database migrations
│   ├── assets/                 # Desktop app assets
│   │   ├── icons/              # Application icons
│   │   └── sounds/             # Default sound files
│   ├── tests/                  # Desktop server tests
│   ├── electron.config.js      # Electron configuration
│   └── package.json
├── deployment/                 # Deployment configurations
│   ├── electron/               # Electron build configs
│   │   ├── build/              # Build configurations
│   │   │   ├── windows.json    # Windows build config
│   │   │   ├── macos.json      # macOS build config
│   │   │   └── linux.json      # Linux build config
│   │   ├── installers/         # Installer configurations
│   │   │   ├── nsis.nsi        # Windows NSIS installer
│   │   │   ├── dmg.json        # macOS DMG configuration
│   │   │   └── deb.json        # Debian package config
│   │   └── auto-updater/       # Auto-update configurations
│   │       ├── update-server.js # Update server
│   │       ├── signature.json  # Code signing config
│   │       └── channels.json   # Update channels
│   ├── scripts/                # Deployment scripts
│   └── electron-builder.json   # Main build configuration
├── docs/                       # Documentation
│   ├── prd_rewrite_v0.4.md
│   ├── ui-brainstorming-session.md
│   └── fullstack-architecture.md
├── .env.example                # Environment template
├── package.json                # Root package.json
├── yarn.lock                   # Dependency lock file
└── README.md
```

---
