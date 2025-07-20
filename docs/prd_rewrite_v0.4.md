# Product Requirements Document: Soundboard App Rewrite (v0.4)

## 1. Purpose & Background

The Stream Deck–inspired Soundboard App delivers a virtual control surface for audio playback and mixing across desktop (Windows, macOS) and mobile (Android, iOS) platforms. It addresses the need for low-latency, programmable sound triggers in streaming, broadcasting, and enterprise AV environments.

## 2. Scope

### 2.1 In-Scope (MVP)
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

### 2.2 Out-of-Scope
- Real-time multi-user sharing (Phase 2).
- Browser clients or physical hardware.
- Video editing capabilities.
- Smart home automation (Alexa/Google Home integration) - Future consideration.

## 3. User Personas

| Persona          | Role                | Goals                                     | Pain Points                                |
|------------------|---------------------|-------------------------------------------|--------------------------------------------|
| Streamer Sam     | Live streamer       | Trigger sounds seamlessly during streams  | Context switching; high latency            |
| AV Engineer Alex | Enterprise AV tech  | Manage event cues reliably with monitoring| Unreliable networks; diagnostics           |
| Podcaster Pat    | Podcast host        | Insert jingles and stingers during recording | Disruptive workflows; format issues        |

## 4. Requirements

### 4.1 Functional
- **UI Grid:** 4×3–10×10 programmable with swipe/tab/dropdown board-switching; <200 MB memory footprint.
- **Button Programming:** Assign local or library clips, smart album art detection, adaptive theming, custom labels/icons/macros.
- **Playback Engine:** <10 ms trigger, volume/mute/loop controls.
- **Audio Editor Suite:** Audacity-level editing with OpenVINO AI (stem separation, noise reduction, upscaling), YouTube sampling, plugin architecture, real-time preview.
- **Third-Party Integration Engine:** RESTful API endpoints, webhook support, OBS WebSocket integration, Discord Rich Presence, streaming platform SDKs.
- **VoiceMeeter Advanced Integration:** Recorder function control, bus routing, macro execution, parameter automation, virtual cable management.
- **Connectivity:** WebSocket mTLS, USB/ADB, mDNS (Consul fallback), direct pairing.
- **Library Integration:** Voicy/MyInstants REST endpoints, LRU cache, offline support.
- **Settings Panel:** Live preview, “Preview Mode” toggle, grid/theme/button/text controls.
- **Health Dashboard:** CPU/memory/buffer metrics.
- **Analytics:** Firebase event logging (<1 min ingestion).
- **Security:** TLS 1.3, certificate pinning, mTLS for pairing.
- **Error Handling:** Retries, user notifications, cache-rebuild for corrupted data.
- **Offline Mode:** Pre-cached playback with recovery for corrupt/missing cache.

### 4.2 Non-Functional
- **Performance:** <10 ms audio, <50 ms UI updates, <100 ms preview render.
- **Reliability:** 99.9% uptime, resilient to network fluctuations.
- **Scalability:** Up to 100 buttons/board; efficient caching.
- **Usability:** Onboarding in <2 min; live preview.
- **Accessibility:** WCAG AA, keyboard navigation, screen-reader labels.
- **Compatibility:** Android API ≥21, iOS 12+, Windows 10+, macOS 10.14+.

## 5. User Flows & Wireframe References

*User flows for Pairing, Home, Library, Mixer, Settings, and Error states will be detailed in `UX-wireframes.md`.*

## 6. Acceptance Criteria

- Grid setup & live preview functions as expected.
- Local & API sounds can be assigned and played back.
- Audio trigger latency is consistently <10 ms.
- Pairing succeeds via all specified methods.
- TLS + certificate pinning is enforced on all connections.
- Analytics events appear in Firebase in <1 minute.
- Automatic retry mechanisms and user notifications are functional.
- Full keyboard and screen-reader support is implemented.
- Live preview renders in ≤100 ms.
- Smart button customization automatically detects and applies album art with <2s load time.
- Audio editor processes files with OpenVINO AI features (stem separation, noise reduction) in <30s for 3-minute clips.
- YouTube audio sampling successfully extracts audio with proper attribution and copyright compliance.
- Plugin architecture supports third- Third-party audio effects and processors.
- API endpoints respond within 200ms and handle 100+ concurrent requests.
- OBS Studio integration successfully triggers scene changes and source controls.
- VoiceMeeter recorder function starts/stops recording with <50ms latency.
- Discord bot commands execute within 500ms of soundboard triggers.

