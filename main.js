
const { app, BrowserWindow, Tray, Menu, nativeImage, dialog, shell, ipcMain } = require('electron');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');
const os = require('os');

// Import our tray application
const SoundboardServerTray = require('./scripts/server-tray.js');

// Override app.whenReady to prevent duplicate initialization
const originalWhenReady = app.whenReady;
app.whenReady = () => {
    return originalWhenReady.call(app);
};

// Initialize the server tray
new SoundboardServerTray();
