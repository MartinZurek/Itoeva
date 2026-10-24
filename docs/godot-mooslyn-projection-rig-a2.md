# Mooslin 3D Prototyp – Anker A2, 24.10.2026

## Auftrag und Ergebnis

Fortsetzung von A1: Den originalgetreuen Seitenprototyp als bewegbare Figur in
Itoeva einbauen. Basis ist `codex/mooslyn-distance-cadence-0153` bei
`f47e08f1b848747d4fc05540fa036a17e1d27eff`. Aktuelles `main` wurde vor dem
isolierten Checkout verglichen: `124186c77f20954321008b0bf373a1527562f11a`.
So bleiben die neueren Groessen- und Kadenzkorrekturen enthalten.

**FACT:** Das A1-Volumen traegt jetzt ein importiertes Skelett mit 12 Gelenken;
Originalprojektion, kein neues Fawn-Design. 28584 statt 127970 Dreiecke.
`compact_actor` nutzt es fuer Mooslyns seitliches Stehen/Gehen, wenn der Schalter
in Einstellungen aktiviert ist. Dieselbe Steuerung, Kapsel, Schatten und
Wegstreckenphase; keine Aenderung von WALK_SPEED/RUN_SPEED oder Ausdauer.

**DOCUMENTED INTENT:** Die sichtbare Originalidentitaet hat Vorrang. Der Prototyp
bleibt freiwillig zuschaltbar; eine fertige Vieransichten-Figur wird nicht behauptet.

## Benutzung und Rueckfall

Einstellungen → **Mooslyn 3D: an**, Mooslyn waehlen und seitlich gehen.
Vorder-/Rueckansicht, Sprint, Springen, Rollen und Sitzen nutzen weiterhin die
vorhandenen Original-/Bewegungszeichnungen. Die Prototypoption ist zuerst aus.
Einstellung `settings.mosslyn_volume` wird im vorhandenen Godot-JSON gespeichert;
fehlender Schluessel bedeutet false. Keine Datenbankmigration.
Ausschalten stellt den bisherigen Pfad wieder her. Ein Revert des isolierten
Integrationscommits entfernt den Code; ein uebrig gebliebener boolescher Saveschluessel
wird vom vorherigen Stand ignoriert. Keine Android-/Reminder-Daten beruehrt.

## Geprueft

Godot **4.5.1** lokal, Headless und echtes OpenGL/Mesa-Rendering:

- `tests/mosslyn_volume.gd`: 334 Headless / 398 OpenGL, jeweils 0 Fehler.
  Beide Seiten, 32 Phasen, IK-Ziel gegen importierte Gelenkkette, Sichtbarkeit,
  Begrenzung und Rueckfall fuer Sonderaktionen/andere Spieler.
- `tests/mosslyn_volume_world.gd`: 13/0, Headless und OpenGL. Echter
  Einstellungsknopf, Speicherung/Neuladen, Weltstart, Steuerung, Bodenkontakt,
  Phasenfortschritt, Anhalten, Abschalten. Godot-Aufnahmen stammen aus diesem Test.
- Bestehende Charakterpruefung 869/0, Ausdauer 204/0, gemalte Mooslyn-Phasen
  1164/0. Vollstaendiger Spiel-Smoke: 86/0; bestehende Python-Assettests: 2/0.

Beim Beenden einiger vorhandener Spieltests meldet Godot noch Resource-/ObjectDB-
Restobjekte; die vorhandenen Tests melden dasselbe. Keine Fehler in den Assertions.
Keine Android-APK gebaut, kein Telefon-FPS-/GPU-Test. Workflow und Signierung unveraendert.

## Sichtpruefung und Grenzen

**TESTED BEHAVIOR:** Godot rendert die Originalzeichnung auf dem bewegten Volumen;
Kopf/Geweih/Schmuck wechseln nicht zwischen neu gezeichneten Bildern. Ein zuerst
mit dem Bein verformter Schwanz wurde beim Rendervergleich entdeckt und bekam
ein eigenes Gelenk. Spielweltaufnahme visuell geprueft.

**UNVERIFIED / noch unvollstaendig:** Keine endgueltige menschliche
Bewegungsabnahme. Knie und besonders Hueft-/Bauchuebergaenge koennen sich noch
verziehen, weil das A1-Volumen dort verbunden ist. Weltweite Fussverriegelung fehlt;
die IK-Pruefung belegt die Gelenkkette, nicht schlupffreien Bodenkontakt. Freie
Kamerawinkel, Vorder-/Rueckansicht und Vierfuesser-Sprint sind nicht gebaut.
Automatische Umschaltung zu gezeichneten Ansichten/Aktionen kann sichtbar sein.

## Naechster konkreter Schritt

Die Beine am Becken sauber trennen und verdeckte Flaechen anatomisch ergaenzen,
ohne Kopf/Fell/Umhang neu zu erfinden. Erst dann Fussstandphase mit dem wirklichen
Weltfortschritt verriegeln und Uebergaenge/Sprint entwickeln. Nicht bloss groessere
Gelenkwinkel als Ersatz fuer neue Geometrie verwenden. Bis zur Sichtabnahme
Schalter aus als Standard und PR als Entwurf behalten.

## Beauftragte Test-APK 0.15.4

Am 24.10.2026 hat Martin GitHub-Push, Entwurfs-PR und APK ausdruecklich freigegeben.
AndroidTest: Version 0.15.4, Code 34. Bestehenden Build-/Signierungs-/Drive-Weg
auf diesen Branch und ein aktuelles Laufzeitmanifest angepasst; beide neuen
Godot-Pruefungen auch in CI. Prototyp weiter in Einstellungen zuschaltbar.
Build und Auslieferung erst nach erfolgreichem Lauf bestaetigen.
