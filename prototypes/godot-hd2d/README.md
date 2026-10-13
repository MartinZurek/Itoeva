# Aktueller Stand v0.5.0

Acht Wesen, eigene Spezialkräfte, 39 Kartendefinitionen und animierte Arenaminions.
Minion antippen: Werte sehen und bei Bereitschaft Angreifer wählen. Gegnerischen
Minion oder Nachtwurz antippen: angreifen; Schutzhelfer zuerst besiegen.
„Karten“ öffnet im Kampf die vollständigen Beschreibungen der Handkarten.
Im Menü „Deck bauen“ lassen sich Karten ansehen und Decks bearbeiten.
Details: [Spezialkräfte und Minions](../../docs/godot-spezialkraefte-und-minions-v050.md).
AndroidTest: 0.5.0 / Code 15; bestehende Paketkennung und Testsignatur.
Merge-Abgleich, Datei-Hashes und Drive-Anleitung: [Merge v0.5.0](../../docs/godot-merge-v050.md).

```bash
godot --headless --path prototypes/godot-hd2d --export-debug AndroidTest /absoluter/pfad/Itoeva_Godot_Test_v0.5.0.apk
```

## Vorheriger Stand v0.4.0

Kartenkampf mit Deckeditor, eigenen Kristallen und bleibenden Helfern.
Karte antippen und gegebenenfalls Ziel wählen; Helfer antippen und Ziel wählen;
Superkraft kostenlos einmal im eigenen Zug; „Zug beenden“ gibt weiter.
Im Hauptmenü „Deck bauen“: 8–12 Karten pro Wesen, höchstens zwei je Karte.
Details und Prüfgrenzen: [Kartenkampf v0.4.0](../../docs/godot-kartenkampf-v040.md).
AndroidTest: 0.4.0 / Code 14, bestehendes Testpaket und Testzertifikat.

## Vorheriger Stand v0.3.0

Wildtierruf, Lichtstrahl, Schutzbarriere, Splitter und Wurzelfesseln.
Die dokumentierte Umsetzung steht in ../../docs/godot-kampfeffekte-v030.md.
AndroidTest aktualisiert die vorhandene Test-App mit derselben Signatur.

## Vorheriger Stand v0.2.9

Siegesbildschirm, gespeicherte XP/Beute und sicherer Weltabschluss.
Details: [Siegesabschluss v0.2.9](../../docs/godot-siegesbildschirm-v029.md).

## Vorheriger Stand: v0.2.8 – Charakterfähigkeiten und eigener Rundenkampf

Sechs Charaktere besitzen je zwei individuelle Fähigkeiten, eine aufladbare
Spezialaktion und 16 eigene Kampfposen. Nachtwurz ist der neue gezeichnete
Waldgegner. Separate Kampfbühne mit Zugfolge, LP/MP/Herz, Gegenständen, Schutz,
Schwächen und tatsächlichen Statuswirkungen. Menüpausen und Neustarts erhalten
auch gerade ausgeführte Aktionen, ohne Schäden oder Kosten zu verdoppeln.
Feuer nutzt gemalte Flammenphasen, Wasser gerichtete Strömung und Reflexe.
AndroidTest v0.2.8 / Code 11 verwendet das bestehende Testzertifikat.
Auf der bereits gewonnenen Lichtung ist „Kampf wiederholen“ verfügbar.
[Details und Fähigkeiten](../../docs/godot-rundenkampf-v028.md).

## Vorheriger Stand: v0.2.7 – Waldpfad und erster Schattenkampf

Vom bestehenden Wald fuehrt der Eingang „Waldpfad zum Schattenwald“ in sieben
verbundene neue Abschnitte. Folgen: Waldsaum, gruener Wald, letzte Fackeln,
Schattenwald mit Ruhebank, Schattenbach mit Bruecke, Kampflichtung, tiefer Wald.
Auf der Lichtung erloeschen die Fackeln, Restrauch steigt auf und ein einzelnes
Schattenwesen erscheint. Schlag, Lichtstoss und Schuetzen bilden den ersten
rundenbasierten Kampf. Sieg, Niederlage, Wiederholung und Rueckzug sind spielbar.
Menuepausen und Szenenneustarts erhalten den Zustand je Wesen.

Flammen lodern lokal, der Bach bewegt sich ausserhalb des festen Brueckendecks.
Im tiefen Wald gibt es keine installierten Leuchten; „Traglicht: an/aus“ steuert
die mitgefuehrte Laterne. Die Waldruhebank hat sechs angepasste Sitzhoehen.
Die Originalwelt und ihre bisherigen Innenraeume bleiben erreichbar.
AndroidTest v0.2.7 / Code 10 verwendet denselben Testschluessel wie v0.2.6.
[Umsetzung, Bedienung und Pruefung](../../docs/godot-shadow-forest-v027.md).

