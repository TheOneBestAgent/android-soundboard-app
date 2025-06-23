package com.soundboard.android.di

import android.content.Context
import com.soundboard.android.diagnostics.DiagnosticsManager
import com.soundboard.android.diagnostics.LoggingManager
import com.soundboard.android.diagnostics.PerformanceTuner
import com.soundboard.android.diagnostics.AlertingSystem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideDiagnosticsManager(@ApplicationContext context: Context): DiagnosticsManager {
        return DiagnosticsManager(context)
    }
    
    @Provides
    @Singleton
    fun provideLoggingManager(@ApplicationContext context: Context): LoggingManager {
        return LoggingManager(context)
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