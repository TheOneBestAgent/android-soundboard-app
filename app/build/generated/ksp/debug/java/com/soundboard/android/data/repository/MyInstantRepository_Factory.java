package com.soundboard.android.data.repository;

import android.content.Context;
import com.soundboard.android.network.api.MyInstantApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MyInstantRepository_Factory implements Factory<MyInstantRepository> {
  private final Provider<MyInstantApiService> myInstantApiServiceProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Context> contextProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public MyInstantRepository_Factory(Provider<MyInstantApiService> myInstantApiServiceProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.myInstantApiServiceProvider = myInstantApiServiceProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.contextProvider = contextProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public MyInstantRepository get() {
    return newInstance(myInstantApiServiceProvider.get(), okHttpClientProvider.get(), contextProvider.get(), settingsRepositoryProvider.get());
  }

  public static MyInstantRepository_Factory create(
      Provider<MyInstantApiService> myInstantApiServiceProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<Context> contextProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new MyInstantRepository_Factory(myInstantApiServiceProvider, okHttpClientProvider, contextProvider, settingsRepositoryProvider);
  }

  public static MyInstantRepository newInstance(MyInstantApiService myInstantApiService,
      OkHttpClient okHttpClient, Context context, SettingsRepository settingsRepository) {
    return new MyInstantRepository(myInstantApiService, okHttpClient, context, settingsRepository);
  }
}
