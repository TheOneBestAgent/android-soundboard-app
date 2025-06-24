const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs-extra');
const os = require('os');

class VoicemeeterManager {
    constructor(audioPlayer) {
        this.platform = process.platform;
        this.audioPlayer = audioPlayer; // Fallback to direct audio playback
        this.voicemeeter = null;
        this.isConnected = false;
        this.isAvailable = false;
        
        console.log(`🎛️ VoicemeeterManager initialized for platform: ${this.platform}`);
        
        // Only try to initialize Voicemeeter on Windows
        if (this.platform === 'win32') {
            this.initializeVoicemeeter();
        } else {
            console.log(`⚠️ Voicemeeter is only available on Windows. Using direct audio playback on ${this.platform}.`);
        }
    }
    
    async initializeVoicemeeter() {
        try {
            // Dynamic import for Windows-only dependency
            const voicemeeterRemote = require('voicemeeter-remote');
            
            console.log('🔌 Initializing Voicemeeter Remote API...');
            
            await voicemeeterRemote.init();
            this.voicemeeter = voicemeeterRemote;
            this.isAvailable = true;
            
            console.log('✅ Voicemeeter Remote API initialized successfully');
            
            // Try to connect
            await this.connect();
            
        } catch (error) {
            console.warn('⚠️ Failed to initialize Voicemeeter Remote API:', error.message);
            console.log('📢 Using direct audio playback as fallback');
            this.isAvailable = false;
        }
    }
    
    async connect() {
        if (!this.isAvailable || this.isConnected) {
            return false;
        }
        
        try {
            console.log('🔗 Connecting to Voicemeeter...');
            this.voicemeeter.login();
            this.isConnected = true;
            
            console.log('✅ Connected to Voicemeeter successfully');
            
            // Log Voicemeeter version and type
            const version = this.voicemeeter.getVoicemeeterVersion();
            const type = this.voicemeeter.getVoicemeeterType();
            console.log(`🎛️ Voicemeeter Type: ${type}, Version: ${version}`);
            
            return true;
        } catch (error) {
            console.error('❌ Failed to connect to Voicemeeter:', error.message);
            this.isConnected = false;
            return false;
        }
    }
    
    async disconnect() {
        if (!this.isAvailable || !this.isConnected) {
            return;
        }
        
        try {
            console.log('🔌 Disconnecting from Voicemeeter...');
            this.voicemeeter.logout();
            this.isConnected = false;
            console.log('✅ Disconnected from Voicemeeter');
        } catch (error) {
            console.error('❌ Error disconnecting from Voicemeeter:', error.message);
        }
    }
    
    async playSound(filePath, volume = 1.0, buttonId = null, options = {}) {
        try {
            const fileName = require('path').basename(filePath);
            console.log(`🎵 VoicemeeterManager: Playing ${fileName} (original volume: ${volume})`);
            
            // Apply volume normalization if enabled
            const normalizedVolume = options.enableNormalization !== false ? 
                this.normalizeVolume(volume, fileName) : volume;
            
            if (normalizedVolume !== volume) {
                console.log(`🎚️ Volume adjusted: ${(volume * 100).toFixed(0)}% -> ${(normalizedVolume * 100).toFixed(0)}%`);
            }
            
            if (this.isAvailable && this.isConnected) {
                return await this.playViaVoicemeeter(filePath, normalizedVolume, buttonId, {
                    ...options,
                    originalVolume: volume,
                    normalizedVolume: normalizedVolume,
                    fileName: fileName
                });
            } else {
                console.log('🔄 Voicemeeter not available, using direct audio playback');
                return await this.audioPlayer.playSound(filePath, normalizedVolume);
            }
        } catch (error) {
            console.error('❌ Error in VoicemeeterManager.playSound:', error);
            // Fallback to direct audio playback
            console.log('🔄 Falling back to direct audio playback');
            return await this.audioPlayer.playSound(filePath, volume);
        }
    }
    
    async playViaVoicemeeter(filePath, volume, buttonId, options) {
        try {
            console.log(`🎛️ Playing via Voicemeeter: ${filePath} (Volume: ${(volume * 100).toFixed(0)}%)`);
            
            // Enhanced volume handling for Voicemeeter integration
            const normalizedVolume = Math.max(0, Math.min(1, volume));
            const volumeDb = this.linearToDb(normalizedVolume);
            
            console.log(`🔊 Volume settings: Linear=${normalizedVolume.toFixed(2)}, dB=${volumeDb.toFixed(1)}dB`);
            
            // Option 1: Try to use cassette player with enhanced volume control
            if (this.voicemeeter.setCassetteFile) {
                console.log('📼 Loading file into Voicemeeter cassette player...');
                
                // Stop any currently playing cassette
                try {
                    await this.voicemeeter.setCassettePlay(false);
                    await new Promise(resolve => setTimeout(resolve, 100)); // Small delay
                } catch (e) {
                    // Ignore if cassette wasn't playing
                }
                
                // Load new file and set volume
                await this.voicemeeter.setCassetteFile(filePath);
                
                // Set cassette volume before playing
                if (this.voicemeeter.setCassetteGain) {
                    await this.voicemeeter.setCassetteGain(volumeDb);
                    console.log(`🎚️ Cassette gain set to ${volumeDb.toFixed(1)}dB`);
                }
                
                // Start playback
                await this.voicemeeter.setCassettePlay(true);
                
                console.log('✅ Audio playing via Voicemeeter cassette player with volume control');
                return true;
            }
            
            // Option 2: Route through virtual input with dynamic strip control
            console.log('🔄 Cassette player not available, using virtual input routing with strip control...');
            
            // Try to set volume on a designated strip (e.g., Strip 0 for soundboard)
            const soundboardStripIndex = options.stripIndex || 0;
            
            try {
                // Set the strip gain for this playback
                await this.setStripGain(soundboardStripIndex, volumeDb);
                console.log(`🎚️ Strip ${soundboardStripIndex} gain set to ${volumeDb.toFixed(1)}dB for soundboard`);
            } catch (e) {
                console.warn(`⚠️ Could not set strip gain: ${e.message}`);
            }
            
            // Play the audio file normally and let Voicemeeter capture it
            // The volume will be controlled by the strip gain we just set
            const result = await this.audioPlayer.playSound(filePath, 1.0); // Use full volume, let Voicemeeter handle it
            
            // Optional: Reset strip gain after playback (with delay)
            if (options.resetStripAfterPlayback !== false) {
                setTimeout(async () => {
                    try {
                        await this.setStripGain(soundboardStripIndex, 0); // Reset to 0dB
                        console.log(`🔄 Strip ${soundboardStripIndex} gain reset to 0dB`);
                    } catch (e) {
                        console.warn(`⚠️ Could not reset strip gain: ${e.message}`);
                    }
                }, 5000); // Reset after 5 seconds
            }
            
            return result;
            
        } catch (error) {
            console.error('❌ Error playing via Voicemeeter:', error);
            throw error;
        }
    }
    
