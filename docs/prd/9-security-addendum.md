# 9. Security Addendum

- **Threat Modeling:** Documented threat model for all components.
- **Transport Security:** TLS 1.3, mTLS for pairing, and certificate pinning.
- **Input Validation:** Rigorous input sanitization and validation for all user-supplied data and API responses.
- **API Security:** HMAC-signed REST calls, file-type/size validation on uploads.
- **Secure Storage:** Use of platform-native secure storage (Keychain/Keystore, EncryptedSharedPreferences).
- **Resilience:** API rate-limiting, circuit breakers, and cryptographically signed application updates.
- **Monitoring:** Logging to a SIEM, real-time health alerts, and quarterly penetration tests.
- **Compliance:** Adherence to OWASP Top 10 and GDPR principles.
