# Data Models

## Core Entities

```typescript
// User Management
interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatar?: string;
  subscription: SubscriptionTier;
  preferences: UserPreferences;
  createdAt: Date;
  updatedAt: Date;
}

interface UserPreferences {
  theme: 'light' | 'dark' | 'auto';
  audioQuality: 'low' | 'medium' | 'high' | 'lossless';
  defaultVolume: number;
  enableHapticFeedback: boolean;
  enableAIProcessing: boolean;
  autoBackupLocally: boolean;
}

// Audio Content
interface SoundButton {
  id: string;
  name: string;
  description?: string;
  audioFileUrl: string;
  localFilePath?: string;
  duration: number;
  waveformData?: number[];
  tags: string[];
  category: string;
  volume: number;
  pitch: number;
  effects: AudioEffect[];
  hotkey?: string;
  color?: string;
  icon?: string;
  isCustom: boolean;
  userId?: string;
  createdAt: Date;
  updatedAt: Date;
}

interface AudioEffect {
  id: string;
  type: 'reverb' | 'echo' | 'distortion' | 'filter' | 'ai_enhance';
  parameters: Record<string, number>;
  enabled: boolean;
  order: number;
}

// Soundboard Organization
interface Soundboard {
  id: string;
  name: string;
  description?: string;
  layout: SoundboardLayout;
  buttons: SoundButton[];
  isDefault: boolean;
  isShared: boolean;
  userId: string;
  createdAt: Date;
  updatedAt: Date;
}

interface SoundboardLayout {
  rows: number;
  columns: number;
  buttonPositions: Record<string, { row: number; col: number }>;
}

// Audio Library
interface AudioLibrary {
  id: string;
  name: string;
  description?: string;
  sounds: SoundButton[];
  categories: Category[];
  isPublic: boolean;
  downloadCount: number;
  rating: number;
  userId: string;
  createdAt: Date;
  updatedAt: Date;
}

interface Category {
  id: string;
  name: string;
  color: string;
  icon: string;
  soundCount: number;
}

// Streaming and Integration
interface StreamingSession {
  id: string;
  platform: 'twitch' | 'youtube' | 'discord' | 'obs';
  status: 'active' | 'paused' | 'ended';
  startTime: Date;
  endTime?: Date;
  soundsPlayed: number;
  userId: string;
}

interface VoiceMeeterConfig {
  id: string;
  name: string;
  inputChannels: VoiceMeeterChannel[];
  outputChannels: VoiceMeeterChannel[];
  isActive: boolean;
  userId: string;
}

interface VoiceMeeterChannel {
  index: number;
  name: string;
  volume: number;
  muted: boolean;
  solo: boolean;
}

// AI Processing
interface AIProcessingJob {
  id: string;
  type: 'noise_reduction' | 'voice_enhancement' | 'auto_mastering';
  status: 'pending' | 'processing' | 'completed' | 'failed';
  inputFileUrl: string;
  outputFileUrl?: string;
  parameters: Record<string, any>;
  progress: number;
  userId: string;
  createdAt: Date;
  completedAt?: Date;
}
```

## Data Relationships
- Users have many Soundboards and AudioLibraries
- Soundboards contain many SoundButtons
- SoundButtons can belong to multiple Categories
- Users can have multiple StreamingSessions
- AI processing jobs are linked to specific audio files and users

---
