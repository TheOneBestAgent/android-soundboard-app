const EventEmitter = require('events');
const os = require('os');
const path = require('path');

/**
 * Manages all interactions with the Voicemeeter Remote API.
 * This class handles initialization, connection, and audio playback routing.
 * Enhanced with conditional loading for cross-platform executable builds.
 */
class VoicemeeterManager extends EventEmitter {
    constructor(audioPlayer) {
        super();
        this.audioPlayer = audioPlayer;
        this.voicemeeter = null;
        this.isConnected = false;
        this.isAvailable = false;
        this.platform = process.platform;
        
        console.log(`🎛️ VoicemeeterManager initializing for platform: ${this.platform}`);
        
        if (this.platform !== 'win32') {
            console.log('🎛️ Voicemeeter not supported on this platform - using direct audio playback');
            return;
        }
        
        try {
            console.log('🔌 Attempting to load Voicemeeter Connector...');
            this.vm = require('voicemeeter-connector');
            this.method = 'native';
            console.log('✅ Voicemeeter Connector loaded successfully');
        } catch (error) {
            console.warn('⚠️ Voicemeeter connector module not available:', error);
            console.log('🔄 Falling back to direct audio playback');
            this.method = 'direct';
        }
    }

    /**
     * Connects to the Voicemeeter client application.
     */
    async connect() {
        if (this.platform !== 'win32') {
            return { status: 'direct', message: 'Using direct audio playback (non-Windows platform)' };
        }
        
        if (this.method === 'native' && this.vm) {
            try {
                await this.vm.connect();
                return { status: 'connected', message: 'Connected to Voicemeeter' };
            } catch (error) {
                console.error('❌ Failed to connect to Voicemeeter:', error);
                return { status: 'error', message: error.message };
            }
        }
        
        return { status: 'direct', message: 'Using direct audio playback (Voicemeeter not available)' };
    }
    
    /**
     * Disconnects from the Voicemeeter client application.
     */
    async disconnect() {
        if (this.method === 'native' && this.vm) {
            try {
                await this.vm.disconnect();
                return { status: 'disconnected', message: 'Disconnected from Voicemeeter' };
            } catch (error) {
                console.error('❌ Failed to disconnect from Voicemeeter:', error);
                return { status: 'error', message: error.message };
            }
        }
        return { status: 'ok', message: 'No active Voicemeeter connection to disconnect' };
    }

    /**
     * Plays a sound file using the most appropriate Voicemeeter method.
     * Prefers using the 'Recorder' for direct playback. Falls back to controlling
     * a virtual input strip if the recorder is unavailable.
     */
    async playSound(filePath, volume, buttonId, options = {}) {
        const { fileName } = options;
        console.log(`🎵 VoicemeeterManager: Playing ${fileName || path.basename(filePath)} (original volume: ${volume})`);
        const normalizedVolume = Math.max(0, Math.min(1, volume));
            
            if (this.isAvailable && this.isConnected) {
            try {
                // Use the 'setOption' method from the new library to execute a script
                const script = `Recorder.load = "${filePath.replace(/\\/g, '\\\\')}"; recorder.play = 1;`;
                console.log(`📼 Executing Voicemeeter script:\n${script}`);
                await this.voicemeeter.setOption(script);
                return true;
            } catch (error) {
                console.error('❌ Failed to play sound via Voicemeeter script:', error);
                console.warn('⚠️ Voicemeeter recorder API not available. Falling back to strip gain control.');
                return await this.playViaVoicemeeterFallback(filePath, normalizedVolume, buttonId, options);
            }
        } else {
            console.log('🔄 Voicemeeter not available, using direct audio playback.');
            return await this.audioPlayer.playSound(filePath, normalizedVolume, buttonId);
        }
    }
    
    async playViaVoicemeeterFallback(filePath, volume, buttonId, options) {
        try {
            console.log(`🎛️ Playing via Voicemeeter (Fallback): ${filePath} (Volume: ${(volume * 100).toFixed(0)}%)`);
            
            // Enhanced volume handling for Voicemeeter integration
            const normalizedVolume = Math.max(0, Math.min(1, volume));
            const volumeDb = this.linearToDb(normalizedVolume);
            
            console.log(`🔊 Volume settings: Linear=${normalizedVolume.toFixed(2)}, dB=${volumeDb.toFixed(1)}dB`);
            
            // Route through virtual input with dynamic strip control
            console.log('🔄 Using virtual input routing with strip control...');
            
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
            method: this.method || 'direct',
            isWindows: this.platform === 'win32',
            hasVoicemeeter: this.method === 'native' && !!this.vm,
            connectionStatus: this.vm ? 'available' : 'unavailable'
        };
    }
    
    // Graceful shutdown
    async shutdown() {
        console.log('🛑 Shutting down VoicemeeterManager...');
        await this.disconnect();
    }
}

module.exports = VoicemeeterManager; 