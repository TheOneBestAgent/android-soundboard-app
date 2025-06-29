#!/usr/bin/env node

/**
 * Phase 5.1: Build Validation Testing Framework
 * Comprehensive testing suite for build system validation
 */

import { execSync, spawn } from 'child_process';
import fs from 'fs-extra';
import path from 'path';
import os from 'os';

class BuildValidationTester {
    constructor() {
        this.serverDir = process.cwd();
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

    async cleanBuildTest() {
        this.log('Performing clean build test...', 'progress');
        
        // Clean all build artifacts
        try {
            await fs.remove(path.join(this.serverDir, 'node_modules'));
            await fs.remove(path.join(this.serverDir, 'dist'));
            await fs.remove(path.join(this.serverDir, 'build'));
            await fs.remove(path.join(this.serverDir, 'package-lock.json'));
        } catch (error) {
            // Ignore errors for non-existent directories
        }

        // Fresh npm install
        const installStart = Date.now();
        execSync('npm install', { 
            cwd: this.serverDir, 
            stdio: 'inherit',
            timeout: 300000 // 5 minutes timeout
        });
        const installTime = Date.now() - installStart;

        // Build test
        const buildStart = Date.now();
        execSync('npm run build', { 
            cwd: this.serverDir, 
            stdio: 'inherit',
            timeout: 120000 // 2 minutes timeout
        });
        const buildTime = Date.now() - buildStart;

        // Verify build artifacts (executable is in parent dist directory)
        const distExists = await fs.pathExists(path.join(this.serverDir, '../dist'));
        const executableExists = await fs.pathExists(path.join(this.serverDir, '../dist/audiodeck-server'));
        
        if (!distExists || !executableExists) {
            throw new Error('Build artifacts not created successfully');
        }

        // Get executable size
        const executableStats = await fs.stat(path.join(this.serverDir, '../dist/audiodeck-server'));
        const executableSizeMB = Math.round(executableStats.size / (1024 * 1024));

        return {
            installTime,
            buildTime,
            executableSizeMB,
            success: true
        };
    }

    async nativeModuleValidationTest() {
        this.log('Testing native module integration...', 'progress');
        
        const nativeModules = [
            '@yume-chan/adb',
            '@yume-chan/adb-server-node-tcp',
            'socket.io',
            'express',
            'fs-extra'
        ];

        const optionalModules = [
            'voicemeeter-connector'
        ];

        const results = {};

        // Test required modules
        for (const moduleName of nativeModules) {
            try {
                const modulePath = path.join(this.serverDir, 'node_modules', moduleName);
                const moduleExists = await fs.pathExists(modulePath);
                
                if (!moduleExists) {
                    throw new Error(`Module ${moduleName} not found`);
                }

                // Try to require the module
                const module = await import(moduleName);
                results[moduleName] = { 
                    status: 'success', 
                    path: modulePath,
                    hasExports: Object.keys(module).length > 0
                };
                
            } catch (error) {
                results[moduleName] = { 
                    status: 'error', 
                    error: error.message 
                };
            }
        }

        // Test optional modules
        for (const moduleName of optionalModules) {
            try {
                const modulePath = path.join(this.serverDir, 'node_modules', moduleName);
                const moduleExists = await fs.pathExists(modulePath);
                
                results[moduleName] = { 
                    status: moduleExists ? 'available' : 'optional-missing', 
                    path: moduleExists ? modulePath : null
                };
                
            } catch (error) {
                results[moduleName] = { 
                    status: 'optional-error', 
                    error: error.message 
                };
            }
        }

        return results;
    }

    async bundleAnalysisTest() {
        this.log('Analyzing bundle composition...', 'progress');
        
        const distDir = path.join(this.serverDir, 'dist');
        const buildDir = path.join(this.serverDir, 'build');
        
        const analysis = {
            executable: {},
            bundle: {},
            nativeModules: {}
        };

        // Analyze executable
        if (await fs.pathExists(path.join(distDir, 'audiodeck-server'))) {
            const execStats = await fs.stat(path.join(distDir, 'audiodeck-server'));
            analysis.executable = {
                size: execStats.size,
                sizeMB: Math.round(execStats.size / (1024 * 1024)),
                created: execStats.birthtime
            };
        }

        // Analyze bundle
        if (await fs.pathExists(path.join(buildDir, 'server-bundle.js'))) {
            const bundleStats = await fs.stat(path.join(buildDir, 'server-bundle.js'));
            analysis.bundle = {
                size: bundleStats.size,
                sizeMB: Math.round(bundleStats.size / (1024 * 1024)),
                created: bundleStats.birthtime
            };
        }

        // Analyze native modules directory
        const nativeModulesDir = path.join(buildDir, 'node_modules');
        if (await fs.pathExists(nativeModulesDir)) {
            const nativeModulesList = await fs.readdir(nativeModulesDir);
            analysis.nativeModules = {
                count: nativeModulesList.length,
                modules: nativeModulesList
            };
        }

        return analysis;
    }

    async performanceBaselineTest() {
        this.log('Establishing performance baseline...', 'progress');
        
        const executablePath = path.join(this.serverDir, '../dist/audiodeck-server');
        
        if (!await fs.pathExists(executablePath)) {
            throw new Error('Executable not found for performance testing');
        }

        // Test startup time
        const startupStart = Date.now();
        
        return new Promise((resolve, reject) => {
            const serverProcess = spawn(executablePath, [], {
                stdio: ['ignore', 'pipe', 'pipe'],
                env: { ...process.env, NODE_ENV: 'test' }
            });

            let startupComplete = false;
            let serverStarted = false;

            // Monitor stdout for startup completion
            serverProcess.stdout.on('data', (data) => {
                const output = data.toString();
                if (output.includes('Server is running') || output.includes('AudioDeck Connect Server')) {
                    if (!startupComplete) {
                        startupComplete = true;
                        const startupTime = Date.now() - startupStart;
                        
                        // Kill the server process
                        serverProcess.kill('SIGTERM');
                        
                        resolve({
                            startupTime,
                            success: true
                        });
                    }
                }
            });

            // Handle process errors
            serverProcess.on('error', (error) => {
                if (!startupComplete) {
                    reject(new Error(`Server startup failed: ${error.message}`));
                }
            });

            // Handle process exit
            serverProcess.on('exit', (code) => {
                if (!startupComplete) {
                    if (code === 0) {
                        // Server started and shut down cleanly
                        const startupTime = Date.now() - startupStart;
                        resolve({
                            startupTime,
                            success: true,
                            note: 'Server started and shut down cleanly'
                        });
                    } else {
                        reject(new Error(`Server exited with code ${code}`));
                    }
                }
            });

            // Timeout after 30 seconds
            setTimeout(() => {
                if (!startupComplete) {
                    serverProcess.kill('SIGKILL');
                    reject(new Error('Server startup timeout (30 seconds)'));
                }
            }, 30000);
        });
    }

    async errorRecoveryTest() {
        this.log('Testing error recovery scenarios...', 'progress');
        
        const results = {};

        // Test 1: Build with corrupted package.json
        try {
            const packageJsonPath = path.join(this.serverDir, 'package.json');
            const originalPackageJson = await fs.readFile(packageJsonPath, 'utf8');
            
            // Temporarily corrupt package.json
            await fs.writeFile(packageJsonPath, '{ invalid json }');
            
            try {
                execSync('npm run build', { 
                    cwd: this.serverDir, 
                    stdio: 'pipe',
                    timeout: 30000
                });
                results.corruptedPackageJson = { status: 'unexpected-success' };
            } catch (buildError) {
                results.corruptedPackageJson = { 
                    status: 'expected-failure',
                    error: buildError.message
                };
            }
            
            // Restore package.json
            await fs.writeFile(packageJsonPath, originalPackageJson);
            
        } catch (error) {
            results.corruptedPackageJson = { 
                status: 'test-error', 
                error: error.message 
            };
        }

        // Test 2: Build with missing dependencies
        try {
            const nodeModulesPath = path.join(this.serverDir, 'node_modules');
            const nodeModulesExists = await fs.pathExists(nodeModulesPath);
            
            if (nodeModulesExists) {
                // Temporarily rename node_modules
                await fs.move(nodeModulesPath, `${nodeModulesPath}.backup`);
                
                try {
                    execSync('npm run build', { 
                        cwd: this.serverDir, 
                        stdio: 'pipe',
                        timeout: 30000
                    });
                    results.missingDependencies = { status: 'unexpected-success' };
                } catch (buildError) {
                    results.missingDependencies = { 
                        status: 'expected-failure',
                        error: buildError.message
                    };
                }
                
                // Restore node_modules
                await fs.move(`${nodeModulesPath}.backup`, nodeModulesPath);
            }
            
        } catch (error) {
            results.missingDependencies = { 
                status: 'test-error', 
                error: error.message 
            };
        }

        return results;
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
        const reportPath = path.join(this.serverDir, 'build-validation-report.json');
        await fs.writeFile(reportPath, JSON.stringify(report, null, 2));
        
        this.log(`Build validation report saved to: ${reportPath}`, 'success');
        
        return report;
    }

    async runAllTests() {
        this.log('Starting comprehensive build validation testing...', 'progress');
        
        // Run all test suites
        await this.runTest('Clean Build Test', () => this.cleanBuildTest());
        await this.runTest('Native Module Validation', () => this.nativeModuleValidationTest());
        await this.runTest('Bundle Analysis', () => this.bundleAnalysisTest());
        await this.runTest('Performance Baseline', () => this.performanceBaselineTest());
        await this.runTest('Error Recovery', () => this.errorRecoveryTest());
        
        // Generate final report
        const report = await this.generateReport();
        
        this.log(`Build validation completed: ${report.summary.passedTests}/${report.summary.totalTests} tests passed`, 
                 report.summary.successRate === 100 ? 'success' : 'warning');
        
        return report;
    }
}

// Run tests if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
    const tester = new BuildValidationTester();
    
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
            console.error('❌ Build validation failed:', error);
            process.exit(1);
        });
}

export default BuildValidationTester;