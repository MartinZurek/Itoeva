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

## Aus den Konzeptblaettern (aktueller Stand im Spiel)

Seit dem zweiten Durchgang am 04.10. stammen die Boegen in `app-sim/src/main/assets/creatures/`
aus `from_concept.py`: Die gemalten Posen der Konzeptblaetter
(`docs/concept-art/character-conceptbook/`, Branch `art/concept-studies-2026-10-04`) werden
ausgeschnitten, vom Papier freigestellt, verkleinert, auf eine gemeinsame Palette je Wesen gebracht
und mit einer dunklen Einpixel-Kontur versehen - Fell, Umhang, Taschen und die lebendigen Haltungen
der Studien bleiben so erhalten. Bogenformat und Fusslinie sind unveraendert (17 Bilder, Zeile 61),
der Spielcode bleibt gleich.

```
python3 from_concept.py                       # Boegen schreiben
python3 from_concept.py --preview /tmp/a.png  # alle Boegen vergroessert
python3 from_concept.py --poses /tmp/b.png    # die freigestellten Posen
python3 -m unittest test_from_concept.py
```

Posen je Wesen stehen in `POSES` (Rechteck im Blatt, gespiegelt ja/nein); `IDLE` waehlt fuer die
runden Wesen eine halb zugewandte Ruhepose, damit das Gesicht sichtbar bleibt.
Die gezeichneten Figuren (`characters.py`, `sheets.py`) bleiben als Werkzeug erhalten.

## Im Spiel (erster Durchgang, 04.10.)

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

## Feine 3D-Figuren mit Bewegung (Probe: Fennec, seit 04.10. abends)

Nach den Key-Design-Blaettern wird ein Wesen als Figur aus Formen im Raum gebaut und daraus in
Pixel-Art gerendert - so entstehen echte Drehungen und Bewegungen mit Nachschwingen:

| Datei | Inhalt |
|---|---|
| `rig3d.py` | Renderer: Ellipsoide aus jedem Blickwinkel, vierfach ueberabgetastet, je Pixel Material und Lichtstufe, dann Handpalette (Mehrheit je 4x4-Block), Fellstriche, Verdeckungsschatten, selektive Kontur |
| `fennec3d.py` | Fennec: Formen, Farbrampen, `Pose` (Gelenke, Ohren, Schwanzglieder, Umhang) |
| `motion.py` | Gang (8 Bilder), Ruhe mit Ohrzucken, Sprung (Ausholen bis Landung), Schlaf, Vorder-/Rueckansicht, Drehbilder; Ohren, Schwanz und Umhang als gedaempfte Federn |
| `rich_sheets.py` | schreibt den feinen Bogen (96 x 96, 39 Bilder) |

`CreatureSprites.lookRich` waehlt die Bilder, `CreatureSprites.Turn` spielt beim Richtungswechsel
die Drehung (rechts -> halb vorn -> vorn -> halb vorn -> links). Welcher Bogen vorliegt, erkennt
das Spiel an seiner Hoehe; die anderen Wesen bleiben vorerst beim einfachen Bogen.
