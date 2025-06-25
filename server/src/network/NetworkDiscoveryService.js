import { createAdvertisement, createBrowser } from '@homebridge/ciao';
import os from 'os';
import crypto from 'crypto';

/**
 * NetworkDiscoveryService - Phase 2: Discovery & Automation
 * 
 * Provides automatic server discovery via mDNS/Bonjour, QR code pairing,
 * and network topology mapping for seamless connection setup.
 */
class NetworkDiscoveryService {
    constructor() {
        this.serviceName = 'AudioDeck Connect';
        this.serviceType = 'audiodeck';
        this.port = process.env.PORT || 3001;
        this.discoveredServices = new Map();
        this.advertisement = null;
        this.browser = null;
        
        // Generate a unique token for this instance
        this.token = crypto.randomBytes(32).toString('hex');
        
        // Get network information
        this.networkInfo = this.getNetworkInfo();
        console.log('🌐 Network info initialized:', {
            hostname: this.networkInfo.hostname,
            primaryAddress: this.networkInfo.primaryAddress,
            interfaces: this.networkInfo.interfaceCount
        });
    }
    
    getNetworkInfo() {
        const interfaces = os.networkInterfaces();
        let primaryAddress = '';
        let interfaceCount = 0;
        
        // Find primary non-internal IPv4 address
        for (const [name, addrs] of Object.entries(interfaces)) {
            for (const addr of addrs) {
                if (addr.family === 'IPv4' && !addr.internal) {
                    primaryAddress = addr.address;
                    interfaceCount++;
                }
            }
        }
        
        return {
            hostname: os.hostname(),
            primaryAddress,
            interfaceCount
        };
    }
    
    async start() {
        try {
            // Start service advertisement
            this.advertisement = createAdvertisement('audiodeck');
            
            const txtRecord = {
                version: '8.0.0',
                platform: process.platform,
                hostname: this.networkInfo.hostname,
                capabilities: JSON.stringify({
                    audio_playback: true,
                    websocket_support: true,
                    real_time_communication: true,
                    health_monitoring: true,
                    analytics: true,
                    multi_format_support: ['mp3', 'wav', 'm4a', 'ogg'],
                    connection_types: ['websocket', 'polling']
                }),
                token: this.token,
                timestamp: new Date().toISOString()
            };
            
            await this.advertisement.advertise(this.serviceName, this.port, txtRecord);
            console.log('📡 Service advertisement started');
            
            // Start service discovery
            this.browser = createBrowser('audiodeck');
            
            this.browser.on('serviceUp', (service) => {
                console.log('🔍 Discovered AudioDeck service:', service.name);
                this.discoveredServices.set(service.name, {
                    name: service.name,
                    address: service.addresses[0],
                    port: service.port,
                    txt: service.txt,
                    timestamp: new Date()
                });
            });
            
            this.browser.on('serviceDown', (service) => {
                console.log('📉 AudioDeck service down:', service.name);
                this.discoveredServices.delete(service.name);
            });
            
            await this.browser.browse();
            console.log('🔍 Service discovery started');
            
        } catch (error) {
            console.error('❌ Failed to start network discovery:', error);
            throw error;
        }
    }
    
    async stop() {
        try {
            if (this.advertisement) {
                await this.advertisement.destroy();
                console.log('📡 Service advertisement stopped');
            }
            
            if (this.browser) {
                await this.browser.destroy();
                console.log('🔍 Service discovery stopped');
            }
        } catch (error) {
            console.error('❌ Failed to stop network discovery:', error);
            throw error;
        }
    }
    
    getStatus() {
        return {
            serviceName: this.serviceName,
            serviceType: this.serviceType,
            port: this.port,
            discoveredServices: Array.from(this.discoveredServices.values()),
            networkInfo: this.networkInfo,
            isAdvertising: !!this.advertisement,
            isBrowsing: !!this.browser
        };
    }
}

export default NetworkDiscoveryService; 