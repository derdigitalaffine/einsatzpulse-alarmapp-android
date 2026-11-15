# EinsatzPulse Alarm v0.2.0-beta.1

Vorabversion des nativen Kotlin/Jetpack-Compose-Android-Clients.

## Neu
- Expedited Android WorkManager für FCM-Alarmhinweise
- Eindeutige Benachrichtigungen für mehrere Einheiten
- Deduplizierung und 120-Sekunden-Ablauf alter FCM-Nachrichten
- Deutlich markierte Testalarme ohne Änderung des Einsatzstatus
- Android-Gerätediagnose für Notification Permission, DND und Akkuoptimierung
- ForegroundInfo-Kompatibilität für ältere Android-Versionen (API 26–30)

## Einschränkungen
Die veröffentlichte APK ist debugsigniert; Firebase Client JSON ist installationsspezifisch.
Ohne konfigurierte Firebase-Zugangsdaten findet keine echte FCM-Zustellung statt.
Nicht als offizielle oder garantierte Feuerwehralarmierung verwenden.
Geräte-, Netz-, DND-, Betreiber- und Ausfallsicherheitstests fehlen weiterhin.
