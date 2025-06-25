// AudioDeck Connect - Enterprise Server
// Professional audio control platform with real-time communication

import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

import { AudioManager } from './audio/AudioManager.js';
import { VoicemeeterManager } from './audio/VoicemeeterManager.js';
import { AdbManager } from './device/AdbManager.js';
import { USBAutoDetectionService } from './device/USBAutoDetectionService.js';
import { NetworkDiscoveryService } from './network/NetworkDiscoveryService.js';
import { HealthMonitor } from './monitoring/HealthMonitor.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

class AudioDeckServer {
    constructor() {
        this.app = express();
        this.server = createServer(this.app);
        this.io = new Server(this.server, {
            cors: {
                origin: '*',
                methods: ['GET', 'POST']
            }
        });
        
        this.port = process.env.PORT || 3001;
        this.setupMiddleware();
        this.initializeServices();
        this.setupRoutes();
        this.setupSocketHandlers();
    }
    
    setupMiddleware() {
        this.app.use(cors());
        this.app.use(express.json());
        this.app.use(express.static(path.join(__dirname, 'public')));
    }
    
    async initializeServices() {
        // Initialize core services
        this.audioManager = new AudioManager();
        this.voicemeeterManager = new VoicemeeterManager();
        this.adbManager = new AdbManager();
        this.usbService = new USBAutoDetectionService();
        this.discoveryService = new NetworkDiscoveryService();
        this.healthMonitor = new HealthMonitor();
        
        // Start services
        await this.voicemeeterManager.connect();
        await this.adbManager.initialize();
        await this.usbService.startMonitoring();
        await this.discoveryService.start();
        this.healthMonitor.startMonitoring();
        
        console.log('🚀 AudioDeck Connect services initialized');
    }
    
    setupRoutes() {
        this.app.get('/health', (req, res) => {
            const health = this.healthMonitor.getStatus();
            res.json(health);
        });
        
        this.app.get('/info', (req, res) => {
            res.json({
                name: 'AudioDeck Connect',
                version: '8.0.0',
                platform: process.platform,
                services: {
                    audio: this.audioManager.getStatus(),
                    voicemeeter: this.voicemeeterManager.getStatus(),
                    adb: this.adbManager.getStatus(),
                    usb: this.usbService.getStatus(),
                    discovery: this.discoveryService.getStatus()
                }
            });
        });
        
        // Additional routes...
    }
    
    setupSocketHandlers() {
        this.io.on('connection', (socket) => {
            console.log('📱 New client connected');
            
            socket.on('disconnect', () => {
                console.log('📱 Client disconnected');
            });
            
            // Audio control events
            socket.on('play', async (data) => {
                await this.audioManager.play(data);
            });
            
            socket.on('stop', async () => {
                await this.audioManager.stop();
            });
            
            // Additional socket handlers...
        });
    }
    
    start() {
        this.server.listen(this.port, () => {
            console.log(`
============================================================
🎵 AudioDeck Connect Server
============================================================
Server running on port ${this.port}
Platform: ${process.platform}
Mode: Enterprise (v8.0.0)

Endpoints:
  Health Check: http://localhost:${this.port}/health
  Server Info:  http://localhost:${this.port}/info
  Audio Control: WebSocket
  
Features:
  ✓ Real-time audio control
  ✓ Multi-platform support
  ✓ Enterprise-grade reliability
  ✓ Professional audio integration
  ✓ Automated device discovery
  ✓ Health monitoring
============================================================`);
        });
    }
}

// Start the server
const server = new AudioDeckServer();
server.start(); 