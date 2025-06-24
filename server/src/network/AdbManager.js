const { exec } = require('child_process');
const path = require('path');
const os = require('os');

class AdbManager {
    constructor(adbPath) {
        this.isConnected = false;
        this.connectedDevices = [];
        this.forwardedPorts = new Set();
        this.adbPath = adbPath || 'adb';
        this.devices = new Map();
        console.log(`🔧 AdbManager initialized with ADB path: ${this.adbPath}`);
    }

    findAdbPath() {
        // Try to find ADB in common locations
        const platform = os.platform();
        const fs = require('fs');
        
        let possiblePaths = [];

        if (platform === 'win32') {
            possiblePaths = [
                'adb.exe',
                path.join(process.env.LOCALAPPDATA || '', 'Android', 'Sdk', 'platform-tools', 'adb.exe'),
                path.join('C:', 'Android', 'sdk', 'platform-tools', 'adb.exe')
            ];
        } else {
            possiblePaths = [
                'adb',
                path.join(os.homedir(), 'Library', 'Android', 'sdk', 'platform-tools', 'adb'),
                path.join(os.homedir(), 'Android', 'Sdk', 'platform-tools', 'adb'),
                '/usr/local/bin/adb'
            ];
        }

        // Find the first existing path
        for (const possiblePath of possiblePaths) {
            try {
                if (fs.existsSync(possiblePath)) {
                    console.log(`Found ADB at: ${possiblePath}`);
                    return possiblePath;
                }
            } catch (error) {
                // Continue to next path
            }
        }

        // Fallback to 'adb' (assuming it's in PATH)
        return 'adb';
    }

    async checkAdbAvailable() {
        return new Promise((resolve) => {
            exec(`"${this.adbPath}" --version`, (error, stdout, stderr) => {
                if (error) {
                    console.error('ADB not available:', error.message);
                    resolve(false);
                } else {
                    console.log('ADB version:', stdout.trim());
                    resolve(true);
                }
            });
        });
    }

