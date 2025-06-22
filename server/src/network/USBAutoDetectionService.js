const EventEmitter = require('events');
const { spawn, exec } = require('child_process');
const fs = require('fs').promises;
const path = require('path');

/**
 * USBAutoDetectionService - Phase 2: Discovery & Automation
 * 
 * Automatically detects USB-connected Android devices and sets up
 * ADB port forwarding for seamless connection establishment.
 */
class USBAutoDetectionService extends EventEmitter {
    constructor(adbManager, targetPort = 3001) {
        super();
        
        this.adbManager = adbManager;
        this.targetPort = targetPort;
        this.monitoringInterval = null;
        this.isMonitoring = false;
        this.connectedDevices = new Map();
        this.forwardedPorts = new Map();
        this.lastDeviceCheck = new Date();
        
        // Monitoring configuration
        this.checkInterval = 2000; // Check every 2 seconds
        this.retryDelay = 5000; // Retry failed operations after 5 seconds
        this.maxRetries = 3;
        
        console.log('🔌 USBAutoDetectionService initialized for Phase 2');
    }
    
    /**
     * Start monitoring for USB device connections
     */
    startMonitoring() {
        if (this.isMonitoring) {
            console.log('🔌 USB monitoring already active');
            return;
        }
        
        this.isMonitoring = true;
        console.log('🔌 Starting USB device monitoring...');
        
        // Initial device scan
        this.scanForDevices();
        
        // Set up periodic monitoring
        this.monitoringInterval = setInterval(() => {
            this.scanForDevices();
        }, this.checkInterval);
        
        this.emit('monitoringStarted');
    }
    
    /**
     * Stop monitoring for USB device connections
     */
    stopMonitoring() {
        if (!this.isMonitoring) {
            return;
        }
        
        this.isMonitoring = false;
        console.log('🔌 Stopping USB device monitoring...');
        
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
        }
        
