# AudioDeck Connect - Operations & Maintenance Guide
## Production Operations, Monitoring, and Maintenance Procedures
**Version:** 9.0  
**Date:** January 8, 2025  
**Target:** Production Operations Team

---

## 🎯 **OPERATIONS OVERVIEW**

AudioDeck Connect is designed for enterprise-grade operations with comprehensive monitoring, automated health checks, and streamlined maintenance procedures. This guide covers daily operations, monitoring, maintenance, and incident response.

### **Operational Architecture**
```
Production Environment
        ↓
Health Monitoring & Alerting
        ↓
Performance Metrics Collection
        ↓
Automated Maintenance Tasks
        ↓
Incident Response & Recovery
```

---

## 📊 **MONITORING AND HEALTH CHECKS**

### **Real-Time Health Monitoring**

#### **Health Check Endpoints**
```bash
# Primary health check
curl http://localhost:3001/health

# Detailed system status
curl http://localhost:3001/api/status

# Performance metrics
curl http://localhost:3001/api/system/performance

# Memory usage
curl http://localhost:3001/api/system/memory
```

#### **Expected Health Responses**
```json
// Healthy Response
{
  "status": "healthy",
  "timestamp": "2025-01-08T23:30:00Z",
  "version": "9.0.0",
  "uptime": 86400,
  "services": {
    "audio": "operational",
    "devices": "operational",
    "network": "operational"
  },
  "metrics": {
    "memoryUsage": 52428800,
    "responseTime": 15,
    "activeConnections": 3
  }
}
```

### **Automated Monitoring Setup**

#### **Health Check Script**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/health-monitor.sh

HEALTH_URL="http://localhost:3001/health"
STATUS_URL="http://localhost:3001/api/status"
LOG_FILE="/var/log/audiodeck/health.log"
ALERT_WEBHOOK="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"

# Health check function
check_health() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Basic health check
    local health_response=$(curl -s -w "%{http_code}" -o /tmp/health.json "$HEALTH_URL")
    
    if [ "$health_response" -eq 200 ]; then
        local status=$(jq -r '.status' /tmp/health.json)
        local uptime=$(jq -r '.uptime' /tmp/health.json)
        local memory=$(jq -r '.metrics.memoryUsage' /tmp/health.json)
        
        echo "[$timestamp] OK - Status: $status, Uptime: ${uptime}s, Memory: ${memory}B" >> "$LOG_FILE"
        
        # Check memory threshold (200MB)
        if [ "$memory" -gt 209715200 ]; then
            send_alert "WARNING" "High memory usage: ${memory}B (>200MB)"
        fi
        
    else
        echo "[$timestamp] ERROR - Health check failed (HTTP $health_response)" >> "$LOG_FILE"
        send_alert "CRITICAL" "Health check failed with HTTP $health_response"
    fi
}

# Alert function
send_alert() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Log alert
    echo "[$timestamp] ALERT [$level] $message" >> "$LOG_FILE"
    
    # Send to Slack (optional)
    if [ -n "$ALERT_WEBHOOK" ]; then
        curl -s -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"AudioDeck Alert [$level]: $message\"}" \
            "$ALERT_WEBHOOK"
    fi
    
    # Send email (optional)
    # echo "$message" | mail -s "AudioDeck Alert [$level]" admin@company.com
}

# Run health check
check_health

# Cleanup
rm -f /tmp/health.json
```

#### **Cron Job Setup**
```bash
# Add to crontab
crontab -e

# Monitor every minute
* * * * * /opt/audiodeck/scripts/health-monitor.sh

# Daily log rotation
0 0 * * * /opt/audiodeck/scripts/rotate-logs.sh

