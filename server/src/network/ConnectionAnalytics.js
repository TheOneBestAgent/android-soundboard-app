class ConnectionAnalytics {
    constructor() {
        this.connectionHistory = [];
        this.connectionStats = {
            successfulConnections: 0,
            failedConnections: 0,
            averageDuration: 0,
        };
        console.log('📈 ConnectionAnalytics initialized');
    }

    logConnection(status, duration) {
        const timestamp = new Date();
        this.connectionHistory.push({ status, duration, timestamp });

        if (status === 'connected') {
            this.connectionStats.successfulConnections++;
        } else {
            this.connectionStats.failedConnections++;
        }

        this.updateAverageDuration();
    }

    updateAverageDuration() {
        const successfulConnections = this.connectionHistory.filter(c => c.status === 'connected');
        if (successfulConnections.length > 0) {
            const totalDuration = successfulConnections.reduce((sum, c) => sum + c.duration, 0);
            this.connectionStats.averageDuration = totalDuration / successfulConnections.length;
        }
    }

    getStats() {
        return this.connectionStats;
    }

    getHistory() {
        return this.connectionHistory;
    }
}

module.exports = ConnectionAnalytics; 