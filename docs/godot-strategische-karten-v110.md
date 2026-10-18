# Strategische Karten v1 – Zwischenstand 18.10.2026

Auftrag von Martin: Karten neu bauen und nach Spielspaß / strategischer Tiefe optimieren.
Basis: Godot-Kampf `6677651` (PR #369). Alle 616 bestehenden K-IDs werden übernommen.
Die Quelldaten bleiben unverändert; `strategy_export.py` erzeugt getrennte Laufzeitwerte.

## Bisher umgesetzt und geprüft

- Laufzeitkatalog: 616 Helfer, 13 Völker, sechs Merkmale und 26 Volks-/Gabenregeln.
- Kosten 1–7 statt 1–10, Körperbudget an den vorhandenen Godot-Kampf angepasst.
- Glück wird zu einem planbaren Reihenfolgebonus. Flackernd gibt einen begrenzten
  ersten Angriff statt zufälligen Schadensschwankungen. Mythische Karten höchstens 1×.
- Die drei Plätze pro Figur bleiben; Motoren haben klare Trigger und Obergrenzen.
- Heimvolk +1 Leben; alle Völker für alle Figuren spielbar. Mosslyn passt zu Garten-
  und Erntewesen, Nocti zu Artefakten und Irrlichtern.
- Drei bewusste Deckvorschläge: Tempo, Bollwerk, Motor; jeweils 40 Karten mit Frühspiel.
- Zug-/Karten-/Lernzähler, Wiederkehr, Wachstum und Schutz werden mitgespeichert.
- Neue Bilder aus reproduzierbaren Vektorbausteinen plus drei gerettete gemalte Urformen.
- Editor mit Suche, Volksfilter, Deckfilter und 18 Karten pro Seite; vollständige
  Regeltexte sind scrollbar. Neue Deckvorschläge ändern nur den ungespeicherten Entwurf.

Godot 4.5.1: 3.139 Strategieregel-Checks und die 15 bestehenden Kartenregel-Checks
bestanden. Jede der 616 Karten wurde ausgespielt, gespeichert und mit ausstehender
Animation wiederhergestellt. Die folgenden Punkte sind noch KEINE Abnahme:
Bildimport/-darstellung, Deckeditor-Berührungstest, komplette Kampfverläufe und
Deckvergleich stehen für diesen Zwischenstand aus.

## Technische Grenzen

Keine Bezahlung, neue Kartensammlung, neue Cloud, Signier-/Workflow-Änderung oder
Android-Veröffentlichung. Die bestehende Testfassung stellt den Katalog wie ihre
bisherigen Karten frei bereit; dieser PR entscheidet keine spätere Erwerbswirtschaft.
Die Faktorquintile steuern weiterhin die Gestalt der Rohwerte; sie werden nicht
als Garantie von Spielbalance behandelt. Ein numerisches Budget ersetzt keinen
menschlichen Spielspaßtest. Bestehende Startdecks werden nicht automatisch ersetzt.

Nächste Schritte: echte UI-/Renderprüfung, deterministische Kämpfe mit mehreren
Deckstilen, eventuelle Balancingkorrekturen und abschließende Dokumentation.
