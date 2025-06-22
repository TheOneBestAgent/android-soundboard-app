package com.soundboard.android.di;

import android.content.Context;
import com.soundboard.android.data.database.SoundboardDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideSoundboardDatabaseFactory implements Factory<SoundboardDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideSoundboardDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SoundboardDatabase get() {
    return provideSoundboardDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideSoundboardDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideSoundboardDatabaseFactory(contextProvider);
  }

  public static SoundboardDatabase provideSoundboardDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSoundboardDatabase(context));
  }
}
