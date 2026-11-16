# EinsatzPulse Alarm · Android

**Aktueller Stand: v0.4.0-beta.1** – Multi-Einheiten-Zustellung, kontogebundene FCM-Jobs, persistente Quittierungen und sicherer Gerätewiderruf.

Native **Android-Alarm-App für EinsatzPulse** auf Kotlin/Jetpack Compose + Material 3.
Eigenes Projekt/Repository nach dem Gradle-/APK-CI-Muster von Tromuwelle Android.
**Keine WebView**, keine eingebetteten Passwörter und keine universelle Ausnahme von Android-Ruhemodi.

## Funktionen
- HTTPS-Server verbinden und mit einem zehn Minuten gültigen Einmalcode aus **Meine Alarme → Android-Geräte** koppeln.
- Individueller widerrufbarer Gerätezugang verschlüsselt per Android Keystore (AES-GCM), nur Hash serverseitig.
- Persönliche einsatz-/einheitsbasierte Live-Liste, Rückmeldung Ich komme/Später/Nein und Verfügbarkeitsstatus.
- Native FCM-Datenpushes mit *ausschließlich* Einsatz-/Einheiten-IDs. Einsatzdetails nur nach serverseitig autorisiertem GET.
- Separate Empfangs-, Anzeige- und Öffnungsbestätigungen; Rückmeldung nur nach HTTP-Serverbestätigung.
- Vier **eigenständig erzeugte** Signalfolgen (Signal, Sirene, Klangfolge, Dringend) mit Tonvorschau.
- Individuelle Android-Notification-Channels mit USAGE_ALARM, Vibration und optionalem DND-Prioritätskanal.
- Gerätekonfiguration über EinsatzPulse-Weboberfläche (ein/aus, Ton, Vibration, DND-Priorität, Widerruf).
- Standardmodus: keine dauerhaften Hintergrunddienste. FCM-Nachrichten werden vom Android-System empfangen, wenn verfügbar.

## Firebase für echte Pushzustellung
1. Firebase-Projekt erstellen und Android-App mit Paketname `de.einsatzpulse.alarm` registrieren.
2. JSON der Android-App als Datei `app/google-services.json` bereitstellen (lokal) oder als Secret
   `FIREBASE_GOOGLE_SERVICES_JSON_B64` (base64-kodierter Dateiinhalt) in GitHub Actions eintragen.
