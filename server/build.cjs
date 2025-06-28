#!/usr/bin/env node

const fs = require('fs-extra');
const path = require('path');
const { execSync } = require('child_process');

class ConsolidatedServerBuilder {
    constructor() {
        this.serverRoot = __dirname;
        this.projectRoot = path.join(this.serverRoot, '..');
        this.buildDir = path.join(this.serverRoot, 'build'); // Temp build dir for transpiled code
        this.distDir = path.join(this.projectRoot, 'dist'); // Final output dir
        
        console.log('🏗️ CONSOLIDATED SERVER BUILDER v9.1');
        console.log('🔥 Definitive Build Process (with Transpilation)');
        console.log('=====================================');
        
        this.build();
    }
    
    async build() {
        try {
            console.log('Phase 1: Environment Cleanup');
            this.cleanBuildEnvironment();
            
            console.log('Phase 2: Dependency Installation');
            this.installDependencies();
            
            console.log('Phase 3: Vendoring Dependencies');
            this.vendorDependencies();

            console.log('Phase 4: Transpilation with Babel');
            this.transpileWithBabel();

            console.log('Phase 5: PKG Build');
            this.buildExecutable();
            
            console.log('🎉 SUCCESS: Consolidated server executable created!');
            
        } catch (error) {
            console.error('❌ Build failed:', error.message);
            process.exit(1);
        }
    }
    
    cleanBuildEnvironment() {
        console.log('🧹 Cleaning build and dist directories...');
        if (fs.existsSync(this.buildDir)) {
            fs.rmSync(this.buildDir, { recursive: true, force: true });
        }
        if (fs.existsSync(this.distDir)) {
            fs.rmSync(this.distDir, { recursive: true, force: true });
        }
        fs.mkdirSync(this.buildDir, { recursive: true });
        fs.mkdirSync(this.distDir, { recursive: true });
        console.log('✅ Directories cleaned and recreated.');
    }
    
    installDependencies() {
        console.log('📦 Installing dependencies via npm...');
        execSync('npm install', { cwd: this.serverRoot, stdio: 'inherit' });
        console.log('✅ Dependencies installed.');
    }

    vendorDependencies() {
        console.log('🚚 Vendoring node_modules into build directory...');
        const source = path.join(this.serverRoot, 'node_modules');
        const destination = path.join(this.buildDir, 'node_modules');
        fs.copySync(source, destination);
        console.log('✅ Dependencies vendored.');
    }

    transpileWithBabel() {
        console.log('⚙️ Transpiling ES Modules to CommonJS...');
        const inputFile = path.join(this.serverRoot, 'src-consolidated.js');
        const babelCommand = `npx babel ${inputFile} --out-dir ${this.buildDir}`;
        execSync(babelCommand, { cwd: this.serverRoot, stdio: 'inherit' });
        console.log('✅ Transpilation complete.');
    }
    
    buildExecutable() {
        console.log('📦 Building executable with PKG...');
        const target = `node18-macos-arm64`;
        const outputFilename = 'soundboard-server';
        const outputPath = path.join(this.distDir, outputFilename);
        const inputFileForPkg = 'src-consolidated.js'; // Relative to the build directory

        // Run pkg from within the build directory to isolate it
        const command = `npx pkg ${inputFileForPkg} --targets ${target} --output ${outputPath} --debug`;
        
        execSync(command, { cwd: this.buildDir, stdio: 'inherit' });
        console.log(`✅ Executable built successfully at: ${outputPath}`);
    }
}

new ConsolidatedServerBuilder(); 