# Mooslyn: gemalte Bewegungsentwürfe, 26.10.2026

Basis: `claude/hd2d-pixel-sprites`, Commit `cb1c2b5`.
Arbeitsbranch: `codex/hd2d-motion-phases-20261026`.

**Zwischenstand, keine Bewegungsabnahme und keine APK-Freigabe.**
Der zweite gesicherte Schnitt enthält acht seitliche Geh- und acht Galoppphasen,
die Raster-/Alphaaufbereitung und eine ausdrücklich aktivierte Weltvorschau.
Die Gehversuche wiederholten zu oft dieselbe Beinstellung. Die aktuelle Fassung
ist ein Prüfentwurf, keine Behauptung eines bereits vollständig richtigen Gangzyklus.

`sources/mosslyn-gallop-8.png` ist die unveränderte Bildgenerierung nach der
freigegebenen Moosling-Vorlage. `mosslyn-gallop-8.json` hält die gemessenen
Gesichtsregionen und die vorläufig annotierten Flugabstände fest.

```bash
python3 tools/motion-art/prepare_cycle.py tools/motion-art/mosslyn-gallop-8.json tools/motion-art/mosslyn-walk-m2.json
python3 tools/pixel-pass/pixel_pass.py prototypes/godot-hd2d/assets/characters --species mosslyn
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p test_authored_cycles.py
```

Die Aufbereitung trennt ganze Alphakomponenten (auch bei Überschreiten der
Quellrastergrenze), registriert den Kopf einmalig und schreibt feste 320er-Zellen.
Eine einmalige einheitliche Skalierung pro Quellzeichnung gleicht die Größenfehler
der Generierung aus. Zur Laufzeit sind keine wechselnden Gesichtsskalierungen
vorgesehen. Maske und Bounds entstehen aus dem gemalten Original, nicht aus Pixeln.

Offen: eindeutige Abfolge der vier tragenden Pfoten, ruhiger Kopf im bewegten
Bild, Kontakte über den ganzen Zyklus messen statt aus einem Kontaktpaar schätzen,
acht wirklich wechselnde Gehphasen, vorne/hinten, Übergänge und Wenden. Erst
danach weitere Figuren. Keine Aussage „kein Rutschen“ ohne Kontaktmessung.

Die alten abgenommenen Dateien bleiben außerhalb der Vorschau aktiv. Kein
Workflow wird eingeschaltet und keine Version erhöht, bevor Martin Vorschauen
gesehen und die APK freigegeben hat.

## Gesicherte Weltvorschau

`MOTION_DRAFT=1 TAG=moos-phase-test SPECIES=mosslyn PLACE=PARK` vor dem
bestehenden `preview_pixel/play_capture.gd`-Aufruf aktiviert die Entwürfe. Ohne
diese Umgebungsvariable bleiben die abgenommenen Originalfolgen aktiv.

- `docs/validation/motion-20261026/Mooslyn_Seitenphasen_Entwurf.png`: tatsächlich
  verwendete Pixelfassungen, 1–8 Gehen und 1–8 Galopp.
- `Mooslyn_Weltvorschau_Entwurf.mp4` im selben Ordner: 9,4 Sekunden Seitenaufnahme
  aus der echten Welt (Stillstand, rechts gehen, Sprint, stoppen, links gehen).
- `capture-report.json`: numerische Auswertung der vollständigen 390-Bilder-Quelle.
  Die Tiefenabschnitte sind wegen Wegkollision überwiegend blockiert und ausdrücklich
  **kein Nachweis** neuer Front-/Rückwärtsbewegung. Deshalb fehlen sie im gelieferten Seitenclip.

