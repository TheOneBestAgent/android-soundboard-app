package com.soundboard.android.di;

import com.soundboard.android.network.api.MyInstantApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideMyInstantApiServiceFactory implements Factory<MyInstantApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideMyInstantApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public MyInstantApiService get() {
    return provideMyInstantApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideMyInstantApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideMyInstantApiServiceFactory(retrofitProvider);
  }

  public static MyInstantApiService provideMyInstantApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMyInstantApiService(retrofit));
  }
}
