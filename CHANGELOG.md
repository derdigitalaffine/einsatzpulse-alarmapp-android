# EinsatzPulse Alarm – Changelog

## v0.3.0-beta.1 (2026-11-15)
- Serverzustellung pro Gerät, Einsatz und berechtigter Einheit (EinsatzPulse Migration 0037)
- Kontogebundene FCM-WorkManager-Jobs gegen Alarmverwechslung nach Umkoppeln
- Dauerhafte, idempotente Versandversuche für Empfangs-, Anzeige- und Öffnungsquittungen
- FCM-Token-Nachregistrierung über WorkManager bei Rotation und Verbindungsausfällen
- Serverbestätigter Widerruf direkt in der Android-App
- Diagnose je Einheit in der persönlichen Webgeräteverwaltung
- Wiederverwendbare FCM-OAuth-Zugangsdaten auf Serverseite

### Betriebsgrenzen
Nicht als alleinige oder amtliche Alarmierung verwenden. DND-Übersteuerung
erfordert Android-Systemfreigaben und ist nicht garantiert. Firebase-Clientkonfiguration
und serverseitiges Dienstkonto sind installationsabhängig. Debugsignatur;
Geräte-/Sperrbildschirm-/FCM-E2E-Tests und Backend-CI-Runner sind noch offen.
