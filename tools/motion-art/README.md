# Mooslyn: gemalte Bewegungsentwürfe, 26.10.2026

**Aktuell: M3 mit zehn Galoppphasen und gezeichneten Gangwechseln**, siehe den
gleichnamigen Abschnitt am Ende. M1/M2 darunter dokumentieren die Vorgeschichte.

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
python3 tools/motion-art/prepare_cycle.py tools/motion-art/mosslyn-walk-m3.json tools/motion-art/mosslyn-gallop-m3.json tools/motion-art/mosslyn-lower-m3.json tools/motion-art/mosslyn-rise-m3.json
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

## Aktuell M3: zehn Galoppphasen und gezeichnete Gangwechsel

M3 setzt M2 am selben Branch fort. Martin beauftragte zusätzlich den
Vierbeiner-Galopp und seine Übergänge und ließ ausdrücklich die Wahl einer kurzen
Testanimation statt einer APK. Gewählt wurde die echte Spielaufnahme.

- `mosslyn-gallop-m3.json`: zehn gemalte Phasen. Acht neue Grundphasen, gezielte
  Korrektur des Hinterbeinabstoßes und zwei zusätzliche gezeichnete Stützphasen.
  Vorderstütze, Sammeln/Hinterstütze, Abstoß, Streckflug und Landungsanflug.
- `mosslyn-lower-m3.json` / `mosslyn-rise-m3.json`: jeweils vier Verbindungsposen.
  Neu gezeichnete Zwischenhaltungen, Endposen exakt aus Geh-/Galoppquellen.
  Dazu kann `prepare_cycle.py` einzelne Alphakomponenten aus weiteren Originalen
  übernehmen. Kein Bildmorphing, keine Gliedmaßen- oder Gesichtsdehnung.
- `mosslyn-walk-m3.json`: die M2-Gehzeichnungen unverändert, nur separate
  M3-Atlanten für die gemeinsame Palette und Pixeldichte. M1/M2 bleiben erhalten.
  Die bekannten Gehkontaktfehler aus M2 gelten weiter.
- Alle vier Folgen teilen Kopfregistrierung42,5px, Referenzhöhe208,046px,
  Bodenlinie300 und feste320er-Zellen. Der zuvor größere Galopp-Kopf wird
  nicht durch wechselnde Laufzeitskalierung ausgeglichen.
- Pixelpass für diese vier Atlanten gemeinsam mit `--reference-height 221`:
  dieselbe Palette und Pixeldichte auch beim abgesenkten Körper. Die Endpunkte
  sind im gemalten Original **und** in der Pixelfassung bytegleich als Kacheln.
- `authored_motion_transition.gd` wechselt die Darstellung über nominell0,28s.
  Beim langsamer Weitergehen erst die passende Vorderstütze erreichen. Direktes
  Stoppen richtet auch ohne Wegfortschritt auf. Umkehr eines kurzen Impulses
  nimmt die entsprechende Gegenhaltung, statt an einer Endpose neu anzufangen.
  Sprung, Sitzen, Rollen und Tiefenbewegung brechen die Folge ab.
- Galopp-Zyklus240px (ca.1,86Welt,2,07Hz bei3,85Welt/s). Dauergewichte aus den
  sichtbaren Stütz-Sohlenwegen, Mindestdauer ca.1/30s; verbleibende Zeit auf
  freie beziehungsweise ungeprüfte Wechselphasen verteilt. Sichtbare Stützen
  sind keine vier einzeln verifizierten Druckpunkte. Restabweichungen sind im
  Kontaktbericht ausgewiesen; **kein Nachweis vollständig schlupffreier Pfoten**.
- Spieltempo, Geh-Takt, Kollisions-/Ausdauerlogik und Standarddarstellung bleiben.
  M3 ausschließlich mit `MOTION_DRAFT=1`. Keine Version/Workflow geändert, kein
  Merge und keine APK.

Originalgenerierungen und genaue Prompts liegen in `sources/`. Eine verworfene
achtphasige Zwischenaufnahme wurde durch die zehnphasige Endaufnahme ersetzt.
Die Aufnahmedateien unter `docs/validation/motion-m3-20261026/` sind333 echte
Weltbilder bei30fps,11,1s: Gehen, Galopp, zurück zum Gehen, beide Richtungen,
erneuter Galopp und direktes Loslassen. `CLEAN_VIEW=1` blendet ausschließlich
die HUD-Ebene für die Bewegungsbeurteilung aus. Der Detailfilm ist ein fester,
dreifach vergrößerter Ausschnitt derselben Bilder ohne zeitliche Interpolation.

