package com.soundboard.android.di

import android.content.Context
import androidx.room.Room
import com.soundboard.android.data.dao.ConnectionHistoryDao
import com.soundboard.android.data.dao.SoundButtonDao
import com.soundboard.android.data.dao.SoundboardLayoutDao
import com.soundboard.android.data.database.SoundboardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideSoundboardDatabase(@ApplicationContext context: Context): SoundboardDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SoundboardDatabase::class.java,
            SoundboardDatabase.DATABASE_NAME
        )
            .addMigrations(SoundboardDatabase.MIGRATION_2_3)
            .build()
    }
    
    @Provides
    fun provideSoundButtonDao(database: SoundboardDatabase): SoundButtonDao {
        return database.soundButtonDao()
    }
    
    @Provides
    fun provideSoundboardLayoutDao(database: SoundboardDatabase): SoundboardLayoutDao {
        return database.soundboardLayoutDao()
    }
    
    @Provides
    fun provideConnectionHistoryDao(database: SoundboardDatabase): ConnectionHistoryDao {
        return database.connectionHistoryDao()
    }
} 