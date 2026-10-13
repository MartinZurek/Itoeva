# Itoeva Godot v0.2.7 – Waldpfad und erster Schattenkampf

Der freigegebene Waldstrang ist spielbar in die ausgelieferte Godot-Fassung
v0.2.6 integriert. AndroidTest trägt jetzt Version 0.2.7 und Versionscode 10.

## So findest du den neuen Wald

Vom Park nach rechts durch die Wiese bis zum bisherigen Wald gehen.
Dort den Eingang **„Waldpfad zum Schattenwald“** antippen oder in Reichweite
mit E betreten. Der Rückweg liegt links am Anfang des neuen Waldsaums.
Zwischen den sieben neuen Abschnitten gibt es keinen Raumwechsel:

| Abschnitt | Umsetzung |
|---|---|
| Waldsaum | Helle Birken, Blumen und Bewohnerlaterne |
| Grüner Wald | Dichtere Kronen und kühlere Farben |
| Letzte Fackeln | Zwei lodernde Feuer und begrenzte Lichtinseln |
| Tor zum Schattenwald | Dunkle Wurzelbögen, benutzbare Ruhebank |
| Schattenbach | Animiertes Wasser, schmale trockene Holzbrücke |
| Dunkle Lichtung | Fackeln erlöschen, Restrauch und Dämonenbegegnung |
| Tiefer Schattenwald | Keine installierten Lichter, lokale Traglaterne |

**Traglicht: an/aus** schaltet die mitgeführte Laterne. Es gibt keinen
Lichtverbrauch und keine Verbrauchsstrafe. Der Schattenwald bleibt durch sein
Blätterdach auch am Tag dunkel. Die normale Berliner Spielzeit bleibt erhalten.

## Erster Kampf

Im Inneren der Lichtung erlöschen die Flammen samt warmen Lichtflächen.
Die saubere Arenakulisse enthält keine eingebrannten Flammen oder orangefarbenen
Restlichter. Kurzer Rauch steigt auf, danach erscheint ein einzelner Dämon aus
Schatten. Nach 3,2 Sekunden ist die Rundensteuerung verfügbar.

- **Schlag:** 6 Schaden, kein Lichtverbrauch.
- **Lichtstoß:** 9 Schaden, kostet einen Lichtpunkt.
- **Schützen + Licht:** reduziert den Schattenhieb und gewinnt einen Lichtpunkt.

Die Figur beginnt mit 30 Lebenspunkten, der Schatten mit 24; drei Lichtpunkte
sind verfügbar. Ein besiegter Schatten greift nicht noch einmal an. Der Sieg
schaltet die freie Bewegung wieder ein. Nach Niederlage sind Wiederholung und
Rückzug zum Waldsaum möglich. Während einer Aktion sperrt eine kurze Animation
weitere Angriffe; mehrfaches Tippen verbraucht keine zusätzlichen Runden.
Menü und Esc unterbrechen den Kampf; Fortsetzen kehrt in denselben Zustand zurück.

Die Begegnung wird **je Wesen** unter `forest_encounters` im bestehenden lokalen
Spielstand gespeichert. Alte v0.2.6-Spielstände benötigen keine Migration.
Szenenneustarts übernehmen Phase, Lebenspunkte, Licht und Runde. Nach Sieg
erscheint derselbe Dämon für dieses Wesen nicht erneut; die Arenafackeln bleiben
aus. Ein Rückzug nach Niederlage erlaubt einen neuen Versuch.

## Geometrie und Darstellung

Nachgezeichnete Wegprofile bestimmen sowohl die physische Bodenfläche und ihre
Randwände als auch Bewegungskontrolle und Tipp-Wegsuche. Der Kollisionsradius
der jeweiligen Figur wird berücksichtigt. Auch Sprung oder Rolle können die
Ufergrenzen nicht überspringen. Auf der Brücke bleibt nur das Deck begehbar.

Die Panoramen überlappen um zwei Welteinheiten. Beide Bilder werden in diesem
Bereich auf dasselbe interpolierte Bodenprofil verzogen; der nächste Bildrand
blendet weich ein. Sechs zusätzliche Erdwegstreifen verwenden die hochaufgelöste Malerei des grünen
Waldes, abgestimmte Bodenfarben und weiche Masken an den Wegsäumen. Das verbindet
auch unterschiedlich hohe gemalte Wegenden.
Helligkeit, Grün und Kronendichte folgen den Konzeptbildern. Boden und Bewegung
bleiben über alle sechs Anschlüsse kontinuierlich.

