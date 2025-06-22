package com.soundboard.android.data.dao

import androidx.room.*
import com.soundboard.android.data.model.ConnectionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionHistoryDao {
    
    @Query("SELECT * FROM connection_history ORDER BY last_connected DESC")
    fun getAllConnections(): Flow<List<ConnectionHistory>>
    
    @Query("SELECT * FROM connection_history WHERE is_favorite = 1 ORDER BY last_connected DESC")
    fun getFavoriteConnections(): Flow<List<ConnectionHistory>>
    
    @Query("SELECT * FROM connection_history WHERE id = :id")
    suspend fun getConnectionById(id: Int): ConnectionHistory?
    
    @Query("SELECT * FROM connection_history WHERE ip_address = :ipAddress AND port = :port LIMIT 1")
    suspend fun getConnectionByAddress(ipAddress: String, port: Int): ConnectionHistory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: ConnectionHistory): Long
    
    @Update
    suspend fun updateConnection(connection: ConnectionHistory)
    
    @Delete
    suspend fun deleteConnection(connection: ConnectionHistory)
    
    @Query("UPDATE connection_history SET last_connected = :timestamp WHERE ip_address = :ipAddress AND port = :port")
    suspend fun updateLastConnected(ipAddress: String, port: Int, timestamp: Long)
    
    @Query("UPDATE connection_history SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)
    
    @Query("DELETE FROM connection_history WHERE last_connected < :cutoffTime AND is_favorite = 0")
    suspend fun deleteOldConnections(cutoffTime: Long)
} 