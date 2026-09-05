# Reaktions-Vorschau — die Animationen ansehen, ohne Gerät

```
tools/reaction-preview/render.sh [SPEZIES] [ZIELORDNER]
```

Erzeugt je Hauptgruppe einen Kontaktbogen als PNG: eine Zeile pro Knoten, acht gleichmäßig
über die Sequenz verteilte Standbilder nebeneinander. Danach läuft der Duplikat-Bericht.

Beim ersten Aufruf holt das Skript den Kotlin-Compiler (~80 MB) nach `.work/`. Danach dauert
ein Durchlauf gut eine Minute.

## Warum es das gibt

In `SKILLBAUM.md` steht dreizehnmal irgendeine Fassung von **„auf dem Gerät noch nicht
gesehen"**. Der Grund war nie Nachlässigkeit: In einer Cloud-Sitzung ist `dl.google.com`
gesperrt, also lässt sich das Android-Gradle-Plugin nicht laden, also baut nichts, also
rendert nichts. Wer eine Animation ändern wollte, änderte Zahlen und hoffte.

Das ist unnötig. Die Reaktionen sind **reines Kotlin** — `AvatarAnimations`, `AvatarBody`,
`AvatarReactions` und die Choreografien importieren zusammen kein einziges `android.*`. Sie
brauchen kein Android, sie brauchten nur nie jemanden, der sie ohne Android übersetzt.

Genau das tut dieses Skript: Kotlin-Compiler von `repo1.maven.org` bzw. GitHub (beide
erreichbar), die zwanzig beteiligten Dateien übersetzen, Frames als PNG ausgeben.

## Die drei Stellen, an denen es hakt — und wie sie gelöst sind

1. **`R.string.*`** erzeugt sonst das Android-Gradle-Plugin. Das Skript liest die Namen mit
   `grep` aus den Quellen und schreibt ein Platzhalter-`R` mit fortlaufenden Zahlen. Die IDs
   sind für eine Bildvorschau bedeutungslos — es geht nur darum, dass es übersetzt. **Gelesen
   statt gepflegt**, damit ein neuer String das Werkzeug nicht stillschweigend lahmlegt.
2. **`@StringRes`** kommt aus `androidx.annotation` — als Stub in `src/Annotations.kt`.
3. **Room-Entities** (`LibraryAnimation`, `FrameCodec`) hängen an Android. Sie werden nicht
   gebraucht und stehen deshalb nicht in der Dateiliste. Wächst die Liste, ist das die erste
   Stelle, an der es klemmt.

## Der Duplikat-Bericht

`DupesKt` vergleicht die Reaktionen aller Knoten Bild für Bild und nennt jene, die
**identisch** sind. Am 2026-09-04 waren das 38 von 80 Knoten (55 verschiedene Reaktionen bei
80 Motiven) — der Befund, aus dem P16 entstanden ist. Ein Blatt ohne eigene Choreografie erbt
die requisitenfreie Gruppen-Antwort; mehrere Geschwister erben damit *dieselbe*. Solange nur
die Uhr Reaktionen auslöste, fiel das kaum auf. Seit eine Freischaltung im Alltag sichtbar
wird (P15), ist es die Belohnung selbst, die unsichtbar bleibt.

Die Zahl gehört beim Verbessern von Animationen zuerst angesehen: Sie sagt, **wo** es sich
lohnt.

## Der zweite Ausgang: `routines.sh`

```
tools/reaction-preview/routines.sh
```

Gibt aus, welche `PlayRoutine` ein **kontextueller Skill-Intent** tatsächlich erzeugt — je Ort und
je Freischaltungsstand:

```
Anfaenger, Sportplatz -> [Stroll, Training.WARM_UP, Linger(2500), Training.REST, Linger(3000)]
mit Heben             -> [Stroll, Training.WARM_UP, Linger(4000), Training.LIFT, ...]
aus dem Wohnzimmer    -> [GoToPlace(SPORT), Stroll, Training.WARM_UP, ...]
```

Gedacht für die vertikalen Schnitte in `AvatarActivityPlans.resolve` (Fußball, Kraft & Ausdauer,
Musizieren, und was danach kommt). Man sieht die Schrittfolge, statt sie aus dem Code zu erraten —
und vor allem sieht man, dass eine **fehlende Freischaltung** die Handlung wirklich verkürzt statt
nur eine Zugabe wegzulassen.

