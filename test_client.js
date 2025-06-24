const io = require('socket.io-client');
const axios = require('axios');

// Use the actual server port instead of the Android forwarded port
const SERVER_URL = 'http://localhost:3001';
const socket = io(SERVER_URL, {
    transports: ['websocket', 'polling']
});

console.log('🤖 Android App Simulator - Testing Phase 2 Implementation');
console.log('============================================================');
console.log(`🔗 Connecting to server at ${SERVER_URL}...`);

socket.on('connect', () => {
    console.log('✅ Connected to server!');
    console.log(`Socket ID: ${socket.id}`);
    
    // Simulate Android app authentication
    console.log('\n🔐 Authenticating...');
    socket.emit('authenticate', {
        client_type: 'android',
        version: '1.0.0',
        timestamp: Date.now()
    });
});

socket.on('authenticated', (data) => {
    console.log('✅ Authentication successful!');
    console.log('Server info:', JSON.stringify(data, null, 2));
    
    // Test playing sounds
    setTimeout(() => {
        console.log('\n🎵 Testing sound playback...');
        
        const testSounds = [
            { file: 'applause.mp3', button: 1 },
            { file: 'drumroll.wav', button: 2 },
            { file: 'airhorn.mp3', button: 3 }
        ];
        
        testSounds.forEach((sound, index) => {
            setTimeout(() => {
                console.log(`🔊 Playing ${sound.file} (Button ${sound.button})`);
                socket.emit('play_sound', {
                    file_path: sound.file,
                    volume: 1.0,
                    button_id: sound.button,
                    timestamp: Date.now()
                });
            }, index * 2000); // 2 second intervals
        });
    }, 1000);
});

socket.on('play_response', (data) => {
    console.log('🎯 Play response received:');
    console.log(`   Status: ${data.status}`);
    console.log(`   Message: ${data.message}`);
    console.log(`   Button ID: ${data.button_id}`);
    console.log(`   Timestamp: ${data.timestamp}`);
    console.log('');
});

socket.on('connect_error', (error) => {
    console.error('❌ Connection error:', error.message);
});

socket.on('disconnect', () => {
    console.log('🔌 Disconnected from server');
});

// Test REST API endpoints
setTimeout(async () => {
    console.log('\n🌐 Testing REST API endpoints...');
    
    try {
        console.log('📊 Health check...');
        const healthResponse = await axios.get(`${SERVER_URL}/health`);
        console.log('✅ Health check response:', healthResponse.data);
        
        console.log('📁 Audio files list...');
        const filesResponse = await axios.get(`${SERVER_URL}/audio-files`);
        console.log('✅ Available audio files:', filesResponse.data.length);
        filesResponse.data.forEach(file => {
            console.log(`   - ${file.name} (${Math.round(file.size/1024)}KB)`);
        });
        
    } catch (error) {
        console.log('❌ REST API test failed:', error.message);
    }
    
    console.log('\n✅ Testing complete! Disconnecting...');
    socket.disconnect();
    process.exit(0);
}, 12000); 