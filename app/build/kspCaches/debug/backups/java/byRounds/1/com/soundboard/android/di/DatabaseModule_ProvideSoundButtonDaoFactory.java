package com.soundboard.android.di;

import com.soundboard.android.data.dao.SoundButtonDao;
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
public final class DatabaseModule_ProvideSoundButtonDaoFactory implements Factory<SoundButtonDao> {
  private final Provider<SoundboardDatabase> databaseProvider;

  public DatabaseModule_ProvideSoundButtonDaoFactory(
      Provider<SoundboardDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SoundButtonDao get() {
    return provideSoundButtonDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSoundButtonDaoFactory create(
      Provider<SoundboardDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSoundButtonDaoFactory(databaseProvider);
  }

  public static SoundButtonDao provideSoundButtonDao(SoundboardDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSoundButtonDao(database));
  }
}