3. Serverseitig FCM-HTTP-v1-Dienstkonto in `ALARM_FIREBASE_SERVICE_ACCOUNT_JSON` einrichten, siehe Hauptrepo
   [docs/android-alarmierung.md](https://github.com/derdigitalaffine/einsatzpulse/blob/main/docs/android-alarmierung.md).
4. Keine Firebase-Schlüssel fest im Quelltext. **Ohne die Client-Konfiguration entsteht zwar eine APK, aber keine FCM-Verbindung.**

## APK bauen
```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
Android SDK 35, JDK 17, Gradle 8.11.1. Auf Branch/PR lädt GitHub Actions das
**Debug-APK** unter »Actions → Native Alarm APK → Artifacts« hoch.
Ein `release/vX.Y.Z`-Branch erzeugt zusätzlich ein GitHub-Prerelease mit Debugsignatur.
Produktive APKs benötigen einen eigenen gesicherten Release-Keystore und reale Geräte-Abnahme.

## Kritische Grenzen
- DND-Vorrang nur mit explizitem **Benutzerzugriff auf die Android-Benachrichtigungsrichtlinie**
  und aktivierter Servereinstellung; Betriebssystem und geänderte Channels behalten die Hoheit.
- Ohne Google-Play-Dienste oder bei Energiespar-/Netzproblemen wird kein zuverlässiges Echtzeit-Push garantiert.
- HTTPS mit gültigem, vom Android-System vertrautem Zertifikat erforderlich.
- Keine telefonische Alarmannahme, keine Leitstellen-/DME-Schnittstelle und **kein Ersatz offizieller BOS-Alarmierung**.
- Erforderliche Freigabe: reales Android-13/14/15/16-Gerät, Sperrbildschirm/DND, FCM, Reconnect, Widerruf, Lasttests und Datenschutz.

## Alarmzuverlässigkeit: zweite Entwicklungsrunde
- Die FCM-Rückrufmethode verarbeitet keine blockierenden Serverabfragen mehr. Ein priorisierter `WorkManager`-Auftrag übernimmt die Authentifizierungs- und Einsatzprüfung.
- Pushdaten haben ein serverseitiges Sendezeitfeld; nach 120 Sekunden werden sie verworfen. Offline-Rückmeldungen werden **nicht** als bestätigt gewertet.
- Dieselbe Einsatz-/Einheitenkombination wird auf Android 24 Stunden nicht erneut akustisch alarmiert; andere Einheiten desselben Einsatzes besitzen getrennte Notification-IDs und PendingIntents.
- Ein Web-initiierter **TESTALARM – KEIN EINSATZ** erzeugt keine Einsatzdaten und besitzt keine Teilnahmeaktionen. Nur mit vorbereitetem Firebase-Server nutzbar.
- Android-Systemausnahmen für „Nicht stören“ sind weiterhin freiwillig und keine Garantie für die Übersteuerung des Gerätes.

## Android-Gerätebereitschaft (v0.2)
Unter **Gerät → Android-Berechtigungen** werden getrennt angezeigt:
Benachrichtigungsfreigabe, DND-Richtlinienzugriff, Akkuausnahme und tatsächlich
vorbereiteter Prioritätskanal. Der Nutzer kann die jeweiligen Android-Einstellungen
direkt öffnen und den eigenen Kanal ausdrücklich vorbereiten. Kein automatischer
Berechtigungsdialog und keine Umgehung der Android-Systemregeln. Selbst bei allen
grünen Anzeigen bleibt FCM bestmöglich und kann durch Systemrichtlinien ausfallen.

### Android 8–11
Exklusive High-Priority-FCM-Verarbeitung über WorkManager stellt den für ältere
Android-Versionen erforderlichen zeitweiligen Vordergrundhinweis bereit. Ohne
ForegroundInfo kann ein beschleunigter Job auf Android 11 und älter abstürzen;
die Prüfung läuft deshalb mit einem generischen, nicht-sensiblen Low-Priority-Kanal.

## v0.3: kontogebundene Alarmprüfung und quittierbare Entkopplung
- Pending-Alarmjobs, lokale Deduplizierung und Benachrichtigungsaktionen sind
  an einen SHA-256-Fingerabdruck der aktuell gekoppelten Server-/Geräteanmeldung
  gebunden. Ehemalige Konten dürfen nach Abmelden/Umkoppeln keine Rückmeldung
  im neuen Konto auslösen; es werden keine Bearer Tokens in Worker-Daten abgelegt.
- `received`, `shown` und `opened` werden mit dauerhaften, getrennten
  WorkManager-Jobs (Retry bei Netzfehlern) an das Backend gemeldet. Eine lokale
  Anzeige bleibt jedoch **kein unabhängiger Beweis** für BOS-Zustellung.
- `Gerät am Server widerrufen` ruft den authentifizierten Widerruf auf und
  entfernt Zugangsdaten erst nach erfolgreicher Bestätigung. `Nur lokal
  abmelden` entfernt Daten/Benachrichtigungen ohne serverseitigen Widerruf.

## Verlustrisiko bei FCM-Tokenrotation reduziert
`FirebaseMessagingService.onNewToken` beendet sich schnell und plant nur
einen dauerhaften Registrierungsauftrag ein. `PushRegistrationWorker`
fordert das aktuelle Token beim Firebase-SDK erneut ab, prüft die
Geräteanmeldung und überträgt es mit bestmöglichen, begrenzten Offline-Retries
an den Server. Das FCM-Token selbst steht nicht im WorkManager-Speicher.
Beim App-Start wird eine erneute Registrierung versucht, auch wenn der
Server noch ein altes Token gespeichert hat. Ein fehlendes oder widerrufenes
Serverrecht wird nicht durch unbeschränkte Wiederholungen umgangen.

## v0.4: QR-Kopplung

In **EinsatzPulse Web → Meine Alarme → Android-Geräte** erzeugt der Server
einen PNG-QR-Code zur Einmal-Kopplung. Die Android-App öffnet über
**QR-Code scannen** die Google Code Scanner-Oberfläche (Google Play Dienste),
ohne der App eine Kamera-Berechtigung zu geben. Der Scanner prüft strikt
das QR-Format, die Protokollversion, die HTTPS-Serveradresse, den Code und die
Ablaufzeit. Die Serveradresse wird vor dem Koppeln angezeigt und muss bewusst
durch **Gerät verbinden** bestätigt werden. Der alte manuelle Weg bleibt
für Geräte ohne Google Play Dienste verfügbar.

QR-Codes sind zehn Minuten gültige, einmalig verwendbare Zugangs-Credentials.
Nicht fotografieren, weitergeben oder öffentlich anzeigen; kein automatischer
Kontozugriff nach dem Scannen. Der Server benötigt eine gültige HTTPS-Origin.
