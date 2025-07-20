# Tech Stack Definition

## Frontend Stack
- **Framework:** React Native 0.73+
- **Language:** TypeScript 5.0+
- **State Management:** Zustand 4.4+
- **Navigation:** React Navigation 6.x
- **UI Components:** React Native Elements + Custom components
- **Audio Processing:** React Native Audio API + Native modules
- **AI Integration:** OpenVINO React Native bridge
- **Testing:** Jest + React Native Testing Library
- **Code Quality:** ESLint + Prettier + Husky

## Backend Stack (Local Desktop Server)
- **Runtime:** Node.js 18+ LTS
- **Framework:** Express.js 4.18+ (HTTP API)
- **Desktop Framework:** Electron 28+ (headless mode)
- **Language:** TypeScript 5.0+
- **Database:** SQLite 3.44+ (local file-based)
- **Audio Engine:** Web Audio API + node-speaker/PvSpeaker
- **Authentication:** JWT (local storage)
- **File Management:** Node.js fs + path modules
- **Real-time:** Socket.io 4.7+ + Electron IPC
- **System Integration:** System tray (Electron Tray API)
- **Testing:** Jest + Supertest
- **Documentation:** Swagger/OpenAPI 3.0

## Local Infrastructure Stack
- **Platform:** Cross-platform desktop (Windows/macOS)
- **Service Management:** PM2 (process management)
- **System Tray:** Electron Tray with context menu
- **Audio Output:** Native system audio drivers
- **Storage:** Local file system + SQLite
- **Networking:** Local HTTP server (localhost)
- **Monitoring:** Local logging + health checks
- **Packaging:** Electron Builder (executable)
- **Auto-start:** System service registration

## AI/ML Stack
- **Framework:** OpenVINO 2023.3+
- **Models:** Custom audio processing models
- **Training:** PyTorch (for model development)
- **Deployment:** Embedded inference on device
- **Model Format:** ONNX → OpenVINO IR

---
