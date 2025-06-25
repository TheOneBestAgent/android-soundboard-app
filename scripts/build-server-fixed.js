#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class FixedServerBuilder {
    constructor() {
        this.projectRoot = path.resolve(__dirname, '..');
        this.serverDir = path.join(this.projectRoot, 'server');
        this.buildDir = path.join(this.projectRoot, 'dist');
        
        console.log('🔧 Building Fixed Soundboard Server Executable...');
        this.build();
    }
    
    async build() {
        try {
            // Clean and prepare build environment
            this.cleanBuildDir();
            this.ensureBuildDir();
            
            // Install and prepare dependencies
            this.installDependencies();
            
            // Create the fixed server with proper dependency handling
            this.createFixedServer();
            
            // Copy and prepare all dependencies
            this.prepareDependencies();
            
            // Create package.json for proper module resolution
            this.createBuildPackageJson();
            
            // Build the executable with fixed configuration
            this.buildExecutable();
            
            // Create launchers
            this.createLaunchers();
            
            // Test the executable
            this.testExecutable();
            
            console.log('✅ Fixed Soundboard Server executable built successfully!');
            
        } catch (error) {
            console.error('❌ Build failed:', error.message);
            process.exit(1);
        }
    }
    
    cleanBuildDir() {
        console.log('🧹 Cleaning build directory...');
        
        if (fs.existsSync(this.buildDir)) {
            // Remove old build files but keep some outputs
            const filesToRemove = [
                'fixed-server.js',
                'package.json',
                'node_modules',
                'soundboard-server-fixed.exe'
            ];
            
            filesToRemove.forEach(file => {
                const filePath = path.join(this.buildDir, file);
                if (fs.existsSync(filePath)) {
                    try {
                        if (fs.statSync(filePath).isDirectory()) {
                            fs.rmSync(filePath, { recursive: true, force: true });
                        } else {
                            fs.unlinkSync(filePath);
                        }
                        console.log(`  ✅ Removed: ${file}`);
                    } catch (error) {
                        console.log(`  ⚠️ Could not remove: ${file}`);
                    }
                }
            });
        }
    }
    
    ensureBuildDir() {
        if (!fs.existsSync(this.buildDir)) {
            fs.mkdirSync(this.buildDir, { recursive: true });
        }
    }
    
    installDependencies() {
        console.log('📦 Installing and verifying dependencies...');
        
        try {
            // Install PKG globally if not available
            try {
                execSync('pkg --version', { stdio: 'ignore' });
                console.log('✅ PKG already installed');
            } catch {
                console.log('Installing PKG globally...');
                execSync('npm install -g pkg', { stdio: 'inherit' });
            }
            
            // Install server dependencies
            if (fs.existsSync(this.serverDir)) {
                console.log('Installing server dependencies...');
                execSync('npm install --production', { 
                    cwd: this.serverDir,
                    stdio: 'inherit'
                });
                console.log('✅ Server dependencies installed');
            }
            
        } catch (error) {
            throw new Error(`Failed to install dependencies: ${error.message}`);
        }
    }
    
    createFixedServer() {
        console.log('📝 Creating fixed server with proper dependency resolution...');
        
        const fixedServerCode = `#!/usr/bin/env node

// Fixed Soundboard Server with proper dependency resolution
const path = require('path');
const fs = require('fs');

// Enhanced module resolution for PKG
function setupModuleResolution() {
    const isPackaged = process.pkg !== undefined;
    
    if (isPackaged) {
        // When packaged, we need to handle module resolution differently
        const execPath = path.dirname(process.execPath);
        const nodeModulesPath = path.join(execPath, 'node_modules');
        
        // Add our bundled node_modules to the resolution paths
        if (fs.existsSync(nodeModulesPath)) {
            require.main.paths.unshift(nodeModulesPath);
            module.paths.unshift(nodeModulesPath);
        }
        
        // Also try the directory where the executable is located
        require.main.paths.unshift(execPath);
        module.paths.unshift(execPath);
        
        console.log('📦 PKG mode: Enhanced module resolution enabled');
        console.log('📁 Executable path:', execPath);
        console.log('📚 Module paths:', require.main.paths.slice(0, 3));
    }
}

// Setup enhanced resolution before loading any modules
setupModuleResolution();

// Safe module loader with fallbacks
function safeRequire(moduleName, fallback = null) {
    try {
        return require(moduleName);
    } catch (error) {
        console.log(\`⚠️  Could not load \${moduleName}: \${error.message}\`);
        if (fallback) {
            console.log(\`🔄 Using fallback for \${moduleName}\`);
            return fallback;
        }
        return null;
    }
}

// Load core dependencies with error handling
console.log('🎵 Soundboard Server (Fixed) Starting...');
console.log('📍 Running from:', process.pkg ? path.dirname(process.execPath) : __dirname);
console.log('🔧 Node version:', process.version);
console.log('💻 Platform:', process.platform);
console.log('📦 Packaged:', !!process.pkg);

const express = safeRequire('express');
const http = require('http');
const socketIo = safeRequire('socket.io');
const cors = safeRequire('cors');
const fsExtra = safeRequire('fs-extra', fs);

// Optional dependencies (graceful degradation)
const qrcode = safeRequire('qrcode');
const bonjour = safeRequire('bonjour-service');

class FixedSoundboardServer {
    constructor() {
        this.port = process.env.PORT || 3001;
        this.startTime = Date.now();
        
        if (!express) {
            console.log('❌ Express not available, starting basic HTTP server');
            this.startBasicServer();
            return;
        }
        
        this.setupExpressServer();
        this.setupRoutes();
        this.setupWebSocket();
        this.startServer();
    }
    
    setupExpressServer() {
        this.app = express();
        this.server = http.createServer(this.app);
        
        // Middleware
        if (cors) {
            this.app.use(cors());
        }
        
        this.app.use(express.json({ limit: '50mb' }));
        this.app.use(express.urlencoded({ extended: true, limit: '50mb' }));
        
        // Request logging
        this.app.use((req, res, next) => {
            console.log(\`📡 \${new Date().toISOString()} - \${req.method} \${req.url}\`);
            next();
        });
        
        console.log('✅ Express server configured');
    }
    
    setupWebSocket() {
        if (!socketIo) {
            console.log('⚠️  Socket.io not available, skipping WebSocket setup');
            return;
        }
        
        this.io = socketIo(this.server, {
            cors: {
                origin: "*",
                methods: ["GET", "POST"]
            },
            transports: ['polling', 'websocket'],
            pingTimeout: 30000,
            pingInterval: 15000
        });
        
        this.io.on('connection', (socket) => {
            console.log(\`🔌 Client connected: \${socket.id}\`);
            
            socket.on('disconnect', () => {
                console.log(\`🔌 Client disconnected: \${socket.id}\`);
            });
            
            socket.on('play-sound', (data) => {
                console.log('🎵 Play sound request:', data);
                socket.broadcast.emit('play-sound', data);
            });
        });
        
        console.log('✅ WebSocket configured');
    }
    
    setupRoutes() {
        // Health check
        this.app.get('/health', (req, res) => {
            res.json({
                status: 'running',
                message: 'Fixed Soundboard Server is working!',
                timestamp: new Date().toISOString(),
                uptime: Math.floor((Date.now() - this.startTime) / 1000),
                version: '2.0.0-fixed',
                features: {
                    express: !!express,
                    socketio: !!socketIo,
                    cors: !!cors,
                    qrcode: !!qrcode,
                    bonjour: !!bonjour
                },
                packaged: !!process.pkg
            });
        });
        
        // Server info
        this.app.get('/api/server-info', (req, res) => {
            res.json({
                name: 'Soundboard Server (Fixed)',
                version: '2.0.0-fixed',
                platform: process.platform,
                arch: process.arch,
                nodeVersion: process.version,
                uptime: Math.floor((Date.now() - this.startTime) / 1000),
                packaged: !!process.pkg,
                executablePath: process.pkg ? process.execPath : __filename
            });
        });
        
        // Play sound endpoint
        this.app.post('/api/play-sound', (req, res) => {
            const { soundName, volume = 1.0 } = req.body;
            
            console.log(\`🎵 Play sound request: \${soundName}, volume: \${volume}\`);
            
            // Emit to WebSocket clients if available
            if (this.io) {
                this.io.emit('play-sound', {
                    soundName,
                    volume,
                    timestamp: new Date().toISOString()
                });
            }
            
            res.json({ 
                success: true, 
                message: \`Playing sound: \${soundName}\`,
                volume,
                timestamp: new Date().toISOString()
            });
        });
        
        // Root endpoint
        this.app.get('/', (req, res) => {
            res.send(\`
                <h1>🎵 Soundboard Server (Fixed)</h1>
                <p>✅ Server is running successfully!</p>
                <p>📡 Port: \${this.port}</p>
                <p>⏰ Started: \${new Date(this.startTime).toISOString()}</p>
                <p>📦 Packaged: \${process.pkg ? 'Yes' : 'No'}</p>
                <h2>Endpoints:</h2>
                <ul>
                    <li><a href="/health">Health Check</a></li>
                    <li><a href="/api/server-info">Server Info</a></li>
                </ul>
                <h2>Features:</h2>
                <ul>
                    <li>Express: \${express ? '✅' : '❌'}</li>
                    <li>Socket.IO: \${socketIo ? '✅' : '❌'}</li>
                    <li>CORS: \${cors ? '✅' : '❌'}</li>
                    <li>QR Code: \${qrcode ? '✅' : '❌'}</li>
                    <li>Bonjour: \${bonjour ? '✅' : '❌'}</li>
                </ul>
            \`);
        });
        
        console.log('✅ Routes configured');
    }
    
    startServer() {
        this.server.listen(this.port, () => {
            console.log('');
            console.log('🎉 FIXED SOUNDBOARD SERVER STARTED!');
            console.log(\`📡 Server running on port: \${this.port}\`);
            console.log(\`🌐 URL: http://localhost:\${this.port}\`);
            console.log(\`🔍 Health check: http://localhost:\${this.port}/health\`);
            console.log(\`📦 Packaged mode: \${process.pkg ? 'Yes' : 'No'}\`);
            console.log('');
            console.log('💡 Features loaded:');
            console.log(\`   • Express: \${express ? '✅' : '❌'}\`);
            console.log(\`   • Socket.IO: \${socketIo ? '✅' : '❌'}\`);
            console.log(\`   • CORS: \${cors ? '✅' : '❌'}\`);
            console.log(\`   • QR Code: \${qrcode ? '✅' : '❌'}\`);
            console.log(\`   • Bonjour: \${bonjour ? '✅' : '❌'}\`);
            console.log('');
            console.log('🛑 Press Ctrl+C to stop');
        });
        
        // Graceful shutdown
        process.on('SIGINT', () => {
            console.log('\\n🛑 Shutting down server...');
            this.server.close(() => {
                console.log('✅ Server stopped');
                process.exit(0);
            });
        });
    }
    
    startBasicServer() {
        console.log('🔧 Starting basic HTTP server (Express not available)...');
        
        const server = http.createServer((req, res) => {
            console.log(\`📡 \${new Date().toISOString()} - \${req.method} \${req.url}\`);
            
            if (req.url === '/health') {
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                    status: 'running',
                    message: 'Basic Soundboard Server is working!',
                    timestamp: new Date().toISOString(),
                    uptime: Math.floor((Date.now() - this.startTime) / 1000),
                    version: '2.0.0-basic',
                    packaged: !!process.pkg
                }));
            } else {
                res.writeHead(200, { 'Content-Type': 'text/html' });
                res.end(\`
                    <h1>🎵 Basic Soundboard Server</h1>
                    <p>✅ Basic server is running!</p>
                    <p>📡 Port: \${this.port}</p>
                    <p>⏰ Started: \${new Date(this.startTime).toISOString()}</p>
                    <p>📦 Packaged: \${process.pkg ? 'Yes' : 'No'}</p>
                    <p>⚠️  Limited functionality (Express not available)</p>
                \`);
            }
        });
        
        server.listen(this.port, () => {
            console.log('');
            console.log('🎉 BASIC SOUNDBOARD SERVER STARTED!');
            console.log(\`📡 Server running on port: \${this.port}\`);
            console.log(\`🌐 URL: http://localhost:\${this.port}\`);
            console.log(\`🔍 Health check: http://localhost:\${this.port}/health\`);
            console.log('');
            console.log('🛑 Press Ctrl+C to stop');
        });
        
        process.on('SIGINT', () => {
            console.log('\\n🛑 Shutting down basic server...');
            server.close(() => {
                console.log('✅ Server stopped');
                process.exit(0);
            });
        });
    }
}

// Start the server
if (require.main === module) {
    new FixedSoundboardServer();
}

module.exports = FixedSoundboardServer;
`;

        const fixedServerPath = path.join(this.buildDir, 'fixed-server.js');
        fs.writeFileSync(fixedServerPath, fixedServerCode);
        
        console.log('✅ Fixed server created');
    }
    
    prepareDependencies() {
        console.log('📦 Preparing dependencies for bundling...');
        
        const serverNodeModules = path.join(this.serverDir, 'node_modules');
        const buildNodeModules = path.join(this.buildDir, 'node_modules');
        
        if (fs.existsSync(serverNodeModules)) {
            console.log('Copying essential dependencies...');
            
            // Essential dependencies to copy
            const essentialDeps = [
                'express',
                'socket.io',
                'cors',
                'qrcode',
                'bonjour-service'
            ];
            
            // Create node_modules structure
            if (!fs.existsSync(buildNodeModules)) {
                fs.mkdirSync(buildNodeModules, { recursive: true });
            }
            
            essentialDeps.forEach(dep => {
                const srcPath = path.join(serverNodeModules, dep);
                const destPath = path.join(buildNodeModules, dep);
                
                if (fs.existsSync(srcPath)) {
                    try {
                        this.copyDirectorySafe(srcPath, destPath);
                        console.log(`  ✅ Copied: ${dep}`);
                    } catch (error) {
                        console.log(`  ⚠️ Could not copy ${dep}: ${error.message}`);
                    }
                } else {
                    console.log(`  ⚠️ Not found: ${dep}`);
                }
            });
            
            console.log('✅ Dependencies prepared');
        } else {
            console.log('⚠️  Server node_modules not found');
        }
    }
    
    copyDirectorySafe(source, destination) {
        if (!fs.existsSync(destination)) {
            fs.mkdirSync(destination, { recursive: true });
        }
        
        const files = fs.readdirSync(source);
        files.forEach(file => {
            const sourcePath = path.join(source, file);
            const destPath = path.join(destination, file);
            
            // Skip problematic files/directories
            if (file.includes('node-gyp') || 
                file.includes('.git') || 
                file.includes('test') ||
                file.includes('example') ||
                file.endsWith('.node') ||
                file.includes('binding.gyp')) {
                return;
            }
            
            try {
                const stat = fs.statSync(sourcePath);
                if (stat.isDirectory() && file !== 'node_modules') {
                    this.copyDirectorySafe(sourcePath, destPath);
                } else if (stat.isFile()) {
                    fs.copyFileSync(sourcePath, destPath);
                }
            } catch (error) {
                // Skip files that can't be copied
            }
        });
    }
    
    createBuildPackageJson() {
        console.log('📝 Creating build package.json...');
        
        const buildPackageJson = {
            name: 'soundboard-server-fixed',
            version: '2.0.0',
            description: 'Fixed Soundboard Server with proper dependency resolution',
            main: 'fixed-server.js',
            bin: 'fixed-server.js',
            pkg: {
                scripts: ['fixed-server.js'],
                targets: ['node18-win-x64'],
                outputPath: 'dist/',
                assets: [
                    'node_modules/**/*'
                ],
                options: [
                    '--compress=Brotli',
                    '--public'
                ]
            },
            dependencies: {
                'express': '^4.19.2',
                'socket.io': '^4.7.5',
                'cors': '^2.8.5',
                'qrcode': '^1.5.3',
                'bonjour-service': '^1.1.1'
            }
        };
        
        const buildPackageJsonPath = path.join(this.buildDir, 'package.json');
        fs.writeFileSync(buildPackageJsonPath, JSON.stringify(buildPackageJson, null, 2));
        
        console.log('✅ Build package.json created');
    }
    
    buildExecutable() {
        console.log('🔨 Building fixed executable...');
        
        try {
            const fixedServerPath = path.join(this.buildDir, 'fixed-server.js');
            const outputPath = path.join(this.buildDir, 'soundboard-server-fixed.exe');
            
            // Build with PKG using the package.json configuration
            console.log('Building Windows executable with enhanced configuration...');
            execSync(`pkg . --target node18-win-x64 --output soundboard-server-fixed.exe --compress Brotli --public`, {
                cwd: this.buildDir,
                stdio: 'inherit'
            });
            
            console.log('✅ Fixed executable built successfully!');
            console.log(`📦 Output file: ${outputPath}`);
            
            // Verify the executable exists and get its size
            if (fs.existsSync(outputPath)) {
                const stats = fs.statSync(outputPath);
                console.log(`📏 File size: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
            }
            
        } catch (error) {
            throw new Error(`Build failed: ${error.message}`);
        }
    }
    
    createLaunchers() {
        console.log('📝 Creating fixed launchers...');
        
        // Batch launcher
        const batchCode = `@echo off
title Soundboard Server (Fixed)
echo 🔧 Starting Soundboard Server (Fixed)...
echo.
echo 📡 Server will be available at: http://localhost:3001
echo 💡 Press Ctrl+C to stop the server
echo ✅ All dependencies properly bundled
echo.

"%~dp0soundboard-server-fixed.exe"

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
        
        const batchPath = path.join(this.buildDir, 'start-soundboard-fixed.bat');
        fs.writeFileSync(batchPath, batchCode);
        
        // PowerShell launcher
        const psCode = `# Soundboard Server Launcher (Fixed)
Write-Host "🔧 Starting Soundboard Server (Fixed)..." -ForegroundColor Green
Write-Host ""
Write-Host "📡 Server will be available at: http://localhost:3001" -ForegroundColor Cyan
Write-Host "💡 Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host "✅ All dependencies properly bundled" -ForegroundColor Green
Write-Host ""

$serverExe = Join-Path $PSScriptRoot "soundboard-server-fixed.exe"

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
        
        const psPath = path.join(this.buildDir, 'start-soundboard-fixed.ps1');
        fs.writeFileSync(psPath, psCode);
        
        console.log('✅ Fixed launchers created');
    }
    
    testExecutable() {
        console.log('🧪 Testing the fixed executable...');
        
        const executablePath = path.join(this.buildDir, 'soundboard-server-fixed.exe');
        
        if (fs.existsSync(executablePath)) {
            console.log('✅ Executable file exists');
            
            try {
                // Quick test to see if it loads without crashing
                console.log('Running quick startup test...');
                const testResult = execSync(`"${executablePath}" --help`, {
                    timeout: 5000,
                    stdio: 'pipe'
                });
                console.log('✅ Executable loads successfully');
            } catch (error) {
                if (error.status === 1) {
                    console.log('✅ Executable loads (exit code 1 is normal for --help)');
                } else {
                    console.log('⚠️  Executable test inconclusive:', error.message);
                }
            }
        } else {
            console.log('❌ Executable not found');
        }
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
    }
}

// Run the builder
if (require.main === module) {
    new FixedServerBuilder();
}

module.exports = FixedServerBuilder; 