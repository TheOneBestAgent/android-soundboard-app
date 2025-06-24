const path = require('path');
// Load environment variables from .env file
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });

const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const fs = require('fs-extra');
const AudioPlayer = require('./audio/AudioPlayer');
const VoicemeeterManager = require('./audio/VoicemeeterManager');
const AdbManager = require('./network/AdbManager');
const ConnectionHealthMonitor = require('./network/ConnectionHealthMonitor');
const SmartReconnectionManager = require('./network/SmartReconnectionManager');
const NetworkDiscoveryService = require('./network/NetworkDiscoveryService');
const USBAutoDetectionService = require('./network/USBAutoDetectionService');
const { Server } = require('socket.io');
const ConnectionAnalytics = require('./network/ConnectionAnalytics');

class SoundboardServer {
    constructor() {
        this.app = express();
        this.server = http.createServer(this.app);
        
        // Configure Socket.io for enhanced connection management
        this.io = new Server(this.server, {
            cors: {
                origin: "*",
                methods: ["GET", "POST"]
            },
            // Allow both polling and websocket transports for compatibility
            transports: ['polling', 'websocket'],
            // Balanced timeouts for stability
            pingTimeout: 30000,   // 30 seconds
            pingInterval: 15000,  // 15 seconds
            // Connection optimizations
            upgradeTimeout: 10000, // 10 seconds for transport upgrade
            maxHttpBufferSize: 50e6, // 50MB buffer for audio uploads
            // Enhanced connection features
            allowEIO3: true,
            serveClient: false,
            connectionStateRecovery: {
                maxDisconnectionDuration: 2 * 60 * 1000, // 2 minutes
                skipMiddlewares: true,
            },
            // Compression for better performance
            httpCompression: true,
            perMessageDeflate: {
                threshold: 1024,
                concurrencyLimit: 10,
                // Use defaults for other options
            }
        });
        
        this.audioPlayer = new AudioPlayer();
        this.voicemeeterManager = new VoicemeeterManager(this.audioPlayer);
        this.connectedClients = new Map();
        this.audioFiles = new Map(); // Store audio file metadata
        
        // Enhanced connection monitoring system
        this.healthMonitor = new ConnectionHealthMonitor();
        this.reconnectionManager = new SmartReconnectionManager();
        
        // Phase 2: Discovery & Automation services
        // The USB service now creates its own AdbManager with the correct path
        this.usbAutoDetection = new USBAutoDetectionService();
        this.networkDiscovery = new NetworkDiscoveryService(this.port, 'Soundboard Server');
        
        // Phase 3: Enhanced Connection Analytics
        this.startTime = Date.now();
        this.connectionStats = {
            totalConnections: 0,
            activeConnections: new Map(),
            connectionHistory: [],
            startTime: this.startTime,
            totalRequests: 0,
            failedRequests: 0,
            averageLatency: 0,
            transportMetrics: {
                websocket: { connections: 0, failures: 0, avgLatency: 0 },
                http: { connections: 0, failures: 0, avgLatency: 0 },
                usb: { connections: 0, failures: 0, avgLatency: 0 }
            }
        };
        
        // Setup enhanced monitoring event handlers
        this.setupEnhancedMonitoring();
        
        this.setupMiddleware();
        this.setupRoutes();
        this.setupSocketHandlers();
        this.setupAudioDirectory();
        
        this.voicemeeterManager.initializeVoicemeeter();
        this.usbAutoDetection.startMonitoring();
    }
    
