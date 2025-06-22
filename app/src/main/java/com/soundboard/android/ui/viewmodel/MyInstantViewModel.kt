package com.soundboard.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundboard.android.data.repository.MyInstantRepository
import com.soundboard.android.network.api.MyInstantResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MyInstantViewModel @Inject constructor(
    private val myInstantRepository: MyInstantRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<MyInstantResponse>>(emptyList())
    val searchResults: StateFlow<List<MyInstantResponse>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<String?>(null)
    val currentlyPlaying: StateFlow<String?> = _currentlyPlaying.asStateFlow()

    private val _downloadingItems = MutableStateFlow<Set<String>>(emptySet())
    val downloadingItems: StateFlow<Set<String>> = _downloadingItems.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        // Initialize categories and load trending sounds
        _categories.value = myInstantRepository.getCategories()
        loadTrendingSounds()
    }

    fun searchSounds(query: String) {
        if (query.isBlank()) {
            loadByCategory(_selectedCategory.value)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            myInstantRepository.searchSounds(query)
                .onSuccess { sounds ->
                    _searchResults.value = sounds
                    if (sounds.isEmpty()) {
                        _errorMessage.value = "No sounds found matching '$query'"
                    }
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Search failed"
                    _searchResults.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun loadByCategory(category: String) {
        _selectedCategory.value = category
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            myInstantRepository.loadByCategory(category)
                .onSuccess { sounds ->
                    _searchResults.value = sounds
                    if (sounds.isEmpty()) {
                        _errorMessage.value = "No sounds found for '$category'"
                    }
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load sounds"
                    _searchResults.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun loadTrendingSounds() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            myInstantRepository.getTrendingSounds()
                .onSuccess { sounds ->
                    _searchResults.value = sounds
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load trending sounds"
                    // Fallback to recent sounds
                    loadRecentSounds()
                }

            _isLoading.value = false
        }
    }

    private fun loadRecentSounds() {
        viewModelScope.launch {
            myInstantRepository.getRecentSounds()
                .onSuccess { sounds ->
                    _searchResults.value = sounds
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load sounds"
                    _searchResults.value = emptyList()
                }
        }
    }

    fun loadBestSounds() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            myInstantRepository.getBestSounds()
                .onSuccess { sounds ->
                    _searchResults.value = sounds
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load best sounds"
                    _searchResults.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun togglePlayback(soundId: String) {
        viewModelScope.launch {
            val sound = _searchResults.value.find { it.id == soundId }
            if (sound != null) {
                if (_currentlyPlaying.value == soundId) {
                    // Stop current playback
                    stopPlayback()
                } else {
                    // Start new playback
                    startPlayback(sound)
                }
            }
        }
    }

    private suspend fun startPlayback(sound: MyInstantResponse) {
        // Stop any current playback first
        myInstantRepository.stopPreview()
        _currentlyPlaying.value = sound.id

        myInstantRepository.previewSound(sound.mp3)
            .onSuccess {
                // Preview started successfully
            }
            .onFailure { error ->
                _errorMessage.value = "Failed to preview sound: ${error.message}"
                _currentlyPlaying.value = null
            }
    }

    private fun stopPlayback() {
        myInstantRepository.stopPreview()
        _currentlyPlaying.value = null
    }

    fun downloadSound(sound: MyInstantResponse, onComplete: (String, String) -> Unit) {
        viewModelScope.launch {
            _downloadingItems.value = _downloadingItems.value + sound.id
            _errorMessage.value = null

            myInstantRepository.downloadSound(sound.mp3, sound.title)
                .onSuccess { filePath ->
                    _successMessage.value = "Downloaded '${sound.title}' successfully!"
                    onComplete(sound.title, filePath)
                }
                .onFailure { error ->
                    _errorMessage.value = "Download failed: ${error.message}"
                }

            _downloadingItems.value = _downloadingItems.value - sound.id
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Stop any playing audio when ViewModel is cleared
        myInstantRepository.stopPreview()
    }
} 