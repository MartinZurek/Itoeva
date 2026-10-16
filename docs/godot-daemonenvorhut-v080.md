# Godot 0.8.0 – Dämonenvorhut und APK

Basis: PR #364, Git-Tree `523efbac18310d8ab7f8bcbd6a90c18a7493f2d2`.
Branch `codex/godot-demon-v080`; AndroidTest 0.8.0, Code 20.

## Beauftragter Umfang und Ergebnis

Vor dem APK-Bau Figuren und Kampffähigkeiten prüfen; ein bis zwei neue Charaktere
mit eigener Concept Art integrieren und einen ersten Dämonen-Endboss planen.
Reihenfolge: Dämonen, danach Artefakte, danach Maschinen. Artefakt-/Maschinenwelt
sind in diesem Schritt weder implementiert noch als fertig dargestellt.

Mühlenbach, Miro, sämtliche Landschafts-/Brückenkorrekturen aus PR #364 sowie der
0.7.0-Ausbau bleiben enthalten. Acht spielbare Wesen behalten ihre eigenen Decks,
Heldenkräfte und Spezialkräfte: Wildruf, Sternenlicht/Lichtschirm, Feuer,
Wasser/Heilung, Wind/Bereitschaft, Mond/Kontrolle, Ranken/Regeneration,
Echo/Ausweichen. Neue Figuren sind zwei bekämpfbare Gegner, keine zusätzlichen
spielbaren Helden; Bewohner werden nicht ungefragt zu Kämpfern umgewandelt.

## Neue Gegner an der Nebelwacht

| Gegner | Mechanik | Erkennbare Antwort |
| --- | --- | --- |
| Glutwicht, 145 LP | Ein Gegnerzug sammelt Glut; der folgende wirft Feuer für 24 Schaden auf das angezeigte Ziel. | Wasser löscht die Ladung, Wanken bricht sie ab. Schwächen, Lichtschirm, Ausweichen und Schutzhelfer greifen über dieselben bestehenden Regeln. |
| Siegelweber, 175 LP | Bereitet mit einem Siegelanker (0 Angriff, 10 LP, Schutz) ein Ritual vor. Im nächsten Gegnerzug: 10 Schaden an beide Helden, Verstummen nur bei durchdringendem Treffer. | Anker mit Angriffen/Zaubern zerstören: 12 Rückstoß am Weber statt Ritual. Wanken löst den Anker auf. Lichtschirm/Schutz fangen Treffer und damit den Status ab. |

Beide besitzen sechs eigene Posen mit transparenten Zuschnitten: Ruhe, Vorbereitung,
Angriff, Treffer, Niederlage und Schutz. Die markierte Aufladung und die
Absichtsanzeige zeigen den nächsten Schritt. Im Weltbild verwenden auch die
bisherigen Gegner jetzt Kontaktschatten, dieselbe Kulissen-Tiefenmaske wie die
Helden und vorhandenes lokales Licht. Die Dämonenvorhut steht auf der bekannten
Seite der Nebelwacht. Keine Brücke zur eigentlichen Dämonenwelt.

Die beiden Ritualzustände werden als optionale Felder gespeichert. Alte
Spielstände erhalten `false`. Ein ausstehender Effekt wird beim Fortsetzen nicht
noch einmal angewandt. Wiederholen/Rückzug setzt die Vorbereitung zurück;
geteilte XP/Vorräte und bisherige Begegnungsschlüssel bleiben bestehen.

Das ist ein erster taktischer Ausbau. Umfang, Balance und Produktionsreife eines
Hearthstone oder Magic werden damit nicht behauptet. Keine Karten oder Grafiken
aus diesen Spielen werden übernommen.

## Vharok – erster Endboss-Entwurf (noch kein spielbarer Boss)

Eigene Concept Art: `prototypes/godot-hd2d/concept-art/vharok-demon-boss-v080.png`.
Vharok, der Aschenpförtner: massiver Basaltdämon, gebrochene Geweihkrone,
Glutofen im Brustkorb, drei voneinander getrennte Siegelsteine.