# Weekly performance report
0 9 * * 1 /opt/audiodeck/scripts/weekly-report.sh
```

### **Performance Metrics Dashboard**

#### **Key Performance Indicators (KPIs)**
| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| **Response Time** | <50ms | >100ms | >500ms |
| **Memory Usage** | <200MB | >200MB | >300MB |
| **CPU Usage** | <50% | >70% | >90% |
| **Uptime** | 99.9% | <99.5% | <99% |
| **Error Rate** | <0.1% | >1% | >5% |

#### **Performance Monitoring Script**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/performance-monitor.sh

METRICS_LOG="/var/log/audiodeck/metrics.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Get process ID
PID=$(pgrep -f audiodeck-server)

if [ -z "$PID" ]; then
    echo "[$TIMESTAMP] ERROR - AudioDeck process not found" >> "$METRICS_LOG"
    exit 1
fi

# Collect metrics
CPU_USAGE=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')
MEMORY_KB=$(ps -p $PID -o rss --no-headers | tr -d ' ')
MEMORY_MB=$((MEMORY_KB / 1024))

# Test response time
RESPONSE_TIME=$(curl -w "%{time_total}" -s -o /dev/null http://localhost:3001/health)
RESPONSE_MS=$(echo "$RESPONSE_TIME * 1000" | bc | cut -d. -f1)

# Get connection count
CONNECTIONS=$(netstat -an | grep :3001 | grep ESTABLISHED | wc -l)

# Log metrics
echo "[$TIMESTAMP] CPU: ${CPU_USAGE}%, Memory: ${MEMORY_MB}MB, Response: ${RESPONSE_MS}ms, Connections: $CONNECTIONS" >> "$METRICS_LOG"

# Check thresholds and alert
if (( $(echo "$CPU_USAGE > 70" | bc -l) )); then
    echo "[$TIMESTAMP] WARNING - High CPU usage: ${CPU_USAGE}%" >> "$METRICS_LOG"
fi

if [ "$MEMORY_MB" -gt 200 ]; then
    echo "[$TIMESTAMP] WARNING - High memory usage: ${MEMORY_MB}MB" >> "$METRICS_LOG"
fi

if [ "$RESPONSE_MS" -gt 100 ]; then
    echo "[$TIMESTAMP] WARNING - Slow response time: ${RESPONSE_MS}ms" >> "$METRICS_LOG"
fi
```

---

## 🔧 **MAINTENANCE PROCEDURES**

### **Daily Maintenance Tasks**

#### **Daily Checklist**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/daily-maintenance.sh

echo "=== AudioDeck Daily Maintenance $(date) ==="

# 1. Check service status
echo "1. Checking service status..."
systemctl is-active audiodeck
systemctl status audiodeck --no-pager -l

# 2. Check health
echo "2. Checking health..."
curl -s http://localhost:3001/health | jq '.'

# 3. Check disk space
echo "3. Checking disk space..."
df -h /opt/audiodeck
df -h /var/log/audiodeck

# 4. Check log sizes
echo "4. Checking log sizes..."
ls -lh /var/log/audiodeck/

# 5. Check memory usage
echo "5. Checking memory usage..."
ps aux | grep audiodeck-server | grep -v grep

# 6. Check network connectivity
echo "6. Checking network connectivity..."
netstat -tlnp | grep audiodeck-server

# 7. Test audio functionality
echo "7. Testing audio functionality..."
curl -s -X POST -H "Content-Type: application/json" \
    -d '{"file":"test-tone.mp3","volume":0.1}' \
    http://localhost:3001/api/audio/test

echo "=== Daily maintenance complete ==="
```

### **Weekly Maintenance Tasks**

#### **Weekly Checklist**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/weekly-maintenance.sh

echo "=== AudioDeck Weekly Maintenance $(date) ==="

# 1. Log rotation and cleanup
echo "1. Rotating logs..."
/opt/audiodeck/scripts/rotate-logs.sh

# 2. Update system packages (if applicable)
echo "2. Checking for system updates..."
if command -v apt &> /dev/null; then
    apt list --upgradable
elif command -v yum &> /dev/null; then
    yum check-update
fi

# 3. Backup configuration
echo "3. Backing up configuration..."
tar -czf "/backup/audiodeck-config-$(date +%Y%m%d).tar.gz" \
    /opt/audiodeck/config/ \
    /etc/systemd/system/audiodeck.service

# 4. Performance analysis
echo "4. Generating performance report..."
/opt/audiodeck/scripts/weekly-report.sh

# 5. Security audit
echo "5. Running security checks..."
# Check file permissions
find /opt/audiodeck -type f -perm /o+w
# Check for unusual network connections
netstat -tulpn | grep audiodeck-server

# 6. Disk cleanup
echo "6. Cleaning up temporary files..."
find /tmp -name "*audiodeck*" -mtime +7 -delete
find /var/log/audiodeck -name "*.log.*" -mtime +30 -delete

echo "=== Weekly maintenance complete ==="
```

