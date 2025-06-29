# AudioDeck Connect - Installation Guide
## Quick Start and Platform-Specific Setup
**Version:** 9.0  
**Date:** January 8, 2025  
**Platforms:** macOS, Windows, Linux

---

## 🚀 **QUICK START**

### **1-Minute Setup**
```bash
# Download latest release
curl -L https://releases.audiodeck.com/v9.0/audiodeck-server-macos-arm64 -o audiodeck-server

# Make executable and run
chmod +x audiodeck-server
./audiodeck-server

# Access in browser
open http://localhost:3001
```

**That's it!** AudioDeck Connect is now running with full enterprise features.

---

## 📋 **SYSTEM REQUIREMENTS**

### **Minimum Requirements**
| Component | Requirement |
|-----------|-------------|
| **Operating System** | macOS 11+, Windows 10+, Ubuntu 18.04+ |
| **Architecture** | x64 or ARM64 |
| **Memory** | 512MB RAM available |
| **Storage** | 200MB free disk space |
| **Network** | Internet connection for setup |
| **Audio** | System audio output device |

### **Recommended Requirements**
| Component | Recommendation |
|-----------|----------------|
| **Memory** | 1GB RAM available |
| **Storage** | 1GB free disk space |
| **Network** | Stable broadband connection |
| **Audio** | Dedicated audio interface (optional) |

---

## 🍎 **macOS INSTALLATION**

### **Automated Installation**
```bash
# Download installer
curl -L https://install.audiodeck.com/macos | bash

# Or manual download
curl -L https://releases.audiodeck.com/v9.0/audiodeck-server-macos-arm64 -o audiodeck-server
```

### **Step-by-Step Installation**

#### **1. Download and Verify**
```bash
# Create installation directory
mkdir -p ~/Applications/AudioDeck
cd ~/Applications/AudioDeck

# Download based on your Mac
# For Apple Silicon (M1/M2)
curl -L https://releases.audiodeck.com/v9.0/audiodeck-server-macos-arm64 -o audiodeck-server

# For Intel Macs
curl -L https://releases.audiodeck.com/v9.0/audiodeck-server-macos-x64 -o audiodeck-server

# Verify download
file audiodeck-server
# Expected: Mach-O 64-bit executable arm64/x86_64
```

#### **2. Permissions and First Run**
```bash
# Make executable
chmod +x audiodeck-server

# First run (may prompt for security permissions)
./audiodeck-server

# If blocked by Gatekeeper
xattr -d com.apple.quarantine audiodeck-server
./audiodeck-server
```

#### **3. System Integration**
```bash
# Add to PATH (optional)
echo 'export PATH="$HOME/Applications/AudioDeck:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Create launch agent (auto-start)
mkdir -p ~/Library/LaunchAgents
cat > ~/Library/LaunchAgents/com.audiodeck.server.plist << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.audiodeck.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>$HOME/Applications/AudioDeck/audiodeck-server</string>
    </array>
    <key>WorkingDirectory</key>
    <string>$HOME/Applications/AudioDeck</string>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
</dict>
</plist>
EOF

# Load launch agent
launchctl load ~/Library/LaunchAgents/com.audiodeck.server.plist
```

### **macOS-Specific Features**
- **Audio**: Native afplay integration for high-quality audio
- **Discovery**: Bonjour/mDNS for automatic device discovery
- **Notifications**: Native macOS notifications for events
- **Permissions**: Automatic microphone/audio permissions handling

---

## 🪟 **WINDOWS INSTALLATION**

### **Automated Installation**
```powershell
# Download and run installer
Invoke-WebRequest -Uri "https://install.audiodeck.com/windows.exe" -OutFile "AudioDeckInstaller.exe"
.\AudioDeckInstaller.exe
```

### **Manual Installation**

#### **1. Download and Setup**
```powershell
# Create installation directory
New-Item -ItemType Directory -Path "C:\Program Files\AudioDeck" -Force
Set-Location "C:\Program Files\AudioDeck"

# Download executable
Invoke-WebRequest -Uri "https://releases.audiodeck.com/v9.0/audiodeck-server-windows-x64.exe" -OutFile "audiodeck-server.exe"

# Verify download
Get-FileHash .\audiodeck-server.exe -Algorithm SHA256
```

#### **2. Windows Service Installation**
```powershell
# Install NSSM (Non-Sucking Service Manager)
# Download from: https://nssm.cc/download

# Install as Windows service
nssm install AudioDeckConnect "C:\Program Files\AudioDeck\audiodeck-server.exe"
nssm set AudioDeckConnect AppDirectory "C:\Program Files\AudioDeck"
nssm set AudioDeckConnect DisplayName "AudioDeck Connect Server"
nssm set AudioDeckConnect Description "Enterprise Audio Control Platform"
nssm set AudioDeckConnect Start SERVICE_AUTO_START

# Configure logging
nssm set AudioDeckConnect AppStdout "C:\Program Files\AudioDeck\logs\stdout.log"
nssm set AudioDeckConnect AppStderr "C:\Program Files\AudioDeck\logs\stderr.log"
nssm set AudioDeckConnect AppRotateFiles 1
nssm set AudioDeckConnect AppRotateOnline 1
nssm set AudioDeckConnect AppRotateSeconds 86400
nssm set AudioDeckConnect AppRotateBytes 1048576

# Start service
nssm start AudioDeckConnect

# Verify service status
Get-Service AudioDeckConnect
```

