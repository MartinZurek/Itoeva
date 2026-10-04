# world-art – die gemalte Welt von Itoeva 2

Pixel-Art-Kulissen fuer die Spiel-Variante (`:app-sim`, Build-Typ `game`), als Code gemalt:
480 x 270 Bildpixel, echte Perspektive, Licht und Dunst, am Ende auf eine Palette reduziert.
Vorbild sind die Weltstudien in `docs/concept-art/world-studies/` (Wohnraum, Park mit Sportplatz,
Waldsee, Uferviertel).

## Aufbau

| Datei | Inhalt |
|---|---|
| `px.py` | Werkzeugkasten: Leinwand, Masken, Rauschen, gerasterte Verlaeufe und Lichthoefe, Palette |
| `kit.py` | Baukasten fuer draussen: Himmel, Huegel, Baeume, Wiese, Pflaster, Wasser mit Spiegelung, Fassaden, Bank, Laterne, Felsen, Zelt, Palme, Kristall, Abendstimmung (`dusk`) |
| `room3d.py` | Zimmer in Zentralperspektive: Ebenen per Pixel zurueckgerechnet, Moebel als Quader, Licht |
| `pixelate.py` | uebersetzt einen Studienausschnitt in Pixel-Art: Flaechen beruhigen, Bleistiftkonturen als Pixellinien, eigene Palette, Ausreisser weg |
| `concept.py` | liest die Studien aus dem Concept-Art-Branch, nimmt Figuren aus dem Bild und setzt Gegenstaende aus den Objektspalten ein |
| `nature.py`, `town.py` | Einzelstuecke fuer draussen: Wolken, Baeume, Graeser, Blumen, Steine; Haeuser, Fenster, Markisen, Laternen, Toepfe |
| `places/<ort>.py` | ein Skript je Ort (`build(out)` malt das Bild und gibt die Spielangaben zurueck) |
| `overview.png`, `rooms_preview.png` | Uebersicht der gemalten Orte; die sechs Orte aus den Studien |
| `build_all.py` | rendert die Orte nach `app-sim/src/game/assets/scenes/` und schreibt `GameSceneCatalog.kt` |
| `park.py`, `reading_room.py`, `hero.py` | erste Studien vom 04.10. (Park am Meer, Lesezimmer = Ort `NOOK`, Fuchs) |

## Bauen

```
python3 tools/world-art/build_all.py            # alle Orte
python3 tools/world-art/build_all.py city beach  # nur diese
```

Die Spielangaben (Gehflaeche, Figurgroesse, antippbare Plaetze, gesperrte Raender) stehen im
Skript direkt neben dem, was sie beschreiben, und landen ueber `places/meta.json` im generierten
`app-sim/.../matrix/GameSceneCatalog.kt` - Bild und Spiel koennen nicht auseinanderlaufen.
Ein Ort darf nur Plaetze haben, die er im Spiel schon hat (`PlayScene.stationsAt`); das prueft
`GameScenesTest`.

## Die Orte

| Ort | Vorlage | Plaetze |
|---|---|---|
| Wohnzimmer, Schlafzimmer | Wohnraum-Studie, eins zu eins uebersetzt (Seitenansicht) | Sofa, Klavier (Platz TV), Bett, Tuer |
| Strasse | Uferviertel, eins zu eins uebersetzt (Cafe, Buchladen, Promenade) | Laterne, Bank |
| Marktplatz (Abend) | frei nach dem Uferviertel | Laterne, Bank |
| Park, Sportplatz | Park mit Sportplatz, eins zu eins uebersetzt | Bank |
| Waldsee | Waldsee, eins zu eins uebersetzt (Steg, See, Insel) | - |
| Wiese, Wald, Berge, Ebene, Lager, Strand, Dschungel, Sumpf, Grotte | Konzeptbuch Band 2 (`world-conceptbook-v2/images/*-pixel-concept-v1.jpg`), eins zu eins uebernommen ueber `places/_landscape.py` | Sitzplatz (Stamm, Bank, Heuballen, Liegestuhl, Steinplatte) |

