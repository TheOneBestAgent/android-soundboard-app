#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class SafeServerPkgBuilder {
    constructor() {
        this.projectRoot = path.resolve(__dirname, '..');
        this.serverDir = path.join(this.projectRoot, 'server');
        this.buildDir = path.join(this.projectRoot, 'dist');
        
        console.log('🔨 Building Safe Soundboard Server Executable...');
        this.build();
    }
    
    async build() {
        try {
            // Ensure build directory exists
            this.ensureBuildDir();
            
            // Install dependencies
            this.installDependencies();
            
            // Create safe server version
            this.createSafeServer();
            
            // Build with PKG
            this.buildWithPkg();
            
            // Create launchers
            this.createLaunchers();
            
            console.log('✅ Safe Soundboard Server executable built successfully!');
            console.log(`📦 Output directory: ${this.buildDir}`);
            
        } catch (error) {
            console.error('❌ Build failed:', error.message);
            process.exit(1);
        }
    }
    
    ensureBuildDir() {
        if (!fs.existsSync(this.buildDir)) {
            fs.mkdirSync(this.buildDir, { recursive: true });
        }
    }
    
    installDependencies() {
        console.log('📦 Installing dependencies...');
        
        try {
            // Install PKG globally if not available
            try {
                execSync('pkg --version', { stdio: 'ignore' });
            } catch {
                console.log('Installing PKG...');
                execSync('npm install -g pkg', { stdio: 'inherit' });
            }
            
            // Install server dependencies
            if (fs.existsSync(this.serverDir)) {
                console.log('Installing server dependencies...');
                execSync('npm install', { 
                    cwd: this.serverDir,
                    stdio: 'inherit'
                });
            }
            
            console.log('✅ Dependencies installed');
        } catch (error) {
            throw new Error(`Failed to install dependencies: ${error.message}`);
        }
    }
    
    createSafeServer() {
        console.log('📝 Creating safe server version...');
        
        const safeServerCode = `#!/usr/bin/env node

const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const os = require('os');

// Safe imports - handle missing native dependencies gracefully
let voicemeeterConnector = null;
let bonjourService = null;

try {
    voicemeeterConnector = require('voicemeeter-connector');
    console.log('✅ Voicemeeter connector loaded');
} catch (error) {
    console.log('⚠️  Voicemeeter connector not available (native dependency)');
}

try {
    bonjourService = require('bonjour-service');
    console.log('✅ Bonjour service loaded');
} catch (error) {
    console.log('⚠️  Bonjour service not available');
}

class SoundboardServer {
    constructor() {
        this.port = process.env.PORT || 3001;
        this.logFile = path.join(os.tmpdir(), 'soundboard-server.log');
        
        this.setupServer();
        this.setupRoutes();
        this.setupWebSocket();
        this.startServer();
    }
    
    setupServer() {
        this.app = express();
        this.server = http.createServer(this.app);
        this.io = socketIo(this.server, {
            cors: {
                origin: "*",
                methods: ["GET", "POST"]
            }
        });
        
        this.app.use(cors());
        this.app.use(express.json());
        this.app.use(express.static(path.join(__dirname, 'public')));
    }
    
    setupRoutes() {
        // Health check
        this.app.get('/health', (req, res) => {
            res.json({
                status: 'running',
                timestamp: new Date().toISOString(),
                features: {
                    voicemeeter: !!voicemeeterConnector,
                    bonjour: !!bonjourService
                }
            });
        });
        
        // Get server info
        this.app.get('/api/server-info', (req, res) => {
            res.json({
                name: 'Soundboard Server',
                version: '1.0.0',
                platform: os.platform(),
                arch: os.arch(),
                nodeVersion: process.version,
                uptime: process.uptime(),
                features: {
                    voicemeeter: !!voicemeeterConnector,
                    bonjour: !!bonjourService
                }
            });
        });
        
        // Play sound endpoint
        this.app.post('/api/play-sound', (req, res) => {
            const { soundName, volume = 1.0 } = req.body;
            
            this.logToFile(\`Play sound request: \${soundName}, volume: \${volume}\`);
            
            // Emit to all connected clients
            this.io.emit('play-sound', {
                soundName,
                volume,
                timestamp: new Date().toISOString()
            });
            
            res.json({ 
                success: true, 
                message: \`Playing sound: \${soundName}\`,
                volume 
            });
        });
        
        // Voicemeeter control (if available)
        this.app.post('/api/voicemeeter/:action', (req, res) => {
            if (!voicemeeterConnector) {
                return res.status(503).json({
                    success: false,
                    error: 'Voicemeeter connector not available'
                });
            }
            
            const { action } = req.params;
            const { value } = req.body;
            
            try {
                // Handle voicemeeter actions here
                res.json({
                    success: true,
                    action,
                    value,
                    message: 'Voicemeeter action executed'
                });
            } catch (error) {
                res.status(500).json({
                    success: false,
                    error: error.message
                });
            }
        });
    }
    
    setupWebSocket() {
        this.io.on('connection', (socket) => {
            console.log('Client connected:', socket.id);
            this.logToFile(\`Client connected: \${socket.id}\`);
            
            socket.on('disconnect', () => {
                console.log('Client disconnected:', socket.id);
                this.logToFile(\`Client disconnected: \${socket.id}\`);
            });
            
            socket.on('play-sound', (data) => {
                console.log('Play sound via WebSocket:', data);
                this.logToFile(\`WebSocket play sound: \${JSON.stringify(data)}\`);
                
                // Broadcast to all other clients
                socket.broadcast.emit('play-sound', data);
            });
        });
    }
    
    startServer() {
        this.server.listen(this.port, () => {
            console.log('🎵 Soundboard Server Started!');
            console.log(\`📡 Server running on port: \${this.port}\`);
            console.log(\`🌐 Server URL: http://localhost:\${this.port}\`);
            console.log(\`📝 Logs saved to: \${this.logFile}\`);
            console.log('');
            console.log('💡 Features:');
            console.log(\`   • Basic Server: ✅\`);
            console.log(\`   • WebSocket: ✅\`);
            console.log(\`   • Voicemeeter: \${voicemeeterConnector ? '✅' : '❌ (requires native deps)'}\`);
            console.log(\`   • Network Discovery: \${bonjourService ? '✅' : '❌ (optional)'}\`);
            console.log('');
            console.log('🛑 Press Ctrl+C to stop the server');
            console.log('');
            
            this.logToFile('Server started successfully');
            
            // Setup graceful shutdown
            process.on('SIGINT', () => {
                console.log('\\n🛑 Shutting down server...');
                this.logToFile('Server shutdown requested');
                this.server.close(() => {
                    console.log('✅ Server stopped');
                    process.exit(0);
                });
            });
        });
    }
    
    logToFile(message) {
        const timestamp = new Date().toISOString();
        const logMessage = \`[\${timestamp}] \${message}\\n\`;
        
        try {
            fs.appendFileSync(this.logFile, logMessage);
        } catch (error) {
            console.error('Failed to write to log file:', error);
        }
    }
}

// Start the server
if (require.main === module) {
    new SoundboardServer();
}

module.exports = SoundboardServer;
`;

        const safeServerPath = path.join(this.buildDir, 'safe-server.js');
        fs.writeFileSync(safeServerPath, safeServerCode);
        
        console.log('✅ Safe server created');
    }
    
    buildWithPkg() {
        console.log('🔨 Building executable with PKG...');
        
        try {
            const safeServerPath = path.join(this.buildDir, 'safe-server.js');
            const outputPath = path.join(this.buildDir, 'soundboard-server-safe.exe');
            
            // Copy server node_modules to build directory
            const serverNodeModules = path.join(this.serverDir, 'node_modules');
            const buildNodeModules = path.join(this.buildDir, 'node_modules');
            
            if (fs.existsSync(serverNodeModules)) {
                console.log('📦 Copying server dependencies...');
                this.copyDirectory(serverNodeModules, buildNodeModules);
            }
            
            // Create package.json for the build
            const buildPackageJson = {
                name: 'soundboard-server-safe',
                version: '1.0.0',
                main: 'safe-server.js',
                dependencies: {
                    "express": "^4.19.2",
                    "socket.io": "^4.7.5",
                    "cors": "^2.8.5"
                }
            };
            
            const buildPackageJsonPath = path.join(this.buildDir, 'package.json');
            fs.writeFileSync(buildPackageJsonPath, JSON.stringify(buildPackageJson, null, 2));
            
            // Build executable
            console.log('Building Windows executable...');
            execSync(`pkg "${safeServerPath}" --target node18-win-x64 --output "${outputPath}"`, {
                cwd: this.buildDir,
                stdio: 'inherit'
            });
            
            console.log('✅ Safe executable built successfully!');
            console.log(`📦 Output file: ${outputPath}`);
            
            // List build output
            this.listBuildOutput();
            
        } catch (error) {
            throw new Error(`PKG build failed: ${error.message}`);
        }
    }
    
    copyDirectory(source, destination) {
        if (!fs.existsSync(destination)) {
            fs.mkdirSync(destination, { recursive: true });
        }
        
        const files = fs.readdirSync(source);
        files.forEach(file => {
            const sourcePath = path.join(source, file);
            const destPath = path.join(destination, file);
            
            // Skip problematic native dependency directories
            if (file.includes('ffi-napi') || file.includes('voicemeeter') || file.includes('.bin')) {
                return;
            }
            
            try {
                if (fs.statSync(sourcePath).isDirectory()) {
                    this.copyDirectory(sourcePath, destPath);
                } else {
                    fs.copyFileSync(sourcePath, destPath);
                }
            } catch (error) {
                // Skip files that can't be copied
                console.log(`⚠️ Skipped: ${file}`);
            }
        });
    }
    
    createLaunchers() {
        console.log('📝 Creating launchers...');
        
        // Batch launcher
        const batchLauncherCode = `@echo off
title Soundboard Server (Safe Mode)
echo 🎵 Starting Soundboard Server (Safe Mode)...
echo.
echo 📡 Server will be available at: http://localhost:3001
echo 💡 Press Ctrl+C to stop the server
echo ⚠️  Running in safe mode (some features may be limited)
echo.

"%~dp0soundboard-server-safe.exe"

if errorlevel 1 (
    echo.
    echo ❌ Server failed to start
    pause
) else (
    echo.
    echo ✅ Server stopped normally
    pause
)
`;
        
        const batchPath = path.join(this.buildDir, 'start-soundboard-server-safe.bat');
        fs.writeFileSync(batchPath, batchLauncherCode);
        
        // PowerShell launcher
        const psLauncherCode = `# Soundboard Server Launcher (Safe Mode)
Write-Host "🎵 Starting Soundboard Server (Safe Mode)..." -ForegroundColor Green
Write-Host ""
Write-Host "📡 Server will be available at: http://localhost:3001" -ForegroundColor Cyan
Write-Host "💡 Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host "⚠️  Running in safe mode (some features may be limited)" -ForegroundColor Yellow
Write-Host ""

$serverExe = Join-Path $PSScriptRoot "soundboard-server-safe.exe"

if (Test-Path $serverExe) {
    try {
        & $serverExe
        Write-Host ""
        Write-Host "✅ Server stopped normally" -ForegroundColor Green
    } catch {
        Write-Host ""
        Write-Host "❌ Server failed to start: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Server executable not found: $serverExe" -ForegroundColor Red
}

Read-Host "Press Enter to exit"
`;
        
        const psPath = path.join(this.buildDir, 'start-soundboard-server-safe.ps1');
        fs.writeFileSync(psPath, psLauncherCode);
        
        console.log('✅ Safe launchers created');
    }
    
    listBuildOutput() {
        console.log('\\n📋 Build Output:');
        
        try {
            const distContents = fs.readdirSync(this.buildDir);
            distContents.forEach(file => {
                const filePath = path.join(this.buildDir, file);
                const stats = fs.statSync(filePath);
                const size = stats.isFile() ? `(${(stats.size / 1024 / 1024).toFixed(2)} MB)` : '(directory)';
                console.log(`   📁 ${file} ${size}`);
            });
        } catch (error) {
            console.log('   (Could not list output files)');
        }
        
        console.log('\\n🎉 Safe build completed! This version handles missing native dependencies gracefully.');
        console.log('\\n💡 Usage:');
        console.log('   • Double-click: start-soundboard-server-safe.bat');
        console.log('   • PowerShell: .\\\\start-soundboard-server-safe.ps1');
        console.log('   • Direct: .\\\\soundboard-server-safe.exe');
    }
}

// Run the builder
if (require.main === module) {
    new SafeServerPkgBuilder();
}

module.exports = SafeServerPkgBuilder; 