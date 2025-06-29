# AudioDeck Connect - API Specification
## REST API and WebSocket Documentation
**Version:** 9.0  
**Base URL:** `http://localhost:3001`  
**WebSocket:** `ws://localhost:3001`

---

## 🌐 **REST API ENDPOINTS**

### **Health & Status**

#### `GET /health`
**Description:** Server health check endpoint  
**Authentication:** None required

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2025-01-08T23:30:00Z",
  "version": "9.0.0",
  "uptime": 3600,
  "services": {
    "audio": "operational",
    "devices": "operational",
    "network": "operational"
  }
}
```

#### `GET /api/status`
**Description:** Detailed system status  
**Authentication:** Required

**Response:**
```json
{
  "server": {
    "version": "9.0.0",
    "nodeVersion": "18.20.4",
    "platform": "darwin",
    "architecture": "arm64",
    "uptime": 3600,
    "memory": {
      "used": 52428800,
      "free": 147857408,
      "total": 200286208
    }
  },
  "services": {
    "audioPlayer": "ready",
    "adbManager": "connected",
    "usbManager": "monitoring",
    "networkDiscovery": "active"
  }
}
```

---

## 🎵 **AUDIO API**

### **Audio Management**

#### `GET /api/audio/list`
**Description:** Get list of available audio files  
**Authentication:** Required

**Response:**
```json
{
  "files": [
    {
      "name": "airhorn.mp3",
      "size": 62259,
      "format": "mp3",
      "duration": 3.2,
      "path": "/audio/airhorn.mp3"
    },
    {
      "name": "applause.mp3", 
      "size": 58127,
      "format": "mp3",
      "duration": 5.1,
      "path": "/audio/applause.mp3"
    }
  ],
  "totalFiles": 2,
  "totalSize": 120386
}
```

#### `POST /api/audio/play`
**Description:** Play an audio file  
**Authentication:** Required

**Request Body:**
```json
{
  "file": "airhorn.mp3",
  "volume": 0.8
}
```

**Response:**
```json
{
  "success": true,
  "message": "Audio playback started",
  "file": "airhorn.mp3",
  "volume": 0.8,
  "platform": "darwin",
  "method": "afplay"
}
```

#### `POST /api/audio/stop`
**Description:** Stop current audio playback  
**Authentication:** Required

**Response:**
```json
{
  "success": true,
  "message": "Audio playback stopped"
}
```

#### `GET /api/audio/player/status`
**Description:** Get audio player status  
**Authentication:** Required

**Response:**
```json
{
  "platform": "darwin",
  "currentPlaying": null,
  "supportedFormats": [".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac"],
  "volume": 1.0,
  "ready": true
}
```

#### `GET /api/audio/formats`
**Description:** Get supported audio formats  
**Authentication:** Required

**Response:**
```json
{
  "supportedFormats": [".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac"],
  "platform": "darwin",
  "nativeSupport": true
}
```

### **Audio Upload**

#### `POST /api/audio/upload`
**Description:** Upload audio file  
**Authentication:** Required  
**Content-Type:** `multipart/form-data`

**Request:**
```
POST /api/audio/upload
Content-Type: multipart/form-data

