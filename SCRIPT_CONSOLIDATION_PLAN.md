# SCRIPT_CONSOLIDATION_PLAN.md
## Script Rationalization Planning
**Phase 1.2.2 - Comprehensive Server Rebuild Plan**  
**Date:** 2025-01-08  
**MCP Session:** documentStore endpoint integration

---

## 📊 CURRENT SCRIPT INVENTORY

### Root Package.json Scripts (14 total):
```json
{
  "setup": "node scripts/setup-environment.js",
  "build": "npm run setup && npm run build:android",
  "build:android": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean assembleDebug', {stdio:'inherit'});\"",
  "build:release": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean assembleRelease', {stdio:'inherit'});\"",
  "build:windows": "gradlew.bat clean assembleDebug",
  "build:unix": "./gradlew clean assembleDebug",
  "build:server-exe": "node scripts/build-server-exe.js",
  "build:server-safe": "node scripts/build-server-pkg-safe.js",
  "build:server-fixed": "node scripts/build-server-fixed.js",
  "build:server-comprehensive": "node scripts/build-comprehensive.js",
  "build:server-admin": "powershell -ExecutionPolicy Bypass -File scripts/build-admin.ps1",
  "build:server-admin-force": "powershell -ExecutionPolicy Bypass -File scripts/build-admin.ps1 -Force",
  "server": "cd server && npm start",
  "server:dev": "cd server && npm run dev",
  "server:exe": "node scripts/start-server-exe.js",
  "install:server": "cd server && npm install",
  "install:all": "npm install && npm run install:server",
  "clean": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean', {stdio:'inherit'});\"",
  "validate": "npm run setup && npm run build && echo 'Build validation successful!'",
  "test": "echo \"Error: no test specified\" && exit 1",
  "start": "node server/src/server.js",
  "dev": "nodemon server/src/server.js",
  "build:server": "node server/build.cjs",
  "build:server:esbuild": "node scripts/build-server-esbuild.cjs",
  "package:server": "npm run build:server:esbuild && cd server && pkg .",
  "start:tray": "electron .",
  "build:tray": "electron-builder"
}
```

### Server Package.json Scripts (3 total):
```json
{
  "start": "node dist/server.js",
  "build": "node build.cjs",
  "test": "echo \"Error: no test specified\" && exit 1"
}
```

---

## 🔍 SCRIPT ANALYSIS & CATEGORIZATION

### Android Build Scripts:
| Script | Purpose | Status | Action |
|--------|---------|---------|---------|
| `build:android` | Cross-platform Android build | ✅ Good | Keep & Improve |
| `build:release` | Android release build | ✅ Good | Keep |
| `build:windows` | Windows-specific Android build | ❌ Redundant | Remove |
| `build:unix` | Unix-specific Android build | ❌ Redundant | Remove |
| `clean` | Gradle clean | ✅ Good | Keep |

### Server Build Scripts (Root):
| Script | Purpose | Status | Action |
|--------|---------|---------|---------|
| `build:server` | Current server build | ✅ Good | Keep as primary |
| `build:server-exe` | Legacy server build | ❌ Redundant | Remove |
| `build:server-safe` | Legacy server build | ❌ Redundant | Remove |
| `build:server-fixed` | Legacy server build | ❌ Redundant | Remove |
| `build:server-comprehensive` | Legacy server build | ❌ Redundant | Remove |
| `build:server-admin` | PowerShell server build | ❌ Redundant | Remove |
| `build:server-admin-force` | PowerShell server build | ❌ Redundant | Remove |
| `build:server:esbuild` | ESBuild server build | ⚠️ Evaluate | Consolidate |
| `package:server` | Package server executable | ✅ Good | Keep & Improve |

### Server Runtime Scripts:
| Script | Purpose | Status | Action |
|--------|---------|---------|---------|
| `server` | Start server (via cd) | ✅ Good | Keep |
| `server:dev` | Development server | ❌ Missing | Add |
| `server:exe` | Start executable | ⚠️ Evaluate | Keep if needed |
| `start` | Direct server start | ❌ Redundant | Remove |
| `dev` | Direct dev server | ❌ Redundant | Remove |

### Desktop App Scripts:
| Script | Purpose | Status | Action |
|--------|---------|---------|---------|
| `start:tray` | Electron tray app | ⚠️ Evaluate | Keep if needed |
| `build:tray` | Build Electron app | ⚠️ Evaluate | Keep if needed |

### Utility Scripts:
| Script | Purpose | Status | Action |
|--------|---------|---------|---------|
| `setup` | Environment setup | ✅ Essential | Keep |
| `install:server` | Server dependencies | ✅ Good | Keep |
| `install:all` | All dependencies | ✅ Good | Keep |
| `validate` | Build validation | ✅ Good | Keep |
| `test` | Test runner | ❌ Not implemented | Implement or remove |

---

## 🎯 SCRIPT CONSOLIDATION STRATEGY

### Phase 1: Remove Redundant Scripts
**Remove from Root:**
- `build:windows` (use `build:android` with OS detection)
- `build:unix` (use `build:android` with OS detection)  
- `build:server-exe` (legacy)
- `build:server-safe` (legacy)
- `build:server-fixed` (legacy)
- `build:server-comprehensive` (legacy)
- `build:server-admin` (legacy)
- `build:server-admin-force` (legacy)
- `start` (redundant with `server`)
- `dev` (redundant with `server:dev`)

