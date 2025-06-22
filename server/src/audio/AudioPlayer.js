const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs-extra');
const os = require('os');

class AudioPlayer {
    constructor() {
        this.platform = process.platform;
        this.audioDir = path.join(__dirname, '../../audio');
        this.currentPlaying = null;
        
        console.log(`Audio player initialized for platform: ${this.platform}`);
    }
    
    async playSound(filePath, volume = 1.0, customDir = null) {
        try {
            console.log(`🎵 AudioPlayer.playSound called with: ${filePath}`);
            
            // Determine the full path to the audio file
            const fullPath = this.getFullAudioPath(filePath, customDir);
            
            console.log(`🔍 Checking if file exists: ${fullPath}`);
            const fileExists = await fs.pathExists(fullPath);
            
            if (!fileExists) {
                console.error(`❌ Audio file not found: ${fullPath}`);
                
                // Additional debugging: check if it's a temp file issue
                if (fullPath.includes('temp_')) {
                    console.log(`🔍 This appears to be a temp file. Let's check the temp directory...`);
                    const tempDir = path.dirname(fullPath);
                    console.log(`📁 Temp directory: ${tempDir}`);
                    
                    try {
                        const dirContents = await fs.readdir(tempDir);
                        console.log(`📋 Temp directory contents: ${dirContents.join(', ')}`);
                    } catch (e) {
                        console.error(`❌ Could not read temp directory: ${e.message}`);
                    }
                }
                
                return false;
            }
            
            // Get file stats for debugging
            const stats = await fs.stat(fullPath);
            console.log(`✅ File found! Size: ${stats.size} bytes, Modified: ${stats.mtime}`);
            console.log(`🎵 Playing audio: ${fullPath} (volume: ${volume})`);
            
            switch (this.platform) {
                case 'win32':
                    return await this.playOnWindows(fullPath, volume);
                case 'darwin':
                    return await this.playOnMacOS(fullPath, volume);
                case 'linux':
                    return await this.playOnLinux(fullPath, volume);
                default:
                    console.error(`Platform ${this.platform} not supported`);
                    return false;
            }
        } catch (error) {
            console.error('❌ Error playing sound:', error);
            return false;
        }
    }
    
    getFullAudioPath(filePath, customDir = null) {
        // Check if the filePath is already an absolute path
        if (path.isAbsolute(filePath)) {
            // For absolute paths (like temp files), use them directly
            console.log(`📁 Using absolute path: ${filePath}`);
            return filePath;
        }
        
        // For relative paths, prevent directory traversal attacks and use audio directory
        const safePath = path.basename(filePath);
        const baseDir = customDir || this.audioDir;
        const fullPath = path.join(baseDir, safePath);
        console.log(`📁 Using relative path: ${safePath} -> ${fullPath}`);
        return fullPath;
    }
    
    async playOnWindows(filePath, volume) {
        return new Promise((resolve) => {
            try {
                // Use PowerShell with Windows Media Player COM object
                const volumePercent = Math.round(volume * 100);
                const psScript = `
                    Add-Type -AssemblyName presentationCore
                    $mediaPlayer = New-Object system.windows.media.mediaplayer
                    $mediaPlayer.open('${filePath.replace(/'/g, "''")}')
                    $mediaPlayer.Volume = ${volume}
                    $mediaPlayer.Play()
                    Start-Sleep -Seconds 1
                `;
                
                const process = spawn('powershell', ['-Command', psScript], {
                    windowsHide: true
                });
                
                process.on('close', (code) => {
                    console.log(`Windows audio playback finished with code: ${code}`);
                    resolve(code === 0);
                });
                
                process.on('error', (error) => {
                    console.error('Windows audio playback error:', error);
                    resolve(false);
                });
                
                // Don't wait for the process to finish (fire and forget)
                setTimeout(() => resolve(true), 100);
                
            } catch (error) {
                console.error('Windows playback error:', error);
                resolve(false);
            }
        });
    }
    
