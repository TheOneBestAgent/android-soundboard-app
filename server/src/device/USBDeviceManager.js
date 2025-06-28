import { EventEmitter } from 'events';
import { spawn } from 'child_process';
import os from 'os';

/**
 * USB Device Manager
 * Handles USB device detection and management across platforms
 */
export class USBDeviceManager extends EventEmitter {
    constructor() {
        super();
        this.platform = process.platform;
        this.devices = new Map();
        this.monitoringInterval = null;
        this.isInitialized = false;
        
        console.log(`🔌 USBDeviceManager initializing for platform: ${this.platform}`);
    }
    
    async initialize() {
        try {
            this.isInitialized = true;
            console.log('✅ USBDeviceManager initialized');
            
            // Start monitoring for USB devices
            this.startMonitoring();
            
        } catch (error) {
            console.error('❌ USBDeviceManager initialization failed:', error);
            throw error;
        }
    }
    
    startMonitoring() {
        if (this.monitoringInterval) {
            return; // Already monitoring
        }
        
        console.log('👀 Starting USB device monitoring...');
        this.monitoringInterval = setInterval(() => {
            this.scanDevices();
        }, 3000);
        
        // Initial scan
        this.scanDevices();
    }
    
    stopMonitoring() {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
            console.log('🛑 Stopped USB device monitoring');
        }
    }
    
    async scanDevices() {
        try {
            const currentDevices = await this.getConnectedDevices();
            const currentSerials = new Set(currentDevices.map(d => d.serial));
            const previousSerials = new Set(this.devices.keys());
            
            // Check for new devices
            for (const device of currentDevices) {
                if (!previousSerials.has(device.serial)) {
                    this.devices.set(device.serial, device);
                    console.log(`🔌 USB device attached: ${device.serial}`);
                    this.emit('deviceAttached', device);
                }
            }
            
            // Check for removed devices
            for (const serial of previousSerials) {
                if (!currentSerials.has(serial)) {
                    const device = this.devices.get(serial);
                    this.devices.delete(serial);
                    console.log(`🔌 USB device detached: ${serial}`);
                    this.emit('deviceDetached', device);
                }
            }
            
        } catch (error) {
            // Silently handle errors to avoid spam
            // console.error('USB scan error:', error);
        }
    }
    
    async getConnectedDevices() {
        switch (this.platform) {
            case 'win32':
                return await this.getWindowsDevices();
            case 'darwin':
                return await this.getMacOSDevices();
            case 'linux':
                return await this.getLinuxDevices();
            default:
                console.warn(`Platform ${this.platform} not supported for USB detection`);
                return [];
        }
    }
    
    async getWindowsDevices() {
        return new Promise((resolve) => {
            try {
                const process = spawn('powershell', [
                    '-Command',
                    'Get-WmiObject -Class Win32_PnPEntity | Where-Object {$_.DeviceID -like "*USB*"} | Select-Object Name, DeviceID'
                ], { windowsHide: true });
                
                let output = '';
                process.stdout.on('data', (data) => {
                    output += data.toString();
                });
                
                process.on('close', () => {
                    const devices = this.parseWindowsOutput(output);
                    resolve(devices);
                });
                
                process.on('error', () => {
                    resolve([]);
                });
                
            } catch (error) {
                resolve([]);
            }
        });
    }
    
    async getMacOSDevices() {
        return new Promise((resolve) => {
            try {
                const process = spawn('system_profiler', ['SPUSBDataType', '-json']);
                
                let output = '';
                process.stdout.on('data', (data) => {
                    output += data.toString();
                });
                
                process.on('close', () => {
                    const devices = this.parseMacOSOutput(output);
                    resolve(devices);
                });
                
                process.on('error', () => {
                    resolve([]);
                });
                
            } catch (error) {
                resolve([]);
            }
        });
    }
    
    async getLinuxDevices() {
        return new Promise((resolve) => {
            try {
                const process = spawn('lsusb');
                
                let output = '';
                process.stdout.on('data', (data) => {
                    output += data.toString();
                });
                
                process.on('close', () => {
                    const devices = this.parseLinuxOutput(output);
                    resolve(devices);
                });
                
                process.on('error', () => {
                    resolve([]);
                });
                
            } catch (error) {
                resolve([]);
            }
        });
    }
    
    parseWindowsOutput(output) {
        const devices = [];
        const lines = output.split('\n');
        
        for (const line of lines) {
            if (line.includes('USB') && line.trim()) {
                const device = {
                    serial: this.generateSerial(),
                    name: line.trim(),
                    platform: 'win32'
                };
                devices.push(device);
            }
        }
        
        return devices;
    }
    
    parseMacOSOutput(output) {
        const devices = [];
        
        try {
            const data = JSON.parse(output);
            const usbData = data.SPUSBDataType || [];
            
            for (const item of usbData) {
                if (item._name && item.serial_num) {
                    const device = {
                        serial: item.serial_num,
                        name: item._name,
                        platform: 'darwin'
                    };
                    devices.push(device);
                }
            }
        } catch (error) {
            // Parse error, return empty array
        }
        
        return devices;
    }
    
    parseLinuxOutput(output) {
        const devices = [];
        const lines = output.split('\n');
        
        for (const line of lines) {
            if (line.includes('Bus') && line.includes('Device')) {
                const device = {
                    serial: this.generateSerial(),
                    name: line.trim(),
                    platform: 'linux'
                };
                devices.push(device);
            }
        }
        
        return devices;
    }
    
    generateSerial() {
        return `usb_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }
    
    getStatus() {
        return {
            isInitialized: this.isInitialized,
            platform: this.platform,
            monitoring: !!this.monitoringInterval,
            connectedDevices: this.devices.size
        };
    }
    
    async shutdown() {
        console.log('🔌 Shutting down USBDeviceManager...');
        
        this.stopMonitoring();
        this.devices.clear();
        this.isInitialized = false;
        
        console.log('✅ USBDeviceManager shutdown complete');
    }
} 