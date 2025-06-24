# Technical Context: Android Soundboard Application

## Technologies Used

### Android Application
- **Language:** Kotlin
- **Minimum SDK:** API 24 (Android 7.0) for broad compatibility
- **Target SDK:** API 34 (Android 14)
- **UI Framework:** Jetpack Compose 1.5+
- **Architecture Components:** 
  - ViewModel
  - LiveData/StateFlow
  - Room Database
  - Navigation Compose
- **Networking:** 
  - Retrofit 2.9+ for REST API calls
  - OkHttp 4.11+ for HTTP client
  - Java Sockets for real-time communication
- **Audio:** ExoPlayer 2.19+ for local audio preview
- **Dependency Injection:** Hilt/Dagger
- **JSON Parsing:** Gson/Moshi

### Computer Server Application
- **Runtime:** Node.js 18+ LTS
- **Framework:** Express.js 4.18+
- **Real-time Communication:** Socket.io 4.7+
- **Audio Processing:** 
  - Windows: node-speaker + node-wav
  - macOS: afplay system command
  - Linux: aplay/paplay system commands
- **Cross-platform:** Electron 25+ for GUI (optional)
- **File System:** fs-extra for file operations
- **Network Discovery:** mdns module for service discovery

## Development Setup

### Cross-Platform Development Environment
- **Supported Platforms:** Windows, macOS, Linux
- **Automated Setup:** `npm run setup` - detects platform and configures environment
- **Configuration Files:** 
  - `platform-config.json` - platform-specific paths and settings
  - `scripts/setup-environment.js` - automated environment detection and setup
  - `local.properties` - auto-generated SDK/JDK paths
  - `server/.env` - auto-generated server environment with ADB path

### Android Development Environment
- **IDE:** Android Studio Flamingo or later
- **Build System:** Gradle 8.14.2+
- **Java Version:** JDK 17
- **Gradle Plugin:** Android Gradle Plugin 8.10.1+
- **Cross-Platform Build Scripts:**
  - Windows: `gradlew.bat clean assembleDebug`
  - macOS/Linux: `./gradlew clean assembleDebug`
  - Universal: `npm run build` (auto-detects platform)

### Computer Server Development
- **IDE:** VS Code or IntelliJ IDEA
- **Package Manager:** npm or yarn
- **Testing:** Jest for unit tests
- **Build:** Webpack for production builds
- **ADB Integration:** Cross-platform ADB path resolution for USB device detection

### Platform-Specific Configurations

#### Windows
- **SDK Path:** `%LOCALAPPDATA%\Android\Sdk`
- **JDK Path:** `%USERPROFILE%\.jdks` or `%JAVA_HOME%`
- **ADB Path:** `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`
- **Build Wrapper:** `gradlew.bat`

#### macOS
- **SDK Path:** `~/Library/Android/sdk`
- **JDK Path:** `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`
- **ADB Path:** `~/Library/Android/sdk/platform-tools/adb`
- **Build Wrapper:** `./gradlew`

#### Linux
- **SDK Path:** `~/Android/Sdk`
- **JDK Path:** `/usr/lib/jvm/java-17-openjdk`
- **ADB Path:** `~/Android/Sdk/platform-tools/adb`
- **Build Wrapper:** `./gradlew`

## Dependencies & Constraints

### Android App Dependencies
```kotlin
// Core Android
implementation "androidx.core:core-ktx:1.12.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
implementation "androidx.activity:activity-compose:1.8.0"

// Compose
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.ui:ui-tooling-preview:1.5.4"
implementation "androidx.compose.material3:material3:1.1.2"

// Architecture
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
implementation "androidx.navigation:navigation-compose:2.7.4"

// Room Database
implementation "androidx.room:room-runtime:2.6.0"
implementation "androidx.room:room-ktx:2.6.0"
kapt "androidx.room:room-compiler:2.6.0"

// Networking
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"
implementation "com.squareup.okhttp3:okhttp:4.11.0"

// Audio
implementation "androidx.media3:media3-exoplayer:1.1.1"
implementation "androidx.media3:media3-ui:1.1.1"

// Dependency Injection
implementation "com.google.dagger:hilt-android:2.48"
kapt "com.google.dagger:hilt-compiler:2.48"
```

### Server Dependencies
```json
{
  "dependencies": {
    "express": "^4.18.2",
    "socket.io": "^4.7.2",
    "cors": "^2.8.5",
    "multer": "^1.4.5",
    "fs-extra": "^11.1.1",
    "node-speaker": "^0.5.4",
    "node-wav": "^0.0.2",
    "mdns": "^2.7.2"
  }
}
```

## USB Connection Architecture
- **Connection Method:** USB cable with ADB (Android Debug Bridge)
- **Port Forwarding:** `adb forward tcp:8080 tcp:8080` for Socket.io communication
- **HTTP API:** RESTful endpoints on port 3000 (forwarded via ADB)
- **Data Format:** JSON for all command and configuration data
- **Requirements:** Android Developer Options enabled, USB Debugging enabled

## Security Considerations
- **Authentication:** Simple token-based authentication
- **Network:** Local network only (no internet required)
- **File Access:** Sandboxed audio file access on computer
- **Input Validation:** All network inputs validated and sanitized

## Performance Requirements
- **Audio Latency:** < 50ms from button press to audio output
- **Connection Time:** < 5 seconds for initial pairing
- **Memory Usage:** < 200MB on Android, < 100MB on computer
- **Battery Efficiency:** Optimized for tablet battery life during extended use

## Database Schema

### Migrations

#### Migration 2 → 3: Local File Support
```sql
ALTER TABLE sound_buttons ADD COLUMN is_local_file INTEGER NOT NULL DEFAULT 0;
```

### Android App Local Database (Room)

#### SoundButton Table
```sql
CREATE TABLE sound_buttons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    is_local_file INTEGER NOT NULL DEFAULT 0,
    position_x INTEGER NOT NULL,
    position_y INTEGER NOT NULL,
    color TEXT DEFAULT '#2196F3',
    icon_name TEXT,
    volume REAL DEFAULT 1.0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

#### SoundboardLayout Table
```sql
CREATE TABLE soundboard_layouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    is_active INTEGER DEFAULT 0,
    grid_columns INTEGER DEFAULT 4,
    grid_rows INTEGER DEFAULT 6,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

#### ConnectionHistory Table
```sql
CREATE TABLE connection_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    computer_name TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    port INTEGER NOT NULL,
    last_connected INTEGER NOT NULL,
    is_favorite INTEGER DEFAULT 0
);
``` 