### **Monthly Maintenance Tasks**

#### **Monthly Checklist**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/monthly-maintenance.sh

echo "=== AudioDeck Monthly Maintenance $(date) ==="

# 1. Full system backup
echo "1. Creating full system backup..."
tar -czf "/backup/audiodeck-full-$(date +%Y%m%d).tar.gz" \
    /opt/audiodeck/ \
    /etc/systemd/system/audiodeck.service \
    /var/log/audiodeck/

# 2. Performance trending analysis
echo "2. Analyzing performance trends..."
python3 /opt/audiodeck/scripts/performance-analysis.py

# 3. Security updates
echo "3. Checking for security updates..."
if command -v apt &> /dev/null; then
    apt update && apt list --upgradable | grep -i security
fi

# 4. Certificate renewal (if applicable)
echo "4. Checking SSL certificates..."
# Check certificate expiration
# openssl x509 -in /path/to/cert.pem -text -noout | grep "Not After"

# 5. Capacity planning review
echo "5. Reviewing capacity metrics..."
echo "Average memory usage over last 30 days:"
awk '/Memory:/ {sum+=$4; count++} END {print sum/count "MB"}' /var/log/audiodeck/metrics.log

echo "Average response time over last 30 days:"
awk '/Response:/ {sum+=$6; count++} END {print sum/count "ms"}' /var/log/audiodeck/metrics.log

echo "=== Monthly maintenance complete ==="
```

---

## 📝 **LOG MANAGEMENT**

### **Log File Structure**
```
/var/log/audiodeck/
├── application.log          # Main application logs
├── health.log              # Health check results
├── metrics.log             # Performance metrics
├── error.log               # Error messages only
├── access.log              # HTTP access logs
├── audit.log               # Security and admin actions
└── archive/                # Rotated log files
    ├── application.log.1
    ├── application.log.2.gz
    └── ...
```

### **Log Rotation Configuration**
```bash
# /etc/logrotate.d/audiodeck
/var/log/audiodeck/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 audiodeck audiodeck
    postrotate
        systemctl reload audiodeck
    endscript
}
```

### **Log Analysis Scripts**

#### **Error Analysis**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/analyze-errors.sh

LOG_FILE="/var/log/audiodeck/error.log"
REPORT_FILE="/tmp/error-report-$(date +%Y%m%d).txt"

echo "AudioDeck Error Analysis - $(date)" > "$REPORT_FILE"
echo "======================================" >> "$REPORT_FILE"

# Count errors by type
echo "Error Summary (Last 24 hours):" >> "$REPORT_FILE"
grep "$(date --date='1 day ago' '+%Y-%m-%d')" "$LOG_FILE" | \
    awk '{print $4}' | sort | uniq -c | sort -nr >> "$REPORT_FILE"

# Recent critical errors
echo -e "\nRecent Critical Errors:" >> "$REPORT_FILE"
grep -i "critical\|fatal" "$LOG_FILE" | tail -10 >> "$REPORT_FILE"

# Audio-related errors
echo -e "\nAudio-Related Errors:" >> "$REPORT_FILE"
grep -i "audio\|sound\|playback" "$LOG_FILE" | tail -10 >> "$REPORT_FILE"

cat "$REPORT_FILE"
```

#### **Performance Analysis**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/performance-analysis.sh

METRICS_LOG="/var/log/audiodeck/metrics.log"
REPORT_FILE="/tmp/performance-report-$(date +%Y%m%d).txt"

echo "AudioDeck Performance Analysis - $(date)" > "$REPORT_FILE"
echo "=========================================" >> "$REPORT_FILE"

# Average response time
echo "Average Response Time (Last 24 hours):" >> "$REPORT_FILE"
grep "$(date --date='1 day ago' '+%Y-%m-%d')" "$METRICS_LOG" | \
    awk -F'Response: ' '{print $2}' | awk -F'ms' '{print $1}' | \
    awk '{sum+=$1; count++} END {printf "%.2f ms\n", sum/count}' >> "$REPORT_FILE"

