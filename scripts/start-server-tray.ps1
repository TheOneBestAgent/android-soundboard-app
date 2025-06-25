# Soundboard Server Tray PowerShell Script
param(
    [switch]$Silent,
    [switch]$NoTray,
    [int]$Port = 3001
)

# Set execution policy for current session if needed
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process -Force

# Colors for output
function Write-ColoredOutput {
    param($Message, $Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

function Show-Banner {
    Clear-Host
    Write-ColoredOutput "=====================================================" "Cyan"
    Write-ColoredOutput "            🎵 Soundboard Server Tray             " "Yellow"
    Write-ColoredOutput "=====================================================" "Cyan"
    Write-ColoredOutput ""
}

function Test-Prerequisites {
    Write-ColoredOutput "🔍 Checking prerequisites..." "Yellow"
    
    # Check Node.js
    try {
        $nodeVersion = node --version 2>$null
        if ($nodeVersion) {
            Write-ColoredOutput "✅ Node.js found: $nodeVersion" "Green"
        } else {
            throw "Node.js not found"
        }
    } catch {
        Write-ColoredOutput "❌ Node.js is not installed or not in PATH" "Red"
        Write-ColoredOutput "   Please install Node.js from https://nodejs.org/" "Yellow"
        return $false
    }
    
    # Check if server directory exists
    $serverDir = Join-Path $PSScriptRoot "..\server"
    if (Test-Path $serverDir) {
        Write-ColoredOutput "✅ Server directory found" "Green"
    } else {
        Write-ColoredOutput "❌ Server directory not found at: $serverDir" "Red"
        return $false
    }
    
    return $true
}

function Install-Dependencies {
    Write-ColoredOutput "📦 Installing dependencies..." "Yellow"
    
    $projectRoot = Split-Path $PSScriptRoot -Parent
    $nodeModules = Join-Path $projectRoot "node_modules"
    
    if (-not (Test-Path $nodeModules)) {
        Write-ColoredOutput "Installing project dependencies..." "Cyan"
        Push-Location $projectRoot
        try {
            npm install | Out-Host
            if ($LASTEXITCODE -ne 0) {
                throw "npm install failed"
            }
            Write-ColoredOutput "✅ Dependencies installed successfully" "Green"
        } catch {
            Write-ColoredOutput "❌ Failed to install dependencies: $_" "Red"
            return $false
        } finally {
            Pop-Location
        }
    } else {
        Write-ColoredOutput "✅ Dependencies already installed" "Green"
    }
    
    return $true
}

function Start-ServerTray {
    Write-ColoredOutput "🚀 Starting Soundboard Server Tray..." "Yellow"
    Write-ColoredOutput "   Port: $Port" "Cyan"
    Write-ColoredOutput "   Tray: $(if ($NoTray) { 'Disabled' } else { 'Enabled' })" "Cyan"
    Write-ColoredOutput ""
    
    if (-not $NoTray) {
        Write-ColoredOutput "💡 Look for the Soundboard icon in your system tray" "Yellow"
        Write-ColoredOutput "   Right-click the tray icon for server controls" "Yellow"
        Write-ColoredOutput "   The server will run in the background" "Yellow"
        Write-ColoredOutput ""
    }
    
    $trayScript = Join-Path $PSScriptRoot "server-tray.js"
    
    if (Test-Path $trayScript) {
        $env:PORT = $Port
        if ($NoTray) {
            $env:NO_TRAY = "true"
        }
        
        try {
            if ($Silent) {
                Start-Process -FilePath "node" -ArgumentList $trayScript -WindowStyle Hidden
                Write-ColoredOutput "✅ Server started in background" "Green"
                Write-ColoredOutput "   Check system tray for the Soundboard icon" "Cyan"
            } else {
                node $trayScript
            }
        } catch {
            Write-ColoredOutput "❌ Failed to start server tray: $_" "Red"
            return $false
        }
    } else {
        Write-ColoredOutput "❌ Tray script not found: $trayScript" "Red"
        return $false
    }
    
    return $true
}

function Show-Help {
    Write-ColoredOutput ""
    Write-ColoredOutput "📋 Usage:" "Yellow"
    Write-ColoredOutput "   .\start-server-tray.ps1              # Start with tray"
    Write-ColoredOutput "   .\start-server-tray.ps1 -Silent      # Start in background"
    Write-ColoredOutput "   .\start-server-tray.ps1 -NoTray      # Start without tray"
    Write-ColoredOutput "   .\start-server-tray.ps1 -Port 8080   # Use custom port"
    Write-ColoredOutput ""
    Write-ColoredOutput "🎮 Server Controls:" "Yellow"
    Write-ColoredOutput "   • Right-click tray icon for options"
    Write-ColoredOutput "   • Ctrl+C to stop (when running in console)"
    Write-ColoredOutput "   • Check logs in system temp directory"
    Write-ColoredOutput ""
}

# Main execution
try {
    if (-not $Silent) {
        Show-Banner
    }
    
    if (-not (Test-Prerequisites)) {
        if (-not $Silent) {
            Write-ColoredOutput ""
            Write-ColoredOutput "❌ Prerequisites check failed" "Red"
            Read-Host "Press Enter to exit"
        }
        exit 1
    }
    
    if (-not (Install-Dependencies)) {
        if (-not $Silent) {
            Write-ColoredOutput ""
            Write-ColoredOutput "❌ Dependency installation failed" "Red"
            Read-Host "Press Enter to exit"
        }
        exit 1
    }
    
    if (-not (Start-ServerTray)) {
        if (-not $Silent) {
            Write-ColoredOutput ""
            Write-ColoredOutput "❌ Failed to start server tray" "Red"
            Read-Host "Press Enter to exit"
        }
        exit 1
    }
    
    if (-not $Silent -and -not $NoTray) {
        Write-ColoredOutput ""
        Write-ColoredOutput "✅ Soundboard Server Tray started successfully!" "Green"
        Show-Help
    }
    
} catch {
    Write-ColoredOutput "❌ Unexpected error: $_" "Red"
    if (-not $Silent) {
        Read-Host "Press Enter to exit"
    }
    exit 1
}