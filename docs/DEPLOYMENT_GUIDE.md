# AudioDeck Connect - Deployment Guide
## Production Deployment and Environment Setup
**Version:** 9.0  
**Date:** January 8, 2025  
**Target:** Production Deployment

---

## 🚀 **DEPLOYMENT OVERVIEW**

AudioDeck Connect is deployed as a self-contained executable with enterprise-grade native modules and cross-platform compatibility. The deployment supports multiple platforms with consistent functionality and performance.

### **Deployment Architecture**
```
Source Repository
        ↓
Build Pipeline (ESBuild + PKG)
        ↓
Cross-Platform Executables
        ↓
Production Environment
        ↓
Monitoring & Health Checks
```

---

## 📋 **PRE-DEPLOYMENT REQUIREMENTS**

### **System Requirements**

#### **Minimum Requirements**
- **OS**: macOS 11+, Windows 10+, Ubuntu 18.04+
- **Architecture**: x64 or ARM64
- **Memory**: 512MB RAM available
- **Storage**: 200MB available disk space
- **Network**: Internet connectivity for initial setup

#### **Recommended Requirements**
- **Memory**: 1GB RAM available
- **Storage**: 1GB available disk space
- **Network**: Stable internet connection
- **Firewall**: Ports 3001 (HTTP), 41234 (UDP Discovery) open

### **Platform-Specific Requirements**

#### **macOS**
- macOS 11.0 (Big Sur) or later
- ARM64 (Apple Silicon) or Intel x64 support
- Administrator privileges for installation
- Audio output device configured

#### **Windows**
- Windows 10 version 1909 or later
- x64 architecture
- Administrator privileges for installation
- Optional: VoiceMeeter for advanced audio routing

#### **Linux**
- Ubuntu 18.04 LTS or equivalent
- x64 architecture
- Audio system (ALSA/PulseAudio) configured
- USB access permissions for device detection

---

## 🔧 **BUILD AND PREPARATION**

### **Build Process**

#### **1. Clean Build Setup**
```bash
# Clone repository
git clone [repository-url]
cd android-soundboard-app/server

# Clean environment
npm run clean-all
rm -rf node_modules package-lock.json

# Fresh installation
npm install
```

#### **2. Production Build**
```bash
# Execute production build
npm run build

# Verify build output
ls -la ../dist/audiodeck-server
file ../dist/audiodeck-server
```

#### **3. Build Validation**
```bash
# Run build validation tests
node test-build-validation.js

# Verify test results
cat build-validation-report.json
```

### **Expected Build Output**
```
dist/
└── audiodeck-server         # 123MB self-contained executable

server/build/
├── server-bundle.js         # 1.8MB JavaScript bundle
├── server-bundle.js.map     # 2.8MB source map
├── node_modules/            # Native modules (144MB)
│   ├── @yume-chan/
│   ├── koffi/
│   └── voicemeeter-connector/
└── audio/                   # Audio assets (215KB)
    ├── airhorn.mp3
    ├── applause.mp3
    ├── bell.wav
    └── Bruh.mp3
```

---

## 📦 **DEPLOYMENT PROCEDURES**

### **Single Server Deployment**

#### **1. Environment Preparation**
```bash
# Create deployment directory
sudo mkdir -p /opt/audiodeck
sudo chown $USER:$USER /opt/audiodeck

# Copy executable
cp dist/audiodeck-server /opt/audiodeck/
chmod +x /opt/audiodeck/audiodeck-server

# Verify executable
/opt/audiodeck/audiodeck-server --version
```

#### **2. Configuration Setup**
```bash
# Create configuration directory
mkdir -p /opt/audiodeck/config

# Create environment configuration
cat > /opt/audiodeck/config/.env << EOF
NODE_ENV=production
PORT=3001
DISCOVERY_PORT=41234
LOG_LEVEL=info
HEALTH_CHECK_INTERVAL=30000
PERFORMANCE_MONITORING=true
EOF
```

