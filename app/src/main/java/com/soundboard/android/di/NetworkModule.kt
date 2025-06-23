package com.soundboard.android.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.soundboard.android.network.SocketManager
import com.soundboard.android.network.DeviceSessionManager
import com.soundboard.android.network.SessionCoordinator
import com.soundboard.android.network.api.SoundboardApiService
import com.soundboard.android.network.api.MyInstantApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        // This will be set dynamically when connecting to a server
        // For now, use a placeholder that will be replaced
        return Retrofit.Builder()
            .baseUrl("http://localhost:8080/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSoundboardApiService(retrofit: Retrofit): SoundboardApiService {
        return retrofit.create(SoundboardApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideSocketManager(): SocketManager {
        return SocketManager()
    }
    
    @Provides
    @Singleton
    @Named("myinstant")
    fun provideMyInstantRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://myinstants-api.vercel.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMyInstantApiService(@Named("myinstant") retrofit: Retrofit): MyInstantApiService {
        return retrofit.create(MyInstantApiService::class.java)
    }
    
    // Phase 4.1: Multi-Device Support Dependencies
    @Provides
    @Singleton
    fun provideDeviceSessionManager(): DeviceSessionManager {
        return DeviceSessionManager()
    }
    
    @Provides
    @Singleton
    fun provideSessionCoordinator(
        deviceSessionManager: DeviceSessionManager,
        gson: Gson
    ): SessionCoordinator {
        return SessionCoordinator(deviceSessionManager, gson)
    }
}

// Helper class to create API service for different server addresses
class RetrofitFactory @Singleton constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun createApiService(baseUrl: String): SoundboardApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        return retrofit.create(SoundboardApiService::class.java)
    }
} 