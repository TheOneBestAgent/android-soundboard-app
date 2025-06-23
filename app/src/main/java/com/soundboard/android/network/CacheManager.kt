package com.soundboard.android.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.2: Intelligent Cache Manager
 * 
 * Provides advanced caching with predictive prefetching, cache optimization,
 * and intelligent eviction strategies for improved performance.
 */
@Singleton
class CacheManager @Inject constructor() {
    
    companion object {
        private const val TAG = "CacheManager"
        private const val DEFAULT_CACHE_SIZE_MB = 50
        private const val MAX_CACHE_SIZE_MB = 200
        private const val PREFETCH_THRESHOLD = 0.8 // Start prefetching at 80% probability
        private const val CACHE_CLEANUP_INTERVAL_MS = 60_000L // 1 minute
        private const val ACCESS_PATTERN_WINDOW_MS = 300_000L // 5 minutes
        private const val POPULARITY_DECAY_FACTOR = 0.9
        private const val PREFETCH_BATCH_SIZE = 5
    }
    
    // Cache entry representation
    data class CacheEntry<T>(
        val key: String,
        val data: T,
        val size: Long,
        val createdTimestamp: Long,
        val lastAccessedTimestamp: Long,
        val accessCount: Long,
        val ttl: Long, // Time to live in milliseconds
        val priority: CachePriority,
        val compressionLevel: CompressionLevel,
        val tags: Set<String> = emptySet()
    )
    
    enum class CachePriority(val weight: Double) {
        LOW(0.2),
        NORMAL(0.5),
        HIGH(0.8),
        CRITICAL(1.0)
    }
    
