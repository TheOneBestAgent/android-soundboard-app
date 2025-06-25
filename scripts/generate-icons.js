const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const DENSITIES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
};

async function generateIcons() {
    const resDir = path.join(__dirname, '../app/src/main/res');
    
    // Ensure mipmap directories exist
    Object.keys(DENSITIES).forEach(density => {
        const dir = path.join(resDir, `mipmap-${density}`);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
    });

    // Generate icons for each density
    for (const [density, size] of Object.entries(DENSITIES)) {
        await sharp(path.join(__dirname, '../icon.png'))
            .resize(size, size)
            .toFile(path.join(resDir, `mipmap-${density}`, 'ic_launcher.png'));
            
        await sharp(path.join(__dirname, '../icon.png'))
            .resize(size, size)
            .toFile(path.join(resDir, `mipmap-${density}`, 'ic_launcher_round.png'));
    }

    console.log('✅ Icon generation complete!');
}

generateIcons().catch(console.error); 