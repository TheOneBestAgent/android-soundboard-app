#!/bin/bash

#############################################################################
# AudioDeck Connect - Production Deployment Script
# Version: 9.0
# Date: January 8, 2025
# Description: Automated production deployment with comprehensive validation
#############################################################################

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DEPLOY_USER="${DEPLOY_USER:-audiodeck}"
DEPLOY_PATH="${DEPLOY_PATH:-/opt/audiodeck}"
SERVICE_NAME="${SERVICE_NAME:-audiodeck}"
BACKUP_DIR="${BACKUP_DIR:-/backup/audiodeck}"
LOG_FILE="/tmp/audiodeck-deploy-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO")  echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$LOG_FILE" ;;
        "WARN")  echo -e "${YELLOW}[WARN]${NC} $message" | tee -a "$LOG_FILE" ;;
        "ERROR") echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE" ;;
        "SUCCESS") echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$LOG_FILE" ;;
    esac
}

# Error handler
error_exit() {
    log ERROR "Deployment failed: $1"
    log ERROR "Check log file: $LOG_FILE"
    exit 1
}

# Check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        error_exit "This script must be run as root"
    fi
}

# Pre-deployment checks
pre_deployment_checks() {
    log INFO "Running pre-deployment checks..."
    
    # Check if build artifacts exist
    if [[ ! -f "$PROJECT_ROOT/dist/audiodeck-server" ]]; then
        error_exit "Build artifact not found. Run 'npm run build' first."
    fi
    
    # Check system requirements
    local available_memory=$(free -m | awk 'NR==2{printf "%.0f", $7}')
    if [[ $available_memory -lt 512 ]]; then
        log WARN "Available memory ($available_memory MB) is below recommended 512MB"
    fi
    
    # Check disk space
    local available_disk=$(df -m "$DEPLOY_PATH" 2>/dev/null | awk 'NR==2 {print $4}' || echo "1000")
    if [[ $available_disk -lt 1000 ]]; then
        error_exit "Insufficient disk space. Need at least 1GB free."
    fi
    
    # Check if ports are available
    if netstat -tlnp | grep -q ":3001 "; then
        log WARN "Port 3001 is already in use"
    fi
    
    log SUCCESS "Pre-deployment checks completed"
}

# Create deployment user and directories
setup_environment() {
    log INFO "Setting up deployment environment..."
    
    # Create deployment user if it doesn't exist
    if ! id "$DEPLOY_USER" &>/dev/null; then
        log INFO "Creating deployment user: $DEPLOY_USER"
        useradd --system --no-create-home --shell /bin/false "$DEPLOY_USER"
    fi
    
    # Create directories
    mkdir -p "$DEPLOY_PATH"/{bin,config,logs,scripts,backup}
    mkdir -p "$BACKUP_DIR"
    mkdir -p /var/log/audiodeck
    
    # Set permissions
    chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH"
    chown -R "$DEPLOY_USER:$DEPLOY_USER" /var/log/audiodeck
    chmod 755 "$DEPLOY_PATH"
    chmod 755 /var/log/audiodeck
    
    log SUCCESS "Environment setup completed"
}

# Backup current deployment
backup_current_deployment() {
    log INFO "Creating backup of current deployment..."
    
    local backup_file="$BACKUP_DIR/audiodeck-backup-$(date +%Y%m%d-%H%M%S).tar.gz"
    
    if [[ -f "$DEPLOY_PATH/bin/audiodeck-server" ]]; then
        tar -czf "$backup_file" \
            -C / \
            --exclude="$DEPLOY_PATH/logs/*" \
            --exclude="/var/log/audiodeck/*.log.*" \
            "opt/audiodeck" \
            "var/log/audiodeck" \
            "etc/systemd/system/$SERVICE_NAME.service" 2>/dev/null || true
        
        log SUCCESS "Backup created: $backup_file"
        echo "$backup_file" > "$DEPLOY_PATH/.last-backup"
    else
        log INFO "No existing deployment to backup"
    fi
}

