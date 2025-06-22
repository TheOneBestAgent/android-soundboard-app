package com.soundboard.android.di;

import com.soundboard.android.data.dao.ConnectionHistoryDao;
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
public final class DatabaseModule_ProvideConnectionHistoryDaoFactory implements Factory<ConnectionHistoryDao> {
  private final Provider<SoundboardDatabase> databaseProvider;

  public DatabaseModule_ProvideConnectionHistoryDaoFactory(
      Provider<SoundboardDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ConnectionHistoryDao get() {
    return provideConnectionHistoryDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideConnectionHistoryDaoFactory create(
      Provider<SoundboardDatabase> databaseProvider) {
    return new DatabaseModule_ProvideConnectionHistoryDaoFactory(databaseProvider);
  }

  public static ConnectionHistoryDao provideConnectionHistoryDao(SoundboardDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideConnectionHistoryDao(database));
  }
}
