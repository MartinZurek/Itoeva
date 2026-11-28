# Kartenschmiede · Kampfinszenierung

Die gemeinsame Kampfansicht von Hauptspiel und Kampfarena zeigt die Hand als überlappenden Fächer mit großflächiger Kartenillustration. Beim Ziehen markiert das Spiel gültige Figuren oder freie Helferplätze. Beim Loslassen über der Hand oder außerhalb eines gültigen Ziels gleitet die Karte zurück. Langes Drücken öffnet die vollständigen Kartendetails.

## Auftrag

Martin möchte spürbar unterschiedliche Karten und kurze Beschwörungen nach Art klassischer JRPGs. In der Werkstatt soll der Schnitt in die Arena zeigen, welche Figuren tatsächlich getroffen, geschützt oder beschworen wurden.

## Änderung

- Die Handkarten in den Übungsduellen verwenden Elementfarben, größere Bildausschnitte und ein Filmzeichen für besonders inszenierte Karten.
- Feuerstoß, Frostfaden, Schutz und andere normale Zauber nutzen kurze Farbspuren und zeigen ihre Wirkung an den im Kampfereignis gespeicherten Zielen.
- Phönixflamme, Sternenfall und mythische Helfer erhalten einen gut zwei Sekunden langen Echtzeiteinspieler. Die 3D-Kulisse nutzt bewegtes Licht und Kamerafahrt mit den gemalten Itoeva-Sprites. Die Montage greift die schwebenden Werkstücke aus der früheren Wesenbuch-Vorschau auf.
- Beim Rückschnitt lösen die gespeicherten Treffer und Beschwörungen ihre Wirkung auf dem Feld aus. „Zum Treffer“ springt vor den Einschlag und hält Anzeige, Ton und Aktionssperre synchron.

## Prüfung

Der Godot-UI-Test spielt Feuerstoß gegen einen geschützten Helfer, Vexzelanz, Phönixflamme und Sternenfall. Er prüft die exakten Schild- und LP-Verluste, den richtigen Beschwörungsplatz, die Begrenzung bei voller Reihe, den Sprung per Touch und Sternenfalls eigenen Schutz. Der Mitschnitt läuft mit Godots OpenGL-Renderer auf dem Desktop. Die Android-Datei ist eine signierte Aktualisierung der Kartenschmiede-Vorschau.

## Effekte beim Ausspielen

Hauptspiel, Kampfarena und Beschwörungskino verwenden dieselben Effekte für ausgespielte Karten. Feuer, Frost und andere Elemente haben eigene Trefferformen. Heilung steigt als Lichtkreuze auf, Schutz legt sich als Bogen über das Ziel, und beschworene Helfer erscheinen in einem Zeichen auf ihrem tatsächlichen Feldplatz. Gezogene und zurückgeholte Karten sowie gewonnene Kristalle erhalten eigene Symbole. Bei kombinierten Wirkungen erscheinen die Effekte an den jeweils betroffenen Figuren.

| Kristallkosten | Inszenierung |
| --- | --- |
| 0–1 | Kurzer Impuls mit wenigen Partikeln |
| 2–3 | Zusätzliche Aufladung und mehr Partikel am Ziel |
| 4–5 | Größere Trefferfläche mit Halo und Nachleuchten |
| 6 und mehr | Zweiter Halo, zusätzliche Lichtsplitter und längstes Nachleuchten |

Die kurzen und vollständigen Beschwörungsfilme gehen zum bestehenden Trefferzeitpunkt in die Feldanimation über. Die Anzeige liest Schaden, Heilung und weitere bestätigte Änderungen aus dem Kampfereignis. `tests/card_effects_play.gd` prüft die sichtbaren Zeichen für alle 737 Kartenprofile, die vier Kristallstufen und ausgewählte Karten mit ihren Auswirkungen auf den Kampfzustand.
