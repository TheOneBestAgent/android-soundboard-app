package com.soundboard.android.data.dao

import androidx.room.*
import com.soundboard.android.data.model.SoundButton
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundButtonDao {
    
    @Query("SELECT * FROM sound_buttons ORDER BY position_y, position_x")
    fun getAllSoundButtons(): Flow<List<SoundButton>>
    
    @Query("SELECT * FROM sound_buttons WHERE id = :id")
    suspend fun getSoundButtonById(id: Int): SoundButton?
    
    @Query("SELECT * FROM sound_buttons WHERE position_x = :x AND position_y = :y")
    suspend fun getSoundButtonByPosition(x: Int, y: Int): SoundButton?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundButton(soundButton: SoundButton): Long
    
    @Update
    suspend fun updateSoundButton(soundButton: SoundButton)
    
    @Delete
    suspend fun deleteSoundButton(soundButton: SoundButton)
    
    @Query("DELETE FROM sound_buttons WHERE id = :id")
    suspend fun deleteSoundButtonById(id: Int)
    
    @Query("DELETE FROM sound_buttons")
    suspend fun deleteAllSoundButtons()
} 