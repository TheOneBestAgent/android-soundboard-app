const { Bonjour } = require('bonjour-service');
const QRCode = require('qrcode');
const os = require('os');
const crypto = require('crypto');
const EventEmitter = require('events');

/**
 * NetworkDiscoveryService - Phase 2: Discovery & Automation
 * 
 * Provides automatic server discovery via mDNS/Bonjour, QR code pairing,
 * and network topology mapping for seamless connection setup.
 */
class NetworkDiscoveryService extends EventEmitter {
    constructor(serverPort = 3001, serverName = 'Soundboard Server') {
        super();
        
        this.serverPort = serverPort;
        this.serverName = serverName;
        this.bonjour = new Bonjour();
        this.advertisedService = null;
        this.discoveredServices = new Map();
        this.networkInfo = null;
        this.connectionToken = null;
        
        // Service configuration
        this.serviceType = 'soundboard';
        this.protocol = 'tcp';
        
        this.initializeNetworkInfo();
        this.generateConnectionToken();
        
        console.log('🔍 NetworkDiscoveryService initialized for Phase 2');
    }
    
    /**
     * Initialize network information and topology
     */
    initializeNetworkInfo() {
        const interfaces = os.networkInterfaces();
        const hostname = os.hostname();
        const platform = os.platform();
        
        // Find primary network interface
        let primaryInterface = null;
        let primaryAddress = null;
        
        for (const [name, addresses] of Object.entries(interfaces)) {
            for (const addr of addresses) {
                if (!addr.internal && addr.family === 'IPv4') {
                    primaryInterface = name;
                    primaryAddress = addr.address;
                    break;
                }
            }
            if (primaryInterface) break;
        }
        
        this.networkInfo = {
            hostname,
            platform,
            primaryInterface,
            primaryAddress,
            interfaces: this.getNetworkInterfaces(),
            capabilities: this.getServerCapabilities(),
            timestamp: new Date().toISOString()
        };
        
        console.log(`🌐 Network info initialized:`, {
            hostname,
            primaryAddress,
            interfaces: Object.keys(interfaces).length
        });
    }
    
    /**
     * Get formatted network interfaces information
     */
    getNetworkInterfaces() {
        const interfaces = os.networkInterfaces();
        const formattedInterfaces = {};
        
        for (const [name, addresses] of Object.entries(interfaces)) {
            formattedInterfaces[name] = addresses
                .filter(addr => !addr.internal)
                .map(addr => ({
                    address: addr.address,
                    family: addr.family,
                    netmask: addr.netmask,
                    mac: addr.mac
                }));
        }
        
        return formattedInterfaces;
    }
    
    /**
     * Get server capabilities for advertisement
     */
    getServerCapabilities() {
        return {
            audio_playback: true,
            file_upload: true,
            websocket_support: true,
            real_time_communication: true,
            health_monitoring: true,
            analytics: true,
            multi_format_support: ['mp3', 'wav', 'm4a', 'ogg'],
            connection_types: ['usb_adb', 'websocket', 'polling'],
            version: '7.0-phase2'
        };
    }
    
    /**
     * Generate secure connection token for authentication
     */
    generateConnectionToken() {
        this.connectionToken = crypto.randomBytes(32).toString('hex');
        console.log(`🔐 Generated connection token: ${this.connectionToken.substring(0, 8)}...`);
    }
    
    /**
     * Start mDNS/Bonjour service advertisement
     */
    startAdvertisement() {
        try {
            // Stop existing advertisement if any
            this.stopAdvertisement();
            
            const txtRecord = {
                version: '7.0-phase2',
                platform: this.networkInfo.platform,
                hostname: this.networkInfo.hostname,
                capabilities: JSON.stringify(this.networkInfo.capabilities),
                token: this.connectionToken,
                timestamp: new Date().toISOString(),
                // Additional metadata
                discovery_protocol: 'bonjour',
                service_type: 'soundboard',
                connection_methods: 'websocket,polling,usb',
                audio_formats: 'mp3,wav,m4a,ogg'
            };
            
            this.advertisedService = this.bonjour.publish({
                name: this.serverName,
                type: this.serviceType,
                protocol: this.protocol,
                port: this.serverPort,
                txt: txtRecord,
                host: this.networkInfo.primaryAddress
            });
            
            this.advertisedService.on('up', () => {
                console.log(`📡 mDNS service advertised: ${this.serverName} on ${this.networkInfo.primaryAddress}:${this.serverPort}`);
                this.emit('serviceAdvertised', {
                    name: this.serverName,
                    address: this.networkInfo.primaryAddress,
                    port: this.serverPort,
                    txtRecord
                });
            });
            
            this.advertisedService.on('error', (error) => {
                console.error('❌ mDNS advertisement error:', error);
                this.emit('advertisementError', error);
            });
            
            return true;
        } catch (error) {
            console.error('❌ Failed to start mDNS advertisement:', error);
            this.emit('advertisementError', error);
            return false;
        }
    }
    
