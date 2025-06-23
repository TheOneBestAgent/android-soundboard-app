package com.soundboard.android.di

import com.soundboard.android.diagnostics.DiagnosticsManager
import com.soundboard.android.diagnostics.LoggingManager
import com.soundboard.android.diagnostics.PerformanceTuner
import com.soundboard.android.diagnostics.AlertingSystem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
object DiagnosticsModule {
    
    @Provides
    @Singleton
    fun provideDiagnosticsManager(): DiagnosticsManager {
        return DiagnosticsManager()
    }
    
    @Provides
    @Singleton
    fun provideLoggingManager(): LoggingManager {
        return LoggingManager()
    }
    
    @Provides
    @Singleton
    fun providePerformanceTuner(
        diagnosticsManager: DiagnosticsManager,
        loggingManager: LoggingManager
    ): PerformanceTuner {
        return PerformanceTuner(diagnosticsManager, loggingManager)
    }
    
    @Provides
    @Singleton
    fun provideAlertingSystem(
        diagnosticsManager: DiagnosticsManager,
        loggingManager: LoggingManager,
        performanceTuner: PerformanceTuner
    ): AlertingSystem {
        return AlertingSystem(diagnosticsManager, loggingManager, performanceTuner)
    }
} 