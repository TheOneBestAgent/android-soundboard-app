package com.soundboard.android.di;

import com.soundboard.android.data.dao.SoundboardLayoutDao;
import com.soundboard.android.data.database.SoundboardDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideSoundboardLayoutDaoFactory implements Factory<SoundboardLayoutDao> {
  private final Provider<SoundboardDatabase> databaseProvider;

  public DatabaseModule_ProvideSoundboardLayoutDaoFactory(
      Provider<SoundboardDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SoundboardLayoutDao get() {
    return provideSoundboardLayoutDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSoundboardLayoutDaoFactory create(
      Provider<SoundboardDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSoundboardLayoutDaoFactory(databaseProvider);
  }

  public static SoundboardLayoutDao provideSoundboardLayoutDao(SoundboardDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSoundboardLayoutDao(database));
  }
}
