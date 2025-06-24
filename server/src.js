this.audioPlayer = new AudioPlayer();
this.voicemeeterManager = new VoicemeeterManager(this.audioPlayer);
this.connectedClients = new Map();
this.audioFiles = new Map(); // Store audio file metadata

// Enhanced connection monitoring system
this.healthMonitor = new ConnectionHealthMonitor();
this.reconnectionManager = new SmartReconnectionManager();

// Phase 2: Discovery & Automation services
// The USB service now creates its own AdbManager with the correct path
this.usbAutoDetection = new USBAutoDetectionService(this.port);
this.networkDiscovery = new NetworkDiscoveryService(this.port, 'Soundboard Server', this.usbAutoDetection.adbManager);

// Phase 3: Enhanced Connection Analytics
this.startTime = Date.now(); 