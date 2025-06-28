#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import os from 'os';
import { execSync, spawn } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

class CrossPlatformSetup {
    constructor() {
        this.platform = this.detectPlatform();
        this.projectRoot = path.resolve(__dirname, '..');
        this.config = this.loadPlatformConfig();
        
        console.log(`🔧 Setting up environment for ${this.platform}`);
    }
    
    detectPlatform() {
        const platform = os.platform();
        switch (platform) {
            case 'win32': return 'windows';
            case 'darwin': return 'darwin';
            case 'linux': return 'linux';
            default: 
                console.warn(`⚠️ Unknown platform: ${platform}, defaulting to linux`);
                return 'linux';
        }
    }
    
    loadPlatformConfig() {
        const configPath = path.join(this.projectRoot, 'platform-config.json');
        try {
            const configData = fs.readFileSync(configPath, 'utf8');
            return JSON.parse(configData);
        } catch (error) {
            console.error('❌ Failed to load platform configuration:', error.message);
            process.exit(1);
        }
    }
    
    expandPath(pathTemplate) {
        if (this.platform === 'windows') {
            // Expand Windows environment variables
            return pathTemplate
                .replace(/%LOCALAPPDATA%/g, process.env.LOCALAPPDATA || '')
                .replace(/%USERPROFILE%/g, process.env.USERPROFILE || '')
                .replace(/%USERNAME%/g, process.env.USERNAME || '')
                .replace(/%JAVA_HOME%/g, process.env.JAVA_HOME || '');
        } else {
            // Expand Unix environment variables
            return pathTemplate
                .replace(/\$HOME/g, os.homedir())
                .replace(/\$JAVA_HOME/g, process.env.JAVA_HOME || '')
                .replace(/\$\{([^}]+)\}/g, (match, varName) => process.env[varName] || '');
        }
    }
    
    findValidPath(pathTemplates) {
        for (const template of pathTemplates) {
            const expandedPath = this.expandPath(template);
            if (fs.existsSync(expandedPath)) {
                return expandedPath;
            }
        }
        return null;
    }
    
    async setupAndroidSDK() {
        console.log('📱 Setting up Android SDK...');
        
        const platformConfig = this.config.platforms[this.platform];
        const sdkPath = this.findValidPath(platformConfig.sdk.defaultPaths);
        
        if (!sdkPath) {
            console.error('❌ Android SDK not found. Please install Android Studio or SDK Command Line Tools.');
            console.log('📋 Recommended paths:');
            platformConfig.sdk.defaultPaths.forEach(path => {
                console.log(`   - ${this.expandPath(path)}`);
            });
            return false;
        }
        
        console.log(`✅ Found Android SDK: ${sdkPath}`);
        return sdkPath;
    }
    
    async setupJDK() {
        console.log('☕ Setting up JDK...');
        
        const platformConfig = this.config.platforms[this.platform];
        const jdkPath = this.findValidPath(platformConfig.jdk.defaultPaths);
        
        if (!jdkPath) {
            console.error('❌ JDK 17 not found. Please install OpenJDK 17.');
            console.log('📋 Recommended paths:');
            platformConfig.jdk.defaultPaths.forEach(path => {
                console.log(`   - ${this.expandPath(path)}`);
            });
            return false;
        }
        
        console.log(`✅ Found JDK: ${jdkPath}`);
        return jdkPath;
    }
    
    async setupADB() {
        console.log('🔌 Setting up ADB (Android Debug Bridge)...');
        
        const platformConfig = this.config.platforms[this.platform];
        const adbPath = this.findValidPath(platformConfig.adb.defaultPaths);
        
        if (!adbPath) {
            console.error('❌ ADB not found. Please install Android SDK Platform Tools.');
            console.log('📋 Expected paths:');
            platformConfig.adb.defaultPaths.forEach(path => {
                console.log(`   - ${this.expandPath(path)}`);
            });
            return false;
        }
        
        console.log(`✅ Found ADB: ${adbPath}`);
        
        // Test ADB functionality
        try {
            const result = execSync(`"${adbPath}" version`, { encoding: 'utf8' });
            console.log(`📱 ADB Version: ${result.split('\n')[0]}`);
        } catch (error) {
            console.warn('⚠️ ADB test failed:', error.message);
        }
        
        return adbPath;
    }
    
    async createLocalProperties(sdkPath, jdkPath) {
        console.log('📝 Creating local.properties...');
        
        const localPropsPath = path.join(this.projectRoot, 'local.properties');
        const content = `# Auto-generated by setup-environment.js
# Platform: ${this.platform}
# Generated: ${new Date().toISOString()}

sdk.dir=${sdkPath.replace(/\\/g, '/')}
java.home=${jdkPath.replace(/\\/g, '/')}
`;
        
        try {
            fs.writeFileSync(localPropsPath, content, 'utf8');
            console.log('✅ local.properties created successfully');
        } catch (error) {
            console.error('❌ Failed to create local.properties:', error.message);
            return false;
        }
        
        return true;
    }
    
    async setupServerEnvironment(adbPath) {
        console.log('🖥️ Setting up server environment...');
        
        const serverEnvPath = path.join(this.projectRoot, 'server', '.env');
        const content = `# Auto-generated by setup-environment.js
# Platform: ${this.platform}
# Generated: ${new Date().toISOString()}

ADB_PATH=${adbPath}
PLATFORM=${this.platform}
NODE_ENV=development
`;
        
        try {
            fs.writeFileSync(serverEnvPath, content, 'utf8');
            console.log('✅ Server .env file created successfully');
        } catch (error) {
            console.error('❌ Failed to create server .env file:', error.message);
            return false;
        }
        
        return true;
    }
    
    async installDependencies() {
        console.log('📦 Installing server dependencies...');
        
        try {
            const serverDir = path.join(this.projectRoot, 'server');
            process.chdir(serverDir);
            
            console.log('🔄 Running npm install...');
            execSync('npm install', { stdio: 'inherit' });
            
            console.log('✅ Server dependencies installed');
            return true;
        } catch (error) {
            console.error('❌ Failed to install dependencies:', error.message);
            return false;
        } finally {
            process.chdir(this.projectRoot);
        }
    }
    
    async validateGradleSetup() {
        console.log('🔨 Validating Gradle setup...');
        
        try {
            const gradleWrapper = this.platform === 'windows' ? 'gradlew.bat' : './gradlew';
            const result = execSync(`${gradleWrapper} --version`, { encoding: 'utf8' });
            console.log('✅ Gradle setup validated');
            return true;
        } catch (error) {
            console.warn('⚠️ Gradle validation failed:', error.message);
            return false;
        }
    }
    
    async createPlatformScripts() {
        console.log('📜 Creating platform-specific scripts...');
        
        if (this.platform === 'windows') {
            this.createWindowsBuildScript();
        } else {
            this.createUnixBuildScript();
        }
        
        console.log('✅ Platform scripts created');
    }
    
    createWindowsBuildScript() {
        const scriptPath = path.join(this.projectRoot, 'build.bat');
        const content = `@echo off
echo Building Android Soundboard Server...
call gradlew.bat clean assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
) else (
    echo Build failed!
    exit /b 1
)
`;
        
        fs.writeFileSync(scriptPath, content, 'utf8');
    }
    
    createUnixBuildScript() {
        const scriptPath = path.join(this.projectRoot, 'build.sh');
        const content = `#!/bin/bash
echo "Building Android Soundboard Server..."
./gradlew clean assembleDebug
if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi
`;
        
        fs.writeFileSync(scriptPath, content, 'utf8');
        fs.chmodSync(scriptPath, '755');
    }
    
    async run() {
        try {
            console.log('🚀 Starting cross-platform environment setup...');
            console.log('===============================================');
            
            // Step 1: Setup Android SDK
            const sdkPath = await this.setupAndroidSDK();
            if (!sdkPath) {
                console.log('⚠️ Continuing without Android SDK...');
            }
            
            // Step 2: Setup JDK
            const jdkPath = await this.setupJDK();
            if (!jdkPath) {
                console.log('⚠️ Continuing without JDK...');
            }
            
            // Step 3: Setup ADB
            const adbPath = await this.setupADB();
            if (!adbPath) {
                console.log('⚠️ Continuing without ADB...');
            }
            
            // Step 4: Create configuration files
            if (sdkPath && jdkPath) {
                await this.createLocalProperties(sdkPath, jdkPath);
            }
            
            if (adbPath) {
                await this.setupServerEnvironment(adbPath);
            }
            
            // Step 5: Install dependencies
            await this.installDependencies();
            
            // Step 6: Validate Gradle
            await this.validateGradleSetup();
            
            // Step 7: Create platform scripts
            await this.createPlatformScripts();
            
            console.log('===============================================');
            console.log('🎉 Environment setup completed successfully!');
            console.log('');
            console.log('📋 Next steps:');
            console.log('   1. Run: npm run build:server-comprehensive');
            console.log('   2. Test: npm run server');
            console.log('   3. Build executable: npm run build:server-comprehensive');
            
        } catch (error) {
            console.error('❌ Setup failed:', error.message);
            process.exit(1);
        }
    }
}

// Run the setup
const setup = new CrossPlatformSetup();
setup.run(); 