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

## Fennec aus dem Key-Design als Puppe (ersetzt die 3D-Probe, 04.10. spaet)

Rueckmeldung zur 3D-Probe: "steif wie eine Puppe, sieht nicht aus wie das Key-Design". Deshalb
kommt Fennec jetzt direkt aus der gemalten Figur des Key-Design-Blatts:

| Datei | Inhalt |
|---|---|
| `source/fennec_key.png` | die grosse Figur oben links im Blatt, freigestellt (rembg mit isnet, Schwanz aus BiRefNet ergaenzt), Augen und Nase leicht nachgezogen, Rest des Luchsschwanzes entfernt |
| `puppet.py` | Puppe: Teile mit weichen Gewichten, Drehung um Gelenke, Vorwaerts-Abbildung mit Ueberabtastung; `pixelize` erhaelt Tuschelinien (Augen, Konturen) beim Verkleinern |
| `fennec_key.py` | Teile und Gelenke, Augen-zu-Variante, Rueckansicht, Bewegungen (Gang, Ruhe, Sprung, Schlaf, Drehung) |

Ohren, Schwanz und Mantelzipfel schwingen ueber die Federn aus `motion.py` nach. Der Bogen hat
jetzt 128 x 128 je Bild (Fuesse auf Zeile 125). `python3 rich_sheets.py` schreibt ihn;
rembg wird dafuer nicht gebraucht, die freigestellte Quelle liegt im Repository.

## Bodenkontakt und Wenden (05.10., Folge-PR)

`fennec_key.py` zerlegt jedes Bein in Oberschenkel, Unterschenkel und Stiefel.
Eine Zwei-Segment-Loesung verfolgt die Boden- und Schwungphase ohne Laengenstauchung.
Die gemalte Quelle bleibt erhalten. Wendeposen werden nicht mehr horizontal zusammengestaucht;
waehrend des Gehens hat die Gangfolge Vorrang vor statischen Zwischenbildern. Ein eigener
monotoner Gangtakt startet bei Kontakt, stoppt mit der Bewegung und begrenzt Pausenspruenge.

Pruefung: `python3 -m unittest test_fennec_gait -v`, dann `python3 rich_sheets.py`.
Vorschau: [Fennec-Gang](fennec-gait-preview.gif). Die Vorschau zeigt die Sprite-Bewegung,
keine Aufnahme aus der APK. Die Rasterung erlaubt einen Pixel Abweichung am Bodenkontakt.
Der damalige Stand hatte noch keine neuen Profilzeichnungen. Die folgende Erweiterung
ergaenzt Kopfansichten; volle Koerperprofile und Wegkopplung bleiben offen.

## Mimik, Umschauen und Gestik (05.10., Erweiterung desselben PR)

Fennecs aktueller Bogen hat **68 Bilder zu 128 x 128 Pixeln**. Neu gezeichnete Koepfe
ersetzen das starre Gesicht: Dreiviertelblick, Profil, Vorder-/Rueckansicht, Blinzeln,
Neugier, Konzentration und Freude. `fennec_faces.py` isoliert die Hauptkomponente jeder
Atlaszelle und registriert Mimikvarianten auf gemeinsame Ansichtsmasse und Halsanker.
Der Koerper bekommt eine Handbewegung zum Mantel sowie versetzte Bewegung von
Fellsträhne, Schwanzspitze und Mantelzipfel. Das sind vorberechnete 2D-Animationen,
noch keine an das Wetter gekoppelte Stoffsimulation.

Die ersten 39 Indizes behalten ihre Rollen. 39–46: Ruhe vorn; 47–54: Ruhe hinten;
55: Blinzeln vorn; 56–61: Freude vorn; 62–67: Freude hinten.
Die Auswahlregel laesst Blinzeln und Freude auch bei gemerkter Vorderansicht zu.
Kopfprofile sind neue Zeichnungen; seitliche Koerper und Links-/Rechts-Spiegelung
stammen weiterhin aus dem bestehenden Rig.

Quellen: `source/fennec-head-atlas.png`, `source/fennec-head-expressions.png`.
Vorschau: [Mimik und Gestik](fennec-expression-preview.gif), gerenderte Sprites,
keine Aufnahme aus der APK. Die Gangvorschau oben dokumentiert die vorherige Version.

```bash
python3 rich_sheets.py
python3 rich_sheets.py --expressions fennec-expression-preview.gif
python3 -m unittest test_fennec_faces test_fennec_gait -v
```

Acht lokale Python-Tests gruen; Kotlin-Regressionen ergaenzt, Android-Pruefung ueber CI.
Naechster Schritt: Wirkung in der APK pruefen, danach eigene seitliche Koerperzeichnungen
und streckenabhaengige Schritte. Weitere Wesen folgen nach Beurteilung dieser Figur.
