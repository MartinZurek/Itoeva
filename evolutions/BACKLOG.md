# Itoeva-Backlog

Diese Datei ist die **einzige** Aufgabenquelle fuer zeitgesteuerte Laeufe von
`.github/workflows/claude-primary-run.yml`. Der oberste Eintrag mit Status `open` ist die
naechste Evolution. Das Modell waehlt seine Aufgabe nicht selbst und darf diese Datei auch
nicht aendern: `BACKLOG.md` steht in `runner/runner.config.json` unter
`scope.forbiddenFileNames`, und die Allowlist wird beim Lauf aus dem Basiscommit gelesen,
nicht aus dem Arbeitsstand.

Ein Eintrag gilt als erledigt, sobald der zugehoerige Pull Request gemerged ist. Den
Statuswechsel auf `done` traegt der Lauf selbst in den Evolutions-Branch ein (Schritt
"Backlog-Status im Branch nachziehen" in `claude-primary-run.yml`); wirksam wird er damit
genau in dem Moment, in dem ein Mensch den Branch merged. Wird der Branch verworfen,
erreicht der Statuswechsel `main` nie und der Eintrag bleibt offen - es ist also weiterhin
ein Mensch, der ueber "erledigt" entscheidet, nur ohne den leicht zu vergessenden
Handgriff danach.

Auf `main` schreibt der Workflow diese Datei nie, und der Builder erreicht sie ueberhaupt
nicht: der Statuscommit entsteht im `publish`-Job, in dem kein Modellcode laeuft, aus der
im `preflight`-Job ermittelten ID.

Bleibt ein erledigter Eintrag versehentlich auf `open`, waehlt der naechste Zeitplan-Lauf
ihn erneut - genau das ist mit ITO-0001 zweimal passiert.

## Format

Streng, weil `runner/backlog-select.sh` es maschinell liest:

    ## [<status>] <ID> - <Titel>
    <Aufgabentext, eine oder mehrere Zeilen>
    <bis zur naechsten "## "-Zeile oder zum Dateiende>

- `<status>` ist genau `open` oder `done`.
- `<ID>` passt auf `ITO-[0-9]{4}` und ist dateiweit eindeutig.
- `<Titel>` ist eine Zeile, nicht leer.
- Der Aufgabentext darf nicht leer sein.
- Jede Zeile, die mit `## ` beginnt und nicht auf dieses Muster passt, ist ein Formatfehler
  und laesst den Lauf fail-closed abbrechen - lieber ein roter Lauf als eine stillschweigend
  falsch gelesene Aufgabe.

Der Aufgabentext geht woertlich in den Builder-Prompt. Er sollte deshalb so vollstaendig und
so gut abgegrenzt sein, dass ein unbeaufsichtigter Lauf daraus arbeiten kann, ohne Rueckfragen
zu stellen - einschliesslich der Erwartungswerte, die der Builder ohne Shell nicht selbst
ausrechnen kann.

## [done] ITO-0001 - KDoc von MatrixCellSizing berichtigen
Berichtige die sachlich falsche Aussage in der KDoc von `app-sim/src/main/java/com/notime/glyphsim/matrix/MatrixCellSizing.kt`, Zeilen 5 bis 9.

Dort steht, die Mittelpunkte der aeussersten LED-Reihe laegen "exakt auf dem Puck-Radius" (6.5). Das stimmt nicht. Unabhaengig nachgerechnet: die groesste vorkommende Distanz einer aktiven Zelle zur Rastermitte betraegt `sqrt(41)`, also rund 6.403 (etwa bei Zelle (2,1)); die aeusserste vollstaendige Reihe liegt bei Abstand 6.0. Ein Wert von exakt 6.5 kommt nirgends vor.

Die Skalierung selbst ist richtig und darf nicht geaendert werden: `EFFECTIVE_GRID_UNITS = SIZE + 2 * DOT_RADIUS_FACTOR = 13.72` sorgt dafuer, dass auch die aeusserste Zelle samt Punktradius (6.403 + 0.36 = 6.763) innerhalb des halben Rasters von 6.86 bleibt. Formuliere die Begruendung so um, dass sie diese tatsaechlichen Zahlen nennt.

