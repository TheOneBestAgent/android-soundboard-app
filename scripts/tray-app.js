const { app, Tray, Menu, nativeImage } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fetch = require('node-fetch');

class SoundboardTrayApp {
    constructor() {
        this.serverProcess = null;
        this.tray = null;
        this.isServerRunning = false;
        this.pingInterval = null;
        this.serverPort = 3001; // Make sure this matches your server's port

        // Ensure only one instance of the app is running
        const gotTheLock = app.requestSingleInstanceLock();
        if (!gotTheLock) {
            app.quit();
        } else {
            this.init();
        }
    }

    init() {
        app.on('ready', () => {
            this.createTray();
            this.startServer();
        });

        app.on('before-quit', () => {
            this.stopServer();
            if (this.pingInterval) {
                clearInterval(this.pingInterval);
            }
        });
    }

    createTray() {
        const icon = nativeImage.createFromNamedImage('NSImageNameStatusUnavailable', [16, 16]);
        this.tray = new Tray(icon);
        this.tray.setToolTip('Soundboard Server');
        this.updateTrayMenu();
    }

    updateTrayMenu() {
        const contextMenu = Menu.buildFromTemplate([
            {
                label: this.isServerRunning ? 'Server is Running' : 'Server is Stopped',
                enabled: false,
            },
            { type: 'separator' },
            {
                label: 'Restart Server',
                click: () => this.restartServer(),
            },
            {
                label: 'Quit',
                click: () => app.quit(),
            },
        ]);
        this.tray.setContextMenu(contextMenu);
    }

    startServer() {
        this.stopServer(); // Ensure no zombie processes

        const serverPath = path.resolve(app.getAppPath(), 'server', 'src-consolidated.js');
        this.serverProcess = spawn('node', [serverPath], { detached: true });

        this.serverProcess.on('spawn', () => {
            console.log('Server process spawned. Starting health checks...');
            this.startPinging();
        });

        this.serverProcess.on('close', (code) => {
            console.log(`Server process exited with code ${code}`);
            this.setServerStatus(false);
            if (this.pingInterval) {
                clearInterval(this.pingInterval);
            }
        });

        this.serverProcess.stderr.on('data', (data) => {
            console.error(`Server Error: ${data}`);
        });
    }

    stopServer() {
        if (this.serverProcess) {
            try {
                // Kill the entire process group to prevent orphan processes
                process.kill(-this.serverProcess.pid);
            } catch (e) {
                console.error('Failed to kill server process:', e.message);
            }
            this.serverProcess = null;
        }
    }
    
    restartServer() {
        this.startServer();
    }
    
    startPinging() {
        if (this.pingInterval) {
            clearInterval(this.pingInterval);
        }

        this.pingInterval = setInterval(async () => {
            try {
                const response = await fetch(`http://localhost:${this.serverPort}/health`);
                if (response.ok) {
                    this.setServerStatus(true);
                } else {
                    this.setServerStatus(false);
                }
            } catch (error) {
                this.setServerStatus(false);
            }
        }, 2000); // Ping every 2 seconds
    }

    setServerStatus(isRunning) {
        if (this.isServerRunning === isRunning) return; // No change

        this.isServerRunning = isRunning;
        const iconName = isRunning ? 'NSImageNameStatusAvailable' : 'NSImageNameStatusUnavailable';
        const icon = nativeImage.createFromNamedImage(iconName, [16, 16]);
        this.tray.setImage(icon);
        this.updateTrayMenu();
        console.log(`Server status updated to: ${isRunning ? 'Running' : 'Stopped'}`);
    }
}

new SoundboardTrayApp(); 