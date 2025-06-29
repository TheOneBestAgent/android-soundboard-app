import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs-extra';
import os from 'os';
import AsyncUtils from '../utils/AsyncUtils.js';

export class AudioPlayer {
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
            
            // Use AsyncUtils for process execution with timeout
            const result = await AsyncUtils.withTimeout(
                async (signal) => {
                    return await AsyncUtils.executeProcess('powershell', ['-Command', psScript], {
                        windowsHide: true
                    }, signal);
                },
                5000,
                'Windows audio playback timeout'
            );
            
            console.log(`Windows audio playback finished with code: ${result.code}`);
            return result.code === 0;
                
        } catch (error) {
            console.error('Windows playback error:', error);
            return false;
        }
    }
    
    async playOnMacOS(filePath, volume) {
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
            
            // Try primary approach with timeout
            try {
                const result = await AsyncUtils.withTimeout(
                    async (signal) => {
                        const result = await AsyncUtils.executeProcess('afplay', afplayArgs, {
                            stdio: ['ignore', 'pipe', 'pipe']
                        }, signal);
                        
                        console.log(`🔊 macOS audio playback finished with code: ${result.code}`);
                        if (result.stdout) console.log(`🔊 Audio output: ${result.stdout.trim()}`);
                        if (result.stderr) console.log(`🔊 Error output: ${result.stderr.trim()}`);
                        
                        return result.code === 0;
                    },
                    3000,
                    'afplay timeout'
                );
                
                return result;
                
            } catch (primaryError) {
                console.log('🔊 Primary afplay approach failed, trying alternative approach...');
                
                // Fallback to osascript approach
                const result = await AsyncUtils.withTimeout(
                    async (signal) => {
                        return await AsyncUtils.executeProcess('osascript', [
                            '-e', 
                            `do shell script "afplay '${filePath.replace(/'/g, "\\'")}' &"`
                        ], { stdio: 'pipe' }, signal);
                    },
                    5000,
                    'osascript timeout'
                );
                
                console.log(`🔊 Alternative osascript approach finished with code: ${result.code}`);
                return result.code === 0;
            }
                
        } catch (error) {
            console.error('🔊 macOS playback error:', error);
            return false;
        }
    }
    
    async playOnLinux(filePath, volume) {
        try {
            // Try different audio players in order of preference
            const players = [
                { cmd: 'paplay', args: [filePath] }, // PulseAudio
                { cmd: 'aplay', args: [filePath] },  // ALSA
                { cmd: 'mpg123', args: [filePath] }, // For MP3s
                { cmd: 'play', args: [filePath] }    // SoX
            ];
            
            return await this.tryLinuxPlayersAsync(players, 0, volume);
            
        } catch (error) {
            console.error('Linux playback error:', error);
            return false;
        }
    }
    
    async tryLinuxPlayersAsync(players, index, volume) {
        if (index >= players.length) {
            console.error('No suitable audio player found on Linux');
            return false;
        }
        
        const player = players[index];
        
        try {
            const result = await AsyncUtils.withTimeout(
                async (signal) => {
                    return await AsyncUtils.executeProcess(player.cmd, player.args, {
                        stdio: 'ignore'
                    }, signal);
                },
                5000,
                `${player.cmd} timeout`
            );
            
            if (result.code === 0) {
                console.log(`Linux audio playback successful with ${player.cmd}`);
                return true;
            } else {
                console.log(`${player.cmd} failed with code ${result.code}, trying next player...`);
                return await this.tryLinuxPlayersAsync(players, index + 1, volume);
            }
            
        } catch (error) {
            console.log(`${player.cmd} not available or failed: ${error.message}, trying next player...`);
            return await this.tryLinuxPlayersAsync(players, index + 1, volume);
        }
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
                
                const result = await AsyncUtils.withTimeout(
                    async (signal) => {
                        return await AsyncUtils.executeProcess('osascript', ['-e', 'beep'], {
                            stdio: 'pipe'
                        }, signal);
                    },
                    2000,
                    'System beep test timeout'
                );
                
                console.log(`🔊 System beep test finished with code: ${result.code}`);
                return result.code === 0;
            }
        } catch (error) {
            console.error('🔊 Audio test error:', error);
            return false;
        }
        
        return true;
    }
} 