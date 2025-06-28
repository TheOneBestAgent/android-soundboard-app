# DEPENDENCY_RESOLUTION_STRATEGY.md
## Dependency Conflict Resolution Strategy
**Phase 1.1.4 - Comprehensive Server Rebuild Plan**  
**Date:** 2025-01-08  
**MCP Session:** memoryManager endpoint integration

---

## 🎯 CONFLICT RESOLUTION MATRIX

### Version Conflicts Identified:
| Package | Root Version | Server Version | Nested Version | Target Resolution |
|---------|-------------|----------------|----------------|-------------------|
| `socket.io` | 4.8.1 (client) | 4.7.5 (server) | - | ✅ 4.8.1 (standardize) |
| `dotenv` | - | 16.4.5 | 16.5.0 | ✅ 16.5.0 (latest) |
| `voicemeeter-connector` | - | 2.0.1 (optional) | 2.1.4 | ✅ 2.1.4 (latest) |
| `esbuild` | 0.25.5 | 0.25.5 | - | ✅ 0.25.5 (keep) |
| `pkg` | 5.8.1 | 5.8.1 | - | ✅ 5.8.1 (keep) |

### Dependency Placement Strategy:
| Package | Current Location | Target Location | Justification |
|---------|------------------|-----------------|---------------|
| `node-fetch` | Root (prod) | Server (prod) | Server-specific HTTP client |
| `socket.io-client` | Root (prod) | Server (prod) | Server needs client connections |
| `electron*` | Root (dev) | Remove/Evaluate | Focus on server executable |
| `@babel/*` | Server (dev) | Remove | Use ESBuild only |

---

## 🔧 RESOLUTION EXECUTION PLAN

### Phase 1: Immediate Cleanup
```bash
# 1. Remove nested server structure
rm -rf server/server/

# 2. Fix missing ADB TCP dependency
cd server && npm install @yume-chan/adb-server-node-tcp@^2.1.0

# 3. Update Socket.io versions for compatibility
cd server && npm install socket.io@^4.8.1
npm install socket.io-client@^4.8.1 --save-dev
```

### Phase 2: Dependency Migration
```bash
# Move server-specific dependencies from root to server
npm uninstall node-fetch socket.io-client
cd server && npm install node-fetch@^2.6.7 socket.io-client@^4.8.1

# Update voicemeeter-connector to latest
cd server && npm install voicemeeter-connector@^2.1.4 --optional
```

### Phase 3: Build Tool Consolidation
```bash
# Remove Babel dependencies (use ESBuild only)
cd server && npm uninstall @babel/cli @babel/core @babel/preset-env

# Keep ESBuild and PKG for executable generation
# (Already present and correct versions)
```

### Phase 4: Version Standardization
```bash
# Update dotenv to latest version
cd server && npm install dotenv@^16.5.0

# Verify all dependencies are at target versions
cd server && npm list --depth=0
```

---

## 🏗️ PACKAGE.JSON RESTRUCTURING

### Root package.json (Simplified):
```json
{
  "name": "audiodeck-connect-server",
  "version": "8.0.0",
  "type": "module",
  "scripts": {
    "setup": "node scripts/setup-environment.js",
    "build": "npm run setup && npm run build:android",
    "build:android": "...",
    "build:server": "cd server && npm run build",
    "server": "cd server && npm start",
    "install:all": "npm install && cd server && npm install"
  },
  "devDependencies": {
    "cross-env": "^7.0.3",
    "esbuild": "^0.25.5",
    "pkg": "^5.8.1"
  }
}
```