    setupEnhancedMonitoring() {
        // Enhanced health monitoring event handlers
        this.healthMonitor.on('healthPrediction', (prediction) => {
            console.log(`🔮 Health prediction for ${prediction.socketId}:`, {
                avgLatency: prediction.avgLatency,
                stability: prediction.predictedStability,
                riskFactors: prediction.riskFactors,
                recommendations: prediction.recommendations.length
            });
            
            // Send health prediction to client if needed
            this.io.to(prediction.socketId).emit('health_prediction', {
                stability: prediction.predictedStability,
                quality: prediction.avgLatency < 50 ? 'excellent' : 
                        prediction.avgLatency < 100 ? 'good' : 
                        prediction.avgLatency < 200 ? 'fair' : 'poor',
                recommendations: prediction.recommendations
            });
        });
        
        this.healthMonitor.on('connectionError', (errorData) => {
            console.log(`⚠️  Connection error detected for ${errorData.socketId}:`, errorData.error);
            
            // Analyze and potentially trigger reconnection strategy
            const connectionHistory = this.getConnectionHistory(errorData.socketId);
            const analysis = this.reconnectionManager.analyzeDisconnectionCause(
                errorData.socketId, 
                errorData.error.type, 
                connectionHistory
            );
            
            console.log(`🧠 Reconnection analysis:`, analysis);
        });
        
        this.reconnectionManager.on('reconnectionRecommendation', (recommendation) => {
            console.log(`💡 Reconnection recommendation for ${recommendation.socketId}:`, recommendation);
            
            // Send reconnection guidance to client
            this.io.to(recommendation.socketId).emit('reconnection_guidance', {
                strategy: recommendation.strategy,
                estimatedDelay: recommendation.estimatedDelay,
                maxAttempts: recommendation.maxAttempts,
                tips: recommendation.tips
            });
        });
        
        // Phase 2: Setup Discovery & Automation services
        this.setupPhase2Services();
        
        console.log('🔧 Enhanced connection monitoring and Phase 2 services initialized');
    }
    
    setupPhase2Services() {
        // Network Discovery Service events
        this.networkDiscovery.on('serviceAdvertised', (serviceInfo) => {
            console.log('📡 mDNS service advertised:', serviceInfo);
        });
        
        this.networkDiscovery.on('serviceDiscovered', (service) => {
            console.log('🔍 Discovered soundboard service:', service.name);
        });
        
        this.networkDiscovery.on('qrCodeGenerated', (qrInfo) => {
            console.log('📱 QR code generated for pairing');
        });
        
        // USB Auto-Detection Service events
        this.usbAutoDetection.on('deviceConnected', (device) => {
            console.log(`🔌 USB device auto-detected: ${device.id}`);
        });
        
        this.usbAutoDetection.on('portForwardingEstablished', (info) => {
            console.log(`✅ Auto port forwarding: ${info.device.id} → :${info.port}`);
        });
        
        this.usbAutoDetection.on('deviceRequiresAuthorization', (device) => {
            console.log(`🔐 Device ${device.id} needs USB debugging authorization`);
        });
        
        // Start Phase 2 services
        this.networkDiscovery.startAdvertisement();
        this.networkDiscovery.startDiscovery();
        this.usbAutoDetection.startMonitoring();
        
        console.log('🚀 Phase 2 Discovery & Automation services started');
    }
    
    getConnectionHistory(socketId) {
        const connection = this.healthMonitor.connections.get(socketId);
        if (!connection) {
            return {
                recentFailures: 0,
                longestConnection: 0,
                networkType: 'unknown',
                serverRestarts: 0
            };
        }
        
        const now = Date.now();
        const connectionDuration = now - connection.startTime;
        const recentErrors = connection.errors.filter(e => now - e.timestamp < 300000); // Last 5 minutes
        
        return {
            recentFailures: recentErrors.length,
            longestConnection: connectionDuration,
            networkType: connection.clientInfo.platform === 'android' ? 'mobile' : 'wifi',
            serverRestarts: 0, // Could be tracked separately
            transportUpgrades: connection.transportUpgrades,
            avgLatency: connection.pingHistory.length > 0 ? 
                connection.pingHistory.reduce((sum, p) => sum + p.latency, 0) / connection.pingHistory.length : 0,
            errorRate: recentErrors.length / Math.max(1, connection.pingHistory.length)
        };
    }
    
    setupMiddleware() {
        this.app.use(cors());
        this.app.use(express.json());
        this.app.use(express.urlencoded({ extended: true }));
        
        // Serve static files from audio directory
        this.app.use('/audio', express.static(path.join(__dirname, '../audio')));
        
        // Logging middleware
        this.app.use((req, res, next) => {
            console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
            next();
        });
    }
    
