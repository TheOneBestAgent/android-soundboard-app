// AudioDeck Connect - Health Check Endpoint
// Enterprise-grade health monitoring and diagnostics

import express from 'express';
import os from 'os';

const router = express.Router();

// Health check endpoint with detailed system metrics
router.get('/', (req, res) => {
    const healthMonitor = req.app.get('healthMonitor');
    const status = healthMonitor ? healthMonitor.getStatus() : getBasicHealth();
    
    res.json({
        name: 'AudioDeck Connect',
        version: '8.0.0',
        status: status.status,
        timestamp: new Date(),
        uptime: process.uptime(),
        metrics: status.metrics,
        environment: {
            node: process.version,
            platform: process.platform,
            arch: process.arch,
            memory: {
                total: os.totalmem(),
                free: os.freemem(),
                used: os.totalmem() - os.freemem()
            },
            cpu: os.cpus().length
        }
    });
});

// Detailed metrics endpoint
router.get('/metrics', (req, res) => {
    const healthMonitor = req.app.get('healthMonitor');
    
    if (!healthMonitor) {
        return res.status(503).json({
            error: 'Health monitor not available',
            timestamp: new Date()
        });
    }
    
    res.json({
        name: 'AudioDeck Connect',
        version: '8.0.0',
        timestamp: new Date(),
        metrics: healthMonitor.getMetrics()
    });
});

// Service status endpoint
router.get('/services', (req, res) => {
    const healthMonitor = req.app.get('healthMonitor');
    
    if (!healthMonitor) {
        return res.status(503).json({
            error: 'Health monitor not available',
            timestamp: new Date()
        });
    }
    
    const metrics = healthMonitor.getMetrics();
    
    res.json({
        name: 'AudioDeck Connect',
        version: '8.0.0',
        timestamp: new Date(),
        services: metrics.services
    });
});

// Error history endpoint
router.get('/errors', (req, res) => {
    const healthMonitor = req.app.get('healthMonitor');
    
    if (!healthMonitor) {
        return res.status(503).json({
            error: 'Health monitor not available',
            timestamp: new Date()
        });
    }
    
    const metrics = healthMonitor.getMetrics();
    
    res.json({
        name: 'AudioDeck Connect',
        version: '8.0.0',
        timestamp: new Date(),
        errors: metrics.application.errors
    });
});

function getBasicHealth() {
    return {
        status: 'limited',
        metrics: {
            system: {
                cpu: 0,
                memory: {
                    total: os.totalmem(),
                    free: os.freemem(),
                    used: os.totalmem() - os.freemem()
                },
                uptime: os.uptime(),
                platform: process.platform,
                arch: process.arch,
                version: process.version
            },
            application: {
                status: 'starting',
                uptime: process.uptime(),
                version: '8.0.0'
            },
            services: {
                network: 'unknown',
                adb: 'unknown',
                audio: 'unknown',
                usb: 'unknown'
            }
        }
    };
}

export default router; 