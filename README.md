# AudioDeck Connect

Enterprise-grade audio control platform for professional sound management.

## Features

- Real-time audio control and playback
- Cross-platform compatibility (Android, Windows, macOS)
- Multiple connection methods:
  - WebSocket for low-latency communication
  - USB/ADB for Android devices
  - Network discovery via mDNS
- Enterprise-grade capabilities:
  - Zero mock data or fallbacks
  - Professional audio format support
  - Health monitoring and diagnostics
  - Real-time analytics
  - Secure connection handling

## System Requirements

### Server
- Node.js 22.x
- Windows, macOS, or Linux
- 22MB RAM minimum
- 52MB disk space

### Android App
- Android 8.0 or higher
- USB debugging (optional)
- Network connectivity

## Quick Start

1. Install dependencies:
```bash
npm install
```

2. Start the server:
```bash
npm start
```

3. Install the Android app from the Play Store or build from source:
```bash
cd android
./gradlew assembleDebug
```

## Architecture

AudioDeck Connect consists of two main components:

1. Server Application:
   - Express.js backend
   - Socket.IO for real-time communication
   - Native modules for hardware integration
   - Enterprise-grade monitoring

2. Android Client:
   - Material Design 3 UI
   - Kotlin/Jetpack Compose
   - Real-time audio control
   - Multiple connection methods

## Documentation

For detailed documentation, please visit our [documentation site](https://docs.audiodeck.connect).

## License

Copyright © 2024. All rights reserved. 