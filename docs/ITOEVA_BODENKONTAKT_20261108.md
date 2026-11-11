# Itoeva – Staub und Sandspuren

Stand 08.11.2026. Basis ist der Quellbaum von 0.19.1, Originalrevision
`3428d0cb75649771b66d60ddc768cc87a7497641`, Baum
`a4d119f03f3e9459b901c1092709bd9dc5fe68dd`.

Der bisherige Effekt greift bei langsamem Touch-Gehen auf Erde unterhalb von
1,2 Welteinheiten pro Sekunde nicht. Auf Sand werden Abdrücke durch nachfolgende
Staubstöße im gemeinsamen Ring überschrieben. Außerdem lagen die schwachen
Sandmarken farblich nahe an der gemalten Textur. Die Skalierung im Weltkoordinatensystem
stauchte die Marke beim Drehen seitwärts.

- Mooslyns Hufe und Wyrmlings Krallen zeichnen auf Sand klarere, lokal gedrehte
  Abdrücke mit einer hellen Kante. Der Abstand zur Körpermitte richtet sich nach
  der Bewegungsrichtung und der wechselnden Stützseite.
- Erde und Sand geben ab 0,55 Welteinheiten pro Sekunde einen kurzen Staubstoß
  am Kontaktpunkt ab. Farbe und Intensität richten sich nach Boden und Tempo.
  Der Staub steigt leicht auf und blendet in 0,62 Sekunden aus.
- Das vorhandene MultiMesh reserviert acht seiner zwölf Plätze für Abdrücke und
  vier für Staub. Ein Staubstoß kann eine Spur nicht mehr überschreiben. Abdrücke
  verblassen abhängig von der Belegung nach bis zu sechs Sekunden.
- Die Material- und Laufprobe sitzt am tatsächlichen Punkt der Spur. Holz, Stein
  und Luftphasen erzeugen keine Abdrücke. Der Effekt verwendet vorhandene Geometrie
  und braucht keine neuen Bilddateien.

Nur `scripts/ground_steps.gd` und `shaders/mud_steps.gdshader` ändern das Spiel.
Die Tests rufen die normale Spielfigur, Laufbewegung und Kulisse auf. Der Patch
berührt keine gemeinsam genutzte Kampfszene, Projektkonfiguration oder Videodatei.

## Prüfen

Die ursprüngliche Prüfung nutzte Godot 4.5.1. Im Kartenschmiede-Branch liegt
das Projekt direkt im Hauptverzeichnis. Von dort die folgenden Befehle ausführen.

```sh
godot --headless --fixed-fps 60 --script res://tests/ground_contact_play.gd
godot --audio-driver Dummy --fixed-fps 60 --script res://tests/ground_contact_play.gd -- /pfad/zum/ausgabeordner
```

OpenGL-Aufnahmen mit eingeblendeter und ausgeblendeter Bodenschicht vergleichen
denselben Spielmoment. Die Gerätedarstellung und Android-Bildrate müssen noch
am Handy geprüft werden. Der visuelle Effekt ändert keine Spielstandsdaten.
