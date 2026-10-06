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

Neun lokale Python-Tests gruen; Kotlin-Regressionen ergaenzt, Android-Pruefung ueber CI.
Naechster Schritt: Wirkung in der APK pruefen, danach eigene seitliche Koerperzeichnungen
und streckenabhaengige Schritte. Weitere Wesen folgen nach Beurteilung dieser Figur.

Korrektur nach Martins Sichtpruefung: Kopf 24 Quellpixel tiefer, Hals-/Brustspitze
unter dem Mantelkragen. Eigene Fussspuren je Huefte statt sich kreuzender Beine;
geringere Schritthoehe. Die Vorschau zeigt rechts und links nebeneinander.
Explizite Laufrichtung bestimmt die Spiegelung auch beim Anhalten; die Schattenseite
ist nur noch der Rueckfall fuer Ablaeufe ohne explizite Richtung.

## Seitlicher Gang aus eigenen Zeichnungen (05.10., nach erneutem Gangfeedback)

Die zuvor geloesten IK-Ziele machten die breitbeinige Ausgangszeichnung nicht zu
einem guten Gang: Stiefel blieben auswaerts orientiert, die Figur wirkte hockend.
`fennec_walk.py` ersetzt deshalb **nur die acht seitlichen Gehbilder 9–16** durch
separate gezeichnete Profilposen aus `source/fennec-walk-profile-atlas.png`.
Gemeinsame Palette und ein einziger Massstab gelten fuer alle acht Bilder;
Registrierung am tuerkisen Schmuck und an der Bodenzeile statt an der Breite der
Silhouette. Neue Knie-/Stiefelposen enthalten das Abrollen und Vorschwingen.

`test_fennec_walk.py` prueft das gerenderte aktive Asset: Bodenkontakt in allen
Bildern, stabile Koerperhoehe, angehobene Durchgangsfuesse und rueckwaertige
Standbewegung relativ zum Koerper. Die bisherigen neun Tests laufen weiterhin;
die IK-Tests betreffen nun das erhaltene Werkzeug, nicht den aktiven Seitengang.
Insgesamt zwoelf Python-Tests gruen. Ausfuehren mit:

```bash
python3 -m unittest test_fennec_faces test_fennec_gait test_fennec_walk -v
```

Quelle und exakter ImageGen-Prompt: `source/fennec-walk-profile-prompt.md`.
Die Front-/Rueckgaenge und Ruhe-/Reaktionsposen nutzen weiterhin den vorhandenen
Rig; der Wechsel zur seitlichen Gangzeichnung braucht noch Beurteilung in der APK.
Der Gang ist zeitgetaktet, keine Laufzeit-Stoff- oder Vollkoerperphysik.

## Fennecs Bewegungsrepertoire (05.10., Folge auf den gemergten PR #331)

Der aktuelle Bogen hat **138 Bilder zu 128 x 128**. Eigene Ganzkoerperzeichnungen
ersetzen nun auch Front-/Rueckgang, gerichteten Stand und die Sprung-/Schlafposen.
Die ersten 68 Rollen bleiben erhalten; 68–75: Frontgang, 76–83: Rueckgang,
84–99: Profilaktionen, 100–106: Frontaktionen, 107–113: Rueckenaktionen.
Die alten vier Richtungs-Gangplaetze bleiben als kompatible Aliasbilder im Bogen.
Der seitliche Gang aus PR #331 ist pixelgleich erhalten.

`fennec_mobility.py` isoliert die Zeichnungen als Zusammenhangskomponenten, sortiert
sie nach wirklichem Zeilenlayout und verwirft angeschnittene Figuren. Jede Quelle
hat einen festen Massstab; Front-/Rueckgang werden am Kopf und an der Bodenzeile
registriert, damit der wechselnde Schwanz nicht den Rumpf mitzieht. Gerichtete
Ruhe nutzt das vorhandene Puppet-Werkzeug fuer kleine Kopf-/Ohr-/Saumregungen mit
festen Stiefeln. Der eigene Front-Lidschlag ersetzt die alte breite Frontpose.

