const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const path = require('path');
const fs = require('fs-extra');
const AudioPlayer = require('./audio/AudioPlayer');
const VoicemeeterManager = require('./audio/VoicemeeterManager');
const AdbManager = require('./network/AdbManager');
const { Server } = require('socket.io');

class SoundboardServer {
    constructor() {
        this.app = express();
        this.server = http.createServer(this.app);
        
        // Configure Socket.io with enhanced transport handling for stability
        this.io = new Server(this.server, {
            cors: {
                origin: "*",
                methods: ["GET", "POST"]
            },
            // Enhanced transport configuration for better stability
            transports: ['websocket', 'polling'], // Prefer websocket for localhost
            // Increased timeouts to prevent transport errors
            pingTimeout: 90000, // 90 seconds (increased from 60)
            pingInterval: 30000, // 30 seconds (increased from 25)
            // Upgrade timeout - more generous for mobile connections
            upgradeTimeout: 45000, // 45 seconds
            // Connection state recovery with longer duration
            connectionStateRecovery: {
                maxDisconnectionDuration: 5 * 60 * 1000, // 5 minutes
                skipMiddlewares: true,
            },
            // Additional stability options
            allowEIO3: true, // Allow Engine.IO v3 clients
            serveClient: false, // Don't serve client files
            // Enhanced transport stability
            httpCompression: true, // Enable compression
            perMessageDeflate: true, // Enable WebSocket compression
            maxHttpBufferSize: 1e8, // 100MB buffer for large audio files
            // Polling configuration for better stability
            pollingTimeout: 30000, // 30 seconds for polling requests
            // Connection validation
            allowRequest: (req, callback) => {
                // Allow all connections but log them
                const origin = req.headers.origin || 'unknown';
                console.log(`🔗 Connection request from: ${origin}`);
                callback(null, true);
            }
        });
        
        this.audioPlayer = new AudioPlayer();
        this.voicemeeterManager = new VoicemeeterManager(this.audioPlayer);
        this.adbManager = new AdbManager();
        this.connectedClients = new Map();
        this.audioFiles = new Map(); // Store audio file metadata
        
        this.setupMiddleware();
        this.setupRoutes();
        this.setupSocketHandlers();
        this.setupAudioDirectory();
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
            console.log(`${new Date().toISOString()} - GET /health`);
            res.json({
                status: 'ok',
                server_version: '1.0.0',
                connected_clients: this.connectedClients.size,
                supported_formats: ['mp3', 'wav', 'm4a', 'ogg'],
                timestamp: new Date().toISOString()
            });
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
                    voicemeeter_volume_recommendations: '/voicemeeter/volume-recommendations'
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
    }
    
