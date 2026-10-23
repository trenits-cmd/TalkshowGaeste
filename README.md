# Talkshow Gäste

[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0-blue)](https://github.com/trenits-cmd/TalkshowGaeste/releases/tag/v1.0.0)

Eine native Android-App (Java, XML-Layouts, Material Design 3) für deutsche politische Talkshows mit Sendezeiten, Sendetagen und Gästen des Tages. Alle Daten werden lokal in SQLite gespeichert und können über eine JSON-Quelle aktualisiert werden.

## Features

- **Heute** – Zeigt alle Talkshows für den aktuellen Tag mit Uhrzeit, Sender, Thema und Gästen (falls veröffentlicht)
- **Sendeplan** – Wochenübersicht mit Tag-Filter (Mo–So) und Sortierung nach Uhrzeit
- **Shows verwalten** – Vollständige CRUD-Funktionalität: eigene Shows hinzufügen, bestehende bearbeiten oder löschen
- **Offline-fähig** – Gebündelte Beispieldaten in `assets/talkshows.json` sorgen für einen funktionsfähigen Erststart ohne Netz
- **Smart Merge** – Beim Remote-Update werden nur unveränderte Standard-Shows ersetzt. Eigene und lokal angepasste Shows bleiben erhalten; gelöschte Remote-Shows werden als Tombstone behandelt
- **Deutsches UI** – Komplett auf Deutsch, dunkel/hell-kompatibles Material 3-Theme
- **Portrait-optimiert** – Für Einhandnutzung auf Smartphones optimiert

## Enthaltene Shows

| Show | Sender | Sendezeit | Rhythmus |
|---|---|---|---|
| Markus Lanz | ZDF | Di, Mi, Do – 23:15 | 3× pro Woche |
| Maybrit Illner | ZDF | Do – 22:15 | Wöchentlich |
| Caren Miosga | ARD | So – 21:45 | Wöchentlich |
| Maischberger | ARD | Di, Mi – 22:50 | 2× pro Woche |
| Kölner Treff | WDR | Fr – 22:00 | Wöchentlich |
| NDR Talk Show | NDR | Fr – 22:00 | Wöchentlich |
| Riverboat | MDR | Fr – 22:00 | Wöchentlich |
| 3 nach 9 | Radio Bremen / NDR | Fr – 22:00 (monatlich) | Monatlich |

## Installation

### APK direkt installieren
1. Lade die neueste APK von den [GitHub Releases](https://github.com/trenits-cmd/TalkshowGaeste/releases) herunter: [`TalkshowGaeste.apk`](https://github.com/trenits-cmd/TalkshowGaeste/releases/latest/download/TalkshowGaeste.apk)
2. Übertrage die APK auf dein Android-Gerät oder öffne sie direkt
3. Erlaube „Aus dieser Quelle installieren“, falls Android danach fragt
4. App öffnen und loslegen

### Über ADB (für Entwicklung/Testing)
```bash
adb install -r TalkshowGaeste.apk
```

**Mindestanforderungen:** Android 8.0 (API 26) oder höher

## Datenquelle & Aktualisierung

- **Remote:** `https://raw.githubusercontent.com/trenits-cmd/TalkshowGaeste/main/data/talkshows.json`
- **Gebündelt:** `app/src/main/assets/talkshows.json`
- **Aktualisierung:** Über den Refresh-Button in der Toolbar oder beim App-Start
- **Merge-Logik:** 
  - Lokale (`is_local=1`) und angepasste (`is_customized=1`) Shows werden nie vom Remote überschrieben
  - Vom Remote gelöschte Standard-Shows werden als `is_deleted=1` markiert (erscheinen nicht mehr)
  - Episoden/Gäste werden bei erfolgreichem Update synchronisiert

### JSON-Schema
```json
{
  "updated": "2026-10-23T18:00:00+02:00",
  "source": "Talkshow Gäste - redaktionelle Beispieldaten",
  "shows": [
    {
      "id": "string (eindeutig)",
      "name": "string",
      "broadcaster": "string",
      "weekdays": [1,2,3,4,5,6,7],
      "time": "HH:mm",
      "frequency": "string",
      "color": "#RRGGBB",
      "description": "string"
    }
  ],
  "episodes": [
    {
      "showId": "string",
      "date": "YYYY-MM-DD",
      "time": "HH:mm",
      "topic": "string",
      "guests": ["Name (Rolle)", "..."]
    }
  ]
}
```

## Entwicklung

### Voraussetzungen
- JDK 21 (z. B. JetBrains Runtime 21)
- Android SDK (compileSdk 35, targetSdk 35, minSdk 26)
- Gradle 8.13 / AGP 8.13.2

### Build aus Quellcode
```bash
# Windows (PowerShell/CMD)
set JAVA_HOME=C:\Users\andi-\.jdks\jbr-21.0.11
.\gradlew.bat assembleRelease --no-daemon

# APK: app\build\outputs\apk\release\app-release.apk
```

### Debug-Build & Installation
```bash
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

### Projektstruktur
```
app/src/main/
├── java/de/trenits/talkshowgaeste/
│   ├── MainActivity.java      # Navigation, Dialoge, UI-Logik
│   ├── data/
│   │   ├── DatabaseHelper.java # SQLite-Schema
│   │   ├── Repository.java     # Datenzugriff, Merge, Netzwerk
│   │   ├── Show.java          # Datenmodell
│   │   └── Episode.java       # Datenmodell
│   ├── ui/                    # Adapter für RecyclerViews
│   └── util/Formats.java      # Datums-/Zeit-Formatierung
├── res/                       # Layouts, Drawables, Values
└── assets/talkshows.json      # Gebündelte Seed-Daten
```

## Quick-Test auf dem Smartphone

Im Repository liegt `tools/install-on-device.ps1`. Damit kannst du die aktuell gebaute Release-APK direkt auf angeschlossene Geräte installieren:

```powershell
.\tools\install-on-device.ps1
```

Das Skript findet automatisch ADB, listet verbundene Geräte und installiert `app\build\outputs\apk\release\app-release.apk` (falls vorhanden). Alternativ wird `TalkshowGaeste.apk` im Root genutzt.

Für einen Desktop-Shortcut kann `tools/create-desktop-shortcut.ps1` ausgeführt werden – dieser legt „Talkshow Gäste – Auf Gerät installieren.lnk“ auf dem Desktop an.

## Hinweise

- Die mitgelieferten Gäste-/Themenangaben sind **redaktionelle Beispieldaten** zu Demonstrationszwecken. Maßgeblich sind stets die offiziellen Angaben der jeweiligen Sender.
- Die Release-APK ist mit dem Android-Debug-Schlüssel signiert und für Sideloading vorgesehen (nicht für den Google Play Store).
- Die App speichert alle Daten lokal; es werden keine Tracking-Dienste verwendet.

## Lizenz

[MIT License](LICENSE) © 2026 trenits-cmd
