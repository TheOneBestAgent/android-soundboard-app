// AudioDeck Connect - ADB Device Manager
// Handles Android device communication and management

import { AdbDaemon, AdbClient } from '@yume-chan/adb';
import { USBDeviceManager } from './USBDeviceManager.js';
import EventEmitter from 'events';

class AdbManager extends EventEmitter {
    constructor() {
        super();
        this.devices = new Map();
        this.usbManager = new USBDeviceManager();
        this.daemon = null;
        this.client = null;
        this.isInitialized = false;
        
        console.log('🤖 ADB Manager initializing...');
    }
    
    async initialize() {
        try {
            if (this.isInitialized) {
                console.log('🤖 ADB Manager already initialized');
                return;
            }
            
            // Initialize USB device detection
            await this.usbManager.initialize();
            
            // Listen for USB device events
            this.usbManager.on('deviceAttached', async (device) => {
                console.log('📱 USB device attached:', device);
                await this.handleDeviceAttached(device);
            });
            
            this.usbManager.on('deviceDetached', async (device) => {
                console.log('📱 USB device detached:', device);
                await this.handleDeviceDetached(device);
            });
            
            // Start ADB daemon
            this.daemon = new AdbDaemon();
            await this.daemon.start();
            
            // Create ADB client
            this.client = new AdbClient(this.daemon);
            
            this.isInitialized = true;
            console.log('✅ ADB Manager initialized successfully');
            
        } catch (error) {
            console.error('❌ Failed to initialize ADB Manager:', error);
            throw error;
        }
    }
    
    async handleDeviceAttached(device) {
        try {
            // Check if device is an Android device
            if (!await this.isAndroidDevice(device)) {
                console.log('ℹ️ Not an Android device, skipping...');
                return;
            }
            
            // Connect to device
            const connection = await this.client.connect(device);
            
            // Get device info
            const deviceInfo = await this.getDeviceInfo(connection);
            
            // Store device
            this.devices.set(deviceInfo.serial, {
                device,
                connection,
                info: deviceInfo,
                status: 'connected',
                timestamp: new Date()
            });
            
            // Emit device connected event
            this.emit('deviceConnected', deviceInfo);
            
            console.log('✅ Android device connected:', deviceInfo.model);
            
        } catch (error) {
            console.error('❌ Failed to handle device attachment:', error);
            this.emit('error', error);
        }
    }
    
    async handleDeviceDetached(device) {
        try {
            // Find device in our map
            for (const [serial, data] of this.devices.entries()) {
                if (data.device === device) {
                    // Close connection
                    await data.connection.close();
                    
                    // Remove from map
                    this.devices.delete(serial);
                    
                    // Emit device disconnected event
                    this.emit('deviceDisconnected', data.info);
                    
                    console.log('📱 Android device disconnected:', data.info.model);
                    break;
                }
            }
        } catch (error) {
            console.error('❌ Failed to handle device detachment:', error);
            this.emit('error', error);
        }
    }
    
    async isAndroidDevice(device) {
        try {
            const descriptor = await device.getDescriptor();
            return descriptor.idVendor === 0x18D1 || // Google
                   descriptor.idVendor === 0x04E8 || // Samsung
                   descriptor.idVendor === 0x12D1;   // Huawei
        } catch (error) {
            console.error('❌ Failed to check device type:', error);
            return false;
        }
    }
    
    async getDeviceInfo(connection) {
        const props = await connection.getProperties();
        
        return {
            serial: props['ro.serialno'],
            model: props['ro.product.model'],
            manufacturer: props['ro.product.manufacturer'],
            version: props['ro.build.version.release'],
            sdk: props['ro.build.version.sdk'],
            fingerprint: props['ro.build.fingerprint'],
            capabilities: {
                audio: true,
                usb: true,
                adb: true
            }
        };
    }
    
    async executeCommand(serial, command) {
        const device = this.devices.get(serial);
        if (!device) {
            throw new Error(`Device ${serial} not found`);
        }
        
        try {
            const shell = await device.connection.shell(command);
            const output = await shell.readAll();
            return output.toString().trim();
        } catch (error) {
            console.error(`❌ Failed to execute command "${command}":`, error);
            throw error;
        }
    }
    
    getConnectedDevices() {
        return Array.from(this.devices.values()).map(({ info, status, timestamp }) => ({
            ...info,
            status,
            connectedSince: timestamp
        }));
    }
    
    async shutdown() {
        console.log('🤖 Shutting down ADB Manager...');
        
        // Close all device connections
        for (const [serial, data] of this.devices.entries()) {
            try {
                await data.connection.close();
                console.log(`📱 Closed connection to device: ${data.info.model}`);
            } catch (error) {
                console.error(`❌ Failed to close connection to device ${serial}:`, error);
            }
        }
        
        // Clear devices map
        this.devices.clear();
        
        // Stop USB manager
        await this.usbManager.shutdown();
        
        // Stop ADB daemon
        if (this.daemon) {
            await this.daemon.stop();
        }
        
        this.isInitialized = false;
        console.log('✅ ADB Manager shutdown complete');
    }
    
    getStatus() {
        return {
            isInitialized: this.isInitialized,
            connectedDevices: this.getConnectedDevices(),
            usbManagerStatus: this.usbManager.getStatus(),
            timestamp: new Date()
        };
    }
}

export default AdbManager; 