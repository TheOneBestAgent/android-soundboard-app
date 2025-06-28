import { EventEmitter } from 'events';
import { spawn, exec } from 'child_process';
import fs from 'fs/promises';
import path from 'path';
import os from 'os';
import { AdbManager } from './AdbManager.js';

/**
 * USBAutoDetectionService - Phase 2: Discovery & Automation
 * 
 * Automatically detects USB-connected Android devices and sets up
 * ADB port forwarding for seamless connection establishment.
 * 
 * Cross-platform compatible with Windows, macOS, and Linux.
 */
export class USBAutoDetectionService extends EventEmitter {
    constructor() {
        super();
        this.adbManager = new AdbManager(process.env.ADB_PATH);
        this.monitoringInterval = null;
        this.connectedDevices = new Set();
        console.log("🔌 USBAutoDetectionService initialized");
    }

    startMonitoring() {
        console.log('👀 Starting USB device monitoring...');
        this.stopMonitoring(); // Ensure no multiple intervals are running
        this.monitoringInterval = setInterval(() => this.scanDevices(), 5000);
        this.scanDevices(); // Initial scan
    }

    stopMonitoring() {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
            console.log('🛑 Stopped USB device monitoring.');
        }
    }

    async scanDevices() {
        try {
            const devices = await this.adbManager.getDevices();
            const currentDeviceSerials = new Set(devices.map(d => d.serial));

            // Check for new devices
            for (const device of devices) {
                if (!this.connectedDevices.has(device.serial)) {
                    console.log(`🔌 USB device connected: ${device.serial} (Model: ${device.model})`);
                    this.connectedDevices.add(device.serial);
                    this.emit('usb-device-connected', device);
                }
            }

            // Check for disconnected devices
            for (const serial of this.connectedDevices) {
                if (!currentDeviceSerials.has(serial)) {
                    console.log(`🔌 USB device disconnected: ${serial}`);
                    this.connectedDevices.delete(serial);
                    this.emit('usb-device-disconnected', { serial });
                }
            }
        } catch (error) {
            // Don't log here as it's very noisy when ADB isn't ready.
            // The adbManager will log the actual command error.
        }
    }
}

// Export handled by class declaration 