## Vorheriger Stand: v0.2.6 – Entdecken und Begegnen

Ein Tipp auf Wesen, Sprechblase, Sitzplatz oder Eingang fuehrt ueber einen freien
Weg zum Ziel und startet die benannte Aktion. Der linke Stick uebernimmt sofort.
Gespraeche zeigen Originalportraet, Leitsatz und eine kurze Entdeckungsantwort.
Parkbewohner spazieren mit seitlichem Ausweichen; Sitzen zeigt die ganze Figur
auf dem Originalmoebel. Der einzelne rechte Gestenkreis bleibt erhalten.

Weltzeit beginnt mit Berliner Ortszeit (MEZ/MESZ). Tueren erhalten den Spielzeit-
versatz; Lesen verlaengert ihn um zwei Stunden, Schlafen bis zum naechsten 08:00.
Wasser und 64 gemalte Leuchten/Feuer erhalten begrenzte Shaderanimationen.
Nur der Kuestenvordergrund verwendet eine neue Uferfassung unterhalb UV .75;
alle Originalbilder und Figuren bleiben erhalten.
[Umsetzung und Pruefung](../../docs/godot-interactions-and-time.md).
AndroidTest v0.2.6 / Code 9 verwendet weiterhin denselben Testschluessel.

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
godot --headless --path prototypes/godot-hd2d --export-debug AndroidTest /absoluter/pfad/Itoeva_Godot_HD2D_v0.2.6_Test.apk
```

Android-Export: Godot-4.5.1-Vorlage, Java 17, SDK Build-Tools 35.0.1 und eigener
Debug-Keystore in den Editor-Einstellungen. Schluessel und SDK-Pfade liegen ausserhalb
des Repositorys. Mit Martins ausdruecklicher Freigabe vom 2026-10-11 existiert
jetzt das zusaetzliche Preset **AndroidTest**, Paket
`com.itoeva.godot.hd2d.worldtest`, Anzeigename **Itoeva Godot Test**, Version
0.2.6 / Code 9. Die APK ist signiert, ARM64 ab Android 7, Querformat,
ohne Internetberechtigung. Sie wird separat installiert und startet mit eigenen
lokalen Daten. Die bestehende Godot-Welt-App und ihre Spielstaende bleiben erhalten.
Der neue Testschluessel ist separat gesichert; Alias `androiddebugkey`,
Standard-Debug-Passwort `android`. Er gehoert nicht in Git oder das Quellprojekt.
Tests, Python-Dateien, Vorschauen und Captures werden nicht exportiert.

Das urspruengliche Preset **Android** bleibt mit
`com.itoeva.godot.hd2d.world` erhalten. Sein bisheriger Debug-Keystore fehlt
im wiederhergestellten Stand; fuer kompatible Updates dieses alten Pakets ist
weiterhin der alte Schluessel erforderlich. Der neue Schluessel wird ausschliesslich
fuer das separat installierbare Testpaket verwendet.

Die Build-Pruefung und der Bildabgleich sind in
[docs/godot-proportions-and-shoreline.md](../../docs/godot-proportions-and-shoreline.md) dokumentiert.
Der vorherige Bild-/Konturabgleich steht in
[docs/godot-visual-path-audit.md](../../docs/godot-visual-path-audit.md).
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
`data/world_catalog.json`. `data/visual_geometry.json` ergaenzt direkt an den
Originalbildern abgestimmte Wege, Raumgrenzen, sechs sichtbare Aussensitzplaetze
und die Brueckenbruestung. Die Parkkiste verwendet dieselben Pixelrechtecke wie
`GameSurfaceView`. Bereits gemalte Moebel erhalten nur Kollisionskoerper,
Sitzanker und eine Tiefenmaske; ihre Bildflaechen werden nicht erneut gezeichnet.

Der Laufstreifen ist ein horizontaler 3D-Boden. Jedes Panorama und Anschlussbild
besitzt ein eigenes Wegprofil; die Bruecke fuehrt hinter ihrer Steinbruestung.
Wegraender begrenzen das Betreten von Wasser und nicht begehbarer Vegetation.
Ein Sicherheitssaum und eine Pruefung des gesamten Bewegungsschritts halten
auch Sprung und Rolle auf begehbaren Flaechen. Beide Holzstege und die Steinbruecke folgen
jetzt den nachgemessenen Planken-/Deckkoordinaten, ihre Bildflaechen liegen auf
demselben Boden wie die Physik. Einzelne Steinraender und ein niedriges
Steggelaender an beiden Holzstegen verdeutlichen unklare Stellen; farbige Weglinien sind entfernt.
Hintergrundrelief und Boden-UV erhalten die Bildkomposition. Die begrenzte
orthografische Kamera folgt der Figur und laesst Zoom zu. Innen und aussen
bleibt die Blickachse passend zur gezeichneten Perspektive fest.

## Wesen, Menues und Bewegung

- **Fennec, Gloop, Puffling, Wyrmling, Starlet, Hootlet**: je 170 unveraenderte
  Zeichnungen aus Rich und Living, nur in mobile 1536x1920-Atlanten umgepackt.
  Kameraorientierte, fussverankerte `Sprite3D`-Karten ohne Verzerrung, kein 3D-Rig.
  Sichtbare Referenzhoehen bestimmen die Groessen. Der Aussen-Grundmassstab ist
  0.0145 fuer die Zeichnung, 0.0115 fuer den Physikfuss; innen folgt der Massstab der Kamera,
  damit die sichtbare Referenzgroesse genau der Aussenwelt entspricht. Hootlets relative Hoehe betraegt 0.62, die anderen
  kleinen Wesen sind ebenfalls kompakter. Spieler und Parkbewohner verwenden
  dieselbe Skalierung. Lineare Filterung erhaelt weiche Kanten.
  Innen, aussen und bei Abendlicht verwenden die Wesen dieselben Originalfarben;
  zusaetzliche Raumtoenung und Entsaettigung sind entfernt.
- Anfangsmenue mit originalem Wohnzimmerbild, sechs stabilen Auswahlkarten,
  originalen Leitsaetzen und fortsetzbarer Wesenwahl.
- Ein einzelner runder rechter Gestenkreis: oben wischen springt, seitlich wischen
  rollt bzw. dreht Hootlet im Flug. Tippen fuehrt die benannte Aktion aus, sonst
  springt die Figur. Unten wischen oder 500 ms halten setzt hin und stoppt den bisherigen
  Gehdaumen. Der Kreis hat jetzt Radius 86 statt 62 Pixel, sein Sitzsymbol ist
  deutlich groesser. Kein Raster aus
  Aktionsbuttons. Sprechblasen, Tuerpfeile und Sitzsymbole sind direkt antippbar;
  im erreichbaren Bereich zeigen sie ihre Aktion. Kleine Pfeile an den Bildraendern
  weisen auf weiterfuehrende Wege hin. Hinweise und Interaktion teilen ihre
  Abstandspruefung. Entfernte Tipps gehen erst auf dem geprueften Pfad zum Ziel.
- Floating- oder fester Analogstick, einstellbare Empfindlichkeit, unabhaengige
  zwei Finger. Deadzone, Beschleunigung/Bremsung und stetiger Uebergang vom
  Gehen zum Rennen ersetzen einen abrupten Tempo-Schalter.
  Die Laufzyklus-Strecke ist an die kleinere Figur angepasst.
- Laufphasen folgen der wirklich zurueckgelegten Strecke. Richtungsgetreue
  Walk-/Run-Frames, Sprungphasen,
  kurze Sprungeingabe-Pufferung und Landereaktion bleiben im vorhandenen Design.
  Ruhe verwendet eine stabile Zeichnung je Blickrichtung (142/148/149); die
  falschen Ansichtswechsel in den bisherigen Wendebildern sind entfernt.
  Sitzen nutzt die originalen Posen 93/103/110 statt des Schlafbilds.
  Wyrmlings Atlasmassstab bleibt beim Wechsel von Ruhe zu Rennen gleich.
  Kontakt-Schatten treffen nur statische Boeden, nicht andere Figuren.
  Figuren werden nach Bodentiefe geordnet, unabhaengig von ihrer Sprunghoehe.
- Einstellungen fuer Stick, Empfindlichkeit, Zoom, Ortszeit/Abendvorschau und die vorhandene
  Theme-Musik. Musik ist beim ersten Start aus; Menue/App-Pause unterbrechen sie.
- Eigene lokale Datei `user://painted_world_v2.json` speichert Einstellungen und
  Standort je Wesen atomar. Keine Migration oder Verbindung zu nativen Spielstaenden.
