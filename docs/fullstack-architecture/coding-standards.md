# Coding Standards

## Critical Rules

```typescript
// 1. Always use TypeScript strict mode
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}

// 2. Explicit error handling
// BAD
const data = await api.getData();

// GOOD
try {
  const data = await api.getData();
  return { success: true, data };
} catch (error) {
  logger.error('Failed to fetch data:', error);
  return { success: false, error: error.message };
}

// 3. Consistent async/await usage
// BAD
function fetchData() {
  return api.getData().then(data => {
    return processData(data);
  }).catch(error => {
    throw new Error('Failed to fetch');
  });
}

// GOOD
async function fetchData(): Promise<ProcessedData> {
  try {
    const data = await api.getData();
    return await processData(data);
  } catch (error) {
    throw new ApiError('Failed to fetch data', error);
  }
}

// 4. Proper component composition
// BAD
const SoundboardScreen = () => {
  const [soundboards, setSoundboards] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // 100+ lines of component logic...
};

// GOOD
const SoundboardScreen = () => {
  const { soundboards, loading, error, fetchSoundboards } = useSoundboards();
  
  return (
    <ScreenContainer>
      <SoundboardHeader />
      <SoundboardList 
        soundboards={soundboards}
        loading={loading}
        error={error}
        onRefresh={fetchSoundboards}
      />
    </ScreenContainer>
  );
};
```

## Naming Conventions

```typescript
// Files and Directories
// PascalCase for components: SoundButton.tsx
// camelCase for utilities: audioUtils.ts
// kebab-case for screens: soundboard-screen/
// UPPER_CASE for constants: API_ENDPOINTS.ts

// Variables and Functions
const userName = 'john_doe';           // camelCase
const MAX_AUDIO_SIZE = 10 * 1024 * 1024; // UPPER_CASE for constants
const isAudioPlaying = false;          // Boolean prefix: is, has, can, should

// Functions
const getUserById = (id: string) => {}; // Verb + noun
const handleButtonPress = () => {};     // Event handlers: handle + event
const validateAudioFile = () => {};     // Validation: validate + subject

// Types and Interfaces
interface User {                        // PascalCase
  id: string;
  email: string;
}

type AudioFormat = 'mp3' | 'wav' | 'flac'; // Union types

// Enums
enum SubscriptionTier {                 // PascalCase
  FREE = 'free',
  PRO = 'pro',
  ENTERPRISE = 'enterprise',
}
```

---
