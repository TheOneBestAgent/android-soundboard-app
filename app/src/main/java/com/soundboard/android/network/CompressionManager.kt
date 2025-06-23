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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.InflaterInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4.2: Adaptive Compression Manager
 * 
 * Provides intelligent compression based on network conditions, data characteristics,
 * and performance metrics for optimal bandwidth utilization.
 */
@Singleton
class CompressionManager @Inject constructor() {
    
    companion object {
        private const val TAG = "CompressionManager"
        private const val MIN_COMPRESSION_SIZE = 1024L // 1KB
        private const val COMPRESSION_SAMPLE_SIZE = 512 // bytes to sample for analysis
        private const val NETWORK_SPEED_WINDOW_MS = 30_000L // 30 seconds
        private const val COMPRESSION_EFFICIENCY_THRESHOLD = 0.8 // 80% efficiency
        private const val AUTO_ADAPTATION_INTERVAL_MS = 60_000L // 1 minute
    }
    
    // Compression algorithms
    enum class CompressionAlgorithm(
        val description: String,
        val compressionRatio: Double,
        val compressionSpeed: Double,
        val decompressionSpeed: Double
    ) {
        NONE("No Compression", 1.0, 1.0, 1.0),
        GZIP("GZIP Compression", 0.4, 0.7, 0.9),
        DEFLATE("Deflate Compression", 0.45, 0.8, 0.95),
        LZ4("LZ4 Fast Compression", 0.6, 0.95, 0.98),
        BROTLI("Brotli High Compression", 0.35, 0.5, 0.8)
    }
    
    // Data type characteristics for compression optimization
    enum class DataType(
        val expectedCompressionRatio: Double,
        val preferredAlgorithm: CompressionAlgorithm
    ) {
        TEXT(0.3, CompressionAlgorithm.GZIP),
        JSON(0.25, CompressionAlgorithm.GZIP),
        AUDIO(0.95, CompressionAlgorithm.NONE), // Already compressed
        IMAGE(0.9, CompressionAlgorithm.NONE), // Already compressed
        BINARY(0.7, CompressionAlgorithm.DEFLATE),
        UNKNOWN(0.5, CompressionAlgorithm.GZIP)
    }
    
    // Network condition assessment
    enum class NetworkCondition(val description: String, val bandwidthMbps: Double) {
        EXCELLENT("Excellent (100+ Mbps)", 100.0),
        GOOD("Good (25-100 Mbps)", 50.0),
        AVERAGE("Average (5-25 Mbps)", 15.0),
        POOR("Poor (1-5 Mbps)", 3.0),
        VERY_POOR("Very Poor (<1 Mbps)", 0.5)
    }
    
    // Compression strategy
    data class CompressionStrategy(
        val algorithm: CompressionAlgorithm,
        val compressionLevel: Int, // 1-9 for applicable algorithms
        val enableStreaming: Boolean,
        val chunkSize: Int,
        val reason: String
    )
    
    // Compression result
    data class CompressionResult(
        val compressedData: ByteArray,
        val originalSize: Long,
        val compressedSize: Long,
        val compressionRatio: Double,
        val compressionTime: Long,
        val algorithm: CompressionAlgorithm,
        val efficiency: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CompressionResult
            return compressedData.contentEquals(other.compressedData) &&
                    originalSize == other.originalSize &&
                    compressedSize == other.compressedSize
        }
        
