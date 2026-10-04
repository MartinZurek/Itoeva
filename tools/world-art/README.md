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
| `overview.png`, `rooms_preview.png` | Uebersicht der gemalten Orte; Wohn- und Schlafzimmer in doppelter Groesse |
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
| Strasse, Marktplatz (Abend) | Uferviertel | Laterne, Bank |
| Park, Sportplatz | Park mit Sportplatz | Bank |
| Waldsee | Waldsee | - |
| Wiese, Wald, Berge, Ebene, Lager, Strand, Dschungel, Sumpf, Grotte | frei im selben Stil | Sitzplatz (Stamm oder Steinbank) |

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
