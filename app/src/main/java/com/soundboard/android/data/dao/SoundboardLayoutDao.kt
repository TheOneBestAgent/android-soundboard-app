package com.soundboard.android.data.dao

import androidx.room.*
import com.soundboard.android.data.model.SoundboardLayout
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundboardLayoutDao {
    
    @Query("SELECT * FROM soundboard_layouts ORDER BY created_at DESC")
    fun getAllLayouts(): Flow<List<SoundboardLayout>>
    
    @Query("SELECT * FROM soundboard_layouts WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveLayout(): SoundboardLayout?
    
    @Query("SELECT * FROM soundboard_layouts WHERE id = :id")
    suspend fun getLayoutById(id: Int): SoundboardLayout?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: SoundboardLayout): Long
    
    @Update
    suspend fun updateLayout(layout: SoundboardLayout)
    
    @Delete
    suspend fun deleteLayout(layout: SoundboardLayout)
    
    @Query("DELETE FROM soundboard_layouts")
    suspend fun deleteAllLayouts()
    
    @Query("UPDATE soundboard_layouts SET is_active = 0")
    suspend fun deactivateAllLayouts()
    
    @Query("UPDATE soundboard_layouts SET is_active = 1 WHERE id = :id")
    suspend fun setActiveLayout(id: Long)
    
    @Transaction
    suspend fun switchActiveLayout(id: Long) {
        deactivateAllLayouts()
        setActiveLayout(id)
    }
} 