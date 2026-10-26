# Mooslyn: gemalte Bewegungsentwürfe, 26.10.2026

Basis: `claude/hd2d-pixel-sprites`, Commit `cb1c2b5`.
Arbeitsbranch: `codex/hd2d-motion-phases-20261026`.

**Zwischenstand, keine Bewegungsabnahme und keine APK-Freigabe.**
Der erste gesicherte Schnitt enthält den achtphasigen Galoppentwurf und seine
reproduzierbare Raster-/Alphaaufbereitung. Die Gehversuche wiederholten zu oft
dieselbe Beinstellung; sie werden nicht als fertiger Gehzyklus eingesetzt.

`sources/mosslyn-gallop-8.png` ist die unveränderte Bildgenerierung nach der
freigegebenen Moosling-Vorlage. `mosslyn-gallop-8.json` hält die gemessenen
Gesichtsregionen und die vorläufig annotierten Flugabstände fest.

```bash
python3 tools/motion-art/prepare_cycle.py tools/motion-art/mosslyn-gallop-8.json
python3 tools/pixel-pass/pixel_pass.py prototypes/godot-hd2d/assets/characters --species mosslyn
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p test_authored_cycles.py
```

Die Aufbereitung trennt ganze Alphakomponenten (auch bei Überschreiten der
Quellrastergrenze), registriert den Kopf einmalig und schreibt feste 256er-Zellen.
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
