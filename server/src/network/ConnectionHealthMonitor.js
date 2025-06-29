import EventEmitter from 'events';

class ConnectionHealthMonitor extends EventEmitter {
    constructor() {
        super();
        this.connections = new Map();
        this.globalMetrics = {
            totalConnections: 0,
            successfulConnections: 0,
            failedConnections: 0,
            avgConnectionDuration: 0,
            avgLatency: 0,
            transportErrorRate: 0,
            lastUpdated: Date.now()
        };
        
        // Health prediction configuration
        this.healthThresholds = {
            excellent: { latency: 30, errorRate: 0.01, stability: 0.95 },
            good: { latency: 60, errorRate: 0.05, stability: 0.85 },
            fair: { latency: 120, errorRate: 0.15, stability: 0.70 },
            poor: { latency: Infinity, errorRate: 1.0, stability: 0 }
        };
        
        // Monitoring intervals
        this.healthCheckInterval = null;
        this.metricsUpdateInterval = null;
        
        this.startMonitoring();
    }
    
    // Enhanced connection tracking with predictive analytics
    trackConnection(socketId, clientInfo = {}) {
        const connection = {
            id: socketId,
            startTime: Date.now(),
            lastPing: Date.now(),
            pingHistory: [],
            transportUpgrades: 0,
            errors: [],
            quality: 'excellent',
            predictedStability: 0.95,
            riskFactors: [],
            clientInfo,
            transport: clientInfo.transport || 'unknown',
            events: []
        };
        
        this.connections.set(socketId, connection);
        this.globalMetrics.totalConnections++;
        
        console.log(`📊 Connection tracking started for ${socketId} (transport: ${connection.transport})`);
        this.emit('connectionStarted', connection);
        
        return connection;
    }
    
    // Record latency and predict connection health
    recordLatency(socketId, latency) {
        const connection = this.connections.get(socketId);
        if (!connection) return;
        
        connection.lastPing = Date.now();
        connection.pingHistory.push({
            timestamp: Date.now(),
            latency: latency
        });
        
        // Keep only last 50 pings for analysis
        if (connection.pingHistory.length > 50) {
            connection.pingHistory.shift();
        }
        
        // Update connection quality based on latency trends
        this.updateConnectionQuality(connection);
        
        // Predict potential issues
        this.predictConnectionHealth(connection);
        
        console.log(`📡 Latency recorded for ${socketId}: ${latency}ms (quality: ${connection.quality})`);
    }
    
    // Transport upgrade tracking
    recordTransportUpgrade(socketId, fromTransport, toTransport) {
        const connection = this.connections.get(socketId);
        if (!connection) return;
        
        connection.transportUpgrades++;
        connection.transport = toTransport;
        connection.events.push({
            type: 'transport_upgrade',
            timestamp: Date.now(),
            from: fromTransport,
            to: toTransport
        });
        
        console.log(`🚀 Transport upgrade recorded for ${socketId}: ${fromTransport} → ${toTransport}`);
        this.emit('transportUpgrade', { socketId, fromTransport, toTransport });
    }
    
    // Error tracking with pattern analysis
    recordError(socketId, errorType, errorDetails) {
        const connection = this.connections.get(socketId);
        if (!connection) return;
        
        const error = {
            type: errorType,
            details: errorDetails,
            timestamp: Date.now()
        };
        
        connection.errors.push(error);
        connection.events.push({
            type: 'error',
            timestamp: Date.now(),
            errorType,
            errorDetails
        });
        
        // Keep only last 20 errors
        if (connection.errors.length > 20) {
            connection.errors.shift();
        }
        
        this.analyzeErrorPatterns(connection);
        console.log(`❌ Error recorded for ${socketId}: ${errorType} - ${errorDetails}`);
        this.emit('connectionError', { socketId, error });
    }
    
    // Predictive health analysis
    predictConnectionHealth(connection) {
        const now = Date.now();
        const recentPings = connection.pingHistory.filter(p => now - p.timestamp < 60000); // Last minute
        
        if (recentPings.length === 0) return;
        
        // Calculate trends
        const avgLatency = recentPings.reduce((sum, p) => sum + p.latency, 0) / recentPings.length;
        const latencyTrend = this.calculateLatencyTrend(recentPings);
        const errorRate = this.calculateRecentErrorRate(connection);
        const stabilityScore = this.calculateStabilityScore(connection);
        
        // Risk factor analysis
        const riskFactors = [];
        
        if (latencyTrend > 10) {
            riskFactors.push('increasing_latency');
        }
        
        if (errorRate > 0.1) {
            riskFactors.push('high_error_rate');
        }
        
        if (stabilityScore < 0.8) {
            riskFactors.push('connection_instability');
        }
        
        if (connection.transportUpgrades > 3) {
            riskFactors.push('frequent_transport_changes');
        }
        
        // Predict stability for next 5 minutes
        const predictedStability = Math.max(0, Math.min(1, 
            stabilityScore - (latencyTrend * 0.01) - (errorRate * 0.5)
        ));
        
        connection.riskFactors = riskFactors;
        connection.predictedStability = predictedStability;
        
        // Generate recommendations
        const recommendations = this.generateRecommendations(connection);
        
        // Emit prediction update
        this.emit('healthPrediction', {
            socketId: connection.id,
            avgLatency,
            latencyTrend,
            errorRate,
            stabilityScore,
            predictedStability,
            riskFactors,
            recommendations
        });
        
        // Trigger preemptive actions if needed
        if (predictedStability < 0.5) {
            this.triggerPreemptiveHealing(connection);
        }
    }
    
