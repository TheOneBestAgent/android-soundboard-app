// AudioDeck Connect - ADB Device Manager
// Handles Android device communication and management

import { Adb, AdbServerClient } from '@yume-chan/adb';
import { AdbServerNodeTcpConnector } from '@yume-chan/adb-server-node-tcp';
import EventEmitter from 'events';

export class AdbManager extends EventEmitter {
    constructor() {
        super();
        this.devices = new Map();
        this.client = undefined;
        this.isInitialized = false;
        this.tracking = false;
        
        console.log('🤖 ADB Manager initializing...');
    }
    
    async initialize() {
        if (this.isInitialized) {
            console.log('🤖 ADB Manager already initialized');
            return;
        }

        try {
            // Establish connection to the ADB server
            const connector = AdbServerNodeTcpConnector.create();
            this.client = await connector.connect();
            this.isInitialized = true;
            console.log('✅ ADB Manager initialized successfully and connected to ADB server.');

            // Start tracking devices
            this.startTracking();

        } catch (error) {
            console.error('❌ Failed to initialize ADB Manager or connect to ADB server:', error);
            this.isInitialized = false;
            // Optionally, retry connection after a delay
            setTimeout(() => this.initialize(), 10000);
        }
    }
    
    async startTracking() {
        if (this.tracking || !this.client) return;
        this.tracking = true;
        console.log('👀 Starting to track ADB devices...');

        (async () => {
            try {
                const tracker = await this.client.trackDevices();
                for await (const device of tracker) {
                    if (device.state === 'device') {
                        if (!this.devices.has(device.serial)) {
                            // New device connected
                            const adb = await device.connect();
                            this.devices.set(device.serial, { adb, info: device });
                            this.emit('deviceConnected', device);
                            console.log(`✅ Device connected: ${device.serial}`);
                        }
                    } else if (device.state === 'offline' || device.state === 'unauthorized') {
                        if (this.devices.has(device.serial)) {
                            // Device disconnected or went offline
                            const disconnectedDevice = this.devices.get(device.serial);
                            this.devices.delete(device.serial);
                            this.emit('deviceDisconnected', disconnectedDevice.info);
                            console.log(`❌ Device disconnected: ${device.serial}`);
                        }
                    }
                }
            } catch (e) {
                console.error('Device tracking failed:', e);
                this.tracking = false;
                // Optional: try to restart tracking
                setTimeout(() => this.startTracking(), 5000);
            }
        })();
    }
    
    getConnectedDevices() {
        return Array.from(this.devices.values()).map(d => d.info);
    }
    
    async executeCommand(serial, command) {
        const device = this.devices.get(serial);
        if (!device) {
            throw new Error(`Device ${serial} not found`);
        }
        
        try {
            const shell = await device.adb.shell(command);
            const output = await shell.readAll();
            return new TextDecoder().decode(output);
        } catch (error) {
            console.error(`❌ Failed to execute command "${command}":`, error);
            throw error;
        }
    }
    
    async shutdown() {
        console.log('🤖 Shutting down ADB Manager...');
        if (this.client) {
            await this.client.killServer();
        }
        this.tracking = false;
        this.isInitialized = false;
        this.devices.clear();
        console.log('✅ ADB Manager shutdown complete');
    }

    getStatus() {
        return {
            isInitialized: this.isInitialized,
            isTracking: this.tracking,
            connectedDevicesCount: this.devices.size,
            devices: this.getConnectedDevices(),
        };
    }
} 