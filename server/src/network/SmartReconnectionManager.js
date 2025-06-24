const EventEmitter = require('events');

class SmartReconnectionManager extends EventEmitter {
    constructor() {
        super();
        this.clientReconnectionStates = new Map();
        this.globalReconnectionStats = {
            totalReconnectionAttempts: 0,
            successfulReconnections: 0,
            failedReconnections: 0,
            averageReconnectionTime: 0
        };
    }
    
    // Analyze disconnection cause and recommend strategy
    analyzeDisconnectionCause(socketId, reason, connectionHistory = {}) {
        const analysis = {
            cause: this.categorizeCause(reason),
            severity: this.calculateSeverity(reason, connectionHistory),
            recoverability: this.assessRecoverability(reason, connectionHistory),
            recommendedStrategy: null,
            backoffMultiplier: 1,
            maxAttempts: 10,
            contextualFactors: []
        };
        
        // Determine recommended strategy based on cause
        switch (analysis.cause) {
            case 'NETWORK_TIMEOUT':
                analysis.recommendedStrategy = 'EXPONENTIAL_BACKOFF';
                analysis.backoffMultiplier = 1.5;
                analysis.maxAttempts = 8;
                analysis.contextualFactors.push('network_instability');
                break;
                
            case 'SERVER_SHUTDOWN':
                analysis.recommendedStrategy = 'LINEAR_BACKOFF';
                analysis.backoffMultiplier = 2.0;
                analysis.maxAttempts = 5;
                analysis.contextualFactors.push('server_maintenance');
                break;
                
            case 'TRANSPORT_ERROR':
                analysis.recommendedStrategy = 'TRANSPORT_SWITCH';
                analysis.backoffMultiplier = 0.5; // Quick retry with different transport
                analysis.maxAttempts = 6;
                analysis.contextualFactors.push('transport_instability');
                break;
                
            case 'AUTHENTICATION_FAILURE':
                analysis.recommendedStrategy = 'USER_PROMPT';
                analysis.backoffMultiplier = 0;
                analysis.maxAttempts = 0;
                analysis.contextualFactors.push('auth_issue');
                break;
                
            case 'RESOURCE_EXHAUSTION':
                analysis.recommendedStrategy = 'ADAPTIVE_TIMING';
                analysis.backoffMultiplier = 3.0;
                analysis.maxAttempts = 4;
                analysis.contextualFactors.push('resource_pressure');
                break;
                
            case 'USER_INITIATED':
                analysis.recommendedStrategy = 'USER_PROMPT';
                analysis.backoffMultiplier = 0;
                analysis.maxAttempts = 0;
                analysis.contextualFactors.push('manual_disconnect');
                break;
                
            default:
                analysis.recommendedStrategy = 'ADAPTIVE_TIMING';
                analysis.backoffMultiplier = 1.2;
                analysis.maxAttempts = 6;
        }
        
        // Adjust based on connection history
        this.adjustForHistory(analysis, connectionHistory);
        
        console.log(`🧠 Disconnection analysis for ${socketId}:`, {
            cause: analysis.cause,
            severity: analysis.severity,
            strategy: analysis.recommendedStrategy,
            maxAttempts: analysis.maxAttempts
        });
        
        return analysis;
    }
    
    // Categorize disconnection causes
    categorizeCause(reason) {
        const causeMap = {
            'ping timeout': 'NETWORK_TIMEOUT',
            'transport close': 'TRANSPORT_ERROR',
            'transport error': 'TRANSPORT_ERROR',
            'client namespace disconnect': 'USER_INITIATED',
            'io server disconnect': 'SERVER_SHUTDOWN',
            'connection timeout': 'NETWORK_TIMEOUT',
            'server error': 'SERVER_SHUTDOWN',
            'auth failed': 'AUTHENTICATION_FAILURE',
            'resource limit': 'RESOURCE_EXHAUSTION'
        };
        
        const lowerReason = reason.toLowerCase();
        for (const [key, cause] of Object.entries(causeMap)) {
            if (lowerReason.includes(key)) {
                return cause;
            }
        }
        
        return 'UNKNOWN';
    }
    
