package com.soundboard.android.data.repository;

import com.google.gson.Gson;
import com.soundboard.android.data.dao.ConnectionHistoryDao;
import com.soundboard.android.data.dao.SoundButtonDao;
import com.soundboard.android.data.dao.SoundboardLayoutDao;
import com.soundboard.android.network.SocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class SoundboardRepository_Factory implements Factory<SoundboardRepository> {
  private final Provider<SoundButtonDao> soundButtonDaoProvider;

  private final Provider<SoundboardLayoutDao> soundboardLayoutDaoProvider;

  private final Provider<ConnectionHistoryDao> connectionHistoryDaoProvider;

  private final Provider<SocketManager> socketManagerProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Gson> gsonProvider;

  private final Provider<LocalAudioFileManager> localAudioFileManagerProvider;

  public SoundboardRepository_Factory(Provider<SoundButtonDao> soundButtonDaoProvider,
      Provider<SoundboardLayoutDao> soundboardLayoutDaoProvider,
      Provider<ConnectionHistoryDao> connectionHistoryDaoProvider,
      Provider<SocketManager> socketManagerProvider, Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<LocalAudioFileManager> localAudioFileManagerProvider) {
    this.soundButtonDaoProvider = soundButtonDaoProvider;
    this.soundboardLayoutDaoProvider = soundboardLayoutDaoProvider;
    this.connectionHistoryDaoProvider = connectionHistoryDaoProvider;
    this.socketManagerProvider = socketManagerProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.gsonProvider = gsonProvider;
    this.localAudioFileManagerProvider = localAudioFileManagerProvider;
  }

  @Override
  public SoundboardRepository get() {
    return newInstance(soundButtonDaoProvider.get(), soundboardLayoutDaoProvider.get(), connectionHistoryDaoProvider.get(), socketManagerProvider.get(), okHttpClientProvider.get(), gsonProvider.get(), localAudioFileManagerProvider.get());
  }

  public static SoundboardRepository_Factory create(Provider<SoundButtonDao> soundButtonDaoProvider,
      Provider<SoundboardLayoutDao> soundboardLayoutDaoProvider,
      Provider<ConnectionHistoryDao> connectionHistoryDaoProvider,
      Provider<SocketManager> socketManagerProvider, Provider<OkHttpClient> okHttpClientProvider,
      Provider<Gson> gsonProvider, Provider<LocalAudioFileManager> localAudioFileManagerProvider) {
    return new SoundboardRepository_Factory(soundButtonDaoProvider, soundboardLayoutDaoProvider, connectionHistoryDaoProvider, socketManagerProvider, okHttpClientProvider, gsonProvider, localAudioFileManagerProvider);
  }

  public static SoundboardRepository newInstance(SoundButtonDao soundButtonDao,
      SoundboardLayoutDao soundboardLayoutDao, ConnectionHistoryDao connectionHistoryDao,
      SocketManager socketManager, OkHttpClient okHttpClient, Gson gson,
      LocalAudioFileManager localAudioFileManager) {
    return new SoundboardRepository(soundButtonDao, soundboardLayoutDao, connectionHistoryDao, socketManager, okHttpClient, gson, localAudioFileManager);
  }
}
