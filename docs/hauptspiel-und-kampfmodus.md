# Hauptspiel und Kampfmodus

Das Hauptspiel startet in `scenes/gameplay.tscn`, der Kampfmodus in `scenes/duel_lab.tscn`. Beide verwenden dieselbe Kampfansicht `scripts/card_view.gd`. Im Kampfmodus öffnet **Regionale Arenen** die regionalen Kämpfe mit dem gewählten Deck und einem eigenen Speicherstand.

## Weiterarbeiten und exportieren

Godot 4.6.3 verwenden. In den Exporteinstellungen sind **Itoeva Main Game** und **Itoeva Battle Mode** als getrennte Android-Profile eingerichtet. Für Android-Exporte werden die passenden Godot-Exportvorlagen, das Android SDK, Java und ein lokaler Signierschlüssel benötigt.

Die 15 Illustrationen der Gameplay-APK liegen in `assets/travel/`. Die Ladeansicht wechselt bei wiederholten Besuchen zwischen den beiden Varianten eines Schauplatzes. Die Übersicht `places-concepts.png` liefert die Ausschnitte für den Reiseatlas.

```sh
godot --headless --editor --import
godot --headless --fixed-fps 60 --script res://tests/mode_switch.gd
godot --headless --fixed-fps 60 --script res://tests/gameplay_play.gd
```