`CreatureSprites.MotionCue` beschreibt die Darstellung einer vorhandenen Handlung:
Sprung, Buecken, Knien, Hinsetzen, Aufstehen, Strecken, Greifen, Treten. `DockScreen`
liefert den Fortschritt beim Hinsetzen/Aufstehen, im Training, beim Greifen,
Abstellen, Schalten, Strecken und Schuss. Sprungphasen folgen den wirklichen
Raster-Hoehen, statt die ganze Freudenfolge irgendwo in der Luft zu beginnen.
Abbruch gibt den Hinweis frei; ein alter Coroutine-Abschluss darf die naechste
Handlung nicht loeschen. Sitzpose bleibt beim Verweilen sichtbar. Echte Reminder
und Gruppenspiel koennen diese Routine-Darstellung uebersteuern.

Die Zeichnungen enthalten Bodenbewegungen selbst: Strecken und Greifen bekommen
keinen zusaetzlichen Raster-Huepfer. Fortbewegung hat Vorrang vor Handgestik.

```bash
python3 rich_sheets.py
python3 rich_sheets.py --mobility fennec-mobility-preview.gif
python3 -m unittest test_fennec_faces test_fennec_gait test_fennec_walk test_fennec_mobility -v
```

[Bewegungsvorschau](fennec-mobility-preview.gif): gerenderte Sprite-Folgen, keine
APK-Aufnahme. Exakte Generierungsanweisungen und Quellenzuordnung stehen unter
`source/fennec-mobility-prompts.md`. Bildgenerierung mit dem integrierten ImageGen.
18 lokale Python-Tests bestehen; Kotlin-Verhaltenstests pruefen
Bildwahl, Richtung, Hoehe, Endposen und ungueltigen Fortschritt in Android-CI.

Grenzen: Front-/Rueckansicht teilen die Hockpose fuer Ausholen und Landung;
gerichtetes Knien verwendet vorerst Buecken, gerichtetes Greifen die Streckpose.
Der Schuss hat nur eine Profilzeichnung. Kein eigener Lauf-/Schleich-/Saltozyklus,
kein neues Bewegungsrecht oder Eingabeknopf und keine Weg-/Windphysik.
APK-Uebergaenge, Haende an Requisiten und Sitzhoehen muessen am Geraet beurteilt werden.
Die bestehende Clip-Aufzeichnung verwendet weiterhin das grobe Raster.
Ruecksetzen: Generator, PNG, Auswahlregel und Routine-Hinweise gemeinsam auf
`97f767d514c66b41ad69963e7ace5e953cbe092e`; keine Datenmigration.


## Analoge Smartphone-Steuerung und schnelle Bewegungen

Der Bewegungsbogen ergaenzt 114–121: Vierbeiner im Profil (8 Phasen),
122–125: Vierbeiner vorn, 126–129: Vierbeiner hinten (je 4 Phasen) und
130–137: Ausweichrolle. Front-/Ruecklauf halten jedes Bild zwei Schrittphasen,
so bleibt die volle Zyklusdauer gleich. Originalprofilgang 9–16 bleibt pixelgleich.
Quellen und exakte Built-in-ImageGen-Prompts: `source/fennec-controller-prompts.md`.

Links unten analog ziehen: innerer Ring = Gehen, weiter ziehen = schneller,
aussen = vierbeiniger Lauf. Diagonalen sind normiert. Rechts tippen = Sprung,
waagerecht wischen = Rolle in Wischrichtung, nach oben wischen = Sprung,
nach unten wischen oder 500 ms halten = Sitzen/Ruhe. Erneutes Halten oder
links ziehen steht wieder auf. Beide Pointer-IDs bleiben unabhaengig;
Abbruch, Menue und Hintergrund geben Eingaben frei. Tastatur: WASD/Pfeile,
Shift schnell, Space Sprung, Q Rolle, R Ruhe, E/Enter Interaktion.
Gamepad-D-Pad und A/X/B/Y entsprechen Bewegung/Sprung/Rolle/Ruhe/Interaktion.