Aendere ausschliesslich den Kommentartext. Kein Code, keine Konstanten, keine Signaturen, keine anderen Dateien. Die bestehenden Tests in `app-sim/src/test/java/com/notime/glyphsim/matrix/MatrixCellSizingTest.kt` und `MatrixGeometryTest.kt` muessen unveraendert gruen bleiben.

## [open] ITO-0002 - Unit-Tests fuer MatrixColors
Ergaenze eine JVM-Unit-Testdatei `app-sim/src/test/java/com/notime/glyphsim/matrix/MatrixColorsTest.kt` fuer das bisher ungetestete Objekt `MatrixColors` aus `app-sim/src/main/java/com/notime/glyphsim/matrix/MatrixColors.kt`.

`MatrixColors` haelt vier ARGB-Konstanten, die zwischen der Compose-Ansicht und dem Widget-Bitmap-Renderer geteilt werden - genau deshalb ist ein unbeabsichtigtes Verschieben dieser Werte eine sichtbare Regression.

Verankere die vier Werte als Literale, nicht abgeleitet aus den Produktionskonstanten:

- `PUCK` entspricht `0xFF0A0A0A.toInt()`
- `PUCK_RIM` entspricht `0xFF2A2A2A.toInt()`
- `LED_OFF` entspricht `0xFF232323.toInt()`
- `LED_ON` entspricht `0xFFF3F1EA.toInt()`

Pruefe zusaetzlich, dass alle vier Werte voll deckend sind, also der Alpha-Kanal `0xFF` betraegt, und dass `LED_ON` heller ist als `LED_OFF` (Summe der drei Farbkanaele). Diese beiden Zusicherungen halten die Absicht fest, nicht nur die Zahlen.

Aendere keinen Produktionscode. Halte dich an den bestehenden Teststil des Moduls (JUnit 4, `org.junit.Assert`, deutschsprachige Backtick-Testnamen), siehe `MatrixGeometryTest.kt` als Vorbild.

## [open] ITO-0003 - Unit-Tests fuer ClockRing
Ergaenze eine JVM-Unit-Testdatei `app-sim/src/test/java/com/notime/glyphsim/matrix/ClockRingTest.kt` fuer das bisher ungetestete Objekt `ClockRing` aus `app-sim/src/main/java/com/notime/glyphsim/matrix/ClockRing.kt`.

`ClockRing.perimeterCells` liefert den aeussersten Ring aktiver Zellen, im Uhrzeigersinn ab "12 Uhr" sortiert. Getestet werden soll die zugesicherte Ordnung und Vollstaendigkeit, nicht die Winkelformel selbst.

Die folgenden Werte wurden ausserhalb des Produktionscodes nachgerechnet. Verwende sie als feste Literale und leite sie nicht aus `MatrixGeometry.RADIUS`, `MatrixGeometry.CENTER` oder der Winkelfunktion neu her:

- `perimeterCells` enthaelt genau 40 Zellen.
- Die erste Zelle ist `(6, 0)` - "12 Uhr", senkrecht ueber der Mitte.
- Die ersten sechs Zellen sind in dieser Reihenfolge: `(6,0)`, `(7,0)`, `(8,0)`, `(9,1)`, `(10,1)`, `(10,2)` - also im Uhrzeigersinn nach rechts laufend.
- Die letzten drei Zellen sind in dieser Reihenfolge: `(3,1)`, `(4,0)`, `(5,0)` - der Ring schliesst sich also von links wieder nach oben.

Pruefe ausserdem: keine Zelle kommt doppelt vor; jede Ringzelle ist auch in `MatrixGeometry.activeCells` enthalten; und der Ring ist echt kleiner als die Gesamtmenge der aktiven Zellen (40 von 137).

Die Reihenfolge der ersten sechs und der letzten drei Zellen ist der eigentliche Kern: sie faellt auseinander, wenn jemand die Sortierrichtung dreht oder den Nullpunkt des Winkels verschiebt.

Aendere keinen Produktionscode. Halte dich an den bestehenden Teststil des Moduls.
