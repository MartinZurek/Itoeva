# Originalvolumen der weiteren Spieler (A3)

`python3 tools/companions-3d/build.py` erzeugt zwoelf geschlossene GLB-Volumen:
Nocti, Starlit, Wyrmling und Noctis Flugform, jeweils Seite/Vorne/Hinten.
Abhaengigkeiten: numpy, scipy, Pillow. Quelle sind die registrierten Original-
Bewegungsatlanten, ihre Motivmasken und die vorhandenen Koerper-/Schrittanker.
Keine neue Concept-Art und keine Neuzeichnung von Gesicht, Fell oder Schmuck.

Wie bei Mooslyn A2 liegen Originalfarben auf einem gewoelbten Volumen mit echtem
GLTF-Skelett. Die anderen Ansichten sind eigene Projektionsvolumen, kein frei
betrachtbares Modell. Jede Netzkante hat zwei Dreiecksnachbarn; Tiefe, endliche
Koordinaten und normalisierte Gelenkgewichte werden beim Bau geprueft.
Je Ansicht 20.324 bis 34.156 Dreiecke. `data/companion_volumes.json` nennt Quellen,
Pruefsummen, Geometriegroesse und Grenzen. Mesh und eingebettete Farbe sind GLB.

`companion_volume.gd` benutzt den bestehenden Ganzkoerper-Bewegungsplan fuer das
importierte Skelett. Der Stern hat nur Koerper und Schal, keine Fuss-/Beingelenke:
kurze Kompression, Abheben, langer Schwebebogen, Landung. Schatten und Eigenlicht
laufen weiterhin durch den bestehenden Weltvertrag. Noctis Flugmodell besitzt
keine Laufbeingelenke. Sonderaktionen bleiben der bisherige Zeichnungspfad.

Tests: `tests/companion_volume.gd` (alle Ansichten, 32 Phasen, Sprint/Flug,
Sternbogen, endliche Gelenke, Volumentiefe, Abschalten/Sonderaktionen),
`tests/companion_volume_world.gd` (echte Einstellungen, Save, alle drei Spieler).
Mit OpenGL werden Bilder unter `captures/companions-volume` erzeugt.

Grenzen: keine freie Kamera, kein weltweit verriegelter Fusskontakt, keine
vollstaendig anatomisch getrennten Gliedmassen. Hueft-/Pfotenuebergaenge bleiben
verbesserungsbeduerftig. Ansichtswechsel wechseln zwischen Volumen. Android-
Leistung und menschliche Bewegungsabnahme sind durch PC-Tests nicht belegt.
