#!/usr/bin/env node

const { app, BrowserWindow, Tray, Menu, nativeImage, dialog, shell } = require('electron');
const path = require('path');
const { spawn, exec } = require('child_process');
const fs = require('fs');
const os = require('os');

class SoundboardServerTray {
    constructor() {
        this.serverProcess = null;
        this.tray = null;
        this.mainWindow = null;
        this.serverPort = 3001;
        this.isServerRunning = false;
        this.autoStart = true;
        
        // Paths
        this.appDir = path.resolve(__dirname, '..');
        this.serverDir = path.join(this.appDir, 'server');
        this.logFile = path.join(os.tmpdir(), 'soundboard-server.log');
        
        this.initializeApp();
    }
    
    initializeApp() {
        // Electron app events
        app.whenReady().then(() => {
            this.createTray();
            this.createWindow();
            
            if (this.autoStart) {
                this.startServer();
            }
        });
        
        app.on('window-all-closed', (e) => {
            // Prevent app from quitting when all windows are closed
            e.preventDefault();
        });
        
        app.on('before-quit', () => {
            this.stopServer();
        });
    }
    
    createTray() {
        // Create tray icon (16x16 PNG)
        const iconPath = this.createTrayIcon();
        this.tray = new Tray(iconPath);
        
        this.updateTrayMenu();
        
        this.tray.setToolTip('Soundboard Server');
        this.tray.on('double-click', () => {
            this.showWindow();
        });
    }
    
    createTrayIcon() {
        // Create a simple icon programmatically
        const iconData = Buffer.from([
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
            0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x00, 0x10,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0xF3, 0xFF, 0x61, 0x00, 0x00, 0x00,
            0x19, 0x74, 0x45, 0x58, 0x74, 0x53, 0x6F, 0x66, 0x74, 0x77, 0x61, 0x72,
            0x65, 0x00, 0x41, 0x64, 0x6F, 0x62, 0x65, 0x20, 0x49, 0x6D, 0x61, 0x67,
            0x65, 0x52, 0x65, 0x61, 0x64, 0x79, 0x71, 0xC9, 0x65, 0x3C, 0x00, 0x00,
            0x00, 0x3E, 0x49, 0x44, 0x41, 0x54, 0x78, 0xDA, 0x62, 0xFC, 0xFF, 0xFF,
            0x3F, 0x03, 0x25, 0x00, 0x04, 0x4C, 0x18, 0x00, 0xE1, 0xFE, 0xFF, 0xFF,
            0xFF, 0xFF, 0x0F, 0x20, 0x80, 0x08, 0x88, 0x80, 0x08, 0x88, 0x80, 0x08,
            0x88, 0x80, 0x08, 0x88, 0x80, 0x08, 0x88, 0x80, 0x08, 0x88, 0x80, 0x08,
            0x88, 0x80, 0x08, 0x88, 0x80, 0x08, 0x88, 0x80, 0x08, 0x88, 0x00, 0x00,
            0x1A, 0x00, 0x03, 0x00, 0x4A, 0x6E, 0x24, 0x19, 0x00, 0x00, 0x00, 0x00,
            0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82
        ]);
        
        const iconPath = path.join(os.tmpdir(), 'soundboard-icon.png');
        fs.writeFileSync(iconPath, iconData);
        return iconPath;
    }
    
    updateTrayMenu() {
        const contextMenu = Menu.buildFromTemplate([
            {
                label: this.isServerRunning ? '🟢 Server Running' : '🔴 Server Stopped',
                enabled: false
            },
            { type: 'separator' },
            {
                label: this.isServerRunning ? 'Stop Server' : 'Start Server',
                click: () => {
                    if (this.isServerRunning) {
                        this.stopServer();
                    } else {
                        this.startServer();
                    }
                }
            },
            {
                label: 'Restart Server',
                click: () => this.restartServer(),
                enabled: this.isServerRunning
            },
            { type: 'separator' },
            {
                label: 'Show Dashboard',
                click: () => this.showWindow()
            },
            {
                label: 'Open Server URL',
                click: () => shell.openExternal(`http://localhost:${this.serverPort}`),
                enabled: this.isServerRunning
            },
            {
                label: 'View Logs',
                click: () => this.showLogs()
            },
            { type: 'separator' },
            {
                label: 'Settings',
                submenu: [
                    {
                        label: 'Auto-start server',
                        type: 'checkbox',
                        checked: this.autoStart,
                        click: (item) => {
                            this.autoStart = item.checked;
                        }
                    },
                    {
                        label: 'Server Port',
                        click: () => this.changePort()
                    }
                ]
            },
            { type: 'separator' },
            {
                label: 'About',
                click: () => this.showAbout()
            },
            {
                label: 'Quit',
                click: () => {
                    this.stopServer();
                    app.quit();
                }
            }
        ]);
        
        this.tray.setContextMenu(contextMenu);
    }
    