- Fuenf andere Wesen spazieren als ansprechbare Parkbewohner mit Wendepausen.
  Nur Gespraech und Annaeherung pausieren den betroffenen Nachbarn; ein Abbruch
  stellt seinen vorigen Pausenzustand wieder her. Keine Living-Agent-Migration.

| Eingabe | Wirkung |
|---|---|
| WASD / Pfeile / linker Gamepadstick | Gehen |
| Shift / aeusserer Analogring | Rennen |
| Leertaste / Gamepad A | Springen |
| R / Gamepad B | Rolle |
| C / Gamepad Y | Sitzen / aufstehen |
| E / Gamepad X | Interagieren / Tuer betreten |
| Q / F oder rechter Gamepadstick | Blickachse bleibt zur Kulisse fest |
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
godot --headless --editor --path prototypes/godot-hd2d --import --quit
godot --headless --path prototypes/godot-hd2d --script res://tests/full_smoke.gd
godot --headless --path prototypes/godot-hd2d --script res://tests/full_framing.gd
godot --headless --path prototypes/godot-hd2d --script res://tests/full_doors.gd
godot --headless --fixed-fps 60 --path prototypes/godot-hd2d --script res://tests/visual_geometry.gd
godot --headless --fixed-fps 60 --path prototypes/godot-hd2d --script res://tests/compact_boundaries.gd
godot --headless --fixed-fps 60 --path prototypes/godot-hd2d --script res://tests/gestures_paths.gd
godot --headless --fixed-fps 60 --path prototypes/godot-hd2d --script res://tests/proportions_and_readability.gd
godot --headless --fixed-fps 60 --path prototypes/godot-hd2d --script res://tests/shoreline_alignment.gd
godot --path prototypes/godot-hd2d --script res://tests/readability_capture.gd
godot --path prototypes/godot-hd2d --script res://tests/visual_capture.gd
godot --path prototypes/godot-hd2d --script res://tests/natural_capture.gd
godot --path prototypes/godot-hd2d --script res://tests/full_capture.gd
```

- Assetvergleich: 34 bisherige PNGs byte-identisch, 6x170 Frames pixel-identisch,
  Katalog synchron mit den nativen Quellen.
- `full_smoke.gd`: 76 Laufzeitchecks fuer Startmenue, Preload, alle Aussenboeden,
  Analogtempo, Run-/Sprung-/Roll-/Sitzrollen, zwei echte ScreenTouch-Finger,
  Brueckenbegrenzung, Tueren, Sitzanker und lokale Einstellungen.
- `full_framing.gd`: 66 Kombinationen aus elf Innenraeumen und sechs Wesen.
  Opake Pixel von Idle, Sprint, Rolle und Sitzen bleiben an beiden Seiten,
  in beiden Tiefen im Bild; drei Schwenkeingaben behalten die feste Innenblickachse.
- `full_doors.gd`: alle 132 Spezies/Tuerpfade; erhoehtes Raum-Padding muss auch
  fuer die groessten Wesen einen erreichbaren Ausgang behalten.
- `visual_geometry.gd`: 2048 Checks, darunter wirkliche Bewegung entlang 411
  Wegpunkten in jeder Richtung, verbotene Randflaechen und 1848 freie Raumspawns.
- `compact_boundaries.gd`: 291 Checks fuer gemeinsamen NPC-Massstab, kompakte
  Raumfiguren, Geh-/Roll-/Sprunggrenzen draussen und diagonal in allen elf Raeumen.
- `gestures_paths.gd`: 117 Checks fuer echten Zweifinger-Touch, gerichtete Rollen,
  Tippen, Halten, Abbruch, Menuewechsel, unabhaengige Wasser-/Deckkoordinaten,
  einheitliche Figurenfarben/-groesse und antippbare Sprechblasen/Tuerpfeile.
- `visual_capture.gd`: 100 Bildpaare mit/ohne Figur, inklusive sechs Steinbruecken-,
  vier Holzstegpositionen und Gespraechsnachbarschaft. Kein Avatar ist vollstaendig
  ueberdeckt; ausgewaehlte Ansichten werden visuell geprueft.
- `natural_capture.gd`: sechs ruhige Abnahmebilder mit physischem Bodenkontakt und
  aktueller Bedienung; `preview/natural-*.jpg` zeigt echte Desktop-Renderings.
- `proportions_and_readability.gd`: 670 Checks fuer feste Ruhe-/Sitzansichten,
  konstanten Massstab, Bodentiefe, Gespräche vom freien Weg und den groesseren Kreis.
- `shoreline_alignment.gd`: 59 unabhängige Planken-/Wasser-/Anschlusskoordinaten
  und echte Geh-/Roll-/Sprungversuche an beiden Holzstegen.
- `readability_capture.gd`: 17 echte Renderbilder von Menue, Raumgroessen, Sitzen,
  beiden Stegen, Uferanschluss, Gespraech und acht Bewegungsphasen.
- Die urspruengliche Ausschnittszene und `tests/smoke.gd` bleiben fuer Regressionen.
- `full_capture.gd` erzeugt echte Godot-Renderings. Ausgewaehlte Bilder unter
  `preview/full-*.jpg` sind Desktop-Software-OpenGL, keine APK-Aufnahmen.

## Bekannte Grenzen und naechste Abnahme

Die gesamte vorhandene **Geografie und Figurenzeichnung** ist uebertragen.
Die Kulissen bleiben gerichtete Reliefbuehnen; ihre Rueckseiten und verdeckten
Objektseiten sind nicht gezeichnet. Konturverdeckung bleibt eine Naeherung;
die sichtbaren Aussensitzplaetze sind an den Originalkulissen abgestimmt. Manche Originalraeume
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
