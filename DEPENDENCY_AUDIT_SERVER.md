# DEPENDENCY_AUDIT_SERVER.md
## Server Package.json Dependency Analysis
**Phase 1.1.2 - Comprehensive Server Rebuild Plan**  
**Date:** 2025-01-08  
**MCP Session:** documentStore endpoint integration

---

## 📊 SERVER DEPENDENCY CLASSIFICATION

### Main Server Dependencies (8 total)
| Package | Version | Type | Purpose | ES Module | Keep/Update |
|---------|---------|------|---------|-----------|-------------|
| `@homebridge/ciao` | ^1.1.7 | Network | mDNS service discovery | ✅ ESM | ✅ KEEP |
| `@yume-chan/adb` | ^2.1.0 | Native | Android Debug Bridge | ✅ ESM | ✅ KEEP |
| `@yume-chan/adb-server-node-tcp` | ^2.1.0 | Native | ADB TCP server | ✅ ESM | ✅ KEEP |
| `cors` | ^2.8.5 | Middleware | Cross-origin requests | ⚠️ CJS | ✅ KEEP |
| `dotenv` | ^16.4.5 | Config | Environment variables | ✅ ESM | ✅ KEEP |
| `express` | ^4.19.2 | Framework | HTTP server framework | ⚠️ CJS | ✅ KEEP |
| `fs-extra` | ^11.3.0 | Utility | Enhanced file system | ✅ ESM | ✅ KEEP |
| `socket.io` | ^4.7.5 | Network | WebSocket server | ⚠️ CJS | ✅ KEEP |

### Optional Dependencies (1 total)
| Package | Version | Type | Purpose | Platform | Keep/Update |
|---------|---------|------|---------|----------|-------------|
| `voicemeeter-connector` | ^2.0.1 | Native | Audio routing (Windows) | Windows only | ✅ KEEP |

### Development Dependencies (5 total)
| Package | Version | Type | Purpose | Keep/Update |
|---------|---------|------|---------|-------------|
| `@babel/cli` | ^7.27.2 | Build | Babel command line | ❌ REMOVE (Use ESBuild) |
| `@babel/core` | ^7.27.7 | Build | Babel transpiler | ❌ REMOVE (Use ESBuild) |
| `@babel/preset-env` | ^7.27.2 | Build | Babel presets | ❌ REMOVE (Use ESBuild) |
| `esbuild` | ^0.25.5 | Build | Fast bundler | ✅ KEEP |
| `pkg` | ^5.8.1 | Build | Executable packager | ✅ KEEP |

### Nested Server Dependencies (3 total)
| Package | Version | Source | Purpose | Action |
|---------|---------|--------|---------|--------|
| `dotenv` | ^16.5.0 | server/server/ | Environment config | ❌ REMOVE (Duplicate) |
| `soundboard-server` | file:.. | server/server/ | Local package ref | ❌ REMOVE (Unnecessary) |
| `voicemeeter-connector` | ^2.1.4 | server/server/ | Audio routing | ⚠️ VERSION CONFLICT |

---

## 🔍 CRITICAL ANALYSIS

### Native Dependencies Deep Dive:
1. **@yume-chan/adb** (Koffi-based)
   - Uses Koffi for native ADB communication
   - ARM64 compatible
   - Requires proper packaging for PKG
   
2. **voicemeeter-connector** (N-API)
   - Windows-specific audio routing
   - Optional dependency (good practice)
   - Version conflict: 2.0.1 vs 2.1.4

### ES Module vs CommonJS Issues:
- **ESM Native:** `@homebridge/ciao`, `@yume-chan/*`, `dotenv`, `fs-extra`
- **CJS Legacy:** `cors`, `express`, `socket.io` (but have ESM support)
- **Build Target:** Need CJS output for PKG compatibility

### Version Conflicts Identified:
1. **dotenv:** 16.4.5 (server) vs 16.5.0 (nested)
2. **voicemeeter-connector:** 2.0.1 (optional) vs 2.1.4 (nested)
3. **Socket.io mismatch:** Server 4.7.5 vs Root client 4.8.1

---

## 🎯 DEPENDENCY RESOLUTION STRATEGY

### Immediate Cleanup:
1. **REMOVE** Babel dependencies (use ESBuild only)
2. **REMOVE** nested server/server/ directory entirely
3. **ADD** missing dependencies from root (`node-fetch`, `socket.io-client`)
4. **STANDARDIZE** voicemeeter-connector to latest version
5. **ALIGN** Socket.io versions between client/server

### Native Module Packaging:
- Configure PKG assets for voicemeeter-connector binaries
- Ensure Koffi native modules are included
- Test cross-platform native module loading

### Module System Strategy:
- **Source:** Pure ES modules with import/export
- **Build Output:** CommonJS for PKG compatibility
- **Runtime:** Node.js 18+ with ES module support

---

## 📋 MISSING DEPENDENCIES

### From Root Analysis:
- `node-fetch` (needed for HTTP client operations)
- `socket.io-client` (if server needs client connections)

### Potential Additions:
- Better error handling and logging libraries
- Performance monitoring utilities
- Configuration validation

---

## 🚨 CRITICAL ISSUES

1. **Nested Package Structure:** server/server/ creates confusion
2. **Version Fragmentation:** Multiple versions of same packages
3. **Build Tool Redundancy:** Babel + ESBuild (choose one)
4. **Native Module Packaging:** Need proper PKG configuration

---

## 📋 NEXT STEPS (Phase 1.1.3)
- Deep dive into native dependency requirements
- Map platform-specific packaging needs
- Create native module loading strategy
- Plan PKG asset configuration

**Status:** ✅ COMPLETE - Ready for Phase 1.1.3 