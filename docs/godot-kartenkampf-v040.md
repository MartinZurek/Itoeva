# Itoeva: Kartenkampf v0.4.0

Der Kampf gegen Nachtwurz verwendet jetzt Charakterdecks. Diese erste spielbare
Fassung setzt den ausdrücklich gewünschten Wechsel vom bisherigen Kampfsystem um.
Die gemalte Welt, Figuren und bestehenden Effekte werden weiterverwendet.

## Spielen

Im Hauptmenü ein Wesen wählen und „Deck bauen“ öffnen. Vier gemeinsame und drei
eigene Karten sind verfügbar; das Deck enthält 8 bis 12 Karten, höchstens zwei
Exemplare pro Karte. Das Startdeck enthält 12. Gespeicherte Änderungen gelten
beim nächsten Kampf oder Wiederholungsversuch, nicht mitten im laufenden Kampf.
Auch Starlets Begleiterdeck kann durch Auswahl im Hauptmenü bearbeitet werden.

Auf der dunklen Lichtung beginnt der Kampf. Hauptfigur, Begleiter und Nachtwurz
ziehen nacheinander. Jeder besitzt eine eigene Hand und ein eigenes Budget.
Zu Beginn gibt es drei Handkarten; jeder eigene Zug zieht eine weitere Karte.
Eine volle Hand mit fünf Karten legt neue Karten ab. Ein leeres Deck zieht nichts
mehr nach; in dieser Fassung gibt es noch keinen Erschöpfungsschaden.

Runde 1 beginnt mit einem Kristall. Jede neue Runde erhöht die Kapazität um eins,
bis maximal zehn; beim eigenen Zug wird das Budget aufgefüllt. Karten verbrauchen
Kristalle und beenden den Zug nicht. „Zug beenden“ gibt an die nächste Figur weiter.
Schadenskarten antippen, anschließend Nachtwurz oder einen feindlichen Helfer wählen.
Heilung und kleinere Barrieren wirken auf die spielende Figur; sie benötigen kein Ziel.

## Superkräfte und Helfer

Eine kostenlose Superkraft pro eigenem Zug, unabhängig von Kristallen. Die zwei
Schaltflächen teilen sich diese Nutzung. Fennecs Wildruf beschwört zufällig Waldkatze
(35 %), Wildschwein (30 %), Silberwolf (25 %) oder Waldgorilla (10 %). Stärke/LP:
3/6, 5/10, 9/14, 12/18. Das Tier bleibt als Helfer erhalten. Starlet wählt zwischen
Lichtstrahl (30 Schaden gegen Nachtwurz) und einer Barriere mit bis zu 40 Schutzpunkten
für beide lebenden Figuren. Andere Wesen behalten ihre persönlichen Element- und
Unterstützungskräfte. Ein Windstoß kann Nachtwurz zum Wanken bringen, Feuer brennt
nach, Wasser heilt und Mondlicht schwächt.

Jede Figur kann bis zu drei Helfer besitzen. Neu beschworene Helfer greifen erst im
nächsten eigenen Zug an. Bereiten Helfer antippen und Ziel wählen: ein Angriff pro
Zug, bei Helfer gegen Helfer gleichzeitiger Gegenschaden. Goldene Schutzhelfer müssen
vor normalen Helferangriffen auf die geschützte Seite besiegt werden. Zauberkarten
umgehen diese Zielpflicht. Schutzhelfer der Gruppe fangen auch Nachtwurz' Treffer ab,
ohne überschüssigen Schaden weiterzugeben. Danach absorbiert eine Lichtbarriere
Schaden vor den LP; verbleibender Schaden trifft die Figur.

