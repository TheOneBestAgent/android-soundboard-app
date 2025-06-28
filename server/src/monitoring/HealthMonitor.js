import os from 'os';
import EventEmitter from 'events';

const MONITORING_INTERVAL = 15000; // 15 seconds

/**
 * HealthMonitor - Phase 2
 * 
 * Provides real-time monitoring of server health, including CPU usage,
 * memory consumption, and event loop latency. It now supports dynamic
 * health scoring for a more nuanced view of system performance.
 */
export class HealthMonitor extends EventEmitter {
    constructor() {
        super();
        this.monitoringInterval = null;
        this.eventLoopLag = 0;
        this.lastCheck = null;
    }

    startMonitoring() {
        this.lastCheck = Date.now();
        this.monitoringInterval = setInterval(() => {
            const now = Date.now();
            this.eventLoopLag = now - this.lastCheck - MONITORING_INTERVAL;
            this.lastCheck = now;
            this.emit('health-update', this.getStatus());
        }, MONITORING_INTERVAL);
    }

    stopMonitoring() {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
        }
    }

    getMemoryUsage() {
        const memory = process.memoryUsage();
        return {
            rss: (memory.rss / 1024 / 1024).toFixed(2), // Resident Set Size in MB
            heapTotal: (memory.heapTotal / 1024 / 1024).toFixed(2), // Total heap size in MB
            heapUsed: (memory.heapUsed / 1024 / 1024).toFixed(2), // Used heap size in MB
            external: (memory.external / 1024 / 1024).toFixed(2), // External memory usage in MB
        };
    }

    getCpuUsage() {
        const cpus = os.cpus();
        const total = cpus.reduce((acc, cpu) => {
            for (const type in cpu.times) {
                acc[type] = (acc[type] || 0) + cpu.times[type];
            }
            return acc;
        }, {});

        const idle = total.idle;
        const all = Object.values(total).reduce((a, b) => a + b, 0);

        return {
            total: all,
            idle: idle,
            usage: ((all - idle) / all * 100).toFixed(2)
        };
    }

    getStatus() {
        return {
            memory: this.getMemoryUsage(),
            cpu: this.getCpuUsage(),
            uptime: process.uptime().toFixed(2),
            eventLoopLag: this.eventLoopLag.toFixed(2),
            timestamp: new Date().toISOString()
        };
    }
} 