    async playOnMacOS(filePath, volume) {
        return new Promise((resolve) => {
            try {
                console.log(`macOS: Playing ${filePath} with volume ${volume}`);
                
                // Use afplay command (built into macOS) with volume control
                const afplayArgs = [filePath];
                
                // afplay supports volume control with -v flag (0.0 to 1.0)
                if (volume !== 1.0) {
                    afplayArgs.push('-v', volume.toString());
                }
                
                // Add debug flag to get more information
                afplayArgs.push('-d');
                
                console.log(`🔊 Running afplay with args: ${afplayArgs.join(' ')}`);
                
                const process = spawn('afplay', afplayArgs, {
                    stdio: ['ignore', 'pipe', 'pipe']
                });
                
                // Store the current playing process
                this.currentPlaying = process;
                
                let hasResolved = false;
                let audioOutput = '';
                let errorOutput = '';
                
                process.stdout.on('data', (data) => {
                    const output = data.toString();
                    audioOutput += output;
                    console.log(`🔊 afplay stdout: ${output.trim()}`);
                });
                
                process.stderr.on('data', (data) => {
                    const output = data.toString();
                    errorOutput += output;
                    console.error(`🔊 afplay stderr: ${output.trim()}`);
                });
                
                process.on('close', (code, signal) => {
                    console.log(`🔊 macOS audio playback finished with code: ${code}, signal: ${signal}`);
                    if (audioOutput) console.log(`🔊 Audio output: ${audioOutput.trim()}`);
                    if (errorOutput) console.log(`🔊 Error output: ${errorOutput.trim()}`);
                    
                    this.currentPlaying = null;
                    if (!hasResolved) {
                        hasResolved = true;
                        resolve(code === 0);
                    }
                });
                
                process.on('error', (error) => {
                    console.error(`🔊 macOS audio playback error: ${error.message}`);
                    this.currentPlaying = null;
                    if (!hasResolved) {
                        hasResolved = true;
                        resolve(false);
                    }
                });
                
                // Test if afplay is actually working by checking after a short delay
                setTimeout(() => {
                    if (!hasResolved && this.currentPlaying && !this.currentPlaying.killed) {
                        console.log('🔊 afplay process started successfully');
                    } else if (!hasResolved) {
                        console.error('🔊 afplay failed to start properly');
                        hasResolved = true;
                        resolve(false);
                    }
                }, 100);
                
                // Also try alternative approach if afplay doesn't work
                setTimeout(async () => {
                    if (!hasResolved) {
                        console.log('🔊 afplay taking too long, trying alternative approach...');
                        try {
                            // Try using osascript to play sound as backup
                            const osascriptProcess = spawn('osascript', [
                                '-e', 
                                `do shell script "afplay '${filePath.replace(/'/g, "\\'")}' &"`
                            ], { stdio: 'pipe' });
                            
                            osascriptProcess.on('close', (code) => {
                                if (!hasResolved) {
                                    console.log(`🔊 Alternative osascript approach finished with code: ${code}`);
                                    hasResolved = true;
                                    resolve(code === 0);
                                }
                            });
                            
                        } catch (altError) {
                            console.error(`🔊 Alternative approach failed: ${altError.message}`);
                            if (!hasResolved) {
                                hasResolved = true;
                                resolve(false);
                            }
                        }
                    }
                }, 2000);
                
            } catch (error) {
                console.error('🔊 macOS playback error:', error);
                resolve(false);
            }
        });
    }
    
    async playOnLinux(filePath, volume) {
        return new Promise((resolve) => {
            try {
                // Try different audio players in order of preference
                const players = [
                    { cmd: 'paplay', args: [filePath] }, // PulseAudio
                    { cmd: 'aplay', args: [filePath] },  // ALSA
                    { cmd: 'mpg123', args: [filePath] }, // For MP3s
                    { cmd: 'play', args: [filePath] }    // SoX
                ];
                
                this.tryLinuxPlayers(players, 0, volume, resolve);
                
            } catch (error) {
                console.error('Linux playback error:', error);
                resolve(false);
            }
        });
    }
    
    tryLinuxPlayers(players, index, volume, resolve) {
        if (index >= players.length) {
            console.error('No suitable audio player found on Linux');
            resolve(false);
            return;
        }
        
        const player = players[index];
        const process = spawn(player.cmd, player.args, {
            stdio: 'ignore'
        });
        
        process.on('close', (code) => {
            if (code === 0) {
                console.log(`Linux audio playback successful with ${player.cmd}`);
                resolve(true);
            } else {
                console.log(`${player.cmd} failed, trying next player...`);
                this.tryLinuxPlayers(players, index + 1, volume, resolve);
            }
        });
        
        process.on('error', (error) => {
            console.log(`${player.cmd} not available, trying next player...`);
            this.tryLinuxPlayers(players, index + 1, volume, resolve);
        });
        
        // Don't wait too long for each attempt
        setTimeout(() => {
            if (!process.killed) {
                process.kill();
                this.tryLinuxPlayers(players, index + 1, volume, resolve);
            }
        }, 5000);
    }
    
    stopCurrentSound() {
        if (this.currentPlaying) {
            try {
                this.currentPlaying.kill();
                this.currentPlaying = null;
                console.log('Stopped current audio playback');
            } catch (error) {
                console.error('Error stopping audio:', error);
            }
        }
    }
    
    getSupportedFormats() {
        return ['.mp3', '.wav', '.m4a', '.ogg', '.aac', '.flac'];
    }
    
    // Test function to verify audio is working
    async testAudio() {
        console.log('🔊 Testing audio system...');
        console.log(`🔊 Platform: ${this.platform}`);
        console.log(`🔊 Audio directory: ${this.audioDir}`);
        
        // Test system audio first
        try {
            if (this.platform === 'darwin') {
                console.log('🔊 Testing macOS system beep...');
                const beepProcess = spawn('osascript', ['-e', 'beep'], { stdio: 'pipe' });
                
                return new Promise((resolve) => {
                    beepProcess.on('close', (code) => {
                        console.log(`🔊 System beep test finished with code: ${code}`);
                        resolve(code === 0);
                    });
                    
                    beepProcess.on('error', (error) => {
                        console.error(`🔊 System beep test failed: ${error.message}`);
                        resolve(false);
                    });
                    
                    setTimeout(() => {
                        console.log('🔊 System beep test completed');
                        resolve(true);
                    }, 1000);
                });
            }
        } catch (error) {
            console.error('🔊 Audio test error:', error);
            return false;
        }
        
        return true;
    }
}

module.exports = AudioPlayer; 