- Webhook delivery has 99.9% success rate with retry mechanisms.
- Offline playback of cached sounds works correctly.
- Cache integrity and error handling (corruption, missing files) are tested.
- Usability testing with ≥5 users shows a <5% error rate.

## 7. Backlog & Prioritization

| Priority     | Story ID | Summary                            | Sprint    |
|--------------|----------|------------------------------------|-----------|
| P0 Must-Have | 1.1      | QR code pairing                    | Sprint 1  |
| P0 Must-Have | 2.1      | Low-latency playback engine        | Sprint 1  |
| P0 Must-Have | 2.2      | Grid customization & live preview  | Sprint 1  |
| P1 Should-Have| 3.1      | Library search integration         | Sprint 2  |
| P1 Should-Have| 2.3      | Custom icons & labels              | Sprint 2  |
| P2 Could-Have| 4.1      | Health dashboard implementation    | Sprint 3  |
| P2 Could-Have| 5.1      | TLS & pinning implementation       | Sprint 3  |
| P1 Should-Have| 6.1      | Smart button customization & album art | Sprint 2  |
| P1 Should-Have| 7.1      | Advanced audio editor with OpenVINO    | Sprint 3  |
| P2 Could-Have| 7.2      | YouTube audio sampling integration      | Sprint 3  |
| P1 Should-Have| 8.1      | OBS Studio WebSocket integration        | Sprint 2  |
| P2 Could-Have| 8.2      | Discord bot and Rich Presence           | Sprint 3  |
| P1 Should-Have| 9.1      | VoiceMeeter recorder function control   | Sprint 2  |

| P2 Could-Have| 8.3      | RESTful API and webhook endpoints       | Sprint 3  |
| P3 Optional  | 4.2      | Usage analytics reporting          | Sprint 4  |
| P3 Optional  | 5.2      | Auto–reconnect after network loss  | Sprint 4  |

## 8. Dependencies

- Voicy & MyInstants APIs
- OpenVINO AI toolkit and pre-trained models for audio processing
- YouTube Data API v3 and youtube-dl/yt-dlp for audio extraction
- MusicBrainz/Last.fm APIs for album art and metadata retrieval
- Certificate management infrastructure (e.g., Let's Encrypt/Vault)
- VoiceMeeter (Windows) & macOS Core Audio SDKs
- Firebase Analytics SDK
- Consul for mDNS fallback discovery
- Audio plugin SDK (VST3/AU) integration libraries
- OBS Studio WebSocket API and plugin development kit
- Discord Developer Portal API and Rich Presence SDK
- VoiceMeeter Remote API and macro scripting engine

- Streaming platform APIs (Twitch, YouTube Live, Facebook Gaming)
- QA test mocks for mDNS/Consul services
- CI/CD pipeline for certificate rotation & feature-flag management

## 9. Security Addendum

- **Threat Modeling:** Documented threat model for all components.
- **Transport Security:** TLS 1.3, mTLS for pairing, and certificate pinning.
- **Input Validation:** Rigorous input sanitization and validation for all user-supplied data and API responses.
- **API Security:** HMAC-signed REST calls, file-type/size validation on uploads.
- **Secure Storage:** Use of platform-native secure storage (Keychain/Keystore, EncryptedSharedPreferences).
- **Resilience:** API rate-limiting, circuit breakers, and cryptographically signed application updates.
- **Monitoring:** Logging to a SIEM, real-time health alerts, and quarterly penetration tests.
- **Compliance:** Adherence to OWASP Top 10 and GDPR principles.

## 10. Architecture Deep Dive

*(This section will be expanded into a separate Technical Architecture Document)*
- System overview & data flow diagrams.
- Module breakdown: UI, pairing, audio engine, library, security, analytics.
- Interface & data contracts (WebSocket, REST, telemetry, cache schemas, preferences).
- Sequence diagrams for all 11 core user scenarios.
- Configuration management, feature flags, and observability pipeline.
- Certificate rotation strategy, disaster recovery/backup plan, version negotiation, and SDK upgrade strategy.