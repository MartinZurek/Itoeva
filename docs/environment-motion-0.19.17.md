# Umgebungsdynamik · Gameplay 0.19.17

Wolken, Wind, Wasser und lokale Lichtbewegung teilen ihre Funktionen zwischen Erkundung und Kartenkampf. Neue weiche Bewegungsmasken für Sonnenhain, Nebelwacht, Mühlenbach, Aschenaufstieg, Siegelhof und Relaishof begrenzen Wolken auf Himmelsausschnitte und Wind auf Blattfarben. Wege und Gebäude werden nicht pauschal verzerrt.

Laternen bleiben tagsüber sichtbar, Feuer flackert stärker und die Feuerschalen in Aschenaufstieg und Siegelhof verwenden den vorhandenen Flammenatlas. Wasserströmung und Reflexe sind verstärkt. Masken werden einmal erstellt und wiederverwendet; keine zusätzlichen Partikelsysteme.

Validierung: sechs Gebiete in Erkundung und Kampf geprüft, 30 Zuordnungs-/Maskenprüfungen bestanden. Godot 4.6.3 Compatibility gerendert: Vergleichsbilder bei 0 und 4 Sekunden unterscheiden sich, trockener Kontrollpunkt bleibt gleich. Beim Testende meldet Godot noch nicht freigegebene Ressourcen; dies ist keine Messung der mobilen Speicher- oder Bildratenentwicklung. Android-Gerätetest steht aus.

Reproduktion: `godot --headless --path . --script tests/environment_motion_play.gd`. Für Bildvergleiche ohne `--headless` starten und optional `ITO_ENV_CAPTURE_DIR` auf einen vorhandenen Ausgabeordner setzen. Gemeinsame Shader beeinflussen auch ältere Kulissen mit bestehenden Wasser-/Wolkenmasken; neue Windmasken gelten nur für die genannten sechs Gebiete.