    enum class CompressionLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }
    
    // Cache configuration
    data class CacheConfiguration(
        val maxSizeBytes: Long,
        val enablePrefetching: Boolean,
        val enableCompression: Boolean,
        val defaultTtl: Long,
        val evictionStrategy: EvictionStrategy,
        val compressionThreshold: Long
    )
    
    enum class EvictionStrategy {
        LRU,
        LFU,
        WEIGHTED_LRU,
        ADAPTIVE
    }
    
    // Access pattern tracking
    data class AccessPattern(
        val key: String,
        val accessTimes: MutableList<Long>,
        val relatedKeys: MutableMap<String, Double>,
        var predictedNextAccess: Long,
        var accessProbability: Double
    )
    
    // Cache statistics
    data class CacheStatistics(
        val totalEntries: Int,
        val totalSizeBytes: Long,
        val maxSizeBytes: Long,
        val hitRate: Double,
        val missRate: Double,
        val evictionCount: Long,
        val prefetchHitRate: Double,
        val compressionRatio: Double,
        val averageAccessTime: Long,
        val memoryEfficiency: Double
    )
    
    // Prefetch recommendation
    data class PrefetchRecommendation(
        val key: String,
        val priority: CachePriority,
        val probability: Double,
        val estimatedAccessTime: Long,
        val reason: String
    )
    
    // Cache state management
    private val cacheMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val accessPatterns = ConcurrentHashMap<String, AccessPattern>()
    private val recentAccesses = ConcurrentLinkedQueue<Pair<String, Long>>()
    
    // Configuration and metrics
    private var cacheConfiguration = CacheConfiguration(
        maxSizeBytes = DEFAULT_CACHE_SIZE_MB * 1024L * 1024L,
        enablePrefetching = true,
        enableCompression = true,
        defaultTtl = 3600_000L, // 1 hour
        evictionStrategy = EvictionStrategy.ADAPTIVE,
        compressionThreshold = 1024L * 10L // 10KB
    )
    
    // Performance tracking
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    private val prefetchHits = AtomicLong(0)
    private val prefetchMisses = AtomicLong(0)
    private var totalAccessTime = AtomicLong(0)
    private var accessTimeCount = AtomicLong(0)
    
    // State flows for monitoring
    private val _cacheStatistics = MutableStateFlow(
        CacheStatistics(
            totalEntries = 0,
            totalSizeBytes = 0L,
            maxSizeBytes = cacheConfiguration.maxSizeBytes,
            hitRate = 0.0,
            missRate = 0.0,
            evictionCount = 0L,
            prefetchHitRate = 0.0,
            compressionRatio = 1.0,
            averageAccessTime = 0L,
            memoryEfficiency = 1.0
        )
    )
    val cacheStatistics: StateFlow<CacheStatistics> = _cacheStatistics.asStateFlow()
    
    private val _prefetchRecommendations = MutableStateFlow<List<PrefetchRecommendation>>(emptyList())
    val prefetchRecommendations: StateFlow<List<PrefetchRecommendation>> = _prefetchRecommendations.asStateFlow()
    
    /**
     * Initialize the cache manager
     */
    fun initialize(configuration: CacheConfiguration = cacheConfiguration) {
        scope.launch {
            cacheMutex.withLock {
                cacheConfiguration = configuration
                startCacheMaintenanceTask()
                startPatternAnalysisTask()
                startStatisticsUpdater()
            }
        }
        Log.i(TAG, "Cache manager initialized with ${configuration.maxSizeBytes / (1024*1024)}MB capacity")
    }
    
    /**
     * Get an item from cache
     */
    suspend fun <T> get(key: String): T? {
        val startTime = System.currentTimeMillis()
        
        val result = cacheMutex.withLock {
            val entry = cache[key] as? CacheEntry<T>
            
            if (entry != null) {
                // Check if entry is still valid
                if (isEntryValid(entry)) {
                    // Update access statistics
                    updateAccessPattern(key)
                    // Update entry in cache with new statistics
                    @Suppress("UNCHECKED_CAST") 
                    val anyEntry = entry as CacheEntry<Any>
                    val updatedEntry = anyEntry.copy(
                        lastAccessedTimestamp = System.currentTimeMillis(),
                        accessCount = anyEntry.accessCount + 1
                    )
                    cache[key] = updatedEntry
                    
                    cacheHits.incrementAndGet()
                    @Suppress("UNCHECKED_CAST")
                    entry.data as T
                } else {
                    // Entry expired, remove it
                    cache.remove(key)
                    cacheMisses.incrementAndGet()
                    null
                }
            } else {
                cacheMisses.incrementAndGet()
                null
            }
        }
        
        val endTime = System.currentTimeMillis()
        recordAccessTime(endTime - startTime)
        
        return result
    }
    
    /**
     * Put an item into cache
     */
    suspend fun <T> put(
        key: String, 
        data: T, 
        size: Long = estimateSize(data),
        priority: CachePriority = CachePriority.NORMAL,
        ttl: Long = cacheConfiguration.defaultTtl,
        tags: Set<String> = emptySet()
    ) {
        cacheMutex.withLock {
            // Check if we need to make space
            val totalSize = getCurrentCacheSize()
            if (totalSize + size > cacheConfiguration.maxSizeBytes) {
                evictItemsToMakeSpace(size)
            }
            
            // Determine compression level
            val compressionLevel = determineCompressionLevel(size)
            
            // Create cache entry
            val entry = CacheEntry(
                key = key,
                data = data as Any,
                size = size,
                createdTimestamp = System.currentTimeMillis(),
                lastAccessedTimestamp = System.currentTimeMillis(),
                accessCount = 1L,
                ttl = ttl,
                priority = priority,
                compressionLevel = compressionLevel,
                tags = tags
            )
            
            cache[key] = entry
            updateAccessPattern(key)
        }
        
        Log.d(TAG, "Cached item: $key (${size} bytes, ${priority.name} priority)")
    }
    
    /**
     * Remove an item from cache
     */
    suspend fun remove(key: String): Boolean {
        return cacheMutex.withLock {
            val removed = cache.remove(key) != null
            if (removed) {
                accessPatterns.remove(key)
                Log.d(TAG, "Removed item from cache: $key")
            }
            removed
        }
    }
    
    /**
     * Clear cache items with specific tags
     */
    suspend fun clearByTags(tags: Set<String>) {
        cacheMutex.withLock {
            val keysToRemove = cache.values
                .filter { entry -> entry.tags.any { it in tags } }
                .map { it.key }
            
            keysToRemove.forEach { key ->
                cache.remove(key)
                accessPatterns.remove(key)
            }
            
            Log.d(TAG, "Cleared ${keysToRemove.size} items with tags: $tags")
        }
    }
    
    // Private helper methods (abbreviated for brevity)
    private fun startCacheMaintenanceTask() {
        scope.launch {
            while (true) {
                try {
                    performCacheMaintenance()
                    kotlinx.coroutines.delay(CACHE_CLEANUP_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in cache maintenance", e)
                }
            }
        }
    }
    
    private fun startPatternAnalysisTask() {
        if (cacheConfiguration.enablePrefetching) {
            scope.launch {
                while (true) {
                    try {
                        analyzeAccessPatterns()
                        performPredictivePrefetch()
                        kotlinx.coroutines.delay(30_000L)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in pattern analysis", e)
                    }
                }
            }
        }
    }
    
    private fun startStatisticsUpdater() {
        scope.launch {
            while (true) {
                try {
                    updateStatistics()
                    kotlinx.coroutines.delay(5000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating statistics", e)
                }
            }
        }
    }
    
    private suspend fun performCacheMaintenance() {
        cacheMutex.withLock {
            val expiredKeys = cache.entries
                .filter { !isEntryValid(it.value) }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                cache.remove(key)
                accessPatterns.remove(key)
            }
        }
    }
    
    private fun analyzeAccessPatterns() {
        val currentTime = System.currentTimeMillis()
        val windowStart = currentTime - ACCESS_PATTERN_WINDOW_MS
        
        accessPatterns.values.forEach { pattern ->
            pattern.accessTimes.removeAll { it < windowStart }
            
            if (pattern.accessTimes.size >= 2) {
                val intervals = pattern.accessTimes.zipWithNext { a, b -> b - a }
                val avgInterval = intervals.average().toLong()
                pattern.predictedNextAccess = pattern.accessTimes.last() + avgInterval
                
                val intervalVariance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()
                pattern.accessProbability = 1.0 / (1.0 + intervalVariance / (avgInterval * avgInterval))
            }
        }
    }
    
    private suspend fun performPredictivePrefetch() {
        val recommendations = mutableListOf<PrefetchRecommendation>()
        val currentTime = System.currentTimeMillis()
        
        accessPatterns.values.forEach { pattern ->
            if (pattern.accessProbability >= PREFETCH_THRESHOLD &&
                pattern.predictedNextAccess <= currentTime + 60_000L &&
                !cache.containsKey(pattern.key)) {
                
                recommendations.add(
                    PrefetchRecommendation(
                        key = pattern.key,
                        priority = CachePriority.NORMAL,
                        probability = pattern.accessProbability,
                        estimatedAccessTime = pattern.predictedNextAccess,
                        reason = "Pattern-based prediction"
                    )
                )
            }
        }
        
        val topRecommendations = recommendations
            .sortedByDescending { it.probability }
            .take(PREFETCH_BATCH_SIZE)
        
        _prefetchRecommendations.value = topRecommendations
    }
    
    private fun evictItemsToMakeSpace(requiredSpace: Long) {
        val targetSize = cacheConfiguration.maxSizeBytes - requiredSpace
        var currentSize = getCurrentCacheSize()
        
        if (currentSize <= targetSize) return
        
        val candidates = when (cacheConfiguration.evictionStrategy) {
            EvictionStrategy.LRU -> cache.values.sortedBy { it.lastAccessedTimestamp }
            EvictionStrategy.LFU -> cache.values.sortedBy { it.accessCount }
            EvictionStrategy.WEIGHTED_LRU -> cache.values.sortedBy { 
                it.lastAccessedTimestamp * it.priority.weight 
            }
            EvictionStrategy.ADAPTIVE -> cache.values.sortedWith(
                compareBy<CacheEntry<Any>> { it.priority.weight }
                    .thenBy { it.accessCount }
                    .thenBy { it.lastAccessedTimestamp }
            )
        }
        
        candidates.forEach { entry ->
            if (currentSize <= targetSize) return
            
            cache.remove(entry.key)
            accessPatterns.remove(entry.key)
            currentSize -= entry.size
            evictionCount.incrementAndGet()
        }
    }
    
    private fun isEntryValid(entry: CacheEntry<Any>): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime <= entry.createdTimestamp + entry.ttl
    }
    
    private fun updateAccessPattern(key: String) {
        val currentTime = System.currentTimeMillis()
        recentAccesses.offer(key to currentTime)
        
        val pattern = accessPatterns.getOrPut(key) {
            AccessPattern(
                key = key,
                accessTimes = mutableListOf(),
                relatedKeys = mutableMapOf(),
                predictedNextAccess = 0L,
                accessProbability = 0.0
            )
        }
        
        pattern.accessTimes.add(currentTime)
    }
    
    private fun determineCompressionLevel(size: Long): CompressionLevel {
        return if (!cacheConfiguration.enableCompression) {
            CompressionLevel.NONE
        } else when {
            size < cacheConfiguration.compressionThreshold -> CompressionLevel.NONE
            size < cacheConfiguration.compressionThreshold * 5 -> CompressionLevel.LOW
            size < cacheConfiguration.compressionThreshold * 20 -> CompressionLevel.MEDIUM
            else -> CompressionLevel.HIGH
        }
    }
    
    private fun getCurrentCacheSize(): Long {
        return cache.values.sumOf { it.size }
    }
    
    private fun estimateSize(data: Any?): Long {
        return when (data) {
            is String -> data.length * 2L
            is ByteArray -> data.size.toLong()
            is List<*> -> data.size * 50L
            is Map<*, *> -> data.size * 100L
            else -> 100L
        }
    }
    
    private fun recordAccessTime(accessTime: Long) {
        totalAccessTime.addAndGet(accessTime)
        accessTimeCount.incrementAndGet()
    }
    
    private fun updateStatistics() {
        val totalEntries = cache.size
        val totalSizeBytes = getCurrentCacheSize()
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        
        val hitRate = if (total > 0) hits.toDouble() / total else 0.0
        val missRate = if (total > 0) misses.toDouble() / total else 0.0
        
        val prefetchTotal = prefetchHits.get() + prefetchMisses.get()
        val prefetchHitRate = if (prefetchTotal > 0) {
            prefetchHits.get().toDouble() / prefetchTotal
        } else 0.0
        
        val averageAccessTime = if (accessTimeCount.get() > 0) {
            totalAccessTime.get() / accessTimeCount.get()
        } else 0L
        
        val memoryEfficiency = if (cacheConfiguration.maxSizeBytes > 0) {
            totalSizeBytes.toDouble() / cacheConfiguration.maxSizeBytes
        } else 0.0
        
        _cacheStatistics.value = CacheStatistics(
            totalEntries = totalEntries,
            totalSizeBytes = totalSizeBytes,
            maxSizeBytes = cacheConfiguration.maxSizeBytes,
            hitRate = hitRate,
            missRate = missRate,
            evictionCount = evictionCount.get(),
            prefetchHitRate = prefetchHitRate,
            compressionRatio = 1.0,
            averageAccessTime = averageAccessTime,
            memoryEfficiency = memoryEfficiency
        )
    }
}