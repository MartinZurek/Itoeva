# Mooslyn: Groesse und Bildanker in 0.15.2

Martin meldet nach dem Telefontest von0.15.1 sichtbares Wachsen/Schrumpfen
innerhalb der 28 gemalten Geh-/Sprintphasen und unpassende Bilduebergaenge.

## Ursache

Eine konstante Skalierung pro Atlas garantiert bei unterschiedlich gross
gemalten Figuren keine konstante Figurengroesse. Die Sprintkonturen reichen
von199 bis239 Quellpixeln. Ein Vergleich derselben Gesicht-/Ohrregion ergab
innerhalb der Sprintfolge rund15,5Prozent Groessenspanne. Zusaetzlich wandert
die Zeichnung innerhalb ihrer Rasterzellen seitlich.

## Korrektur

Alle56 Seitenbilder werden anhand der Gesicht-/Ohrregion gleichmaessig skaliert.
Die vorhandene seitliche Standzeichnung liefert die gemeinsame Referenz fuer
Gehen, Sprint und Anhalten. Ein gemessener horizontaler Bildanker gleicht die
verschobenen Zellpositionen aus. Linksbewegung spiegelt den gesamten Versatz.
Lichtanker und Bildschirmbegrenzung verwenden denselben korrigierten Ursprung.

Die Gesamt-Silhouettenhoehe wird nicht festgeklemmt: Kniebeugung und Lauflage
duerfen sich weiterhin aendern. Es gibt keine unterschiedliche X-/Y-Streckung,
keine neuen PNGs und keine zweite Verformung ueber das Hautnetz. Front/Ruecken
verwenden die bisherigen Ansichten. Weltgeschwindigkeit, Ausdauer und Saves
bleiben unveraendert.

`tools/register_mosslyn_size.py` erzeugt Messdaten und Konfidenzen offline aus
den Originalen (numpy, Pillow, opencv-python-headless); OpenCV ist keine
Spiel-/Android-Abhaengigkeit. Gesicht/Ohr werden ohne Beine und Umhang verglichen.
Die Python-Pruefung gleicht die gespeicherten Messrechtecke unabhaengig mit
den tatsaechlichen Originalpixeln ab.

## Validierung und Grenzen

- Python-Atlas-/Groessenvertrag:2Tests erfolgreich.
- Godot-Charakterpruefung869/0 und Ausdauer204/0 erfolgreich.
- Echte OpenGL-Aufnahmen aller56Phasen in beiden Richtungen erfolgreich;
  aktive Gangpruefung aller vier Startfiguren3442/0 erfolgreich.
- Neue Regressionen pruefen gleiche Kopf-Groessenreferenz, seitlichen Bildanker
  und die Rueckkehr zur Standzeichnung ohne alten Versatz.
- Ein zusaetzlicher Kopfvergleich der echten Renderbilder zeigt noch kleine
  gemalte Unterschiede; die Registrierung ist keine anatomische Neuzeichnung.
  Bein-/Armformen und Kontaktphasen bleiben eigenstaendige visuelle Abnahmepunkte.
- Desktop-OpenGL/Mesa ist keine Telefon-GPU-/FPS-Messung. Telefonabnahme offen.

## APK und Wiederaufnahme

AndroidTest0.15.2/Code32, bestehendes Paket, Zertifikat und Drive-Ziel.
Der APK-Workflow prueft das neue549-Dateien-Manifest vor allen Spiel-/Renderchecks,
Export und Upload. Bis zum erfolgreichen Lauf keine neue APK als geliefert werten.
Ruecksetzen: diesen isolierten Korrekturcommit zuruecknehmen; kein Savewechsel.
