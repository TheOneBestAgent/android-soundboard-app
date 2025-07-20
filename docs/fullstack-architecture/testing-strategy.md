# Testing Strategy

## Testing Pyramid

```yaml
Testing Levels:
  Unit Tests (70%):
    - Individual functions and components
    - Business logic validation
    - Utility function testing
    - Fast execution (<1s per test)
  
  Integration Tests (20%):
    - API endpoint testing
    - Database integration
    - External service mocking
    - Component interaction testing
  
  E2E Tests (10%):
    - Critical user journeys
    - Cross-platform compatibility
    - Performance benchmarks
    - Accessibility compliance
```

## Test Organization

### Frontend Testing
```typescript
// Component testing with React Native Testing Library
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { SoundButton } from '../SoundButton';

describe('SoundButton', () => {
  const mockButton = {
    id: '1',
    name: 'Test Sound',
    audioUrl: 'test.mp3',
    position: 0,
  };
  
  it('should render button with correct name', () => {
    const { getByText } = render(
      <SoundButton button={mockButton} onPress={jest.fn()} />
    );
    
    expect(getByText('Test Sound')).toBeTruthy();
  });
  
  it('should call onPress when button is pressed', () => {
    const mockOnPress = jest.fn();
    const { getByText } = render(
      <SoundButton button={mockButton} onPress={mockOnPress} />
    );
    
    fireEvent.press(getByText('Test Sound'));
    expect(mockOnPress).toHaveBeenCalledWith('1');
  });
});

// Store testing
import { renderHook, act } from '@testing-library/react-hooks';
import { useSoundboardStore } from '../stores/soundboardStore';

describe('soundboardStore', () => {
  beforeEach(() => {
    useSoundboardStore.getState().reset();
  });
  
  it('should add soundboard to store', () => {
    const { result } = renderHook(() => useSoundboardStore());
    
    act(() => {
      result.current.addSoundboard({
        id: '1',
        name: 'Test Board',
        buttons: [],
      });
    });
    
    expect(result.current.soundboards).toHaveLength(1);
    expect(result.current.soundboards[0].name).toBe('Test Board');
  });
});
```

### Backend Testing
```typescript
// API testing with Supertest
import request from 'supertest';
import { app } from '../server';
import { prisma } from '../utils/database';

describe('POST /api/soundboards', () => {
  beforeEach(async () => {
    await prisma.soundboard.deleteMany();
  });
  
  it('should create a new soundboard', async () => {
    const soundboardData = {
      name: 'Test Soundboard',
      description: 'A test soundboard',
      isPublic: false,
    };
    
    const response = await request(app)
      .post('/api/soundboards')
      .set('Authorization', 'Bearer valid-jwt-token')
      .send(soundboardData)
      .expect(201);
    
    expect(response.body.name).toBe('Test Soundboard');
    expect(response.body.id).toBeDefined();
  });
  
  it('should return 400 for invalid data', async () => {
    const invalidData = {
      name: '', // Invalid: empty name
    };
    
    await request(app)
      .post('/api/soundboards')
      .set('Authorization', 'Bearer valid-jwt-token')
      .send(invalidData)
      .expect(400);
  });
});

// Service testing
import { SoundboardService } from '../services/SoundboardService';
import { prismaMock } from '../__mocks__/prisma';

jest.mock('../utils/database', () => ({
  prisma: prismaMock,
}));

describe('SoundboardService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  
  it('should create soundboard with valid data', async () => {
    const mockSoundboard = {
      id: '1',
      name: 'Test Board',
      userId: 'user1',
      buttons: [],
    };
    
    prismaMock.soundboard.create.mockResolvedValue(mockSoundboard);
    
    const result = await SoundboardService.create({
      name: 'Test Board',
      userId: 'user1',
    });
    
    expect(result).toEqual(mockSoundboard);
    expect(prismaMock.soundboard.create).toHaveBeenCalledWith({
      data: { name: 'Test Board', userId: 'user1' },
      include: { buttons: true },
    });
  });
});
```

### E2E Testing
```typescript
// Detox E2E testing for React Native
import { device, expect, element, by } from 'detox';

describe('Soundboard App', () => {
  beforeAll(async () => {
    await device.launchApp();
  });
  
  beforeEach(async () => {
    await device.reloadReactNative();
  });
  
  it('should create and play a sound', async () => {
    // Navigate to soundboard creation
    await element(by.id('create-soundboard-button')).tap();
    
    // Fill in soundboard details
    await element(by.id('soundboard-name-input')).typeText('Test Board');
    await element(by.id('create-button')).tap();
    
    // Add a sound button
    await element(by.id('add-sound-button')).tap();
    await element(by.id('sound-name-input')).typeText('Test Sound');
    await element(by.id('upload-audio-button')).tap();
    
    // Select audio file (mock)
    await element(by.text('test-audio.mp3')).tap();
    await element(by.id('save-sound-button')).tap();
    
    // Verify sound button appears
    await expect(element(by.text('Test Sound'))).toBeVisible();
    
    // Play the sound
    await element(by.text('Test Sound')).tap();
    
    // Verify audio playback (check for visual feedback)
    await expect(element(by.id('audio-playing-indicator'))).toBeVisible();
  });
});
```

---