Gemessen im Außenmaßstab: Gehen 2,3543 Welt/s bei 2,2084 Welt/Zyklus
(1,0661 Hz, ca. 8,53 Phasen/s); Sprint 3,8498 Welt/s bei 1,5323 Welt/Zyklus
(2,5124 Hz, ca. 20,10 Phasen/s). Die Zyklusstrecken basieren derzeit auf einer
vorläufigen Schätzung aus Standpfotenposen, nicht einer vollständigen Kontaktmessung.
Einheitliche Zeitabstände der Phasen garantieren noch keine konstante Standpfotengeschwindigkeit.
Die 0.15.6-Standardfassung und deren Spieltempo wurden nicht verändert.

244 Godot-Laufzeitchecks für die Entwürfe sowie die bestehenden 1164 Mooslyn-Checks
und zwei bestehende plus zwei neue Python-Assettests bestanden. Der Renderlauf
meldet wie der unveränderte Basislauf beim Beenden zwei noch gebundene Ressourcen
und ObjectDB-Leaks; kein Parse- oder Aufnahmelaufzeitfehler.

Frontentwurf verworfen: Tasche wechselt in der zweiten Reihe die Seite; auch eine
gezielte Bildkorrektur behebt das nicht durchgehend. Rückentwurf noch nicht
registriert oder eingebunden. Übergänge sind weiterhin die vorhandenen Zustandswechsel,
keine neu gemalten Verbindungsphasen. Weitere Figuren wurden bewusst noch nicht
auf einen ungeprüften Mooslyn-Maßstab umgestellt.

Bildquelle: eingebautes Imagegen, verbindliche Moosling-Bildvorlage plus explizite
Gangphasen und für das Gehen eine Poseführung. Keine 3D-Modelle. Die beiden
verwendeten gemalten Quellen stehen unverändert in `sources/`, die normalisierten
Atlanten in `prototypes/godot-hd2d/assets/characters/`.

## Kontaktmessung und inzwischen bestätigte Taktentscheidung

`python3 tools/motion-art/measure_walk_contacts.py` misst den sichtbaren
Sohlenumriss im gemalten Original. Ergebnis in `contact-estimate.json` neben
dem Video: Nahbeinstand 91px, Fernbeinstand101px, zusammen etwa192px pro
Zyklus statt der bislang angenommenen285px. Im Außenmaßstab sind das etwa
1,488 statt2,208Welteinheiten. Sohlenmitten sind nur eine Näherung für echte
Druckpunkte; die Differenz ist dennoch deutlich. Der letzte Phasenwechsel hat
gar keinen erkennbaren Standpfotenrückweg, andere Intervalle16–44px.

Bei unverändert2,35Welt/s erfordert die geschätzte gezeichnete Strecke etwa
1,58Gehzyklen/s statt des aktuellen Entwurfstakts1,06Hz. Die folgenden beiden
Möglichkeiten wurden Martin vorgelegt:

1. Höheren Geh-Takt verwenden (ca.1,58Hz), Kontaktphasen entsprechend gewichten
   und die fehlerhafte Abschlussphase nachzeichnen.
2. Den ruhigeren Takt erhalten und die gezeichnete Schrittlänge um etwa48Prozent
   vergrößern; anschließend Kontaktphasen und Gegenbein erneut prüfen.

**Martin hat Möglichkeit2 bestätigt:** ruhigen Geh-Takt erhalten und die
gezeichneten Schritte verlängern. Hüfte, Abrollen und Abstoß mitzeichnen statt
Beine nur zu spreizen oder Bilder zu dehnen. Etwa48Prozent ist eine vorläufige
Messgröße, keine starre Anatomievorgabe. Spieltempo nicht pauschal senken.
Nicht erneut nach dieser Entscheidung fragen. Der Entwurf bleibt bis zur
Korrektur nur in MOTION_DRAFT=1 sichtbar. Vollständige Übergabe:
`docs/ITOEVA_HD2D_MOTION_SAVESPOT_20261026.md` (SavespotM1).

## Fortsetzung M2: Beinwechsel nachgezeichnet, Kontaktprüfung offen

