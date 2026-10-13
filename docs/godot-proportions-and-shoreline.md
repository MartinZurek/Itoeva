# Godot v0.2.4: Figuren, Bedienung und trockene Wege

Martins Auftrag: Groessen, Bodenkontakt, gummiartige Gesichtswechsel und
Ueberlappungen korrigieren; den einzelnen Aktionskreis samt Sitzsymbol vergroessern;
Gespraeche vom freien Weg ermoeglichen. Danach die ganze Laufstrecke gegen die
Kulisse pruefen, besonders beide Holzstege und das Ufer. Unklare Grenzen sollen
mit Landschaft, Steinen und Holz statt einer goldenen Linie sichtbar werden.

## Ergebnis

- Alle 28 Orte, sieben Panoramen, sechs Anschlussbilder, elf Innenraeume,
  elf Tuerpaare und sechs Wesen bleiben erhalten. Der fruehere Ausschnitt bleibt.
- Alle 25 Originaldateien bleiben byte-identisch; alle 6x170 Figurenframes bleiben
  pixel-identisch. Keine 3D-Figur, kein neues Character Design, keine Farbkorrektur.
- Fennecs Referenzhoehe bleibt bei Standardzoom innen/aussen rund 119 Pixel in
  1280x720. Hootlet bleibt die kleinere Eule (0.62). Wyrmling wechselt nicht mehr
  zwischen zwei Atlasmassstaeben. Sprunghoehe innen/aussen ist nun gleich projiziert.
- Ruhe nutzt je Blickrichtung eine passende Originalzeichnung (142/148/149).
  Wendebilder, die das Gesicht und teils die Blickrichtung kurz wechselten,
  werden beim Gehen nicht eingeschoben. Die acht Gangphasen und bestehenden
  Rennzyklen bleiben erhalten. Sitzen verwendet 93/103/110 statt des Schlafbilds.
- Figuren haben Physiklayer 2. Kontakt-Schatten treffen nur statische Flaechen
  auf Layer 1; sie landen nicht mehr auf benachbarten Figuren. Die Zeichenfolge
  folgt der Bodentiefe, ohne beim Springen die Nachbarn falsch zu uebermalen.
- Die Kamera folgt und zoomt; die gemalte Blickachse bleibt in der gesamten Welt
  fest, damit sich gemalte Baeume und Mobiliar nicht seitlich verziehen.
- Der rechte Kreis hat Radius 86 statt 62. Sprung, gerichtetes Rollen und
  500-ms-Sitzen bleiben eine Gestenflaeche. Das Sitzsymbol ist groesser; die
  Beschriftung zeigt Sitzen/Aufstehen. Sitzen stoppt den bisherigen Gehdaumen.
- Gespraeche sind auf demselben freien Boden bis 2.6 Einheiten statt 2.0 moeglich.
  Eine Wand-/Grenzpruefung verhindert Gespraeche quer ueber Hindernisse. Die Blase
  sitzt ueber der aktuellen Zeichnung und hat einen dezenten hellen Hof. Auch
  die sichtbare Figur kann angetippt werden. NPCs draengen nicht in Nachbarn hinein
  und setzen sich beim Ansprechen nicht zwangsweise hin.

## Wegeabgleich

Alle sieben Originalpanoramen und sechs Anschlussbilder wurden auf ein gemeinsames
1920x640-Koordinatensystem normalisiert und mit den Laufgrenzen verglichen.
Die farbigen Prueflinien waren nur Inspektionsbilder, keine Spielflaechen.

| Fund | Korrektur |
|---|---|
| Kleiner Holzsteg unter dem Haus: Laufband bis in Wasser/Stuetzpfosten | Eigene Anker entlang der schraeg gezeichneten Planken; hinteres niedriges Holzgeländer |
| Grosser Holzsteg: Figur stand zu weit am oberen Schilfrand | Band auf Planken/Deckkante nachgezogen; bestehendes hinteres Geländer folgt ihm |
| Erstes Anschlussbild: Physikprofil lag im See unter dem Erdweg | Gesamtes Anschlussprofil am tatsaechlich gemalten Pfad neu vermessen |
| Linker Kuestenpfad: Originalband enthielt Wasser und Ufersteine | Band auf trockenen Erdweg verschoben; kleine Steinraender an unklaren Auslaeufen |
| Zweiter Anschluss: vorderer Rand links teilweise auf Pflanzen/Steinen | Vorderkante enger an den trockenen Pfad gesetzt |
| Uebrige Panoramen/Anschluesse | Vorhandene Konturen stimmen zum durchgehenden Erdweg; bestaetigt und beibehalten |

Physikboden, unsichtbare Randwaende, Schrittbegrenzung und sichtbare Stein-/Holzanker
verwenden dasselbe Wegprofil. Die Schrittpruefung gilt fuer Gehen, Rolle und Sprung.
Kein Sprung schaltet die Wassergrenze aus.

## Pruefung

