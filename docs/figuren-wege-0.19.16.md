# Gameplay 0.19.16 – Figuren und Wege

Basis: `a2e573d`, `codex/gameplay-living-world-20261119`.

Die bisherige Pose-Auswahl nutzte die M4-Grafiken hauptsächlich seitlich.
Tiefenbewegung, Rollen und Sitzen fielen auf ältere Atlanten zurück.
Neue ergänzende Atlanten für Mooslyn, Nocti, Starlight und Wyrmling schließen
diese Rückfälle. Die seitlichen M4-Laufzyklen bleiben erhalten. Auch die
Ruheplätze verwenden die aktuelle Identität; ihre räumlichen Bewegungen bleiben
im bestehenden Ruheplatzsystem. Die neuen Ruheposen sind einzelne Posen, keine
neu gezeichneten vollständigen Einschlafzyklen. Die Begleiter haben zwei
Tiefenbewegungsposen je Ansicht, Mooslyn vier. Weitere Zwischenbilder bleiben
eine mögliche spätere Verfeinerung.

Alle vier Startfiguren nutzen im Kartenkampf ihre aktuellen M4-Atlanten,
einschließlich Anlauf und Zauberdarstellung. NPCs ohne Designwechsel behalten
ihre eigenen Grafiken.

In Sonnenhain, Nebelwacht, Mühlenbach, Aschenstieg, Siegelhof und Relaisgarten
wurden die sichtbaren Abzweige und zugänglichen Vorplätze nachgezeichnet.
Außenkarten verwenden die tatsächlichen Wegkonturen statt eines zusätzlichen
pauschalen Randabstands. Übergänge zum alten Zulauf und durch die Tore liegen
jetzt auf den zugehörigen Pfaden. Die Polygonvereinigung prüft nach jeder
Verbindung erneut alle Teilflächen, damit keine inneren Kollisionskanten bleiben.

## Prüfung

- Godot 4.6.3: `tests/character_routes_play.gd`, 536 Prüfungen bestanden:
  vier Figuren, vier Richtungen, sechs Zustände, Kampfgrafiken, Pfadabschnitte
  mit Figurbreite und gesperrte Hintergrund-/Vordergrundpunkte.
- `tests/gameplay_play.gd`: bestanden, einschließlich sieben Kampfplätze,
  Gegneraktionen, Kartenvorschau, Zielablage, Rückgabe und Helferpositionen.
- `tests/ground_contact_play.gd`: 45 Prüfungen bestanden.
- Headless-Läufe melden beim Beenden bereits vorhandene Ressourcenlecks.
- Gerenderte Ingame-Abnahme und Installation auf Android noch offen.
  Die lokale Desktop-Vorschau konnte wegen fehlendem Eingabefokus nicht
  gestartet werden. Die neuen Atlasbilder wurden direkt visuell geprüft.

Die Gameplay-APK hat Version 0.19.16, Build 78 und das bestehende Paket
`com.itoeva.gameplay.preview`. Die vorhandene Gameplay-Testsignatur wird
weiterverwendet. Quellen der ergänzenden Atlanten liegen unter
`assets/characters/source`; sie werden nicht in die Gameplay-APK übernommen.
`tools/import_character_views.gd` erzeugt daraus normierte Zellen und Masken.

Nächste Abnahme: auf Android Vorder-/Rückwärtsbewegung, Rollen, Sitzen,
Ruheplätze und die sechs korrigierten Außenbereiche durchspielen.