    /**
     * Stop mDNS service advertisement
     */
    stopAdvertisement() {
        if (this.advertisedService) {
            this.advertisedService.stop();
            this.advertisedService = null;
            console.log('📡 mDNS advertisement stopped');
        }
    }
    
    /**
     * Start discovering other soundboard services on the network
     */
    startDiscovery() {
        try {
            const browser = this.bonjour.find({ type: this.serviceType }, (service) => {
                // Skip our own service
                if (service.port === this.serverPort && 
                    service.addresses.includes(this.networkInfo.primaryAddress)) {
                    return;
                }
                
                console.log(`🔍 Discovered soundboard service:`, {
                    name: service.name,
                    address: service.addresses[0],
                    port: service.port,
                    txt: service.txt
                });
                
                this.discoveredServices.set(service.name, {
                    name: service.name,
                    addresses: service.addresses,
                    port: service.port,
                    txt: service.txt,
                    discoveredAt: new Date().toISOString()
                });
                
                this.emit('serviceDiscovered', service);
            });
            
            browser.on('down', (service) => {
                console.log(`🔍 Service went down: ${service.name}`);
                this.discoveredServices.delete(service.name);
                this.emit('serviceDown', service);
            });
            
            console.log('🔍 Started mDNS discovery for soundboard services');
            return browser;
        } catch (error) {
            console.error('❌ Failed to start mDNS discovery:', error);
            this.emit('discoveryError', error);
            return null;
        }
    }
    
    /**
     * Generate QR code for instant pairing
     */
    async generatePairingQRCode(options = {}) {
        try {
            const connectionData = {
                type: 'soundboard_connection',
                version: '7.0-phase2',
                server: {
                    name: this.serverName,
                    address: this.networkInfo.primaryAddress,
                    port: this.serverPort,
                    hostname: this.networkInfo.hostname
                },
                connection: {
                    token: this.connectionToken,
                    methods: ['websocket', 'polling'],
                    endpoints: {
                        websocket: `ws://${this.networkInfo.primaryAddress}:${this.serverPort}/socket.io/`,
                        http: `http://${this.networkInfo.primaryAddress}:${this.serverPort}/`,
                        health: `http://${this.networkInfo.primaryAddress}:${this.serverPort}/health`
                    }
                },
                capabilities: this.networkInfo.capabilities,
                timestamp: new Date().toISOString(),
                expires: new Date(Date.now() + (options.expiryHours || 24) * 60 * 60 * 1000).toISOString()
            };
            
            const qrDataString = JSON.stringify(connectionData);
            
            // Generate QR code with high error correction for mobile scanning
            const qrCodeOptions = {
                errorCorrectionLevel: 'H',
                type: 'image/png',
                quality: 0.92,
                margin: 1,
                color: {
                    dark: '#000000',
                    light: '#FFFFFF'
                },
                width: options.size || 256,
                ...options.qrOptions
            };
            
            const qrCodeDataURL = await QRCode.toDataURL(qrDataString, qrCodeOptions);
            const qrCodeSVG = await QRCode.toString(qrDataString, { type: 'svg', ...qrCodeOptions });
            
            console.log(`📱 Generated pairing QR code for ${this.serverName}`);
            
            const qrCodeInfo = {
                dataURL: qrCodeDataURL,
                svg: qrCodeSVG,
                connectionData,
                metadata: {
                    size: qrCodeOptions.width,
                    format: 'PNG/SVG',
                    errorCorrection: qrCodeOptions.errorCorrectionLevel,
                    generatedAt: new Date().toISOString(),
                    expiresAt: connectionData.expires
                }
            };
            
            this.emit('qrCodeGenerated', qrCodeInfo);
            return qrCodeInfo;
            
        } catch (error) {
            console.error('❌ Failed to generate QR code:', error);
            this.emit('qrCodeError', error);
            throw error;
        }
    }
    
    /**
     * Validate incoming connection token
     */
    validateConnectionToken(token) {
        return token === this.connectionToken;
    }
    
    /**
     * Get network topology information
     */
    getNetworkTopology() {
        const interfaces = this.getNetworkInterfaces();
        const routes = this.getNetworkRoutes();
        
        return {
            hostname: this.networkInfo.hostname,
            platform: this.networkInfo.platform,
            primaryInterface: this.networkInfo.primaryInterface,
            primaryAddress: this.networkInfo.primaryAddress,
            interfaces,
            routes,
            services: {
                advertised: this.advertisedService ? {
                    name: this.serverName,
                    port: this.serverPort,
                    type: this.serviceType
                } : null,
                discovered: Array.from(this.discoveredServices.values())
            },
            capabilities: this.networkInfo.capabilities,
            timestamp: new Date().toISOString()
        };
    }
    
