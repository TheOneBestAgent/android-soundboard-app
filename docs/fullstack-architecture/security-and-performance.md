# Security and Performance

## Security Requirements

### Authentication & Authorization
```typescript
// JWT token structure
interface JWTPayload {
  userId: string;
  email: string;
  subscriptionTier: 'free' | 'pro' | 'enterprise';
  permissions: string[];
  iat: number;
  exp: number;
}

// Rate limiting configuration
const rateLimitConfig = {
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP',
  standardHeaders: true,
  legacyHeaders: false,
};

// Input validation
const createSoundboardSchema = {
  name: {
    type: 'string',
    minLength: 1,
    maxLength: 100,
    pattern: '^[a-zA-Z0-9\\s\\-_]+$'
  },
  description: {
    type: 'string',
    maxLength: 500
  },
  isPublic: {
    type: 'boolean'
  }
};
```

### Data Protection
```yaml
Encryption:
  - Data at Rest: AES-256 encryption for local file storage and SQLite database
  - Data in Transit: TLS 1.3 for all API communications
  - Sensitive Data: Field-level encryption for PII

Access Control:
  - Principle of Least Privilege
  - Role-based access control (RBAC)
  - API key rotation every 90 days
  - Multi-factor authentication for admin accounts

Compliance:
  - GDPR compliance for EU users
  - CCPA compliance for California users
  - SOC 2 Type II certification
  - Regular security audits and penetration testing
```

## Performance Optimization

### Frontend Performance
```typescript
// React Native performance optimizations
const SoundButton = React.memo(({ button, onPress }: SoundButtonProps) => {
  const handlePress = useCallback(() => {
    onPress(button.id);
  }, [button.id, onPress]);
  
  return (
    <Pressable
      onPress={handlePress}
      style={styles.button}
      android_ripple={{ color: '#ffffff20' }}
    >
      <Text style={styles.buttonText}>{button.name}</Text>
    </Pressable>
  );
});

// Audio optimization
const useAudioPlayer = () => {
  const audioCache = useRef(new Map<string, Sound>());
  
  const preloadAudio = useCallback(async (audioUrl: string) => {
    if (!audioCache.current.has(audioUrl)) {
      const sound = new Sound(audioUrl, Sound.MAIN_BUNDLE, (error) => {
        if (error) {
          console.error('Failed to load audio:', error);
        }
      });
      audioCache.current.set(audioUrl, sound);
    }
  }, []);
  
  const playAudio = useCallback((audioUrl: string) => {
    const sound = audioCache.current.get(audioUrl);
    if (sound) {
      sound.play();
    }
  }, []);
  
  return { preloadAudio, playAudio };
};
```

### Backend Performance
```typescript
// Database query optimization
class SoundboardRepository {
  async findUserSoundboards(userId: string, page = 1, limit = 20) {
    return this.prisma.soundboard.findMany({
      where: { userId },
      include: {
        buttons: {
          select: {
            id: true,
            name: true,
            audioUrl: true,
            position: true,
          },
          orderBy: { position: 'asc' },
        },
        _count: {
          select: { buttons: true },
        },
      },
      orderBy: { updatedAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit,
    });
  }
}

// Caching strategy
class CacheService {
  private redis: Redis;
  
  async get<T>(key: string): Promise<T | null> {
    const cached = await this.redis.get(key);
    return cached ? JSON.parse(cached) : null;
  }
  
  async set(key: string, value: any, ttl = 3600): Promise<void> {
    await this.redis.setex(key, ttl, JSON.stringify(value));
  }
  
  async invalidatePattern(pattern: string): Promise<void> {
    const keys = await this.redis.keys(pattern);
    if (keys.length > 0) {
      await this.redis.del(...keys);
    }
  }
}
```

---
