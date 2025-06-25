# Cross-Platform Development Setup

This guide helps you set up the Android Soundboard project to work seamlessly across Windows, macOS, and Linux development environments.

## 🚀 Quick Setup

Run the automated setup script:

```bash
npm run setup
```

This will automatically:
- Detect your platform (Windows/macOS/Linux)
- Find Android SDK and JDK installations
- Configure ADB path for USB device detection
- Create platform-specific configuration files
- Install server dependencies
- Validate build environment

## 📋 Prerequisites

### All Platforms
- **Node.js 20+** - [Download from nodejs.org](https://nodejs.org/)
- **Android Studio 2024.2.1+** or **Android SDK Command Line Tools (API 35)**
- **JDK 21** - [Download OpenJDK 21](https://adoptium.net/temurin/releases/?version=21)

### Platform-Specific Requirements

#### Windows
- **PowerShell 5.1+** (usually pre-installed)
- **Android SDK** typically located at:
  - `%LOCALAPPDATA%\Android\Sdk`
  - `C:\Users\{username}\AppData\Local\Android\Sdk`

#### macOS
- **Xcode Command Line Tools**: `xcode-select --install`
- **Android SDK** typically located at:
  - `~/Library/Android/sdk`
  - `/usr/local/android-sdk`

#### Linux
- **Build essentials**: `sudo apt-get install build-essential` (Ubuntu/Debian)
- **Android SDK** typically located at:
  - `~/Android/Sdk`
  - `/opt/android-sdk`

## 🔧 Manual Configuration

If the automated setup doesn't work, you can configure manually:

### 1. Create local.properties

Create `local.properties` in the project root:

```properties
# Replace paths with your actual SDK and JDK locations
sdk.dir=/path/to/your/android-sdk
java.home=/path/to/your/jdk-21
```

### 2. Configure Server Environment

Create `server/.env`:

```bash
# Replace with your actual ADB path
ADB_PATH=/path/to/android-sdk/platform-tools/adb
PLATFORM=your_platform
NODE_ENV=development
```

### 3. Install Dependencies

```bash
# Install server dependencies
npm run install:server

# Or manually
cd server && npm install
```

## 🛠️ Building the Project

### Cross-Platform Build (Recommended)
```bash
npm run build
```

### Platform-Specific Builds

#### Windows
```bash
npm run build:windows
# Or directly
gradlew.bat clean assembleDebug
```

#### macOS/Linux
```bash
npm run build:unix
# Or directly
./gradlew clean assembleDebug
```

## 🖥️ Running the Server

### Quick Start
```bash
npm run server
```

### Development Mode (Auto-restart)
```bash
npm run server:dev
```

### Windows System Tray (Recommended for Windows)
```bash
# PowerShell (Recommended)
.\scripts\start-server-tray.ps1

# Batch file
.\scripts\start-server-tray.bat

# Background/Silent mode
.\scripts\start-server-tray.ps1 -Silent
```

### Standalone Executable
```bash
# Build executable
npm run build:server-exe

# Run the built .exe
.\dist\soundboard-server-win.exe
```

**Features of Windows Tray App:**
- 🔄 Right-click to start/stop/restart server
- 📊 System tray status indicator
- 📝 Automatic logging
- ⚙️ Settings and configuration
- 🌐 Quick access to server dashboard

## 📱 Device Connection

### USB Connection Setup

1. **Enable Developer Options** on your Android device:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times

2. **Enable USB Debugging**:
   - Go to Settings > Developer Options
   - Enable "USB Debugging"

3. **Connect via USB** and verify:
   ```bash
   # Check if device is detected
   adb devices
   ```

### Troubleshooting Device Connection

#### Windows
- Install USB drivers for your device
- Check Device Manager for any driver issues
- Try different USB ports/cables

#### macOS
- No additional drivers usually needed
- Check System Preferences > Security for any blocked connections

#### Linux
- Add udev rules for your device:
  ```bash
  echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"' | sudo tee /etc/udev/rules.d/51-android.rules
  sudo udevadm control --reload-rules
  ```

## 🔍 Platform-Specific Paths

The setup script automatically detects these paths:

### Windows
- **Android SDK**: `%LOCALAPPDATA%\Android\Sdk`
- **JDK**: `%USERPROFILE%\.jdks` or `%JAVA_HOME%`
- **ADB**: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`

### macOS
- **Android SDK**: `~/Library/Android/sdk`
- **JDK**: `/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
- **ADB**: `~/Library/Android/sdk/platform-tools/adb`

### Linux
- **Android SDK**: `~/Android/Sdk`
- **JDK**: `/usr/lib/jvm/java-21-openjdk`
- **ADB**: `~/Android/Sdk/platform-tools/adb`

## 🧪 Testing Your Setup

### 1. Verify Android Build
```bash
npm run clean
npm run build
```

### 2. Test Server
```bash
npm run server
# Server should start on http://localhost:3001
```

### 3. Test USB Connection
```bash
# Connect Android device via USB
adb devices
# Should show your device
```

### 4. Test App Connection
1. Install the built APK on your device
2. Start the server
3. Open the app and try connecting

## 🚨 Common Issues & Solutions

### ADB Not Found
```bash
# Check if ADB is in PATH
adb version

# If not found, add to PATH or set ADB_PATH environment variable
export ADB_PATH=/path/to/android-sdk/platform-tools/adb
```

### SDK License Issues
```bash
# Accept all SDK licenses
yes | sdkmanager --licenses
```

### Gradle Build Failures
```bash
# Clean and rebuild
npm run clean
npm run build

# Check Java version
java -version  # Should be 21.x
```

### Server Connection Issues
- Check firewall settings
- Ensure USB debugging is enabled
- Try different USB ports/cables
- Restart ADB: `adb kill-server && adb start-server`

## 📂 Project Structure

```
android-soundboard-app/
├── platform-config.json          # Platform-specific configurations
├── scripts/
│   ├── setup-environment.js      # Automated setup script
│   ├── build.sh                  # Unix build script (auto-generated)
│   └── build.bat                 # Windows build script (auto-generated)
├── server/
│   ├── .env                      # Server environment (auto-generated)
│   └── src/
├── app/                          # Android app source
├── local.properties              # SDK/JDK paths (auto-generated)
└── package.json                  # Cross-platform npm scripts
```

## 🔄 Switching Between Platforms

When moving between development machines:

1. **Pull latest code**:
   ```bash
   git pull
   ```

2. **Run setup**:
   ```bash
   npm run setup
   ```

3. **Verify build**:
   ```bash
   npm run build
   ```

The setup script will automatically detect the new platform and configure everything accordingly.

## 🆘 Getting Help

If you encounter issues:

1. **Check the setup output** for specific error messages
2. **Verify prerequisites** are installed correctly
3. **Check platform-specific troubleshooting** above
4. **Run setup again** after fixing any issues:
   ```bash
   npm run setup
   ```

## 🔧 Advanced Configuration

### Custom SDK/JDK Paths

Set environment variables before running setup:

```bash
# Windows
set ANDROID_HOME=C:\custom\android-sdk
set JAVA_HOME=C:\custom\jdk-21

# macOS/Linux
export ANDROID_HOME=/custom/android-sdk
export JAVA_HOME=/custom/jdk-21

npm run setup
```

### Custom ADB Path

```bash
# Set custom ADB path
export ADB_PATH=/custom/path/to/adb
npm run setup
```

### Development vs Production

The setup automatically configures for development. For production builds:

1. Update `platform-config.json` build settings
2. Create signed APK configurations
3. Set production environment variables

## 📋 Project Structure (Updated)

```
android-soundboard-app/
├── platform-config.json          # Platform-specific configurations (updated)
├── scripts/
│   ├── setup-environment.js      # Automated setup script (updated)
│   ├── build-server-exe.js       # Build Windows executable
│   ├── build-server-pkg.js       # Build with PKG
│   ├── server-tray.js            # Windows system tray app
│   ├── start-server-tray.ps1     # PowerShell tray launcher
│   ├── start-server-tray.bat     # Batch tray launcher
│   ├── build.sh                  # Unix build script (auto-generated)
│   └── build.bat                 # Windows build script (auto-generated)
├── server/
│   ├── .env                      # Server environment (auto-generated)
│   └── src/                      # Server source code
├── app/                          # Android app source
├── dist/                         # Built executables and packages
├── local.properties              # SDK/JDK paths (auto-generated)
├── SERVER_SETUP.md               # Detailed server setup guide
└── package.json                  # Cross-platform npm scripts (updated)
```

## 🆕 New Features

### Windows System Tray Application
- **Background Operation**: Server runs silently in system tray
- **Right-Click Controls**: Start, stop, restart server from tray menu
- **Auto-Start**: Automatically starts server when Windows starts
- **Log Management**: View and manage server logs
- **Status Indicators**: Visual feedback on server status
- **Settings Panel**: Configure port, auto-start, and other options

### Cross-Platform Executables
- **Windows**: Standalone .exe with tray functionality
- **macOS**: App bundle with dock integration  
- **Linux**: AppImage for universal compatibility

### Enhanced Setup Script
- **Latest SDK Detection**: Automatically finds Android SDK 35 and JDK 17/21
- **Dependency Management**: Handles all server and build dependencies
- **Validation**: Comprehensive environment validation
- **Platform Optimization**: Platform-specific optimizations

---

This setup ensures your Android Soundboard project works consistently across all platforms while maintaining optimal performance and compatibility. The new Windows tray application provides professional-grade server management for seamless daily use. 