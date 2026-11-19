# Gameplay-Vorschau 0.19.13 (Build 75)

Die Android-App `Itoeva Gameplay` startet `scenes/gameplay.tscn`. Das Paket
`com.itoeva.gameplay.preview` und der Spielstand `gameplay-preview.json` sind
für diese Vorschau getrennt. Im Hauptmenü führt **Spielorte testen** direkt zu
Erkundung oder Kampf. Sonnenhain, Nebelwacht, Mühlenbach, Aschenstieg, Strand,
Siegelhof und Relaisgarten enthalten zusammen zehn regionale Gegner.

Mooslyn verwendet im Kampf die freigegebenen Cape-, Taschen- und Pixelposen
aus dem Erkundungsspiel. Der Aschenläufer nutzt die Drei-Wurzeln-Lichtung mit
Anlauf, Krallenhieb und Glutsprung auch im normalen Zweierkampf. Die übrigen
Schauplätze verwenden ihre jeweilige Ortsgrafik mit angepasster Aufstellung,
Lichtfarbe und sparsamen Ortsreaktionen. Wasser bewegt sich innerhalb seiner
Maske; der Sonnenhain-Wind bleibt auf die sechs Strauchspitzen begrenzt.

In der Erkundung reagieren Blüten auf Nähe bei höherem Tempo oder auf eine
Landung. Mooslyn kann sie zusätzlich mit **Blüten wecken** anstoßen. Höchstens
18 kurzlebige Blatt- und Pollenpartikel bleiben gleichzeitig aktiv. Gegner
wenden sich bei Annäherung zur Figur; die Dornmotte schwebt auch im Wachzustand.
Die vorhandenen Hufkontakt-, Sand- und Spureneffekte bleiben erhalten.

Karten lassen sich aus der Hand ins Feld ziehen. Zielkarten werden über einem
gültigen Ziel losgelassen. Halten öffnet die Kartenansicht; das Ablegen außerhalb
des Feldes verbraucht weder Karte noch Kristalle. Tippen bleibt möglich.

## Build

`python3 tools/stage_gameplay.py /absolute/path/to/new-stage` erzeugt das
Android-Exportprojekt mit eigenem Namen, Paket, Spielstand und Version.
Godot 4.6.3 und dessen Android-Templates werden verwendet. Den Export mit
`tools/fix_duel_lab_android.py` reparieren, mit `zipalign -P 16` ausrichten und
anschließend signieren. Die separate Testsignatur muss für Updates beibehalten
werden.

## Prüfung

- `tests/gameplay_play.gd`: alle zehn Gegner, sieben Orte, neue Mooslyn-Posen,
  Blütenreaktion und Partikelgrenze, Wolfshieb und Sprung samt Unterbrechung,
  Kartenwisch, Heilkarte, ungültige Ablage und Rückkehr zur Erkundung.
- `tests/ground_contact_play.gd`: 45 Prüfungen zu Hufkontakt, Sand, Spuren,
  Sprungpuffer und Beleuchtung.
- `tests/duel_lab_play.gd`: Deckspeicherung, KI-Zug, Aufstellung, Laufrichtung,
  Wolfssprung und Wurzelbank. Mit festem Zeitschritt von 30 FPS ausführen.

Die Vorschau wurde unter Godot gerendert. Android-Signatur, Paketangaben und
16-KB-Ausrichtung wurden geprüft. Eine Installation oder Leistungsprüfung auf
einem physischen Android-Gerät steht noch aus.
