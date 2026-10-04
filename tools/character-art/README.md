# character-art – die sechs Wesen in feiner Pixel-Art

Neuentwurf der Figuren nach den Charakterstudien vom 04.10.
(`docs/concept-art/character-art-studies/`, PR #326). Bewusst **keine Uebersetzung** der
bisherigen 16x19-Zellen-Figuren, sondern neu gezeichnet: 64 x 64 Pixel je Figur.

| Datei | Inhalt |
|---|---|
| `sprite.py` | Werkzeugkasten: Teile mit Woelbung aus der eigenen Form (Licht von oben links), farblich verschobene Tonrampen, Fellstruktur, glaenzende Augen, selektive farbige Kontur |
| `sheets.py` | Bilderboegen fuers Spiel (siehe unten) |
| `characters.py` | Fennec, Gloop, Starlet, Puffling, Wyrmling, Hootlet - je eine Funktion, Farben aus den Paletten der Studien |

`python3 characters.py /tmp/figuren.png` rendert alle sechs nebeneinander (sechsfach vergroessert).
Benoetigt Python 3 mit `numpy`, `scipy`, `Pillow`.

Die Identitaeten (Namen, Rollen, Entscheidungslogik) bleiben unveraendert.

## Im Spiel (seit 04.10.)

`python3 sheets.py` schreibt je Wesen einen Bogen nach `app-sim/src/main/assets/creatures/`
(11 Bilder: Ruhe, Ruhe eingeatmet, Blinzeln, 4 x Laufen, 2 x Freude, 2 x Schlafen; Reihenfolge
wie in `CreatureSprites.kt`). Die Figuren stehen dort auf Zeile 61. Welches Bild gezeigt wird,
entscheidet `CreatureSprites.look` aus der groben Pose der bestehenden Ablaeufe.

## Fantasy-Varianten (Character-Konzeptbuch, seit 04.10.)

Die Figuren werden einzeln nach `docs/concept-art/character-conceptbook/` weiterentwickelt, je eine
Figur pro Schritt. Umgesetzt: alle sechs - **Wyrmling** (getrennte Fluegelhaeute, Halstuch mit
Bernstein, eigene Schlafpose), **Fennec** (Reisemantel, Wegstein, Flasche), **Gloop** (Blattschulter,
Samenstein, Tasche), **Starlet** (Himmelskragen, Mondanhaenger), **Puffling** (Kapuze, Blattschliesse),
**Hootlet** (Sternenumhang, Messingschliesse, Kartenrolle). Eine eigene
Schlafpose meldet eine Figur in `sheets.OWN_SLEEP` an; die anderen sinken zusammen (`curled`).

## Blickrichtung im aktiven Spiel

`directions.py` zeichnet fuer jedes der sechs Wesen eine eigene Vorder- und Rueckansicht mit
Fussanker auf Zeile 61. `sheets.py` haengt diese sechs Bilder an den bisherigen Bogen an;
die ersten elf Bilder bleiben pixelgenau identisch. `CreatureSprites.look` waehlt sie nur
fuer Hoch-/Runtergehen in Itoeva 2. Nach dem Anhalten bleibt die Blickrichtung stehen,
waehrend die Schrittphase endet. Links/rechts verwenden weiterhin die Seitenansicht.

Pruefung: `python3 -m unittest test_directions.py`; Bildbogen mit
`python3 sheets.py --preview /tmp/figuren-richtungen.png` ansehen.
