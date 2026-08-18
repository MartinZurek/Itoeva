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

## [done] ITO-0002 - Unit-Tests fuer MatrixColors
Ergaenze eine JVM-Unit-Testdatei `app-sim/src/test/java/com/notime/glyphsim/matrix/MatrixColorsTest.kt` fuer das bisher ungetestete Objekt `MatrixColors` aus `app-sim/src/main/java/com/notime/glyphsim/matrix/MatrixColors.kt`.

`MatrixColors` haelt vier ARGB-Konstanten, die zwischen der Compose-Ansicht und dem Widget-Bitmap-Renderer geteilt werden - genau deshalb ist ein unbeabsichtigtes Verschieben dieser Werte eine sichtbare Regression.

Verankere die vier Werte als Literale, nicht abgeleitet aus den Produktionskonstanten:

- `PUCK` entspricht `0xFF0A0A0A.toInt()`
- `PUCK_RIM` entspricht `0xFF2A2A2A.toInt()`
- `LED_OFF` entspricht `0xFF232323.toInt()`
- `LED_ON` entspricht `0xFFF3F1EA.toInt()`

Pruefe zusaetzlich, dass alle vier Werte voll deckend sind, also der Alpha-Kanal `0xFF` betraegt, und dass `LED_ON` heller ist als `LED_OFF` (Summe der drei Farbkanaele). Diese beiden Zusicherungen halten die Absicht fest, nicht nur die Zahlen.

Aendere keinen Produktionscode. Halte dich an den bestehenden Teststil des Moduls (JUnit 4, `org.junit.Assert`, deutschsprachige Backtick-Testnamen), siehe `MatrixGeometryTest.kt` als Vorbild.

## [open] ITO-0004 - Eine weitere Beziehung zwischen zwei Wesen ergaenzen
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18): weitere Beziehungen zwischen den sechs Wesen duerfen ohne
Rueckfrage zur kreativen Richtung ergaenzt werden. CONTENT-Evolution, keine Konstanten- oder
Codeaenderung.

Waehle GENAU EIN Wesen-Paar (A, B) aus den sechs Wesen (Puffling, Starlet, Wyrmling, Fennec,
Gloop, Hootlet), das noch KEINE der drei bestehenden Hauptbeziehungen ist. Die drei bestehenden
und weiterhin unveraenderten Paare: Puffling-Gloop, Wyrmling-Fennec, Starlet-Hootlet. Jedes
andere Paar ist erlaubt, deine Wahl.

Ergaenze in `app-sim/src/main/res/values-de/strings.xml` UND `app-sim/src/main/res/values/strings.xml`
(Deutsch und Englisch, inhaltlich gleich) an GENAU EINER Stelle einen zusaetzlichen Satz: entweder
an `lore_<A>_6` oder an `lore_<A>_7` (also bei EINEM der beiden gewaehlten Wesen, ein Stueck
deiner Wahl) - angehaengt an den bestehenden Text derselben Zeile, nicht als neue Zeile und nicht
als Ersatz des Bestehenden. Der neue Satz muss Wesen B beim Namen nennen und in der Stimme von
Wesen A geschrieben sein, ein bis zwei kurze Saetze, keine Ausrufezeichen, kein pathetischer Ton -
siehe die vorhandenen Stuecke 6 und 7 aller sechs Wesen in denselben Dateien als Vorbild
(z. B. `lore_puffling_6`, `lore_fennec_7`, `lore_gloop_6`). Eine Erwaehnung genuegt: Wesen B muss
in derselben Evolution NICHT umgekehrt auch von Wesen A erzaehlen.

Bedingungen, die nicht verhandelbar sind:
- Keine bestehende Zeile darf geloescht, umbenannt oder inhaltlich veraendert werden - nur die
  eine gewaehlte Zeile bekommt einen angehaengten Satz.
- Der neue Satz darf keinem bestehenden Fakt in irgendeinem der 42 Lore-Stuecke widersprechen
  (Orte, Ereignisse, andere Beziehungen). Bei Zweifel: Wesen B nur beilaeufig erwaehnen, keine
  neue Tatsachenbehauptung ueber B aufstellen, die B's eigene Lore-Stuecke nicht schon stuetzen.
- Keine andere Datei aendern, insbesondere nicht `PlayLore.kt` oder `PlayLoreTest.kt` - beide
  pruefen nur Struktur (Anzahl, Eindeutigkeit), keine Wortlaute, und muessen unveraendert gruen
  bleiben.