    setupRoutes() {
        // Health check endpoint
        this.app.get('/health', (req, res) => {
            const uptime = Date.now() - this.startTime;
            const globalAnalytics = this.healthMonitor.getGlobalAnalytics();
            const healthStatus = {
                status: 'healthy',
                uptime: uptime,
                connections: {
                    total: globalAnalytics.totalConnections,
                    active: globalAnalytics.activeConnections,
                    history: this.connectionStats.connectionHistory.slice(-10) // Last 10 connections
                },
                server: {
                    memory: process.memoryUsage(),
                    timestamp: new Date().toISOString()
                }
            };
            
            res.json(healthStatus);
        });
        
        // Server information
        this.app.get('/info', (req, res) => {
            res.json({
                server_name: 'Soundboard Server',
                version: '1.0.0',
                platform: process.platform,
                hostname: require('os').hostname(),
                supported_formats: ['mp3', 'wav', 'm4a', 'ogg'],
                voicemeeter: this.voicemeeterManager.getStatus(),
                endpoints: {
                    health: '/health',
                    info: '/info',
                    audio_files: '/audio-files',
                    play_audio_data: '/play-audio-data',
                    voicemeeter_status: '/voicemeeter/status',
                    voicemeeter_control: '/voicemeeter/control',
                    voicemeeter_volume_recommendations: '/voicemeeter/volume-recommendations',
                    connection_analytics: '/analytics/connections',
                    global_analytics: '/analytics/global',
                    reconnection_stats: '/analytics/reconnection',
                    // Phase 2: Discovery & Automation endpoints
                    network_discovery: '/discovery/network',
                    qr_pairing: '/discovery/qr-code',
                    usb_status: '/discovery/usb',
                    connection_recommendations: '/discovery/recommendations'
                }
            });
        });

        // Audio files listing
        this.app.get('/audio-files', async (req, res) => {
            console.log(`${new Date().toISOString()} - GET /audio-files`);
            try {
                const audioDir = path.join(__dirname, '../audio');
                const files = await this.scanAudioFiles(audioDir);
                // Return just the files array to match Android app expectation
                res.json(files);
            } catch (error) {
                console.error('Error listing audio files:', error);
                res.status(500).json({
                    status: 'error',
                    message: error.message
                });
            }
        });

        // New endpoint: Play audio data sent from Android
        this.app.post('/play-audio-data', express.raw({ type: 'application/octet-stream', limit: '50mb' }), async (req, res) => {
            try {
                console.log(`${new Date().toISOString()} - POST /play-audio-data - Received ${req.body.length} bytes`);
                
                const buttonId = req.headers['x-button-id'];
                const fileName = req.headers['x-file-name'] || 'uploaded_audio';
                const volume = parseFloat(req.headers['x-volume']) || 1.0;
                
                // Create temporary file with proper extension
                const tempDir = path.join(__dirname, '../temp');
                await fs.ensureDir(tempDir);
                
                // Ensure the temp file has a proper audio extension
                const fileExtension = path.extname(fileName) || '.mp3'; // Default to .mp3 if no extension
                const baseFileName = path.basename(fileName, path.extname(fileName));
                const tempFilePath = path.join(tempDir, `temp_${Date.now()}_${baseFileName}${fileExtension}`);
                
                // Write audio data to temporary file
                await fs.writeFile(tempFilePath, req.body);
                
                console.log(`Playing uploaded audio: ${fileName} (${req.body.length} bytes)`);
                console.log(`📄 Temp file created at: ${tempFilePath}`);
                
                // Verify the file exists and get its stats
                const fileExists = await fs.pathExists(tempFilePath);
                console.log(`📋 Temp file exists: ${fileExists}`);
                
                if (fileExists) {
                    const stats = await fs.stat(tempFilePath);
                    console.log(`📊 Temp file size: ${stats.size} bytes`);
                    console.log(`📅 Temp file created: ${stats.birthtime}`);
                    
                    // Validate file size matches uploaded data
                    if (stats.size !== req.body.length) {
                        console.warn(`⚠️  File size mismatch: expected ${req.body.length}, got ${stats.size}`);
                    }
                } else {
                    console.error(`❌ ERROR: Temp file was not created at ${tempFilePath}`);
                    throw new Error(`Temp file was not created successfully: ${tempFilePath}`);
                }
                
                // Play the audio file via VoicemeeterManager (with fallback to direct audio)
                const success = await this.voicemeeterManager.playSound(tempFilePath, volume, buttonId);
                
                // Clean up temporary file after a delay with improved error handling
                setTimeout(async () => {
                    try {
                        const stillExists = await fs.pathExists(tempFilePath);
                        if (stillExists) {
                            await fs.remove(tempFilePath);
                            console.log(`🗄️  Cleaned up temp file: ${path.basename(tempFilePath)}`);
                        }
                    } catch (error) {
                        console.warn(`⚠️  Could not clean up temp file ${path.basename(tempFilePath)}:`, error.message);
                    }
                }, 10000); // 10 seconds delay
                
                res.json({
                    status: success ? 'success' : 'error',
                    message: success ? `Playing ${fileName} on computer speakers` : 'Failed to play audio',
                    button_id: buttonId,
                    file_size: req.body.length,
                    timestamp: new Date().toISOString()
                });
                
            } catch (error) {
                console.error('Error in play-audio-data handler:', error);
                res.status(500).json({
                    status: 'error',
                    message: error.message,
                    timestamp: new Date().toISOString()
                });
            }
        });

        // Audio test endpoint
        this.app.get('/audio/test', async (req, res) => {
            try {
                console.log('🔊 Audio test endpoint called');
                const testResult = await this.audioPlayer.testAudio();
                
                res.json({
                    status: testResult ? 'success' : 'error',
                    message: testResult ? 'Audio system test passed' : 'Audio system test failed',
                    platform: process.platform,
                    timestamp: new Date().toISOString()
                });
            } catch (error) {
                console.error('🔊 Audio test endpoint error:', error);
                res.status(500).json({
                    status: 'error',
                    message: error.message,
                    timestamp: new Date().toISOString()
                });
            }
        });

        // Play audio file test endpoint
        this.app.get('/audio/play/:filename', async (req, res) => {
            try {
                const filename = req.params.filename;
                console.log(`🔊 Playing audio file: ${filename}`);
                
                const audioPath = path.join(__dirname, '../audio', filename);
                const success = await this.audioPlayer.playSound(audioPath, 1.0);
                
                res.json({
                    status: success ? 'success' : 'error',
                    message: success ? `Playing ${filename}` : `Failed to play ${filename}`,
                    filename: filename,
                    timestamp: new Date().toISOString()
                });
            } catch (error) {
                console.error('🔊 Play audio file error:', error);
                res.status(500).json({
                    status: 'error',
                    message: error.message,
                    timestamp: new Date().toISOString()
                });
            }
        });

        // ADB status endpoint
        this.app.get('/adb/status', async (req, res) => {
            const isAvailable = await this.adbManager.checkAdbAvailable();
            res.json({
                adb_available: isAvailable,
                timestamp: new Date().toISOString()
            });
        });

        // ADB devices endpoint
        this.app.get('/adb/devices', async (req, res) => {
            const devices = await this.adbManager.listDevices();
            res.json({
                devices,
                count: devices.length,
                timestamp: new Date().toISOString()
            });
        });

        // Voicemeeter status endpoint
        this.app.get('/voicemeeter/status', (req, res) => {
            res.json({
                status: 'success',
                voicemeeter: this.voicemeeterManager.getStatus(),
                timestamp: new Date().toISOString()
            });
        });

        // Voicemeeter volume recommendations endpoint
        this.app.get('/voicemeeter/volume-recommendations', (req, res) => {
            res.json({
                status: 'success',
                recommendations: this.voicemeeterManager.getVolumeRecommendations(),
                tips: {
                    normalization: "Volume normalization is automatically applied based on filename patterns",
                    cassette: "When using Voicemeeter cassette player, individual volume control is more precise",
                    strips: "Strip-based control allows for real-time volume adjustment during playback",
                    presets: "Use volume presets (25%, 50%, 75%, 100%) for consistent levels"
                },
                timestamp: new Date().toISOString()
            });
        });

        // Voicemeeter control endpoint
        this.app.post('/voicemeeter/control', async (req, res) => {
            try {
                const { action, stripIndex, busIndex, value, parameter } = req.body;
                
                if (!action) {
                    return res.status(400).json({
                        status: 'error',
                        message: 'Action is required'
                    });
                }
                
                let result = false;
                
                switch (action) {
                    case 'setStripMute':
                        result = await this.voicemeeterManager.setStripMute(stripIndex, value);
                        break;
                    case 'setStripGain':
                        result = await this.voicemeeterManager.setStripGain(stripIndex, value);
                        break;
                    case 'setBusMute':
                        result = await this.voicemeeterManager.setBusMute(busIndex, value);
                        break;
                    case 'setBusGain':
                        result = await this.voicemeeterManager.setBusGain(busIndex, value);
                        break;
                    case 'connect':
                        result = await this.voicemeeterManager.connect();
                        break;
                    case 'disconnect':
                        await this.voicemeeterManager.disconnect();
                        result = true;
                        break;
                    default:
                        return res.status(400).json({
                            status: 'error',
                            message: `Unknown action: ${action}`
                        });
                }
                
                res.json({
                    status: result ? 'success' : 'error',
                    action,
                    result,
                    voicemeeter: this.voicemeeterManager.getStatus(),
                    timestamp: new Date().toISOString()
                });
                
            } catch (error) {
                console.error('Error in voicemeeter control:', error);
                res.status(500).json({
                    status: 'error',
                    message: error.message,
                    timestamp: new Date().toISOString()
                });
            }
        });

        // Enhanced Analytics Endpoints
        this.app.get('/analytics/connections', (req, res) => {
            const socketId = req.query.socketId;
            if (socketId) {
                const analytics = this.healthMonitor.getConnectionAnalytics(socketId);
                res.json(analytics || { error: 'Connection not found' });
            } else {
                // Return all active connections analytics
                const allAnalytics = {};
                for (const [id] of this.healthMonitor.connections) {
                    allAnalytics[id] = this.healthMonitor.getConnectionAnalytics(id);
                }
                res.json(allAnalytics);
            }
        });

        this.app.get('/analytics/global', (req, res) => {
            const globalAnalytics = this.healthMonitor.getGlobalAnalytics();
            res.json(globalAnalytics);
        });

        this.app.get('/analytics/reconnection', (req, res) => {
            const reconnectionStats = this.reconnectionManager.getGlobalStats();
            res.json(reconnectionStats);
        });

        // Phase 2: Discovery & Automation Endpoints
        this.app.get('/discovery/network', (req, res) => {
            const networkStatus = this.networkDiscovery.getStatus();
            const topology = this.networkDiscovery.getNetworkTopology();
            
            res.json({
                status: networkStatus,
                topology: topology,
                timestamp: new Date().toISOString()
            });
        });

        this.app.get('/discovery/qr-code', async (req, res) => {
            try {
                const options = {
                    size: parseInt(req.query.size) || 256,
                    expiryHours: parseInt(req.query.expiryHours) || 24
                };
                
                const qrCodeInfo = await this.networkDiscovery.generatePairingQRCode(options);
                
                res.json({
                    qrCode: qrCodeInfo.dataURL,
                    svg: qrCodeInfo.svg,
                    connectionData: qrCodeInfo.connectionData,
                    metadata: qrCodeInfo.metadata
                });
            } catch (error) {
                console.error('❌ QR code generation error:', error);
                res.status(500).json({
                    error: 'Failed to generate QR code',
                    message: error.message
                });
            }
        });

        this.app.get('/discovery/usb', (req, res) => {
            const usbStatus = this.usbAutoDetection.getConnectionStatus();
            res.json(usbStatus);
        });

        this.app.post('/discovery/usb/scan', async (req, res) => {
            try {
                const status = await this.usbAutoDetection.forceScan();
                res.json({
                    message: 'USB scan completed',
                    status: status
                });
            } catch (error) {
                console.error('❌ USB scan error:', error);
                res.status(500).json({
                    error: 'USB scan failed',
                    message: error.message
                });
            }
        });

        this.app.get('/discovery/recommendations', (req, res) => {
            const clientInfo = {
                address: req.ip || req.connection.remoteAddress,
                platform: req.query.platform || 'unknown',
                usbDebugging: req.query.usbDebugging === 'true',
                connectionType: req.query.connectionType || 'unknown'
            };
            
            const recommendations = this.networkDiscovery.recommendConnectionMethod(clientInfo);
            res.json(recommendations);
        });

        this.app.get('/discovery/services', (req, res) => {
            const discoveredServices = Array.from(this.networkDiscovery.discoveredServices.values());
            res.json({
                services: discoveredServices,
                count: discoveredServices.length,
                lastScan: new Date().toISOString()
            });
        });
    }
    
