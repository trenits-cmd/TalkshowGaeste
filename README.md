# Talkshow Gäste

Eine native Android-App für deutsche politische Talkshows mit Sendezeiten, Sendetagen und Gästen des Tages. Die App zeigt für heute anstehende Sendungen mit Gästen, einen Wochen-Sendeplan und eine verwaltbare Liste aller Shows.

## Features

- **Heute** – Überblick über alle Talkshows, die heute laufen, mit Uhrzeit, Sender, Thema und Gästen (falls verfügbar)
- **Sendeplan** – Wochentageumschalter für den kompletten Sendekalender aller Shows
- **Shows verwalten** – Integrierte Datenbank: Shows hinzufügen, bearbeiten, löschen. Eigene Shows werden lokal gespeichert
- **Offline-fähig** – Beim ersten Start werden gebündelte Daten geladen; Updates können aus dem Netz gezogen werden
- **Automatisches Merge** – Remote-Updates ergänzen bestehende Shows, löschen nicht eigene Shows und respektieren lokale Anpassungen
- **Deutsches UI** – Komplett auf Deutsch, Material Design 3

## Enthaltene Shows

- Markus Lanz (ZDF)
- Maybrit Illner (ZDF)
- Caren Miosga (ARD)
- Maischberger (ARD)
- Kölner Treff (WDR)
- NDR Talk Show (NDR)
- Riverboat (MDR)
- 3 nach 9 (Radio Bremen/NDR)

## Installation (APK)

1. Lade die neueste APK aus dem [Releases](https://github.com/trenits-cmd/TalkshowGaeste/releases)-Bereich herunter (z. B. `TalkshowGaeste.apk`)
2. Öffne die APK auf deinem Android-Gerät (Android 8.0+ / API 26+)
3. Erlaube die Installation aus unbekannten Quellen, falls gefragt
4. Öffne die App

Die Release-APK ist mit dem Debug-Schlüssel signiert und für das direkte Sideloading gedacht.

## Daten & Aktualisierung

- **Remote-Datenquelle:** `https://raw.githubusercontent.com/trenits-cmd/TalkshowGaeste/main/data/talkshows.json`
- **Gebündelte Daten:** `app/src/main/assets/talkshows.json`
- **Update:** Über den Aktualisieren-Button in der App (oben rechts) oder beim App-Start
- **Persistenz:** Nutzerdefinierte Shows und lokale Bearbeitungen bleiben erhalten. Vom Remote entfernte Shows werden als gelöscht markiert, tauchen aber nicht mehr auf, falls sie nie lokal angelegt wurden

### Datenformat (Auszug)

```json
{
  "updated": "2026-10-23T18:00:00+02:00",
  "source": "Talkshow Gäste - redaktionelle Beispieldaten",
  "shows": [
    {
      "id": "markus-lanz",
      "name": "Markus Lanz",
      "broadcaster": "ZDF",
      "weekdays": [2, 3, 4],
      "time": "23:15",
      "frequency": "3× pro Woche",
      "color": "#E7B45B",
      "description": "..."
    }
  ],
  "episodes": [
    {
      "showId": "maischberger",
      "date": "2026-10-23",
      "time": "22:50",
      "topic": "...",
      "guests": ["...", "..."]
    }
  ]
}
```

## Selbst bauen

### Voraussetzungen

- Android Studio (empfohlen) oder
- JDK 21
- Android SDK mit `compileSdk 35`, `targetSdk 35`, `minSdk 26`

### Build

```bash
# JAVA_HOME auf JDK 21 setzen (Windows)
set JAVA_HOME=C:\Users\andi-\.jdks\jbr-21.0.11

# Release-APK bauen
.\gradlew.bat assembleRelease --no-daemon
```

Die fertige APK liegt dann unter: `app/build/outputs/apk/release/app-release.apk`

### Debug-Installation

```bash
.\gradlew.bat installDebug
```

## Hinweis zu den Daten

Die mitgelieferten Gäste- und Themenangaben sind **redaktionelle Beispieldaten** zu Demonstrationszwecken. Sie erheben keinen Anspruch auf Vollständigkeit oder Aktualität. Maßgeblich sind stets die offiziellen Angaben der jeweiligen Sender.

## Lizenz

MIT License - siehe [LICENSE](LICENSE)
