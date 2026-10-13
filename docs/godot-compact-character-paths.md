# Godot v0.2.2: kompakte Wesen und erkennbare Laufwege

Stand: 2026-10-11. Folgekorrektur zur Gesamtwelt und zum Bildabgleich aus v0.2.1.
Martins Auftrag: kleinere, stimmigere Figuren im vorhandenen gemalten Stil,
enger begrenzte Wege und eine sichtbare Orientierung gegen falsche Laufwege.

## Darstellung und Bewegung

| Bereich | Aenderung gegenueber v0.2.1 |
|---|---|
| Aussenfiguren | Grundmassstab 0.0115 statt 0.020: 42.5 Prozent kleiner bei gleicher Kamera. |
| Innenfiguren | Korrigierter Raum-Grundmassstab mal 0.60: 40 Prozent kleiner. |
| Hootlet | Relative Hoehe 0.62 statt 0.70; keine Breitenverzerrung. |
| Andere Wesen | Gloop 0.76, Puffling 0.64, Wyrmling 0.94, Starlet 0.74 relativ zu Fennec. |
| Parkbewohner | Derselbe Buehnenmassstab wie die Spielerfigur; vorher verblieb ihr Standardwert 0.020. |
| Farbe/Kanten | Lineare Menuefilterung, sieben Prozent Entsaettigung im Sprite-Shader, leichte warme Raumtoenung. |
| Rennen | Streckenbasierter Laufzyklus folgt dem kleineren Grundmassstab. |
| Aussenzoom | Bereich 7.2 bis 8.8, Standard weiterhin 7.6. |

Die sechs Originalatlanten mit je 170 Zeichnungen bleiben pixel-identisch.
Es wird kein neues Figurenmodell und kein 3D-Rig eingefuehrt. Der sichtbare
Fortschritt entsteht durch Massstab, Artverhaeltnisse, Filterung und Farbanschluss.
Eine neue Zeichnung oder Krita-Bearbeitung wird nicht behauptet.

Der Vergleich mit Octopath Traveler dient als Gestaltungsreferenz fuer Figuren-
und Kulissenverhaeltnis. Das offizielle Interview mit Acquire/Square Enix beschreibt
bei Teil II kleinere Kopf-/Koerperverhaeltnisse und die Abstimmung von 2D-Sprites
mit 3D-Szenen. Itoeva behaelt seine eigene Zeichnung und seine eigene Umsetzung:
[Offizielles Entwicklerinterview](https://www.unrealengine.com/developer-interviews/octopath-traveler-ii-builds-a-bigger-bolder-world-in-its-stunning-hd-2d-style?lang=en-US).

## Wege

- Alle bestehenden Panorama- und Anschlussprofile erhalten innen vier native Pixel
  Sicherheitssaum, Raumprofile drei Pixel. Sehr schmale Wege begrenzen den Saum
  auf 18 Prozent ihrer Breite, damit Tueren und die Bachbruecke passierbar bleiben.
- Der gesamte Bewegungsschritt wird nach Godots `move_and_slide` gegen dieselbe
  Bodenprojektion geprueft. Springen und Rollen koennen Wegraender nicht ueberqueren;
  blockierte Diagonalen gleiten entlang einer gueltigen Achse. Echte hochgelegene
  Moebelflaechen bleiben bei ausreichender Fusshoehe nutzbar.
- Am Brueckenausgang und vor der Kuestenbank wurde der Laufstreifen nochmals auf
  den gemalten Weg verschoben: die vorher erreichbaren Blumen und Steinwand sind
  ausgeschlossen. Vier feste Bildpunkte pruefen dies unabhaengig vom Wegprofil.
- Goldene Wegkanten folgen derselben Kollisionskontur. Moebel unterbrechen die
  Markierung. `Einstellungen > Wege zeigen` schaltet sie um; der lokale Speicher
  merkt sich die Wahl. Bestehende Speicher ohne den Schluessel zeigen Wege an.

## Pruefung

- 25 Originaldateien byte-identisch; 6 mal 170 Frames pixel-identisch.
- 2048 Geometriechecks, einschliesslich wirklicher Bewegung entlang 411 Wegpunkten
  in beiden Richtungen.
- 293 zusaetzliche Grenz-/Massstabchecks: Gehen, Rollen und Springen gegen Aussenraender;
  diagonale Aktionen gegen alle elf Raumgrenzen; vier Kuesten-Bildpunkte;
  NPC-Massstab und gespeicherte Weganzeige.
- 76 Laufzeitchecks fuer Menue, Touch, Analogtempo und Aktionen.
- 132 Spezies/Tuerpfade und 66 Raum/Spezies-Bildrahmen.
- 94 echte Godot-Renderpaare mit/ohne Figur: alle Raum/Spezies-Kombinationen,
  alle Orte, sechs Anschluesse und fuenf Bruecken-/Ausgangspositionen.

Alle genannten Tests laufen in Godot 4.5.1. Beispielbilder unter
`prototypes/godot-hd2d/preview/compact-*.jpg` sind Desktop-Software-OpenGL;
es sind keine Aufnahmen einer Android-Installation.

## APK und Grenzen

Version 0.2.2 / Code 5, Paket `com.itoeva.godot.hd2d.world`, derselbe Debug-Schluessel
wie v0.2.0/v0.2.1. Keine neue native Datenmigration und keine Aenderung der
Signierungsinfrastruktur. Der neue Einstellungswert ist optional und rueckwaertskompatibel.
APK-Signaturen v2/v3, 16-KiB-ZIP-Ausrichtung, ZIP-Inhalt, ARM64, API 24/35,
Querformat und fehlende Internetberechtigung sind am finalen Artefakt geprueft.
78.132.306 Bytes; SHA-256:
`fef14a467d40b246e5810065cc9abe4c591a7230001993f70ec0d1e3f77c923d`.

Telefon-GPU, Temperatur, FPS und echte Android-Zweifingerbedienung bleiben offen.
Die Kulissen sind gerichtete Reliefbuehnen. Kleine Innenraum-Originalbilder bleiben
weich; Polygonmasken liefern keine vollstaendige Tiefenkarte jedes Blattes.
Ruecknahme des v0.2.2-Commits stellt v0.2.1 wieder her und erhaelt Wesen-/Raum-IDs.
