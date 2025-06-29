#!/usr/bin/env node

const fs = require('fs-extra');
const path = require('path');
const { execSync } = require('child_process');
const { build } = require('esbuild');

/**
 * PHASE 3.1: ESBuild Configuration Setup
 * Comprehensive ESBuild-based build system for AudioDeck Connect Server
 * Replaces Babel with modern ESBuild bundling and PKG integration
 */
class ESBuildServerBuilder {
    constructor() {
        this.serverRoot = __dirname;
        this.projectRoot = path.join(this.serverRoot, '..');
        this.srcDir = path.join(this.serverRoot, 'src');
        this.buildDir = path.join(this.serverRoot, 'build');
        this.distDir = path.join(this.projectRoot, 'dist');
        
        console.log('🚀 ESBUILD SERVER BUILDER v9.2');
        console.log('⚡ Phase 3.1: ESBuild Configuration Setup');
        console.log('==========================================');
        
        this.buildServer();
    }
    
    async buildServer() {
        try {
            console.log('Phase 3.1.1: Environment Preparation');
            await this.prepareEnvironment();
            
            console.log('Phase 3.1.2: ESBuild Bundling');
            await this.bundleWithESBuild();
            
            console.log('Phase 3.1.3: Native Module Handling');
            await this.handleNativeModules();
            
            console.log('Phase 3.2.1: PKG Executable Generation');
            await this.generateExecutable();
            
            console.log('Phase 3.3.1: Build Validation');
            await this.validateBuild();
            
            console.log('🎉 SUCCESS: ESBuild-based server executable created!');
            console.log(`📦 Executable location: ${path.join(this.distDir, 'audiodeck-server')}`);
            
        } catch (error) {
            console.error('❌ ESBuild process failed:', error.message);
            console.error('Stack trace:', error.stack);
            process.exit(1);
        }
    }
    
    async prepareEnvironment() {
        console.log('🧹 Cleaning build environment...');
        
        // Clean directories
        if (fs.existsSync(this.buildDir)) {
            await fs.remove(this.buildDir);
        }
        if (fs.existsSync(this.distDir)) {
            await fs.remove(this.distDir);
        }
        
        // Create directories
        await fs.ensureDir(this.buildDir);
        await fs.ensureDir(this.distDir);
        
        console.log('✅ Environment prepared');
    }
    
    async bundleWithESBuild() {
        console.log('⚡ Bundling with ESBuild...');
        
        const entryPoint = path.join(this.srcDir, 'server.js');
        const outputFile = path.join(this.buildDir, 'server-bundle.js');
        
        // Phase 3.1.1: Core ESBuild Configuration
        const buildOptions = {
            entryPoints: [entryPoint],
            bundle: true,
            platform: 'node',
            target: 'node18',
            format: 'cjs', // CommonJS for PKG compatibility
            outfile: outputFile,
            external: [
                // Native modules marked as external
                'voicemeeter-connector',
                'koffi'
                // Note: @yume-chan modules need to be bundled due to ES module format
            ],
            loader: {
                '.node': 'copy' // Native module handling
            },
            sourcemap: true, // For debugging
            minify: false, // Keep readable for debugging
            treeShaking: true, // Optimize bundle size
            metafile: true, // For bundle analysis
            logLevel: 'info'
        };
        
        try {
            const result = await build(buildOptions);
            
            // Save metafile for analysis
            const metafilePath = path.join(this.buildDir, 'meta.json');
            await fs.writeFile(metafilePath, JSON.stringify(result.metafile, null, 2));
            
            console.log('✅ ESBuild bundling completed');
            console.log(`📊 Bundle size: ${fs.statSync(outputFile).size} bytes`);
            
        } catch (error) {
            throw new Error(`ESBuild failed: ${error.message}`);
        }
    }
    
    async handleNativeModules() {
        console.log('🔧 Handling native modules...');
        
        // Phase 3.1.2: Native Module Handling Implementation
        const nodeModulesSource = path.join(this.serverRoot, 'node_modules');
        const nodeModulesDest = path.join(this.buildDir, 'node_modules');
        
        // Copy essential native modules
        const nativeModules = [
            'voicemeeter-connector',
            'koffi'
            // @yume-chan modules are now bundled, not copied as external
        ];
        
        await fs.ensureDir(nodeModulesDest);
        
        for (const moduleName of nativeModules) {
            const sourcePath = path.join(nodeModulesSource, moduleName);
            const destPath = path.join(nodeModulesDest, moduleName);
            
            if (await fs.pathExists(sourcePath)) {
                await fs.copy(sourcePath, destPath);
                console.log(`✅ Copied native module: ${moduleName}`);
            } else {
                console.log(`⚠️ Native module not found: ${moduleName}`);
            }
        }
        
        // Copy audio assets
        const audioSource = path.join(this.serverRoot, 'audio');
        const audioDest = path.join(this.buildDir, 'audio');
        
        if (await fs.pathExists(audioSource)) {
            await fs.copy(audioSource, audioDest);
            console.log('✅ Audio assets copied');
        }
        
        console.log('✅ Native module handling completed');
    }
    
    async generateExecutable() {
        console.log('📦 Generating PKG executable...');
        
        // Phase 3.2.1: PKG Configuration Modernization
        const bundleFile = path.join(this.buildDir, 'server-bundle.js');
        const executablePath = path.join(this.distDir, 'audiodeck-server');
        
        // Create temporary package.json for PKG
        const pkgConfig = {
            name: 'audiodeck-server',
            version: '9.0.0',
            main: 'server-bundle.js',
            bin: 'server-bundle.js', // PKG requires bin property
            pkg: {
                assets: [
                    'node_modules/**/*',
                    'audio/**/*'
                ],
                targets: ['node18-macos-arm64'],
                outputPath: this.distDir
            }
        };
        
        const pkgConfigPath = path.join(this.buildDir, 'package.json');
        await fs.writeFile(pkgConfigPath, JSON.stringify(pkgConfig, null, 2));
        
        try {
            // Run PKG from build directory
            const pkgCommand = `npx pkg . --targets node18-macos-arm64 --output ${executablePath}`;
            execSync(pkgCommand, { 
                cwd: this.buildDir, 
                stdio: 'inherit',
                env: { ...process.env, NODE_ENV: 'production' }
            });
            
            console.log('✅ PKG executable generated');
            
        } catch (error) {
            throw new Error(`PKG generation failed: ${error.message}`);
        }
    }
    
    async validateBuild() {
        console.log('🔍 Validating build output...');
        
        const executablePath = path.join(this.distDir, 'audiodeck-server');
        
        // Check if executable exists
        if (!await fs.pathExists(executablePath)) {
            throw new Error('Executable not found after build');
        }
        
        // Check executable size
        const stats = await fs.stat(executablePath);
        console.log(`📊 Executable size: ${(stats.size / 1024 / 1024).toFixed(2)} MB`);
        
        // Make executable
        await fs.chmod(executablePath, 0o755);
        
        console.log('✅ Build validation completed');
    }
}

// Execute the build
new ESBuildServerBuilder(); 