# Deploy application
deploy_application() {
    log INFO "Deploying application..."
    
    # Stop service if running
    if systemctl is-active "$SERVICE_NAME" &>/dev/null; then
        log INFO "Stopping existing service..."
        systemctl stop "$SERVICE_NAME"
    fi
    
    # Deploy executable
    log INFO "Deploying executable..."
    cp "$PROJECT_ROOT/dist/audiodeck-server" "$DEPLOY_PATH/bin/"
    chmod +x "$DEPLOY_PATH/bin/audiodeck-server"
    chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH/bin/audiodeck-server"
    
    # Deploy configuration
    if [[ -f "$PROJECT_ROOT/server/config/production.env" ]]; then
        log INFO "Deploying configuration..."
        cp "$PROJECT_ROOT/server/config/production.env" "$DEPLOY_PATH/config/.env"
        chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH/config/.env"
        chmod 640 "$DEPLOY_PATH/config/.env"
    else
        log INFO "Creating default configuration..."
        cat > "$DEPLOY_PATH/config/.env" << EOF
NODE_ENV=production
PORT=3001
DISCOVERY_PORT=41234
LOG_LEVEL=info
HEALTH_CHECK_INTERVAL=30000
PERFORMANCE_MONITORING=true
EOF
        chown "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH/config/.env"
        chmod 640 "$DEPLOY_PATH/config/.env"
    fi
    
    # Deploy scripts
    log INFO "Deploying operational scripts..."
    cp -r "$SCRIPT_DIR"/*.sh "$DEPLOY_PATH/scripts/" 2>/dev/null || true
    chmod +x "$DEPLOY_PATH/scripts"/*.sh 2>/dev/null || true
    chown -R "$DEPLOY_USER:$DEPLOY_USER" "$DEPLOY_PATH/scripts"
    
    log SUCCESS "Application deployment completed"
}

# Configure systemd service
configure_service() {
    log INFO "Configuring systemd service..."
    
    cat > "/etc/systemd/system/$SERVICE_NAME.service" << EOF
[Unit]
Description=AudioDeck Connect Server
Documentation=https://docs.audiodeck.com
After=network.target sound.service

[Service]
Type=simple
User=$DEPLOY_USER
Group=$DEPLOY_USER
WorkingDirectory=$DEPLOY_PATH
ExecStart=$DEPLOY_PATH/bin/audiodeck-server
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=audiodeck

# Security settings
NoNewPrivileges=yes
PrivateTmp=yes
PrivateDevices=yes
ProtectHome=yes
ProtectSystem=strict
ReadWritePaths=$DEPLOY_PATH
ReadWritePaths=/var/log/audiodeck

# Environment
Environment=NODE_ENV=production
EnvironmentFile=$DEPLOY_PATH/config/.env

[Install]
WantedBy=multi-user.target
EOF
    
    # Reload systemd
    systemctl daemon-reload
    systemctl enable "$SERVICE_NAME"
    
    log SUCCESS "Systemd service configured"
}

# Configure log rotation
configure_logging() {
    log INFO "Configuring log rotation..."
    
    cat > "/etc/logrotate.d/audiodeck" << EOF
/var/log/audiodeck/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 $DEPLOY_USER $DEPLOY_USER
    postrotate
        systemctl reload $SERVICE_NAME || true
    endscript
}
EOF
    
    log SUCCESS "Log rotation configured"
}

# Configure firewall
configure_firewall() {
    log INFO "Configuring firewall..."
    
    # Ubuntu/Debian with UFW
    if command -v ufw &> /dev/null; then
        ufw allow 3001/tcp comment "AudioDeck Connect HTTP"
        ufw allow 41234/udp comment "AudioDeck Connect Discovery"
        log SUCCESS "UFW firewall rules added"
    
    # RHEL/CentOS with firewalld
    elif command -v firewall-cmd &> /dev/null; then
        firewall-cmd --permanent --add-port=3001/tcp
        firewall-cmd --permanent --add-port=41234/udp
        firewall-cmd --reload
        log SUCCESS "Firewalld rules added"
    
    # Manual iptables
    else
        log WARN "No recognized firewall. Manually configure:"
        log WARN "  - Allow TCP port 3001 for HTTP API"
        log WARN "  - Allow UDP port 41234 for discovery (optional)"
    fi
}

# Start and validate service
start_and_validate() {
    log INFO "Starting service and validating deployment..."
    
    # Start service
    systemctl start "$SERVICE_NAME"
    
    # Wait for startup
    log INFO "Waiting for service to start..."
    sleep 10
    
    # Check service status
    if ! systemctl is-active "$SERVICE_NAME" &>/dev/null; then
        error_exit "Service failed to start. Check: journalctl -u $SERVICE_NAME"
    fi
    
    # Health check
    local health_check_attempts=0
    local max_attempts=12  # 1 minute total
    
    while [[ $health_check_attempts -lt $max_attempts ]]; do
        if curl -s http://localhost:3001/health > /dev/null 2>&1; then
            log SUCCESS "Health check passed"
            break
        fi
        
        ((health_check_attempts++))
        log INFO "Health check attempt $health_check_attempts/$max_attempts..."
        sleep 5
    done
    
    if [[ $health_check_attempts -eq $max_attempts ]]; then
        error_exit "Health check failed after $max_attempts attempts"
    fi
    
    # Get detailed status
    local status_response=$(curl -s http://localhost:3001/api/status 2>/dev/null || echo '{"error":"no response"}')
    log INFO "Service status: $status_response"
    
    log SUCCESS "Service started and validated successfully"
}

# Post-deployment validation
post_deployment_validation() {
    log INFO "Running post-deployment validation..."
    
    # Check process
    local pid=$(pgrep -f audiodeck-server)
    if [[ -z "$pid" ]]; then
        error_exit "AudioDeck process not found"
    fi
    log INFO "Process running with PID: $pid"
    
    # Check memory usage
    local memory_kb=$(ps -p "$pid" -o rss --no-headers | tr -d ' ')
    local memory_mb=$((memory_kb / 1024))
    log INFO "Memory usage: ${memory_mb}MB"
    
    if [[ $memory_mb -gt 300 ]]; then
        log WARN "High memory usage: ${memory_mb}MB (>300MB)"
    fi
    
    # Check response time
    local response_time=$(curl -w "%{time_total}" -s -o /dev/null http://localhost:3001/health 2>/dev/null || echo "999")
    local response_ms=$(echo "$response_time * 1000" | bc 2>/dev/null | cut -d. -f1 || echo "999")
    log INFO "Response time: ${response_ms}ms"
    
    if [[ $response_ms -gt 100 ]]; then
        log WARN "Slow response time: ${response_ms}ms (>100ms)"
    fi
    
    # Check listening ports
    local listening_ports=$(netstat -tlnp | grep "$pid" | awk '{print $4}' | cut -d: -f2 | sort | tr '\n' ' ')
    log INFO "Listening ports: $listening_ports"
    
    # Test API endpoints
    local endpoints=("/health" "/api/status" "/api/audio/list")
    for endpoint in "${endpoints[@]}"; do
        if curl -s "http://localhost:3001$endpoint" > /dev/null; then
            log SUCCESS "Endpoint $endpoint responding"
        else
            log WARN "Endpoint $endpoint not responding"
        fi
    done
    
    log SUCCESS "Post-deployment validation completed"
}

# Setup monitoring
setup_monitoring() {
    log INFO "Setting up monitoring..."
    
    # Create monitoring scripts
    cp "$SCRIPT_DIR"/health-monitor.sh "$DEPLOY_PATH/scripts/" 2>/dev/null || true
    cp "$SCRIPT_DIR"/performance-monitor.sh "$DEPLOY_PATH/scripts/" 2>/dev/null || true
    
    # Add cron jobs
    (crontab -u root -l 2>/dev/null || true; cat << EOF
# AudioDeck Connect Monitoring
* * * * * $DEPLOY_PATH/scripts/health-monitor.sh >/dev/null 2>&1
*/5 * * * * $DEPLOY_PATH/scripts/performance-monitor.sh >/dev/null 2>&1
0 0 * * * $DEPLOY_PATH/scripts/daily-maintenance.sh >/dev/null 2>&1
EOF
    ) | crontab -u root -
    
    log SUCCESS "Monitoring setup completed"
}

