#!/usr/bin/env node

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function buildMacOSDMG() {
    const projectRoot = path.resolve(__dirname, '..');
    const serverDir = path.join(projectRoot, 'server');
    const buildDir = path.join(projectRoot, 'dist');
    const sourceDir = path.join(projectRoot, 'server', 'src');
    
    console.log('🍎 Building macOS DMG for AudioDeck Connect Server');
    console.log('===============================================');
    
    try {
        // Ensure build directory exists
        if (!fs.existsSync(buildDir)) {
            fs.mkdirSync(buildDir, { recursive: true });
        }
        
        // Install dependencies
        console.log('📦 Installing dependencies...');
        execSync('npm install', { cwd: serverDir, stdio: 'inherit' });
        
        // Install electron-builder for DMG creation
        console.log('🛠️ Installing build tools...');
        execSync('npm install --save-dev electron-builder', { cwd: serverDir, stdio: 'inherit' });
        
        // Create electron-builder configuration
        const builderConfig = {
            appId: 'com.audiodeck.connect.server',
            productName: 'AudioDeck Connect Server',
            directories: {
                output: buildDir
            },
            mac: {
                category: 'public.app-category.music',
                target: 'dmg',
                icon: path.join(projectRoot, 'ADC.png')
            },
            files: [
                'src/**/*',
                'package.json',
                'node_modules/**/*'
            ]
        };
        
        // Write config
        fs.writeFileSync(
            path.join(serverDir, 'electron-builder.json'),
            JSON.stringify(builderConfig, null, 2)
        );
        
        // Build DMG
        console.log('🏗️ Building DMG...');
        execSync('npx electron-builder --mac', { cwd: serverDir, stdio: 'inherit' });
        
        console.log('✅ DMG build complete!');
        console.log(`📦 Output: ${path.join(buildDir, 'AudioDeck Connect Server.dmg')}`);
        
    } catch (error) {
        console.error('❌ Build failed:', error.message);
        process.exit(1);
    }
}

buildMacOSDMG().catch(console.error); 