#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class ServerPkgBuilder {
    constructor() {
        this.projectRoot = path.resolve(__dirname, '..');
        this.serverDir = path.join(this.projectRoot, 'server');
        this.buildDir = path.join(this.projectRoot, 'dist');
        
        console.log('🔨 Building Soundboard Server Executable with PKG...');
        this.build();
    }
    
    async build() {
        try {
            // Ensure build directory exists
            this.ensureBuildDir();
            
            // Install dependencies
            this.installDependencies();
            
            // Create standalone server entry
            this.createStandaloneServer();
            
            // Build with PKG
            this.buildWithPkg();
            
            // Create Windows system tray wrapper
            this.createTrayWrapper();
            
            console.log('✅ Soundboard Server executable built successfully!');
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
    
    createStandaloneServer() {
        console.log('📝 Creating standalone server...');
        
        const standaloneServerCode = `#!/usr/bin/env node

const path = require('path');
const { spawn, exec } = require('child_process');
const fs = require('fs');
const os = require('os');

class SoundboardServerStandalone {
    constructor() {
        this.serverPort = process.env.PORT || 3001;
        this.isRunning = false;
        this.logFile = path.join(os.tmpdir(), 'soundboard-server.log');
        
        console.log('🎵 Soundboard Server Starting...');
        console.log('📡 Server will run on port: ' + this.serverPort);
        console.log('📝 Logs will be saved to: ' + this.logFile);
        
        this.startServer();
        this.setupSignalHandlers();
        this.showHelp();
    }
    
    startServer() {
        try {
            // Import and start the server
            const serverPath = path.join(__dirname, 'src', 'server.js');
            
            if (fs.existsSync(serverPath)) {
                require(serverPath);
                this.isRunning = true;
                this.logToFile('Server started successfully');
                console.log('✅ Server started successfully!');
                console.log('🌐 Server URL: http://localhost:' + this.serverPort);
            } else {
                throw new Error('Server file not found: ' + serverPath);
            }
        } catch (error) {
            this.logToFile('Error starting server: ' + error.message);
            console.error('❌ Failed to start server:', error.message);
            process.exit(1);
        }
    }
    
    setupSignalHandlers() {
        process.on('SIGINT', () => {
            console.log('\\n🛑 Shutting down server...');
            this.logToFile('Server shutdown requested');
            process.exit(0);
        });
        
        process.on('SIGTERM', () => {
            console.log('\\n🛑 Server terminated');
            this.logToFile('Server terminated');
            process.exit(0);
        });
    }
    
    showHelp() {
        console.log('\\n📋 Server Controls:');
        console.log('   • Ctrl+C to stop the server');
        console.log('   • Check logs at:', this.logFile);
        console.log('   • Server status: Running ✅');
        console.log('\\n💡 Tips:');
        console.log('   • Make sure your Android device is on the same network');
        console.log('   • Enable USB Debugging for ADB connection');
        console.log('   • Use the Android app to connect to this server');
    }
    
    logToFile(message) {
        const timestamp = new Date().toISOString();
        const logMessage = '[' + timestamp + '] ' + message + '\\n';
        
        try {
            fs.appendFileSync(this.logFile, logMessage);
        } catch (error) {
            console.error('Failed to write to log file:', error);
        }
    }
}

// Initialize the server
if (require.main === module) {
    new SoundboardServerStandalone();
}

module.exports = SoundboardServerStandalone;
`;

        const standalonePath = path.join(this.buildDir, 'soundboard-server-standalone.js');
        fs.writeFileSync(standalonePath, standaloneServerCode);
        
        console.log('✅ Standalone server created');
    }
    
    buildWithPkg() {
        console.log('🔨 Building executable with PKG...');
        
        try {
            // Build directly from the server directory
            const serverMainPath = path.join(this.serverDir, 'src', 'server.js');
            const outputPath = path.join(this.buildDir, 'soundboard-server-windows.exe');
            
            if (!fs.existsSync(serverMainPath)) {
                throw new Error(`Server main file not found: ${serverMainPath}`);
            }
            
            // Build for Windows
            console.log('Building Windows executable...');
            execSync(`pkg "${serverMainPath}" --target node18-win-x64 --output "${outputPath}"`, {
                cwd: this.serverDir,
                stdio: 'inherit'
            });
            
            console.log('✅ Windows executable built successfully!');
            console.log(`📦 Output file: ${outputPath}`);
            
            // List build output
            this.listBuildOutput();
            
        } catch (error) {
            throw new Error(`PKG build failed: ${error.message}`);
        }
    }
    
    listBuildOutput() {
        console.log('\n📋 Build Output:');
        
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
        
        console.log('\n🎉 Build completed! You can now run the executable from the dist folder.');
        console.log('\n💡 Usage:');
        console.log('   • Double-click: start-soundboard-server.bat');
        console.log('   • PowerShell: .\\start-soundboard-server.ps1');
        console.log('   • Direct: .\\soundboard-server-windows.exe');
    }
    
    createTrayWrapper() {
        console.log('📝 Creating Windows batch launcher...');
        
        const batchLauncherCode = `@echo off
title Soundboard Server
echo 🎵 Starting Soundboard Server...
echo.
echo 📡 Server will be available at: http://localhost:3001
echo 💡 Press Ctrl+C to stop the server
echo.

"%~dp0soundboard-server-windows.exe"

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
        
        const batchPath = path.join(this.buildDir, 'start-soundboard-server.bat');
        fs.writeFileSync(batchPath, batchLauncherCode);
        
        // Also create a PowerShell launcher
        const psLauncherCode = `# Soundboard Server Launcher
Write-Host "🎵 Starting Soundboard Server..." -ForegroundColor Green
Write-Host ""
Write-Host "📡 Server will be available at: http://localhost:3001" -ForegroundColor Cyan
Write-Host "💡 Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

$serverExe = Join-Path $PSScriptRoot "soundboard-server-windows.exe"

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
        
        const psPath = path.join(this.buildDir, 'start-soundboard-server.ps1');
        fs.writeFileSync(psPath, psLauncherCode);
        
        console.log('✅ Launchers created');
    }
    
    copyDirectory(source, destination) {
        if (!fs.existsSync(destination)) {
            fs.mkdirSync(destination, { recursive: true });
        }
        
        const files = fs.readdirSync(source);
        files.forEach(file => {
            const sourcePath = path.join(source, file);
            const destPath = path.join(destination, file);
            
            if (fs.statSync(sourcePath).isDirectory()) {
                this.copyDirectory(sourcePath, destPath);
            } else {
                fs.copyFileSync(sourcePath, destPath);
            }
        });
    }
}

// Run the builder
if (require.main === module) {
    new ServerPkgBuilder();
}

module.exports = ServerPkgBuilder;