# Generate deployment report
generate_report() {
    log INFO "Generating deployment report..."
    
    local report_file="/tmp/audiodeck-deployment-report-$(date +%Y%m%d-%H%M%S).txt"
    
    cat > "$report_file" << EOF
AudioDeck Connect - Deployment Report
=====================================
Date: $(date)
Version: 9.0
Deployment Path: $DEPLOY_PATH
Service Name: $SERVICE_NAME

Deployment Status: SUCCESS

System Information:
- OS: $(uname -o)
- Kernel: $(uname -r)
- Architecture: $(uname -m)
- Memory: $(free -h | awk 'NR==2{print $2}')
- Disk Space: $(df -h $DEPLOY_PATH | awk 'NR==2{print $4}' || echo "Unknown")

Service Information:
- User: $DEPLOY_USER
- PID: $(pgrep -f audiodeck-server || echo "Not running")
- Memory Usage: $(ps -p $(pgrep -f audiodeck-server) -o rss --no-headers 2>/dev/null | awk '{print int($1/1024)"MB"}' || echo "Unknown")
- Status: $(systemctl is-active $SERVICE_NAME 2>/dev/null || echo "Unknown")

Network Information:
- Listening Ports: $(netstat -tlnp | grep audiodeck-server | awk '{print $4}' | cut -d: -f2 | sort | tr '\n' ' ' || echo "None")
- Health Check: $(curl -s http://localhost:3001/health > /dev/null && echo "PASS" || echo "FAIL")

Files Deployed:
- Executable: $DEPLOY_PATH/bin/audiodeck-server ($(ls -lh $DEPLOY_PATH/bin/audiodeck-server | awk '{print $5}'))
- Configuration: $DEPLOY_PATH/config/.env
- Service: /etc/systemd/system/$SERVICE_NAME.service
- Log Directory: /var/log/audiodeck/

Security:
- Service User: $DEPLOY_USER (system account)
- File Permissions: Configured for least privilege
- Firewall: Configured for ports 3001/tcp, 41234/udp

Monitoring:
- Health checks: Enabled (every minute)
- Performance monitoring: Enabled (every 5 minutes)
- Log rotation: Configured (daily)

Next Steps:
1. Verify application functionality
2. Configure monitoring alerts
3. Schedule regular maintenance
4. Update documentation

For troubleshooting, check:
- Service logs: journalctl -u $SERVICE_NAME
- Application logs: tail -f /var/log/audiodeck/application.log
- Health status: curl http://localhost:3001/health
EOF
    
    log SUCCESS "Deployment report generated: $report_file"
    cat "$report_file"
}

# Rollback function
rollback() {
    log ERROR "Initiating rollback..."
    
    local backup_file
    if [[ -f "$DEPLOY_PATH/.last-backup" ]]; then
        backup_file=$(cat "$DEPLOY_PATH/.last-backup")
    else
        log ERROR "No backup file found for rollback"
        return 1
    fi
    
    if [[ ! -f "$backup_file" ]]; then
        log ERROR "Backup file not found: $backup_file"
        return 1
    fi
    
    log INFO "Rolling back to: $backup_file"
    
    # Stop service
    systemctl stop "$SERVICE_NAME" || true
    
    # Restore from backup
    tar -xzf "$backup_file" -C / 2>/dev/null || true
    
    # Restart service
    systemctl daemon-reload
    systemctl start "$SERVICE_NAME"
    
    log SUCCESS "Rollback completed"
}

# Main deployment function
main() {
    log INFO "Starting AudioDeck Connect production deployment..."
    log INFO "Log file: $LOG_FILE"
    
    # Trap errors for rollback
    trap 'rollback; exit 1' ERR
    
    check_root
    pre_deployment_checks
    setup_environment
    backup_current_deployment
    deploy_application
    configure_service
    configure_logging
    configure_firewall
    start_and_validate
    post_deployment_validation
    setup_monitoring
    generate_report
    
    log SUCCESS "AudioDeck Connect deployment completed successfully!"
    log INFO "Service is running at: http://localhost:3001"
    log INFO "Health check: curl http://localhost:3001/health"
    log INFO "Service logs: journalctl -u $SERVICE_NAME -f"
}

# Handle command line arguments
case "${1:-deploy}" in
    "deploy"|"")
        main
        ;;
    "rollback")
        rollback
        ;;
    "validate")
        post_deployment_validation
        ;;
    "report")
        generate_report
        ;;
    *)
        echo "Usage: $0 {deploy|rollback|validate|report}"
        echo "  deploy   - Deploy AudioDeck Connect (default)"
        echo "  rollback - Rollback to previous version"
        echo "  validate - Validate current deployment"
        echo "  report   - Generate deployment report"
        exit 1
        ;;
esac