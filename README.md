# Android Soundboard Application

A professional-grade Android soundboard application that connects wirelessly to your computer for remote audio control. Functions like a digital Elgato Streamdeck, perfect for content creators, streamers, podcasters, and live event managers.

## 🎯 Features

- **Wireless Connection**: Connect your Android tablet to your computer via WiFi
- **Low Latency**: Under 50ms audio trigger response time
- **Cross-Platform Server**: Works on Windows, macOS, and Linux
- **Customizable Layout**: 4x6 grid (24 buttons) with customizable colors and labels
- **Professional Audio**: Supports MP3, WAV, M4A, and OGG formats
- **Real-time Communication**: Socket.io for reliable, fast command transmission
- **Auto-Discovery**: mDNS service discovery for easy connection setup
- **Touch-Optimized**: Designed specifically for tablet interfaces

## 🏗️ Architecture

### Android App (Client)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Database**: Room for local configuration storage
- **Networking**: Socket.io client for real-time communication
- **Minimum SDK**: API 24 (Android 7.0)

### Computer Server
- **Runtime**: Node.js 18+
- **Framework**: Express.js with Socket.io
- **Audio**: Platform-specific native audio APIs
- **Discovery**: mDNS/Bonjour for automatic device discovery
- **Cross-Platform**: Windows, macOS, Linux support

## 🚀 Quick Start

### Prerequisites

#### For Android Development
- Android Studio Flamingo or later
- JDK 17
- Android device or emulator (API 24+)

#### For Server
- Node.js 18+ LTS
- npm or yarn

### Setup Instructions

#### 1. Android App Setup

```bash
# Clone the repository
git clone <repository-url>
cd "Android APP - Soundboard"

# Open in Android Studio
# Build and run the app on your device/emulator
```

#### 2. Server Setup

```bash
# Navigate to server directory
cd server

# Install dependencies
npm install

# Start the server
npm start
```

#### 3. Audio Setup

```bash
# Place your audio files in the server/audio directory
# Supported formats: MP3, WAV, M4A, OGG
cp your-audio-files.mp3 server/audio/
```

#### 4. Connection

1. Make sure your Android device and computer are on the same WiFi network
2. Start the server on your computer
3. Open the Android app
4. Tap the settings icon to connect
5. Enter your computer's IP address (displayed in server console)
6. Tap "Connect"

## 📱 Usage

### Connecting to Server
1. Launch the server on your computer
2. Note the IP address shown in the console
3. Open the Android app
4. Tap the settings (⚙️) icon
5. Enter the IP address and port (default: 8080)
6. Tap "Connect"

### Adding Sound Buttons
1. Place audio files in `server/audio/` directory
2. Use the "+" button in the Android app to add buttons
3. Assign audio files to grid positions
4. Customize button colors and labels

### Playing Sounds
1. Ensure you're connected to the server
2. Tap any configured sound button
3. Audio plays instantly on your computer

## 🎵 Audio File Management

### Supported Formats
- **MP3**: Most compatible, good compression
- **WAV**: Uncompressed, best quality, larger files
- **M4A**: Good compression, Apple ecosystem
- **OGG**: Open source, good compression

### File Placement
Place all audio files in the `server/audio/` directory:
```
server/
├── audio/
│   ├── applause.mp3
│   ├── drumroll.wav
│   ├── airhorn.mp3
│   └── your-sounds.wav
└── src/
```

## 🔧 Configuration

### Server Configuration
The server runs on port 8080 by default. You can change this:

```bash
# Set custom port
PORT=3000 npm start
```

### Android App Configuration
- Default grid: 4 columns × 6 rows (24 buttons)
- Customizable button colors and labels
- Connection history with favorites
- Local database for offline configuration

## 🌐 Network Requirements

- Both devices must be on the same local network
- Firewall should allow connections on the chosen port (default: 8080)
- For automatic discovery, mDNS/Bonjour must be enabled

## 🎛️ API Endpoints

### REST API
- `GET /health` - Server health check
- `GET /info` - Server information
- `GET /audio-files` - List available audio files
- `POST /play` - Play audio file (JSON payload)

### Socket.io Events
- `authenticate` - Client authentication
- `play_sound` - Play audio command
- `play_response` - Server response

## 🔒 Security

- Local network only (no internet required)
- Input validation and sanitization
- File path traversal protection
- Optional token-based authentication

## 🚨 Troubleshooting

### Connection Issues
1. **Cannot connect**: Verify both devices are on same WiFi network
2. **Firewall blocking**: Allow the server port through firewall
3. **Wrong IP**: Check server console for correct IP address

### Audio Issues
1. **No sound**: Check computer volume and audio output device
2. **File not found**: Verify audio files are in `server/audio/` directory
3. **Format not supported**: Convert to MP3 or WAV format

### Android Issues
1. **App crashes**: Check device has API 24+ (Android 7.0+)
2. **UI issues**: Ensure device is in landscape orientation
3. **Database errors**: Clear app data and restart

## 📊 Performance

- **Target Latency**: < 50ms button press to audio output
- **Connection Time**: < 5 seconds for initial setup
- **Memory Usage**: < 200MB on Android, < 100MB on server
- **Battery Optimization**: Efficient for extended tablet use

## 🛠️ Development

### Building Android App
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Running Server in Development
```bash
# Development mode with auto-restart
npm run dev

# Run tests
npm test
```

### Memory Bank Documentation
This project uses a Memory Bank system for documentation:
- `memory-bank/projectbrief.md` - Core requirements and goals
- `memory-bank/productContext.md` - User experience goals
- `memory-bank/systemPatterns.md` - Architecture decisions
- `memory-bank/techContext.md` - Technology stack and database schema
- `memory-bank/activeContext.md` - Current focus and next steps
- `memory-bank/progress.md` - Development progress and status

## 📋 Project Status

### ✅ Completed (Phase 1)
- Android project structure with Kotlin and Compose
- MVVM architecture with Repository pattern
- Room database with all entities and DAOs
- Core UI components and Material 3 theme
- Node.js server foundation with Express and Socket.io
- Cross-platform audio playback system
- mDNS service discovery

### 🔄 In Progress
- Network communication layer integration
- Real-time Socket.io connection between Android and server
- Audio file management and button configuration

### 📋 Next Steps
- Complete Socket.io integration in Android app
- Implement sound button creation and editing
- Add file upload/management features
- Performance optimization and testing
- Packaging and distribution

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Update Memory Bank documentation if needed
5. Submit a pull request

## 📄 License

This project is open source and available under the MIT License.

## 🎯 Use Cases

- **Live Streaming**: Trigger sound effects, music stings, voice clips
- **Podcasting**: Play intro/outro music, sound effects, transitions
- **Live Events**: Remote audio cue control for sound technicians
- **Music Production**: Trigger samples and backing tracks
- **Content Creation**: Add audio elements during video recording

---

**Ready to transform your content creation workflow? Get started with the Android Soundboard today!** 🎵📱💻 