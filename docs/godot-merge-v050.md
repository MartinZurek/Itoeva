# Itoeva Godot v0.5.0: Auslieferung und Merge-Abgleich

Stand: 13.10.2026. Das spielbare Testpaket ist **0.5.0 / Code 15**.

Die Laufzeitdateien entsprechen der bereits geprueften Auslieferung. Martin hat
am 13.10.2026 um 18:41 Uhr die Veroeffentlichung nach MartinZurek/Itoeva und
die Vorbereitung eines neuen Pull Requests ausdruecklich freigegeben.

## Richtiger Quellstand

Die historische Commit-ID `faf3b01c34354aca7d08eb193bb22904b191edeb` ist
im erhaltenen Checkout nicht mehr verfuegbar. Der wiederhergestellte Quellcommit
ist `c074e2a8e7cc0c20f2fb34d8e22cf7ecc3181437`, mit Elterncommit
`a2f06286efd839399bfdbaee1df82d9697ef6ff0`. Alle 205 Laufzeit-/Exportdateien
dieses Commits wurden erneut bytegenau gegen das unten genannte Manifest geprueft.
Die Veroeffentlichung ueber die GitHub-Verbindung erzeugt eine neue Commit-ID.
Fuer den Build den aktuellen Head von `codex/godot-card-arena-v050` verwenden
und das Manifest pruefen, statt die fehlende historische Commit-ID vorauszusetzen.