`GameMovement` ist reine Rechnung im vorhandenen Bildtakt. Beschleunigung und
Abbremsen, Gangphase, kurze Gangart-Ueberblendung und Hysterese teilen denselben
Zustand. Einmalig ausgeschnittene 128-px-Bitmaps vermeiden Android-Texturlimits; keine
Bitmap-Erzeugung je Bild. Standflaechen werden pro Layout gemerkt.
Ein Sprung hilft zur naechsten erreichbaren Standflaeche in Zug-/Blickrichtung.
Teetisch im gemalten Wohnzimmer, niedrige Kiste im Park, Tischplatten der
Zellenzimmer sind ausdruecklich definiert. Keine beliebige Bildpixel-Erkennung.
Auf der Oberkante bleibt die Hoehe erhalten, Kanten fuehren monoton zum Boden;
zu hohe Gegenstaende bleiben gesperrt. Rollen kollidiert am Boden und verleiht
keine Unverwundbarkeit. Ruhe ist freiwillig und veraendert keine Beduerfnisse/XP.

Die neue Rolle ist eine Profilzeichnung; Vorwaerts-/Rueckwaertsrollen hat noch
keinen eigenen Richtungsbogen. Physische Telefon-Latenz, Ueberblendung, Kamerafahrt
und alle Moebelanker sind weiterhin an der APK zu bewerten. Die GIF zeigt nur
Spritefolgen. Fuenf instrumentierte Tests pruefen zwei Finger, Wischen, Halten
und Eingabeabbruch; ein Bitmap-Test prueft kleinen, pixelgleichen Cache. JVM-Tests pruefen Tempo, Diagonalen, Landen, Kanten und Ruhe.

Android-Kompilierung von Debug, Game und AndroidTest lokal erfolgreich.
Die sechs neuen instrumentierten Tests sind kompiliert, ihre Ausfuehrung wartet
auf einen GitHub-Runner; der erste Controller-Lauf scheiterte vor dem Start
an fehlender Runner-Zuteilung.


## Alle fuenf weiteren Wesen animiert (06.10.2026)

`ensemble_motion.py` ersetzt Gloop, Puffling, Wyrmling, Starlet und Hootlet durch
feine Boegen mit je **138 Rollen zu 128 x 128**. Es sind 32 neue gezeichnete
Quellposen je Wesen plus registrierte Regungen und Aktionszwischenbilder, keine
138 unabhaengigen Zeichnungen. Die Konzeptstudien dienen als Identitaetsreferenz.
Fennecs ausgeliefertes Asset bleibt bytegleich zum gemergten PR #332.

| Wesen | Eigene Bewegungssprache |
|---|---|
| Gloop | Niedrige Stauch-/Streckwelle ohne Beine, Spross und Tasche folgen versetzt |
| Puffling | Kleine federnde Schritte, Flaum und Kapuze folgen dem Koerper |
| Wyrmling | Geerdeter Drachengang, schneller niedriger Lauf, Fluegel und Schwanz balancieren |
| Starlet | Zwei untere Sternspitzen setzen wechselnd auf, seitliche Spitzen geben Gestik |
| Hootlet | Kleine Krallenschritte, geduldige Kopfneigung, Federn und Sternenumhang folgen |

```bash
cd tools/character-art
python3 ensemble_motion.py
python3 ensemble_motion.py --preview ensemble-motion-preview.gif
python3 -m unittest test_ensemble_motion -v
```

