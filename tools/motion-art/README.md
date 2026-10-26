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
python3 tools/motion-art/prepare_cycle.py tools/motion-art/mosslyn-gallop-8.json tools/motion-art/mosslyn-walk-8.json
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

## Offene Taktentscheidung vor der nächsten Bewegungskorrektur

`python3 tools/motion-art/measure_walk_contacts.py` misst den sichtbaren
Sohlenumriss im gemalten Original. Ergebnis in `contact-estimate.json` neben
dem Video: Nahbeinstand 91px, Fernbeinstand101px, zusammen etwa192px pro
Zyklus statt der bislang angenommenen285px. Im Außenmaßstab sind das etwa
1,488 statt2,208Welteinheiten. Sohlenmitten sind nur eine Näherung für echte
Druckpunkte; die Differenz ist dennoch deutlich. Der letzte Phasenwechsel hat
gar keinen erkennbaren Standpfotenrückweg, andere Intervalle16–44px.

Bei unverändert2,35Welt/s erfordert die geschätzte gezeichnete Strecke etwa
1,58Gehzyklen/s statt des aktuellen Entwurfstakts1,06Hz. Gemäß Martins
Auftrag vor der Umstellung entscheiden lassen:

1. Höheren Geh-Takt verwenden (ca.1,58Hz), Kontaktphasen entsprechend gewichten
   und die fehlerhafte Abschlussphase nachzeichnen.
2. Den ruhigeren Takt erhalten und die gezeichnete Schrittlänge um etwa48Prozent
   vergrößern; anschließend Kontaktphasen und Gegenbein erneut prüfen.

Spieltempo nicht pauschal senken. Der bisherige Entwurf bleibt ausschließlich
in MOTION_DRAFT=1 sichtbar; keine vorweggenommene Taktentscheidung. Die Front-
und Rückansicht sowie weitere Wesen hängen am noch zu bestätigenden Maßstab.
