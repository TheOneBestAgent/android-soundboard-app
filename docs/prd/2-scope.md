# 2. Scope

## 2.1 In-Scope (MVP)
- **Platforms:** Cross-platform desktop (Windows w/ VoiceMeeter, macOS) and mobile (Android & iOS) apps.
- **UI:** Stream-Deck–style programmable grid (4×3 default, up to 10×10; adaptive layouts for phones, tablets, resizable windows).
- **Core Functionality:** Real-time audio playback & mixing (<10 ms latency).
- **Connectivity:**
    - WebSocket (mTLS)
    - USB/ADB tunneling
    - mDNS discovery (with Consul fallback)
    - Direct mobile-to-desktop pairing
- **Enterprise Features:** Live data only, WAV/FLAC/MP3/AAC support, health dashboards, TLS 1.3 w/ certificate pinning, Firebase Analytics.
- **Library Integration:** Voicy & MyInstants REST APIs, HMAC-signed, local caching (IndexedDB/file cache; max 500 MB/board).
- **Smart Button Customization:** Auto-fetch album art from metadata/online sources, fallback options (custom images, text, emoji, gradients), adaptive color theming, haptic feedback animations.
- **Advanced Audio Editor:** Full-featured editor with OpenVINO AI integration, YouTube audio sampling, stem separation, noise reduction, audio upscaling, plugin architecture support.
- **Third-Party Integrations:** API/webhook support, streaming platform integrations (OBS Studio, Streamlabs, XSplit), Discord bot integration.
- **VoiceMeeter Integration:** Enhanced Windows integration with VoiceMeeter recorder function, direct audio routing, macro support, and real-time parameter control.
- **Settings & Customization:** Real-time live preview (≤100 ms), "Preview Mode" toggle.
- **User Onboarding:** Pairing wizard, device discovery.
- **Offline Mode:** Pre-cached board playback when disconnected.

## 2.2 Out-of-Scope
- Real-time multi-user sharing (Phase 2).
- Browser clients or physical hardware.
- Video editing capabilities.
- Smart home automation (Alexa/Google Home integration) - Future consideration.
