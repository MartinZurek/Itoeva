# Weltausbau – Orte, Interaktionen und Musik, Stück für Stück

Stand 2026-09-24. Auftrag des Nutzers: „Das Spiel an sich ist zu monoton … sowohl was das Visuelle
anbelangt, als auch das Auditive, das zu erweitern, Stück für Stück … nimm dir alle Freiheiten.“

Dieser Plan ist die Arbeitsliste dafür. Jede Stufe ist ein eigener, kleiner PR, der für sich
spielbar ist. Neue Musik kommt immer sofort in die Hörtest-APK (Regel in `UEBERGABE.md`) und geht
erst nach dem Hörtest nach `main`.

## Was es heute gibt

- **16 Orte** (`PlayScene.Place`): Schlafzimmer, Bad, Schreibtisch, Arbeit, Küche, Leseecke,
  Wohnzimmer, Bastelecke, Park, Sportplatz, Teich, Laden, Straße, Wald, Wiese, Stadt.
- **5 Sonderaktivitäten** (`PlayRoutines.SpecialActivity`): Drachen, Fußball, Basketball,
  Training, Angeln.
- **Tageszeit**: vier Phasen (`PlayAmbientActivity.DayPhase`: Morgen 6–10, Mittag 11–17, Abend
  18–22, Nacht 23–5), dazu Wetter (`PlayWeather`), Mond und Sterne.
- **Musik-Engine**: Rollen je Lage (`MusicResolver`), Rotation am Stückende, nahtlose Loops,
  bestätigte Ortswechsel, Themen-Einspieler beim Auftritt eines Gasts, Begrüßung am Tagesanfang.

## Stufe 1 – Musik kennt Umgebung und Aktivität (dieser PR)

- [x] `MusicContext.activity`: die gerade laufende Sonderaktivität.
- [x] Neue Rollen: **Natur** (Wald, Wiese, Teich tagsüber), **Laden**, **Angeln**, **Ballspiel**
      (Basketball, Fußball). Aktivitätsmusik beginnt sofort, endet mit Bestätigung.
- [x] Fünf Stücke: *My Neighbor Pixel*, *Clover Hills* (Natur), *Checkout Bossa* (Laden),
      *Gone Pixel Fishing* (Angeln), *Pixel Hoops* (Ballspiel).

## Stufe 2 – Wetter und Tageslicht in der Musik (klein, nur Engine)

- Regen und Gewitter wählen innerhalb einer Rolle bevorzugt die ruhigere Variante (z. B. *La
  Mélancolie des Pixels* statt *Pixel Supernova* in der Stadt). Dafür bekommt jede Variante im
  Katalog ein Stimmungs-Etikett (hell/ruhig/melancholisch); `PlayMusicRotation` gewichtet danach.
- Sonnenaufgang und -untergang als eigene kurze Momente: ein Einspieler wie beim Gast
  (`PlayMusicCue`), einmal je Übergang, wenn der Bildschirm gerade an ist.
- Drachensteigen bekommt eine eigene Aktivitätsrolle (*Wind*).
- [x] **Lautstärkeausgleich je Stück** (PR `claude/itoeva-loudness-kwlk4h`): Gemessen am 2026-09-24 liegen die Stücke zwischen −19,5 und
  −10,8 LUFS (*99 Pixels* ist fast doppelt so laut wie *Bitter Pixel Symphony*). Ein kleines
  Skript misst jede Datei und schreibt eine reine Kotlin-Tabelle; `PlayMusic` gleicht damit auf
  ein gemeinsames Ziel (etwa −16 LUFS) an, statt jede Datei mit derselben Lautstärke abzuspielen.

## Stufe 3 – Neuer Ort: die Spielhalle (visuell + Musik)

Warum zuerst: Sie passt zur Pixelwelt wie kein anderer Ort und bringt eine neue Art zu spielen
mit – das Wesen spielt selbst ein Spiel.
- Ort `ARCADE` in `PlayScene` (Automaten, Neonlicht, Teppich), erreichbar aus der Stadt.
- Interaktion: an einem Automaten spielen; Highscore als Erinnerung in der Welt.
- Musik: Chiptune-Hommage an klassische Automatenspiele (Rolle `ARCADE`).

## Stufe 4 – Neuer Ort: das Café

- Sozialer Treffpunkt: Bewohner sitzen dort, man trifft sich auf einen Kakao.
- Interaktion: bestellen, sitzen, mit einem Gast reden (nutzt die vorhandene Gesprächslogik).
- Musik: warmer Lo-Fi-Jazz / Café-Musette.

## Stufe 5 – Neuer Ort: das Seeufer / der Strand

- Sommerort mit Wasserlinie, Sand, Sonnenuntergang über dem Wasser.
- Interaktionen: schwimmen, Sandburg bauen, Steine hüpfen lassen.
- Musik: sonniger Surf-/Tropical-Sound.

## Stufe 6 – Neuer Ort: die Sternwarte

- Hootlets Sternwarte kommt schon in der Lore vor (siehe `strings.xml`, `lore_wyrmling_7`).
- Interaktion: nachts durch das Teleskop sehen; Sternbilder als kleine Entdeckungen.
- Musik: eine Nacht-Variante im Stil von *Ita Stella*.

## Stufe 7 – Neue Interaktionen an vorhandenen Orten

- Zu Hause: Gitarre spielen (das Wesen macht selbst Musik – die Engine duckt dabei den Score),
  kochen mit sichtbarem Ergebnis.
- Wiese: gärtnern, Blumen wachsen über Tage.
- Straße: Skateboard.
- Jede Interaktion bekommt, wo es passt, eine eigene Aktivitätsrolle in der Musik.

## Stufe 8 – Jahreszeiten und Feste

- Herbstlaub, Schnee, Sommerhitze als Kulisse; passende Varianten je Rolle.
- Feste im Jahreslauf (Laternenabend, Silvester) mit eigenem Stück.

## Regeln für jede Stufe

- Klein halten: ein Ort oder eine Engine-Fähigkeit je PR, reine Logik mit Tests.
- `PlayScene.kt` und `DockScreen.kt` nie ganz lesen, nur gezielt (siehe `UEBERGABE.md`).
- Eine neue `:core`-Entity braucht Migrationen in **beiden** Datenbanken (siehe `CLAUDE.md`).
- Musik: Stable Audio 3 `small-music` mit 8/1, keine Künstlernamen im Prompt, nur Essenz.
