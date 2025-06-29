#!/usr/bin/env node

/**
 * Phase 5.2: Runtime Functionality Testing Framework
 * Comprehensive testing suite for server runtime validation
 */

import { spawn } from 'child_process';
import fetch from 'node-fetch';
import fs from 'fs-extra';
import path from 'path';
import { io } from 'socket.io-client';

class RuntimeValidationTester {
    constructor() {
        this.serverDir = process.cwd();
        this.serverProcess = null;
        this.serverPort = 3001;
        this.serverUrl = `http://localhost:${this.serverPort}`;
        this.testResults = {
            timestamp: new Date().toISOString(),
            platform: process.platform,
            architecture: process.arch,
            nodeVersion: process.version,
            tests: []
        };
        this.startTime = Date.now();
    }

    log(message, type = 'info') {
        const timestamp = new Date().toISOString();
        const prefix = {
            'info': '📋',
            'success': '✅',
            'error': '❌',
            'warning': '⚠️',
            'progress': '🔄'
        }[type] || '📋';
        
        console.log(`[${timestamp}] ${prefix} ${message}`);
    }

    async runTest(testName, testFunction) {
        this.log(`Starting test: ${testName}`, 'progress');
        const testStart = Date.now();
        
        try {
            const result = await testFunction();
            const duration = Date.now() - testStart;
            
            this.testResults.tests.push({
                name: testName,
                status: 'PASS',
                duration: duration,
                result: result
            });
            
            this.log(`Test passed: ${testName} (${duration}ms)`, 'success');
            return { success: true, result, duration };
            
        } catch (error) {
            const duration = Date.now() - testStart;
            
            this.testResults.tests.push({
                name: testName,
                status: 'FAIL',
                duration: duration,
                error: error.message
            });
            
            this.log(`Test failed: ${testName} - ${error.message}`, 'error');
            return { success: false, error: error.message, duration };
        }
    }

    async startTestServer() {
        this.log('Starting test server...', 'progress');
        
        const executablePath = path.join(this.serverDir, '../dist/audiodeck-server');
        
        if (!await fs.pathExists(executablePath)) {
            throw new Error('Server executable not found. Run build first.');
        }

        return new Promise((resolve, reject) => {
            this.serverProcess = spawn(executablePath, [], {
                stdio: ['ignore', 'pipe', 'pipe'],
                env: { 
                    ...process.env, 
                    NODE_ENV: 'test',
                    PORT: this.serverPort.toString()
                }
            });

            let serverReady = false;

            // Monitor stdout for server startup
            this.serverProcess.stdout.on('data', (data) => {
                const output = data.toString();
                if ((output.includes('Server is running') || output.includes('AudioDeck Connect Server')) && !serverReady) {
                    serverReady = true;
                    setTimeout(() => resolve(), 2000); // Give server time to fully initialize
                }
            });

            // Monitor stderr for errors
            this.serverProcess.stderr.on('data', (data) => {
                const output = data.toString();
                if (!serverReady && output.includes('Error')) {
                    reject(new Error(`Server startup error: ${output}`));
                }
            });

            // Handle process exit
            this.serverProcess.on('exit', (code) => {
                if (!serverReady) {
                    reject(new Error(`Server exited early with code ${code}`));
                }
            });

            // Timeout after 30 seconds
            setTimeout(() => {
                if (!serverReady) {
                    this.serverProcess.kill('SIGKILL');
                    reject(new Error('Server startup timeout'));
                }
            }, 30000);
        });
    }

    async stopTestServer() {
        if (this.serverProcess) {
            this.log('Stopping test server...', 'progress');
            
            return new Promise((resolve) => {
                this.serverProcess.on('exit', () => {
                    this.serverProcess = null;
                    resolve();
                });
                
                this.serverProcess.kill('SIGTERM');
                
                // Force kill after 5 seconds
                setTimeout(() => {
                    if (this.serverProcess) {
                        this.serverProcess.kill('SIGKILL');
                        this.serverProcess = null;
                        resolve();
                    }
                }, 5000);
            });
        }
    }

