# EinsatzPulse Alarm v0.4.0-beta.1

## Neu: Android-Kopplung per QR-Code
- QR-Code **serverseitig erzeugt** in EinsatzPulse → Meine Alarme → Android-Geräte.
- Ein QR enthält nur geprüfte HTTPS-Weborigin, einen einmaligen, auf 10 Minuten
  beschränkten Kopplungscode, Version und Ablaufzeit.
- Native Android-App mit Button „QR-Code scannen“ und Google Code Scanner.
- Prüfung von QR-Format, Schema, HTTPS-Host, Code und Ablaufzeit.
- Servername wird vor der Kopplung sichtbar angezeigt und muss vom Nutzer
  bestätigt werden; keine automatische Anmeldung durch Scannen.
- Manueller Kopplungscode bleibt möglich, insbesondere ohne Google Play Dienste.
- Keine zusätzliche Kamera-Berechtigung der App erforderlich.

## Voraussetzungen
EinsatzPulse-Backend mit QR-Generator aus PR #118 und Python-Abhängigkeit
`segno>=1.6,<2`; HTTPS-originierter Webzugriff. Der Einmalcode und seine
Aktivierung verwenden dieselbe bestehende sichere Server-API.

## Sicherheit und Reife
Die APK ist eine debugsignierte Beta ohne installationsspezifische
Firebase-Konfiguration. Realgeräte-Tests für QR-Scanner, FCM, DND und
Sperrbildschirm fehlen. Das private Backend-Repo hat den bekannten
GitHub-CI-Runner-Blocker. Keinesfalls als alleinigen BOS-Alarmierungsweg nutzen.
