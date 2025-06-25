@echo off
title Soundboard Server Tray

echo.
echo ===================================================
echo            Soundboard Server Tray
echo ===================================================
echo.
echo Starting server with system tray functionality...
echo.

REM Check if Node.js is installed
node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed or not in PATH
    echo Please install Node.js from https://nodejs.org/
    echo.
    pause
    exit /b 1
)

REM Check if server directory exists
if not exist "%~dp0..\server" (
    echo ERROR: Server directory not found
    echo Make sure you're running this from the correct location
    echo.
    pause
    exit /b 1
)

REM Install dependencies if needed
if not exist "%~dp0..\node_modules" (
    echo Installing dependencies...
    cd /d "%~dp0.."
    npm install
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies
        pause
        exit /b 1
    )
)

REM Start the tray application
echo Starting Soundboard Server Tray...
echo.
echo The server will run in the background.
echo Look for the Soundboard icon in your system tray.
echo Right-click the tray icon for server controls.
echo.

cd /d "%~dp0"
node server-tray.js

if errorlevel 1 (
    echo.
    echo ERROR: Failed to start the server tray
    echo Check the logs for more information
    pause
)

echo.
echo Server tray has stopped.
pause