    async httpServerTest() {
        this.log('Testing HTTP server functionality...', 'progress');
        
        const tests = {};

        // Test health endpoint
        try {
            const healthResponse = await fetch(`${this.serverUrl}/health`, {
                timeout: 5000
            });
            
            tests.healthEndpoint = {
                status: healthResponse.status,
                ok: healthResponse.ok,
                responseTime: Date.now()
            };
            
            if (healthResponse.ok) {
                const healthData = await healthResponse.json();
                tests.healthEndpoint.data = healthData;
            }
        } catch (error) {
            tests.healthEndpoint = {
                error: error.message,
                success: false
            };
        }

        // Test CORS headers
        try {
            const corsResponse = await fetch(`${this.serverUrl}/health`, {
                method: 'OPTIONS',
                timeout: 5000
            });
            
            tests.corsSupport = {
                status: corsResponse.status,
                headers: {
                    'access-control-allow-origin': corsResponse.headers.get('access-control-allow-origin'),
                    'access-control-allow-methods': corsResponse.headers.get('access-control-allow-methods')
                }
            };
        } catch (error) {
            tests.corsSupport = {
                error: error.message,
                success: false
            };
        }

        // Test API endpoints
        const apiEndpoints = ['/api/audio/list', '/api/devices', '/api/status'];
        
        for (const endpoint of apiEndpoints) {
            try {
                const response = await fetch(`${this.serverUrl}${endpoint}`, {
                    timeout: 5000
                });
                
                tests[`endpoint_${endpoint.replace(/\//g, '_')}`] = {
                    status: response.status,
                    ok: response.ok,
                    contentType: response.headers.get('content-type')
                };
                
                if (response.ok && response.headers.get('content-type')?.includes('application/json')) {
                    const data = await response.json();
                    tests[`endpoint_${endpoint.replace(/\//g, '_')}`].hasData = Object.keys(data).length > 0;
                }
            } catch (error) {
                tests[`endpoint_${endpoint.replace(/\//g, '_')}`] = {
                    error: error.message,
                    success: false
                };
            }
        }

        return tests;
    }

    async websocketTest() {
        this.log('Testing WebSocket functionality...', 'progress');
        
        return new Promise((resolve, reject) => {
            const socket = io(this.serverUrl, {
                timeout: 10000,
                transports: ['websocket']
            });

            const tests = {};
            let connected = false;

            socket.on('connect', () => {
                connected = true;
                tests.connection = { success: true, socketId: socket.id };
                
                // Test echo functionality
                socket.emit('test-echo', { message: 'test-message', timestamp: Date.now() });
            });

            socket.on('test-echo-response', (data) => {
                tests.echo = { 
                    success: true, 
                    received: data,
                    responseTime: Date.now() - data.timestamp
                };
                
                socket.disconnect();
                resolve(tests);
            });

            socket.on('connect_error', (error) => {
                tests.connection = { 
                    success: false, 
                    error: error.message 
                };
                resolve(tests);
            });

            socket.on('disconnect', () => {
                if (connected && !tests.echo) {
                    tests.echo = { 
                        success: false, 
                        error: 'Disconnected before echo response' 
                    };
                    resolve(tests);
                }
            });

            // Timeout after 10 seconds
            setTimeout(() => {
                if (!connected) {
                    tests.connection = { 
                        success: false, 
                        error: 'Connection timeout' 
                    };
                    socket.disconnect();
                    resolve(tests);
                }
            }, 10000);
        });
    }

