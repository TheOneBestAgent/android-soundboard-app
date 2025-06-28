import { EventEmitter } from 'events';
import { AudioPlayer } from './AudioPlayer.js';
import { VoicemeeterManager } from './VoicemeeterManager.js';
import path from 'path';
import fs from 'fs-extra';

/**
 * Enterprise-grade Audio Manager
 * Coordinates all audio operations between AudioPlayer and VoicemeeterManager
 * Provides unified interface for audio control across all platforms
 */
export class AudioManager extends EventEmitter {
    constructor() {
        super();
        this.audioPlayer = new AudioPlayer();
        this.voicemeeterManager = new VoicemeeterManager(this.audioPlayer);
        this.currentlyPlaying = new Map();
        this.audioQueue = [];
        this.globalVolume = 1.0;
        this.isInitialized = false;
        
        console.log('🎵 AudioManager: Enterprise audio system initializing...');
        this.initialize();
    }
    
    async initialize() {
        try {
            console.log('🎵 AudioManager: Initializing audio services...');
            
            // Initialize Voicemeeter if available
            if (this.voicemeeterManager.platform === 'win32') {
                await this.voicemeeterManager.initializeVoicemeeter();
                const vmStatus = await this.voicemeeterManager.connect();
                console.log('🎛️ Voicemeeter status:', vmStatus);
            }
            
            this.isInitialized = true;
            console.log('✅ AudioManager: Initialization complete');
            this.emit('initialized');
            
        } catch (error) {
            console.error('❌ AudioManager: Initialization failed:', error);
            this.emit('error', error);
        }
    }
    
    /**
     * Play audio with enterprise-grade error handling and routing
     */
    async play(data) {
        try {
            const { filePath, volume = this.globalVolume, buttonId, options = {} } = data;
            
            if (!filePath) {
                throw new Error('Audio file path is required');
            }
            
            console.log(`🎵 AudioManager: Playing audio - ${filePath}`);
            
            // Validate file exists
            const fullPath = this.resolveAudioPath(filePath);
            if (!await fs.pathExists(fullPath)) {
                throw new Error(`Audio file not found: ${fullPath}`);
            }
            
            // Use Voicemeeter if available, otherwise direct audio
            let result;
            if (this.voicemeeterManager.method === 'native' && this.voicemeeterManager.isConnected) {
                result = await this.voicemeeterManager.playSound(fullPath, volume, buttonId, options);
            } else {
                result = await this.audioPlayer.playSound(fullPath, volume, buttonId);
            }
            
            // Track currently playing
            if (result && buttonId) {
                this.currentlyPlaying.set(buttonId, {
                    filePath: fullPath,
                    volume,
                    startTime: Date.now()
                });
            }
            
            this.emit('playback-started', { filePath: fullPath, volume, buttonId });
            return result;
            
        } catch (error) {
            console.error('❌ AudioManager: Playback error:', error);
            this.emit('playback-error', { error: error.message, data });
            throw error;
        }
    }
    
    /**
     * Stop audio playback
     */
    async stop(buttonId = null) {
        try {
            if (buttonId) {
                // Stop specific audio
                this.currentlyPlaying.delete(buttonId);
                console.log(`🛑 AudioManager: Stopped audio for button ${buttonId}`);
            } else {
                // Stop all audio
                this.audioPlayer.stopCurrentSound();
                this.currentlyPlaying.clear();
                console.log('🛑 AudioManager: Stopped all audio');
            }
            
            this.emit('playback-stopped', { buttonId });
            return true;
            
        } catch (error) {
            console.error('❌ AudioManager: Stop error:', error);
            this.emit('stop-error', { error: error.message, buttonId });
            return false;
        }
    }
    
    /**
     * Set global volume
     */
    setGlobalVolume(volume) {
        this.globalVolume = Math.max(0, Math.min(1, volume));
        console.log(`🔊 AudioManager: Global volume set to ${(this.globalVolume * 100).toFixed(0)}%`);
        this.emit('volume-changed', { volume: this.globalVolume });
    }
    
    /**
     * Get current status
     */
    getStatus() {
        return {
            initialized: this.isInitialized,
            platform: process.platform,
            globalVolume: this.globalVolume,
            currentlyPlaying: this.currentlyPlaying.size,
            voicemeeterAvailable: this.voicemeeterManager.method === 'native',
            voicemeeterConnected: this.voicemeeterManager.isConnected,
            audioMethod: this.voicemeeterManager.method,
            supportedFormats: this.audioPlayer.getSupportedFormats()
        };
    }
    
    /**
     * Resolve audio file path
     */
    resolveAudioPath(filePath) {
        if (path.isAbsolute(filePath)) {
            return filePath;
        }
        
        // Default audio directory
        const audioDir = path.join(process.cwd(), 'server', 'audio');
        return path.join(audioDir, filePath);
    }
    
    /**
     * Test audio system
     */
    async testAudio() {
        try {
            console.log('🧪 AudioManager: Running audio system test...');
            
            const testResult = await this.audioPlayer.testAudio();
            const status = this.getStatus();
            
            console.log('✅ AudioManager: Audio test complete');
            return {
                success: true,
                audioPlayerTest: testResult,
                status
            };
            
        } catch (error) {
            console.error('❌ AudioManager: Audio test failed:', error);
            return {
                success: false,
                error: error.message,
                status: this.getStatus()
            };
        }
    }
    
    /**
     * Shutdown audio manager
     */
    async shutdown() {
        try {
            console.log('🛑 AudioManager: Shutting down...');
            
            // Stop all audio
            await this.stop();
            
            // Disconnect Voicemeeter
            if (this.voicemeeterManager.isConnected) {
                await this.voicemeeterManager.disconnect();
            }
            
            // Cleanup
            this.currentlyPlaying.clear();
            this.audioQueue = [];
            this.isInitialized = false;
            
            console.log('✅ AudioManager: Shutdown complete');
            this.emit('shutdown');
            
        } catch (error) {
            console.error('❌ AudioManager: Shutdown error:', error);
            this.emit('shutdown-error', error);
        }
    }
} 