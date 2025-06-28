package com.example.soundboard.data

import java.time.LocalDateTime

data class AuctionItem(
    val itemId: String,
    val auctionId: String,
    val title: String,
    val category: String,
    val currentPrice: Double,
    val timeRemaining: String,
    val bidIncrement: Double,
    val imageUrl: String? = null,
    val description: String? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val url: String? = null
)

data class UserPreferences(
    val trackedItems: List<String> = emptyList(),
    val trackedCategories: List<String> = emptyList(),
    val trackedKeywords: List<String> = emptyList(),
    val notificationSettings: Map<String, Boolean> = mapOf(
        "price_change" to true,
        "ending_soon" to true,
        "auction_ended" to true,
        "new_item" to true,
        "bid_placed" to true,
        "bid_failed" to true
    ),
    val maxBidAmounts: Map<String, Double> = emptyMap() // itemId -> maxBidAmount
)

data class Bid(
    val itemId: String,
    val auctionId: String,
    val bidAmount: Double,
    val timestamp: LocalDateTime,
    val success: Boolean,
    val message: String? = null
)

data class NotificationEvent(
    val eventType: String, // e.g., "price_change", "ending_soon", "bid_placed"
    val itemId: String,
    val timestamp: LocalDateTime,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)