    // Generate intelligent recommendations
    generateRecommendations(connection) {
        const recommendations = [];
        
        if (connection.riskFactors.includes('increasing_latency')) {
            recommendations.push({
                type: 'optimize_transport',
                message: 'Consider switching to a more stable transport method',
                priority: 'medium'
            });
        }
        
        if (connection.riskFactors.includes('high_error_rate')) {
            recommendations.push({
                type: 'connection_reset',
                message: 'Connection may benefit from a restart',
                priority: 'high'
            });
        }
        
        if (connection.riskFactors.includes('connection_instability')) {
            recommendations.push({
                type: 'buffer_adjustment',
                message: 'Adjust buffer sizes for better stability',
                priority: 'low'
            });
        }
        
        return recommendations;
    }
    
    // Preemptive healing actions
    triggerPreemptiveHealing(connection) {
        console.log(`🩹 Triggering preemptive healing for ${connection.id}`);
        
        // Example healing actions
        this.emit('preemptiveHealing', {
            socketId: connection.id,
            actions: [
                'clear_buffers',
                'optimize_transport',
                'adjust_timeouts'
            ],
            reason: 'predicted_instability'
        });
    }
    
    // Connection quality assessment
    updateConnectionQuality(connection) {
        const recentPings = connection.pingHistory.slice(-10); // Last 10 pings
        if (recentPings.length === 0) return;
        
        const avgLatency = recentPings.reduce((sum, p) => sum + p.latency, 0) / recentPings.length;
        const errorRate = this.calculateRecentErrorRate(connection);
        const stability = this.calculateStabilityScore(connection);
        
        if (avgLatency <= this.healthThresholds.excellent.latency && 
            errorRate <= this.healthThresholds.excellent.errorRate && 
            stability >= this.healthThresholds.excellent.stability) {
            connection.quality = 'excellent';
        } else if (avgLatency <= this.healthThresholds.good.latency && 
                   errorRate <= this.healthThresholds.good.errorRate && 
                   stability >= this.healthThresholds.good.stability) {
            connection.quality = 'good';
        } else if (avgLatency <= this.healthThresholds.fair.latency && 
                   errorRate <= this.healthThresholds.fair.errorRate && 
                   stability >= this.healthThresholds.fair.stability) {
            connection.quality = 'fair';
        } else {
            connection.quality = 'poor';
        }
    }
    
    // Calculate latency trend (positive = increasing, negative = decreasing)
    calculateLatencyTrend(pings) {
        if (pings.length < 5) return 0;
        
        const recent = pings.slice(-5);
        const older = pings.slice(-10, -5);
        
        if (older.length === 0) return 0;
        
        const recentAvg = recent.reduce((sum, p) => sum + p.latency, 0) / recent.length;
        const olderAvg = older.reduce((sum, p) => sum + p.latency, 0) / older.length;
        
        return recentAvg - olderAvg;
    }
    
    // Calculate recent error rate
    calculateRecentErrorRate(connection) {
        const now = Date.now();
        const recentErrors = connection.errors.filter(e => now - e.timestamp < 60000); // Last minute
        const recentEvents = connection.events.filter(e => now - e.timestamp < 60000);
        
        if (recentEvents.length === 0) return 0;
        return recentErrors.length / recentEvents.length;
    }
    
    // Calculate connection stability score
    calculateStabilityScore(connection) {
        const now = Date.now();
        const connectionDuration = now - connection.startTime;
        const errors = connection.errors.length;
        const transportChanges = connection.transportUpgrades;
        
        // Base stability on uptime, errors, and transport changes
        let stability = 1.0;
        
        // Reduce stability based on errors
        stability -= (errors * 0.05);
        
        // Reduce stability based on transport changes
        stability -= (transportChanges * 0.1);
        
        // Increase stability based on connection duration (longer = more stable)
        const durationBonus = Math.min(0.2, connectionDuration / (5 * 60 * 1000)); // Max 0.2 bonus for 5+ minutes
        stability += durationBonus;
        
        return Math.max(0, Math.min(1, stability));
    }
    