#### **3. Service Installation (Linux/macOS)**
```bash
# Create systemd service (Linux)
sudo cat > /etc/systemd/system/audiodeck.service << EOF
[Unit]
Description=AudioDeck Connect Server
After=network.target

[Service]
Type=simple
User=audiodeck
WorkingDirectory=/opt/audiodeck
ExecStart=/opt/audiodeck/audiodeck-server
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
EnvironmentFile=/opt/audiodeck/config/.env

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl enable audiodeck
sudo systemctl start audiodeck
sudo systemctl status audiodeck
```

#### **4. Windows Service Installation**
```powershell
# Install as Windows service using NSSM
nssm install AudioDeckConnect "C:\Program Files\AudioDeck\audiodeck-server.exe"
nssm set AudioDeckConnect AppDirectory "C:\Program Files\AudioDeck"
nssm set AudioDeckConnect AppStdout "C:\Program Files\AudioDeck\logs\stdout.log"
nssm set AudioDeckConnect AppStderr "C:\Program Files\AudioDeck\logs\stderr.log"
nssm start AudioDeckConnect
```

---

## 🌐 **NETWORK CONFIGURATION**

### **Firewall Configuration**

#### **Required Ports**
- **3001/TCP**: HTTP API and WebSocket connections
- **41234/UDP**: Network discovery (optional)

#### **Linux (UFW)**
```bash
sudo ufw allow 3001/tcp
sudo ufw allow 41234/udp
sudo ufw reload
```

#### **macOS**
```bash
# Add firewall rules
sudo pfctl -f /etc/pf.conf
# Configure in System Preferences > Security & Privacy > Firewall
```

#### **Windows**
```powershell
# Allow through Windows Firewall
netsh advfirewall firewall add rule name="AudioDeck HTTP" dir=in action=allow protocol=TCP localport=3001
netsh advfirewall firewall add rule name="AudioDeck Discovery" dir=in action=allow protocol=UDP localport=41234
```

### **Reverse Proxy Configuration (Optional)**

#### **Nginx Configuration**
```nginx
server {
    listen 80;
    server_name audiodeck.local;
    
    location / {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

---

## 📊 **MONITORING AND HEALTH CHECKS**

### **Health Check Endpoints**

#### **Basic Health Check**
```bash
# Test server health
curl http://localhost:3001/health

# Expected response
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

#### **Detailed System Status**
```bash
# Get detailed status
curl http://localhost:3001/api/status

# Performance metrics
curl http://localhost:3001/api/system/performance
```

### **Monitoring Setup**

#### **Log Monitoring**
```bash
# View server logs (systemd)
journalctl -u audiodeck -f

# View application logs
tail -f /opt/audiodeck/logs/application.log

# Log rotation setup
sudo logrotate /etc/logrotate.d/audiodeck
```

#### **Performance Monitoring Script**
```bash
#!/bin/bash
# monitor-audiodeck.sh

HEALTH_URL="http://localhost:3001/health"
LOG_FILE="/var/log/audiodeck-monitor.log"

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Health check
    HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)
    
    if [ "$HEALTH_STATUS" -eq 200 ]; then
        echo "[$TIMESTAMP] OK - Server healthy" >> $LOG_FILE
    else
        echo "[$TIMESTAMP] ERROR - Server unhealthy (HTTP $HEALTH_STATUS)" >> $LOG_FILE
        # Alert notification (email, Slack, etc.)
    fi
    
    sleep 60
done
```

---

## 🔒 **SECURITY CONFIGURATION**

### **Service User Setup**
```bash
# Create dedicated service user (Linux)
sudo useradd --system --no-create-home --shell /bin/false audiodeck
sudo chown -R audiodeck:audiodeck /opt/audiodeck
```

### **File Permissions**
```bash
# Set secure permissions
chmod 755 /opt/audiodeck/audiodeck-server
chmod 644 /opt/audiodeck/config/.env
chown audiodeck:audiodeck /opt/audiodeck/config/.env
```

### **Network Security**
- Bind to localhost only for internal use
- Use reverse proxy for external access
- Enable HTTPS in production environments
- Implement rate limiting and request validation

---

## 🔄 **UPDATE PROCEDURES**

### **Rolling Update Process**

