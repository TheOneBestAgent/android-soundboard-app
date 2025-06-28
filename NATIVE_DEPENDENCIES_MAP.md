# NATIVE_DEPENDENCIES_MAP.md
## Native Dependency Deep Dive Analysis
**Phase 1.1.3 - Comprehensive Server Rebuild Plan**  
**Date:** 2025-01-08  
**MCP Session:** contextWindow endpoint integration

---

## 🔍 NATIVE DEPENDENCY INVENTORY

### 1. Koffi (via @yume-chan/adb)
**Package:** `koffi` (indirect dependency)  
**Purpose:** Foreign Function Interface for ADB communication  
**Type:** Multi-platform native binaries

#### Platform Support Matrix:
| Platform | Architecture | Binary Location | Status |
|----------|-------------|----------------|---------|
| **macOS** | ARM64 | `koffi/build/koffi/darwin_arm64/koffi.node` | ✅ Available |
| **macOS** | x64 | `koffi/build/koffi/darwin_x64/koffi.node` | ✅ Available |
| **Windows** | x64 | `koffi/build/koffi/win32_x64/koffi.node` | ✅ Available |
| **Windows** | ARM64 | `koffi/build/koffi/win32_arm64/koffi.node` | ✅ Available |
| **Windows** | x86 | `koffi/build/koffi/win32_ia32/koffi.node` | ✅ Available |
| **Linux** | x64 | `koffi/build/koffi/linux_x64/koffi.node` | ✅ Available |
| **Linux** | ARM64 | `koffi/build/koffi/linux_arm64/koffi.node` | ✅ Available |
| **Linux** | ARMhf | `koffi/build/koffi/linux_armhf/koffi.node` | ✅ Available |

#### PKG Packaging Requirements:
```json
"assets": [
  "node_modules/koffi/build/koffi/**/*"
]
```

### 2. Voicemeeter-Connector
**Package:** `voicemeeter-connector`  
**Purpose:** Windows audio routing interface  
**Type:** Windows-only native module  
**Status:** ⚠️ INSTALLATION ISSUE

#### Current State:
- **Main Server:** NOT installed (optional dependency failed)
- **Nested Server:** v2.1.4 installed in `server/server/`
- **Platform:** Windows only (.dll dependencies)

#### Installation Analysis:
```bash
# Main server directory - voicemeeter-connector missing
server/node_modules/ - NOT FOUND

# Nested server directory - installed
server/server/node_modules/voicemeeter-connector@2.1.4 - FOUND
```

#### PKG Packaging Requirements:
```json
"assets": [
  "node_modules/voicemeeter-connector/**/*"
]
```

### 3. @yume-chan/adb Ecosystem
**Package:** `@yume-chan/adb@2.1.0`  
**Purpose:** Android Debug Bridge interface  
**Type:** Pure JavaScript with Koffi native bindings

#### Dependency Tree:
```
@yume-chan/adb@2.1.0
├── @yume-chan/async@4.1.3
├── @yume-chan/event@2.0.0
├── @yume-chan/no-data-view@2.0.0
├── @yume-chan/stream-extra@2.1.0
└── @yume-chan/struct@2.0.1
```

#### Missing Dependency:
- **@yume-chan/adb-server-node-tcp@^2.1.0** - UNMET DEPENDENCY

---

## 🚨 CRITICAL ISSUES IDENTIFIED

### 1. Voicemeeter-Connector Installation Failure
**Problem:** Optional dependency failed to install in main server  
**Impact:** Windows audio routing unavailable  
**Root Cause:** Platform-specific installation issues

### 2. Missing ADB TCP Server
**Problem:** `@yume-chan/adb-server-node-tcp@^2.1.0` unmet dependency  
**Impact:** ADB TCP server functionality broken  
**Root Cause:** Dependency resolution failure

### 3. Nested Package Structure Confusion
**Problem:** `server/server/` contains duplicate/conflicting dependencies  
**Impact:** Version conflicts and build complexity  
**Root Cause:** Improper package structure

---

## 🎯 NATIVE MODULE PACKAGING STRATEGY

### PKG Asset Configuration:
```json
{
  "pkg": {
    "assets": [
      "node_modules/koffi/build/koffi/**/*",
      "node_modules/voicemeeter-connector/**/*",
      "node_modules/@yume-chan/**/*",
      "audio/**/*"
    ],
    "targets": [
      "node18-macos-arm64",
      "node18-macos-x64", 
      "node18-win-x64",
      "node18-linux-x64"
    ]
  }
}
```

### Platform-Specific Loading Strategy:
```javascript
// Conditional native module loading
let voicemeeterConnector = null;
if (process.platform === 'win32') {
  try {
    voicemeeterConnector = require('voicemeeter-connector');
  } catch (error) {
    console.warn('Voicemeeter-connector not available on this system');
  }
}
```

### Runtime Detection:
```javascript
// Platform and architecture detection
const platform = process.platform; // 'win32', 'darwin', 'linux'
const arch = process.arch;         // 'x64', 'arm64', 'ia32'
const koffiPath = `koffi/build/koffi/${platform}_${arch}/koffi.node`;
```

---

## 🔧 RESOLUTION PLAN

### Immediate Actions:
1. **FIX** @yume-chan/adb-server-node-tcp missing dependency
2. **RESOLVE** voicemeeter-connector installation issues
3. **REMOVE** server/server/ nested structure
4. **CONSOLIDATE** native dependencies in main server/package.json

### Installation Commands:
```bash
# Fix missing ADB TCP server
cd server && npm install @yume-chan/adb-server-node-tcp@^2.1.0

# Force install voicemeeter-connector (Windows only)
cd server && npm install voicemeeter-connector@^2.1.4 --optional

# Remove nested structure
rm -rf server/server/
```

### PKG Build Configuration:
```javascript
// Enhanced build.cjs configuration
const pkgConfig = {
  assets: [
    'node_modules/koffi/build/koffi/**/*',
    'node_modules/voicemeeter-connector/**/*', 
    'node_modules/@yume-chan/**/*',
    'audio/**/*'
  ],
  targets: ['node18-macos-arm64'], // Current platform
  outputPath: 'dist'
};
```

---

## 📊 PLATFORM COMPATIBILITY MATRIX

| Feature | Windows | macOS | Linux | Status |
|---------|---------|-------|-------|--------|
| **ADB Communication** | ✅ Koffi | ✅ Koffi | ✅ Koffi | Ready |
| **Audio Routing** | ✅ Voicemeeter | ❌ System | ❌ System | Partial |
| **Network Discovery** | ✅ mDNS | ✅ mDNS | ✅ mDNS | Ready |
| **File Operations** | ✅ fs-extra | ✅ fs-extra | ✅ fs-extra | Ready |
| **PKG Packaging** | ✅ Supported | ✅ Supported | ✅ Supported | Ready |

---

## 📋 NEXT STEPS (Phase 1.1.4)
- Create dependency conflict resolution strategy
- Plan version standardization approach
- Design installation verification process
- Prepare for Phase 1.2 package.json consolidation

**Status:** ✅ COMPLETE - Critical native dependency issues identified and mapped 