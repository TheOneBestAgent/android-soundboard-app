package com.soundboard.android.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soundboard.android.data.dao.*
import com.soundboard.android.data.model.*

@Database(
    entities = [
        SoundButton::class,
        SoundboardLayout::class,
        ConnectionHistory::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(SoundboardTypeConverters::class)
abstract class SoundboardDatabase : RoomDatabase() {
    
    abstract fun soundButtonDao(): SoundButtonDao
    abstract fun soundboardLayoutDao(): SoundboardLayoutDao
    abstract fun connectionHistoryDao(): ConnectionHistoryDao
    
    companion object {
        const val DATABASE_NAME = "soundboard_database"
        
        @Volatile
        private var INSTANCE: SoundboardDatabase? = null
        
        // Migration from version 2 to 3 - add isLocalFile column
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sound_buttons ADD COLUMN is_local_file INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        fun getDatabase(context: Context): SoundboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundboardDatabase::class.java,
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
class SoundboardTypeConverters {
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