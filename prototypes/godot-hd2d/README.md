# Itoeva: die gemalte Welt in Godot

Separat startbare HD-2D-Fassung von Martins Auftrag: **die vorhandene Welt als
3D-Buehne, alle sechs Wesen als originale gemalte 2D-Sprites**.
Basis ist der Godot-Ausschnitt aus PR #357; der native Android-Client bleibt ein
eigenstaendiges Projekt.

## Starten und APK

In **Godot 4.5.1** `project.godot` importieren und F5 starten.
Bilder, Katalog und Musik liegen im Projekt; Python ist zum Spielen nicht erforderlich.

```bash
godot --path prototypes/godot-hd2d
godot --headless --path prototypes/godot-hd2d --export-debug Android /absoluter/pfad/Itoeva_Godot_HD2D_v0.2.0.apk
```

Android-Export: Godot-4.5.1-Vorlage, Java 17, SDK Build-Tools 35.0.1 und eigener
Debug-Keystore in den Editor-Einstellungen. Schluessel und SDK-Pfade liegen ausserhalb
des Repositorys. Die separate Test-App **Itoeva Godot Welt** verwendet Paket
`com.itoeva.godot.hd2d.world`, Version 0.2.0 / Code 3, ARM64 ab Android 7,
Querformat, ohne Internetberechtigung. Sie kann neben der bisherigen
Ausschnitt-App installiert werden; deren Signierschluessel wird nicht vorausgesetzt.
Tests, Python-Dateien, Vorschauen und Captures werden nicht exportiert.
APK: 78.119.773 Bytes; ZIP, ARM64, Katalog, 16-KiB-Ausrichtung und Signatur v2/v3 geprueft.
SHA-256: `4954ce883463514e95ec5dbdc3173a29d2f4f416d9ec15aa40b3c9662648609c`.
Eine Pruefung auf einem echten Android-Telefon steht aus.

## Uebertragene Welt

Alle **28 Orte** teilen die nativen Ortsnamen und Tueranker. Sieben verbundene
Panoramen enthalten 17 Aussenorte; sechs originale Uebergangsbilder verbinden
die Kulissen. Elf Innenraeume verwenden ihre unveraenderten Bilder.
Alle elf Tuerpaare funktionieren in beiden Richtungen.

| Bereich | Orte |
|---|---|
| Kueste | Waldsee, Strand, Sumpf, Dschungel |
| Verbindung | Kuestenpfad mit enger Bachbruecke |
| Gruene Umgebung | Strasse, Park, Wiese, Wald |
| Verbindung | Dorfrand |
| Hochland | Marktplatz, Sportplatz, Ebene, Berge |
| Verbindung | Bergpass |
| Expedition | Lager, Grotte |
| Haus | Wohnzimmer, Schlafzimmer, Bad, Schreibzimmer, Leseecke, Kueche, Werkstatt |
| Weitere Innenraeume | Laden, Cafe, Arbeitsstube, Spielhalle |

`prepare_world.py` liest Regionen, 22 gerichtete Tueren, Innenraumanker,
51 Innenmoebel und Speziesreferenzen aus den nativen Kotlin-Quellen in
`data/world_catalog.json`. Acht Aussen-Sitzflaechen folgen dem nativen
Moebel-/Oberflaechenkatalog. Die Parkkiste verwendet dieselben Pixelrechtecke wie
`GameSurfaceView`. Konturen bilden gemalte Vordergrundkarten; dieselben
Koordinaten liefern Kollisionskoerper, Standhoehen und Sitzanker.

Der Laufstreifen ist ein horizontaler 3D-Boden. Die Bruecke folgt den sieben
nativen Laufbandankern; Wegraender begrenzen das Betreten von Wasser.
Hintergrundrelief und Boden-UV erhalten die Bildkomposition. Die begrenzte
orthografische Kamera laesst Zoom und Schwenk von maximal acht Grad zu.

## Wesen, Menues und Bewegung

- **Fennec, Gloop, Puffling, Wyrmling, Starlet, Hootlet**: je 170 unveraenderte
  Zeichnungen aus Rich und Living, nur in mobile 1536x1920-Atlanten umgepackt.
  Aufrechte `Sprite3D`-Karten, kein 3D-Rig. Native sichtbare Referenzhoehen
  bestimmen die Groessen; Fennecs Aussenmassstab bleibt 0.020.
- Anfangsmenue mit originalem Wohnzimmerbild, sechs animierten Auswahlkarten,
  originalen Leitsaetzen und fortsetzbarer Wesenwahl.
- Aktionsbedienung mit Sprung, Rolle, Sitzen und kontextueller Interaktion:
  Tueren betreten, Wesen ansprechen, auf gezeichneten Sitzankern ruhen.
- Floating- oder fester Analogstick, einstellbare Empfindlichkeit, unabhaengige
  zwei Finger. Deadzone, Beschleunigung/Bremsung und stetiger Uebergang vom
  Gehen zum Rennen ersetzen einen abrupten Tempo-Schalter.
- Laufphasen folgen der wirklich zurueckgelegten Strecke. Richtungsgetreue
  Walk-/Run-Frames, gezeichnete Anlauf-/Stoppbilder, Sprungphasen,
  kurze Sprungeingabe-Pufferung und Landereaktion bleiben im vorhandenen Design.
