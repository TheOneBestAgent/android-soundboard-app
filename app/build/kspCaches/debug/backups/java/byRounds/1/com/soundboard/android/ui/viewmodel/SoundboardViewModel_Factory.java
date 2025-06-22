package com.soundboard.android.ui.viewmodel;

import com.soundboard.android.data.repository.SoundboardRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SoundboardViewModel_Factory implements Factory<SoundboardViewModel> {
  private final Provider<SoundboardRepository> repositoryProvider;

  public SoundboardViewModel_Factory(Provider<SoundboardRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SoundboardViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static SoundboardViewModel_Factory create(
      Provider<SoundboardRepository> repositoryProvider) {
    return new SoundboardViewModel_Factory(repositoryProvider);
  }

  public static SoundboardViewModel newInstance(SoundboardRepository repository) {
    return new SoundboardViewModel(repository);
  }
}
