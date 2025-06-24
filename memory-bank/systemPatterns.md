# System Patterns: Android Soundboard Application

## Overall Architecture
**Pattern:** Client-Server Architecture with USB Cable Communication
- **Android App (Client):** Touch interface for triggering sounds
- **Computer Application (Server):** Receives commands and plays audio
- **Communication Protocol:** ADB TCP port forwarding over USB cable

## Key Technical Decisions

### Communication Architecture
- **Primary Protocol:** ADB TCP port forwarding over USB cable
- **Connection Method:** USB cable with Android Debug Bridge (ADB)
- **Port Forwarding:** `adb forward tcp:8080 tcp:8080` for Socket.io communication
- **Data Format:** JSON for command structure and metadata
- **Audio Streaming:** Command-based playback (no audio streaming over USB)

### Android App Architecture
- **Pattern:** MVVM (Model-View-ViewModel) with Repository pattern
- **UI Framework:** Jetpack Compose for modern, reactive UI
- **Networking:** Retrofit/OkHttp for HTTP operations, native sockets for real-time communication
- **Local Storage:** Room database for soundboard configurations
- **Audio Management:** MediaPlayer/ExoPlayer for local preview playback

### Computer Application Architecture
- **Cross-Platform:** Electron or Qt for Windows/Mac/Linux compatibility
- **Audio Backend:** Native audio APIs (WASAPI/CoreAudio/ALSA)
- **Server Component:** Node.js/Express for HTTP API, Socket.io for real-time communication
- **Audio Processing:** Native modules for low-latency audio playback

## Component Relationships
```
Android App → Network Layer → Computer Server → Audio System
     ↓              ↓              ↓              ↓
 UI Layer    ←→ Socket Layer ←→ Command Parser → Audio Player
     ↓              ↓              ↓              ↓
Data Layer    ←→ Config Sync ←→ File Manager  → System Audio
```

## Critical Implementation Paths

### Connection Flow
1. USB cable connection detection
2. ADB port forwarding setup (`adb forward tcp:8080 tcp:8080`)
3. Handshake and authentication via forwarded port
4. Configuration synchronization
5. Real-time command channel establishment

### Audio Trigger Flow
1. User touches button on Android app
2. Command sent via ADB-forwarded TCP socket (localhost:8080)
3. Computer receives and validates command
4. Audio file played through system audio output
5. Feedback sent back to Android app via same ADB connection

### Configuration Management
1. Soundboard layouts stored locally on Android
2. Audio files stored on computer
3. Sync mechanism for layout-to-file mapping
4. Backup/restore functionality for configurations

## Design Patterns Applied
- **Repository Pattern:** For data access abstraction
- **Observer Pattern:** For real-time status updates
- **Command Pattern:** For audio trigger actions
- **Strategy Pattern:** For different connection types
- **Factory Pattern:** For creating different audio players 