Der vorbereitete Branch `codex/godot-card-arena-v050` basiert auf
`a2f06286efd839399bfdbaee1df82d9697ef6ff0` (PR #359), der die aktuelle
main-Basis `124186c77f20954321008b0bf373a1527562f11a` bereits enthaelt.
Er uebernimmt alle Spiel-/Bilddateien aus `Itoeva_Godot_Quellen_v0.5.0.zip`.
Historische Repository-Vorschauen bleiben zusaetzlich erhalten. Die Kotlin-Module,
Workflows, Signierschluessel und nativen Datenvertraege werden nicht veraendert.

**PR #359 ist bisher nur 0.2.0.** Seine gruene Android-CI beweist keine v0.5.0.
Die aktuelle Erweiterung wird im eigenen Branch fuer einen neuen PR vorbereitet.
Die anfaengliche automatische Ablehnung des Pushs ist durch Martins ausdrueckliche
Freigabe geklaert. main bleibt bis zum anschliessenden Merge unveraendert.

## Vollstaendigkeit

| Vergleich | Vorherige Dateien | Fehlende Dateien in 0.5.0 |
|---|---:|---:|
| Drive-Quellen 0.2.8 | 252 | 0 |
| Ausgelieferte Quellen 0.3.0 | 270 | 0 |
| Ausgelieferte Quellen 0.4.0 | 311 | 0 |

Die Bilddateien aus 0.2.8 sind byte-identisch. Die aktuelle ZIP enthaelt 411
Dateien einschliesslich Pruefbelegen; 0.3.0 nach 0.4.0 ergaenzt 41 Dateien,
0.4.0 nach 0.5.0 weitere 100. Veraenderte Skripte setzen die neuen Funktionen um.
Alle 205 Laufzeit-/Exportdateien im vorbereiteten Branch stimmen bytegenau mit
der ausgelieferten ZIP ueberein. Der kanonische Hash ueber Pfad und Datei-SHA-256
steht in `godot-v050-runtime-manifest.json`:
`d7b854b204cf3d99954c210cf6b8593d12d5998b22bde6709a03401a63c2a1de`.
Nur Dokumentation und zwei historische Tests sind zusaetzlich aktualisiert.

## Funktionen und Regressionen

Welt, Innenraeume, Tueren, NPCs, Tageszeit, Licht, Wasser und Bewegung bleiben
enthalten. Der Schattenwald und Siegesabschluss bleiben enthalten. Kartenkampf,
Deckeditor, Kristalle, Spezialkraefte, acht Wesen und animierte Arenaminions
liegen gemeinsam im aktuellen Projekt. Die 48 Posen der zwei neuen Wesen werden
korrekt getrennt von den 170er-Atlanten der sechs Originalwesen geprueft.

Erneut ausgefuehrt: Kartenmodell 95, Regeln 15, Spezialregeln 82, Siegesablauf 96,
Karten-UI 69, Minion-Touch 24 und Waldtest 22.027 Checks ohne Fehler; acht von acht
vollstaendigen Kartenkaempfen gewonnen. Welt-Smoke: 78 Checks, Framing: 88 Raum-/
Spezies-Kombinationen, Tueren: 176 echte Spezies-/Tuerpfade, jeweils ohne Fehler.
Godot-Import und Diff-Whitespace-Pruefung sind erfolgreich. Headless-Lauf;
Telefonstart, GPU/FPS und ein neuer APK-Build aus dem integrierten Branch fehlen.

## APK und Quellen in Drive

Ziel ist der bisherige [Notime-Ordner](https://drive.google.com/drive/folders/1_8rk3o_iSfuK0kniFqGq5CO0Vv0YAM7c).
Die bereitgestellte APK ist unveraendert, 109.962.996 Bytes gross und hat SHA-256
`ae6265cab89a9b5c95d6a805e62577462c510b28e9622274e7da44f0512ab252`.
Das Original-Quell-ZIP ist 179.518.891 Bytes gross und hat SHA-256
`137b289456dfaf56b7fe313d5299d7c0fb4167821225a7e4ab610aea53d13317`.

Der Uploadweg erlaubt maximal 104.857.600 Bytes je Datei. Deshalb liegen APK und
Quell-ZIP je als `.001` und `.002` im Ordner. **Die Teile sind nicht direkt
installierbar und nicht einzeln entpackbar.** Auf einem PC beide Teile herunterladen
und binaer verbinden; unter Windows im Downloadordner mit Eingabeaufforderung:

```bat
copy /b Itoeva_Godot_Test_v0.5.0.apk.001+Itoeva_Godot_Test_v0.5.0.apk.002 Itoeva_Godot_Test_v0.5.0.apk
copy /b Itoeva_Godot_Quellen_v0.5.0.zip.001+Itoeva_Godot_Quellen_v0.5.0.zip.002 Itoeva_Godot_Quellen_v0.5.0.zip
```

Oder `Itoeva_v0.5.0_Dateien_zusammenfuegen.ps1` mit PowerShell ausfuehren: Das
Skript prueft die fertigen Dateien gegen die obigen Hashes. Erst danach APK aufs
Telefon kopieren und installieren. Die vollstaendige APK bleibt auch im Chat
direkt herunterladbar. Gameplay-Video und Pruefbericht liegen als normale Dateien
in Drive. Alte Drive-Versionen bleiben erhalten.

## Lokale Merge-Vorbereitung verwenden

Das Merge-Paket enthaelt ein Git-Bundle mit dem neuen Commit und alle neuen
Bilder. Es benoetigt die vorhandene PR-359-Basis, keinen rekonstruierten lokalen
Basiscommit aus einem alten ZIP. Im bestehenden GitHub-Checkout:

```bash
git fetch origin codex/godot-full-painted-world
git bundle verify /pfad/Itoeva_v0.5.0_Merge.bundle
git fetch /pfad/Itoeva_v0.5.0_Merge.bundle refs/heads/codex/godot-card-arena-v050:refs/heads/codex/godot-card-arena-v050
git switch codex/godot-card-arena-v050
```

Vor dem spaeteren Merge aktuellen main-Head und CI am neuen Commit erneut pruefen.
Der vorbereitete Stand passt konfliktfrei zur oben genannten main-Basis. Das ist
keine Zusage fuer eine inzwischen geaenderte main-Version. Der normale native
Deliver-APK-Workflow baut keine Godot-Test-APK; zum manuellen Godot-Bau das Preset
**AndroidTest** verwenden. Die private Testsignatur separat bereitstellen.
