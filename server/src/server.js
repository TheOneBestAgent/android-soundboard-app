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
import AsyncUtils from './utils/AsyncUtils.js';
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
        this.isShuttingDown = false;
        this.serviceErrors = new Map();
        
        // Enhanced error handling setup
        this.setupErrorHandling();
        this.setupMiddleware();
        this.initializeServices();
        this.setupRoutes();
        this.setupSocketHandlers();
    }

    /**
     * Phase 4.3: Enhanced error handling with async/await patterns
     */
    setupErrorHandling() {
        // Setup async signal handlers for graceful shutdown
        AsyncUtils.setupAsyncSignalHandlers({
            SIGTERM: async () => await this.gracefulShutdown('SIGTERM'),
            SIGINT: async () => await this.gracefulShutdown('SIGINT')
        });
        
        // Unhandled promise rejection handling
        process.on('unhandledRejection', (reason, promise) => {
            console.error('🚨 Unhandled Promise Rejection:', reason);
            console.error('Promise:', promise);
            this.logError('UNHANDLED_PROMISE_REJECTION', reason);
        });
        
        // Uncaught exception handling with async cleanup
        process.on('uncaughtException', async (error) => {
            console.error('🚨 Uncaught Exception:', error);
            this.logError('UNCAUGHT_EXCEPTION', error);
            
            try {
                // Attempt graceful cleanup before exit
                await AsyncUtils.withTimeout(
                    async () => await this.gracefulShutdown('UNCAUGHT_EXCEPTION'),
                    5000,
                    'Cleanup timeout'
                );
            } catch (cleanupError) {
                console.error('Error during cleanup:', cleanupError.message);
            } finally {
                process.exit(1);
            }
        });
        
        console.log('🛡️ Enhanced async error handling initialized');
    }
    
    setupMiddleware() {
        this.app.use(cors());
        this.app.use(express.json());
        this.app.use(express.static(path.join(process.cwd(), 'server', 'src', 'public')));
    }
    
    async initializeServices() {
        console.log('🔧 Initializing AudioDeck Connect services...');
        
        // Initialize core services with enhanced error handling
        const serviceInitializers = [
            {
                name: 'AudioPlayer',
                init: () => { this.audioManager = new AudioPlayer(); },
                critical: true
            },
            {
                name: 'VoicemeeterManager',
                init: () => { this.voicemeeterManager = new VoicemeeterManager(); },
                critical: false
            },
            {
                name: 'AdbManager', 
                init: () => { this.adbManager = new AdbManager(); },
                critical: false
            },
            {
                name: 'NetworkDiscoveryService',
                init: () => { this.discoveryService = new NetworkDiscoveryService(); },
                critical: false
            },
            {
                name: 'HealthMonitor',
                init: () => { this.healthMonitor = new HealthMonitor(); },
                critical: true
            }
        ];

        // Initialize services with error tracking
        for (const service of serviceInitializers) {
            try {
                console.log(`🔧 Initializing ${service.name}...`);
                service.init();
                console.log(`✅ ${service.name} initialized successfully`);
            } catch (error) {
                const errorMsg = `Failed to initialize ${service.name}: ${error.message}`;
                this.logError(service.name, error);
                
                if (service.critical) {
                    console.error(`🚨 Critical service ${service.name} failed to initialize`);
                    throw new Error(errorMsg);
                } else {
                    console.warn(`⚠️ Non-critical service ${service.name} failed: ${errorMsg}`);
                }
            }
        }
        
        // Start services with enhanced error handling and retry logic
        await this.startServicesWithRetry();
        
        console.log('🚀 AudioDeck Connect services initialization complete');
    }

    async startServicesWithRetry() {
        const serviceStarters = [
            {
                name: 'Voicemeeter',
                start: () => this.voicemeeterManager.connect(),
                critical: false,
                retries: 2
            },
            {
                name: 'ADB',
                start: () => this.adbManager.initialize(),
                critical: false,
                retries: 3
            },
            {
                name: 'NetworkDiscovery',
                start: () => this.discoveryService.start(),
                critical: false,
                retries: 1
            },
            {
                name: 'HealthMonitor',
                start: () => this.healthMonitor.startMonitoring(),
                critical: true,
                retries: 2
            }
        ];

        for (const service of serviceStarters) {
            await this.startServiceWithRetry(service);
        }
    }

    async startServiceWithRetry(service) {
        let lastError = null;
        
        for (let attempt = 1; attempt <= service.retries + 1; attempt++) {
            try {
                console.log(`🔄 Starting ${service.name} (attempt ${attempt}/${service.retries + 1})...`);
                await service.start();
                console.log(`✅ ${service.name} started successfully`);
                this.serviceErrors.delete(service.name);
                return;
            } catch (error) {
                lastError = error;
                console.warn(`⚠️ ${service.name} start attempt ${attempt} failed: ${error.message}`);
                
                if (attempt < service.retries + 1) {
                    const delay = Math.min(1000 * Math.pow(2, attempt - 1), 5000);
                    console.log(`⏳ Retrying ${service.name} in ${delay}ms...`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                }
            }
        }
        
        // All retries failed
        const errorMsg = `${service.name} failed to start after ${service.retries + 1} attempts: ${lastError.message}`;
        this.logError(service.name, lastError);
        
        if (service.critical) {
            throw new Error(errorMsg);
        } else {
            console.warn(`⚠️ Non-critical service ${service.name} will remain unavailable: ${errorMsg}`);
        }
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
            console.log(`📱 New client connected: ${socket.id}`);
            
            // Enhanced disconnect handling
            socket.on('disconnect', (reason) => {
                console.log(`📱 Client disconnected: ${socket.id} (${reason})`);
            });
            
            // Enhanced audio control events with error handling
            socket.on('play', async (data) => {
                try {
                    console.log(`🎵 Play request from ${socket.id}:`, data);
                    const result = await this.audioManager.playSound(data.filePath, data.volume, data.customDir);
                    socket.emit('playResult', { success: result, data });
                } catch (error) {
                    console.error(`❌ Play error for ${socket.id}:`, error);
                    this.logError('AUDIO_PLAY', error);
                    socket.emit('playResult', { success: false, error: error.message, data });
                }
            });
            
            socket.on('stop', async () => {
                try {
                    console.log(`🛑 Stop request from ${socket.id}`);
                    await this.audioManager.stopCurrentSound();
                    socket.emit('stopResult', { success: true });
                } catch (error) {
                    console.error(`❌ Stop error for ${socket.id}:`, error);
                    this.logError('AUDIO_STOP', error);
                    socket.emit('stopResult', { success: false, error: error.message });
                }
            });

            // Service status request
            socket.on('getStatus', () => {
                try {
                    const status = this.getServiceStatus();
                    socket.emit('statusUpdate', status);
                } catch (error) {
                    console.error(`❌ Status error for ${socket.id}:`, error);
                    socket.emit('statusUpdate', { error: error.message });
                }
            });
            
            // Error handling for socket events
            socket.on('error', (error) => {
                console.error(`🚨 Socket error for ${socket.id}:`, error);
                this.logError('SOCKET', error);
            });
            
            console.log(`✅ Socket handlers setup complete for ${socket.id}`);
        });

        // Handle IO errors
        this.io.on('error', (error) => {
            console.error('🚨 Socket.IO error:', error);
            this.logError('SOCKET_IO', error);
        });
    }
    
    /**
     * Enhanced error logging with categorization
     */
    logError(category, error) {
        const errorInfo = {
            timestamp: new Date().toISOString(),
            category,
            message: error.message,
            stack: error.stack,
            platform: process.platform
        };
        
        this.serviceErrors.set(category, errorInfo);
        
        // Could be extended to write to file or external logging service
        console.error(`🚨 [${category}] Error logged:`, errorInfo);
    }

    /**
     * Graceful shutdown with cleanup
     */
    async gracefulShutdown(signal) {
        if (this.isShuttingDown) {
            console.log('🚨 Force shutdown requested');
            process.exit(1);
        }
        
        this.isShuttingDown = true;
        console.log(`\n🛑 Graceful shutdown initiated (${signal})`);
        
        try {
            // Stop accepting new connections
            console.log('🔌 Closing server...');
            await AsyncUtils.promisify(this.server.close, this.server)();
            
            // Cleanup services
            console.log('🧹 Cleaning up services...');
            if (this.voicemeeterManager) {
                await this.voicemeeterManager.disconnect().catch(console.error);
            }
            if (this.adbManager) {
                await this.adbManager.shutdown().catch(console.error);
            }
            if (this.healthMonitor) {
                this.healthMonitor.stopMonitoring();
            }
            
            console.log('✅ Graceful shutdown complete');
            process.exit(0);
            
        } catch (error) {
            console.error('❌ Error during graceful shutdown:', error);
            process.exit(1);
        }
    }

    /**
     * Get service status with error information
     */
    getServiceStatus() {
        return {
            audioManager: this.audioManager?.getStatus?.() || 'unknown',
            voicemeeterManager: this.voicemeeterManager?.getStatus?.() || 'unknown',
            adbManager: this.adbManager?.getStatus?.() || 'unknown',
            discoveryService: this.discoveryService?.getStatus?.() || 'unknown',
            healthMonitor: this.healthMonitor?.getStatus?.() || 'unknown',
            errors: Object.fromEntries(this.serviceErrors)
        };
    }

    start() {
        this.server.listen(this.port, '0.0.0.0', () => {
            console.log(`\n============================================================`);
            console.log(`🎵 AudioDeck Connect Server`);
            console.log(`============================================================`);
            console.log(`Server running on port ${this.port}`);
            console.log(`Platform: ${process.platform}`);
            console.log(`Mode: Enterprise (v9.0.0)`);
            console.log(`🛡️ Enhanced error handling: Active`);
            console.log(`============================================================`);
        });
        
        // Handle server errors
        this.server.on('error', (error) => {
            console.error('🚨 Server error:', error);
            this.logError('SERVER', error);
        });
    }
}

// Start the server
const server = new AudioDeckServer();
server.start(); 