    setupSocketHandlers() {
        // Handle both polling and WebSocket connections
        this.io.on('connection', (socket) => {
            console.log(`✅ Client connected: ${socket.id}`);
            console.log(`   🚀 Transport: ${socket.conn.transport.name}`);
            console.log(`   📍 Address: ${socket.handshake.address}`);
            
            // Enhanced connection tracking
            const clientInfo = {
                id: socket.id,
                connectedAt: new Date().toISOString(),
                transport: socket.conn.transport.name,
                address: socket.handshake.address,
                userAgent: socket.handshake.headers['user-agent'],
                platform: socket.handshake.query.platform || 'unknown'
            };
            
            // Track with health monitor
            this.healthMonitor.trackConnection(socket.id, clientInfo);
            
            // Update connection statistics
            if (this.connectionStats) {
                this.connectionStats.totalConnections++;
                this.connectionStats.activeConnections.set(socket.id, clientInfo);
            }
            
            // Log initial transport and let Socket.io handle upgrades automatically
            console.log(`🔍 Initial transport: ${socket.conn.transport.name} for ${socket.id}`);
            if (socket.handshake.address === '127.0.0.1' || socket.handshake.address === '::1') {
                console.log(`🏠 Localhost connection detected - WebSocket upgrade will happen automatically if supported`);
            }
            
            // Handle transport upgrades
            socket.conn.on('upgrade', () => {
                console.log(`🚀 Transport upgraded to: ${socket.conn.transport.name} for ${socket.id}`);
                
                // Update transport stats
                if (this.connectionStats) {
                    const client = this.connectionStats.activeConnections.get(socket.id);
                    if (client) {
                        client.transport = socket.conn.transport.name;
                        this.connectionStats.activeConnections.set(socket.id, client);
                    }
                }
                
                // Track with health monitor
                this.healthMonitor.recordTransportUpgrade(
                    socket.id, 
                    'polling', 
                    socket.conn.transport.name
                );
            });
            
            // Log active connections based on transport type
            if (this.connectionStats) {
                if (socket.conn.transport.name === 'websocket') {
                    console.log(`🔗 Active WebSocket connections: ${Array.from(this.connectionStats.activeConnections.values()).filter(c => c.transport === 'websocket').length}`);
                } else {
                    console.log(`🔗 Active connections: ${this.connectionStats.activeConnections.size}`);
                }
            }
            
            // Enhanced WebSocket ping/pong handling with latency tracking
            socket.on('ping', (data) => {
                const pingTime = data && data.timestamp ? data.timestamp : Date.now();
                const latency = Date.now() - pingTime;
                
                console.log(`🏓 ${socket.conn.transport.name === 'websocket' ? 'WebSocket' : 'Polling'} ping from ${socket.id} (${latency}ms)`);
                
                // Record latency with health monitor
                this.healthMonitor.recordLatency(socket.id, latency);
                
                socket.emit('pong', { 
                    timestamp: Date.now(),
                    serverUptime: Date.now() - this.startTime,
                    latency: latency
                });
            });
            
            // Handle play sound commands
            socket.on('play_sound', (data) => {
                console.log(`🎵 Play sound request from ${socket.id}:`, data);
                // Handle sound playing logic here
            });
            
            // Handle client info updates
            socket.on('client_info', (data) => {
                console.log(`📋 Client info from ${socket.id}:`, data);
                if (this.connectionStats) {
                    const client = this.connectionStats.activeConnections.get(socket.id);
                    if (client) {
                        Object.assign(client, data);
                        this.connectionStats.activeConnections.set(socket.id, client);
                    }
                }
            });
            
            // Enhanced disconnection handling with analysis
            socket.on('disconnect', (reason) => {
                console.log(`❌ ${socket.conn.transport.name === 'websocket' ? 'WebSocket' : 'Client'} disconnected: ${socket.id}, reason: ${reason}`);
                
                // End health monitoring for this connection
                this.healthMonitor.endConnection(socket.id, reason);
                
                // Analyze disconnection for future reconnection strategies
                const connectionHistory = this.getConnectionHistory(socket.id);
                const analysis = this.reconnectionManager.analyzeDisconnectionCause(
                    socket.id, 
                    reason, 
                    connectionHistory
                );
                
                console.log(`🧠 Disconnection analysis: ${analysis.cause} (${analysis.severity} severity, ${analysis.recoverability} recoverability)`);
                
                // Update connection stats
                if (this.connectionStats) {
                    this.connectionStats.activeConnections.delete(socket.id);
                    console.log(`🔗 Active ${socket.conn.transport.name === 'websocket' ? 'WebSocket ' : ''}connections: ${socket.conn.transport.name === 'websocket' ? Array.from(this.connectionStats.activeConnections.values()).filter(c => c.transport === 'websocket').length : this.connectionStats.activeConnections.size}`);
                }
            });
            
            // Enhanced error handling with detailed tracking
            socket.on('error', (error) => {
                console.error(`❌ ${socket.conn.transport.name === 'websocket' ? 'WebSocket' : 'Socket'} error for ${socket.id}:`, error);
                
                // Record error with health monitor
                this.healthMonitor.recordError(socket.id, 'socket_error', error.message || error.toString());
            });

            // Send initial connection confirmation
            socket.emit('server_status', {
                connected: true,
                transport: socket.conn.transport.name,
                serverTime: new Date().toISOString(),
                capabilities: ['audio_forward', 'file_upload', 'ping_pong'],
                uptime: Date.now() - this.startTime
            });
        });
    }
    