    // Voicemeeter Control Methods
    async setStripMute(stripIndex, mute) {
        if (!this.isConnected) return false;
        
        try {
            await this.voicemeeter.setStripMute(stripIndex, mute);
            console.log(`🎛️ Strip ${stripIndex} mute: ${mute}`);
            return true;
        } catch (error) {
            console.error(`❌ Error setting strip ${stripIndex} mute:`, error);
            return false;
        }
    }
    
    async setStripGain(stripIndex, gainDb) {
        if (!this.isConnected) return false;
        
        try {
            await this.voicemeeter.setStripGain(stripIndex, gainDb);
            console.log(`🎛️ Strip ${stripIndex} gain: ${gainDb}dB`);
            return true;
        } catch (error) {
            console.error(`❌ Error setting strip ${stripIndex} gain:`, error);
            return false;
        }
    }
    
    async setBusGain(busIndex, gainDb) {
        if (!this.isConnected) return false;
        
        try {
            await this.voicemeeter.setBusGain(busIndex, gainDb);
            console.log(`🎛️ Bus ${busIndex} gain: ${gainDb}dB`);
            return true;
        } catch (error) {
            console.error(`❌ Error setting bus ${busIndex} gain:`, error);
            return false;
        }
    }
    
    async setBusMute(busIndex, mute) {
        if (!this.isConnected) return false;
        
        try {
            await this.voicemeeter.setBusMute(busIndex, mute);
            console.log(`🎛️ Bus ${busIndex} mute: ${mute}`);
            return true;
        } catch (error) {
            console.error(`❌ Error setting bus ${busIndex} mute:`, error);
            return false;
        }
    }
    
    // Utility Methods
    linearToDb(linear) {
        if (linear <= 0) return -60; // Minimum dB
        return 20 * Math.log10(linear);
    }
    
    dbToLinear(db) {
        return Math.pow(10, db / 20);
    }
    
    // Volume normalization for consistent levels across different audio files
    normalizeVolume(volume, audioFileName = '') {
        // Basic volume normalization based on common audio file characteristics
        let normalizedVolume = volume;
        
        // Apply normalization based on file type or name patterns
        const fileName = audioFileName.toLowerCase();
        
        if (fileName.includes('airhorn') || fileName.includes('loud')) {
            // Reduce volume for typically loud files
            normalizedVolume *= 0.7;
            console.log(`🔧 Applied loud file normalization: ${volume.toFixed(2)} -> ${normalizedVolume.toFixed(2)}`);
        } else if (fileName.includes('whisper') || fileName.includes('quiet') || fileName.includes('soft')) {
            // Boost volume for typically quiet files
            normalizedVolume *= 1.3;
            console.log(`🔧 Applied quiet file normalization: ${volume.toFixed(2)} -> ${normalizedVolume.toFixed(2)}`);
        } else if (fileName.includes('voice') || fileName.includes('speech')) {
            // Slight boost for voice files
            normalizedVolume *= 1.1;
            console.log(`🔧 Applied voice normalization: ${volume.toFixed(2)} -> ${normalizedVolume.toFixed(2)}`);
        }
        
        // Ensure we don't exceed maximum volume
        return Math.min(1.0, normalizedVolume);
    }
    
    // Get recommended volume levels for different audio types
    getVolumeRecommendations() {
        return {
            voice: { min: 0.6, max: 0.8, default: 0.7 },
            music: { min: 0.4, max: 0.7, default: 0.5 },
            effects: { min: 0.3, max: 0.9, default: 0.6 },
            alerts: { min: 0.7, max: 1.0, default: 0.8 }
        };
    }
    
    getStatus() {
        return {
            platform: this.platform,
            available: this.isAvailable,
            connected: this.isConnected,
            version: this.isConnected && this.voicemeeter ? this.voicemeeter.getVoicemeeterVersion() : null,
            type: this.isConnected && this.voicemeeter ? this.voicemeeter.getVoicemeeterType() : null
        };
    }
    
    // Graceful shutdown
    async shutdown() {
        console.log('🛑 Shutting down VoicemeeterManager...');
        await this.disconnect();
    }
}

module.exports = VoicemeeterManager; 