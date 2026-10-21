# Dynamik, Ladeaufwand und vier Spielerfiguren

Stand:21.10.2026. PR376, Branch `codex/godot-focused-environment-budget` auf
PR375/ausgelieferter Testfassung0.13.1. Kein Merge ins ältere `main`.

## Ergebnis

Umfangreiche Spieler-, Tür-, Bewegungs-, Framing- und Kampfmatrizen lesen die
verbindliche Auswahl `Catalog.STARTERS`: Mooslyn, Nocti, Starlet und Wyrmling.
NPC-Dialoge/Tagespläne und Helferidentität bleiben gezielt geprüft; der vollständige
Acht-Spezies-Katalog bleibt erhalten. Deshalb bleiben kleine Daten-/Assetprüfungen
für Bewohner bestehen, ohne sie erneut durch sämtliche Spielerwege zu schicken.

Ebene Bildböden brauchen eine statt acht Tiefenreihen. Hintergrundreliefs behalten
zwölf statt32 Reihen; Laufwege und Kollisionskonturen bleiben vollständig.
Die Außenwelt hat75264 statt230784 Geometriepunkte (67,4% weniger). Die gemalten
Landschaftsbilder behalten ihre Auflösung; keine PNG-Datei wurde verändert.

Wasserrauschen wird nur auf Wasser berechnet; entfernte Lichtkreise werden vor
teuren Lichtfunktionen ausgeschlossen. Opake Bilder verwenden keinen zusätzlichen
Alpha-Vorlauf, transparente Anschlüsse behalten ihren bisherigen Renderpfad.
Wasser und weich begrenzte Wolken teilen vorberechnete RG-Masken. Damit entfallen
pixelweise Konturprüfungen bei Szenenwechseln; leere Masken sind nur1×1 Pixel groß.

Dezente Wolkenbewegung in ausgewählten Himmelsfenstern von Küste, Küstenpfad,
Parkpanorama und Hochland. Zusätzliche Wasserfenster am Meer und Sonnenhain;
Wege, Ufer, Holz und Felsen bleiben ausgespart. Mühlen-/Schattenbäche behalten
ihre Fließbewegung. Acht reine Kampfatlanten werden beim Start nicht dauerhaft
vorgehalten, bleiben aber bei Bedarf verfügbar.

Der reduzierte Testumfang hat eine alte Cache-Abhängigkeit bei Spezieshelfern
aufgedeckt: Deren Reisekontur setzte einen vorherigen Besuch dieser Spezies voraus.
Die vorhandenen Konturdaten werden jetzt bei Bedarf geladen. Eine gezielte Prüfung
mit zuvor geleertem Cache erhält die eigene Identität auch ungesehener Helfer.

## Messung und Bildqualität

Godot4.5.1,1280×720, OpenGL/Mesa llvmpipe. Drei getrennte Prozessstarts je Fassung,
abwechselnd alte/neue Fassung; Ausgangscommitf4dda4a. Keine Telefon-FPS-Messung.

| Messgröße | Vorher | Nachher | Veränderung |
|---|---:|---:|---:|
| Start bis spielbare Welt, Median |2411ms|1934ms|−19,8%|
| Bildvorbereitung, Median |1645ms|1507ms|−8,4%|
| Erster Weltaufbau, Median |803ms|459ms|−42,9%|
| Texturspeicher vor Weltaufbau |529225364B|462122520B|−12,7%|
| Vorbereitete Texturen |74|66|−8|
| Außenwelt-Geometriepunkte |230784|75264|−67,4%|

Renderzeit in sechs Ansichten im Median aus3×8 Frames: etwa20–32% geringer.
Schattenwald-Aufbau1848→711ms, Mühlenbach72→35ms (je ein Szenenwechsel-Messlauf).
Die Werte sind host- und cacheabhängig; sie sind keine Geschwindigkeitsgarantie
für Android. Erst nach Wiederherstellung unter gleichen Bedingungen neu gemessen;
frühere Einzelmessungen aus der zurückgesetzten Umgebung werden nicht verglichen.

Sechs Landschaften vor/nachher bei festem Animationszeitpunkt gerendert und visuell
geprüft. Der mittlere absolute Unterschied bleibt unter0,12 von255 Farbwerten;
Wald/Hochland sind praktisch pixelgleich. Änderungen betreffen vor allem Wasser.
24 gezielte OpenGL-Prüfungen bestätigen Bewegung nur in Dynamikfenstern und stabile
trockene Flächen. Rohdaten: `docs/validation/godot-environment-budget-profile.json`.
Reproduzierbar mit `tests/environment_profile.gd`/`environment_render.gd`.

## Prüfstand und Lieferung

Bestanden:128 Türpfade,109 Streckenchecks,64 Raum-/Spielerfälle,11023 Waldchecks,
84 Vollwelt-,869 Figuren-/Helfer-,195 Ausdauer-,690 Bewohner-,155 Tiefen-,21 Hang-
und262 Ambience-Prüfungen. Außerdem24 Dynamik-,9 Ambience-,7 Wald-,9 Feuer-/Licht-
und2 Tiefen-OpenGL-Prüfungen. Der Masken-Rebuild-/Hashvergleich ist bestanden.
Bekannte Meldungen über beim Testprozessende noch gehaltene Katalogressourcen
bestehen weiter; keine funktionalen Fehler in diesen Prüfungen.

Test-APK0.14.0/Code27 ist ausgeliefert: Baulauf39603771878, Job119234617541,
Quellcommit955be67999ee2b2a4ba55cde19427fb6d971621c. Sämtliche Godot-Spieltests,
OpenGL-Aufnahmen und1088 Bewegungskonturchecks bestanden. Paket, Version,616 Karten,
vier Startwesen, Exportinventar und bestehendes Signierzertifikat sind geprüft.
Die vollständig hochgeladene Drive-Datei stimmt in Größe und Prüfsummen überein.
191734829 Bytes; SHA-256 `edfe94928fe079cda65bf14f44f58dc6f35910726eb35e62249e92943079f0dc`.
Liefernachweis: `docs/validation/godot-v0140-build-report.json`.

[APK0.14.0 herunterladen](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk).
Paket und Zertifikat entsprechen0.13.1; als Update installierbar.
APK-Artefakt12347403237, Prüfprotokolle12347752677 (14 Tage aufbewahrt).
Telefon-Touchgefühl, GPU/FPS und menschliche Abnahme der Dynamik bleiben offen.

Masken nach Konturänderungen neu erzeugen: `python3 tools/bake_environment_masks.py`
im Godot-Projekt. `tests/test_environment_masks.py` vergleicht Konturen, komprimierte
Daten und Prüfsummen. Die Produktions-App rastert diese Konturen nicht neu.