    async setupAudioDirectory() {
        const audioDir = path.join(__dirname, '../audio');
        try {
            await fs.ensureDir(audioDir);
            console.log(`Audio directory ready: ${audioDir}`);
            
            // Create sample audio files info (for testing)
            const sampleFiles = [
                'sample1.mp3',
                'sample2.wav',
                'applause.mp3',
                'drumroll.wav'
            ];
            
            console.log('Place your audio files in:', audioDir);
            console.log('Supported formats: MP3, WAV, M4A, OGG');
            
        } catch (error) {
            console.error('Error setting up audio directory:', error);
        }
    }
    
    async scanAudioFiles(directory) {
        const supportedExtensions = ['.mp3', '.wav', '.m4a', '.ogg'];
        const files = [];
        
        try {
            const entries = await fs.readdir(directory, { withFileTypes: true });
            
            for (const entry of entries) {
                if (entry.isFile()) {
                    const ext = path.extname(entry.name).toLowerCase();
                    if (supportedExtensions.includes(ext)) {
                        const filePath = path.join(directory, entry.name);
                        const stats = await fs.stat(filePath);
                        
                        files.push({
                            name: entry.name,
                            path: entry.name, // Relative path for client
                            size: stats.size,
                            format: ext.substring(1),
                            modified: stats.mtime
                        });
                    }
                }
            }
        } catch (error) {
            console.error('Error scanning directory:', error);
        }
        
        return files;
    }
    