`mosslyn-walk-m2.json` und `sources/mosslyn-walk-m2.png` sind der aktuelle
Geh-Prüfentwurf. Der M1-Bogen bleibt unverändert erhalten; nur MOTION_DRAFT=1
verwendet den M2-Gehbogen. Der Galopp wurde nicht verändert.

Bildweg: eingebautes Imagegen mit freigegebener 2D-Vorlage und separater
Poseführung; zwei gezielte Bildkorrekturen (Schattenfell/Abstoß, hintere Pfote).
Die letzte Generierung ist unverändert als Quelle gespeichert. Prompts und
Poseführung liegen daneben. Danach Registrierung der ganzen Zeichnungen und
Pixelpass; keine Gliedmaßenverformung. Zwei falsche automatische Kopf-Treffer
wurden durch manuelle Annotation gleicher Gesichtsmerkmale ersetzt.

```bash
python3 tools/motion-art/measure_annotated_contacts.py tools/motion-art/mosslyn-walk-m2.json
python3 -m unittest discover -s tools/motion-art -p test_annotated_contacts.py
```

Die Rechtecke `frames[].feet` benennen Nah- und Fernpfote im normalisierten
gemalten Original, nicht in der Pixelfassung. Die Messung prüft die unterste
Alphakontur pro benannter Pfote. Über drei Pixel Bodenabstand gelten nicht als
Kontakt. Ein fehlender Kontakt wird **nicht** durch das Gegenbein ersetzt.
`measure_walk_contacts.py` ist nur noch die historische M1-Messung und weist
einen anderen Atlas ausdrücklich zurück.

Offen: Nahpfote in Phase 5 noch fünf Pixel über dem Boden; damit fehlt ein
gesicherter Kontaktabschnitt (Phase 4→5). Sieben beobachtbare Abschnitte ergeben
zusammen 203,5px, der ungeprüfte Sohlenweg des achten wäre 29,5px. Auch deren
Summe 233px wäre nur rund 21% länger als die alte 192px-Schätzung und noch
unter der für den ruhigen Takt angesetzten Strecke 285px. Das ist ausdrücklich
kein vollständiger Kontakt- oder Schlupfnachweis. Die beobachtbaren Wege liegen
zwischen 6,5 und 50px pro Phase. Phasendauern deshalb noch nicht scheinpräzise
aus dem lückenhaften Kontaktweg abgeleitet; der Takt bleibt unverändert.

Unter `docs/validation/motion-m2-20261026/` liegen Pixelbogen, 7,2s echte
Spielaufnahme sowie Kontakt- und Aufnahmebericht. Aufnahme mit `WALK_ONLY=1`
zusätzlich zu `MOTION_DRAFT=1`; alle acht Phasen rechts/links erreicht.
Gemessen 2,3543Welt/s und ca.1,066Hz; gleicher Pixelmaßstab über die Folge,
Bildhaltezeiten bei 30fps überwiegend 0,100/0,133s. 244 Godot-Entwurfschecks,
1276 bestehende Mooslyn-Checks unter OpenGL sowie sechs Python-Checks grün.
Die eigentliche Weltaufnahme meldet weiterhin die bereits bekannten Lecks
beim Beenden. Ein frischer vollständiger Editor-Import stürzte in dieser
Umgebung im Vulkan-Initialisierungspfad ab; die zwei neuen Atlanten wurden
separat importiert, die echte Aufnahme und Renderprüfung liefen mit OpenGL.

Nächster Schritt bleibt Mooslyns **Gehen**: Kontaktwechsel zu Phase 5 auf dem
Boden zeichnen, vollständigen Standweg und Abrollen weiter ausarbeiten, dann
erst belastbare Phasendauern ableiten und wieder aufnehmen. Galopp,
Verbindungsphasen, Wenden und Tiefenansichten sind weiterhin offen. Kein
Weiterreichen dieses ungeprüften Maßstabs an andere Figuren und keine APK.