    // Calculate severity level
    calculateSeverity(reason, connectionHistory) {
        let severity = 'MEDIUM';
        
        if (reason.includes('timeout') || reason.includes('error')) {
            severity = 'HIGH';
        } else if (reason.includes('client') || reason.includes('user')) {
            severity = 'LOW';
        }
        
        // Adjust based on recent failures
        if (connectionHistory.recentFailures > 3) {
            severity = 'HIGH';
        }
        
        return severity;
    }
    
    // Assess how likely the connection is to recover
    assessRecoverability(reason, connectionHistory) {
        if (reason.includes('auth') || reason.includes('user')) {
            return 'LOW';
        }
        
        if (reason.includes('server') && connectionHistory.serverRestarts > 0) {
            return 'MEDIUM';
        }
        
        if (reason.includes('timeout') || reason.includes('transport')) {
            return 'HIGH';
        }
        
        return 'MEDIUM';
    }
    
    // Adjust strategy based on connection history
    adjustForHistory(analysis, history) {
        // If we've had many recent failures, be more conservative
        if (history.recentFailures > 5) {
            analysis.backoffMultiplier *= 1.5;
            analysis.maxAttempts = Math.max(3, analysis.maxAttempts - 2);
        }
        
        // If we've had successful long connections, be more aggressive
        if (history.longestConnection > 300000) { // 5 minutes
            analysis.backoffMultiplier *= 0.8;
            analysis.maxAttempts += 2;
        }
        
        // If network type is unreliable, be more patient
        if (history.networkType === 'mobile') {
            analysis.backoffMultiplier *= 1.3;
            analysis.contextualFactors.push('mobile_network');
        }
    }
    
    // Generate reconnection schedule based on strategy
    generateReconnectionSchedule(analysis, baseDelay = 1000) {
        const schedule = [];
        let currentDelay = baseDelay;
        
        for (let attempt = 1; attempt <= analysis.maxAttempts; attempt++) {
            switch (analysis.recommendedStrategy) {
                case 'IMMEDIATE_RETRY':
                    schedule.push({
                        attempt,
                        delay: 100,
                        transport: 'websocket'
                    });
                    break;
                    
                case 'EXPONENTIAL_BACKOFF':
                    schedule.push({
                        attempt,
                        delay: currentDelay,
                        transport: 'websocket'
                    });
                    currentDelay = Math.min(currentDelay * analysis.backoffMultiplier, 30000);
                    break;
                    
                case 'LINEAR_BACKOFF':
                    schedule.push({
                        attempt,
                        delay: baseDelay * attempt * analysis.backoffMultiplier,
                        transport: 'websocket'
                    });
                    break;
                    
                case 'ADAPTIVE_TIMING':
                    // Adaptive timing based on network conditions
                    const adaptiveDelay = this.calculateAdaptiveDelay(attempt, analysis);
                    schedule.push({
                        attempt,
                        delay: adaptiveDelay,
                        transport: 'websocket',
                        adaptive: true
                    });
                    break;
                    
                case 'TRANSPORT_SWITCH':
                    // Try different transports
                    const transports = ['websocket', 'polling'];
                    schedule.push({
                        attempt,
                        delay: currentDelay,
                        transport: transports[attempt % transports.length]
                    });
                    currentDelay *= 1.2;
                    break;
                    
                default:
                    schedule.push({
                        attempt,
                        delay: currentDelay,
                        transport: 'websocket'
                    });
                    currentDelay *= 1.5;
            }
        }
        
        return schedule;
    }
    
    // Calculate adaptive delay based on current conditions
    calculateAdaptiveDelay(attempt, analysis) {
        let baseDelay = 1000;
        
        // Adjust based on contextual factors
        if (analysis.contextualFactors.includes('network_instability')) {
            baseDelay *= 2;
        }
        
        if (analysis.contextualFactors.includes('resource_pressure')) {
            baseDelay *= 3;
        }
        
        if (analysis.contextualFactors.includes('mobile_network')) {
            baseDelay *= 1.5;
        }
        
        // Apply attempt multiplier
        return Math.min(baseDelay * Math.pow(1.4, attempt - 1), 30000);
    }
    
