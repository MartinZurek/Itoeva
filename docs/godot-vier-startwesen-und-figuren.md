# Vier Startwesen und überarbeitete Figuren

Martins Auftrag vom 19.10.2026: Nur Mooslyn, Nocti, Starlet (Starlit) und
Wyrmling (Würmling) bleiben in der Startauswahl. Fennec, Gloop, Puffling und
Hootlet bleiben mit ihren bestehenden Tagesplänen, Tätigkeiten und Dialogen Bewohner.

## Umsetzung

- Sechs neue gemalte Atlanten: Starlet, Wyrmling und die vier bisherigen Startfiguren.
  Welt, Dialogporträt, Kartenporträt und Kampffigur greifen auf dieselbe Identität zu.
  Originaldateien bleiben erhalten; die neuen Dateien heißen `*-v2.png`.
- Mooslyn und Nocti behalten ihre Gestaltung und erhalten eigene 24-Posen-Atlanten
  für Seiten-, Vorder- und Rückansicht: Lauf beziehungsweise Flügelschlag.
  Nocti hebt beim Sprint sichtbar ab und landet nach Loslassen des Sticks.
  Die Boden-/Wegkollision bleibt aktiv; Flug umgeht keine Weltgrenzen.
- Starlits vorhandene Aura/Eigenbeleuchtung wird auch im CompactActor eingerichtet.
- Formhelfer (Phönix, Urwaldhüter, Spiegelgeist usw.), Wildruf-Tiere und
  Strategiekarten behalten während Anlauf/Kontakt/Rückkehr ihre eigenen Grafiken.
  Die bisherige pauschale Laufbild-Ersetzung anhand der Herkunftsspezies entfällt.
- Alte ausgewählte Bewohner werden beim Laden durch Mooslyn als Auswahl ersetzt.
  Ihre Standorte, Decks, XP und Begegnungseinträge werden nicht gelöscht oder umgebucht.

## Grafikherstellung

Integrierte Bildgenerierung mit den vorhandenen PNGs als Identitätsreferenz.
Gemeinsames Promptset: feine Farbstift-/gemalte Wald-Fantasy; konsistente Silhouette,
Körpermasse, Blickrichtung, Farbpalette und Kleidung; echte Transparenz; keine Schrift,
Gitter oder Kulisse; acht Spalten/sechs Reihen, 32 Welt- und 16 Kampfposen.
Starlet: goldener fünfzackiger Stern, Indigoschal/Mondsichel, keine menschlichen Hände.
Wyrmling: schlanker graugrüner Drache, cremefarbener Bauch, orange Flügel/Kamm,
reptilienartige Schnauze, keine Fuchsmerkmale. Ein abweichender Entwurf wurde verworfen.
Fennec: große Ohren, orange/cremefarbenes Fell, rostbrauner Mantel/Türkisschließe.
Gloop: gelbgrüner Moosgeist, Blätter/Gänseblümchen, Ledertasche.
Puffling: cremefarbenes Fell, blaue Ohren/Pfoten, Salbeimantel/Tasche.
Hootlet: violette Eule, goldene Brille, indigofarbener bestickter Mantel.

Lauf-/Flugprompts: acht Spalten/drei Reihen, je acht Phasen für rechts/vorn/hinten;
artikulierte Fußkontakte und Flugphasen beim Lauf, echter Flügelschlag und angezogene
Beine bei Nocti. Eine abgewandte Seitenphase von Mooslyn wird nicht abgespielt.
`prepare_character_polish.py` leitet Ausschnitte aus zusammenhängenden Alphakonturen ab;
der Shader begrenzt CompactActor-Zellen auf diese Konturen. Kein Neuzeichnen durch Python.

## Prüfung und Grenzen

`tests/character_polish.gd` prüft vier Startfiguren, Erhalt alter Profile,
Dialoge/Tagespläne, alle acht Identitäten, Blickrichtungen/Gangphasen, Starlits Aura,
Noctis tatsächlichen Sprint samt Landung sowie die Helferdarstellung einschließlich
aller 616 Strategiekarten. Weitere Ergebnisse stehen im PR.

Vor der Unterbrechung wurden 1.073 Checks auch unter echtem Mesa/OpenGL bestanden und
79 Spielbilder geprüft. Nach dem Zurücksetzen der temporären Umgebung wurden der Code
wiederhergestellt und die Prüfungen erneut ausgeführt; maßgeblich ist der abschließende
PR-Bericht. Telefon-FPS, menschliche Animationsabnahme und neue APK bleiben offen.
Keine Änderung an Version, Paket, Signatur, Workflow oder Drive-Auslieferung.

Rückbau: den gesamten Figuren-/Auswahl-PR zurücknehmen. Die alten PNGs und alten
Spielstandschlüssel sind erhalten. Keine native Room-/LAS-/Reminder-Migration.