    async start(port = 3001) {
        this.port = port || process.env.PORT;

        try {
            // Check for ADB availability via the USB service's manager
            const adbAvailable = await this.usbAutoDetection.adbManager.checkAdbAvailable();
            if (adbAvailable) {
                console.log('✅ ADB is available.');
        } else {
                console.warn('⚠️ ADB not found. USB features will be disabled.');
        }
        } catch (error) {
            console.error('❌ Error checking for ADB:', error.message);
        }

        this.server.listen(this.port, () => {
            console.log(`\n============================================================`);
            console.log('🎵 Soundboard Server Started (Network Mode)');
            console.log('='.repeat(60));
            console.log(`Server running on port ${this.port}`);
            console.log(`Computer: ${require('os').hostname()}`);
            console.log(`Platform: ${process.platform}`);
            console.log(`Connection: Network & USB with ADB Port Forwarding`);
            console.log('');
            console.log('Endpoints:');
            console.log(`  Health Check: http://localhost:${this.port}/health`);
            console.log(`  Server Info:  http://localhost:${this.port}/info`);
            console.log(`  Audio Files:  http://localhost:${this.port}/audio-files`);
            console.log(`  ADB Status:   http://localhost:${this.port}/adb/status`);
            console.log(`  ADB Devices:  http://localhost:${this.port}/adb/devices`);
            console.log('');
            console.log('Network Access:');
            const os = require('os');
            const interfaces = os.networkInterfaces();
            Object.keys(interfaces).forEach(name => {
                interfaces[name].forEach(iface => {
                    if (iface.family === 'IPv4' && !iface.internal) {
                        console.log(`  ${name}: http://${iface.address}:${this.port}`);
                    }
                });
            });
            console.log('');
            console.log('Setup Instructions:');
            console.log('1. Enable Developer Options on your Android device');
            console.log('2. Enable USB Debugging in Developer Options');
            console.log('3. Connect Android device via USB cable');
            console.log('4. Accept USB debugging authorization on device');
            console.log('5. Port forwarding will be setup automatically');
            console.log('');
            console.log('Socket.io available for real-time communication');
            console.log('Ready to accept connections from Android app');
            console.log('='.repeat(60));
        });
    }
}

