package com.soundboard.android.di

import android.content.Context
import com.soundboard.android.diagnostics.*
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Lazy

/**
 * DiagnosticsModule - Provides Phase 4.3 Advanced Diagnostics & Monitoring components
 * 
 * Manages dependency injection for the comprehensive diagnostic system including:
 * - DiagnosticsManager: Core health monitoring and bottleneck detection
 * - LoggingManager: Advanced logging with pattern analysis
 * - PerformanceTuner: Automated optimization engine
 * - AlertingSystem: Proactive alerting and notification management
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {
    
    companion object {
        @Provides
        @Singleton
        fun provideLoggingManager(
            @ApplicationContext context: Context
        ): LoggingManager {
            return LoggingManager(context)
        }
        
        @Provides
        @Singleton
        fun provideDiagnosticsManager(
            @ApplicationContext context: Context,
            loggingProvider: Lazy<LoggingProvider>
        ): DiagnosticsManager {
            return DiagnosticsManager(context, loggingProvider)
        }
        
        @Provides
        @Singleton
        fun providePerformanceTuner(
            @ApplicationContext context: Context,
            diagnosticsProvider: Lazy<DiagnosticsProvider>,
            loggingProvider: Lazy<LoggingProvider>
        ): PerformanceTuner {
            return PerformanceTuner(context, diagnosticsProvider, loggingProvider)
        }
        
        @Provides
        @Singleton
        fun provideAlertingSystem(
            @ApplicationContext context: Context,
            diagnosticsProvider: Lazy<DiagnosticsProvider>,
            loggingProvider: Lazy<LoggingProvider>,
            performanceProvider: Lazy<PerformanceProvider>
        ): AlertingSystem {
            return AlertingSystem(context, diagnosticsProvider, loggingProvider, performanceProvider)
        }
    }

    @Binds
    @Singleton
    abstract fun bindLoggingProvider(loggingManager: LoggingManager): LoggingProvider

    @Binds
    @Singleton
    abstract fun bindDiagnosticsProvider(diagnosticsManager: DiagnosticsManager): DiagnosticsProvider

    @Binds
    @Singleton
    abstract fun bindPerformanceProvider(performanceTuner: PerformanceTuner): PerformanceProvider

    @Binds
    @Singleton
    abstract fun bindAlertingProvider(alertingSystem: AlertingSystem): AlertingProvider
} 