package com.audiodeck.connect.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.audiodeck.connect.data.dao.*
import com.audiodeck.connect.data.model.*

@Database(
    entities = [
        SoundButton::class,
        AudioDeckLayout::class,
        ConnectionHistory::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(AudioDeckTypeConverters::class)
abstract class AudioDeckDatabase : RoomDatabase() {
    
    abstract fun soundButtonDao(): SoundButtonDao
    abstract fun audioDeckLayoutDao(): AudioDeckLayoutDao
    abstract fun connectionHistoryDao(): ConnectionHistoryDao
    
    companion object {
        const val DATABASE_NAME = "audiodeck_database"
        
        @Volatile
        private var INSTANCE: AudioDeckDatabase? = null
        
        // Migration from version 2 to 3 - add isLocalFile column
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sound_buttons ADD COLUMN is_local_file INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        fun getDatabase(context: Context): AudioDeckDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudioDeckDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Type converters for custom types
class AudioDeckTypeConverters {
    @TypeConverter
    fun fromLayoutPreset(preset: LayoutPreset): String {
        return preset.name
    }
    
    @TypeConverter
    fun toLayoutPreset(presetName: String): LayoutPreset {
        return try {
            LayoutPreset.valueOf(presetName)
        } catch (e: IllegalArgumentException) {
            LayoutPreset.CUSTOM // Default fallback
        }
    }
} 