    setupSocketHandlers() {
        this.io.on('connection', (socket) => {
            const clientInfo = {
                id: socket.id,
                connectedAt: new Date(),
                lastActivity: new Date(),
                transport: socket.conn.transport.name,
                remoteAddress: socket.handshake.address
            };
            
            this.connectedClients.set(socket.id, clientInfo);
            console.log(`📱 Android device connected: ${socket.id} (transport: ${clientInfo.transport})`);
            
            // Send initial connection confirmation with server details
            socket.emit('server_status', {
                connected: true,
                serverTime: new Date().toISOString(),
                capabilities: ['audio_forward', 'file_upload', 'ping_pong', 'authentication'],
                transport: clientInfo.transport
            });

            // Handle transport upgrades with detailed logging
            socket.conn.on('upgrade', () => {
                const newTransport = socket.conn.transport.name;
                console.log(`🔄 Transport upgraded for ${socket.id}: ${clientInfo.transport} → ${newTransport}`);
                clientInfo.transport = newTransport;
                this.connectedClients.set(socket.id, clientInfo);
                
                // Log successful upgrade for debugging
                console.log(`✅ ${socket.id} now using ${newTransport} transport`);
            });

            // Enhanced transport error handling
            socket.conn.on('error', (error) => {
                console.error(`❌ Transport error for ${socket.id} (${clientInfo.transport}):`, error.message);
                // Log additional error details for debugging
                if (error.code) {
                    console.error(`   Error code: ${error.code}`);
                }
                if (error.type) {
                    console.error(`   Error type: ${error.type}`);
                }
            });
            
            // Handle transport close events
            socket.conn.on('close', (reason) => {
                console.log(`🔌 Transport closed for ${socket.id}: ${reason}`);
            });

            // Handle authentication (for compatibility with Android app)
            socket.on('authenticate', (data) => {
                try {
                    console.log(`🔐 Authentication request from ${socket.id}:`, data);
                    
                    // Parse authentication data
                    let authData;
                    if (typeof data === 'string') {
                        authData = JSON.parse(data);
                    } else {
                        authData = data;
                    }
                    
                    // Update client info
                    clientInfo.lastActivity = new Date();
                    clientInfo.authenticated = true;
                    clientInfo.clientType = authData.client_type;
                    clientInfo.version = authData.version;
                    this.connectedClients.set(socket.id, clientInfo);
                    
                    // Send authentication success response
                    socket.emit('authenticated', {
                        status: 'success',
                        message: 'Authentication successful',
                        server_info: {
                            computer_name: require('os').hostname(),
                            server_version: '1.0.0',
                            supported_formats: ['mp3', 'wav', 'm4a', 'ogg'],
                            platform: process.platform,
                            capabilities: ['audio_forward', 'file_upload', 'ping_pong'],
                            transport: clientInfo.transport
                        }
                    });
                    
                    console.log(`✅ Client ${socket.id} authenticated successfully (${authData.client_type} v${authData.version})`);
                    
                } catch (error) {
                    console.error(`❌ Authentication error for ${socket.id}:`, error.message);
                    socket.emit('authenticated', {
                        status: 'error',
                        message: 'Authentication failed',
                        error: error.message
                    });
                }
            });

            // Handle audio file forwarding
            socket.on('forward_audio', async (data) => {
                try {
                    if (!data.audioData || !data.fileName) {
                        socket.emit('forward_error', { error: 'Missing audio data or file name' });
                        return;
                    }

                    // Update client activity
                    clientInfo.lastActivity = new Date();
                    this.connectedClients.set(socket.id, clientInfo);

                    console.log(`🎵 Forwarding audio from ${socket.id}: ${data.fileName}`);
                    
                    // Ensure temp directory exists
                    const tempDir = path.join(process.cwd(), 'temp');
                    if (!fs.existsSync(tempDir)) {
                        fs.mkdirSync(tempDir, { recursive: true });
                    }
                    
                    // Create temp file with proper extension
                    const fileExtension = path.extname(data.fileName) || '.wav';
                    const timestamp = Date.now();
                    const tempFileName = `temp_${timestamp}_${data.fileName.replace(/[^a-zA-Z0-9.-_]/g, '_')}`;
                    const tempFilePath = path.join(tempDir, tempFileName);

                    // Convert Base64 to Buffer and write file
                    const audioBuffer = Buffer.from(data.audioData, 'base64');
                    fs.writeFileSync(tempFilePath, audioBuffer);
                    
                    // Verify file was created and has content
                    if (!fs.existsSync(tempFilePath)) {
                        throw new Error(`Failed to create temp file: ${tempFilePath}`);
                    }
                    
                    const fileStats = fs.statSync(tempFilePath);
                    if (fileStats.size === 0) {
                        fs.unlinkSync(tempFilePath); // Clean up empty file
                        throw new Error(`Temp file is empty: ${tempFilePath}`);
                    }
                    
                    console.log(`📁 Audio file saved: ${tempFileName} (${fileStats.size} bytes)`);

                    // Play audio through Voicemeeter
                    if (this.voicemeeterManager) {
                        try {
                            await this.voicemeeterManager.playAudio(tempFilePath);
                            console.log('🔊 Audio forwarded to Voicemeeter successfully');
                            socket.emit('forward_success', { 
                                message: 'Audio forwarded successfully',
                                fileName: data.fileName,
                                fileSize: fileStats.size
                            });
                        } catch (voicemeeterError) {
                            console.error('❌ Voicemeeter playback failed:', voicemeeterError.message);
                            socket.emit('forward_error', { 
                                error: 'Failed to play audio through Voicemeeter',
                                details: voicemeeterError.message
                            });
                        }
                    } else {
                        console.log('⚠️ Voicemeeter not available - audio file saved but not played');
                        socket.emit('forward_success', { 
                            message: 'Audio file saved (Voicemeeter not available)',
                            fileName: data.fileName,
                            fileSize: fileStats.size
                        });
                    }

                    // Schedule file cleanup after a delay
                    setTimeout(() => {
                        try {
                            if (fs.existsSync(tempFilePath)) {
                                fs.unlinkSync(tempFilePath);
                                console.log(`🧹 Cleaned up temp file: ${tempFileName}`);
                            }
                        } catch (cleanupError) {
                            console.error('❌ Error cleaning up temp file:', cleanupError.message);
                        }
                    }, 30000); // Clean up after 30 seconds

                } catch (error) {
                    console.error(`❌ Audio forwarding error from ${socket.id}:`, error.message);
                    socket.emit('forward_error', { 
                        error: 'Failed to forward audio',
                        details: error.message
                    });
                }
            });

            // Enhanced ping/pong handling for connection health monitoring
            socket.on('ping', (data) => {
                // Update client activity
                clientInfo.lastActivity = new Date();
                this.connectedClients.set(socket.id, clientInfo);
                
                // Support both old and new ping formats
                const pingData = data || {};
                const serverTime = Date.now();
                
                // Create comprehensive pong response
                const response = {
                    ...pingData,
                    serverTime: serverTime,
                    serverTimestamp: serverTime, // For compatibility
                    transport: clientInfo.transport,
                    connectionId: socket.id
                };
                
                // Support both clientTimestamp and timestamp for latency calculation
                if (typeof pingData === 'number') {
                    // Simple timestamp format
                    response.clientTimestamp = pingData;
                } else if (pingData.timestamp) {
                    response.clientTimestamp = pingData.timestamp;
                } else if (typeof pingData === 'object' && pingData.clientTimestamp) {
                    response.clientTimestamp = pingData.clientTimestamp;
                }
                
                // Log ping/pong for debugging (only occasionally to avoid spam)
                if (Math.random() < 0.1) { // 10% chance to log
                    console.log(`📡 Ping/Pong with ${socket.id} (${clientInfo.transport})`);
                }
                
                socket.emit('pong', response);
            });

            // Handle disconnect with more detailed logging
            socket.on('disconnect', (reason) => {
                const clientData = this.connectedClients.get(socket.id);
                const sessionDuration = clientData ? 
                    Math.round((Date.now() - clientData.connectedAt.getTime()) / 1000) : 0;
                
                console.log(`📱 Android device disconnected: ${socket.id} (reason: ${reason}, session: ${sessionDuration}s)`);
                
                // Clean up client data
                this.connectedClients.delete(socket.id);
                
                // Log disconnect reason details
                if (reason === 'transport error') {
                    console.log(`⚠️ Transport error disconnect - this may indicate network instability`);
                } else if (reason === 'ping timeout') {
                    console.log(`⏰ Ping timeout disconnect - client may have network issues`);
                }
            });

            // Handle connection errors
            socket.on('error', (error) => {
                console.error(`❌ Socket error for ${socket.id}:`, error);
            });
        });
        
        // Log connection statistics periodically
        setInterval(() => {
            const activeConnections = this.connectedClients.size;
            if (activeConnections > 0) {
                console.log(`📊 Active connections: ${activeConnections}`);
            }
        }, 60000); // Every minute
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
        // Initialize ADB
        console.log('🔌 Initializing ADB connection...');
        const adbAvailable = await this.adbManager.checkAdbAvailable();
        
        if (adbAvailable) {
            console.log('✅ ADB is available');
            
            // Track properly forwarded devices to prevent spam
            const forwardedDevices = new Set();
            let lastDeviceState = '';
            
            // Start device monitoring
            this.adbManager.startDeviceMonitoring((devices) => {
                // Create a hash of current device state to detect changes
                const currentState = devices.map(d => `${d.id}:${d.status}`).sort().join(',');
                
                // Only process if device state actually changed
                if (currentState !== lastDeviceState) {
                    lastDeviceState = currentState;
                    
                    const connectedDevices = devices.filter(d => d.connected);
                    console.log('📱 Connected devices:', connectedDevices);
                    
                    // Setup port forwarding for newly connected devices only
                    connectedDevices.forEach(async (device) => {
                        if (!forwardedDevices.has(device.id)) {
                            try {
                                const success = await this.adbManager.setupSoundboardForwarding(device.id, port);
                                if (success) {
                                    console.log(`🔗 Port forwarding established for ${device.id}`);
                                    forwardedDevices.add(device.id);
                                }
                            } catch (err) {
                                // Only log non-duplicate errors
                                if (!err.message.includes('Address already in use')) {
                                    console.error('❌ Port forwarding error:', err.message);
                                }
                            }
                        }
                    });
                    
                    // Remove disconnected devices from tracking
                    const connectedIds = new Set(connectedDevices.map(d => d.id));
                    forwardedDevices.forEach(deviceId => {
                        if (!connectedIds.has(deviceId)) {
                            console.log(`📱 Device disconnected: ${deviceId}`);
                            forwardedDevices.delete(deviceId);
                        }
                    });
                }
                
                // Emit device status to all connected clients (only on changes)
                if (currentState !== lastDeviceState) {
                    this.io.emit('device_status', {
                        devices,
                        timestamp: new Date().toISOString()
                    });
                }
            });
        } else {
            console.log('❌ ADB not available - USB connection will not work');
            console.log('💡 Make sure Android SDK platform-tools are installed and in PATH');
        }

        // Handle port conflicts gracefully
        this.server.on('error', (err) => {
            if (err.code === 'EADDRINUSE') {
                console.error(`❌ Port ${port} is already in use!`);
                console.log(`💡 To fix this, run: lsof -ti:${port} | xargs kill -9`);
                console.log(`💡 Then restart the server with: npm start`);
                process.exit(1);
            } else {
                console.error('❌ Server error:', err);
                process.exit(1);
            }
        });

        this.server.listen(port, '127.0.0.1', () => {
            console.log('='.repeat(60));
            console.log('🎵 Soundboard Server Started (USB Mode)');
            console.log('='.repeat(60));
            console.log(`Server running on port ${port}`);
            console.log(`Computer: ${require('os').hostname()}`);
            console.log(`Platform: ${process.platform}`);
            console.log(`Connection: USB Cable with ADB Port Forwarding`);
            console.log('');
            console.log('Endpoints:');
            console.log(`  Health Check: http://localhost:${port}/health`);
            console.log(`  Server Info:  http://localhost:${port}/info`);
            console.log(`  Audio Files:  http://localhost:${port}/audio-files`);
            console.log(`  ADB Status:   http://localhost:${port}/adb/status`);
            console.log(`  ADB Devices:  http://localhost:${port}/adb/devices`);
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
    console.log('\nShutting down server gracefully...');
    
    // Clean up ADB connections
    if (server.adbManager) {
        console.log('Stopping device monitoring...');
        server.adbManager.stopDeviceMonitoring();
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
    
    console.log('Server shutdown complete');
    process.exit(0);
});

module.exports = SoundboardServer; 