- `MEDICINE`, Nutzerdaten oder Aussagen ueber den Nutzer duerfen in Lore-Text nicht vorkommen -
  Lore ist erfundenes Worldbuilding, keine Aussage ueber den Nutzer.

## [open] ITO-0005 - Ein achtes Lore-Stueck je Wesen ergaenzen
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18): weitere Lore-Stuecke ueber die bestehenden sieben hinaus duerfen
ohne Rueckfrage zur kreativen Richtung ergaenzt werden - als CODE-Evolution, weil dabei
`PlayLore.PIECES` und Produktionscode angefasst werden. Baut auf keiner anderen offenen Aufgabe
auf; falls ITO-0004 noch offen ist, kann diese Aufgabe unabhaengig davon bearbeitet werden.

**Ziel:** Jedes der sechs Wesen bekommt ein achtes Lore-Stueck. Alle sechs muessen in DERSELBEN
Evolution zusammen entstehen - `PlayLoreTest` prueft `jedes Wesen hat gleich viel zu erzaehlen`
und schlaegt fehl, wenn nur einzelne Wesen ein achtes Stueck bekommen.

**Was das achte Stueck ERZAEHLERISCH ist:** Die bestehenden sieben Stuecke sind eine
Kennenlernfolge (siehe KDoc von `PlayLore`, Abschnitt "Aufbau je Wesen"): Identitaet, Wohnen,
Hobby, Beziehung, Aussenwelt, persoenliche Offenbarung, gemeinsame Welt - danach ist das
Kennenlernen fertig. Das achte Stueck ist kein neunter Schritt derselben Folge, sondern ein
erster GEWOEHNLICHER Moment DANACH: eine kleine, in sich abgeschlossene Beobachtung oder
Alltagsszene, im Ton naeher an Stueck 3 oder 6 als an eine grosse Enthuellung. Kein Cliffhanger,
kein Verweis auf ein kommendes neuntes Stueck - episodisches Erzaehlen bleibt episodisch (siehe
`EVOLUTION.md`, "Evolution Goals": eine groessere lineare Handlung ist weiterhin `OPEN DECISION`
und wird durch diese Aufgabe NICHT entschieden).

Stimme je Wesen (siehe die vorhandenen sieben Stuecke in `values-de/strings.xml` als vollstaendiges
Vorbild fuer Wortwahl, Satzlaenge und Tonlage): Puffling neugierig-optimistisch, Starlet ruhig und
etwas vertraeumt, Wyrmling laut und antreibend, aber nicht hektisch, Fennec gelassen und
verlaesslich, Gloop gemuetlich-chaotisch, Hootlet still und wortkarg. Ein bis drei kurze Saetze,
kein Ausrufezeichen, keine Aussage ueber den Nutzer.

**Umsetzung:**
1. In `app-sim/src/main/java/com/notime/glyphsim/ui/PlayLore.kt`: `PIECES` von `7` auf `8` setzen.
2. In derselben Datei, Funktion `story()`: bei allen sechs `AvatarSpecies`-Zweigen den Verweis auf
   die neue achte Ressource ergaenzen (`R.string.lore_<species>_8`).
3. In der KDoc des Objekts (`Aufbau je Wesen`): eine achte Zeile ergaenzen, die die Rolle des
   neuen Stuecks in einem Halbsatz beschreibt (passend zum tatsaechlich geschriebenen Inhalt,
   nicht wortgleich mit dieser Aufgabenbeschreibung uebernommen).
4. In `app-sim/src/main/res/values/strings.xml` (Englisch) UND
   `app-sim/src/main/res/values-de/strings.xml` (Deutsch): fuer alle sechs Wesen je eine neue
   Zeile `lore_<species>_8`, inhaltlich gleich in beiden Sprachen.

**Nicht anfassen:** `unlockedBy()`, `remember()`, `forget()`, `heard()`, `hasMore()`,
`hasMoreEver()` - die Kalenderlogik ist bereits generisch auf `PIECES` gebaut und braucht keine
Aenderung. Keine Aenderung an `PlayLoreTest.kt` - der bestehende Test muss unveraendert gruen
bleiben und ist bewusst so geschrieben, dass er acht Stuecke ohne Anpassung akzeptiert.

## [done] ITO-0003 - Unit-Tests fuer ClockRing
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
