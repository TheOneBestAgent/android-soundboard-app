package com.soundboard.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.soundboard.android.ui.screen.SoundboardScreen
import com.soundboard.android.ui.theme.AndroidSoundboardTheme
import com.soundboard.android.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSoundboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SoundboardScreen(settingsRepository = settingsRepository)
                }
            }
        }
    }
}

// Preview temporarily disabled due to dependency injection requirements
// @Preview(showBackground = true)
// @Composable
// fun DefaultPreview() {
//     AndroidSoundboardTheme {
//         SoundboardScreen()
//     }
// } 