    /**
     * Get basic network routing information
     */
    getNetworkRoutes() {
        // Basic route detection - could be enhanced with more detailed analysis
        const interfaces = this.getNetworkInterfaces();
        const routes = [];
        
        for (const [name, addresses] of Object.entries(interfaces)) {
            for (const addr of addresses) {
                if (addr.family === 'IPv4') {
                    // Calculate network address
                    const ip = addr.address.split('.').map(Number);
                    const mask = addr.netmask.split('.').map(Number);
                    const network = ip.map((octet, i) => octet & mask[i]).join('.');
                    
                    routes.push({
                        interface: name,
                        network: network,
                        netmask: addr.netmask,
                        gateway: addr.address, // Simplified - actual gateway detection would require system calls
                        metric: name.includes('wifi') || name.includes('wlan') ? 20 : 10
                    });
                }
            }
        }
        
        return routes;
    }
    
    /**
     * Recommend optimal connection method based on network analysis
     */
    recommendConnectionMethod(clientInfo = {}) {
        const recommendations = [];
        
        // Analyze network conditions
        const isLocalNetwork = this.isLocalNetworkAddress(clientInfo.address);
        const hasUSBDebugging = clientInfo.usbDebugging || false;
        const networkQuality = this.assessNetworkQuality(clientInfo);
        
        if (hasUSBDebugging && clientInfo.platform === 'android') {
            recommendations.push({
                method: 'usb_adb',
                priority: 1,
                reason: 'USB ADB provides most stable connection',
                setup: 'Enable USB debugging and connect via USB cable'
            });
        }
        
        if (isLocalNetwork && networkQuality.latency < 50) {
            recommendations.push({
                method: 'websocket',
                priority: 2,
                reason: 'Low latency local network connection',
                setup: 'Connect to same WiFi network'
            });
        }
        
        recommendations.push({
            method: 'polling',
            priority: 3,
            reason: 'Fallback method for any network condition',
            setup: 'Works over any internet connection'
        });
        
        return {
            recommendations: recommendations.sort((a, b) => a.priority - b.priority),
            networkAnalysis: {
                isLocal: isLocalNetwork,
                quality: networkQuality,
                topology: this.getNetworkTopology()
            }
        };
    }
    
    /**
     * Check if address is on local network
     */
    isLocalNetworkAddress(address) {
        if (!address) return false;
        
        // Check for common local network ranges
        const localRanges = [
            /^192\.168\./,
            /^10\./,
            /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
            /^127\./,
            /^::1$/,
            /^fe80:/
        ];
        
        return localRanges.some(range => range.test(address));
    }
    
    /**
     * Assess network quality for connection recommendations
     */
    assessNetworkQuality(clientInfo) {
        // Simplified network quality assessment
        // In a real implementation, this would include actual latency testing
        
        const isLocal = this.isLocalNetworkAddress(clientInfo.address);
        const connectionType = clientInfo.connectionType || 'unknown';
        
        let estimatedLatency = 100; // Default
        let bandwidth = 'medium';
        let stability = 'good';
        
        if (isLocal) {
            estimatedLatency = 20;
            bandwidth = 'high';
            stability = 'excellent';
        } else if (connectionType === 'wifi') {
            estimatedLatency = 50;
            bandwidth = 'high';
            stability = 'good';
        } else if (connectionType === 'cellular') {
            estimatedLatency = 150;
            bandwidth = 'medium';
            stability = 'variable';
        }
        
        return {
            latency: estimatedLatency,
            bandwidth,
            stability,
            isLocal,
            connectionType
        };
    }
    
    /**
     * Get discovery service status
     */
    getStatus() {
        return {
            advertisement: {
                active: !!this.advertisedService,
                service: this.advertisedService ? {
                    name: this.serverName,
                    port: this.serverPort,
                    address: this.networkInfo.primaryAddress
                } : null
            },
            discovery: {
                servicesFound: this.discoveredServices.size,
                services: Array.from(this.discoveredServices.values())
            },
            network: this.networkInfo,
            connectionToken: this.connectionToken ? `${this.connectionToken.substring(0, 8)}...` : null,
            timestamp: new Date().toISOString()
        };
    }
    
    /**
     * Cleanup and shutdown
     */
    shutdown() {
        console.log('🔍 Shutting down NetworkDiscoveryService...');
        
        this.stopAdvertisement();
        
        if (this.bonjour) {
            this.bonjour.destroy();
        }
        
        this.discoveredServices.clear();
        this.removeAllListeners();
        
        console.log('🔍 NetworkDiscoveryService shutdown complete');
    }
}

module.exports = NetworkDiscoveryService; 