# Itoeva Kartenschmiede

Die Kartenschmiede ist die eigenständige Android-Testanwendung für Deckbau und KI-Duelle. Sie nutzt denselben Katalog, die Kartenregeln, Heldenfähigkeiten und die Kampfvorschau wie Itoeva.

## Spielen

1. Mooslyn, Nocti, Starlight oder Wyrmling wählen.
2. Deck A, B oder C auswählen und **Deck bearbeiten** öffnen. Der für diese Figur zugelassene Kartenpool ist direkt verfügbar. Zwei Kopien je gewöhnlicher Karte und eine je Sonderkarte bleiben das Decklimit.
3. Ein Deck mit 40 Karten speichern und einen der drei KI-Gegner auf **Taktik** oder **Meisterprüfung** auswählen.

Deckplätze und Toneinstellungen werden lokal gespeichert. Jedes begonnene Duell verwendet eine Kopie des aktiven Decks. Die App hat die Android-Paketkennung `com.itoeva.kartenschmiede.lab` und verwendet ihren eigenen Spielstand.

## Kampfarena in Version 1.0.1

Der Aschenläufer nutzt sechs eigene Kampfausschnitte aus dem Spritebogen. Anlauf, Anschleichen, Sprung, Krallenhieb und Rückbewegung folgen dem bestehenden Angriff. Die Animation richtet sich nach dem wirklichen Ziel der Schadensberechnung. Fängt ein Schutzhelfer den Treffer ab, landet der Kralleneffekt bei ihm. Im Übungsduell dauert der Aschenbiss 2,86 Sekunden und der Glutsprung 3,15 Sekunden.

Die Aufstellung folgt der begehbaren Fläche der gemalten Lichtung. Helfer wachsen im Vordergrund leicht mit der Perspektive, stehen mit ihren Kontaktschatten auf dem Boden und werden anhand ihres Bodenkontakts in der Tiefe sortiert. Gegnerhelfer wenden ihre Sprite-Ausrichtung dem Spieler zu. Die Arena und die Figuren verwenden im Querformat denselben Ausschnitt innerhalb der sicheren Bildschirmfläche.

## Entwicklungsstart und Export

```sh
godot --path . res://scenes/duel_lab.tscn
godot --headless --path . --script res://tests/duel_lab_play.gd
python3 tools/stage_duel_lab.py /tmp/itoeva-lab-export
godot --headless --editor --path /tmp/itoeva-lab-export --import
godot --headless --path /tmp/itoeva-lab-export --export-release Kartenschmiede /tmp/itoeva-lab-unsigned.apk
```

Der Export enthält die Kampf-UI und ihre Assets, Kartenillustrationen und Kampfmusik. Der Staging-Schritt komprimiert große Texturen für die APK. Texturen mit festen Sprite-Koordinaten behalten ihre Abmessungen.

Beim Export mit Godot 4.6.3 behebt `tools/fix_duel_lab_android.py` die doppelte Provider-Kennung und ergänzt das fehlende thematische Icon aus der Android-Exportvorlage. Der Aufruf erhält die unsignierte APK, die zugehörige Vorlage `android_release.apk` und den Pfad für die korrigierte APK. Anschließend die APK mit `zipalign -P 16 -f 4` ausrichten, mit dem Kartenschmiede-Testschlüssel signieren und mit `apksigner verify` prüfen. Der Export ist für ARM64 und Android ab API 24 konfiguriert.
