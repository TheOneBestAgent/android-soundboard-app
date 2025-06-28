const esbuild = require('esbuild');
const path = require('path');

async function build() {
  try {
    await esbuild.build({
      entryPoints: [path.resolve(__dirname, '..', 'server', 'src', 'server.js')],
      bundle: true,
      platform: 'node',
      format: 'cjs',
      outfile: path.resolve(__dirname, '..', 'server', 'dist', 'server.js'),
      external: [
        'voicemeeter-connector',
        'koffi'
      ],
    });
    console.log('Server build successful');
  } catch (e) {
    console.error('Server build failed:', e);
    process.exit(1);
  }
}

build(); 