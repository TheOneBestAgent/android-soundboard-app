package com.soundboard.android.ui.viewmodel

import com.soundboard.android.diagnostics.ComponentType
import com.soundboard.android.diagnostics.LogCategory
import com.soundboard.android.diagnostics.LogEvent
import com.soundboard.android.diagnostics.LogLevel

fun SoundboardViewModel.logInteraction(
    message: String,
    component: ComponentType,
    metadata: Map<String, String> = emptyMap()
) {
    logEvent(
        LogEvent(
            level = LogLevel.INFO,
            category = LogCategory.USER_INTERACTION,
            message = message,
            component = component,
            metadata = metadata
        )
    )
} 