    // Analyze error patterns
    analyzeErrorPatterns(connection) {
        const recentErrors = connection.errors.slice(-10);
        const patterns = {};
        
        recentErrors.forEach(error => {
            patterns[error.type] = (patterns[error.type] || 0) + 1;
        });
        
        // Detect concerning patterns
        Object.entries(patterns).forEach(([errorType, count]) => {
            if (count >= 3) {
                console.log(`⚠️ Error pattern detected for ${connection.id}: ${errorType} occurred ${count} times recently`);
                this.emit('errorPattern', { socketId: connection.id, errorType, count });
            }
        });
    }
    
    // End connection tracking
    endConnection(socketId, reason = 'unknown') {
        const connection = this.connections.get(socketId);
        if (!connection) return;
        
        const duration = Date.now() - connection.startTime;
        connection.endTime = Date.now();
        connection.duration = duration;
        connection.disconnectReason = reason;
        
        // Update global metrics
        if (connection.errors.length === 0) {
            this.globalMetrics.successfulConnections++;
        } else {
            this.globalMetrics.failedConnections++;
        }
        
        // Update average connection duration
        const totalSessions = this.globalMetrics.successfulConnections + this.globalMetrics.failedConnections;
        this.globalMetrics.avgConnectionDuration = (
            (this.globalMetrics.avgConnectionDuration * (totalSessions - 1)) + duration
        ) / totalSessions;
        
        this.connections.delete(socketId);
        
        console.log(`📊 Connection tracking ended for ${socketId} (duration: ${(duration/1000).toFixed(1)}s, reason: ${reason})`);
        this.emit('connectionEnded', connection);
    }
    
    // Get connection analytics
    getConnectionAnalytics(socketId) {
        const connection = this.connections.get(socketId);
        if (!connection) return null;
        
        const recentPings = connection.pingHistory.slice(-10);
        const avgLatency = recentPings.length > 0 ? 
            recentPings.reduce((sum, p) => sum + p.latency, 0) / recentPings.length : 0;
        
        return {
            id: connection.id,
            duration: Date.now() - connection.startTime,
            quality: connection.quality,
            avgLatency,
            errorCount: connection.errors.length,
            transportUpgrades: connection.transportUpgrades,
            predictedStability: connection.predictedStability,
            riskFactors: connection.riskFactors,
            transport: connection.transport
        };
    }
    
    // Get global analytics
    getGlobalAnalytics() {
        const activeConnections = Array.from(this.connections.values());
        const totalLatency = activeConnections.reduce((sum, conn) => {
            const recentPings = conn.pingHistory.slice(-5);
            const avgLatency = recentPings.length > 0 ? 
                recentPings.reduce((s, p) => s + p.latency, 0) / recentPings.length : 0;
            return sum + avgLatency;
        }, 0);
        
        return {
            ...this.globalMetrics,
            activeConnections: activeConnections.length,
            avgCurrentLatency: activeConnections.length > 0 ? totalLatency / activeConnections.length : 0,
            connectionQualities: activeConnections.reduce((qualities, conn) => {
                qualities[conn.quality] = (qualities[conn.quality] || 0) + 1;
                return qualities;
            }, {}),
            lastUpdated: Date.now()
        };
    }
    
    // Start monitoring
    startMonitoring() {
        // Health check every 30 seconds
        this.healthCheckInterval = setInterval(() => {
            this.performHealthCheck();
        }, 30000);
        
        // Update metrics every 60 seconds
        this.metricsUpdateInterval = setInterval(() => {
            this.updateGlobalMetrics();
        }, 60000);
        
        console.log('📊 Connection health monitoring started');
    }
    
    // Perform regular health checks
    performHealthCheck() {
        this.connections.forEach((connection, socketId) => {
            const timeSinceLastPing = Date.now() - connection.lastPing;
            
            if (timeSinceLastPing > 60000) { // No ping in last minute
                console.log(`⚠️ Health check warning: ${socketId} hasn't pinged in ${timeSinceLastPing}ms`);
                this.emit('healthWarning', { socketId, timeSinceLastPing });
            }
            
            // Run prediction if we have enough data
            if (connection.pingHistory.length >= 5) {
                this.predictConnectionHealth(connection);
            }
        });
    }
    
    // Update global metrics
    updateGlobalMetrics() {
        this.globalMetrics.lastUpdated = Date.now();
        this.emit('metricsUpdated', this.globalMetrics);
    }
    
    // Stop monitoring
    stopMonitoring() {
        if (this.healthCheckInterval) {
            clearInterval(this.healthCheckInterval);
        }
        if (this.metricsUpdateInterval) {
            clearInterval(this.metricsUpdateInterval);
        }
        console.log('📊 Connection health monitoring stopped');
    }
}

export default ConnectionHealthMonitor; 