# API Specifications

## REST API Endpoints

### Authentication
```typescript
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh
GET  /api/auth/profile
PUT  /api/auth/profile
```

### Soundboards
```typescript
GET    /api/soundboards              # List user's soundboards
POST   /api/soundboards              # Create new soundboard
GET    /api/soundboards/:id          # Get soundboard details
PUT    /api/soundboards/:id          # Update soundboard
DELETE /api/soundboards/:id          # Delete soundboard
POST   /api/soundboards/:id/duplicate # Duplicate soundboard
```

### Sound Buttons
```typescript
GET    /api/soundboards/:id/buttons     # List buttons in soundboard
POST   /api/soundboards/:id/buttons     # Add button to soundboard
PUT    /api/buttons/:id                 # Update button
DELETE /api/buttons/:id                 # Delete button
POST   /api/buttons/:id/play            # Log button play event
```

### Audio Library
```typescript
GET    /api/library                     # Browse public libraries
GET    /api/library/search              # Search audio content
GET    /api/library/:id                 # Get library details
POST   /api/library/:id/download        # Download library content
```

### File Upload
```typescript
POST   /api/upload/audio               # Upload audio file
POST   /api/upload/bulk                # Bulk upload multiple files
GET    /api/upload/progress/:jobId     # Check upload progress
```

### AI Processing
```typescript
POST   /api/ai/process                 # Start AI processing job
GET    /api/ai/jobs                    # List user's AI jobs
GET    /api/ai/jobs/:id                # Get job status
DELETE /api/ai/jobs/:id                # Cancel job
```

### Streaming Integration
```typescript
GET    /api/streaming/platforms        # List connected platforms
POST   /api/streaming/connect          # Connect to platform
DELETE /api/streaming/disconnect/:platform # Disconnect platform
GET    /api/streaming/sessions         # List streaming sessions
```

## WebSocket Events

### Real-time Audio Streaming
```typescript
// Client to Server
'audio:stream_start'    # Start audio streaming
'audio:stream_data'     # Send audio data chunk
'audio:stream_end'      # End audio streaming

// Server to Client
'audio:stream_ready'    # Server ready to receive
'audio:stream_error'    # Streaming error
'audio:stream_complete' # Streaming completed
```

### Live Collaboration
```typescript
// Client to Server
'soundboard:join'       # Join collaborative session
'soundboard:leave'      # Leave session
'button:play'           # Play button (broadcast to others)
'button:update'         # Update button (real-time sync)

// Server to Client
'soundboard:user_joined'   # User joined session
'soundboard:user_left'     # User left session
'soundboard:button_played' # Button played by other user
'soundboard:button_updated' # Button updated by other user
```

---
