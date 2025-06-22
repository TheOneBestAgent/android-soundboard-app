const VoicemeeterManager = require('./src/audio/VoicemeeterManager');
const AudioPlayer = require('./src/audio/AudioPlayer');

async function testVoicemeeter() {
    console.log('🧪 Testing Voicemeeter Integration...\n');
    
    // Create instances
    const audioPlayer = new AudioPlayer();
    const voicemeeterManager = new VoicemeeterManager(audioPlayer);
    
    // Wait a moment for initialization
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // Test status
    console.log('📊 Voicemeeter Status:');
    console.log(JSON.stringify(voicemeeterManager.getStatus(), null, 2));
    console.log('');
    
    // Test connection (Windows only)
    if (process.platform === 'win32') {
        console.log('🔗 Testing Voicemeeter connection...');
        const connected = await voicemeeterManager.connect();
        console.log(`Connection result: ${connected ? '✅ Success' : '❌ Failed'}`);
        console.log('');
        
        if (connected) {
            // Test control functions
            console.log('🎛️ Testing Voicemeeter controls...');
            
            // Test strip mute
            console.log('Testing strip mute...');
            await voicemeeterManager.setStripMute(0, true);
            await new Promise(resolve => setTimeout(resolve, 1000));
            await voicemeeterManager.setStripMute(0, false);
            
            // Test strip gain
            console.log('Testing strip gain...');
            await voicemeeterManager.setStripGain(0, -10);
            await new Promise(resolve => setTimeout(resolve, 1000));
            await voicemeeterManager.setStripGain(0, 0);
            
            console.log('✅ Control tests completed');
        }
    } else {
        console.log('⚠️ Voicemeeter is Windows-only. Testing fallback to direct audio...');
        
        // Test fallback audio playback
        const testAudioPath = './audio/airhorn.mp3';
        console.log(`🎵 Testing audio playback: ${testAudioPath}`);
        
        try {
            const success = await voicemeeterManager.playSound(testAudioPath, 0.5);
            console.log(`Playback result: ${success ? '✅ Success' : '❌ Failed'}`);
        } catch (error) {
            console.log(`Playback error: ${error.message}`);
        }
    }
    
    // Cleanup
    console.log('\n🧹 Cleaning up...');
    await voicemeeterManager.shutdown();
    console.log('✅ Test completed');
}

// Run the test
testVoicemeeter().catch(console.error); 