| Pruefung | Ergebnis |
|---|---|
| Originalasset-/Katalogvergleich | 25 Dateien, 6x170 Frames, 28 Orte, 11 Tuerpaare, 51 Innenmoebel erhalten |
| Laufzeit/start/menu/action (`full_smoke.gd`) | 76 Checks, 0 Fehler |
| Neuer Figuren-/Bedienungstest (`proportions_and_readability.gd`) | 670 Checks, 0 Fehler |
| Unabhaengige Planken-/Wasser-/Uferkoordinaten und Bewegung (`shoreline_alignment.gd`) | 59 Checks, 0 Fehler |
| Gesamte Geometrie (`visual_geometry.gd`) | 2048 Checks, 0 Fehler; 411/411 Wegpunkte in beiden Richtungen physisch angelaufen |
| Roll-/Sprung-/Gehgrenzen (`compact_boundaries.gd`) | 291 Checks, 0 Fehler |
| Echte Touchgesten (`gestures_paths.gd`) | 117 Checks, 0 Fehler |
| Raum-/Wesen-Bildrahmen (`full_framing.gd`) | 66 Kombinationen, 0 Fehler |
| Tueren (`full_doors.gd`) | 132 Spezies/Tuerpfade, 0 Fehler |
| Desktop-Renderings (`readability_capture.gd`) | 17 gueltige Bilder; Menue, Fennec/Hootlet stehend/sitzend, beide Stege, Ufer, Gespraech, acht Bewegungsphasen |

Der Vollwegtest startet mit dem tatsaechlichen Figurenradius statt dem groesseren
Standardradius eines generischen Spawnaufrufs. Der Preload-Test wartet auf den
asynchronen Loader mit einer echten 30-s-Frist; beschleunigte Testframes duerfen
dem Bildloader nicht vorher seine Ladefrist nehmen. Beide Anpassungen behalten
die urspruenglichen Erfolgsbedingungen.

Die Screenshots zeigen Godot 4.5.1 auf Desktop-Software-OpenGL. Sie sind keine
Telefonaufnahmen. Die PCK-Erstellung mit dem Android-Preset ist erfolgreich.
Bestehende Godot-Exitwarnungen zu Testinstanzen/Ressourcen sind separat im Log
sichtbar; die Tests melden keine funktionalen Fehler.

## Build und Uebergabe

Version 0.2.4, Code 7. Der vorherige Debug-Keystore ist nach der Wiederherstellung
nicht mehr vorhanden. Das alte Android-Preset behaelt Paket
`com.itoeva.godot.hd2d.world` und signed=true. Eine APK kann den privaten
Signierschluessel nicht ersetzen.

Martin hat am 2026-10-11 ausdruecklich die separat installierbare Test-APK mit
neuem Schluessel und eigener Paketkennung sowie die Ablage im bisherigen
Drive-APK-Ordner freigegeben (Antwort: „Ja du darfst“).

- Zusaetzliches Exportpreset: AndroidTest.
- Anzeigename: Itoeva Godot Test.
- Paket: com.itoeva.godot.hd2d.worldtest.
- Datei: Itoeva_Godot_HD2D_v0.2.4_Test.apk; 78.759.206 Bytes.
- Signatur v2 und v3 sowie ZIP-Alignment erfolgreich geprueft.
- Zertifikat SHA-256: fe124bbf6e3682280f62d0ba7e0619c0fc3c266adfa74b6e99c77d4ed6318582.
- APK SHA-256: 31f2f2e331dc47ae0031b8c1ead3da9aedba29994f952945e1af421aff51f439.
- Android ab API 24, ARM64, Querformat, keine Internetberechtigung.
- APK-Katalog und visuelle Geometrie stimmen bytegenau mit dem getesteten
  Projektstand ueberein; Tests/Captures/Vorschauen sind nicht mitgepackt.
- Drive-Datei-ID: 1mxFxHVdQ0IQad6btu0Y0pjnRSYaHIYI7; im verifizierten Ordner Notime.
  Readback bestaetigt Dateinamen, Zielordner und Dateigroesse.
- Der Testkeystore wird privat und ausserhalb von Git gesichert. Kuenftige
  Testupdates muessen dieselbe Paketkennung und denselben Schluessel verwenden.

Die neue Test-App startet mit eigenen lokalen Daten und ersetzt die bisherige
Installation nicht. Eine Telefonabnahme steht noch aus.

Das vollständige Godot-Quellprojekt mit Assets, Tests und Abnahmebildern ist als
ZIP gesichert. Zusaetzlich liegt ein Patch aller Godot-Korrekturen gegen den
veroeffentlichten Basisstand a2f06286efd839399bfdbaee1df82d9697ef6ff0 bei.
Keine nativen Spielstaende, Reminder, Ortsnamen oder Speichervertraege geaendert.
Kein GitHub-Push vorgenommen. Ruecksetzen durch Revert des isolierten lokalen
Korrekturcommits.

## Verbleibende Grenzen

Die Zeichnungen besitzen keine neuen Rueckseiten; Pflanzen-/Moebelverdeckung
bleibt eine Kontur-Naeherung. Die Rollbilder sind weiterhin seitlich gezeichnet,
auch wenn die Physik in die gewischte Richtung rollt. Unterschiede zwischen
Original-Rennbildern sind nicht mit einer kuenstlichen Gesichtsverformung kaschiert.
Ruhe und Menue sind bewusst stabil, statt durch verschiedene Gesichtszeichnungen
zu wechseln. Telefonbedienung, reale GPU, FPS und Waerme brauchen noch Abnahme.
