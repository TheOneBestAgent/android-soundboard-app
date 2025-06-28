const { app } = require('electron');

// This prevents Electron from re-initializing the app when a second instance is launched.
if (require('electron-squirrel-startup')) {
    app.quit();
}

// Require and start our tray application logic.
require('./scripts/tray-app.js');
