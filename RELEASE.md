# EinsatzPulse Alarm v0.3.0-beta.1

## Erweiterungen
- Mehrere berechtigte Einheiten erhalten separate FCM-Alarmmeldungen (Servermigration 0037)
- Geräte-/Kontowechsel unterbricht fremde ausstehende Alarmjobs und Benachrichtigungsaktionen
- Empfangen, angezeigt und geöffnet: separate dauerhafte Quittierungsaufträge
- Firebase-Tokenrotation wird offlinefest zur Serverregistrierung eingeplant
- Serverwiderruf direkt von der eigenen Android-App
- Webverwaltung mit Zustellversuchen, FCM-/Empfangsdiagnose je aktiver Einheit
- Cache für Firebase OAuth auf Serverseite

## Voraussetzungen
Dieser Client benötigt EinsatzPulse Server v0.3-kompatible native Alarm-API
mit Migration 0037 und `POST /api/alarm/native/unlink/`. Firebase muss
installationsspezifisch mit Google Services JSON und serverseitigem FCM-Dienstkonto
konfiguriert sein.

## Sicherheitshinweis
Die angebotene APK ist debug-signiert und ohne installationsspezifische
Firebase-Zugangsdaten erstellt. Sie ist nicht als alleinige, amtliche oder
garantierte BOS-Alarmierung freigegeben. FCM-, Android-Hersteller-, DND- und
Sperrbildschirm-E2E-Tests stehen aus; das private Hauptrepo hat einen
unabhängig dokumentierten CI-Runner-Blocker.
