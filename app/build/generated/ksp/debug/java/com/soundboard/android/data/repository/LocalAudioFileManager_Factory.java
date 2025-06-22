package com.soundboard.android.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocalAudioFileManager_Factory implements Factory<LocalAudioFileManager> {
  private final Provider<Context> contextProvider;

  public LocalAudioFileManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocalAudioFileManager get() {
    return newInstance(contextProvider.get());
  }

  public static LocalAudioFileManager_Factory create(Provider<Context> contextProvider) {
    return new LocalAudioFileManager_Factory(contextProvider);
  }

  public static LocalAudioFileManager newInstance(Context context) {
    return new LocalAudioFileManager(context);
  }
}
