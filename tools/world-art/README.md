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
| `roomkit.py` | Baukasten fuer drinnen nach der Wohnraum-Studie: Fachwerk, Vertaefelung, Fenster, Tuer, Teppich, Sofa, Bett, Regale, Kuechenzeile, Lampen |
| `places/<ort>.py` | ein Skript je Ort (`build(out)` malt das Bild und gibt die Spielangaben zurueck) |
| `overview.png` | Uebersicht aller Orte (5 x 5) |
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
| Wohnzimmer, Schlafzimmer, Kueche, Bad, Schreibzimmer, Werkstatt, Leseecke | Wohnraum-Studie | je nach Zimmer; Tuer ins Wohnzimmer, von dort auf die Strasse |
| Strasse, Marktplatz (Abend), Laden, Cafe, Arbeitsstube, Spielhalle | Uferviertel | Laterne, Bank, Regal, Kasse, Tische, Automaten |
| Park, Sportplatz | Park mit Sportplatz | Bank |
| Waldsee | Waldsee | - |
| Wiese, Wald, Berge, Ebene, Lager, Strand, Dschungel, Sumpf, Grotte | frei im selben Stil | Sitzplatz (Stamm oder Steinbank) |

Benoetigt Python 3 mit `numpy`, `scipy`, `Pillow`. Der Wald braucht knapp eine Minute (viele
Tannen), alle anderen Orte wenige Sekunden.
