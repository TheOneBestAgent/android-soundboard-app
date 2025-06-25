# 🖥️ Soundboard Server Setup Guide

This guide covers setting up and running the Soundboard Server on your computer, including the new Windows system tray application.

## 🚀 Quick Start

### Option 1: Automated Setup (Recommended)
```bash
npm run setup
npm run server
```

### Option 2: Windows Tray Application
```bash
# Install dependencies
npm install

# Start server with system tray
.\scripts\start-server-tray.ps1

# Or use the batch file
.\scripts\start-server-tray.bat
```

### Option 3: Build Standalone Executable
```bash
# Build Windows .exe file
npm run build:server-exe

# The executable will be in dist/ folder
```

## 📋 Prerequisites

- **Node.js 20+** - [Download from nodejs.org](https://nodejs.org/)
- **Git** (for cloning/updates)
- **Windows 10/11** (for tray application)

## 🖥️ Server Options

### 1. Standard Server
Run the server in a terminal window:
```bash
cd server
npm start
```

### 2. Development Server
Run with auto-restart on file changes:
```bash
cd server
npm run dev
```

### 3. System Tray Server (Windows)
Run as a background service with tray controls:

#### PowerShell (Recommended):
```powershell
.\scripts\start-server-tray.ps1
```

#### Batch File:
```cmd
.\scripts\start-server-tray.bat
```

#### Options:
```powershell
# Silent background start
.\scripts\start-server-tray.ps1 -Silent

# Custom port
.\scripts\start-server-tray.ps1 -Port 8080

# Without system tray (console only)
.\scripts\start-server-tray.ps1 -NoTray
```

### 4. Standalone Executable
Build a single .exe file that includes everything:

```bash
# Build the executable
npm run build:server-exe

# Run the built executable
.\dist\soundboard-server-win.exe
```

## 🎛️ Tray Application Features

When using the Windows tray application, you get:

### **Right-Click Menu Options:**
- 🟢 **Server Status** - Shows if server is running
- ▶️ **Start/Stop Server** - Toggle server state  
- 🔄 **Restart Server** - Restart the server process
- 📊 **Show Dashboard** - Open web-based dashboard
- 🌐 **Open Server URL** - Launch server in browser
- 📝 **View Logs** - Open log file
- ⚙️ **Settings** - Configure auto-start and port
- ❓ **About** - Application information
- ❌ **Quit** - Exit completely

### **Features:**
- **Auto-start** - Server starts automatically when tray app launches
- **Background operation** - Runs silently in system tray
- **Log management** - Automatic logging to temp directory
- **Error handling** - Graceful restart on crashes
- **Port configuration** - Easy port changes through settings
- **Visual indicators** - Tray icon shows server status

## 🔧 Configuration

### Environment Variables
Create a `.env` file in the `server/` directory:

```env
# Server Configuration
PORT=3001
NODE_ENV=development

# Android Debugging
ADB_PATH=/path/to/adb
PLATFORM=windows

# Logging
LOG_LEVEL=info
LOG_FILE=./logs/server.log

# Network
CORS_ORIGIN=*
SOCKET_TIMEOUT=30000
```

### Server Settings
Modify `server/src/config.js` for advanced configuration:

```javascript
module.exports = {
  port: process.env.PORT || 3001,
  cors: {
    origin: process.env.CORS_ORIGIN || "*",
    credentials: true
  },
  socket: {
    timeout: parseInt(process.env.SOCKET_TIMEOUT) || 30000,
    pingInterval: 25000,
    pingTimeout: 5000
  },
  adb: {
    path: process.env.ADB_PATH || "adb",
    timeout: 10000
  }
};
```

## 🌐 Server Endpoints

Once running, the server provides these endpoints:

### Web Interface
- **Dashboard**: `http://localhost:3001/`
- **Status**: `http://localhost:3001/status`
- **Health**: `http://localhost:3001/health`

### API Endpoints
- **POST** `/api/play` - Play audio file
- **GET** `/api/sounds` - List available sounds
- **GET** `/api/devices` - List connected devices
- **POST** `/api/upload` - Upload audio file

### Socket.IO Events
- `connection` - Client connected
- `play-sound` - Play audio command
- `device-status` - Device status update
- `server-info` - Server information

## 🔌 Device Connection

### USB Connection (ADB)
1. Enable **Developer Options** on Android device
2. Enable **USB Debugging**
3. Connect via USB cable
4. Accept debugging prompt on device
5. Server automatically detects device

### Network Connection
1. Ensure device and computer are on same network
2. Note the server IP address (shown in console)
3. Enter IP:PORT in Android app
4. Example: `192.168.1.100:3001`

## 📱 Android App Connection

### Connection Methods
1. **Auto-discovery** - App scans for servers
2. **Manual IP** - Enter server IP manually
3. **QR Code** - Scan QR code from server dashboard
4. **USB** - Connect via ADB bridge

### Connection Troubleshooting
- Check firewall settings
- Verify port availability: `netstat -an | findstr :3001`
- Test server access: Open `http://localhost:3001` in browser
- Check network connectivity: `ping [server-ip]`

## 📊 Monitoring & Logs

### Log Locations
- **Windows Tray**: `%TEMP%\soundboard-server.log`
- **Standard Server**: `./logs/server.log`
- **Console Output**: Real-time in terminal

### Log Levels
- `error` - Errors and failures
- `warn` - Warnings and issues
- `info` - General information
- `debug` - Detailed debugging
- `trace` - Very detailed tracing

### Performance Monitoring
The server includes built-in monitoring:
- Connection count tracking
- Request response times
- Error rate monitoring
- Memory usage tracking
- Device connection status

## 🚨 Troubleshooting

### Common Issues

#### Server Won't Start
```
Error: listen EADDRINUSE :::3001
```
**Solution**: Port is in use
```bash
# Find process using port
netstat -ano | findstr :3001

# Kill process (replace PID)
taskkill /PID [PID] /F

# Or use different port
set PORT=8080 && npm start
```

#### Android App Can't Connect
**Solutions**:
- Check server is running: `http://localhost:3001`
- Verify firewall allows port 3001
- Ensure both devices on same network
- Try manual IP connection
- Check USB debugging enabled

#### Tray Application Won't Start
**Solutions**:
- Install Node.js if missing
- Run as Administrator
- Check PowerShell execution policy:
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  ```

#### Performance Issues
**Solutions**:
- Check system resources
- Reduce concurrent connections
- Clear logs directory
- Restart server process
- Update Node.js version

### Getting Help

1. **Check Logs**: Always check logs first
2. **Server Status**: Visit `http://localhost:3001/status`
3. **Network Test**: Test connectivity with browser
4. **Process Check**: Ensure server process is running
5. **Restart**: Try restarting server and app

## 🔄 Updates & Maintenance

### Updating the Server
```bash
# Pull latest changes
git pull

# Update dependencies
npm install
cd server && npm install

# Restart server
npm run server
```

### Automatic Updates
The tray application can be configured for automatic updates:
1. Enable auto-update in settings
2. Check for updates on startup
3. Download and install updates silently
4. Restart server with new version

## ⚙️ Advanced Configuration

### Custom Build
Build your own executable with custom settings:

```bash
# Modify build configuration
edit scripts/build-server-exe.js

# Build custom executable
npm run build:server-exe
```

### Service Installation (Windows)
Install as Windows service for automatic startup:

```powershell
# Install as service (requires admin)
sc create "SoundboardServer" binPath= "C:\path\to\soundboard-server.exe"

# Start service
sc start "SoundboardServer"

# Set automatic startup
sc config "SoundboardServer" start= auto
```

### Multiple Servers
Run multiple server instances:

```bash
# Server 1 (port 3001)
PORT=3001 npm start

# Server 2 (port 3002)
PORT=3002 npm start

# Server 3 (port 3003)
PORT=3003 npm start
```

## 🎯 Best Practices

1. **Use Tray Application** for everyday use on Windows
2. **Monitor Logs** regularly for issues
3. **Keep Updated** with latest versions
4. **Backup Configurations** before changes
5. **Test Connections** after network changes
6. **Use Firewall Rules** for security
7. **Monitor Performance** during heavy use

---

**Need Help?** Check the troubleshooting section or open an issue on GitHub.