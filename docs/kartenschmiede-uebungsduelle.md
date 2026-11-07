# Kartenschmiede · Übungsduelle

## Auftrag

Martin möchte drei spielbare Lernpartien in der Schmiede. Die gewählte Figur tritt allein gegen einen Gegner an. Die Partien verwenden fertige Decks und bieten erfahrenen Kartenspielern eine zusätzliche Schwierigkeit.

## Änderungen

- **Funkenlauf** belohnt frühe Präsenz und erneute Angriffe. Der Aschenläufer setzt mit Glutwelpen und angekündigten Glutsprüngen ein Zeitlimit.
- **Wurzelwacht** trainiert Schutz, günstige Abtäusche und Flächenschaden. Die Schildwacht baut eine Reihe aus Schutzlarven auf.
- **Echo in der Esse** verbindet Lernen, Ziehen und Kristallgewinn. Der Funkenmeister verstärkt überlebende Schmiedelinge. Ein günstiger Abtausch heute kann einen stärkeren Gegnerzug morgen verhindern.
- Jedes Leihdeck enthält 40 Karten. Die gewählte Figur steuert zwei Klassenkarten und ihre Spezialkarte bei. Eine Starthand enthält immer einen Helfer für einen Kristall. Die normalen Charakterkräfte kosten im Duell drei Kristalle.
- Die Meisterprüfung verstärkt Helfer, setzt früher unter Druck und kombiniert ab Runde zwei bestimmte Aufbauten mit einem angekündigten Treffer. Die Zielwahl berücksichtigt Schutz, tödliche Angriffe und günstige Abtäusche.
- Deckwahl, Revanche und Abbruch benutzen nur den Duellzustand. Das Training vergibt keine Belohnung und schreibt keine Änderungen an Decks, Sammlung oder Vorrat.

## Prüfstand

Die Modelltests prüfen je drei Ziehfolgen für alle acht Figuren und drei Decks, Solorunden, Schutz, Kosten, Niederlage bei reinem Passen und eine Revanche ohne Belohnung. Der UI-Test öffnet die Werkbank, wechselt zur Meisterprüfung und prüft das Duell samt Rückkehr und Spielstand. Eine einfache Spielheuristik erprobte je 24 Partien für die vier Startwesen pro Deck und Schwierigkeit. Sie gewann in jeder Paarung mindestens eine Partie. Die Ergebnisse sind ein Anhaltspunkt für die Abstimmung, keine Messung der Schwierigkeit für Menschen.

## Übergabe

Die Änderungen basieren auf dem aus der APK 0.19.1 wiederhergestellten Projekt. Der GitHub-Zweig `codex/npc-enemy-motion-20261027` enthält noch keinen Decktest an der Werkbank und ist daher nicht die Basis dieses Patches. Die Vorschau trägt die Android-Paketkennung `com.itoeva.godot.hd2d.worldtest.kartenschmiede` und nutzt einen separaten Spielstand.
