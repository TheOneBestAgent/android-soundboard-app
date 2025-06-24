package com.soundboard.android.network

class ConnectionManager {
    private var maxConnections: Int = 100
    private var idleTimeout: Long = 30000 // 30 seconds
    private var validationInterval: Long = 60000 // 1 minute
    private var poolSize: Int = 50
    private var keepAliveTimeout: Long = 60000 // 1 minute

    fun setMaxConnections(max: Int) {
        maxConnections = max
    }

    fun setConnectionIdleTimeout(timeout: Long) {
        idleTimeout = timeout
    }

    fun setConnectionValidationInterval(interval: Long) {
        validationInterval = interval
    }

    fun setConnectionPoolSize(size: Int) {
        poolSize = size
    }

    fun setKeepAliveTimeout(timeout: Long) {
        keepAliveTimeout = timeout
    }
} 