    createWindow() {
        this.mainWindow = new BrowserWindow({
            width: 800,
            height: 600,
            show: false,
            title: 'Soundboard Server Dashboard',
            icon: this.createTrayIcon(),
            webPreferences: {
                nodeIntegration: true,
                contextIsolation: false
            }
        });
        
        // Create a simple dashboard HTML
        const dashboardHTML = this.createDashboardHTML();
        const dashboardPath = path.join(os.tmpdir(), 'soundboard-dashboard.html');
        fs.writeFileSync(dashboardPath, dashboardHTML);
        
        this.mainWindow.loadFile(dashboardPath);
        
        this.mainWindow.on('close', (event) => {
            event.preventDefault();
            this.mainWindow.hide();
        });
    }
    
    createDashboardHTML() {
        return `
<!DOCTYPE html>
<html>
<head>
    <title>Soundboard Server Dashboard</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            min-height: 100vh;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background: rgba(255, 255, 255, 0.1);
            padding: 30px;
            border-radius: 15px;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px rgba(31, 38, 135, 0.37);
        }
        .status {
            text-align: center;
            margin-bottom: 30px;
        }
        .status-indicator {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            display: inline-block;
            margin-right: 10px;
        }
        .running { background-color: #4CAF50; }
        .stopped { background-color: #f44336; }
        .btn {
            background: rgba(255, 255, 255, 0.2);
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: white;
            padding: 12px 24px;
            margin: 5px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            transition: all 0.3s ease;
        }
        .btn:hover {
            background: rgba(255, 255, 255, 0.3);
            transform: translateY(-2px);
        }
        .info-card {
            background: rgba(255, 255, 255, 0.1);
            padding: 20px;
            border-radius: 10px;
            margin: 15px 0;
        }
        .logs {
            background: rgba(0, 0, 0, 0.3);
            padding: 15px;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            max-height: 200px;
            overflow-y: auto;
            white-space: pre-wrap;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎵 Soundboard Server Dashboard</h1>
        
        <div class="status">
            <h2>
                <span id="status-indicator" class="status-indicator stopped"></span>
                <span id="status-text">Server Status: Stopped</span>
            </h2>
        </div>
        
        <div style="text-align: center; margin-bottom: 30px;">
            <button class="btn" onclick="toggleServer()">Start Server</button>
            <button class="btn" onclick="restartServer()">Restart Server</button>
            <button class="btn" onclick="openServerURL()">Open Server</button>
            <button class="btn" onclick="refreshStatus()">Refresh</button>
        </div>
        
        <div class="info-card">
            <h3>Server Information</h3>
            <p><strong>Port:</strong> ${this.serverPort}</p>
            <p><strong>URL:</strong> <a href="http://localhost:${this.serverPort}" style="color: #87CEEB;">http://localhost:${this.serverPort}</a></p>
            <p><strong>Log File:</strong> ${this.logFile}</p>
        </div>
        
        <div class="info-card">
            <h3>Recent Logs</h3>
            <div id="logs" class="logs">
                Loading logs...
            </div>
        </div>
        
        <div class="info-card">
            <h3>Quick Actions</h3>
            <p>• Right-click the tray icon for quick server controls</p>
            <p>• Server automatically starts when application launches</p>
            <p>• Logs are saved to system temp directory</p>
        </div>
    </div>
    
    <script>
        // This would normally communicate with the main process
        // For now, it's a static dashboard
        function toggleServer() {
            alert('Server control is managed through the system tray');
        }
        
        function restartServer() {
            alert('Server restart is managed through the system tray');
        }
        
        function openServerURL() {
            window.open('http://localhost:${this.serverPort}', '_blank');
        }
        
        function refreshStatus() {
            location.reload();
        }
        
        // Update logs periodically
        setInterval(() => {
            // In a real implementation, this would fetch actual logs
            document.getElementById('logs').innerText = 'Server logs would appear here...\\n[' + new Date().toLocaleTimeString() + '] Server status check';
        }, 5000);
    </script>
</body>
</html>`;
    }
    
