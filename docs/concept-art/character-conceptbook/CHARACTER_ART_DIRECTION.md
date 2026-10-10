# Itoeva World - Character-Konzeptbuch

Band 3, 4. Oktober 2026. Ergaenzt das Landschafts-Konzeptbuch (Band 2).

**Status: Referenzablage auf GitHub am 04.10.2026 von Martin freigegeben. Die neuen Figuren und ihre Spielfunktionen bleiben Vorschlaege.**

## Ziel

Die sechs bekannten Wesen erhalten etwas fantasyhaftere Designvarianten. Der Junge aus der Gebirgsstudie wird als eigenstaendiger junger Reisender ausgearbeitet. Zwei weitere Figuren erweitern die visuelle Auswahl: Bramble, ein kleines Waldwesen mit Rehform, und Luma, ein Mondfalter. Beide Namen sind Arbeitsnamen. Fuer alle drei neuen Figuren sind Aufnahme ins Spiel, Spielbarkeit, Rollen und Faehigkeiten offen.

Die neuen Varianten ersetzen keine bereits von Martin und Claude verbesserten Sprites. Vor einer Umsetzung die laufenden Aenderungen ansehen und gezielt abgleichen. Der Auftrag dieser Sitzung betrifft ausschliesslich Konzeptbilder und Beschreibungen.

## Gemeinsame Gestaltung

Die bestehenden Wesen bleiben anhand ihrer Koerperform erkennbar: Fennecs grosse Ohren, Gloops weiche Kuppel, Starlets fuenf Spitzen, Pufflings runde Flaumbogen, Wyrmlings Fluegel und Schwanz, Hootlets Gesichtsscheibe. Fantasy entsteht durch Material, kleine Reiseaccessoires und einzelne besondere Akzente. Ein Accessoire soll die Kontur ergaenzen, nicht die Identitaet verdecken.

Naturfarben, Creme-Lichter und warme Stoffe verbinden die Figuren mit den neuen Landschaften. Pro Figur sind acht Startfarben mit Hexwerten dokumentiert. Diese Werte sind Designvorschlaege; die generierten Bilder enthalten weitere Zwischentoene. Die tatsaechliche Laufzeitpalette erst mit Claudes bestehender Farbumsetzung abgleichen.

Die Zeichnungen verbinden Bleistift, Farbstift und leichte Aquarellflaechen. Die kleinen Pixelbeispiele zeigen nur die Uebertragungsrichtung. Sie sind keine massgleichen, produktionsfertigen Sprite-Sheets. Ansichten und Posen muessen vor Animation hinsichtlich Proportionen und Details vereinheitlicht werden.

## Bildtiefe und Material

Volumen durch wenige grosse Tonwertgruppen aufbauen: Schatten, Grundfarbe, Lichtflaeche, kleiner Glanzpunkt. Auch in kuehler Nachtpalette muss die Figur vor ihrer jeweiligen Landschaft lesbar bleiben. Nicht jede Kante gleichzeitig mit einem hellen Rand versehen.

Fell/Flaum: einige grosse Konturbogen statt zufaelliger Einzelhaare. Stoff: Hauptfalte und Saum statt Musterteppich. Leder: ein breiter Mittelton mit gezielter heller Kante. Metall/Stein: kleiner klarer Akzent. Gloop: transparente Wirkung ueber Kontur und wenige Innenkanten; kein permanentes weisses Glasglanzband. Fluegel: grosse getrennte Flaechen mit wenigen tragenden Linien.

## Groessen und Bodenpunkte

Die Tabelle im PDF und das Feld height in characters.json sind ein relativer Entwurf: der Junge steht auf 100 Einheiten. Gemessen wird die ruhige Hoehe vom Boden bis zum hoechsten festen Punkt inklusive Ohren/Hoernern, bei eingeklappten Fluegeln. Es handelt sich nicht um Pixelzahlen, Zentimeter oder aus den Bildern gemessene Groessen.

Fennec 58, Gloop 38, Starlet 36, Puffling 42, Wyrmling 68, Hootlet 46, Junge 100, Bramble 62, Luma 34. Die Bildtafeln selbst sind unterschiedlich vergroessert und bilden diese Verhaeltnisse nicht exakt ab. Die sechs bestehenden Figuren nicht automatisch auf neue Spielgroessen skalieren. Im ersten Umsetzungsschritt deren vorhandene Groessen beibehalten und nur die neuen Figuren vergleichend testen.

Feste Bodenanker vermeiden Springen beim Animationswechsel. Sitz-, Schlaf- und Gehposen benoetigen eigene nachvollziehbare Anker; die Bounding Box allein ist kein Bodenpunkt. Ohren, Schwanz, Umhang und Fluegel duerfen den Anker nicht verschieben. Luma braucht bei einer spaeteren Umsetzung einen bewusst definierten Landepunkt; die Hoehe einer schwebenden Pose ist kein Bodenanker.

