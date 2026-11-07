# Kartenschmiede · Kampfinszenierung

## Auftrag

Martin möchte spürbar unterschiedliche Karten und kurze Beschwörungen nach Art klassischer JRPGs. In der Werkstatt soll der Schnitt in die Arena zeigen, welche Figuren tatsächlich getroffen, geschützt oder beschworen wurden.

## Änderung

- Die Handkarten in den Übungsduellen verwenden Elementfarben, größere Bildausschnitte und ein Filmzeichen für besonders inszenierte Karten.
- Feuerstoß, Frostfaden, Schutz und andere normale Zauber nutzen kurze Farbspuren und zeigen ihre Wirkung an den im Kampfereignis gespeicherten Zielen.
- Phönixflamme, Sternenfall und mythische Helfer erhalten einen gut zwei Sekunden langen Echtzeiteinspieler. Die 3D-Kulisse nutzt bewegtes Licht und Kamerafahrt mit den gemalten Itoeva-Sprites. Die Montage greift die schwebenden Werkstücke aus der früheren Wesenbuch-Vorschau auf.
- Beim Rückschnitt lösen die gespeicherten Treffer und Beschwörungen ihre Wirkung auf dem Feld aus. „Zum Treffer“ springt vor den Einschlag und hält Anzeige, Ton und Aktionssperre synchron.

## Prüfung

Der Godot-UI-Test spielt Feuerstoß gegen einen geschützten Helfer, Vexzelanz, Phönixflamme und Sternenfall. Er prüft die exakten Schild- und LP-Verluste, den richtigen Beschwörungsplatz, die Begrenzung bei voller Reihe, den Sprung per Touch und Sternenfalls eigenen Schutz. Der Mitschnitt läuft mit Godots OpenGL-Renderer auf dem Desktop. Die Android-Datei ist eine signierte Aktualisierung der Kartenschmiede-Vorschau.