Benoetigt Python 3 mit `numpy`, `scipy`, `Pillow`. Der Wald braucht knapp eine Minute (viele
Tannen), alle anderen Orte wenige Sekunden.

## Innenraeume aus den Studien (seit 04.10. abends)

Die frueheren Zimmer in Zentralperspektive sind verworfen - zu konstruiert. Innenraeume entstehen
jetzt direkt aus der Wohnraum-Studie in deren Seitenansicht: Ausschnitt laden, Figuren mit einer
Hand gezeichneten Maske herausnehmen, Luecke fuellen, mit Gegenstaenden aus der Objektspalte
desselben Blatts ergaenzen, dann `pixelate.translate`. Dazu muss der Branch
`art/concept-studies-2026-10-04` geholt sein (`git fetch origin art/concept-studies-2026-10-04`).
Kueche, Bad, Schreibzimmer, Werkstatt, Leseecke, Laden, Cafe, Arbeitsstube und Spielhalle haben
noch keine Studie und zeigen bis dahin die alte Kulisse.

## Laufzeit-Licht und Bewegung (Folge-PR)

`GameSceneLighting` rechnet die im Bildkoordinatensystem liegenden Lichtquellen aus Uhrzeit,
Lampenschalter, Fernseher und Lagerfeuer. `GameSceneView` legt fallende Lichtfelder und einen
mit dem Avatar bewegten Bodenschatten ueber die gemalte Szene; wenige Blaetter und
Wasserreflexe bewegen sich im vorhandenen Szenentakt. Die Kamera verwendet nur tatsaechlich
ueberstehende Bildteile und denselben `GameScenes.fit` fuer Bild, Avatar und Trefferflaechen.
Das ist ein 2D-Lichtmodell: die in PNGs eingebrannten Highlights und Schatten von
Kulissenobjekten bleiben vorerst statisch. Der Exportclip zeichnet diese neue Schicht noch
nicht mit. Geraeteansicht und Performance sind noch zu pruefen.

## Landschaften aus Konzeptbuch Band 2

Die neun Wildnis-Orte kommen seit dem 04.10. abends aus den Landschaftstafeln von Band 2. Die
Tafeln sind schon Pixel-Art; `_landscape.render` schneidet die Szene im Spielformat (16:9) aus,
nimmt die Figuren heraus (Umgebung versetzt hineinkopiert) und bringt das Bild mit
`pixelate.translate` sanft auf die Spielpalette. Je Ort steht im Skript der Ausschnitt `BOX` in
Tafelkoordinaten (1024 x 683). Das Lagerfeuer-Licht in `GameSceneLighting` (Folge-PR #330 von
Codex) liegt auf dem Feuer dieses Bildes (`CAMP_FIRE_X/Y`).

## Bewegung und Nachtlicht (`animate.py`, seit 04.10. abends)

Jeder Ort bekommt neben dem Bild zwei Ebenen, die `build_all.py` (oder `animate_all.py` fuer die
vorhandenen Bilder) erzeugt:
- `<ort>_anim.png`: acht Bilder nebeneinander. Laub und Gras wiegen sich um einen Pixel (eine Boe
  laeuft durchs Bild), Wasser kraeuselt sich zeilenweise und glitzert, Lampen und Feuer flackern.
  Das Spiel zeigt sie im Szenentakt (200 ms je Bild).
- `<ort>_glow.png`: was nachts leuchtet (Lampen, Fenster, Feuer, Kerzen) mit gestuftem Lichthof.
  Das Spiel legt sie ueber die naechtliche Abdunkelung, damit Lampen hell bleiben.
Je Ort lassen sich in `build_all.ANIM` Wasserzeilen, Feuerstellen, zusaetzliche und
ausgeschlossene Leuchtflaechen und die Windstaerke setzen.
