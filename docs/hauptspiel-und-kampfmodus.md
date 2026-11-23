# Hauptspiel und Kampfmodus

Das Hauptspiel startet in `scenes/gameplay.tscn`, der Kampfmodus in `scenes/duel_lab.tscn`. Beide verwenden dieselbe Kampfansicht `scripts/card_view.gd`. Im Kampfmodus öffnet **Regionale Arenen** die regionalen Kämpfe mit dem gewählten Deck und einem eigenen Speicherstand.

## Hauptfigur

Mooslyn führt die Geschichte und die Deckauswahl in beiden Modi. Bestehende Mooslyn-Spielstände werden fortgesetzt. Ältere Fortschritte und Decks anderer Figuren bleiben gespeichert. Begegnungen mit einem Zweierteam verwenden weiterhin den vorhandenen Begleitplatz. Die Wahl und Rekrutierung von Begleitern im Spiel sind für einen späteren Schritt vorgesehen.

## Reiseatlas und Ladebilder

Die Weltkarte und der Hausplan zeigen besuchte Orte, deren Bilder und ihre Verbindungen. Entdeckungen werden im bestehenden Spielstand gespeichert. Ein Tipp oder Klick auf ein Bild öffnet die größere Ansicht. Routen im Atlas verwenden besuchte, begehbare Wege.

Die Ladeansicht nutzt die Originalgrafiken aus dem Spiel. Für Strand, Sonnenhain, Nebelwacht, Mühlenbach, Aschenstieg, Siegelhof und Relaisgarten gibt es jeweils zwei Ladebilder mit 1672 × 941 Pixeln. Die übrigen Orte der Außenwelt laden ihre Panoramen mit 2048 × 682 Pixeln. Die sieben Waldabschnitte nutzen ihre eigenen Bilder in voller Auflösung. Bad, Schreibtisch, Küche, Leseecke, Werkstatt, Laden, Café, Arbeitsplatz und Spielhalle haben in diesem Quellstand 480 × 270 Pixel als Originalauflösung. Die Ansicht begrenzt dort die Vergrößerung.

## Weiterarbeiten und exportieren

Godot 4.6.3 verwenden. In den Exporteinstellungen sind **Itoeva Main Game** und **Itoeva Battle Mode** als getrennte Android-Profile eingerichtet. Für Android-Exporte werden die passenden Godot-Exportvorlagen, das Android SDK, Java und ein lokaler Signierschlüssel benötigt.

Die Varianten in `assets/travel/` wechseln bei wiederholten Besuchen. Der Atlas zeigt die erste Variante oder das eigene Ortsbild.

```sh
godot --headless --editor --import
godot --headless --fixed-fps 60 --script res://tests/mode_switch.gd
godot --headless --fixed-fps 60 --script res://tests/gameplay_play.gd
godot --headless --fixed-fps 60 --script res://tests/atlas_route_play.gd
godot --headless --fixed-fps 60 --script res://tests/world_passages_play.gd
```
