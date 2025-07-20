# Component Architecture

## Frontend Component Organization
```
src/components/
├── common/                 # Reusable UI components
│   ├── Button/
│   ├── Input/
│   ├── Modal/
│   ├── Loading/
│   └── ErrorBoundary/
├── audio/                  # Audio-specific components
│   ├── SoundButton/
│   ├── AudioPlayer/
│   ├── WaveformVisualizer/
│   ├── VolumeSlider/
│   └── EffectsPanel/
├── soundboard/             # Soundboard components
│   ├── SoundboardGrid/
│   ├── SoundboardEditor/
│   ├── ButtonCustomizer/
│   └── LayoutSelector/
├── library/                # Library components
│   ├── LibraryBrowser/
│   ├── CategoryFilter/
│   ├── SearchBar/
│   └── DownloadManager/
├── streaming/              # Streaming components
│   ├── PlatformConnector/
│   ├── StreamingControls/
│   ├── ChatIntegration/
│   └── ViewerStats/
├── ai/                     # AI processing components
│   ├── AIProcessor/
│   ├── JobQueue/
│   ├── ModelSelector/
│   └── ProcessingProgress/
└── settings/               # Settings components
    ├── UserProfile/
    ├── AudioSettings/
    ├── IntegrationSettings/
    └── SubscriptionManager/
```

## Component Template Example
```typescript
// src/components/audio/SoundButton/SoundButton.tsx
import React, { useCallback, useMemo } from 'react';
import { Pressable, Text, StyleSheet, View } from 'react-native';
import { useAudioPlayer } from '../../../hooks/useAudioPlayer';
import { useSoundboardStore } from '../../../stores/soundboardStore';
import { SoundButton as SoundButtonType } from '../../../types/audio';
import { WaveformVisualizer } from '../WaveformVisualizer';
import { VolumeSlider } from '../VolumeSlider';

interface SoundButtonProps {
  button: SoundButtonType;
  size?: 'small' | 'medium' | 'large';
  showWaveform?: boolean;
  showVolumeControl?: boolean;
  onPlay?: (buttonId: string) => void;
  onLongPress?: (buttonId: string) => void;
}

export const SoundButton: React.FC<SoundButtonProps> = ({
  button,
  size = 'medium',
  showWaveform = false,
  showVolumeControl = false,
  onPlay,
  onLongPress,
}) => {
  const { playSound, isPlaying } = useAudioPlayer();
  const { updateButton } = useSoundboardStore();

  const handlePress = useCallback(async () => {
    try {
      await playSound(button.audioFileUrl, {
        volume: button.volume,
        pitch: button.pitch,
        effects: button.effects,
      });
      onPlay?.(button.id);
    } catch (error) {
      console.error('Failed to play sound:', error);
    }
  }, [button, playSound, onPlay]);

  const handleLongPress = useCallback(() => {
    onLongPress?.(button.id);
  }, [button.id, onLongPress]);

  const handleVolumeChange = useCallback((volume: number) => {
    updateButton(button.id, { volume });
  }, [button.id, updateButton]);

  const buttonStyle = useMemo(() => [
    styles.button,
    styles[size],
    { backgroundColor: button.color || '#007AFF' },
    isPlaying(button.id) && styles.playing,
  ], [size, button.color, isPlaying, button.id]);

  return (
    <View style={styles.container}>
      <Pressable
        style={buttonStyle}
        onPress={handlePress}
        onLongPress={handleLongPress}
        android_ripple={{ color: 'rgba(255,255,255,0.3)' }}
      >
        <Text style={styles.buttonText} numberOfLines={2}>
          {button.name}
        </Text>
        {showWaveform && button.waveformData && (
          <WaveformVisualizer
            data={button.waveformData}
            isPlaying={isPlaying(button.id)}
            style={styles.waveform}
          />
        )}
      </Pressable>
      {showVolumeControl && (
        <VolumeSlider
          value={button.volume}
          onValueChange={handleVolumeChange}
          style={styles.volumeSlider}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    margin: 4,
  },
  button: {
    borderRadius: 8,
    padding: 12,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  small: {
    width: 60,
    height: 60,
  },
  medium: {
    width: 80,
    height: 80,
  },
  large: {
    width: 100,
    height: 100,
  },
  playing: {
    transform: [{ scale: 0.95 }],
    opacity: 0.8,
  },
  buttonText: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
    fontSize: 12,
  },
  waveform: {
    marginTop: 4,
    height: 20,
  },
  volumeSlider: {
    marginTop: 8,
  },
});
```

---
