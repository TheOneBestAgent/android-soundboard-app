# Frontend Architecture

## Component Architecture

### Component Organization
```
src/
├── components/
│   ├── common/           # Reusable UI components
│   ├── audio/            # Audio-specific components
│   ├── soundboard/       # Soundboard-related components
│   ├── library/          # Library browsing components
│   ├── streaming/        # Streaming integration components
│   ├── ai/               # AI processing components
│   └── settings/         # Settings and configuration
├── screens/              # Screen-level components
│   ├── SoundboardScreen/
│   ├── LibraryScreen/
│   ├── SettingsScreen/
│   └── StreamingScreen/
├── navigation/           # Navigation configuration
├── hooks/                # Custom React hooks
├── services/             # API and native services
├── stores/               # Zustand state stores
├── utils/                # Utility functions
└── types/                # TypeScript definitions
```

### Component Template
```typescript
// Base component structure with proper TypeScript and React Native patterns
import React, { memo, useCallback, useMemo } from 'react';
import { View, StyleSheet } from 'react-native';
import { useTheme } from '../../../hooks/useTheme';

interface ComponentProps {
  // Props definition with proper TypeScript
}

export const Component = memo<ComponentProps>(({ ...props }) => {
  const theme = useTheme();
  
  // Memoized calculations
  const computedValue = useMemo(() => {
    // Expensive calculations
  }, [dependencies]);
  
  // Callback handlers
  const handleAction = useCallback(() => {
    // Event handling
  }, [dependencies]);
  
  // Dynamic styles
  const dynamicStyles = useMemo(() => StyleSheet.create({
    container: {
      backgroundColor: theme.colors.background,
    },
  }), [theme]);
  
  return (
    <View style={[styles.container, dynamicStyles.container]}>
      {/* Component content */}
    </View>
  );
});

const styles = StyleSheet.create({
  container: {
    // Static styles
  },
});
```

## State Management Architecture

### State Structure
```typescript
// Global state structure using Zustand
interface AppState {
  // User state
  user: {
    profile: User | null;
    preferences: UserPreferences;
    subscription: SubscriptionInfo;
  };
  
  // Audio state
  audio: {
    currentlyPlaying: string[];
    volume: number;
    effects: AudioEffect[];
    isRecording: boolean;
  };
  
  // Soundboard state
  soundboards: {
    current: Soundboard | null;
    list: Soundboard[];
    isLoading: boolean;
    error: string | null;
  };
  
  // Library state
  library: {
    categories: Category[];
    searchResults: SoundButton[];
    downloads: DownloadProgress[];
  };
  
  // Streaming state
  streaming: {
    isLive: boolean;
    platform: StreamingPlatform | null;
    viewers: number;
    chatMessages: ChatMessage[];
  };
  
  // AI processing state
  ai: {
    jobs: AIProcessingJob[];
    models: AIModel[];
    isProcessing: boolean;
  };
}
```

### State Management Patterns
- **Slice Pattern:** Each domain has its own store slice
- **Computed Values:** Derived state using selectors
- **Optimistic Updates:** Immediate UI updates with rollback
- **Persistence:** Selective state persistence to AsyncStorage
- **Middleware:** Logging, error handling, and sync middleware

## Routing Architecture

### Route Organization
```typescript
// Navigation structure using React Navigation
const RootNavigator = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="Auth" component={AuthNavigator} />
        <Stack.Screen name="Main" component={MainNavigator} />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const MainNavigator = () => {
  return (
    <Tab.Navigator>
      <Tab.Screen name="Soundboard" component={SoundboardStack} />
      <Tab.Screen name="Library" component={LibraryStack} />
      <Tab.Screen name="Streaming" component={StreamingStack} />
      <Tab.Screen name="Settings" component={SettingsStack} />
    </Tab.Navigator>
  );
};
```

### Protected Route Pattern
```typescript
// Authentication guard for protected routes
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isLoading } = useAuthStore();
  
  if (isLoading) {
    return <LoadingScreen />;
  }
  
  if (!user) {
    return <AuthScreen />;
  }
  
  return <>{children}</>;
};
```

## Frontend Services Layer

### API Client Setup
```typescript
// Centralized API client with interceptors
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const apiClient = axios.create({
  baseURL: process.env.API_BASE_URL,
  timeout: 10000,
});

// Request interceptor for authentication
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);
```

### Service Example
```typescript
// Service layer for API interactions
export class SoundboardService {
  async getAllSoundboards(): Promise<Soundboard[]> {
    const response = await apiClient.get('/api/soundboards');
    return response.data;
  }
  
  async createSoundboard(data: CreateSoundboardRequest): Promise<Soundboard> {
    const response = await apiClient.post('/api/soundboards', data);
    return response.data;
  }
  
  async uploadAudio(file: File, onProgress?: (progress: number) => void): Promise<string> {
    const formData = new FormData();
    formData.append('audio', file);
    
    const response = await apiClient.post('/api/upload/audio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        const progress = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );
        onProgress?.(progress);
      },
    });
    
    return response.data.url;
  }
}

export const soundboardService = new SoundboardService();
```

---
