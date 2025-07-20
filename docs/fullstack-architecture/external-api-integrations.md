# External API Integrations

## YouTube Data API v3
- **Purpose:** Search and import audio from YouTube videos
- **Documentation:** https://developers.google.com/youtube/v3
- **Base URL:** https://www.googleapis.com/youtube/v3
- **Authentication:** API Key
- **Rate Limits:** 10,000 units per day (default quota)

**Key Endpoints Used:**
- `GET /search` - Search for videos by keyword
- `GET /videos` - Get video details and metadata
- `GET /channels` - Get channel information

**Integration Notes:** Used for content discovery and import. Audio extraction handled client-side using youtube-dl or similar library.

## VoiceMeeter Remote API
- **Purpose:** Control VoiceMeeter audio mixer from the app
- **Documentation:** https://vb-audio.com/Voicemeeter/VoicemeeterRemoteAPI.htm
- **Base URL:** Local Windows API (DLL integration)
- **Authentication:** None (local application)
- **Rate Limits:** None

**Key Functions Used:**
- `VBVMR_Login()` - Connect to VoiceMeeter
- `VBVMR_SetParameterFloat()` - Set audio parameters
- `VBVMR_GetParameterFloat()` - Get current settings
- `VBVMR_Logout()` - Disconnect from VoiceMeeter

**Integration Notes:** Windows-only integration via native module bridge. Requires VoiceMeeter installation.

## OBS Studio WebSocket API
- **Purpose:** Control OBS Studio for streaming integration
- **Documentation:** https://github.com/obsproject/obs-websocket
- **Base URL:** ws://localhost:4455 (default)
- **Authentication:** WebSocket authentication with password
- **Rate Limits:** None (local connection)

**Key Events Used:**
- `GetSceneList` - Get available scenes
- `SetCurrentScene` - Switch to specific scene
- `StartStream` - Start streaming
- `StopStream` - Stop streaming
- `GetStreamStatus` - Get current stream status

**Integration Notes:** Real-time WebSocket connection for live streaming control.

## Twitch API
- **Purpose:** Stream integration and chat interaction
- **Documentation:** https://dev.twitch.tv/docs/api/
- **Base URL:** https://api.twitch.tv/helix
- **Authentication:** OAuth 2.0 with scopes
- **Rate Limits:** 800 requests per minute

**Key Endpoints Used:**
- `GET /streams` - Get stream information
- `GET /users` - Get user information
- `POST /chat/messages` - Send chat messages

**Integration Notes:** OAuth flow for user authentication and stream management.

## Discord Bot API
- **Purpose:** Discord server integration for soundboard sharing
- **Documentation:** https://discord.com/developers/docs/intro
- **Base URL:** https://discord.com/api/v10
- **Authentication:** Bot token
- **Rate Limits:** Varies by endpoint (typically 5-50 requests per second)

**Key Endpoints Used:**
- `POST /channels/{channel.id}/messages` - Send messages
- `GET /guilds/{guild.id}/channels` - Get server channels
- `POST /applications/{application.id}/commands` - Register slash commands

**Integration Notes:** Bot integration for sharing sounds and controlling playback in Discord voice channels.

---