file: [audio file]
```

**Response:**
```json
{
  "success": true,
  "message": "File uploaded successfully",
  "file": {
    "name": "new-sound.mp3",
    "size": 156789,
    "format": "mp3",
    "path": "/audio/new-sound.mp3"
  }
}
```

### **VoiceMeeter Integration (Windows Only)**

#### `GET /api/audio/voicemeeter/status`
**Description:** Get VoiceMeeter status  
**Authentication:** Required  
**Platform:** Windows only

**Response:**
```json
{
  "available": true,
  "connected": true,
  "version": "3.0.2.8",
  "type": "VoiceMeeter Banana"
}
```

---

## 📱 **DEVICE API**

### **Device Management**

#### `GET /api/devices`
**Description:** Get all connected devices  
**Authentication:** Required

**Response:**
```json
{
  "adb": {
    "connected": true,
    "devices": [
      {
        "serial": "ABCD1234567890",
        "state": "device",
        "product": "samsung_sm_g991b",
        "model": "SM-G991B",
        "device": "o1s"
      }
    ]
  },
  "usb": {
    "monitoring": true,
    "deviceCount": 3,
    "devices": [
      {
        "name": "Android Device",
        "vendor": "Samsung",
        "productId": "6860",
        "vendorId": "04e8"
      }
    ]
  }
}
```

### **ADB Management**

#### `GET /api/devices/adb/status`
**Description:** Get ADB manager status  
**Authentication:** Required

**Response:**
```json
{
  "initialized": true,
  "connected": true,
  "tracking": true,
  "deviceCount": 1,
  "serverVersion": "1.0.41"
}
```

#### `GET /api/devices/adb/devices`
**Description:** Get connected ADB devices  
**Authentication:** Required

**Response:**
```json
{
  "devices": [
    {
      "serial": "ABCD1234567890",
      "state": "device",
      "product": "samsung_sm_g991b",
      "model": "SM-G991B",
      "device": "o1s",
      "transportId": 1
    }
  ],
  "count": 1
}
```

### **USB Device Detection**

#### `GET /api/devices/usb`
**Description:** Get USB devices  
**Authentication:** Required

**Response:**
```json
{
  "devices": [
    {
      "name": "Android Device",
      "vendor": "Samsung Electronics Co., Ltd.",
      "productId": "6860",
      "vendorId": "04e8",
      "platform": "darwin"
    }
  ],
  "monitoring": true,
  "platform": "darwin"
}
```

---

## 🌐 **NETWORK API**

### **Network Discovery**

#### `GET /api/network/discovery`
**Description:** Get network discovery status  
**Authentication:** Required

**Response:**
```json
{
  "active": true,
  "peers": [
    {
      "id": "abc123",
      "address": "192.168.1.100",
      "isLeader": false,
      "lastSeen": "2025-01-08T23:30:00Z"
    }
  ],
  "isLeader": true,
  "port": 41234
}
```

#### `GET /api/network/mdns/status`
**Description:** Get mDNS service status  
**Authentication:** Required

**Response:**
```json
{
  "active": true,
  "serviceName": "AudioDeck Connect",
  "serviceType": "_audiodeck._tcp",
  "port": 3001,
  "platform": "darwin"
}
```

---

## 🖥️ **SYSTEM API**

### **System Information**

#### `GET /api/system/info`
**Description:** Get system information  
**Authentication:** Required

**Response:**
```json
{
  "platform": "darwin",
  "architecture": "arm64",
  "nodeVersion": "18.20.4",
  "serverVersion": "9.0.0",
  "cpu": {
    "model": "Apple M2",
    "cores": 8,
    "speed": 3200
  },
  "memory": {
    "total": 17179869184,
    "free": 8589934592,
    "used": 8589934592
  }
}
```

#### `GET /api/system/memory`
**Description:** Get memory usage  
**Authentication:** Required

**Response:**
```json
{
  "rss": 52428800,
  "heapTotal": 18874368,
  "heapUsed": 12345678,
  "external": 1024000,
  "arrayBuffers": 512000
}
```

#### `GET /api/system/performance`
**Description:** Get performance metrics  
**Authentication:** Required

**Response:**
```json
{
  "uptime": 3600,
  "loadAverage": [0.1, 0.2, 0.3],
  "cpuUsage": {
    "user": 123456,
    "system": 78901
  },
  "memoryUsage": {
    "rss": 52428800,
    "heapTotal": 18874368,
    "heapUsed": 12345678
  }
}
```

---

## 🔌 **WEBSOCKET EVENTS**

### **Connection Events**

#### `connect`
**Description:** Client connection established  
**Direction:** Server → Client

**Payload:**
```json
{
  "event": "connect",
  "socketId": "abc123xyz",
  "timestamp": "2025-01-08T23:30:00Z"
}
```

#### `disconnect`
**Description:** Client disconnection  
**Direction:** Server → Client

**Payload:**
```json
{
  "event": "disconnect",
  "reason": "client_disconnect",
  "timestamp": "2025-01-08T23:30:00Z"
}
```

### **Audio Events**

#### `audio:play` (Client → Server)
**Description:** Request audio playback

**Payload:**
```json
{
  "file": "airhorn.mp3",
  "volume": 0.8
}
```

#### `audio:status` (Server → Client)
**Description:** Audio playback status update

**Payload:**
```json
{
  "playing": true,
  "file": "airhorn.mp3",
  "volume": 0.8,
  "timestamp": "2025-01-08T23:30:00Z"
}
```

#### `audio:stopped` (Server → Client)
**Description:** Audio playback stopped

**Payload:**
```json
{
  "file": "airhorn.mp3",
  "reason": "finished",
  "timestamp": "2025-01-08T23:30:00Z"
}
```

### **Device Events**

#### `device:connected` (Server → Client)
**Description:** Device connection detected

**Payload:**
```json
{
  "type": "adb",
  "device": {
    "serial": "ABCD1234567890",
    "model": "SM-G991B",
    "state": "device"
  },
  "timestamp": "2025-01-08T23:30:00Z"
}
```

#### `device:disconnected` (Server → Client)
**Description:** Device disconnection detected

**Payload:**
```json
{
  "type": "adb",
  "device": {
    "serial": "ABCD1234567890"
  },
  "timestamp": "2025-01-08T23:30:00Z"
}
```

### **System Events**

#### `system:status` (Server → Client)
**Description:** System status update

**Payload:**
```json
{
  "memory": {
    "used": 52428800,
    "percentage": 26.2
  },
  "cpu": {
    "usage": 15.3
  },
  "services": {
    "audio": "operational",
    "devices": "operational"
  },
  "timestamp": "2025-01-08T23:30:00Z"
}
```

#### `system:error` (Server → Client)
**Description:** System error notification

**Payload:**
```json
{
  "level": "error",
  "component": "audio",
  "message": "Audio playback failed",
  "error": "File not found",
  "timestamp": "2025-01-08T23:30:00Z"
}
```

### **Testing Events**

#### `test-echo` (Client → Server)
**Description:** Echo test for connection validation

**Payload:**
```json
{
  "message": "test-message",
  "timestamp": 1704754200000
}
```

#### `test-echo-response` (Server → Client)
**Description:** Echo response

**Payload:**
```json
{
  "message": "test-message",
  "timestamp": 1704754200000,
  "serverTimestamp": "2025-01-08T23:30:00Z"
}
```

---

## 🔒 **AUTHENTICATION**

### **Device Authentication**
- **Method:** Device-based authentication using device identifiers
- **Headers:** `X-Device-ID` header required for authenticated endpoints
- **Session:** Persistent WebSocket connections maintain authentication state

### **Error Responses**

#### `401 Unauthorized`
```json
{
  "error": "Unauthorized",
  "message": "Authentication required",
  "code": 401
}
```

#### `403 Forbidden`
```json
{
  "error": "Forbidden", 
  "message": "Insufficient permissions",
  "code": 403
}
```

---

## ⚠️ **ERROR HANDLING**

### **Standard Error Response Format**
```json
{
  "error": "ErrorType",
  "message": "Human readable error message",
  "code": 400,
  "timestamp": "2025-01-08T23:30:00Z",
  "details": {
    "field": "Additional error details"
  }
}
```

### **Common Error Codes**
- **400**: Bad Request - Invalid request format or parameters
- **401**: Unauthorized - Authentication required
- **403**: Forbidden - Insufficient permissions
- **404**: Not Found - Resource not found
- **409**: Conflict - Resource conflict (e.g., file already exists)
- **500**: Internal Server Error - Server-side error
- **503**: Service Unavailable - Service temporarily unavailable

---

## 📊 **RATE LIMITING**

### **Rate Limits**
- **Audio Playback**: 10 requests per minute per device
- **File Upload**: 5 requests per minute per device
- **Device Operations**: 30 requests per minute per device
- **System Status**: 60 requests per minute per device

### **Rate Limit Headers**
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 8
X-RateLimit-Reset: 1704754260
```

---

## 🧪 **TESTING ENDPOINTS**

### **Test Utilities**

#### `GET /api/test/echo`
**Description:** Simple echo endpoint for testing  
**Authentication:** None required

**Query Parameters:**
- `message` (string): Message to echo back

**Response:**
```json
{
  "echo": "test message",
  "timestamp": "2025-01-08T23:30:00Z",
  "server": "AudioDeck Connect v9.0"
}
```

#### `GET /api/test/performance`
**Description:** Performance test endpoint  
**Authentication:** Required

**Response:**
```json
{
  "responseTime": 15,
  "memoryUsage": 52428800,
  "uptime": 3600,
  "timestamp": "2025-01-08T23:30:00Z"
}
```

---

*API Specification v9.0 - Enterprise-Grade Audio Control Platform*