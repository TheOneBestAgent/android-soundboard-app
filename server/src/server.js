// AudioDeck Connect - Enterprise Server
// Professional audio control platform with real-time communication

import express from 'express';
import http from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'path';
// import { fileURLToPath } from 'url';

// Import local modules
import { AudioPlayer } from './audio/AudioPlayer.js';
import { VoicemeeterManager } from './audio/VoicemeeterManager.js';
import { AdbManager } from './device/AdbManager.js';
// import { USBAutoDetectionService } from './network/USBAutoDetectionService.js';
import { NetworkDiscoveryService } from './network/NetworkDiscoveryService.js';
import { HealthMonitor } from './monitoring/HealthMonitor.js';
import mcpRouter from './routes/mcp.js';
import healthRouter from './routes/health.js';

// const __filename = fileURLToPath(import.meta.url);
// const __dirname = path.dirname(__filename);

dotenv.config({ path: path.resolve(process.cwd(), '.env') });

class AudioDeckServer {
    constructor() {
        this.app = express();
        this.server = http.createServer(this.app);
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
        this.app.use(express.static(path.join(process.cwd(), 'server', 'src', 'public')));
    }
    
    async initializeServices() {
        // Initialize core services
        this.audioManager = new AudioPlayer();
        this.voicemeeterManager = new VoicemeeterManager();
        this.adbManager = new AdbManager();
        // this.usbService = new USBAutoDetectionService();
        this.discoveryService = new NetworkDiscoveryService();
        this.healthMonitor = new HealthMonitor();
        
        // Start services
        try {
            await this.voicemeeterManager.connect();
        } catch (error) {
            console.warn('Voicemeeter not found or failed to connect. Running in standalone mode.');
        }

        try {
            await this.adbManager.initialize();
        } catch (error) {
            console.warn('ADB could not be initialized. USB connection features will be unavailable.');
        }

        /*
        try {
            await this.usbService.startMonitoring();
        } catch (error) {
            console.warn('USB auto-detection service failed to start.');
        }
        */

        try {
            await this.discoveryService.start();
        } catch (error) {
            console.warn('Network discovery service failed to start.');
        }
        
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
                    // usb: this.usbService.getStatus(),
                    discovery: this.discoveryService.getStatus()
                }
            });
        });
        
        // MCP Routes
        this.app.use('/api/mcp', mcpRouter);
        this.app.use('/health', healthRouter);
        
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
        this.server.listen(this.port, '0.0.0.0', () => {
            console.log(`\n============================================================`);
            console.log(`🎵 AudioDeck Connect Server`);
            console.log(`============================================================`);
            console.log(`Server running on port ${this.port}`);
            console.log(`Platform: ${process.platform}`);
            console.log(`Mode: Enterprise (v9.0.0)`);
            console.log(`============================================================`);
        });
    }
}

// Start the server
const server = new AudioDeckServer();
server.start(); 