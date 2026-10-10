# Gezeichnete Schwimmzuege und Pixelwasser (10.10.2026)

Martin beschreibt nach dem bisherigen Korrekturversuch immer noch aufblasende
Boje-Koerper und eine bewegte Flaeche statt Wasser. Der Code bestaetigt die Ursache:
`SWIM` verwendete nur eine Standzeichnung; das Texturnetz verschob breite
Rumpfbereiche gegeneinander. Die Wasserzeichnung verschob die bestehende Textur,
ohne eigene wandernde Wellenkaemme oder bewegungsabhaengige Spur.

## Umsetzung

- Sechs neue gezeichnete Boegen mit je vier Zugphasen aus Seiten-, Front- und
  Rueckansicht. Pfoten, Fluegel und Sternspitzen sind wirklich unterschiedlich
  gezeichnet. Rumpfmasse wird beim Schwimmen nicht im Mesh vergroessert/verkleinert.
- Ein Massstab je Wesen, feste Kopfanker und 256-Pixel-Rahmen mit 64 Pixeln Rand
  verhindern, dass seitliche Schwimmglieder auf eine schmalere Figur gestaucht
  werden. Gezeichnete Schrraeglage wird nicht noch einmal gedreht. Gemeinsame
  Wasserlinie, geringer Auftrieb und Bob bleiben beim bestehenden Modell.
- Unterwasserteile sind mit 26 Prozent Alpha und blauer Toennung sichtbar; ein
  kompletter Schnitt entfernte bisher auch den Beinschlag. Moebelverdeckung gilt
  in beiden Wasserlagen weiter. Lichtkoordinaten beruecksichtigen den Atlasrand.
- Der bestehende Game-Vorlader dekodiert beide Boegen je Wesen, bevor Game bereit
  ist. Keine Bitmap-Erzeugung im Animationsbild. Andere Modi laden den zweiten
  Bogen erst bei Bedarf. Sechs Boegen benoetigen zusammen ca. 18 MiB RGBA-Cache.
- Wasser hat zusaetzliche gerasterte wandernde Kaemme mit dunklem Vorderhang,
  hellen Reflexen, kleiner Ufer-Schaumlage und rueckwaerts offener Schwimmspur.
  Die vermessene Maske schliesst Land, Stege und Felsen weiter aus. Alle Abschnitte
  lesen Weltkoordinaten; keine getrennte Wellenphase an den Abschnittskanten.

Primaere Gestaltungsreferenzen: Raymond Schlitter / SLYNYRD,
[Pixelblog 10: Water in Motion](https://www.slynyrd.com/blog/2018/10/12/pixelblog-10-water-in-motion)
und Cyanilux,
[2D Water Shader Breakdown](https://www.cyanilux.com/tutorials/2d-water-shader-breakdown/).
Uebernommen wurden Gestaltungsprinzipien (Kaemme, Licht, Stroemung, Wasserlinie),
keine fremden Bilder oder Shaderdateien. Bestehender Android-Canvas bleibt.

## Referenzbilder fuer die spaetere Raumumsetzung

Alle elf einzeln gemalten Innenraumtafeln stehen in
[`concept-art/interior-studies-v1`](concept-art/interior-studies-v1/README.md).
Sie haben eigene Architektur, Moebelstudien und Tag-/Nachtansichten nach dem
bisherigen Landschaftsstil. Martin wollte zuerst diese Bilder zur Orientierung:
in diesem Schnitt wird daraus noch keine neue Raumgeometrie gebaut. Die Tafeln
sind keine flachen Runtime-Hintergruende. JPG 96 fuer Projekt-Referenzen; die
urspruenglichen generierten PNG-Bilder bleiben erhalten.

## Pruefung und Grenzen

1.043 reine Kotlin-Tests gruen: komplette vorhandene Offline-Suite mit frisch
kompilierten Wasser-/Charakterklassen und zugehoerigen Tests. Neue Tests pruefen
konstante Rumpfkoordinaten, zwoelf gerichtete Bildrollen und gerasterte bewegte
Wellen. Sichtpruefung aller Boegen und der Softwarevorschau. Vier Phasen sind
stilisiertes Pixelspiel-Schwimmen, kein Schwimmsport-/Physiksimulator.

Native Android-Tests pruefen alle sechs Assetboegen, vier verschiedene Posen je
Ansicht, Transparenz und Cache. Die vorherige API-26-CI fuer `fd91325` hatte genau
einen fehlgeschlagenen Raumtest: eine einzelne Pixelzeile traf zu wenige Dielen.
Die Materialpruefung betrachtet jetzt eine Bodenflaeche und verlangt mehr als
zwoelf Farben; die exportierten elf Raeume zeigen dort 19 bis 30 Farben.
Die neue Android-CI wird im PR separat verfolgt. Lokal fehlt das Wrapper-JAR;
keine Build-/Workflowdatei wurde geaendert. Telefon-Bildrate, Touch-/Musikhoertest
und das subjektive Schwimm-/Wasserbild bleiben am Geraet abzunehmen.

Nachtrag PR-Kontrolle: Die Musik-Recovery hatte noch eine berechtigte offene
Review-Anmerkung. `isPlaying` pruefte nur das Vorhandensein eines Players, wodurch
`player != null && !isPlaying()` nie wahr wurde. Jetzt wird der echte
`MediaPlayer.isPlaying`-Zustand fehlergeschuetzt abgefragt; gestoppte oder bereits
freigegebene Decoder gelten als nicht laufend. Der native Wiedergabetest prueft
vorbereitet, laufend, gestoppt und freigegeben. Die bestehende 30-Sekunden-
Abgleichsschleife kann damit auch bei unveraenderter Rolle wieder starten.

Regeneration (Kotlin-Compiler und JUnit aus dem vorhandenen Offline-Werkzeug):

```sh
python tools/character-art/swimming.py
# SwimmingPreview.kt mit den produktiven Game-Klassen kompilieren:
kotlin -classpath EXPORT_CLASSES:GAME_CLASSES SwimmingPreviewKt "$PWD" EXPORT
python tools/character-art/swimming_preview.py EXPORT tools/character-art/swimming-water-preview.gif
```

[`Vorschau`](../tools/character-art/swimming-water-preview.gif) ist eine
Softwarezeichnung mit echten Assets, produktiven Kotlin-Bildrollen, Massstab,
Masken und Wellenkaemmen. Kein APK-Video; die native Wassertexturverformung,
Moebel und Laufzeitbeleuchtung sind darin nicht zusaetzlich nachgebaut.

Ruecksetzung: diesen Schnitt als Ganzes ruecknehmen; alte Boegen bleiben
unveraendert. Keine Preferences, Datenmodelle, Migration, Reminder-, LAS-,
Progressions-, Musik- oder Eingaberegeln geaendert. Kein Merge/APK-Auftrag.