Flammen werden lokal verformt und durch bewegte Flammenkarten ergänzt. Licht,
Flamme und Rauch sind getrennt steuerbar. Wasseranimation ist auf die
nachgezeichneten Wasserflächen beschränkt; das Deck bleibt fest. Oberste
Kronen erhalten leichte Windbewegung. Die vordere Brüstung verdeckt Figuren
hinter dem Geländer, ohne rechteckige Kopien des Hintergrunds zu erzeugen.
Die neue Ruhebank besitzt Kollisionskörper, Annäherungsfläche, Tiefenmaske und
sechs visuell abgestimmte Sitzhöhen. Die Blickrichtung ist zur Bankvorderseite.

## Prüfung

Ausgeführt mit Godot 4.5.1, einschließlich echter OpenGL-Kompatibilitätsdarstellung
über Mesa/llvmpipe. Ergebnis: **17.505 automatisierte Prüfungen, keine Fehler.**

| Test | Prüfungen | Inhalt |
|---|---:|---|
| `forest_trail.gd` | 16.525 | Sechs Körperradien, alle Wegspalten und Anschlüsse, echte Brückenphysik, Bank, Hin-/Rückweg, Menü, Touch, Speicherzustand, Sieg, Niederlage, Rückzug |
| `forest_render.gd` | 7 | Sichtbare Flammen- und Wasserbewegung, feste Pfosten und Deck, erlöschende Arena, lokale Traglaterne |
| `full_smoke.gd` | 76 | Originalwelt, sechs Wesen, Bewegung, Innenräume, Türen, Sitzen und Speichern |
| `gestures_paths.gd` | 117 | Gesten und vorhandene natürliche Tipp-Wege |
| `bridge_navigation.gd` | 15 | Bestehende Brückennavigation |
| `world_clock.gd` | 71 | Berliner Spielzeit und Zeitfortschritt |
| `proportions_and_readability.gd` | 694 | Figurengrößen und Lesbarkeit |

Zusätzlich wurden alle sieben Waldansichten, die sechs Bildanschlüsse, die
Kampfansicht und die Bank mit allen sechs Wesen direkt in Godot aufgenommen
und visuell kontrolliert. `git diff --check` ist fehlerfrei.

Die APK wurde exportiert und kryptografisch geprüft: ARM64, Android ab API 24,
Ziel-API 35, Paket `com.itoeva.godot.hd2d.worldtest`, Version 0.2.7 / Code 10,
kein Internetzugriff. Zertifikat SHA-256:
`fe124bbf6e3682280f62d0ba7e0619c0fc3c266adfa74b6e99c77d4ed6318582`.
Es ist derselbe Testschlüssel wie in v0.2.6. Der Schlüssel ist nicht im Quellarchiv.
Die signierte Test-APK kann die vorhandene v0.2.6-Testinstallation aktualisieren.

**Noch offen:** Die Übergangsmasken sind eine erste spielbare Fassung; der
Bodenverlauf an den sechs Anschlüssen kann anhand von Geräteaufnahmen weiter
gestalterisch verfeinert werden. Installation, Laufzeit und Bedienung auf einem echten
Android-Gerät konnten hier nicht geprüft werden. Der erste Kampf ist ein kleiner
spielbarer Prototyp mit vorläufigen Zahlen; es gibt noch kein ausgebautes
Kampffähigkeits-, Ausrüstungs- oder Belohnungssystem. Die übergreifende Anpassung
der älteren Stühle und Bänke aus dem ursprünglichen Auftrag ist nicht Teil dieses
Waldschritts; angepasst wurde die neue Waldruhebank.

## Quellprojekt

Basis: zuletzt ausgeliefertes v0.2.6-Quellarchiv. Separater lokaler Branch:
`codex/shadow-forest-first-encounter`. Kein GitHub-Push und kein Merge vorgenommen.
Das Quellarchiv enthält das vollständige Projekt, neue Grafiken, Shader,
Skripte, Tests, Vorschauaufnahmen und diese Dokumentation. Importierte
Godot-Caches und lokale Signierinformationen sind ausgeschlossen.

In Godot `prototypes/godot-hd2d/project.godot` öffnen, den Import abwarten und
`scenes/full_world.tscn` starten. Der bisherige geschützte Android-Preset bleibt
erhalten; allein der separate AndroidTest-Preset wurde versioniert.
