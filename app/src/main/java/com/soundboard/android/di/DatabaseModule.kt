package com.audiodeck.connect.di

import android.content.Context
import androidx.room.Room
import com.audiodeck.connect.data.dao.ConnectionHistoryDao
import com.audiodeck.connect.data.dao.SoundButtonDao
import com.audiodeck.connect.data.dao.AudioDeckLayoutDao
import com.audiodeck.connect.data.database.AudioDeckDatabase
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
    fun provideAudioDeckDatabase(@ApplicationContext context: Context): AudioDeckDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AudioDeckDatabase::class.java,
            AudioDeckDatabase.DATABASE_NAME
        )
            .addMigrations(AudioDeckDatabase.MIGRATION_2_3)
            .build()
    }
    
    @Provides
    fun provideSoundButtonDao(database: AudioDeckDatabase): SoundButtonDao {
        return database.soundButtonDao()
    }
    
    @Provides
    fun provideAudioDeckLayoutDao(database: AudioDeckDatabase): AudioDeckLayoutDao {
        return database.audioDeckLayoutDao()
    }
    
    @Provides
    fun provideConnectionHistoryDao(database: AudioDeckDatabase): ConnectionHistoryDao {
        return database.connectionHistoryDao()
    }
} 