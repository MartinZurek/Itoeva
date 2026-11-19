# Itoeva Beschwörungskino

Das Beschwörungskino ist eine eigenständige Android-App für die Sichtprüfung der Kartenanimationen. Die Kartenliste enthält seltene, epische, mythische und legendäre Karten sowie Spezialkarten. Filter und Suche öffnen direkt eine Vorschau. Im Kampf führen die Pfeile zur nächsten oder vorigen Karte. **Wiederholen** startet dieselbe Vorschau mit identischer Ausgangslage.

Fünf Karten verwenden ihre bestehende, eigene Inszenierung. Das sind Phönixflamme, Sternenfall, Vexzelanz, Mnotessia und Hexcalia. Die anderen Karten nutzen eine gemeinsame, kurze Enthüllung mit Kartenmotiv und Regeltext. Es folgt jeweils der tatsächlich aufgelöste Karten- und Kampfeffekt.

Die Vorschau stellt die nötigen Ziele, Kristalle, Helfer oder gefallenen Karten bereit. Sie aktiviert vorgesehene Kombos, damit ihre Wirkung sichtbar wird. Der Gegner ist Teil der Kampfszene und wartet nach dem Effekt, damit das Resultat auf dem Feld geprüft werden kann.

```sh
godot --path . res://scenes/summoning_lab.tscn
godot --headless --fixed-fps 60 --path . --script res://tests/summoning_lab_play.gd
python3 tools/stage_duel_lab.py /tmp/itoeva-summoning-export --summoning
```

Der anschließende Android-Export nutzt denselben Ablauf wie die [Kartenschmiede](kartenschmiede-app.md#entwicklungsstart-und-export). Für das Beschwörungskino gilt die eigene Android-Paketkennung `com.itoeva.beschwoerungskino`.
