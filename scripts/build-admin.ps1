# Soundboard Server - Admin Build Script
# Automatically requests administrator privileges and builds the executable

param(
    [switch]$Force,
    [string]$BuildType = "pkg"
)

# Check if running as administrator
function Test-Admin {
    $currentUser = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
    return $currentUser.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# Request administrator privileges
function Request-AdminPrivileges {
    if (-not (Test-Admin)) {
        Write-Host "🔐 Requesting Administrator Privileges..." -ForegroundColor Yellow
        
        $arguments = "-ExecutionPolicy Bypass -File `"$($MyInvocation.MyCommand.Path)`""
        if ($Force) { $arguments += " -Force" }
        if ($BuildType) { $arguments += " -BuildType $BuildType" }
        
        try {
            Start-Process powershell -Verb RunAs -ArgumentList $arguments -Wait
            exit 0
        } catch {
            Write-Host "❌ Failed to get administrator privileges: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    }
}

# Enable Developer Mode (helps with symbolic links)
function Enable-DeveloperMode {
    Write-Host "🔧 Enabling Developer Mode for symbolic link support..." -ForegroundColor Cyan
    
    try {
        $regPath = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock"
        if (-not (Test-Path $regPath)) {
            New-Item -Path $regPath -Force | Out-Null
        }
        
        Set-ItemProperty -Path $regPath -Name "AllowDevelopmentWithoutDevLicense" -Value 1 -Type DWord
        Set-ItemProperty -Path $regPath -Name "AllowAllTrustedApps" -Value 1 -Type DWord
        
        Write-Host "✅ Developer Mode enabled" -ForegroundColor Green
    } catch {
        Write-Host "⚠️ Could not enable Developer Mode: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Clean build environment
function Clean-BuildEnvironment {
    Write-Host "🧹 Cleaning build environment..." -ForegroundColor Cyan
    
    $pathsToClean = @(
        "dist",
        "node_modules\.cache",
        "$env:APPDATA\electron-builder",
        "$env:LOCALAPPDATA\electron-builder"
    )
    
    foreach ($path in $pathsToClean) {
        if (Test-Path $path) {
            try {
                Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue
                Write-Host "  ✅ Cleaned: $path" -ForegroundColor Green
            } catch {
                Write-Host "  ⚠️ Could not clean: $path" -ForegroundColor Yellow
            }
        }
    }
}

# Build with PKG
function Build-WithPKG {
    Write-Host "🔨 Building with PKG..." -ForegroundColor Cyan
    
    try {
        & node scripts/build-server-pkg-safe.js
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ PKG build completed successfully!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ PKG build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ PKG build error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Build with Electron
function Build-WithElectron {
    Write-Host "🔨 Building with Electron..." -ForegroundColor Cyan
    
    try {
        & node scripts/build-server-exe-safe.js
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Electron build completed successfully!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Electron build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ Electron build error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Main execution
function Main {
    Write-Host "🎵 Soundboard Server - Admin Build Tool" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    
    # Request admin privileges if needed
    Request-AdminPrivileges
    
    Write-Host "✅ Running with Administrator privileges" -ForegroundColor Green
    
    # Enable developer mode
    Enable-DeveloperMode
    
    # Clean build environment
    if ($Force) {
        Clean-BuildEnvironment
    }
    
    # Change to project directory
    $projectRoot = Split-Path $PSScriptRoot -Parent
    Set-Location $projectRoot
    Write-Host "📁 Working directory: $projectRoot" -ForegroundColor Cyan
    
    # Build based on type
    $success = $false
    
    switch ($BuildType.ToLower()) {
        "pkg" {
            $success = Build-WithPKG
        }
        "electron" {
            $success = Build-WithElectron
        }
        default {
            Write-Host "🔄 Trying PKG first, then Electron if needed..." -ForegroundColor Cyan
            $success = Build-WithPKG
            if (-not $success) {
                Write-Host "🔄 PKG failed, trying Electron..." -ForegroundColor Yellow
                $success = Build-WithElectron
            }
        }
    }
    
    if ($success) {
        Write-Host "`n🎉 Build completed successfully!" -ForegroundColor Green
        Write-Host "📦 Check the 'dist' folder for output files" -ForegroundColor Cyan
        
        # Show output files
        if (Test-Path "dist") {
            Write-Host "`n📋 Build Output:" -ForegroundColor Cyan
            Get-ChildItem "dist" | ForEach-Object {
                $size = if ($_.PSIsContainer) { "(directory)" } else { "($([math]::Round($_.Length / 1MB, 2)) MB)" }
                Write-Host "   * $($_.Name) $size" -ForegroundColor White
            }
        }
    } else {
        Write-Host "`n❌ All build attempts failed!" -ForegroundColor Red
        Write-Host "💡 Try running: npm run build:service" -ForegroundColor Yellow
    }
    
    Write-Host "`nPress any key to exit..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

# Run main function
Main 