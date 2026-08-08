# Projekt von Google Drive auf die lokale Platte umziehen

Warum, was zu tun ist, und wie der bequeme Weg aufs Telefon erhalten bleibt.

## Warum überhaupt

Gemessen auf diesem Rechner, 500 Dateien à 4 KB:

| | Schreiben | Lesen | Löschen |
|---|---|---|---|
| Google Drive (`G:`) | 14.904 ms | 6.439 ms | 5.565 ms |
| Lokale Platte (`C:`) | 497 ms | 1.092 ms | 259 ms |

**Schreiben ist rund 30-mal langsamer.** Genau das tut Gradle bei jedem Build tausendfach —
Klassendateien, Zwischenstände, Ressourcen, DEX, APK. Daher Build-Zeiten von vier Minuten und
mehr, wo lokal Sekunden stünden. Das trifft Android Studio genauso wie die Kommandozeile.

Dazu zwei stille Nebenwirkungen: Google Drive lädt die `build/`-Ordner samt allem Zwischenkram
dauerhaft hoch, und ein Sync mitten im Build kann Dateien anfassen, während Gradle sie schreibt.

## Der Grund, warum es bisher dort lag

Damit die fertige APK ohne Umweg auf dem Telefon auftaucht: Drive-App öffnen, Datei antippen,
installieren. Das ist ein guter Weg und soll erhalten bleiben.

**Man braucht dafür aber nicht das ganze Projekt in der Cloud — nur die eine fertige Datei.**

## Der Umzug

**1. Ordner verschieben**

```
G:\Meine Ablage\Notime   →   C:\dev\Notime
```

**2. `local.properties` ergänzen** (liegt im Projekt-Root, ist nicht eingecheckt)

```properties
sdk.dir=C:\\Users\\marti\\AppData\\Local\\Android\\Sdk
apkDropDir=G:/Meine Ablage/Tama
```

Der zweite Eintrag ist neu. Ab jetzt legt jeder Debug-Build die fertige APK **zusätzlich** dort
ab — und damit im Drive, wo dein Telefon sie findet. Vorwärts-Schrägstriche verwenden, auch
unter Windows.

**3. Zielordner anlegen**

`G:\Meine Ablage\Tama` — dort landet künftig nur `Tama-debug.apk`, sonst nichts.

**4. Einmal bauen**

```
gradlew :app-sim:assembleDebug
```

Die Ausgabe meldet `APK zusätzlich abgelegt: …`, sobald es geklappt hat. Ist das Laufwerk gerade
nicht eingebunden, gibt es nur eine Warnung — der Build scheitert deswegen nicht.

**5. Alten Ordner erst löschen, wenn der neue nachweislich baut.**

## Was sich dadurch ändert

- Builds laufen auf lokaler Geschwindigkeit.
- Auf dem Telefon liegt weiterhin eine aktuelle APK in Drive.
- In die Cloud wandert nur noch eine Datei statt Hunderter Megabyte Zwischenstand.

## Sicherung

Drive war bisher auch dein Netz gegen Datenverlust. Nach dem Umzug übernimmt das Git: Das
Projekt ist noch kein Repository (`git init` fehlt), eine `.gitignore` liegt aber bereits
vollständig vor. Ein privates Repository bei GitHub genügt und ist für Quelltext ohnehin das
passendere Werkzeug — mit Historie statt nur "letzter Stand".

**Wichtig:** `keystore.properties` und `*.keystore` sind in der `.gitignore` ausgenommen und
gehören da auch hin. Der Upload-Schlüssel darf nie in ein Repository — er muss aber trotzdem
gesichert werden, sonst lässt sich die App nach seinem Verlust nie wieder aktualisieren.

## Wenn du bei Drive bleiben willst

Dann wenigstens die teuersten Ordner von der Synchronisierung ausnehmen — sie machen den
Großteil der Schreibvorgänge aus und werden ohnehin bei jedem Build neu erzeugt:

```
build/
app/build/
app-sim/build/
core/build/
.gradle/
```

In der Drive-App unter *Einstellungen → Google Drive → Ordner auswählen* lassen sich einzelne
Ordner ausschließen. Das ist die halbe Lösung: Der Sync-Aufwand sinkt, die Schreibgeschwindigkeit
des Laufwerks selbst bleibt aber langsam.
