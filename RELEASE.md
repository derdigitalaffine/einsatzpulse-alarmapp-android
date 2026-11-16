# EinsatzPulse Alarm v0.5.0-beta.1

## Android-Gerätemodi – Server entscheidet
- Einsatzkraft-App mit eigener Teilnahme-Rückmeldung und persönlichem Geräteprofil
- Native Hallenmonitor-Ansicht mit Bereitschaft, Einsatzanzeigen und großflächigen Karten
- Native Fahrzeugmonitor-Ansicht, an ein geprüftes Fahrzeug gebunden
- Mehrere ausdrücklich erlaubte Einheiten pro Gerät, keine Rechtevererbung
- Live-Umschaltung durch die EinsatzPulse-Administration
- Fern-Demo der drei Oberflächen (1–30 Minuten, ausschließlich fiktiv)
- TEST-Hinweis aus der Administration, ohne Firebase, nur an das geöffnete Gerät
- Serverwirksamer Gerätewiderruf und berechtigungsabhängige Datenfreigabe
- Android-Bildschirm bleibt bei sichtbaren Hallen-/Fahrzeugmonitoren an
- Netzwerkfehlerbanner warnt deutlich vor veralteten Live-Daten

## Transport ohne zusätzliche Cloud-Konfiguration
Bei sichtbarer App erfolgt alle 8 Sekunden eine eigene HTTPS-Abfrage des
EinsatzPulse-Servers (nur während Lifecycle STARTED).
Das funktioniert ohne Firebase, benötigt aber Netzwerk und ein bereits
gekoppeltes Gerät. Es ist **kein Push für geschlossene oder schlafende Apps**.
FCM kann zusätzlich bei eigener Firebase-Konfiguration Android im Hintergrund
erreichen. Providerannahme ist kein Empfangs- oder Wahrnehmungsbeweis.

## Backend-Abhängigkeit
Mindeststand: EinsatzPulse Hauptrepository inkl. PR #119 und Migration
`0038_native_alarm_device_roles`. PostgreSQL sichern, Backend/Frontend bauen,
Migration ausführen und erst danach Alarm-Worker neu starten.

## Noch nicht freigegeben
Kein Ersatz für DME, POCSAG, TETRA, Leitstelle oder normierte Alarmierung.
Die APK ist debug-signiert und ohne organisationsspezifische FCM-Credentials
gebaut. Echte Geräte-/Sperrbildschirm-/Funknetzausfall-Tests sowie Backend
Django/React CI sind aufgrund fehlender GitHub-Runner bzw. fehlender
Infrastrukturtests noch offen. Die native Hallen-/Fahrzeugansicht enthält
noch nicht alle spezialisierten Browser-Widgets, GPS-/Crew-/Routing-Funktionen.
