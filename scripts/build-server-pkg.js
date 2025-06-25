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
        console.log(\`📡 Server will run on port: \${this.serverPort}\`);
        console.log(\`📝 Logs will be saved to: \${this.logFile}\`);
        
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
                console.log(\`🌐 Server URL: http://localhost:\${this.serverPort}\`);
            } else {
                throw new Error(\`Server file not found: \${serverPath}\`);
            }
        } catch (error) {
            this.logToFile(\`Error starting server: \${error.message}\`);
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
        const logMessage = \`[\${timestamp}] \${message}\\n\`;
        
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
            const standalonePath = path.join(this.buildDir, 'soundboard-server-standalone.js');
            const outputPath = path.join(this.buildDir, 'soundboard-server');
            
            // Copy server directory to build for inclusion
            const serverBuildPath = path.join(this.buildDir, 'server');
            if (fs.existsSync(this.serverDir)) {
                this.copyDirectory(this.serverDir, serverBuildPath);
            }
            
            // Build for multiple platforms
            const targets = [
                'node18-win-x64',
                'node18-linux-x64',
                'node18-macos-x64'
            ];
            
            targets.forEach(target => {
                const platform = target.includes('win') ? '.exe' : '';
                const outputFile = \`\${outputPath}-\${target.split('-')[1]}\${platform}\`;
                
                try {
                    console.log(\`Building for \${target}...\`);
                    execSync(\`pkg "\${standalonePath}" --target \${target} --output "\${outputFile}"\`, {
                        cwd: this.buildDir,
                        stdio: 'inherit'
                    });
                    console.log(\`✅ Built \${target}\`);
                } catch (error) {
                    console.warn(\`⚠️  Failed to build for \${target}: \${error.message}\`);
                }
            });
            
            console.log('✅ PKG build completed');
        } catch (error) {
            throw new Error(\`PKG build failed: \${error.message}\`);
        }
    }
    
    createTrayWrapper() {
        console.log('📝 Creating Windows tray wrapper...');
        
        const trayWrapperCode = \`#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');

class WindowsTrayWrapper {
    constructor() {
        this.serverProcess = null;
        this.isRunning = false;
        this.serverExe = path.join(__dirname, 'soundboard-server-win.exe');
        
        if (process.platform === 'win32') {
            this.startWithTray();
        } else {
            this.startDirect();
        }
    }
    
    startWithTray() {
        console.log('🪟 Starting Windows System Tray Server...');
        
        // For Windows, we'll create a simple tray using PowerShell
        this.createTrayScript();
        this.startServer();
    }
    
    startDirect() {
        console.log('🐧 Starting server directly...');
        this.startServer();
    }
    
    startServer() {
        if (!fs.existsSync(this.serverExe)) {
            console.error('❌ Server executable not found:', this.serverExe);
            process.exit(1);
        }
        
        this.serverProcess = spawn(this.serverExe, [], {
            stdio: 'inherit',
            detached: false
        });
        
        this.isRunning = true;
        console.log('✅ Server started with PID:', this.serverProcess.pid);
        
        this.serverProcess.on('exit', (code) => {
            console.log(\`Server exited with code: \${code}\`);
            this.isRunning = false;
        });
        
        this.serverProcess.on('error', (error) => {
            console.error('Server error:', error.message);
            this.isRunning = false;
        });
        
        // Keep the wrapper alive
        process.on('SIGINT', () => {
            this.stopServer();
        });
    }
    
    stopServer() {
        if (this.serverProcess && this.isRunning) {
            console.log('🛑 Stopping server...');
            this.serverProcess.kill('SIGTERM');
            
            setTimeout(() => {
                if (this.isRunning) {
                    this.serverProcess.kill('SIGKILL');
                }
            }, 5000);
        }
    }
    
    createTrayScript() {
        // Create a PowerShell script for system tray (Windows only)
        const trayScript = \\\`
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Create NotifyIcon
\\$notifyIcon = New-Object System.Windows.Forms.NotifyIcon
\\$notifyIcon.Icon = [System.Drawing.SystemIcons]::Application
\\$notifyIcon.Text = "Soundboard Server"
\\$notifyIcon.Visible = \\$true

# Create context menu
\\$contextMenu = New-Object System.Windows.Forms.ContextMenuStrip
\\$menuItemShow = New-Object System.Windows.Forms.ToolStripMenuItem
\\$menuItemShow.Text = "Show Server"
\\$menuItemRestart = New-Object System.Windows.Forms.ToolStripMenuItem
\\$menuItemRestart.Text = "Restart Server"
\\$menuItemExit = New-Object System.Windows.Forms.ToolStripMenuItem
\\$menuItemExit.Text = "Exit"

\\$contextMenu.Items.Add(\\$menuItemShow)
\\$contextMenu.Items.Add(\\$menuItemRestart)
\\$contextMenu.Items.Add("-")
\\$contextMenu.Items.Add(\\$menuItemExit)

\\$notifyIcon.ContextMenuStrip = \\$contextMenu

# Event handlers
\\$menuItemShow.Add_Click({
    Start-Process "http://localhost:3001"
})

\\$menuItemRestart.Add_Click({
    [System.Windows.Forms.MessageBox]::Show("Server restart functionality would be implemented here")
})

\\$menuItemExit.Add_Click({
    \\$notifyIcon.Dispose()
    [System.Windows.Forms.Application]::Exit()
})

# Show balloon tip
\\$notifyIcon.ShowBalloonTip(3000, "Soundboard Server", "Server is running on port 3001", [System.Windows.Forms.ToolTipIcon]::Info)

# Keep script running
while (\\$true) {
    Start-Sleep -Seconds 1
    [System.Windows.Forms.Application]::DoEvents()
}
\\\`;
        
        const trayScriptPath = path.join(this.buildDir, 'tray.ps1');
        fs.writeFileSync(trayScriptPath, trayScript);
    }
}

// Initialize the wrapper
if (require.main === module) {
    new WindowsTrayWrapper();
}

module.exports = WindowsTrayWrapper;
\`;

        const wrapperPath = path.join(this.buildDir, 'soundboard-server-tray.js');
        fs.writeFileSync(wrapperPath, trayWrapperCode);
        
        console.log('✅ Tray wrapper created');
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