    async audioProcessingTest() {
        this.log('Testing audio processing functionality...', 'progress');
        
        const tests = {};

        // Test audio list endpoint
        try {
            const audioListResponse = await fetch(`${this.serverUrl}/api/audio/list`, {
                timeout: 5000
            });
            
            if (audioListResponse.ok) {
                const audioList = await audioListResponse.json();
                tests.audioList = {
                    success: true,
                    count: audioList.length || 0,
                    files: audioList
                };
            } else {
                tests.audioList = {
                    success: false,
                    status: audioListResponse.status
                };
            }
        } catch (error) {
            tests.audioList = {
                success: false,
                error: error.message
            };
        }

        // Test audio player initialization
        try {
            const playerStatusResponse = await fetch(`${this.serverUrl}/api/audio/player/status`, {
                timeout: 5000
            });
            
            if (playerStatusResponse.ok) {
                const playerStatus = await playerStatusResponse.json();
                tests.audioPlayer = {
                    success: true,
                    status: playerStatus
                };
            } else {
                tests.audioPlayer = {
                    success: false,
                    status: playerStatusResponse.status
                };
            }
        } catch (error) {
            tests.audioPlayer = {
                success: false,
                error: error.message
            };
        }

        // Test audio format support
        try {
            const formatsResponse = await fetch(`${this.serverUrl}/api/audio/formats`, {
                timeout: 5000
            });
            
            if (formatsResponse.ok) {
                const formats = await formatsResponse.json();
                tests.audioFormats = {
                    success: true,
                    supportedFormats: formats
                };
            } else {
                tests.audioFormats = {
                    success: false,
                    status: formatsResponse.status
                };
            }
        } catch (error) {
            tests.audioFormats = {
                success: false,
                error: error.message
            };
        }

        return tests;
    }

    async deviceIntegrationTest() {
        this.log('Testing device integration functionality...', 'progress');
        
        const tests = {};

        // Test ADB manager status
        try {
            const adbStatusResponse = await fetch(`${this.serverUrl}/api/devices/adb/status`, {
                timeout: 5000
            });
            
            if (adbStatusResponse.ok) {
                const adbStatus = await adbStatusResponse.json();
                tests.adbManager = {
                    success: true,
                    status: adbStatus
                };
            } else {
                tests.adbManager = {
                    success: false,
                    status: adbStatusResponse.status
                };
            }
        } catch (error) {
            tests.adbManager = {
                success: false,
                error: error.message
            };
        }

        // Test USB device detection
        try {
            const usbDevicesResponse = await fetch(`${this.serverUrl}/api/devices/usb`, {
                timeout: 10000 // USB detection can take longer
            });
            
            if (usbDevicesResponse.ok) {
                const usbDevices = await usbDevicesResponse.json();
                tests.usbDetection = {
                    success: true,
                    deviceCount: usbDevices.length || 0,
                    devices: usbDevices
                };
            } else {
                tests.usbDetection = {
                    success: false,
                    status: usbDevicesResponse.status
                };
            }
        } catch (error) {
            tests.usbDetection = {
                success: false,
                error: error.message
            };
        }

        // Test network discovery
        try {
            const discoveryResponse = await fetch(`${this.serverUrl}/api/network/discovery`, {
                timeout: 5000
            });
            
            if (discoveryResponse.ok) {
                const discoveryStatus = await discoveryResponse.json();
                tests.networkDiscovery = {
                    success: true,
                    status: discoveryStatus
                };
            } else {
                tests.networkDiscovery = {
                    success: false,
                    status: discoveryResponse.status
                };
            }
        } catch (error) {
            tests.networkDiscovery = {
                success: false,
                error: error.message
            };
        }

        return tests;
    }

    async performanceMetricsTest() {
        this.log('Measuring performance metrics...', 'progress');
        
        const metrics = {};

        // Memory usage test
        try {
            const memoryResponse = await fetch(`${this.serverUrl}/api/system/memory`, {
                timeout: 5000
            });
            
            if (memoryResponse.ok) {
                const memoryData = await memoryResponse.json();
                metrics.memory = {
                    success: true,
                    usage: memoryData,
                    withinLimits: memoryData.rss < 200 * 1024 * 1024 // 200MB limit
                };
            } else {
                metrics.memory = {
                    success: false,
                    status: memoryResponse.status
                };
            }
        } catch (error) {
            metrics.memory = {
                success: false,
                error: error.message
            };
        }

        // Response time test
        const responseTimes = [];
        for (let i = 0; i < 10; i++) {
            try {
                const start = Date.now();
                const response = await fetch(`${this.serverUrl}/health`, {
                    timeout: 5000
                });
                const responseTime = Date.now() - start;
                
                if (response.ok) {
                    responseTimes.push(responseTime);
                }
            } catch (error) {
                // Ignore individual failures
            }
        }

        if (responseTimes.length > 0) {
            const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
            metrics.responseTime = {
                success: true,
                average: Math.round(avgResponseTime),
                samples: responseTimes.length,
                withinTarget: avgResponseTime < 50 // 50ms target
            };
        } else {
            metrics.responseTime = {
                success: false,
                error: 'No successful response time measurements'
            };
        }

        return metrics;
    }