- Einstellungen fuer Stick, Empfindlichkeit, Abendtoenung und die vorhandene
  Theme-Musik. Musik ist beim ersten Start aus; Menue/App-Pause unterbrechen sie.
- Eigene lokale Datei `user://painted_world_v2.json` speichert Einstellungen und
  Standort je Wesen atomar. Keine Migration oder Verbindung zu nativen Spielstaenden.
- Fuenf andere Wesen sind als einfache, ansprechbare Parkbewohner sichtbar.
  Ihre Bewegung ist Darstellung, keine portierte Living-Agent-Simulation.

| Eingabe | Wirkung |
|---|---|
| WASD / Pfeile / linker Gamepadstick | Gehen |
| Shift / aeusserer Analogring | Rennen |
| Leertaste / Gamepad A | Springen |
| R / Gamepad B | Rolle |
| C / Gamepad Y | Sitzen / aufstehen |
| E / Gamepad X | Interagieren / Tuer betreten |
| Q / F oder rechter Gamepadstick | Kleiner Kameraschwenk |
| Z / X oder Mausrad | Zoom |
| Esc / Gamepad Start / Android Zurueck | Menue |
| M | Ortsuebersicht |
| L | Abendtoenung |
| Linker Daumen, rechter Aktionsknopf | Gleichzeitig laufen und handeln |

Die Steuerung verwendet Godots `InputMap`, `InputEventScreenTouch` und
`CharacterBody3D.move_and_slide`, keinen externen Joystick-Plugin-Code.
Referenz: [Godot-Eingabeereignisse](https://docs.godotengine.org/en/4.5/tutorials/inputs/inputevent.html).

## Pruefen

```bash
python prototypes/godot-hd2d/prepare_world.py --check
python prototypes/godot-hd2d/prepare_assets.py --check
godot --headless --editor --path prototypes/godot-hd2d --import
godot --headless --path prototypes/godot-hd2d --script res://tests/full_smoke.gd
godot --headless --path prototypes/godot-hd2d --script res://tests/full_framing.gd
godot --headless --path prototypes/godot-hd2d --script res://tests/full_doors.gd
godot --path prototypes/godot-hd2d --script res://tests/full_capture.gd
```

- Assetvergleich: 25 Originaldateien byte-identisch, 6x170 Frames pixel-identisch,
  Katalog synchron mit den nativen Quellen.
- `full_smoke.gd`: 76 Laufzeitchecks fuer Startmenue, Preload, alle Aussenboeden,
  Analogtempo, Run-/Sprung-/Roll-/Sitzrollen, zwei echte ScreenTouch-Finger,
  Brueckenbegrenzung, Tueren, Sitzanker und lokale Einstellungen.
- `full_framing.gd`: 66 Kombinationen aus elf Innenraeumen und sechs Wesen.
  Opake Pixel von Idle, Sprint, Rolle und Sitzen bleiben an beiden Seiten,
  in beiden Tiefen und bei drei Schwenkwinkeln im Bild.
- `full_doors.gd`: alle 132 Spezies/Tuerpfade; erhoehtes Raum-Padding muss auch
  fuer die groessten Wesen einen erreichbaren Ausgang behalten.
- Die urspruengliche Ausschnittszene und `tests/smoke.gd` bleiben fuer Regressionen.
- `full_capture.gd` erzeugt echte Godot-Renderings. Ausgewaehlte Bilder unter
  `preview/full-*.jpg` sind Desktop-Software-OpenGL, keine APK-Aufnahmen.

## Bekannte Grenzen und naechste Abnahme

Die gesamte vorhandene **Geografie und Figurenzeichnung** ist uebertragen.
Die Kulissen bleiben gerichtete Reliefbuehnen; ihre Rueckseiten und verdeckten
Objektseiten sind nicht gezeichnet. Konturverdeckung ist eine Naeherung,
Aussenbaenke nutzen die vorhandenen groben Oberflaechenanker. Manche Originalraeume
haben nur 480x270 Pixel und wirken bei grossem Zoom entsprechend weich.
Es gibt noch keine vollstaendige Tiefen-/Wassermaske fuer jedes Blatt.

Living-Agent-Persistenz, Begegnungs-/Besitzsystem, native Spielstaende,
ortsaufgeloestes Licht, Stoff-/Wasseranimation und die komplette Musikaufloesung
sind nicht portiert. Das aktuelle NPC-Gespraech zeigt einen vorhandenen Leitsatz;
es schreibt keine erfundenen Erinnerungen. Die neue Datei hat keine Verbindung
zu Room, Remindern, App 1, Stream oder bestehenden Preference-Vertraegen.

Naechster konkreter Schritt: die APK auf Martins Telefon pruefen (Bildgroesse,
Zweifingerbedienung, Temperatur/FPS und Speicher), danach einzelne Tiefenmasken
und gewuenschte Spielsysteme an diese Buehne anbinden.
Ruecksetzen durch Revert dieses isolierten Erweiterungscommits; der fruehere
Godot-Ausschnitt und alle nativen Android-Daten bleiben erhalten.