Nachtwurz besitzt 220 LP, ein eigenes vereinfachtes Deck, beschwört Dornenhelfer,
schießt Splitter und wirkt in Runde 4 und deren Vielfachen Wurzelbann. Bindung und
Verstummen sperren Superkräfte. „Fesseln lösen“ entfernt beides und verbraucht die
Superkraft dieses Zuges; Karten und Helfer bleiben spielbar. Bindung/Verstummen
laufen auch am Zugende aus. Gegner-KI spielt höchstens eine bezahlbare Karte pro
Zug, nutzt ihre Waldattacke und greift mit bereits bereiten Helfern an.

## Abschluss, Speichern und Grenzen

Letzter Treffer → Animation → Siegesergebnis → „Weiter in den Wald“. Beide Figuren
erhalten je 120 XP; ein Heiltrank und ein Mondsplitter wandern in den lokalen Vorrat.
Charakterreaktion und XP-Zähler erscheinen im Siegesbildschirm. Erfahrung und Beute
werden einmal pro tatsächlich gewonnenem Kampf vergeben. Heiltränke werden in dieser
Kartenfassung als Vorrat gesammelt; sie sind noch kein separat spielbarer Inventargegenstand.

Der vorhandene lokale Spielstand speichert zusätzlich die eigenen Decks und den
Kampfzustand einschließlich Hände, Ablagen, Kristalle, Helfer, Superkraft und anstehendem
Zugwechsel. Menü oder Laden setzt die Animation fort, ohne Treffer oder Kosten neu
anzuwenden. Frühere abgeschlossene Kämpfe bleiben abgeschlossen. Frühere laufende
Kämpfe übernehmen ihre LP und erhalten für den neuen Kartenmodus Startdecks.

Dies ist ein lokaler Prototyp: keine Booster, Sammelwirtschaft, Kartentausch, Onlinekämpfe
oder automatische Deckoptimierung. Bild- und Filmabnahme sowie Touch-Ereignisse erfolgen
im Desktop-Godot. Installation, Fingerbedienung, Leistung und Spielbalance auf einem
echten Androidgerät bleiben ungemessen. APK: arm64-v8a, Godot 4.5.1, AndroidTest
0.4.0/14, Paket `com.itoeva.godot.hd2d.worldtest`, bestehende Testsignatur.

## Architektur und Rücksetzung

Die Abnahme umfasst 213 erfolgreiche Assertions (73 Modell, 15 Regeln, 53 UI,
72 Siegesablauf) und sechs vollständige gewonnene Kartenkämpfe mit 220 Gegner-LP.
Zusätzlich besteht die Wald-/Weltregression 16525 Checks einschließlich Auslösen,
Speichern, Sieg, Wiederholung und Rückzug. Die Filmvorschau zeigt vorbereitete
Spielsituationen, mit Godot gerendert; sie zeigt keine Android-Geräteaufnahme.
Frühere Film-/Bildskripte dokumentieren die älteren Kampffassungen; für v0.4.0
gelten `card_capture.gd` und `card_movie.gd`.
Prüfskripte: `tests/card_model.gd`, `card_rules.gd`, `card_ui.gd`,
`victory_flow.gd`, `card_playthrough.gd`. Headless-Aufruf ab Projektordner:
`godot --headless --path . --script res://tests/card_model.gd`.
`card_capture.gd` und `card_movie.gd` benötigen einen echten Rendering-Viewport;
die Abnahme nutzt Godot/OpenGL mit Software-Rendering auf Linux.

`card_catalog.gd` enthält Karten und Deckregeln, `card_battle.gd` den Zustand,
`card_encounter.gd` die Weltanbindung, `card_view.gd` die native Touchoberfläche,
`card_effects.gd` die Wald- und Lichtanimationen und `deck_editor.gd` den Editor.
Der alte Kampf bleibt als Basisklasse erhalten. `card_schema=1` ergänzt das bisherige
Speicherformat. Der Branch `feature/card-combat-v040` enthält die Änderung; durch
Rückkehr zum ausgelieferten Basisstand lässt sich v0.3.0 wiederherstellen. Für Android
ist das ein Versionsdowngrade und benötigt gesonderte Installation bzw. Datensicherung.