### Phase 2: Enhance Core Scripts
**Improved Root Scripts:**
```json
{
  "scripts": {
    // Environment & Setup
    "setup": "node scripts/setup-environment.js",
    "clean": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean', {stdio:'inherit'});\"",
    
    // Android Build
    "build:android": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean assembleDebug', {stdio:'inherit'});\"",
    "build:android:release": "node -e \"const os=require('os'); const {execSync}=require('child_process'); const wrapper = os.platform()==='win32'?'gradlew.bat':'./gradlew'; execSync(wrapper + ' clean assembleRelease', {stdio:'inherit'});\"",
    
    // Server Build & Package
    "build:server": "cd server && npm run build",
    "package:server": "cd server && npm run build && npm run package",
    
    // Server Runtime
    "server": "cd server && npm start",
    "server:dev": "cd server && npm run dev",
    
    // Combined Operations
    "build": "npm run setup && npm run build:android",
    "build:all": "npm run build:android && npm run build:server",
    "install:all": "npm install && cd server && npm install",
    "validate": "npm run setup && npm run build:all && echo 'Build validation successful!'",
    
    // Dependencies
    "install:server": "cd server && npm install"
  }
}
```

### Phase 3: Enhance Server Scripts
**Improved Server Scripts:**
```json
{
  "scripts": {
    "start": "node dist/server.js",
    "dev": "node --watch src/server.js",
    "build": "node build.cjs",
    "package": "pkg . --out-path=dist",
    "test": "node --test test/**/*.test.js"
  }
}
```

### Phase 4: Electron Script Evaluation
**Decision Matrix:**
- **Keep if:** Desktop tray app is part of product roadmap
- **Remove if:** Focusing only on server executable
- **Separate if:** Desktop app becomes independent project

---

## 🔧 SCRIPT IMPROVEMENT SPECIFICATIONS

### Enhanced Cross-Platform Android Build:
```javascript
// Improved build:android script
const os = require('os');
const { execSync } = require('child_process');
const path = require('path');

const platform = os.platform();
const wrapper = platform === 'win32' ? 'gradlew.bat' : './gradlew';

// Ensure wrapper exists
if (!require('fs').existsSync(wrapper)) {
  throw new Error(`Gradle wrapper not found: ${wrapper}`);
}

// Execute with proper error handling
try {
  execSync(`${wrapper} clean assembleDebug`, { 
    stdio: 'inherit',
    cwd: process.cwd()
  });
  console.log('✅ Android build completed successfully');
} catch (error) {
  console.error('❌ Android build failed:', error.message);
  process.exit(1);
}
```

### Enhanced Server Development Script:
```json
{
  "server:dev": "cd server && node --watch --enable-source-maps src/server.js"
}
```

### Enhanced Validation Script:
```json
{
  "validate": "npm run setup && npm run build:android && npm run build:server && npm run test && echo '✅ Full validation completed successfully'"
}
```

---

## 📊 BEFORE/AFTER COMPARISON

### Before Consolidation:
- **Total Scripts:** 26 (Root: 23, Server: 3)
- **Redundant Scripts:** 9 legacy server build scripts
- **Platform-Specific:** 2 unnecessary platform scripts
- **Missing Features:** No development server, no proper testing

### After Consolidation:
- **Total Scripts:** 15 (Root: 12, Server: 5)  
- **Redundant Scripts:** 0
- **Platform-Specific:** 1 smart cross-platform script
- **Enhanced Features:** Development server, testing, better validation

### Script Reduction:
- **Removed:** 11 redundant/legacy scripts
- **Enhanced:** 8 existing scripts with better functionality
- **Added:** 2 new scripts (dev server, testing)

---

## 🚨 RISK MITIGATION

### Backwards Compatibility:
- **Document removed scripts** in CHANGELOG
- **Provide migration guide** for any external CI/CD using old scripts
- **Keep git history** of removed scripts for reference

### Testing Strategy:
- **Test each script** on target platforms (Windows, macOS, Linux)
- **Verify build outputs** match previous results
- **Validate server executable** generation works correctly

### Rollback Plan:
- **Git tag** before script changes
- **Backup current package.json** files
- **Document exact commands** for rollback if needed

---

## 📋 IMPLEMENTATION CHECKLIST

### Phase 1: Cleanup
- [ ] Remove 9 legacy server build scripts
- [ ] Remove 2 redundant platform-specific scripts  
- [ ] Remove 2 redundant runtime scripts
- [ ] Update root package.json

### Phase 2: Enhancement
- [ ] Improve cross-platform Android build script
- [ ] Add development server script
- [ ] Enhance validation script
- [ ] Update server package.json

### Phase 3: Testing
- [ ] Test Android build on current platform
- [ ] Test server build and executable generation
- [ ] Test development workflow
- [ ] Validate all scripts work correctly

### Phase 4: Documentation
- [ ] Update README with new script usage
- [ ] Document removed scripts in CHANGELOG
- [ ] Update build documentation

---

## 📋 NEXT STEPS (Phase 1.3)
- Execute script consolidation plan
- Test all enhanced scripts
- Update documentation
- Prepare for Phase 2: Dependency Standardization

**Status:** ✅ PLAN COMPLETE - Ready for implementation 