#### **3. Windows Firewall Configuration**
```powershell
# Add firewall exceptions
New-NetFirewallRule -DisplayName "AudioDeck Connect HTTP" -Direction Inbound -Protocol TCP -LocalPort 3001 -Action Allow
New-NetFirewallRule -DisplayName "AudioDeck Connect Discovery" -Direction Inbound -Protocol UDP -LocalPort 41234 -Action Allow

# Verify rules
Get-NetFirewallRule -DisplayName "*AudioDeck*"
```

### **Windows-Specific Features**
- **VoiceMeeter**: Advanced audio routing with VoiceMeeter integration
- **PowerShell**: Native PowerShell audio playback support
- **Windows Audio**: WASAPI integration for low-latency audio
- **Service**: Native Windows service integration

### **VoiceMeeter Setup (Optional)**
```powershell
# Download VoiceMeeter from: https://vb-audio.com/Voicemeeter/
# Install VoiceMeeter Banana (recommended)

# Verify VoiceMeeter integration
Invoke-RestMethod -Uri "http://localhost:3001/api/audio/voicemeeter/status"
```

---

## 🐧 **LINUX INSTALLATION**

### **Ubuntu/Debian Installation**
```bash
# Add AudioDeck repository
curl -fsSL https://packages.audiodeck.com/gpg | sudo apt-key add -
echo "deb https://packages.audiodeck.com/ubuntu $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/audiodeck.list

# Install package
sudo apt update
sudo apt install audiodeck-connect

# Start service
sudo systemctl enable audiodeck-connect
sudo systemctl start audiodeck-connect
```

### **Manual Installation**
```bash
# Create user and directories
sudo useradd --system --no-create-home --shell /bin/false audiodeck
sudo mkdir -p /opt/audiodeck/{bin,config,logs}

# Download executable
sudo curl -L https://releases.audiodeck.com/v9.0/audiodeck-server-linux-x64 -o /opt/audiodeck/bin/audiodeck-server
sudo chmod +x /opt/audiodeck/bin/audiodeck-server

# Set ownership
sudo chown -R audiodeck:audiodeck /opt/audiodeck
```

### **SystemD Service Setup**
```bash
# Create service file
sudo cat > /etc/systemd/system/audiodeck.service << EOF
[Unit]
Description=AudioDeck Connect Server
Documentation=https://docs.audiodeck.com
After=network.target sound.service

[Service]
Type=simple
User=audiodeck
Group=audiodeck
WorkingDirectory=/opt/audiodeck
ExecStart=/opt/audiodeck/bin/audiodeck-server
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=audiodeck

# Security settings
NoNewPrivileges=yes
PrivateTmp=yes
PrivateDevices=yes
ProtectHome=yes
ProtectSystem=strict
ReadWritePaths=/opt/audiodeck

# Environment
Environment=NODE_ENV=production
Environment=PORT=3001

[Install]
WantedBy=multi-user.target
EOF

# Reload and start service
sudo systemctl daemon-reload
sudo systemctl enable audiodeck
sudo systemctl start audiodeck

# Check status
sudo systemctl status audiodeck
```

### **Audio System Configuration**
```bash
# Install audio dependencies
sudo apt install alsa-utils pulseaudio-utils

# Test audio system
aplay /usr/share/sounds/alsa/Front_Left.wav

# Add audiodeck user to audio group
sudo usermod -a -G audio audiodeck

# Configure PulseAudio (if needed)
sudo -u audiodeck pulseaudio --check -v
```

### **Linux-Specific Features**
- **ALSA/PulseAudio**: Native Linux audio system support
- **SystemD**: Native service integration with logging
- **USB**: Direct USB device access for Android connectivity
- **Security**: Comprehensive security isolation

---

## 🔧 **CONFIGURATION**

### **Environment Configuration**
```bash
# Create config directory
mkdir -p ~/.audiodeck

# Create configuration file
cat > ~/.audiodeck/config.env << EOF
# Server Configuration
NODE_ENV=production
PORT=3001
DISCOVERY_PORT=41234

# Audio Configuration
AUDIO_QUALITY=high
AUDIO_BUFFER_SIZE=1024
DEFAULT_VOLUME=0.8

# Device Configuration
USB_MONITORING=true
ADB_ENABLED=true
ADB_PORT=5555

# Network Configuration
NETWORK_DISCOVERY=true
MDNS_SERVICE=_audiodeck._tcp

# Monitoring Configuration
HEALTH_CHECK_INTERVAL=30000
PERFORMANCE_MONITORING=true
LOG_LEVEL=info

# Security Configuration
CORS_ENABLED=true
RATE_LIMITING=true
MAX_UPLOAD_SIZE=10mb
EOF
```

