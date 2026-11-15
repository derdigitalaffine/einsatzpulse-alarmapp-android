# EinsatzPulse Android · Agentenorganisation
EinsatzPulse Hauptprojekt: siehe AGENTS.md, Issues #1–#3 und docs/development-process.md.
Ziel: native Kotlin/Jetpack-Compose-Alarm-App, Material 3, explizite Berechtigungen, kein WebView.
Rollen: Android/Compose, Android Push & Notifications, Backend/RBAC, QA/CI/Security.
Loop: Observe → Triage → Plan → Implement → Tests/Build → Review → Merge.
Definition of Done: API/Client-Pairing, native FCM, Widerruf, DND-Permission-UX, Empfangsprotokoll,
installierbare APK, Geräte-/DND-/Offline-Tests. FCM-Annahme ist **keine** Zustellbestätigung.
Keine Passwörter oder Serviceaccounts im Repo; keine TLS-Verifikations-Bypasses;
fachliche Rechte autoritativ im Backend. Keine verbindliche BOS-Alarmierung behaupten.