    startServer() {
        if (this.isServerRunning) {
            console.log('Server is already running');
            return;
        }
        
        console.log('Starting Soundboard Server...');
        
        // Start the Node.js server
        this.serverProcess = spawn('node', ['src/server.js'], {
            cwd: this.serverDir,
            stdio: ['ignore', 'pipe', 'pipe'],
            detached: false
        });
        
        // Log server output
        this.serverProcess.stdout.on('data', (data) => {
            this.logToFile(`[STDOUT] ${data.toString()}`);
        });
        
        this.serverProcess.stderr.on('data', (data) => {
            this.logToFile(`[STDERR] ${data.toString()}`);
        });
        
        this.serverProcess.on('close', (code) => {
            this.logToFile(`[INFO] Server process exited with code ${code}`);
            this.isServerRunning = false;
            this.updateTrayMenu();
            this.updateTrayIcon();
        });
        
        this.serverProcess.on('error', (error) => {
            this.logToFile(`[ERROR] Failed to start server: ${error.message}`);
            this.isServerRunning = false;
            this.updateTrayMenu();
            this.updateTrayIcon();
        });
        
        // Give it a moment to start
        setTimeout(() => {
            if (this.serverProcess && !this.serverProcess.killed) {
                this.isServerRunning = true;
                this.updateTrayMenu();
                this.updateTrayIcon();
                this.logToFile(`[INFO] Server started successfully on port ${this.serverPort}`);
            }
        }, 2000);
    }
    
    stopServer() {
        if (!this.isServerRunning || !this.serverProcess) {
            console.log('Server is not running');
            return;
        }
        
        console.log('Stopping Soundboard Server...');
        this.logToFile('[INFO] Stopping server...');
        
        try {
            this.serverProcess.kill('SIGTERM');
            
            // Force kill after 5 seconds if it doesn't respond
            setTimeout(() => {
                if (this.serverProcess && !this.serverProcess.killed) {
                    this.serverProcess.kill('SIGKILL');
                    this.logToFile('[INFO] Server force killed');
                }
            }, 5000);
            
        } catch (error) {
            this.logToFile(`[ERROR] Error stopping server: ${error.message}`);
        }
        
        this.isServerRunning = false;
        this.serverProcess = null;
        this.updateTrayMenu();
        this.updateTrayIcon();
    }
    
    restartServer() {
        this.logToFile('[INFO] Restarting server...');
        this.stopServer();
        
        setTimeout(() => {
            this.startServer();
        }, 2000);
    }
    
    updateTrayIcon() {
        // Update tooltip based on server status
        const tooltip = this.isServerRunning 
            ? `Soundboard Server - Running on port ${this.serverPort}`
            : 'Soundboard Server - Stopped';
        this.tray.setToolTip(tooltip);
    }
    
    showWindow() {
        if (this.mainWindow) {
            this.mainWindow.show();
            this.mainWindow.focus();
        }
    }
    
    showLogs() {
        if (fs.existsSync(this.logFile)) {
            shell.openPath(this.logFile);
        } else {
            dialog.showMessageBox({
                type: 'info',
                title: 'Logs',
                message: 'No log file found yet. Start the server to generate logs.',
                buttons: ['OK']
            });
        }
    }
    
    changePort() {
        dialog.showMessageBox({
            type: 'question',
            title: 'Change Server Port',
            message: `Current port: ${this.serverPort}\\n\\nTo change the port, modify the server configuration and restart.`,
            buttons: ['OK']
        });
    }
    
    showAbout() {
        dialog.showMessageBox({
            type: 'info',
            title: 'About Soundboard Server',
            message: 'Soundboard Server Tray Application\\n\\nVersion: 1.0.0\\nBuilt with Electron\\n\\nManages the Soundboard server for Android app connectivity.',
            buttons: ['OK']
        });
    }
    
    logToFile(message) {
        const timestamp = new Date().toISOString();
        const logMessage = `[${timestamp}] ${message}\\n`;
        
        try {
            fs.appendFileSync(this.logFile, logMessage);
        } catch (error) {
            console.error('Failed to write to log file:', error);
        }
    }
}

// Initialize the application
if (require.main === module) {
    new SoundboardServerTray();
}

module.exports = SoundboardServerTray;