// Create and start server
const server = new SoundboardServer();
const port = process.env.PORT || 3001;
server.start(port);

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n🛑 Graceful shutdown initiated...');
    
    // Clean up ADB connections
    if (server.adbManager) {
        console.log('Stopping device monitoring...');
        server.adbManager.stopDeviceMonitoring();
    }
    
    // Phase 2: Clean up Discovery & Automation services
    if (server.networkDiscovery) {
        console.log('Shutting down network discovery service...');
        server.networkDiscovery.shutdown();
    }
    
    if (server.usbAutoDetection) {
        console.log('Shutting down USB auto-detection service...');
        server.usbAutoDetection.shutdown();
    }
    
    // Clean up Voicemeeter
    if (server.voicemeeterManager) {
        console.log('Shutting down Voicemeeter manager...');
        await server.voicemeeterManager.shutdown();
    }
    
    // Clean up temp files
    try {
        const tempDir = path.join(__dirname, '../temp');
        if (await fs.pathExists(tempDir)) {
            console.log('Cleaning up temp directory...');
            await fs.emptyDir(tempDir);
        }
    } catch (error) {
        console.warn('Could not clean up temp directory:', error.message);
    }
    
    // Close all socket connections
    server.io.close(() => {
        console.log('✅ All socket connections closed');
        
        // Close HTTP server
        server.server.close(() => {
            console.log('✅ HTTP server closed');
            process.exit(0);
        });
    });
});

process.on('SIGTERM', () => {
    console.log('\n🛑 SIGTERM received - shutting down gracefully...');
    
    server.io.close(() => {
        server.server.close(() => {
            process.exit(0);
        });
    });
});

console.log('🎵 Soundboard server enhanced with improved connection management');

module.exports = SoundboardServer; 