## Animationssprache

Zuerst Stand/Atmung, Gehen, Sitzen/Ruhen und eine charakteristische Aktion. Bestehende Zustands- und Animationssysteme wiederverwenden. Die Tafeln zeigen Key-Posen, keine komplette Framefolge.

Primaere Bewegung: Koerper und Bodenkontakt. Sekundaere Bewegung: Ohren, Schwanz, Umhang, Tasche oder Fluegel reagieren leicht verzoegert. Accessoires duerfen weder im Schritt verschwinden noch bei Richtungswechseln unkontrolliert die Seite wechseln. Proportionen zwischen Front-, Seiten- und Rueckenansicht vor dem Bau angleichen.

Gemeinsame Ausdrucksbasis: neutral/ruhig, neugierig, froh, muede und nachdenklich. Die Ausdrucksstaerke je Figur variieren, ohne einen festen Plot oder neue Persoenlichkeitslogik aus einer Pose abzuleiten. Die vorhandenen Autonomieentscheidungen im Stream und die aktive Spielersteuerung in Itoeva 2 bleiben davon getrennt.

## Neue Figuren: offene Entscheidungen

- Der junge Reisende: Design aus dem Jungen mit braunem Haar, gruener Kleidung und braunem Rucksack der Gebirgsstudie. Noch ohne Namen. Ob spielbare Figur, Bewohner oder Reisegefaehrte, entscheidet Martin spaeter. Das illustrierte Alter ist eine Stilentscheidung fuer diese Studie und keine reale Person.
- Bramble: Arbeitsname fuer ein kleines, sanftes Waldwesen mit Rehform, Asthoernern und Mooskragen. Noch kein neuer Bewohner und keine neue Spezies im Code.
- Luma: Arbeitsname fuer ein kleines Mondfalterwesen. Fluegel und dezentes Licht sind visuelle Vorschlaege; keine neue Flugmechanik, Lichtfaehigkeit oder Begleiterfunktion ist dadurch freigegeben.

Wegsteine, Anhaenger und Sternmuster haben in diesen Studien keine definierte Spielfunktion. Neue Gegenstaende, Inventarwerte, Magie oder Kampfsysteme daraus nicht automatisch ableiten.

## Pixel-Art-Uebertragung fuer Claude

1. Die aktuell von Martin und Claude ueberarbeiteten Figuren sowie Raster, Palette, Assetformat, Animationen und Renderer lesen. Keine unbemerkte Rueckkehr zu aelteren Formen.
2. Die gewaehlte Figur zunaechst als schwarze Silhouette im echten Spielmassstab pruefen. Eine eindeutig lesbare Grundform vor Details bauen.
3. Drei bis vier Hauptfarbgruppen, dann gezielte Schatten und Akzente setzen. Die acht Startfarben koennen angepasst werden, wenn das aktuelle Palettensystem es verlangt.
4. Bodenanker, Koerpervolumen und Negativraeume festlegen. Bei Wyrmling Arm und Fluegel, bei Fennec Ohren, bei Starlet Spitzen, bei Luma Fluegelflaechen getrennt halten.
5. Die Basispose mit den wichtigsten Bewegungen abgleichen. Tragepunkte fuer Karte, Becher oder Rucksack nicht nur in einer Ansicht loesen.
6. Vorher-/Nachher-Bilder auf hellem und dunklem Welt-Hintergrund, im Vorder- und Hintergrundlaufbereich sowie sitzend und gehend ansehen. Vorhandene passende Tests ausfuehren.
7. Erst nach Martins Rueckmeldung die naechste Figur bearbeiten. Neue Figuren separat entscheiden und in einem eigenen kleinen Schritt integrieren.

## Spaetere Uebergabe

Martin hat die Ablage dieses Character-Pakets am 04.10.2026 freigegeben. Vor der Umsetzung den aktuellen Stand pruefen und eine Figur bzw. ein klar begrenztes Paket auswaehlen. Keine laufenden Aenderungen ueberschreiben und nicht automatisch mergen. Die neuen Landschaftsstudien aus Band 2 bleiben ausserhalb dieses Uploads.

## Herkunft

Visuelle Ausgangspunkte sind die sechs Charakterstudien und die Gebirgsstudie aus dieser Unterhaltung. Die neun neuen Tafeln wurden mit Imagegen erzeugt. Die vorhandenen Figuren sind gestalterische Weiterentwicklungen; die drei zusaetzlichen Figuren sind neue Vorschlaege. Eine aktuelle Codepruefung oder Implementierung der Figuren war nicht Teil dieser Sitzung.
