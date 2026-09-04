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

## Grenzen

- Rein rechnerisch. Timing (`holdsMs`) steht nur als Zahl am Rand, die Bewegung selbst sieht
  man als Streifen, nicht als Film.
- Die Flugbahn der Rakete (`flightOffsetsFor`) verschiebt im Spiel die ganze Sprite-Box. Der
  Bogen zeigt nur die Figur darin.
- Kein Ersatz für einen Blick auf das Gerät — aber der Unterschied zwischen „nie gesehen" und
  „als Standbildfolge gesehen" ist größer als der zwischen Bogen und Gerät.
