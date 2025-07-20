# Monitoring

## Monitoring Stack

```yaml
Metrics Collection:
  - Local Metrics: Desktop server performance metrics
  - Custom Metrics: Application-specific KPIs
  - Real User Monitoring: Frontend performance
  - System Monitoring: Desktop system resource usage

Logging:
  - Structured Logging: JSON format with correlation IDs
  - Log Files: Local file-based logging with rotation
  - Log Analysis: Built-in log viewer and search
  - Error Tracking: Local error aggregation and reporting

Alerting:
  - System Notifications: Desktop notifications for issues
  - Email Alerts: SMTP-based notification delivery
  - System Tray: Visual indicators for system status
  - Log Alerts: File-based alert triggers

Dashboards:
  - Built-in Dashboard: Desktop server status overview
  - System Tray: Quick status indicators
  - Mobile Analytics: App usage and performance
```

## Key Metrics

```typescript
// Application metrics
interface ApplicationMetrics {
  // Performance
  apiResponseTime: number;        // Average API response time
  audioPlaybackLatency: number;   // Audio playback start latency
  appStartupTime: number;         // Mobile app cold start time
  
  // Usage
  activeUsers: number;            // Daily/Monthly active users
  soundboardsCreated: number;     // New soundboards per day
  audioPlaysPerDay: number;       // Total audio plays
  
  // Business
  subscriptionConversions: number; // Free to paid conversions
  churnRate: number;              // User churn percentage
  revenuePerUser: number;         // Average revenue per user
  
  // Technical
  errorRate: number;              // API error rate percentage
  crashRate: number;              // Mobile app crash rate
  databaseConnections: number;    // Active DB connections
  
  // Infrastructure
  cpuUtilization: number;         // Desktop server CPU usage
  memoryUtilization: number;      // Desktop server memory usage
  diskUsage: number;              // Local storage utilization
}

// Metrics collection service
class MetricsService {
  private metricsDb: Database;
  private logFile: string;
  
  async recordMetric(
    metricName: string,
    value: number,
    unit: string = 'Count',
    dimensions?: { [key: string]: string }
  ) {
    const metric = {
      namespace: 'SoundboardApp',
      metricName,
      value,
      unit,
      dimensions: JSON.stringify(dimensions || {}),
      timestamp: new Date().toISOString(),
    };
    
    // Store in local SQLite database
    await this.metricsDb.run(
      'INSERT INTO metrics (namespace, metric_name, value, unit, dimensions, timestamp) VALUES (?, ?, ?, ?, ?, ?)',
      [metric.namespace, metric.metricName, metric.value, metric.unit, metric.dimensions, metric.timestamp]
    );
    
    // Also log to file for backup
    await this.writeToLogFile(metric);
  }
  
  async recordApiCall(endpoint: string, statusCode: number, duration: number) {
    await Promise.all([
      this.recordMetric('ApiCalls', 1, 'Count', {
        Endpoint: endpoint,
        StatusCode: statusCode.toString(),
      }),
      this.recordMetric('ApiResponseTime', duration, 'Milliseconds', {
        Endpoint: endpoint,
      }),
    ]);
  }
  
  async recordAudioPlayback(userId: string, audioId: string, latency: number) {
    await Promise.all([
      this.recordMetric('AudioPlays', 1, 'Count'),
      this.recordMetric('AudioPlaybackLatency', latency, 'Milliseconds'),
    ]);
  }
}
```

---
