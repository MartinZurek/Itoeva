# world-art – die gemalte Welt von Itoeva 2

Pixel-Art-Kulissen fuer die Spiel-Variante (`:app-sim`, Build-Typ `game`), von Hand als Code
gemalt: 480 x 270 Bildpixel, echte Perspektive, Licht und Dunst, am Ende auf eine Palette
reduziert. Vorbild sind die Konzeptbilder vom 04.10. (Kuestenpark im Abendrot, Lesezimmer zur
blauen Stunde).

| Datei | Inhalt |
|---|---|
| `px.py` | Werkzeugkasten: Leinwand, Masken, Rauschen, gerasterte Verlaeufe und Lichthoefe, Palette |
| `room3d.py` | Zimmer in Zentralperspektive: Ebenen per Pixel zurueckgerechnet, Moebel als Quader, Licht |
| `park.py` | Park am Meer im Abendrot (Ort `PARK`) |
| `reading_room.py` | Lesezimmer am Abend (Ort `LIVING`) |
| `build.sh` | rendert alle Bilder nach `app-sim/src/game/assets/scenes/` |

Was das Spiel ueber ein Bild wissen muss (Gehflaeche, Figurgroesse, antippbare Plaetze, Ausgaenge)
steht in `app-sim/.../matrix/GameScenes.kt` - wer ein Bild aendert, prueft dort die Koordinaten.

Benoetigt Python 3 mit `numpy`, `scipy`, `Pillow`. Vorschau in dreifacher Groesse:
`python3 park.py /tmp/park.png` legt zusaetzlich `/tmp/park_x3.png` an.