Designrichtung für die spätere Umsetzung:

1. **Drei Siegel:** Sichtbare Siegel als angreifbare Ziele. Jedes kündigt einen
   anderen Folgeschritt an; das gebrochene Siegel entscheidet die freie Antwort.
2. **Offener Glutkern:** Zeitlich begrenztes Angriffsfenster, nachdem ein Siegel
   fällt. Schaden oder Gruppenstabilisierung sind gegeneinander abzuwägen.
3. **Aschensturm:** Bei niedrigen Boss-LP eskaliert der erkennbare Zugplan. Keine
   unangekündigte Sofortniederlage; Spezialkräfte und aufgehobene Karten bekommen
   ein sinnvolles Einsatzfenster.

LP, Schadenswerte, Belohnungen, Freischaltbedingungen und finale Phasenlogik sind
noch nicht implementiert. Erst Gegnermechaniken testen und auf dem Telefon
abnehmen, dann Bossarena und zusammenhängenden Dämonenwelt-Einstieg bauen.

## Grafikquellen, Prompts und Qualität

Integrierte Bildgenerierung mit Imagegen-Skill; eigene bestehende
`assets/expansion/enemies.png` als Stilreferenz. Erste gemeinsame Studie:
`concept-art/demon-vanguard-v080.png`. Für saubere Zuschnitte anschließend separate
Blätter mit sechs Posen erzeugt:

- `assets/expansion/glut-imp.png`
- `assets/expansion/seal-weaver.png`
- Rechtecke: `data/demons_atlas.json`

Promptset: detaillierte gemalte/Buntstift-Fantasiewesen im Itoeva-Stil; Glutwicht
als gedrungener Kohle-Imp mit Widderhörnern, Glutrissen, rostfarbenem Schal und
Schweif. Siegelweber als vierarmiger violetter Dämon mit Knochenmaske,
türkisen Augen und roten Wachssiegeln. Je 3 Spalten × 2 Reihen, sechs vollständige
Posen nach links, echte Transparenz, große freie Zwischenräume, keine Schrift,
keine Bodenschatten. Boss-Prompt: Vharok als vierarmiger Basaltdämon mit drei
Siegelsteinen vor uraltem Tor über Glut-Tal; Fennec und Stern als Größenmaßstab,
malerischer Chronikstil, kein Blut, kein Interface, keine Schrift.

Die erzeugten PNGs bleiben unverändert. Alpha-Unterkanten und freie Spalten
bestimmen die Laufzeit-Rechtecke. Concept Art ist vom Android-Export
augeschlossen. Alle acht bestehenden Helden wurden im echten OpenGL-Spielbild am
Mühlenbach kontrolliert, einschließlich der kompakten Mooslyn-/Nocti-Atlanten.

## Prüfstand

Neue Regel-/Integrationsprüfung: `tests/demons_v080.gd`, 89 Checks ohne Fehler.
Erfasst beide Zyklen, Wasser-/Wanken-Gegenmechanik, Schwächen, Lichtschirme,
Schutzhelfer, statusfreie Treffer bei absorbiertem Schaden, Save/Resume,
Rücksetzung, eigene Kräfte aller acht Helden sowie echte Begegnungsaktionen.
Renderaufnahmen: `tests/demon_capture_v080.gd`, alle acht Helden sowie beide
Gegner vor/während/nach Vorbereitung und Beobachten-Dialog. Zusätzlich die
Grafik-/Tiefenprüfung aus PR #364 und die vorhandenen Karten-, Helfer-, Sieg-,
Tür-, Wald-, Licht- und Weltregressionen.

Einige vorhandene Testansichten melden beim Prozessende ObjectDB-/Resource-
Restverweise. Telefon-FPS, Android-GPU und menschliche Abnahme bleiben nach dem
signierten Build offen. Die bestehende Debug-Signatur wird wiederverwendet;
keine Änderungen an Keystore, Berechtigungen oder Lieferziel.