### Server package.json (Enhanced):
```json
{
  "name": "soundboard-server",
  "version": "9.0.0",
  "type": "module",
  "main": "dist/server.js",
  "dependencies": {
    "@homebridge/ciao": "^1.1.7",
    "@yume-chan/adb": "^2.1.0",
    "@yume-chan/adb-server-node-tcp": "^2.1.0",
    "cors": "^2.8.5",
    "dotenv": "^16.5.0",
    "express": "^4.19.2",
    "fs-extra": "^11.3.0",
    "node-fetch": "^2.6.7",
    "socket.io": "^4.8.1",
    "socket.io-client": "^4.8.1"
  },
  "optionalDependencies": {
    "voicemeeter-connector": "^2.1.4"
  },
  "devDependencies": {
    "esbuild": "^0.25.5",
    "pkg": "^5.8.1"
  }
}
```

---

## 🚨 DUAL PACKAGE HAZARD MITIGATION

### ES Module vs CommonJS Strategy:
1. **Source Code:** Pure ES modules (import/export)
2. **Build Output:** CommonJS for PKG compatibility
3. **Runtime:** Node.js 18+ with native ES module support

### ESBuild Configuration:
```javascript
// Enhanced build configuration
{
  entryPoints: ['src/server.js'],
  bundle: true,
  platform: 'node',
  target: 'node18',
  format: 'cjs',           // PKG requires CommonJS
  external: [              // Don't bundle native modules
    'voicemeeter-connector',
    'koffi'
  ],
  outfile: 'dist/server.js'
}
```

### Native Module Handling:
```javascript
// Conditional loading pattern
const loadNativeModule = (moduleName, optional = false) => {
  try {
    return require(moduleName);
  } catch (error) {
    if (optional) {
      console.warn(`Optional module ${moduleName} not available`);
      return null;
    }
    throw error;
  }
};

// Usage
const voicemeeter = loadNativeModule('voicemeeter-connector', true);
```

---

## 📊 CONFLICT RESOLUTION VALIDATION

### Pre-Resolution State:
- ❌ 3 version conflicts identified
- ❌ 2 missing dependencies
- ❌ Nested package structure confusion
- ❌ Build tool redundancy (Babel + ESBuild)

### Post-Resolution Target State:
- ✅ All dependencies at consistent versions
- ✅ No missing dependencies
- ✅ Clean single-level package structure
- ✅ Single build tool (ESBuild only)
- ✅ Proper native module packaging

### Validation Commands:
```bash
# Check for version conflicts
cd server && npm ls 2>&1 | grep -E "(WARN|ERR)"

# Verify all dependencies installed
cd server && npm list --depth=0

# Test native module loading
cd server && node -e "console.log(require('@yume-chan/adb'))"

# Test optional dependency handling
cd server && node -e "try{require('voicemeeter-connector')}catch(e){console.log('Optional OK')}"
```

---

## 🎯 ROLLBACK STRATEGY

### Git Checkpoint:
```bash
# Create checkpoint before resolution
git add -A
git commit -m "Pre-dependency-resolution checkpoint"
git tag pre-dependency-resolution
```

### Rollback Commands:
```bash
# If resolution fails, rollback to checkpoint
git reset --hard pre-dependency-resolution
git clean -fd
```

### Validation Failures:
- If native modules fail to load → Check PKG asset configuration
- If version conflicts persist → Manual dependency tree resolution
- If build fails → Verify ESBuild configuration

---

## 📋 SUCCESS CRITERIA

### Resolution Complete When:
- [ ] All version conflicts resolved
- [ ] No missing dependencies in npm list
- [ ] Clean build with ESBuild succeeds
- [ ] PKG executable generation works
- [ ] Native modules load correctly
- [ ] Server starts without errors

### Performance Targets:
- **Build Time:** < 30 seconds
- **Executable Size:** < 100MB
- **Startup Time:** < 3 seconds
- **Memory Usage:** < 50MB idle

---

## 📋 NEXT STEPS (Phase 1.2)
- Execute dependency resolution plan
- Validate all conflicts are resolved
- Test native module loading
- Prepare for package.json consolidation analysis

**Status:** ✅ STRATEGY COMPLETE - Ready for execution in Phase 1.2 