Der Bericht nennt auch ausdrücklich, was **nicht** angeschlossen ist („kein kontextueller Schnitt
(alter Reaktionsweg)"). Das ist die schnellste Antwort auf die Frage, mit der eine Sitzung an
diesem Thema anfängt: Was liegt schon bereit und was fehlt noch?

Braucht drei Zeilen mehr Attrappe als `render.sh`: `PlayTimeLapse` und `PlayWeather` lesen
`SharedPreferences`, deshalb liegen in `src/AndroidStubs.kt` und `src/AndroidOsStubs.kt` gerade so
viele Platzhalter, dass es übersetzt. Geprüft wird damit **nicht** die Zeitraffer- oder
Wetterlogik, nur die Routinen-Entscheidung.

`AvatarActivityBus` bleibt draußen — er hängt an `kotlinx.coroutines` und wird für die Entscheidung
nicht gebraucht.

## Der dritte Ausgang: `tests.sh` — die Tests wirklich laufen lassen

```
tools/reaction-preview/tests.sh
```

Die beiden Skripte oben können den Code übersetzen und seine Ausgabe zeigen. Ob die vorhandenen
**Tests** dazu grün sind, ließ sich lokal lange gar nicht beantworten — die Antwort kam erst
Minuten später aus der CI. Damit war jeder Push eine Wette, und genau daran sind in diesem
Repository schon Befunde entstanden, die vorher hätten auffallen können.

`tests.sh` führt die reinen Kotlin-Unit-Tests tatsächlich aus: **125 Tests aus elf Klassen in
unter einer Sekunde.** JUnit 4 kommt aus Maven Central, der Kotlin-Compiler ist derselbe, den die
Nachbarskripte schon nach `.work/` holen.

```
JUnit version 4.13.2
..............................................................................................
OK (125 tests)
```

**Was hier nicht laufen kann**, und das ist keine Nachlässigkeit, sondern die Grenze der Methode:
alles, was Android, Room, Compose oder einen Emulator braucht — `app-sim/src/androidTest/`, die
Datenbank-Migrationen, die UI. Das bleibt Sache der CI. Die Liste der ausgeführten Klassen steht
deshalb wortwörtlich im Skript statt als Platzhalter; ein `*Test.kt` zöge Klassen herein, deren
Abhängigkeiten hier gar nicht übersetzt werden.

Fuer die Musikschicht kommen drei weitere Attrappen dazu (`MediaStubs.kt`,
`SettingsStoreStub.kt`, `LogStub.kt`): `PlayMusic` haengt an MediaPlayer und AudioManager,
`SettingsStore` an kotlinx-Flows. Geprueft wird damit ausdruecklich **nicht**, ob etwas
erklingt oder ob eine Einstellung einen App-Start ueberlebt - nur die Entscheidung
`PlayMusic.decide` und die Auswahl im `MusicResolver`. Die Persistenz sichert die CI.

Aus demselben Grund unterscheiden sich die Dateilisten der drei Skripte absichtlich: `render.sh`
braucht die Welt-Dateien nicht, `tests.sh` zusätzlich `LevelUnlocks.kt`. Ein gemeinsamer
Platzhalter hat hier schon einmal ein funktionierendes Werkzeug lahmgelegt, ohne dass jemand etwas
an ihm geändert hätte.

## Grenzen

- Rein rechnerisch. Timing (`holdsMs`) steht nur als Zahl am Rand, die Bewegung selbst sieht
  man als Streifen, nicht als Film.
- Die Flugbahn der Rakete (`flightOffsetsFor`) verschiebt im Spiel die ganze Sprite-Box. Der
  Bogen zeigt nur die Figur darin.
- `routines.sh` zeigt die Schrittfolge, nicht ihre Wirkung: Ob `GoToPlace` gut aussieht, sagt
  erst das Gerät.
- `tests.sh` deckt die Entscheidungs- und Reaktionslogik ab, nicht die Persistenz und nicht die
  Oberfläche. Ein grüner Lauf hier ist ein guter Grund zu pushen, kein Ersatz für die CI.
- Kein Ersatz für einen Blick auf das Gerät — aber der Unterschied zwischen „nie gesehen" und
  „als Standbildfolge gesehen" ist größer als der zwischen Bogen und Gerät.