    async nativeModuleIntegrationTest() {
        this.log('Testing native module integration...', 'progress');
        
        const tests = {};

        // Test voicemeeter integration (Windows only)
        if (process.platform === 'win32') {
            try {
                const voicemeeterResponse = await fetch(`${this.serverUrl}/api/audio/voicemeeter/status`, {
                    timeout: 5000
                });
                
                tests.voicemeeter = {
                    platform: 'windows',
                    available: voicemeeterResponse.ok,
                    status: voicemeeterResponse.status
                };
                
                if (voicemeeterResponse.ok) {
                    const voicemeeterData = await voicemeeterResponse.json();
                    tests.voicemeeter.data = voicemeeterData;
                }
            } catch (error) {
                tests.voicemeeter = {
                    platform: 'windows',
                    available: false,
                    error: error.message
                };
            }
        } else {
            tests.voicemeeter = {
                platform: process.platform,
                available: false,
                note: 'Voicemeeter only available on Windows'
            };
        }

        // Test mDNS/Bonjour service
        try {
            const mdnsResponse = await fetch(`${this.serverUrl}/api/network/mdns/status`, {
                timeout: 5000
            });
            
            tests.mdns = {
                available: mdnsResponse.ok,
                status: mdnsResponse.status
            };
            
            if (mdnsResponse.ok) {
                const mdnsData = await mdnsResponse.json();
                tests.mdns.data = mdnsData;
            }
        } catch (error) {
            tests.mdns = {
                available: false,
                error: error.message
            };
        }

        return tests;
    }

    async generateReport() {
        const totalDuration = Date.now() - this.startTime;
        const passedTests = this.testResults.tests.filter(t => t.status === 'PASS').length;
        const failedTests = this.testResults.tests.filter(t => t.status === 'FAIL').length;
        
        const report = {
            ...this.testResults,
            summary: {
                totalDuration,
                totalTests: this.testResults.tests.length,
                passedTests,
                failedTests,
                successRate: Math.round((passedTests / this.testResults.tests.length) * 100)
            }
        };

        // Save report to file
        const reportPath = path.join(this.serverDir, 'runtime-validation-report.json');
        await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
        
        this.log(`Runtime validation report saved to: ${reportPath}`, 'success');
        
        return report;
    }

    async runAllTests() {
        this.log('Starting comprehensive runtime validation testing...', 'progress');
        
        try {
            // Start the test server
            await this.startTestServer();
            
            // Run all test suites
            await this.runTest('HTTP Server Test', () => this.httpServerTest());
            await this.runTest('WebSocket Test', () => this.websocketTest());
            await this.runTest('Audio Processing Test', () => this.audioProcessingTest());
            await this.runTest('Device Integration Test', () => this.deviceIntegrationTest());
            await this.runTest('Performance Metrics Test', () => this.performanceMetricsTest());
            await this.runTest('Native Module Integration Test', () => this.nativeModuleIntegrationTest());
            
        } finally {
            // Always stop the server
            await this.stopTestServer();
        }
        
        // Generate final report
        const report = await this.generateReport();
        
        this.log(`Runtime validation completed: ${report.summary.passedTests}/${report.summary.totalTests} tests passed`, 
                 report.summary.successRate === 100 ? 'success' : 'warning');
        
        return report;
    }
}

// Run tests if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
    const tester = new RuntimeValidationTester();
    
    tester.runAllTests()
        .then(report => {
            console.log('\n📊 Final Report Summary:');
            console.log(`   Total Tests: ${report.summary.totalTests}`);
            console.log(`   Passed: ${report.summary.passedTests}`);
            console.log(`   Failed: ${report.summary.failedTests}`);
            console.log(`   Success Rate: ${report.summary.successRate}%`);
            console.log(`   Total Duration: ${Math.round(report.summary.totalDuration / 1000)}s`);
            
            process.exit(report.summary.failedTests > 0 ? 1 : 0);
        })
        .catch(error => {
            console.error('❌ Runtime validation failed:', error);
            process.exit(1);
        });
}

export default RuntimeValidationTester;