        this.emit('monitoringStopped');
    }
    
    /**
     * Scan for connected USB devices
     */
    async scanForDevices() {
        try {
            const devices = await this.getConnectedDevices();
            await this.processDeviceChanges(devices);
            this.lastDeviceCheck = new Date();
        } catch (error) {
            console.error('❌ Error scanning for USB devices:', error);
            this.emit('scanError', error);
        }
    }
    
    /**
     * Get list of connected ADB devices
     */
    async getConnectedDevices() {
        return new Promise((resolve, reject) => {
            exec('adb devices -l', (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`ADB devices command failed: ${error.message}`));
                    return;
                }
                
                const devices = this.parseAdbDevicesOutput(stdout);
                resolve(devices);
            });
        });
    }
    
    /**
     * Parse ADB devices command output
     */
    parseAdbDevicesOutput(output) {
        const lines = output.split('\n');
        const devices = [];
        
        for (const line of lines) {
            // Skip header line and empty lines
            if (line.includes('List of devices') || line.trim() === '') {
                continue;
            }
            
            // Parse device line: "deviceId status product:name model:model device:device"
            const parts = line.trim().split(/\s+/);
            if (parts.length >= 2) {
                const deviceId = parts[0];
                const status = parts[1];
                
                // Extract additional info from the rest of the line
                const infoString = parts.slice(2).join(' ');
                const info = this.parseDeviceInfo(infoString);
                
                devices.push({
                    id: deviceId,
                    status: status,
                    product: info.product,
                    model: info.model,
                    device: info.device,
                    transport: info.transport || 'usb',
                    connectedAt: new Date().toISOString()
                });
            }
        }
        
        return devices;
    }
    
    /**
     * Parse device info string from ADB output
     */
    parseDeviceInfo(infoString) {
        const info = {};
        
        if (!infoString) return info;
        
        const parts = infoString.split(' ');
        for (const part of parts) {
            if (part.includes(':')) {
                const [key, value] = part.split(':', 2);
                info[key] = value;
            }
        }
        
        return info;
    }
    
    /**
     * Process changes in connected devices
     */
    async processDeviceChanges(currentDevices) {
        const currentDeviceIds = new Set(currentDevices.map(d => d.id));
        const previousDeviceIds = new Set(this.connectedDevices.keys());
        
        // Find newly connected devices
        const newDevices = currentDevices.filter(d => !previousDeviceIds.has(d.id));
        
        // Find disconnected devices
        const disconnectedDeviceIds = Array.from(previousDeviceIds)
            .filter(id => !currentDeviceIds.has(id));
        
        // Process new connections
        for (const device of newDevices) {
            await this.handleDeviceConnected(device);
        }
        
        // Process disconnections
        for (const deviceId of disconnectedDeviceIds) {
            await this.handleDeviceDisconnected(deviceId);
        }
        
        // Update device status for existing devices
        for (const device of currentDevices) {
            if (previousDeviceIds.has(device.id)) {
                const previousDevice = this.connectedDevices.get(device.id);
                if (previousDevice.status !== device.status) {
                    await this.handleDeviceStatusChanged(device, previousDevice);
                }
            }
        }
        
        // Update connected devices map
        this.connectedDevices.clear();
        for (const device of currentDevices) {
            this.connectedDevices.set(device.id, device);
        }
    }
    
    /**
     * Handle new device connection
     */
    async handleDeviceConnected(device) {
        console.log(`🔌 USB device connected: ${device.id} (${device.status})`);
        console.log(`   📱 Model: ${device.model || 'Unknown'}`);
        console.log(`   📦 Product: ${device.product || 'Unknown'}`);
        
        this.emit('deviceConnected', device);
        
        // Only proceed with port forwarding if device is authorized
        if (device.status === 'device') {
            await this.setupPortForwarding(device);
        } else if (device.status === 'unauthorized') {
            console.log(`🔐 Device ${device.id} requires authorization - please accept USB debugging prompt on device`);
            this.emit('deviceRequiresAuthorization', device);
        } else {
            console.log(`⚠️  Device ${device.id} status: ${device.status} - port forwarding not available`);
        }
    }
    
    /**
     * Handle device disconnection
     */
    async handleDeviceDisconnected(deviceId) {
        const device = this.connectedDevices.get(deviceId);
        console.log(`🔌 USB device disconnected: ${deviceId}`);
        
        // Clean up port forwarding
        await this.cleanupPortForwarding(deviceId);
        
        this.emit('deviceDisconnected', { id: deviceId, device });
    }
    
    /**
     * Handle device status change
     */
    async handleDeviceStatusChanged(device, previousDevice) {
        console.log(`🔌 Device ${device.id} status changed: ${previousDevice.status} → ${device.status}`);
        
        this.emit('deviceStatusChanged', { device, previousStatus: previousDevice.status });
        
        // If device became authorized, set up port forwarding
        if (device.status === 'device' && previousDevice.status === 'unauthorized') {
            console.log(`🔐 Device ${device.id} authorized - setting up port forwarding`);
            await this.setupPortForwarding(device);
        }
        
        // If device became unauthorized or offline, clean up port forwarding
        if (device.status !== 'device' && previousDevice.status === 'device') {
            console.log(`⚠️  Device ${device.id} no longer available - cleaning up port forwarding`);
            await this.cleanupPortForwarding(device.id);
        }
    }
    
    /**
     * Set up ADB port forwarding for a device
     */
    async setupPortForwarding(device) {
        try {
            console.log(`🔗 Setting up port forwarding for ${device.id}...`);
            
            // Use AdbManager if available, otherwise use direct ADB commands
            if (this.adbManager && typeof this.adbManager.setupPortForwarding === 'function') {
                const success = await this.adbManager.setupPortForwarding(device.id, this.targetPort);
                if (success) {
                    this.forwardedPorts.set(device.id, {
                        deviceId: device.id,
                        devicePort: 8080, // Standard port for Android app
                        serverPort: this.targetPort,
                        setupAt: new Date().toISOString(),
                        method: 'adbManager'
                    });
                    
                    console.log(`✅ Port forwarding established for ${device.id}: device:8080 → server:${this.targetPort}`);
                    this.emit('portForwardingEstablished', { device, port: this.targetPort });
                } else {
                    throw new Error('AdbManager port forwarding failed');
                }
            } else {
                // Fallback to direct ADB command
                await this.setupDirectPortForwarding(device);
            }
            
        } catch (error) {
            console.error(`❌ Failed to set up port forwarding for ${device.id}:`, error);
            this.emit('portForwardingError', { device, error });
            
            // Retry after delay
            setTimeout(() => {
                this.retryPortForwarding(device);
            }, this.retryDelay);
        }
    }
    
    /**
     * Set up port forwarding using direct ADB commands
     */
    async setupDirectPortForwarding(device) {
        return new Promise((resolve, reject) => {
            const command = `adb -s ${device.id} reverse tcp:8080 tcp:${this.targetPort}`;
            
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`Port forwarding command failed: ${error.message}`));
                    return;
                }
                
                this.forwardedPorts.set(device.id, {
                    deviceId: device.id,
                    devicePort: 8080,
                    serverPort: this.targetPort,
                    setupAt: new Date().toISOString(),
                    method: 'direct'
                });
                
                console.log(`✅ Direct port forwarding established for ${device.id}`);
                this.emit('portForwardingEstablished', { device, port: this.targetPort });
                resolve();
            });
        });
    }
    
    /**
     * Retry port forwarding setup
     */
    async retryPortForwarding(device, attempt = 1) {
        if (attempt > this.maxRetries) {
            console.error(`❌ Max retries exceeded for port forwarding on ${device.id}`);
            this.emit('portForwardingFailed', { device, reason: 'max_retries_exceeded' });
            return;
        }
        
        console.log(`🔄 Retrying port forwarding for ${device.id} (attempt ${attempt}/${this.maxRetries})`);
        
        try {
            await this.setupPortForwarding(device);
        } catch (error) {
            setTimeout(() => {
                this.retryPortForwarding(device, attempt + 1);
            }, this.retryDelay * attempt); // Exponential backoff
        }
    }
    
    /**
     * Clean up port forwarding for a device
     */
    async cleanupPortForwarding(deviceId) {
        const portInfo = this.forwardedPorts.get(deviceId);
        if (!portInfo) {
            return; // No port forwarding to clean up
        }
        
        try {
            console.log(`🧹 Cleaning up port forwarding for ${deviceId}...`);
            
            // Use AdbManager if available
            if (this.adbManager && typeof this.adbManager.cleanupPortForwarding === 'function') {
                await this.adbManager.cleanupPortForwarding(deviceId);
            } else {
                // Fallback to direct ADB command
                await this.cleanupDirectPortForwarding(deviceId);
            }
            
            this.forwardedPorts.delete(deviceId);
            console.log(`✅ Port forwarding cleaned up for ${deviceId}`);
            this.emit('portForwardingCleaned', { deviceId, portInfo });
            
        } catch (error) {
            console.error(`❌ Failed to clean up port forwarding for ${deviceId}:`, error);
            this.emit('portForwardingCleanupError', { deviceId, error });
        }
    }
    
    /**
     * Clean up port forwarding using direct ADB commands
     */
    async cleanupDirectPortForwarding(deviceId) {
        return new Promise((resolve, reject) => {
            const command = `adb -s ${deviceId} reverse --remove tcp:8080`;
            
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    // Don't treat as error if device is already disconnected
                    if (error.message.includes('device not found') || 
                        error.message.includes('device offline')) {
                        console.log(`📱 Device ${deviceId} already disconnected - port forwarding automatically cleaned`);
                        resolve();
                        return;
                    }
                    reject(new Error(`Port forwarding cleanup failed: ${error.message}`));
                    return;
                }
                
                console.log(`✅ Direct port forwarding cleaned for ${deviceId}`);
                resolve();
            });
        });
    }
    
    /**
     * Get current USB connection status
     */
    getConnectionStatus() {
        const devices = Array.from(this.connectedDevices.values());
        const forwardedPorts = Array.from(this.forwardedPorts.values());
        
        return {
            monitoring: this.isMonitoring,
            devicesConnected: devices.length,
            devicesWithPortForwarding: forwardedPorts.length,
            devices: devices.map(device => ({
                ...device,
                hasPortForwarding: this.forwardedPorts.has(device.id),
                portForwardingInfo: this.forwardedPorts.get(device.id) || null
            })),
            lastCheck: this.lastDeviceCheck.toISOString(),
            configuration: {
                checkInterval: this.checkInterval,
                targetPort: this.targetPort,
                maxRetries: this.maxRetries
            }
        };
    }
    
    /**
     * Force a device scan
     */
    async forceScan() {
        console.log('🔍 Forcing USB device scan...');
        await this.scanForDevices();
        return this.getConnectionStatus();
    }
    
    /**
     * Test USB debugging setup for a specific device
     */
    async testUSBDebugging(deviceId) {
        try {
            console.log(`🧪 Testing USB debugging for ${deviceId}...`);
            
            // Test basic ADB connection
            const testResult = await this.runAdbCommand(deviceId, 'shell echo "USB debugging test"');
            
            if (testResult.success) {
                console.log(`✅ USB debugging test passed for ${deviceId}`);
                return {
                    success: true,
                    message: 'USB debugging is working correctly',
                    output: testResult.output
                };
            } else {
                console.log(`❌ USB debugging test failed for ${deviceId}`);
                return {
                    success: false,
                    message: 'USB debugging test failed',
                    error: testResult.error
                };
            }
            
        } catch (error) {
            console.error(`❌ USB debugging test error for ${deviceId}:`, error);
            return {
                success: false,
                message: 'USB debugging test encountered an error',
                error: error.message
            };
        }
    }
    
    /**
     * Run an ADB command on a specific device
     */
    async runAdbCommand(deviceId, command) {
        return new Promise((resolve) => {
            const fullCommand = `adb -s ${deviceId} ${command}`;
            
            exec(fullCommand, (error, stdout, stderr) => {
                if (error) {
                    resolve({
                        success: false,
                        error: error.message,
                        stderr: stderr
                    });
                } else {
                    resolve({
                        success: true,
                        output: stdout.trim(),
                        stderr: stderr
                    });
                }
            });
        });
    }
    
    /**
     * Get detailed device information
     */
    async getDeviceDetails(deviceId) {
        try {
            const commands = {
                model: 'shell getprop ro.product.model',
                manufacturer: 'shell getprop ro.product.manufacturer',
                version: 'shell getprop ro.build.version.release',
                sdk: 'shell getprop ro.build.version.sdk',
                serial: 'get-serialno'
            };
            
            const details = { id: deviceId };
            
            for (const [key, command] of Object.entries(commands)) {
                const result = await this.runAdbCommand(deviceId, command);
                if (result.success) {
                    details[key] = result.output;
                }
            }
            
            return details;
        } catch (error) {
            console.error(`❌ Failed to get device details for ${deviceId}:`, error);
            return { id: deviceId, error: error.message };
        }
    }
    
    /**
     * Shutdown the USB auto-detection service
     */
    shutdown() {
        console.log('🔌 Shutting down USBAutoDetectionService...');
        
        this.stopMonitoring();
        
        // Clean up all port forwarding
        const deviceIds = Array.from(this.forwardedPorts.keys());
        for (const deviceId of deviceIds) {
            this.cleanupPortForwarding(deviceId).catch(error => {
                console.error(`Error cleaning up port forwarding for ${deviceId}:`, error);
            });
        }
        
        this.connectedDevices.clear();
        this.forwardedPorts.clear();
        this.removeAllListeners();
        
        console.log('🔌 USBAutoDetectionService shutdown complete');
    }
}

module.exports = USBAutoDetectionService; 