# Memory usage trend
echo -e "\nMemory Usage Trend (Last 24 hours):" >> "$REPORT_FILE"
grep "$(date --date='1 day ago' '+%Y-%m-%d')" "$METRICS_LOG" | \
    awk -F'Memory: ' '{print $2}' | awk -F'MB' '{print $1}' | \
    awk '{sum+=$1; count++; if($1>max) max=$1; if($1<min || min==0) min=$1} 
         END {printf "Avg: %.2f MB, Min: %.2f MB, Max: %.2f MB\n", sum/count, min, max}' >> "$REPORT_FILE"

# Connection statistics
echo -e "\nConnection Statistics:" >> "$REPORT_FILE"
grep "$(date --date='1 day ago' '+%Y-%m-%d')" "$METRICS_LOG" | \
    awk -F'Connections: ' '{print $2}' | \
    awk '{sum+=$1; count++; if($1>max) max=$1} 
         END {printf "Avg: %.2f, Peak: %d\n", sum/count, max}' >> "$REPORT_FILE"

cat "$REPORT_FILE"
```

---

## 🚨 **INCIDENT RESPONSE**

### **Incident Classification**

#### **Severity Levels**
| Level | Description | Response Time | Examples |
|-------|-------------|---------------|----------|
| **P1** | Critical - Service Down | 15 minutes | Server crash, total outage |
| **P2** | High - Major Functionality | 1 hour | Audio not working, device connection issues |
| **P3** | Medium - Minor Issues | 4 hours | Slow response times, non-critical errors |
| **P4** | Low - Enhancement/Info | 24 hours | Feature requests, documentation updates |

### **Incident Response Procedures**

#### **P1 - Critical Incident Response**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/incident-p1-response.sh

echo "=== P1 INCIDENT RESPONSE - $(date) ==="

# 1. Immediate assessment
echo "1. Checking service status..."
systemctl status audiodeck

# 2. Quick restart attempt
echo "2. Attempting service restart..."
systemctl restart audiodeck
sleep 10

# 3. Verify restart
if systemctl is-active audiodeck > /dev/null; then
    echo "✓ Service restarted successfully"
    curl -s http://localhost:3001/health
else
    echo "✗ Service restart failed"
    
    # 4. Collect diagnostics
    echo "4. Collecting diagnostics..."
    journalctl -u audiodeck --since "1 hour ago" > "/tmp/incident-logs-$(date +%Y%m%d-%H%M).txt"
    ps aux | grep audiodeck >> "/tmp/incident-logs-$(date +%Y%m%d-%H%M).txt"
    netstat -tlnp | grep 3001 >> "/tmp/incident-logs-$(date +%Y%m%d-%H%M).txt"
    
    # 5. Escalate
    echo "5. Escalating to on-call engineer..."
    # Send alert with diagnostic information
fi

echo "=== P1 INCIDENT RESPONSE COMPLETE ==="
```

#### **Recovery Procedures**

##### **Service Recovery**
```bash
# Standard service recovery
systemctl stop audiodeck
sleep 5
systemctl start audiodeck
sleep 10

# Verify recovery
curl http://localhost:3001/health

# If still failing, try backup executable
if [ $? -ne 0 ]; then
    cp /opt/audiodeck/backup/audiodeck-server /opt/audiodeck/audiodeck-server
    systemctl start audiodeck
fi
```

##### **Database/Configuration Recovery**
```bash
# Restore configuration from backup
cp /backup/audiodeck-config-latest.tar.gz /tmp/
cd /tmp
tar -xzf audiodeck-config-latest.tar.gz
cp -r opt/audiodeck/config/* /opt/audiodeck/config/

# Restart with restored configuration
systemctl restart audiodeck
```

##### **Log Corruption Recovery**
```bash
# Stop service
systemctl stop audiodeck

# Move corrupted logs
mv /var/log/audiodeck /var/log/audiodeck.corrupted.$(date +%Y%m%d)

# Recreate log directory
mkdir -p /var/log/audiodeck
chown audiodeck:audiodeck /var/log/audiodeck

# Restart service
systemctl start audiodeck
```

---

## 🔄 **BACKUP AND RECOVERY**

### **Backup Strategy**

#### **Backup Components**
1. **Executable**: `/opt/audiodeck/audiodeck-server`
2. **Configuration**: `/opt/audiodeck/config/`
3. **Audio Assets**: `/opt/audiodeck/audio/`
4. **Service Definition**: `/etc/systemd/system/audiodeck.service`
5. **Logs**: `/var/log/audiodeck/` (selective)