#### **1. Preparation**
```bash
# Backup current installation
cp /opt/audiodeck/audiodeck-server /opt/audiodeck/audiodeck-server.backup
cp /opt/audiodeck/config/.env /opt/audiodeck/config/.env.backup
```

#### **2. Update Deployment**
```bash
# Stop service
sudo systemctl stop audiodeck

# Deploy new version
cp dist/audiodeck-server /opt/audiodeck/
chmod +x /opt/audiodeck/audiodeck-server

# Verify new version
/opt/audiodeck/audiodeck-server --version

# Start service
sudo systemctl start audiodeck

# Verify health
curl http://localhost:3001/health
```

#### **3. Rollback Procedure (if needed)**
```bash
# Stop service
sudo systemctl stop audiodeck

# Restore backup
cp /opt/audiodeck/audiodeck-server.backup /opt/audiodeck/audiodeck-server
cp /opt/audiodeck/config/.env.backup /opt/audiodeck/config/.env

# Start service
sudo systemctl start audiodeck
```

---

## 🧪 **DEPLOYMENT TESTING**

### **Post-Deployment Validation**

#### **1. Service Validation**
```bash
# Test service startup
sudo systemctl restart audiodeck
sleep 10
sudo systemctl status audiodeck

# Verify process
ps aux | grep audiodeck-server
```

#### **2. Functionality Testing**
```bash
# Run runtime validation tests
cd /opt/audiodeck
node test-runtime-validation.js

# Verify test results
cat runtime-validation-report.json
```

#### **3. Performance Validation**
```bash
# Memory usage check
ps -o pid,vsz,rss,comm -p $(pgrep audiodeck-server)

# Response time test
time curl http://localhost:3001/health

# Load test (optional)
ab -n 100 -c 10 http://localhost:3001/health
```

---

## 🐛 **TROUBLESHOOTING**

### **Common Issues**

#### **Service Won't Start**
```bash
# Check service status
sudo systemctl status audiodeck

# Check logs
journalctl -u audiodeck --since "1 hour ago"

# Check executable permissions
ls -la /opt/audiodeck/audiodeck-server

# Verify dependencies
ldd /opt/audiodeck/audiodeck-server  # Linux only
```

#### **Port Already in Use**
```bash
# Find process using port
sudo netstat -tlnp | grep :3001
sudo lsof -i :3001

# Kill conflicting process
sudo kill -9 [PID]
```

#### **Audio Issues**
```bash
# Test audio system (Linux)
aplay /usr/share/sounds/alsa/Front_Left.wav

# Check audio devices (macOS)
system_profiler SPAudioDataType

# Verify VoiceMeeter (Windows)
# Check VoiceMeeter installation and configuration
```

### **Performance Issues**

#### **High Memory Usage**
```bash
# Monitor memory usage
watch 'ps -p $(pgrep audiodeck-server) -o pid,vsz,rss,pcpu,comm'

# Check for memory leaks
valgrind --tool=memcheck /opt/audiodeck/audiodeck-server
```

#### **Slow Response Times**
```bash
# Profile request handling
curl -w "Time: %{time_total}s\n" http://localhost:3001/health

# Check system load
uptime
iostat 1 5
```

---

## 📋 **DEPLOYMENT CHECKLIST**

### **Pre-Deployment**
- [ ] Build validation tests passed
- [ ] All dependencies verified
- [ ] Configuration files prepared
- [ ] Backup procedures tested
- [ ] Monitoring setup configured

### **Deployment**
- [ ] Service user created
- [ ] Executable deployed and permissions set
- [ ] Configuration files deployed
- [ ] Service installed and configured
- [ ] Firewall rules configured

### **Post-Deployment**
- [ ] Service started successfully
- [ ] Health checks passing
- [ ] All endpoints responding
- [ ] Performance metrics within targets
- [ ] Monitoring alerts configured
- [ ] Backup procedures verified

### **Production Readiness**
- [ ] Load testing completed
- [ ] Security review passed
- [ ] Documentation updated
- [ ] Team trained on operations
- [ ] Incident response procedures ready

---

*Deployment Guide v9.0 - Enterprise-Grade Production Deployment*