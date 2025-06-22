package com.soundboard.android.ui.viewmodel;

import com.soundboard.android.data.repository.MyInstantRepository;
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
public final class MyInstantViewModel_Factory implements Factory<MyInstantViewModel> {
  private final Provider<MyInstantRepository> myInstantRepositoryProvider;

  public MyInstantViewModel_Factory(Provider<MyInstantRepository> myInstantRepositoryProvider) {
    this.myInstantRepositoryProvider = myInstantRepositoryProvider;
  }

  @Override
  public MyInstantViewModel get() {
    return newInstance(myInstantRepositoryProvider.get());
  }

  public static MyInstantViewModel_Factory create(
      Provider<MyInstantRepository> myInstantRepositoryProvider) {
    return new MyInstantViewModel_Factory(myInstantRepositoryProvider);
  }

  public static MyInstantViewModel newInstance(MyInstantRepository myInstantRepository) {
    return new MyInstantViewModel(myInstantRepository);
  }
}