#### **Automated Backup Script**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/backup.sh

BACKUP_DIR="/backup/audiodeck"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="audiodeck-backup-$TIMESTAMP.tar.gz"

echo "Starting AudioDeck backup - $(date)"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Create comprehensive backup
tar -czf "$BACKUP_DIR/$BACKUP_FILE" \
    --exclude='/var/log/audiodeck/*.log.*' \
    /opt/audiodeck/ \
    /etc/systemd/system/audiodeck.service \
    /var/log/audiodeck/

# Verify backup
if [ $? -eq 0 ]; then
    echo "✓ Backup created: $BACKUP_DIR/$BACKUP_FILE"
    echo "Backup size: $(du -h "$BACKUP_DIR/$BACKUP_FILE" | cut -f1)"
    
    # Cleanup old backups (keep 30 days)
    find "$BACKUP_DIR" -name "audiodeck-backup-*.tar.gz" -mtime +30 -delete
    
else
    echo "✗ Backup failed"
    exit 1
fi

echo "Backup completed - $(date)"
```

### **Recovery Procedures**

#### **Full System Recovery**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/recover.sh

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file>"
    echo "Available backups:"
    ls -la /backup/audiodeck/audiodeck-backup-*.tar.gz
    exit 1
fi

echo "Starting AudioDeck recovery from $BACKUP_FILE"

# Stop service
systemctl stop audiodeck

# Backup current state
mv /opt/audiodeck /opt/audiodeck.before-recovery.$(date +%Y%m%d)

# Extract backup
cd /
tar -xzf "$BACKUP_FILE"

# Restore service
systemctl daemon-reload
systemctl enable audiodeck

# Start service
systemctl start audiodeck

# Verify recovery
sleep 10
if systemctl is-active audiodeck > /dev/null; then
    echo "✓ Recovery successful"
    curl http://localhost:3001/health
else
    echo "✗ Recovery failed"
    systemctl status audiodeck
fi
```

---

## 🔒 **SECURITY OPERATIONS**

### **Security Monitoring**

#### **Security Audit Script**
```bash
#!/bin/bash
# /opt/audiodeck/scripts/security-audit.sh

AUDIT_LOG="/var/log/audiodeck/security-audit.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$TIMESTAMP] Starting security audit" >> "$AUDIT_LOG"

# Check file permissions
echo "Checking file permissions..." >> "$AUDIT_LOG"
find /opt/audiodeck -type f -perm /o+w >> "$AUDIT_LOG" 2>/dev/null

# Check for unusual network connections
echo "Checking network connections..." >> "$AUDIT_LOG"
netstat -tulpn | grep audiodeck-server >> "$AUDIT_LOG"

# Check for failed authentication attempts
echo "Checking failed authentication..." >> "$AUDIT_LOG"
grep "authentication failed" /var/log/audiodeck/application.log >> "$AUDIT_LOG" 2>/dev/null

# Check system user accounts
echo "Checking system accounts..." >> "$AUDIT_LOG"
getent passwd audiodeck >> "$AUDIT_LOG"

echo "[$TIMESTAMP] Security audit completed" >> "$AUDIT_LOG"
```

### **Security Updates**
```bash
# Monthly security update procedure
apt update
apt list --upgradable | grep -i security

# Update security packages
apt upgrade $(apt list --upgradable 2>/dev/null | grep -i security | cut -d/ -f1)

# Restart services if needed
systemctl restart audiodeck
```

---

## 📋 **OPERATIONAL CHECKLISTS**

### **Daily Operations Checklist**
- [ ] Check service status and health
- [ ] Review error logs for issues
- [ ] Verify response times within SLA
- [ ] Check memory and CPU usage
- [ ] Validate backup completion
- [ ] Review security alerts

### **Weekly Operations Checklist**
- [ ] Rotate and archive logs
- [ ] Update system packages
- [ ] Review performance trends
- [ ] Test backup/recovery procedures
- [ ] Update documentation
- [ ] Security audit and review

### **Monthly Operations Checklist**
- [ ] Full system backup and verification
- [ ] Capacity planning review
- [ ] Security updates and patches
- [ ] Performance optimization review
- [ ] Disaster recovery testing
- [ ] Documentation updates

---

*Operations & Maintenance Guide v9.0 - Enterprise-Grade Production Operations*