    async getDevices() {
        return new Promise((resolve, reject) => {
            exec(`"${this.adbPath}" devices -l`, (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`ADB devices command failed: ${error.message}`));
                    return;
                }
                const devices = this.parseAdbDevicesOutput(stdout);
                resolve(devices);
            });
        });
    }

    parseAdbDevicesOutput(output) {
        const lines = output.split(/\\r?\\n/);
        const devices = [];
        for (const line of lines) {
            if (line.trim() === '' || line.startsWith('List of devices')) {
                continue;
            }
            const parts = line.split(/\s+/);
            const serial = parts[0];
            if (serial) {
                const model = parts.find(p => p.startsWith('model:'))?.split(':')[1] || 'Unknown';
                devices.push({ serial, model });
            }
        }
        return devices;
    }

    async forwardPort(deviceSerial, localPort, remotePort) {
        return new Promise((resolve, reject) => {
            const command = `"${this.adbPath}" -s ${deviceSerial} forward tcp:${localPort} tcp:${remotePort}`;
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    reject(new Error(`Failed to forward port for ${deviceSerial}: ${stderr}`));
                } else {
                    resolve(stdout);
                }
            });
        });
    }

    async reversePort(deviceSerial, remotePort, localPort) {
        return new Promise((resolve, reject) => {
            const command = `"${this.adbPath}" -s ${deviceSerial} reverse tcp:${remotePort} tcp:${localPort}`;
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    // Don't log if it's just a "Address already in use" error - that means forwarding already exists
                    if (!error.message.includes('Address already in use')) {
                        console.error(`Reverse port forwarding failed: ${error.message}`);
                    }
                    reject(error);
                } else {
                    console.log(`Reverse port forwarding established: device:${remotePort} -> computer:${localPort}`);
                    resolve(true);
                }
            });
        });
    }

    async removeReversePort(devicePort, deviceId = null) {
        return new Promise((resolve, reject) => {
            const deviceArg = deviceId ? `-s ${deviceId}` : '';
            const command = `"${this.adbPath}" ${deviceArg} reverse --remove tcp:${devicePort}`;
            
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    console.error(`Remove reverse port forwarding failed: ${error.message}`);
                    reject(error);
                } else {
                    console.log(`Reverse port forwarding removed: ${devicePort}`);
                    // Remove from our tracking set
                    this.forwardedPorts.forEach(forward => {
                        if (forward.startsWith(`${devicePort}:`)) {
                            this.forwardedPorts.delete(forward);
                        }
                    });
                    resolve(true);
                }
            });
        });
    }

    async setupSoundboardForwarding(deviceId = null, serverPort = 3001) {
        try {
            // Check if reverse forwarding already exists to avoid repeated attempts
            const forwardingKey = `${deviceId || 'default'}:8080:${serverPort}`;
            if (this.forwardedPorts.has(forwardingKey)) {
                // Verify the forwarding is still active
                const isActive = await this.verifyPortForwarding(deviceId, 8080);
                if (isActive) {
                    return true;
                }
                // Remove stale forwarding entry
                this.forwardedPorts.delete(forwardingKey);
            }
            
            // Enhanced retry logic with exponential backoff
            let retryCount = 0;
            const maxRetries = 3;
            const baseDelay = 1000; // 1 second
            
            while (retryCount < maxRetries) {
                try {
                    // Reverse forward: device port 8080 to computer server port
                    await this.reversePort(deviceId, 8080, serverPort);
                    
                    // Verify the forwarding was successful
                    const verification = await this.verifyPortForwarding(deviceId, 8080);
                    if (verification) {
                        this.forwardedPorts.add(forwardingKey);
                        console.log(`✅ Soundboard reverse port forwarding established: device:8080 → computer:${serverPort}`);
                        return true;
                    }
                    
                    throw new Error('Port forwarding verification failed');
                    
                } catch (error) {
                    retryCount++;
                    
                    if (error.message.includes('Address already in use')) {
                        // Port is already forwarded, verify and proceed
                        const verification = await this.verifyPortForwarding(deviceId, 8080);
                        if (verification) {
                            this.forwardedPorts.add(forwardingKey);
                            return true;
                        }
                    }
                    
                    if (retryCount < maxRetries) {
                        const delay = baseDelay * Math.pow(2, retryCount - 1);
                        console.log(`⚠️  ADB forwarding attempt ${retryCount} failed, retrying in ${delay}ms...`);
                        await new Promise(resolve => setTimeout(resolve, delay));
                    } else {
                        console.error('❌ Failed to setup soundboard reverse forwarding after all retries:', error.message);
                        return false;
                    }
                }
            }
            
            return false;
            
        } catch (error) {
            console.error('❌ Critical error in setupSoundboardForwarding:', error.message);
            return false;
        }
    }

    async verifyPortForwarding(deviceId = null, port) {
        try {
            const deviceArg = deviceId ? `-s ${deviceId}` : '';
            const command = `"${this.adbPath}" ${deviceArg} reverse --list`;
            
            return new Promise((resolve) => {
                exec(command, (error, stdout, stderr) => {
                    if (error) {
                        resolve(false);
                        return;
                    }
                    
                    // Check if our port forwarding is listed
                    const isForwarded = stdout.includes(`tcp:${port}`);
                    resolve(isForwarded);
                });
            });
        } catch (error) {
            console.error('Error verifying port forwarding:', error);
            return false;
        }
    }

    async performHealthCheck(deviceId = null) {
        try {
            // Check device connectivity
            const devices = await this.getDevices();
            const targetDevice = deviceId ? 
                devices.find(d => d.serial === deviceId) : 
                devices.find(d => d.connected);
            
            if (!targetDevice || !targetDevice.connected) {
                return { healthy: false, reason: 'Device not connected' };
            }
            
            // Check port forwarding
            const portForwarded = await this.verifyPortForwarding(deviceId, 8080);
            if (!portForwarded) {
                return { healthy: false, reason: 'Port forwarding not active' };
            }
            
            // Test basic communication
            const deviceInfo = await this.getDeviceInfo(deviceId);
            if (!deviceInfo) {
                return { healthy: false, reason: 'Cannot communicate with device' };
            }
            
            return { 
                healthy: true, 
                deviceInfo: deviceInfo,
                timestamp: new Date().toISOString()
            };
            
        } catch (error) {
            return { 
                healthy: false, 
                reason: `Health check failed: ${error.message}` 
            };
        }
    }

    async removeSoundboardForwarding(deviceId = null) {
        try {
            await this.removeReversePort(8080, deviceId);
            console.log('Soundboard reverse port forwarding removed');
            return true;
        } catch (error) {
            console.error('Failed to remove soundboard reverse forwarding:', error);
            return false;
        }
    }

    async getDeviceInfo(deviceId = null) {
        return new Promise((resolve, reject) => {
            const deviceArg = deviceId ? `-s ${deviceId}` : '';
            const command = `"${this.adbPath}" ${deviceArg} shell getprop ro.product.model`;
            
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    reject(error);
                } else {
                    resolve({
                        model: stdout.trim(),
                        id: deviceId || 'unknown'
                    });
                }
            });
        });
    }

    async startDeviceMonitoring(callback) {
        // Monitor device connection status
        const checkDevices = async () => {
            try {
                const devices = await this.getDevices();
                callback(devices);
            } catch (error) {
                console.error('Error checking devices:', error);
                callback([]);
            }
        };

        // Check immediately
        await checkDevices();

        // Check every 3 seconds
        this.monitorInterval = setInterval(checkDevices, 3000);
    }

    stopDeviceMonitoring() {
        if (this.monitorInterval) {
            clearInterval(this.monitorInterval);
            this.monitorInterval = null;
        }
    }

    getConnectionStatus() {
        return {
            isConnected: this.isConnected,
            devices: this.connectedDevices,
            forwardedPorts: Array.from(this.forwardedPorts)
        };
    }
}

module.exports = AdbManager; 