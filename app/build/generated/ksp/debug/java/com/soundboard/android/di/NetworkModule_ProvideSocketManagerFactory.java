package com.soundboard.android.di;

import com.soundboard.android.network.SocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class NetworkModule_ProvideSocketManagerFactory implements Factory<SocketManager> {
  @Override
  public SocketManager get() {
    return provideSocketManager();
  }

  public static NetworkModule_ProvideSocketManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SocketManager provideSocketManager() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideSocketManager());
  }

  private static final class InstanceHolder {
    private static final NetworkModule_ProvideSocketManagerFactory INSTANCE = new NetworkModule_ProvideSocketManagerFactory();
  }
}