Die Bildwahl und alle MotionCue-/Tempo-/Gangartuebergaenge verwenden dieselben
Rollen wie Fennec. Vorder-/Rueckgang hat je vier gezeichnete Phasen, die je zwei
Takte halten. Schnelle Seitenbewegung hat vier Rollen; Gloop/Wyrmling verwenden
drei gueltige Phasen mit einer wiederholten Uebergangsphase, weil die vierte
Quellpose die Augen schliesst und als Sprint unpassend ist. Der Importmanifest
benennt diese Auswahl. Gerichtetes Schnellgehen verwendet vorerst die eigenen
Front-/Rueckschritte im schnelleren Spieltakt; keine neuen Flugrechte.

Ruhe bewegt Kopf, Spitzen/Spross und Saum getrennt, waehrend der Bodenkontakt
fest bleibt. Buecken, gerichtetes Sitzen, Knien und Aufstehen nutzen vorhandene
Skelettverformung; Strecken, Greifen, seitliches Sitzen, Schlaf und eingerollte
Rolle haben gezeichnete Quellen. Die Rolle dreht die eigene kompakte Haltung.
Treten nutzt vorerst die nach vorn reichende Koerperpose, kein eigener Fussball-
Schussbogen. Vorder-/Rueckwenden verwendet die Front-/Rueckbilder statt neuer
Dreiviertelzeichnungen. Diese Grenzen nicht als 138 neue Vollkoerperzeichnungen
oder vollstaendige Richtungsmobilitaet beschreiben.

`source/ensemble-front-blink-atlas.png` ergaenzt echte Front-Lider. Nur die
verifizierten Augenfenster werden importiert; Hootlets Brille und Wyrmlings
Fluegel bleiben pixelgleich. Unvollstaendige Quellen, Randanschnitte und unklare
Zeilenordnung werden abgelehnt. Ein Massstab fuer den gesamten Quellatlas
verhindert, dass kurze Sitz-/Laufposen ungewollt auf Standhoehe gedehnt werden.
Alpha-Halos werden beim Import verworfen; fertige Sprites haben harten Alpha.

`CreatureSprites.Rich.scaleFor` erhaelt die gemessene bisherige Standhoehe der
fuenf Wesen. Wyrmlings Schwanz begrenzt die Importgroesse besonders stark;
deshalb erhaelt er einen eigenen Darstellungsfaktor. Die Vorschau zeigt diese
Weltgroessen, ist aber eine Sprite-Schleife und **keine APK-Aufnahme**.

Alle sechs Wesen zusammen haben etwa 54 MiB unkomprimierte Einzelbilddaten
(138 x 128 x 128 x 4 je Wesen); Android laedt sie ueber den vorhandenen kleinen
Bitmap-Cache nur bei Bedarf. Kein grosser Atlas als GPU-Textur und kein neuer
Bildspeicher im Zeichentakt. Geraete-Speicherverbrauch noch nicht gemessen.

Validierung: acht Tests am ausgelieferten Bogen, achtzehn Fennec-Regressionen,
893 lokale Kotlin-Tests gruen. PR #333: Emulator-Tests API 26/35 und Release-
Torwaechter gruen. Erster Verify-Lauf scheiterte an Lint-Java-Heap (2 GiB);
CI-spezifisch 4 GiB und zwei Worker; voller Nachlauf #829 auf
`3bcfe19c9111e010b2ba7f7b5234f0e3c7505aa2` gruen (Tests/Lint/R8,
API 26/35, Release-Torwaechter). Physische APK-Sichtpruefung bleibt offen.
Generatoren `from_concept.py` und `sheets.py` sind historische Werkzeuge und
wuerden beim Schreiben die feinen Boegen ersetzen; fuer den aktuellen Stand
`rich_sheets.py` fuer Fennec und `ensemble_motion.py` fuer die anderen benutzen.

Exakte Prompts, Atlasbilder und Importmanifest unter `source/ensemble-*` und
`source/<wesen>-motion-atlas.png`. Ruecksetzen: gesamten Charakter-PR gemeinsam
auf `dc443b6d586d3a6a158ff900fadf57b952f54760`; keine Spielstandmigration.