    // Track reconnection attempt
    trackReconnectionAttempt(socketId, attempt, success, duration) {
        let clientState = this.clientReconnectionStates.get(socketId);
        if (!clientState) {
            clientState = {
                attempts: 0,
                successes: 0,
                failures: 0,
                totalDuration: 0,
                lastAttempt: null,
                patterns: []
            };
            this.clientReconnectionStates.set(socketId, clientState);
        }
        
        clientState.attempts++;
        clientState.totalDuration += duration;
        clientState.lastAttempt = {
            attempt,
            success,
            duration,
            timestamp: Date.now()
        };
        
        if (success) {
            clientState.successes++;
            this.globalReconnectionStats.successfulReconnections++;
        } else {
            clientState.failures++;
            this.globalReconnectionStats.failedReconnections++;
        }
        
        this.globalReconnectionStats.totalReconnectionAttempts++;
        this.globalReconnectionStats.averageReconnectionTime = 
            (this.globalReconnectionStats.averageReconnectionTime + duration) / 2;
        
        // Analyze patterns
        clientState.patterns.push({
            attempt,
            success,
            duration,
            timestamp: Date.now()
        });
        
        // Keep only recent patterns
        if (clientState.patterns.length > 20) {
            clientState.patterns.shift();
        }
        
        console.log(`🔄 Reconnection tracked for ${socketId}: attempt ${attempt}, success: ${success}, duration: ${duration}ms`);
        
        this.emit('reconnectionTracked', {
            socketId,
            attempt,
            success,
            duration,
            clientState: { ...clientState }
        });
    }
    
    // Get reconnection recommendations
    getReconnectionRecommendations(socketId) {
        const clientState = this.clientReconnectionStates.get(socketId);
        if (!clientState) return [];
        
        const recommendations = [];
        
        // Analyze recent patterns
        const recentPatterns = clientState.patterns.slice(-10);
        const successRate = recentPatterns.filter(p => p.success).length / recentPatterns.length;
        
        if (successRate < 0.3) {
            recommendations.push({
                type: 'connection_method',
                message: 'Consider switching connection method or checking network',
                priority: 'high'
            });
        }
        
        if (clientState.failures > clientState.successes * 2) {
            recommendations.push({
                type: 'backoff_strategy',
                message: 'Increase backoff delays to reduce connection pressure',
                priority: 'medium'
            });
        }
        
        const avgDuration = clientState.totalDuration / clientState.attempts;
        if (avgDuration > 10000) {
            recommendations.push({
                type: 'timeout_adjustment',
                message: 'Connection timeouts may be too aggressive',
                priority: 'low'
            });
        }
        
        return recommendations;
    }
    
    // Predict reconnection success probability
    predictReconnectionSuccess(socketId, proposedStrategy) {
        const clientState = this.clientReconnectionStates.get(socketId);
        if (!clientState || clientState.patterns.length < 3) {
            return 0.7; // Default probability
        }
        
        const recentPatterns = clientState.patterns.slice(-5);
        const baseSuccessRate = recentPatterns.filter(p => p.success).length / recentPatterns.length;
        
        // Adjust based on proposed strategy
        let strategyMultiplier = 1.0;
        
        switch (proposedStrategy) {
            case 'IMMEDIATE_RETRY':
                strategyMultiplier = 0.8; // Less likely to succeed
                break;
            case 'EXPONENTIAL_BACKOFF':
                strategyMultiplier = 1.2; // More likely to succeed
                break;
            case 'TRANSPORT_SWITCH':
                strategyMultiplier = 1.1; // Slightly better
                break;
            case 'ADAPTIVE_TIMING':
                strategyMultiplier = 1.3; // Best strategy
                break;
        }
        
        return Math.min(1.0, baseSuccessRate * strategyMultiplier);
    }
    
    // Clean up old client states
    cleanup() {
        const cutoffTime = Date.now() - (24 * 60 * 60 * 1000); // 24 hours
        
        for (const [socketId, state] of this.clientReconnectionStates.entries()) {
            if (state.lastAttempt && state.lastAttempt.timestamp < cutoffTime) {
                this.clientReconnectionStates.delete(socketId);
                console.log(`🧹 Cleaned up old reconnection state for ${socketId}`);
            }
        }
    }
    
    // Get global statistics
    getGlobalStats() {
        return {
            ...this.globalReconnectionStats,
            activeClients: this.clientReconnectionStates.size,
            lastUpdated: Date.now()
        };
    }
    
    // Reset client state
    resetClientState(socketId) {
        this.clientReconnectionStates.delete(socketId);
        console.log(`🔄 Reset reconnection state for ${socketId}`);
    }
}

module.exports = SmartReconnectionManager; 