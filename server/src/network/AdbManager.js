const { spawn, exec } = require('child_process');
const path = require('path');
const os = require('os');

class AdbManager {
    constructor() {
        this.isConnected = false;
        this.connectedDevices = [];
        this.forwardedPorts = new Set();
        this.adbPath = this.findAdbPath();
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
            exec(`${this.adbPath} version`, (error, stdout, stderr) => {
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

    async listDevices() {
        return new Promise((resolve, reject) => {
            exec(`${this.adbPath} devices`, (error, stdout, stderr) => {
                if (error) {
                    reject(error);
                    return;
                }

                const lines = stdout.split('\n');
                const devices = [];
                
                for (let i = 1; i < lines.length; i++) {
                    const line = lines[i].trim();
                    if (line && !line.startsWith('*')) {
                        const [deviceId, status] = line.split('\t');
                        if (deviceId && status) {
                            devices.push({
                                id: deviceId,
                                status: status,
                                connected: status === 'device'
                            });
                        }
                    }
                }

                this.connectedDevices = devices;
                this.isConnected = devices.some(device => device.connected);
                resolve(devices);
            });
        });
    }

    async reversePort(devicePort, computerPort, deviceId = null) {
        return new Promise((resolve, reject) => {
            const deviceArg = deviceId ? `-s ${deviceId}` : '';
            const command = `${this.adbPath} ${deviceArg} reverse tcp:${devicePort} tcp:${computerPort}`;
            
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    // Don't log if it's just a "Address already in use" error - that means forwarding already exists
                    if (!error.message.includes('Address already in use')) {
                        console.error(`Reverse port forwarding failed: ${error.message}`);
                    }
                    reject(error);
                } else {
                    console.log(`Reverse port forwarding established: device:${devicePort} -> computer:${computerPort}`);
                    resolve(true);
                }
            });
        });
    }

    async removeReversePort(devicePort, deviceId = null) {
        return new Promise((resolve, reject) => {
            const deviceArg = deviceId ? `-s ${deviceId}` : '';
            const command = `${this.adbPath} ${deviceArg} reverse --remove tcp:${devicePort}`;
            
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
                // Already forwarded - return success without logging spam
                return true;
            }
            
            // Reverse forward: device port 8080 to computer server port (3001 by default)
            // This means: Android app connects to localhost:8080 → gets forwarded to computer server on port 3001
            await this.reversePort(8080, serverPort, deviceId);
            
            // Track the device-specific forwarding with full key
            this.forwardedPorts.add(forwardingKey);
            
            console.log(`✅ Soundboard reverse port forwarding established: device:8080 → computer:${serverPort}`);
            return true;
        } catch (error) {
            // Don't log "Address already in use" as an error - it means forwarding exists
            if (error.message.includes('Address already in use')) {
                const forwardingKey = `${deviceId || 'default'}:8080:${serverPort}`;
                this.forwardedPorts.add(forwardingKey);
                return true; // Still successful
            }
            console.error('❌ Failed to setup soundboard reverse forwarding:', error.message);
            return false;
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
            const command = `${this.adbPath} ${deviceArg} shell getprop ro.product.model`;
            
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
                const devices = await this.listDevices();
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