        override fun hashCode(): Int {
            return compressedData.contentHashCode()
        }
    }
    
    // Performance metrics
    data class CompressionMetrics(
        val totalCompressions: Long,
        val totalDecompressions: Long,
        val averageCompressionRatio: Double,
        val averageCompressionTime: Long,
        val averageDecompressionTime: Long,
        val bandwidthSaved: Long, // bytes
        val compressionEfficiency: Double,
        val algorithmUsage: Map<CompressionAlgorithm, Long>
    )
    
    // State management
    private val compressionMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Performance tracking
    private val compressionStats = ConcurrentHashMap<CompressionAlgorithm, MutableList<Double>>()
    private val networkSpeedHistory = ArrayDeque<Pair<Long, Double>>(100) // timestamp, speed in Mbps
    private var totalCompressions = AtomicLong(0)
    private var totalDecompressions = AtomicLong(0)
    private var totalBandwidthSaved = AtomicLong(0)
    private var totalCompressionTime = AtomicLong(0)
    private var totalDecompressionTime = AtomicLong(0)
    
    // Configuration
    private var currentNetworkCondition = NetworkCondition.AVERAGE
    
    // State flows for monitoring
    private val _compressionMetrics = MutableStateFlow(
        CompressionMetrics(
            totalCompressions = 0L,
            totalDecompressions = 0L,
            averageCompressionRatio = 1.0,
            averageCompressionTime = 0L,
            averageDecompressionTime = 0L,
            bandwidthSaved = 0L,
            compressionEfficiency = 1.0,
            algorithmUsage = emptyMap()
        )
    )
    val compressionMetrics: StateFlow<CompressionMetrics> = _compressionMetrics.asStateFlow()
    
    /**
     * Initialize the compression manager
     */
    fun initialize() {
        scope.launch {
            compressionMutex.withLock {
                initializeCompressionStats()
                startMetricsUpdater()
            }
        }
        Log.i(TAG, "Compression manager initialized with adaptive features")
    }
    
    /**
     * Compress data using optimal strategy
     */
    suspend fun compress(
        data: ByteArray,
        dataType: DataType = DataType.UNKNOWN,
        forceAlgorithm: CompressionAlgorithm? = null
    ): CompressionResult {
        if (data.size < MIN_COMPRESSION_SIZE) {
            return CompressionResult(
                compressedData = data,
                originalSize = data.size.toLong(),
                compressedSize = data.size.toLong(),
                compressionRatio = 1.0,
                compressionTime = 0L,
                algorithm = CompressionAlgorithm.NONE,
                efficiency = 1.0
            )
        }
        
        val algorithm = forceAlgorithm ?: determineOptimalAlgorithm(data, dataType)
        
        val startTime = System.currentTimeMillis()
        val compressedData = performCompression(data, algorithm)
        val compressionTime = System.currentTimeMillis() - startTime
        
        val compressionRatio = compressedData.size.toDouble() / data.size
        val efficiency = calculateCompressionEfficiency(compressionRatio, compressionTime, algorithm)
        val bandwidthSaved = data.size - compressedData.size
        
        // Update metrics
        totalCompressions.incrementAndGet()
        totalCompressionTime.addAndGet(compressionTime)
        totalBandwidthSaved.addAndGet(bandwidthSaved.toLong())
        recordCompressionStats(algorithm, compressionRatio)
        
        Log.d(TAG, "Compressed ${data.size} bytes to ${compressedData.size} bytes using ${algorithm.name}")
        
        return CompressionResult(
            compressedData = compressedData,
            originalSize = data.size.toLong(),
            compressedSize = compressedData.size.toLong(),
            compressionRatio = compressionRatio,
            compressionTime = compressionTime,
            algorithm = algorithm,
            efficiency = efficiency
        )
    }
    
    /**
     * Decompress data
     */
    suspend fun decompress(
        compressedData: ByteArray,
        algorithm: CompressionAlgorithm
    ): ByteArray {
        val startTime = System.currentTimeMillis()
        val decompressedData = performDecompression(compressedData, algorithm)
        val decompressionTime = System.currentTimeMillis() - startTime
        
        // Update metrics
        totalDecompressions.incrementAndGet()
        totalDecompressionTime.addAndGet(decompressionTime)
        
        Log.d(TAG, "Decompressed ${compressedData.size} bytes to ${decompressedData.size} bytes")
        
        return decompressedData
    }
    
    /**
     * Analyze data characteristics for optimal compression
     */
    fun analyzeDataCharacteristics(data: ByteArray): DataType {
        if (data.isEmpty()) return DataType.UNKNOWN
        
        val sampleSize = minOf(data.size, COMPRESSION_SAMPLE_SIZE)
        val sample = data.take(sampleSize).toByteArray()
        
        // Check for text patterns
        val textRatio = sample.count { it in 32..126 || it in listOf(9, 10, 13) }.toDouble() / sampleSize
        if (textRatio > 0.8) {
            // Check for JSON patterns
            val jsonPatterns = listOf('{', '}', '[', ']', '"', ':').map { it.code.toByte() }
            val jsonRatio = sample.count { it in jsonPatterns }.toDouble() / sampleSize
            
            return if (jsonRatio > 0.2) DataType.JSON else DataType.TEXT
        }
        
        // Check entropy for binary vs text
        val entropy = calculateEntropy(sample)
        return if (entropy > 6.0) DataType.BINARY else DataType.UNKNOWN
    }
    
    /**
     * Update network condition assessment
     */
    fun updateNetworkCondition(bandwidthMbps: Double) {
        scope.launch {
            compressionMutex.withLock {
                val currentTime = System.currentTimeMillis()
                networkSpeedHistory.offer(Pair<Long, Double>(currentTime, bandwidthMbps))
                
                // Remove old entries
                val cutoffTime = currentTime - NETWORK_SPEED_WINDOW_MS
                while (networkSpeedHistory.isNotEmpty() && networkSpeedHistory.peek().first < cutoffTime) {
                    networkSpeedHistory.poll()
                }
                
                // Calculate average speed
                val averageSpeed = if (networkSpeedHistory.isNotEmpty()) {
                    networkSpeedHistory.map { it.second }.average()
                } else bandwidthMbps
                
                currentNetworkCondition = when {
                    averageSpeed >= 100.0 -> NetworkCondition.EXCELLENT
                    averageSpeed >= 25.0 -> NetworkCondition.GOOD
                    averageSpeed >= 5.0 -> NetworkCondition.AVERAGE
                    averageSpeed >= 1.0 -> NetworkCondition.POOR
                    else -> NetworkCondition.VERY_POOR
                }
            }
        }
    }
    
    // Private helper methods
    
    private fun initializeCompressionStats() {
        CompressionAlgorithm.values().forEach { algorithm ->
            compressionStats[algorithm] = mutableListOf()
        }
    }
    
    private fun startMetricsUpdater() {
        scope.launch {
            while (true) {
                try {
                    updateMetrics()
                    kotlinx.coroutines.delay(5000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating metrics", e)
                }
            }
        }
    }
    
    private fun determineOptimalAlgorithm(data: ByteArray, dataType: DataType): CompressionAlgorithm {
        // Consider data type preferences
        var preferredAlgorithm = dataType.preferredAlgorithm
        
        // Consider network conditions
        preferredAlgorithm = when (currentNetworkCondition) {
            NetworkCondition.EXCELLENT -> CompressionAlgorithm.NONE
            NetworkCondition.GOOD -> CompressionAlgorithm.LZ4
            NetworkCondition.AVERAGE -> CompressionAlgorithm.GZIP
            NetworkCondition.POOR -> CompressionAlgorithm.BROTLI
            NetworkCondition.VERY_POOR -> CompressionAlgorithm.BROTLI
        }
        
        return preferredAlgorithm
    }
    
    private fun performCompression(data: ByteArray, algorithm: CompressionAlgorithm): ByteArray {
        return when (algorithm) {
            CompressionAlgorithm.NONE -> data
            CompressionAlgorithm.GZIP -> compressGzip(data)
            CompressionAlgorithm.DEFLATE -> compressDeflate(data)
            CompressionAlgorithm.LZ4 -> compressLZ4(data)
            CompressionAlgorithm.BROTLI -> compressBrotli(data)
        }
    }
    
    private fun performDecompression(data: ByteArray, algorithm: CompressionAlgorithm): ByteArray {
        return when (algorithm) {
            CompressionAlgorithm.NONE -> data
            CompressionAlgorithm.GZIP -> decompressGzip(data)
            CompressionAlgorithm.DEFLATE -> decompressDeflate(data)
            CompressionAlgorithm.LZ4 -> decompressLZ4(data)
            CompressionAlgorithm.BROTLI -> decompressBrotli(data)
        }
    }
    
    // Compression implementations
    private fun compressGzip(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipStream ->
            gzipStream.write(data)
        }
        return outputStream.toByteArray()
    }
    
    private fun decompressGzip(data: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(data)
        return GZIPInputStream(inputStream).use { gzipStream ->
            gzipStream.readBytes()
        }
    }
    
    private fun compressDeflate(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        DeflaterOutputStream(outputStream).use { deflateStream ->
            deflateStream.write(data)
        }
        return outputStream.toByteArray()
    }
    
    private fun decompressDeflate(data: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(data)
        return InflaterInputStream(inputStream).use { inflateStream ->
            inflateStream.readBytes()
        }
    }
    
    // Simplified implementations for LZ4 and Brotli
    private fun compressLZ4(data: ByteArray): ByteArray {
        return compressDeflate(data) // Fast deflate as placeholder
    }
    
    private fun decompressLZ4(data: ByteArray): ByteArray {
        return decompressDeflate(data)
    }
    
    private fun compressBrotli(data: ByteArray): ByteArray {
        return compressGzip(data) // High compression GZIP as placeholder
    }
    
    private fun decompressBrotli(data: ByteArray): ByteArray {
        return decompressGzip(data)
    }
    
    private fun calculateCompressionEfficiency(
        compressionRatio: Double,
        compressionTime: Long,
        algorithm: CompressionAlgorithm
    ): Double {
        val timeEfficiency = 1.0 / (1.0 + compressionTime / 1000.0)
        val ratioEfficiency = 1.0 - compressionRatio
        val algorithmBonus = algorithm.compressionSpeed
        
        return (timeEfficiency * 0.3 + ratioEfficiency * 0.5 + algorithmBonus * 0.2).coerceIn(0.0, 1.0)
    }
    
    private fun calculateEntropy(data: ByteArray): Double {
        val frequency = IntArray(256)
        data.forEach { byte ->
            frequency[byte.toInt() and 0xFF]++
        }
        
        var entropy = 0.0
        val length = data.size.toDouble()
        
        frequency.forEach { count ->
            if (count > 0) {
                val probability = count / length
                entropy -= probability * (kotlin.math.ln(probability) / kotlin.math.ln(2.0))
            }
        }
        
        return entropy
    }
    
    private fun recordCompressionStats(algorithm: CompressionAlgorithm, ratio: Double) {
        val stats = compressionStats[algorithm] ?: return
        stats.add(ratio)
        
        if (stats.size > 100) {
            stats.removeAt(0)
        }
    }
    
    private fun updateMetrics() {
        val totalCompr = totalCompressions.get()
        val totalDecompr = totalDecompressions.get()
        val avgCompressionTime = if (totalCompr > 0) {
            totalCompressionTime.get() / totalCompr
        } else 0L
        val avgDecompressionTime = if (totalDecompr > 0) {
            totalDecompressionTime.get() / totalDecompr
        } else 0L
        
        val avgCompressionRatio = compressionStats.values
            .flatten()
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 1.0
        
        val algorithmUsage = compressionStats.mapValues { it.value.size.toLong() }
        
        val efficiency = if (totalCompr > 0) {
            1.0 - avgCompressionRatio
        } else 1.0
        
        _compressionMetrics.value = CompressionMetrics(
            totalCompressions = totalCompr,
            totalDecompressions = totalDecompr,
            averageCompressionRatio = avgCompressionRatio,
            averageCompressionTime = avgCompressionTime,
            averageDecompressionTime = avgDecompressionTime,
            bandwidthSaved = totalBandwidthSaved.get(),
            compressionEfficiency = efficiency,
            algorithmUsage = algorithmUsage
        )
    }
}