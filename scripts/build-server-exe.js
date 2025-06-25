#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class ServerExeBuilder {
    constructor() {
        this.projectRoot = path.resolve(__dirname, '..');
        this.buildDir = path.join(this.projectRoot, 'dist');
        this.serverDir = path.join(this.projectRoot, 'server');
        
        console.log('🔨 Building Soundboard Server Executable...');
        this.build();
    }
    
    async build() {
        try {
            // Ensure build directory exists
            this.ensureBuildDir();
            
            // Create Electron configuration
            this.createElectronConfig();
            
            // Install dependencies if needed
            this.installDependencies();
            
            // Build the executable
            this.buildElectronApp();
            
            console.log('✅ Soundboard Server executable built successfully!');
            console.log(`📦 Output directory: ${this.buildDir}`);
            
        } catch (error) {
            console.error('❌ Build failed:', error.message);
            process.exit(1);
        }
    }
    
    ensureBuildDir() {
        if (!fs.existsSync(this.buildDir)) {
            fs.mkdirSync(this.buildDir, { recursive: true });
        }
    }
    
    createElectronConfig() {
        console.log('📝 Creating Electron configuration...');
        
        // Create main.js for Electron
        const mainJs = `
const { app, BrowserWindow, Tray, Menu, nativeImage, dialog, shell, ipcMain } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');

// Import our tray application
const SoundboardServerTray = require('./scripts/server-tray.js');

// Override app.whenReady to prevent duplicate initialization
const originalWhenReady = app.whenReady;
app.whenReady = () => {
    return originalWhenReady.call(app);
};

// Initialize the server tray
new SoundboardServerTray();
`;

        const mainPath = path.join(this.projectRoot, 'main.js');
        fs.writeFileSync(mainPath, mainJs);
        
        // Create or update electron package.json
        const electronPackageJson = {
            name: 'soundboard-server-tray',
            version: '1.0.0',
            description: 'Soundboard Server System Tray Application',
            main: 'main.js',
            author: 'Soundboard Team',
            license: 'ISC',
            build: {
                appId: 'com.soundboard.server-tray',
                productName: 'Soundboard Server',
                directories: {
                    output: 'dist'
                },
                files: [
                    'main.js',
                    'scripts/server-tray.js',
                    'server/**/*',
                    'node_modules/**/*'
                ],
                win: {
                    target: 'nsis',
                    icon: 'assets/icon.ico'
                },
                nsis: {
                    oneClick: false,
                    allowToChangeInstallationDirectory: true,
                    createDesktopShortcut: true,
                    createStartMenuShortcut: true,
                    shortcutName: 'Soundboard Server'
                },
                mac: {
                    target: 'dmg',
                    icon: 'assets/icon.icns'
                },
                linux: {
                    target: 'AppImage',
                    icon: 'assets/icon.png'
                }
            }
        };
        
        // Update main package.json with electron build info
        const packageJsonPath = path.join(this.projectRoot, 'package.json');
        const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
        
        // Add electron as main and build config
        packageJson.main = 'main.js';
        packageJson.homepage = './';
        if (!packageJson.build) {
            packageJson.build = electronPackageJson.build;
        }
        
        fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
        
        console.log('✅ Electron configuration created');
    }
    
    installDependencies() {
        console.log('📦 Installing dependencies...');
        
        try {
            // Install root dependencies (including electron)
            console.log('Installing root dependencies...');
            execSync('npm install', { 
                cwd: this.projectRoot,
                stdio: 'inherit'
            });
            
            // Install server dependencies
            console.log('Installing server dependencies...');
            execSync('npm install', { 
                cwd: this.serverDir,
                stdio: 'inherit'
            });
            
            console.log('✅ Dependencies installed');
        } catch (error) {
            throw new Error(`Failed to install dependencies: ${error.message}`);
        }
    }
    
    buildElectronApp() {
        console.log('🔨 Building Electron application...');
        
        try {
            // Use electron-builder to create the executable
            const command = process.platform === 'win32' 
                ? 'npx electron-builder --win --x64'
                : process.platform === 'darwin'
                    ? 'npx electron-builder --mac'
                    : 'npx electron-builder --linux';
            
            console.log(`Running: ${command}`);
            execSync(command, { 
                cwd: this.projectRoot,
                stdio: 'inherit'
            });
            
            console.log('✅ Electron application built');
            
            // List output files
            this.listOutputFiles();
            
        } catch (error) {
            throw new Error(`Failed to build Electron app: ${error.message}`);
        }
    }
    
    listOutputFiles() {
        console.log('\\n📋 Build Output:');
        
        try {
            const distContents = fs.readdirSync(this.buildDir);
            distContents.forEach(file => {
                const filePath = path.join(this.buildDir, file);
                const stats = fs.statSync(filePath);
                const size = stats.isFile() ? `(${(stats.size / 1024 / 1024).toFixed(2)} MB)` : '(directory)';
                console.log(`   📁 ${file} ${size}`);
            });
        } catch (error) {
            console.log('   (Could not list output files)');
        }
        
        console.log('\\n🎉 Build completed! You can now run the executable from the dist folder.');
        
        if (process.platform === 'win32') {
            console.log('\\n💡 On Windows, look for:');
            console.log('   • Setup file: Soundboard Server Setup.exe');
            console.log('   • Portable: soundboard-server-tray.exe');
        }
    }
}

// Run the builder
if (require.main === module) {
    new ServerExeBuilder();
}

module.exports = ServerExeBuilder;