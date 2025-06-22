const mdns = require('mdns');
const os = require('os');

function setupMDNS(port) {
    try {
        // Create mDNS advertisement for the soundboard service
        const serviceType = mdns.tcp('soundboard');
        const serviceName = `Soundboard-${os.hostname()}`;
        
        const advertisement = mdns.createAdvertisement(serviceType, port, {
            name: serviceName,
            txtRecord: {
                version: '1.0.0',
                platform: process.platform,
                computer: os.hostname(),
                api: 'http',
                sockets: 'socketio'
            }
        });
        
        advertisement.start();
        
        console.log(`mDNS service advertised: ${serviceName}`);
        console.log(`Service type: _soundboard._tcp`);
        console.log('Android devices can now discover this server automatically');
        
        // Handle errors
        advertisement.on('error', (error) => {
            console.warn('mDNS advertisement error:', error.message);
            console.log('Service discovery will not be available, but manual connection still works');
        });
        
        // Graceful cleanup
        process.on('SIGINT', () => {
            console.log('Stopping mDNS advertisement...');
            advertisement.stop();
        });
        
        return advertisement;
        
    } catch (error) {
        console.warn('Could not start mDNS service discovery:', error.message);
        console.log('mDNS may not be available on this system');
        console.log('Manual IP connection will still work');
        return null;
    }
}

function discoverSoundboardServices() {
    return new Promise((resolve, reject) => {
        try {
            const serviceType = mdns.tcp('soundboard');
            const browser = mdns.createBrowser(serviceType);
            const services = [];
            
            browser.on('serviceUp', (service) => {
                console.log('Discovered soundboard service:', service);
                services.push({
                    name: service.name,
                    host: service.addresses[0],
                    port: service.port,
                    txtRecord: service.txtRecord
                });
            });
            
            browser.on('serviceDown', (service) => {
                console.log('Soundboard service went down:', service.name);
                const index = services.findIndex(s => s.name === service.name);
                if (index !== -1) {
                    services.splice(index, 1);
                }
            });
            
            browser.start();
            
            // Return services after a timeout
            setTimeout(() => {
                browser.stop();
                resolve(services);
            }, 5000);
            
        } catch (error) {
            reject(error);
        }
    });
}

module.exports = {
    setupMDNS,
    discoverSoundboardServices
}; 