```bash
# Nach dem prepare_cycle-Aufruf oben: nur die vier M3-Atlanten neu pixeln.
M3_PIXEL_DIR=$(mktemp -d)
cp prototypes/godot-hd2d/assets/characters/mosslyn-{walk,gallop,lower,rise}-m3.png "$M3_PIXEL_DIR/"
python3 tools/pixel-pass/pixel_pass.py "$M3_PIXEL_DIR" --species mosslyn --reference-height 221
cp "$M3_PIXEL_DIR"/pixel/*.png prototypes/godot-hd2d/assets/characters/pixel/
cp "$M3_PIXEL_DIR"/pixel/pixel_pass.json tools/motion-art/mosslyn-m3-pixelpass.json

# Godot mit OpenGL und echter Anzeige, bei Bedarf über Xvfb:
MOTION_DRAFT=1 MOTION_CHAIN=1 CLEAN_VIEW=1 TAG=m3-final godot --path prototypes/godot-hd2d --fixed-fps 30 --rendering-method gl_compatibility --script preview_pixel/play_capture.gd
python3 tools/motion-art/review_m3_capture.py prototypes/godot-hd2d/captures/play_m3-final docs/validation/motion-m3-20261026
python3 tools/motion-art/measure_annotated_contacts.py tools/motion-art/mosslyn-gallop-m3.json
```

Prüfungen:272 zyklische Godot-Checks,126 Zustandswechsel-Checks,160 Actor-
Integrationschecks,1276 bestehende Mooslyn-Checks unter OpenGL und acht
Python-Tests grün. Die echte Aufnahme erreicht sämtliche zehn Galoppphasen
und vier Phasen beider Übergänge jeweils rechts/links bei konstantem Maßstab.
Die bekannten ObjectDB-/Ressourcenlecks beim Beenden des Weltlaufs bestehen.
Das ist keine vollständige erneute CI- oder Telefonabnahme.

Weiter offen: M2-Gehkontaktwechsel und vollständige Gehschrittlänge, vier
einzeln belastbare Galopp-Druckpunkte, neu gezeichnete Wendefolgen sowie
Front-/Rückansichten. Weitere Figuren wurden nicht umgestellt. Keine solche
offene Arbeit allein aus dem Vorhandensein dieser Vorschau als erledigt ansehen.

## M4: Größenabgleich und weitere Spielfiguren (27.10.2026)

Martin meldet einen Größenwechsel beim Gehen/Galopp und beauftragt danach
Nocti, Starlight und Wyrmling im gleichen gemalten Arbeitsweg. Die bisherige
Wartebedingung auf Mooslyns vollständige Kontaktabnahme ist durch diesen
konkreten Folgeauftrag überholt; die offenen Kontaktprüfungen bleiben offen.

M4 ist separat mit `MOTION_M4=1` aktiviert. Die ausgelieferte APK 0.15.7 und
M3-Dateien bleiben als Vergleich erhalten. Neue Quellen/Prompts unter `sources/`,
Annotationen `*-m4.json`, Masken/Metadaten in `data/*_cycles.json`.

- Mooslyn: voller gezeichneter Galopprumpf, neue Zwischenzeichnungen; die
  Nahiris dient als gemeinsames Größenmerkmal. Ruhe hält dieselbe gezeichnete
  Kontaktpose wie der Gehbeginn (noch keine neue entspannte Ruheanimation).
  Übergangsenden sind einschließlich Pixelpass identisch; Aufrichten kehrt die
  beiden passenden gemalten Zwischenhaltungen um. Takt bleibt durch Umrechnung
  der Pixel-Zyklusstrecke auf den neuen Offline-Maßstab wie M3.
- Nocti: vier Abhebe-, acht Flug- und vier Landephasen, Füße im Flug angezogen.
  Flügelschlag folgt der Zeit, langsamer/schneller Flug teilt dieselben Bilder.
  Flugregistrierung an der Kopfmitte, nicht der untersten Flügelspitze.
- Starlight (`starlet`): acht langsame und acht schnellere Hüpf-/Schwebephasen,
  keine Beine. Registriert wird die Höhe des goldenen Sternkörpers, nicht das
  wechselnde Auge. Über 65% der Zyklusdauer liegen in den freien Phasen.
  Bei Loslassen wird der Hüpfer darstellerisch beendet, ohne weiteren Weltweg.
- Wyrmling: acht Geh- und acht Laufphasen auf vier Beinen, Schwanz/Flügel
  mitgezeichnet. Geschwindigkeit wechselt nur an der gemeinsamen Kontaktphase.
- `prepare_cycle.py --output ...` erzeugt weitere Metadatendateien ohne M3 zu
  überschreiben. Optionale Kopf-Y-Registrierung hält Noctis Flugrumpf ruhig.
- Pixelpass zusätzlich `--cell-size 320` (Mooslyn) bzw. `448` (andere), damit
  identische Anschlusszeichnungen nicht durch Rundung im Atlasraster wandern.
  Gemeinsame Palette und fester Pixelmaßstab pro Figur; Parameter in
  `*-m4-pixelpass.json`. Original-Metadaten beziehen sich weiter auf Gemaltes.

Dieser Quell-/Integrationscheckpoint ist **noch keine optische Abnahme**.
Vier Python-Assettests bestanden. Die neue Zustandsprüfung benötigt zusätzlich
fehlerfreie Texturimporte und echte Aufnahmen. Vorder-/Rückansichten verwenden
noch die vorhandenen Bildbögen. Einzelne anatomisch belegte Pfotendruckpunkte,
Standwege/Schlupf und Kontaktgewichte der neuen Gallopbilder sind noch zu prüfen.
Kein APK-Bau und keine Behauptung schlupffreier Bewegung.