### **Audio Assets Setup**
```bash
# Create audio directory
mkdir -p ~/.audiodeck/audio

# Download sample audio files
curl -L https://assets.audiodeck.com/samples/airhorn.mp3 -o ~/.audiodeck/audio/airhorn.mp3
curl -L https://assets.audiodeck.com/samples/applause.mp3 -o ~/.audiodeck/audio/applause.mp3
curl -L https://assets.audiodeck.com/samples/bell.wav -o ~/.audiodeck/audio/bell.wav

# Set permissions
chmod 644 ~/.audiodeck/audio/*
```

---

## 🧪 **INSTALLATION VERIFICATION**

### **Health Check**
```bash
# Wait for server startup (5-10 seconds)
sleep 10

# Test basic connectivity
curl http://localhost:3001/health

# Expected response:
{
  "status": "healthy",
  "timestamp": "2025-01-08T23:30:00Z",
  "version": "9.0.0",
  "uptime": 10,
  "services": {
    "audio": "operational",
    "devices": "operational", 
    "network": "operational"
  }
}
```

### **Functionality Tests**
```bash
# Test audio list
curl http://localhost:3001/api/audio/list

# Test device detection
curl http://localhost:3001/api/devices

# Test system status
curl http://localhost:3001/api/status
```

### **Performance Verification**
```bash
# Check memory usage
ps aux | grep audiodeck-server

# Check response time
time curl http://localhost:3001/health

# Check listening ports
netstat -tlnp | grep audiodeck-server
```

---

## 🔧 **TROUBLESHOOTING INSTALLATION**

### **Common Issues**

#### **Permission Denied (macOS)**
```bash
# Remove quarantine attribute
xattr -d com.apple.quarantine audiodeck-server

# Or grant permissions in System Preferences
# Security & Privacy > General > Allow anyway
```

#### **Port Already in Use**
```bash
# Find process using port 3001
sudo lsof -i :3001
sudo netstat -tlnp | grep :3001

# Kill conflicting process
sudo kill -9 [PID]

# Or change port in configuration
export PORT=3002
./audiodeck-server
```

#### **Audio Not Working**
```bash
# macOS: Check audio permissions
# System Preferences > Security & Privacy > Privacy > Microphone

# Linux: Check audio group membership
groups $USER | grep audio

# Windows: Check audio device configuration
# Control Panel > Sound > Playback devices
```

#### **Service Won't Start (Linux)**
```bash
# Check service logs
journalctl -u audiodeck --since "1 hour ago"

# Check file permissions
ls -la /opt/audiodeck/bin/audiodeck-server

# Check user permissions
sudo -u audiodeck /opt/audiodeck/bin/audiodeck-server --test
```

### **Getting Help**
- **Documentation**: https://docs.audiodeck.com
- **Community**: https://community.audiodeck.com
- **Issues**: https://github.com/audiodeck/issues
- **Support**: support@audiodeck.com

---

## 📱 **ANDROID APP SETUP**

### **App Installation**
```bash
# Download latest APK
curl -L https://releases.audiodeck.com/v9.0/audiodeck-android.apk -o audiodeck.apk

# Install via ADB
adb install audiodeck.apk

# Or install from Google Play Store
# https://play.google.com/store/apps/details?id=com.audiodeck.connect
```

### **Initial Configuration**
1. **Open AudioDeck Connect app**
2. **Enable necessary permissions**:
   - Network access
   - Audio recording (for voice commands)
   - USB debugging (for ADB connection)
3. **Connect to server**:
   - Enter server IP: `192.168.1.100:3001`
   - Or use auto-discovery
4. **Test connection**:
   - Tap "Test Audio" button
   - Verify audio playback

---

## 🎯 **NEXT STEPS**

### **Essential Setup**
1. **Configure audio files**: Upload your custom sounds
2. **Set up Android connection**: Pair your Android device
3. **Test all features**: Audio, device control, monitoring
4. **Configure monitoring**: Set up health checks and alerts

### **Advanced Setup**
1. **Production deployment**: Follow deployment guide
2. **Custom configuration**: Tune performance settings
3. **Integration**: Connect with existing systems
4. **Monitoring**: Set up comprehensive monitoring

### **Documentation**
- **[Technical Architecture](TECHNICAL_ARCHITECTURE.md)**: System design details
- **[API Specification](API_SPECIFICATION.md)**: Complete API documentation
- **[Deployment Guide](DEPLOYMENT_GUIDE.md)**: Production deployment
- **[Performance Guide](PERFORMANCE_GUIDE.md)**: Optimization strategies

---

*Installation Guide v9.0 - Enterprise-Grade Audio Control Platform*