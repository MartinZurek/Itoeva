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

## [done] ITO-0004 - Eine weitere Beziehung zwischen zwei Wesen ergaenzen
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

## [done] ITO-0005 - Ein achtes Lore-Stueck je Wesen ergaenzen
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

## [done] ITO-0009 - Noch eine weitere Beziehung zwischen zwei Wesen ergaenzen
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18), im selben Rahmen wie bereits ITO-0004 (umgesetzt in PR #14:
Puffling erwaehnt Starlet in `lore_puffling_6`). CONTENT-Evolution, keine Konstanten- oder
Codeaenderung.

Seit ITO-0005 hat jedes Wesen acht Lore-Stuecke (`PlayLore.PIECES = 8`) - die Ankerzeile fuer den
neuen Satz darf deshalb Stueck 6, 7 ODER 8 sein, nicht mehr nur 6/7.

Waehle GENAU EIN Wesen-Paar (A, B) aus den sechs Wesen (Puffling, Starlet, Wyrmling, Fennec,
Gloop, Hootlet), zwischen denen noch KEIN Satz in den bestehenden Lore-Texten eine Verbindung
herstellt. Bereits verbunden und NICHT erneut zu waehlen: Puffling-Gloop, Wyrmling-Fennec,
Starlet-Hootlet (die drei Hauptbeziehungen) sowie Puffling-Starlet (ergaenzt durch ITO-0004).
Jedes andere Paar ist erlaubt, deine Wahl.

Ergaenze in `app-sim/src/main/res/values-de/strings.xml` UND `app-sim/src/main/res/values/strings.xml`
(Deutsch und Englisch, inhaltlich gleich) an GENAU EINER Stelle einen zusaetzlichen Satz: an
`lore_<A>_6`, `lore_<A>_7` ODER `lore_<A>_8` (ein Stueck deiner Wahl, bei EINEM der beiden
gewaehlten Wesen) - angehaengt an den bestehenden Text derselben Zeile, nicht als neue Zeile und
nicht als Ersatz des Bestehenden. Der neue Satz muss Wesen B beim Namen nennen und in der Stimme
von Wesen A geschrieben sein, ein bis zwei kurze Saetze, keine Ausrufezeichen, kein pathetischer
Ton - siehe die vorhandenen Stuecke aller sechs Wesen in denselben Dateien als Vorbild.

Bedingungen, die nicht verhandelbar sind:
- Keine bestehende Zeile darf geloescht, umbenannt oder inhaltlich veraendert werden - nur die
  eine gewaehlte Zeile bekommt einen angehaengten Satz.
- Der neue Satz darf keinem bestehenden Fakt in irgendeinem der 48 Lore-Stuecke widersprechen
  (Orte, Ereignisse, andere Beziehungen, einschliesslich der in ITO-0004 ergaenzten
  Puffling-Starlet-Verbindung). Bei Zweifel: Wesen B nur beilaeufig erwaehnen, keine neue
  Tatsachenbehauptung ueber B aufstellen, die B's eigene Lore-Stuecke nicht schon stuetzen.
- Keine andere Datei aendern, insbesondere nicht `PlayLore.kt` oder `PlayLoreTest.kt` - beide
  pruefen nur Struktur (Anzahl, Eindeutigkeit), keine Wortlaute, und muessen unveraendert gruen
  bleiben.
- `MEDICINE`, Nutzerdaten oder Aussagen ueber den Nutzer duerfen in Lore-Text nicht vorkommen -
  Lore ist erfundenes Worldbuilding, keine Aussage ueber den Nutzer.

## [done] ITO-0010 - Eine weitere Beziehung zwischen zwei Wesen ergaenzen (dritte Runde)
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18), im selben Rahmen wie bereits ITO-0004 (PR #14:
Puffling-Starlet) und ITO-0009 (PR #18: Wyrmling-Hootlet). CONTENT-Evolution, keine
Konstanten- oder Codeaenderung.

Seit ITO-0005 hat jedes Wesen acht Lore-Stuecke (`PlayLore.PIECES = 8`) - die Ankerzeile fuer
den neuen Satz darf Stueck 6, 7 ODER 8 sein.

Waehle GENAU EIN Wesen-Paar (A, B) aus den sechs Wesen (Puffling, Starlet, Wyrmling, Fennec,
Gloop, Hootlet), zwischen denen noch KEIN Satz in den bestehenden Lore-Texten eine Verbindung
herstellt. Bereits verbunden und NICHT erneut zu waehlen: Puffling-Gloop, Wyrmling-Fennec,
Starlet-Hootlet (die drei Hauptbeziehungen), Puffling-Starlet (ITO-0004) sowie Wyrmling-Hootlet
(ITO-0009). Jedes andere Paar ist erlaubt, deine Wahl - zehn Paare stehen noch offen.

Ergaenze in `app-sim/src/main/res/values-de/strings.xml` UND `app-sim/src/main/res/values/strings.xml`
(Deutsch und Englisch, inhaltlich gleich) an GENAU EINER Stelle einen zusaetzlichen Satz: an
`lore_<A>_6`, `lore_<A>_7` ODER `lore_<A>_8` (ein Stueck deiner Wahl, bei EINEM der beiden
gewaehlten Wesen) - angehaengt an den bestehenden Text derselben Zeile, nicht als neue Zeile und
nicht als Ersatz des Bestehenden. Der neue Satz muss Wesen B beim Namen nennen und in der Stimme
von Wesen A geschrieben sein, ein bis zwei kurze Saetze, keine Ausrufezeichen, kein pathetischer
Ton - siehe die vorhandenen Stuecke aller sechs Wesen in denselben Dateien als Vorbild.

Bedingungen, die nicht verhandelbar sind:
- Keine bestehende Zeile darf geloescht, umbenannt oder inhaltlich veraendert werden - nur die
  eine gewaehlte Zeile bekommt einen angehaengten Satz.
- Der neue Satz darf keinem bestehenden Fakt in irgendeinem der 48 Lore-Stuecke widersprechen
  (Orte, Ereignisse, andere Beziehungen, einschliesslich der in ITO-0004 und ITO-0009 ergaenzten
  Verbindungen). Bei Zweifel: Wesen B nur beilaeufig erwaehnen, keine neue Tatsachenbehauptung
  ueber B aufstellen, die B's eigene Lore-Stuecke nicht schon stuetzen.
- Keine andere Datei aendern, insbesondere nicht `PlayLore.kt` oder `PlayLoreTest.kt` - beide
  pruefen nur Struktur (Anzahl, Eindeutigkeit), keine Wortlaute, und muessen unveraendert gruen
  bleiben.
- `MEDICINE`, Nutzerdaten oder Aussagen ueber den Nutzer duerfen in Lore-Text nicht vorkommen -
  Lore ist erfundenes Worldbuilding, keine Aussage ueber den Nutzer.

## [done] ITO-0006 - Eine weitere allgemeine Bibliotheks-Animation ergaenzen
Bereits durch "Evolution Goals" in `EVOLUTION.md` gedeckt ("Vielfalt von Ambient-Aktivitaeten,
Routinen, Szenen, Reaktionen und charaktergerechten Dialogen") - keine `OPEN DECISION` betroffen,
keine Rueckfrage noetig. CONTENT-Evolution (reine Punktdaten, kein neuer Mechanismus).

`core/src/main/java/com/notime/glyphcore/data/DefaultLibraryAnimations.kt` sagt in seiner
eigenen Klassendoku bereits: "26 Beispiel-Animationen ... bevor weitere 30-40 dazukommen." Diese
Aufgabe ist ein einzelner Schritt in genau diese schon dokumentierte Richtung - keine neue
Entscheidung, sondern die Fortsetzung einer bereits getroffenen.

**Aufgabe:** Ergaenze GENAU EINE neue allgemeine Animation (keine charakterspezifische - die
liegen in `AvatarSignatureAnimations.kt` und sind nicht Teil dieser Aufgabe). Thema deiner Wahl,
passend zu einem Alltags- oder Erinnerungsthema (siehe die 26 vorhandenen Labels in
`general()` als Anhaltspunkt fuer den Rahmen: Star, Wave, Rain, Music, Battery, Dog, Cat, Gift,
Football, Fitness, Robot, Trophy, Plant, Target, Airplane, Cake, Idea, Mail, ...). Vermeide ein
Thema, das einem bestehenden Label zu nahekommt.

**Technische Form** (siehe `starFrames()`/`waveFrames()` in derselben Datei als Vorbild):
- Eine private Funktion `private fun <name>Frames(): List<List<Pair<Int, Int>>>` - eine
  Punktliste je Frame, Koordinaten im 13x13-Raster (Zentrum bei 6,6).
- Registrierung als neuer Eintrag in der Liste in `general()`: `LibraryAnimation(label = "...",
  emoji = "...", framesData = FrameCodec.encode(<name>Frames()), sortOrder = 56)`. NICHT 26: dieser
  Wert ist zwar der naechste freie in `DefaultLibraryAnimations.kt` selbst (hoechster bisheriger
  Wert 25), kollidiert aber mit `AvatarSignatureAnimations.kt` - dort reserviert
  `SORT_OFFSET = 26` denselben Zahlenraum fuer die 30 charakterspezifischen Animationen (6 Spezies
  a 5 Stueck, siehe `seed()` dort), belegt also 26..55. 56 ist der erste Wert, der in KEINER der
  beiden Dateien vorkommt.
- Mindestens 4-6 Frames mit ZWEI erkennbaren Bewegungen, nicht nur einem pulsierenden Element -
  das war laut Klassendoku ein Kritikpunkt an frueheren Entwuerfen (siehe `starFrames()`: Strahlen
  pulsieren UND ein Twinkle-Punkt wandert reihum; `waveFrames()`: Sinus-Schwell UND ein
  Schaum-Punkt an jedem Wellenkamm).

**Drei dokumentierte Fallen, die `LibraryAnimationFitTest` automatisiert prueft und die den Lauf
rot werden lassen, wenn sie zutreffen** (siehe die KDoc dieser Testdatei fuer die volle
Begruendung):
1. Die Matrix ist rund - nur der einbeschriebene Kreis (`MatrixGeometry.isActive`) wird
   angezeigt, die vier Ecken des 13x13-Quadrats existieren nicht. Genau das ist "TAMA" passiert,
   das seine Buchstaben in die Ecken gesetzt hatte. Hoechstens 15 % der Punkte ueber alle Frames
   duerfen ausserhalb liegen.
2. Keine wilden Koordinaten - alle Punkte muessen im Bereich -4 bis 17 bleiben (etwas ueber den
   Rand hinaus ist als Gestaltungsmittel erlaubt, z. B. eine Rakete, die aus dem Bild steigt).
3. Punkte auf einer Linie oder gefuellte Flaechen koennen das Motiv unkenntlich machen (siehe
   `EVOLUTION.md`/README zu den Signatur-Animationen) - bevorzuge Umrisse und versetzte Punkte
   gegenueber vollstaendig gefuellten Formen.

**Zusaetzlich Pflichtteil der Aufgabe, NICHT optional:** Die Klassendoku VON DERSELBEN DATEI nennt
an zwei Stellen die alte Anzahl und muss auf den neuen Stand gebracht werden - sonst widerspricht
die Doku dem Code, den sie beschreibt:
- Zeile 9: "26 Beispiel-Animationen" -> "27 Beispiel-Animationen".
- Zeile 27: "Die 26 allgemeinen Animationen plus die 30 charakterspezifischen" -> "Die 27
  allgemeinen Animationen plus die 30 charakterspezifischen".
Beides steht bereits im Umfang dieser Aufgabe (dieselbe Datei, keine neue Entscheidung) und ist
keine "andere Datei" im Sinne der folgenden Einschraenkung.

Aendere keine andere Datei, keine bestehende Animation, keinen `sortOrder`-Wert einer bestehenden
Animation. `LibraryAnimationFitTest` muss fuer die neue Animation ebenso gruen sein wie fuer die
bestehenden 26 (der Test iteriert automatisch ueber `DefaultLibraryAnimations.seed()`, keine
Testaenderung noetig).

## [done] ITO-0007 - Ein weiteres Beiwerk-Requisit im Wald ergaenzen
Bereits durch "Evolution Goals" in `EVOLUTION.md` gedeckt (Vielfalt der Szenen) - keine
`OPEN DECISION` betroffen, keine Rueckfrage noetig. Neues Requisit + ein Placement-Eintrag, kein
neuer Mechanismus: keine neue Station, keine Aenderung an `Acquisition`/`PlayPath` (die
progressionsgebundenen Zimmererweiterungen bleiben `OPEN DECISION` und sind NICHT Teil dieser
Aufgabe).

**Ort:** `app-sim/src/main/java/com/notime/glyphsim/matrix/PlayScene.kt`, `Place.FOREST` in
`furnishing()`. Der Wald ist der von "Natur" ausdruecklich gemeinte, artenaermste geteilte
Aussenort: aktuell sechs Placements (`OLDTREE`, `PINE` zweimal, `BUSH`, `LOG`, `TREE`), belegte
`anchorX`-Werte 0.34, 1, 0, 0.28, 0.58, 0.86.

**Aufgabe:** Genau EIN neues privates `Prop` in derselben Datei ergaenzen (Wahl frei, z. B.
Pilzgruppe, Farn, Laubhaufen, moosbewachsener Stumpf - solange es als Bodenbewuchs/Unterholz
liest und keine der in `habitatPlacements()` bereits vergebenen Landschaftsformen dupliziert:
`ROCK`, `CRAG`, `ACACIA`, `REEDS`, `FLOWER` sind belegt). Format wie `BUSH`/`TREE`: `Prop(width,
height, art = hLine(...)+ vLine(...))` - siehe `BUSH` (Zeile ~1372) als Vorbild fuer eine flache
Silhouette. Richtwert Hoehe <= 4, damit es als Unterholz und nicht als weiterer Baum wirkt.

Das neue Prop als zusaetzliches `Placement(<NEUES_PROP>, anchorX = <freier Wert zwischen 0 und
1, ungleich den sechs belegten>)` OHNE `station` in die Liste unter `Place.FOREST` einfuegen -
Beiwerk wie `BUSH`, keine neue Station, kein `behind = true` (das ist den beiden Hintergrundbaeumen
vorbehalten).

**Bedingungen:**
- Keine Aenderung an anderen `Place`-Zweigen, an `Acquisition`, an `Station`, an `groundDetail()`
  oder `habitatPlacements()`.
- Kein bestehendes Prop, keine bestehende `anchorX`, kein bestehender `sortOrder`/Wert veraendern.
- `SceneCompositionTest` muss unveraendert gruen bleiben, insbesondere `keine zwei Requisiten
  stehen ineinander`, `niemand ragt durch die Zimmerdecke`, `an jedem Ort bleibt Platz zum
  Stehen`, `auf schmalen Bildern nutzt das Zimmer die volle Breite`. Der Test iteriert automatisch
  ueber alle Orte/Spezies, keine Testaenderung noetig - `fitting()` laesst ueberzaehliges Beiwerk
  auf schmalen Bildern ohnehin automatisch wegfallen, das neue Requisit darf dafuer ausgelegt
  sein zu verschwinden, nicht zu ueberlappen.

## [done] ITO-0008 - Zuletzt erzaehltes Lore-Stueck bleibt beim Wiederoeffnen sichtbar
UX-Verbesserung am bestehenden Gespraech, rein additive Leseanzeige: keine neue Ablage, kein
neuer Mechanismus, keine Aenderung an Fortschritt oder Kalender. `PlayLore.heard()`,
`available()`, `PIECES`, `unlockedBy()` bleiben unangetastet - deshalb keine `OPEN DECISION`
betroffen.

**Ausgangslage:** `PlayLore.heard(context, species)` liefert bereits, wie viele Stuecke erzaehlt
sind; `PlayLore.story(species)` liefert sie in Reihenfolge. Das zuletzt erzaehlte Stueck ist damit
rein rechnerisch `story(species).getOrNull(heard(context, species) - 1)` - ohne neue
SharedPreferences-Ablage.

**Luecke:** In `app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt` wird `toldNow`
(Zeile ~457, `var toldNow by remember { mutableStateOf(listOf<Int>()) }`) bei `onDismiss` auf
`emptyList()` zurueckgesetzt (Zeile ~2537). Wer das Gespraechsfenster schliesst und neu oeffnet
(Tap-Handler bei Zeile ~2291, `talkOpen = true`), sieht nichts mehr von dem, was zuletzt erzaehlt
wurde - auch dann nicht, wenn fuer heute nichts Neues mehr da ist (`onTell == null`,
`moreToTellLater`-Hinweis).

**Umsetzung:**
1. In `app-sim/src/main/java/com/notime/glyphsim/ui/PlayLore.kt`: neue oeffentliche Funktion
   `@StringRes fun lastToldPiece(context: Context, species: AvatarSpecies): Int?` ergaenzen - `null`
   wenn `heard(context, species) == 0`, sonst `story(species).getOrNull(heard(context, species) -
   1)`. Reiner Lesezugriff auf bestehende Werte, kein Schreibzugriff, keine neue Ablage.
2. In `DockScreen.kt`: an der Stelle, an der `talkOpen` auf `true` gesetzt wird (Zeile ~2291), VOR
   oder beim Oeffnen `toldNow` so setzen, dass es - nur falls `toldNow` gerade leer ist UND
   `PlayLore.lastToldPiece(context, species)` nicht `null` ist - mit `listOf(lastToldPiece)` startet
   statt mit `emptyList()`. Wird tatsaechlich neu erzaehlt (`onTell`), haengt sich das neue Stueck
   wie bisher an (Zeile ~2504, `toldNow = toldNow + piece`).
3. Keine Aenderung an `onDismiss`, `heard()`, `remember()`, `forget()`, `available()`, `hasMore()`,
   `hasMoreEver()`, `PIECES`, `unlockedBy()`, `nextPiece()`.

**Bedingungen:**
- Wurde noch NIE erzaehlt (`heard() == 0`), bleibt das Verhalten exakt wie bisher (leeres
  Gespraechsfenster ohne Lore-Text).
- `app-sim/src/test/java/com/notime/glyphsim/ui/PlayLoreTest.kt` muss unveraendert gruen bleiben;
  ergaenze dort einen neuen Testfall im bestehenden Stil (JUnit 4, deutschsprachiger
  Backtick-Testname) fuer `lastToldPiece`: `null` vor dem ersten Erzaehlen, und nach einem Aufruf
  von `remember()` liefert `lastToldPiece` genau das Stueck an Index `heard() - 1` aus `story()`.
- Keine Aenderung an Kalenderlogik, keine neue SharedPreferences-Datei oder -Key.

## [done] ITO-0011 - Eine weitere Beziehung zwischen zwei Wesen ergaenzen (vierte Runde)
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18), im selben Rahmen wie bereits ITO-0004 (PR #14: Puffling-Starlet),
ITO-0009 (PR #18: Wyrmling-Hootlet) und ITO-0010 (PR #29: Fennec-Gloop). CONTENT-Evolution, keine
Konstanten- oder Codeaenderung.

Seit ITO-0005 hat jedes Wesen acht Lore-Stuecke (`PlayLore.PIECES = 8`) - die Ankerzeile fuer
den neuen Satz darf Stueck 6, 7 ODER 8 sein.

Waehle GENAU EIN Wesen-Paar (A, B) aus den sechs Wesen (Puffling, Starlet, Wyrmling, Fennec,
Gloop, Hootlet), zwischen denen noch KEIN Satz in den bestehenden Lore-Texten eine Verbindung
herstellt. Bereits verbunden und NICHT erneut zu waehlen: Puffling-Gloop, Wyrmling-Fennec,
Starlet-Hootlet (die drei Hauptbeziehungen), Puffling-Starlet (ITO-0004), Wyrmling-Hootlet
(ITO-0009) sowie Fennec-Gloop (ITO-0010). Jedes andere Paar ist erlaubt, deine Wahl - neun Paare
stehen noch offen (z. B. Puffling-Wyrmling, Puffling-Fennec, Puffling-Hootlet, Starlet-Wyrmling,
Starlet-Fennec, Starlet-Gloop, Wyrmling-Gloop, Fennec-Hootlet, Gloop-Hootlet).

Ergaenze in `app-sim/src/main/res/values-de/strings.xml` UND `app-sim/src/main/res/values/strings.xml`
(Deutsch und Englisch, inhaltlich gleich) an GENAU EINER Stelle einen zusaetzlichen Satz: an
`lore_<A>_6`, `lore_<A>_7` ODER `lore_<A>_8` (ein Stueck deiner Wahl, bei EINEM der beiden
gewaehlten Wesen) - angehaengt an den bestehenden Text derselben Zeile, nicht als neue Zeile und
nicht als Ersatz des Bestehenden. Der neue Satz muss Wesen B beim Namen nennen und in der Stimme
von Wesen A geschrieben sein, ein bis zwei kurze Saetze, keine Ausrufezeichen, kein pathetischer
Ton - siehe die vorhandenen Stuecke aller sechs Wesen in denselben Dateien als Vorbild.

Bedingungen, die nicht verhandelbar sind:
- Keine bestehende Zeile darf geloescht, umbenannt oder inhaltlich veraendert werden - nur die
  eine gewaehlte Zeile bekommt einen angehaengten Satz.
- Der neue Satz darf keinem bestehenden Fakt in irgendeinem der 48 Lore-Stuecke widersprechen
  (Orte, Ereignisse, andere Beziehungen, einschliesslich der in ITO-0004, ITO-0009 und ITO-0010
  ergaenzten Verbindungen). Bei Zweifel: Wesen B nur beilaeufig erwaehnen, keine neue
  Tatsachenbehauptung ueber B aufstellen, die B's eigene Lore-Stuecke nicht schon stuetzen.
- Keine andere Datei aendern, insbesondere nicht `PlayLore.kt` oder `PlayLoreTest.kt` - beide
  pruefen nur Struktur (Anzahl, Eindeutigkeit), keine Wortlaute, und muessen unveraendert gruen
  bleiben.
- `MEDICINE`, Nutzerdaten oder Aussagen ueber den Nutzer duerfen in Lore-Text nicht vorkommen -
  Lore ist erfundenes Worldbuilding, keine Aussage ueber den Nutzer.

## [open] ITO-0013 - Ein weiteres Beiwerk-Requisit auf der Wiese ergaenzen
Bereits durch "Evolution Goals" in `EVOLUTION.md` gedeckt (Vielfalt der Szenen) - keine
`OPEN DECISION` betroffen, keine Rueckfrage noetig. Analog zu ITO-0007 (PR #44, Wald-Farn): genau
ein neues privates `Prop`, kein neuer Mechanismus, keine neue Station.

**Lehre aus ITO-0007:** Dort legte ein zu naiv gewaehlter `anchorX`-Wert (0.14) das neue
Requisit direkt in den Ruheplatz des Avatars (`avatarAnchorX(FOREST) = 0.06`) - die
Standardgestalt ueberzeichnete dort einen Grossteil der neuen Zellen (Codex-Fund auf PR #44),
und bei der kleinsten geprueften Breite (`MIN_SCENE_CELLS = 40`) gab es zwischen den bestehenden
Requisiten ohnehin keine freie Luecke. Diese Aufgabe verlangt deshalb ausdruecklich, den
gewaehlten `anchorX`-Wert VOR dem Commit gegen genau diese zwei Dinge selbst durchzurechnen.

**Ort:** `app-sim/src/main/java/com/notime/glyphsim/matrix/PlayScene.kt`, `Place.MEADOW` in
`furnishing()` (Zeile ~1140). Aktuell: `BUSH` (anchorX 0.10, `behind = true`), `TREE` (0.92,
`behind = true`), `FENCE` (0.02), `BENCH` (0.50, Station `BENCH`), `WILD_TUFT` (0.70),
`MUSHROOMS` (0.84). `avatarAnchorX(Place.MEADOW) = 0.20` (Funktion `avatarAnchorX`, Zeile ~505).

**Aufgabe:** Genau EIN neues privates `Prop` in derselben Datei ergaenzen (Wahl frei, z. B.
Butterblumen, ein einzelner Stein, ein Grasbuendel - solange es als niedriger
Bodenbewuchs/Beiwerk liest, Richtwert Hoehe <= 4, und keine der in `habitatPlacements()` bereits
vergebenen Landschaftsformen dupliziert: `ROCK`, `CRAG`, `ACACIA`, `REEDS`, `FLOWER` sind belegt,
ebenso die Namen `WILD_TUFT`/`MUSHROOMS`). Format wie `WILD_TUFT`/`MUSHROOMS` als Vorbild.

Das neue Prop als zusaetzliches `Placement(<NEUES_PROP>, anchorX = <Wert>)` OHNE `station` und
OHNE `behind = true` in die Liste unter `Place.MEADOW` einfuegen.

**Pflicht vor dem Commit:** Rechne `originX` (Formel in `originX()`, Zeile ~824: `shift + left +
round((span - width) * anchorX)`, mit `left = right = 0` fuer ein Placement ohne
`keepClearLeft`/`keepClearRight`) fuer den gewaehlten `anchorX`-Wert UND fuer FENCE/BENCH/
WILD_TUFT/MUSHROOMS von Hand fuer jede der von `SceneCompositionTest` gepruef­ten Breiten durch:
`PlayScene.MIN_SCENE_CELLS` (40), 46, `ScenePreview.WIDTH` (54); 72 und 96 sind wegen
`MAX_ROOM_CELLS = 60` fuer die Raumbreite mit 60 identisch. Der belegte Zellbereich
`[originX, originX + width)` des neuen Props darf sich bei KEINER dieser Breiten mit
FENCE/BENCH/WILD_TUFT/MUSHROOMS ueberschneiden, und sollte mit deutlichem Sicherheitsabstand vom
Avatar-Ruheplatz (`avatarAnchorX(Place.MEADOW) = 0.20`) entfernt bleiben - im Zweifel eher naeher
an FENCE (0.02) oder zwischen BENCH (0.50) und WILD_TUFT (0.70) planen als in Avatar-Naehe.

**Bedingungen:**
- Keine Aenderung an anderen `Place`-Zweigen, an `Acquisition`, an `Station`, an `groundDetail()`
  oder `habitatPlacements()`.
- Kein bestehendes Prop, keine bestehende `anchorX` veraendern.
- `SceneCompositionTest` muss unveraendert gruen bleiben, insbesondere `keine zwei Requisiten
  stehen ineinander` (`propFootprints` schliesst `behind = true`-Requisiten bewusst aus dem
  Vergleich aus - BUSH/TREE sind davon also nicht betroffen) und `an jedem Ort bleibt Platz zum
  Stehen`. Der Test iteriert automatisch ueber alle Orte/Spezies/Breiten, keine Testaenderung
  noetig.

## [open] ITO-0014 - Eine weitere Beziehung zwischen zwei Wesen ergaenzen (fuenfte Runde)
Freigegeben durch `EVOLUTION.md`, Abschnitt "Character Evolution" -> "Erzaehlerische Autonomie"
(Entscheidung vom 2026-08-18), im selben Rahmen wie bereits ITO-0004 (PR #14: Puffling-Starlet),
ITO-0009 (PR #18: Wyrmling-Hootlet), ITO-0010 (PR #29: Fennec-Gloop) und ITO-0011 (PR #47:
Puffling-Wyrmling). CONTENT-Evolution, keine Konstanten- oder Codeaenderung.

Seit ITO-0005 hat jedes Wesen acht Lore-Stuecke (`PlayLore.PIECES = 8`) - die Ankerzeile fuer
den neuen Satz darf Stueck 6, 7 ODER 8 sein.

Waehle GENAU EIN Wesen-Paar (A, B) aus den sechs Wesen (Puffling, Starlet, Wyrmling, Fennec,
Gloop, Hootlet), zwischen denen noch KEIN Satz in den bestehenden Lore-Texten eine Verbindung
herstellt. Bereits verbunden und NICHT erneut zu waehlen: Puffling-Gloop, Wyrmling-Fennec,
Starlet-Hootlet (die drei Hauptbeziehungen), Puffling-Starlet (ITO-0004), Wyrmling-Hootlet
(ITO-0009), Fennec-Gloop (ITO-0010) sowie Puffling-Wyrmling (ITO-0011). Jedes andere Paar ist
erlaubt, deine Wahl - acht Paare stehen noch offen (Puffling-Fennec, Puffling-Hootlet,
Starlet-Wyrmling, Starlet-Fennec, Starlet-Gloop, Wyrmling-Gloop, Fennec-Hootlet, Gloop-Hootlet).

Ergaenze in `app-sim/src/main/res/values-de/strings.xml` UND `app-sim/src/main/res/values/strings.xml`
(Deutsch und Englisch, inhaltlich gleich) an GENAU EINER Stelle einen zusaetzlichen Satz: an
`lore_<A>_6`, `lore_<A>_7` ODER `lore_<A>_8` (ein Stueck deiner Wahl, bei EINEM der beiden
gewaehlten Wesen) - angehaengt an den bestehenden Text derselben Zeile, nicht als neue Zeile und
nicht als Ersatz des Bestehenden. Der neue Satz muss Wesen B beim Namen nennen und in der Stimme
von Wesen A geschrieben sein, ein bis zwei kurze Saetze, keine Ausrufezeichen, kein pathetischer
Ton - siehe die vorhandenen Stuecke aller sechs Wesen in denselben Dateien als Vorbild.

Bedingungen, die nicht verhandelbar sind:
- Keine bestehende Zeile darf geloescht, umbenannt oder inhaltlich veraendert werden - nur die
  eine gewaehlte Zeile bekommt einen angehaengten Satz.
- Der neue Satz darf keinem bestehenden Fakt in irgendeinem der 48 Lore-Stuecke widersprechen
  (Orte, Ereignisse, andere Beziehungen, einschliesslich der in ITO-0004, ITO-0009, ITO-0010 und
  ITO-0011 ergaenzten Verbindungen). Bei Zweifel: Wesen B nur beilaeufig erwaehnen, keine neue
  Tatsachenbehauptung ueber B aufstellen, die B's eigene Lore-Stuecke nicht schon stuetzen.
- Keine andere Datei aendern, insbesondere nicht `PlayLore.kt` oder `PlayLoreTest.kt` - beide
  pruefen nur Struktur (Anzahl, Eindeutigkeit), keine Wortlaute, und muessen unveraendert gruen
  bleiben.
- `MEDICINE`, Nutzerdaten oder Aussagen ueber den Nutzer duerfen in Lore-Text nicht vorkommen -
  Lore ist erfundenes Worldbuilding, keine Aussage ueber den Nutzer.

## [open] ITO-0012 - Eine weitere allgemeine Bibliotheks-Animation ergaenzen (zweite Runde)
Bereits durch "Evolution Goals" in `EVOLUTION.md` gedeckt ("Vielfalt von Ambient-Aktivitaeten,
Routinen, Szenen, Reaktionen und charaktergerechten Dialogen") - keine `OPEN DECISION` betroffen,
keine Rueckfrage noetig. CONTENT-Evolution (reine Punktdaten, kein neuer Mechanismus). Setzt auf
ITO-0006 (PR #40: "Clock") auf, ist aber davon unabhaengig umsetzbar.

`core/src/main/java/com/notime/glyphcore/data/DefaultLibraryAnimations.kt` enthaelt inzwischen 27
allgemeine Animationen (`sortOrder` 0..25 plus 56 fuer "Clock", siehe `general()`). Diese Aufgabe
ist ein weiterer einzelner Schritt in dieselbe, bereits dokumentierte Richtung.

**Aufgabe:** Ergaenze GENAU EINE neue allgemeine Animation (keine charakterspezifische - die
liegen in `AvatarSignatureAnimations.kt` und sind nicht Teil dieser Aufgabe). Thema deiner Wahl,
passend zu einem Alltags- oder Erinnerungsthema (siehe die vorhandenen Labels in `general()` als
Anhaltspunkt fuer den Rahmen: Star, Wave, Rain, Music, Battery, Dog, Cat, Gift, Football, Fitness,
Robot, Trophy, Plant, Target, Airplane, Cake, Idea, Mail, Clock, ...). Vermeide ein Thema, das
einem bestehenden Label zu nahekommt.

**Zweite, GETRENNTE Kollisionsflaeche - nicht nur `general()` pruefen:** Ausser den
Bibliotheks-Labels gibt es die 12 fest eingebauten Erinnerungs-Animationen aus `enum class
AnimationType` (`core/src/main/java/com/notime/glyphcore/data/AnimationType.kt`), die im selben
Erinnerungs-Bildschirm neben den Bibliotheksanimationen zur Auswahl stehen: FOCUS 🎯, DRINK 💧,
MOVE 🏃, GENERAL 🔔, REST ☕, WORK 💻, MINDFULNESS 🧘, LOVE ❤️, SLEEP 🌙, MEDICINE 💊, BOOK 📖,
CREATIVITY 🎨 (Emoji-Zuordnung siehe `AnimationVisuals.kt` in `app-sim` und `app`). Ein
fruehstuecksbezogener "Kaffeetasse mit Dampf"-Entwurf wurde deshalb bereits dreimal vom Reviewer
abgelehnt (Laeufe 71-73 auf Commit `12b535e`): Emoji ☕ und Thema (Dampf/Ruhepause) dupliziert REST
nahezu 1:1. Waehle daher ein Thema, das WEDER einem `general()`-Label NOCH einem dieser 12 Typen
(weder Emoji noch offensichtliches Bildmotiv) zu nahekommt.

**Dritte, ebenfalls GETRENNTE Kollisionsflaeche - diesmal automatisiert als Testfehler, nicht erst
als Reviewer-Ablehnung:** `AvatarSignatureAnimationsTest.labelsAreUniqueAndDoNotClashWithTheGeneralSet`
(`core/src/test/java/com/notime/glyphcore/data/AvatarSignatureAnimationsTest.kt`) vergleicht ALLE
Labels aus `DefaultLibraryAnimations.seed()` - das ist laut Quelltext (Zeile 31) exakt
`general() + AvatarSignatureAnimations.seed()` - auf Duplikate. Ein neues `general()`-Label, das
zufaellig einem der 30 charakterspezifischen Labels in `AvatarSignatureAnimations.kt` gleicht,
lässt deshalb schon `./gradlew verify` (Task `:core:testDebugUnitTest`) rot werden, bevor der
Reviewer ueberhaupt drankommt - genau das ist Lauf 75 (2026-08-31, Commit `8c60e439`) passiert. Die
30 belegten Labels: Bubble, Butterfly, Kite, Snail, Compass, Comet, Lantern, Feather,
Constellation, Candle, Bolt, Summit, Ladder, Drum, Flag, Shield, Lighthouse, Paw, Nest, Anchor,
Turtle, Cloud, Drip, Puddle, Balloon, Eye, Key, Hourglass, Scroll, Puzzle. Das gewaehlte Label
muss also gegen DREI getrennte Listen geprueft werden: die `general()`-Labels oben, die 12
`AnimationType`-Werte, und diese 30 hier - nicht nur gegen eine davon.

**Technische Form** (siehe `starFrames()`/`waveFrames()`/`alarmClockFrames()` in derselben Datei
als Vorbild):
- Eine private Funktion `private fun <name>Frames(): List<List<Pair<Int, Int>>>` - eine
  Punktliste je Frame, Koordinaten im 13x13-Raster (Zentrum bei 6,6).
- Registrierung als neuer Eintrag in der Liste in `general()`: `LibraryAnimation(label = "...",
  emoji = "...", framesData = FrameCodec.encode(<name>Frames()), sortOrder = 57)`. NICHT 26: dieser
  Zahlenraum (26..55) ist durch `AvatarSignatureAnimations.kt` (`SORT_OFFSET = 26`, 30
  charakterspezifische Animationen) belegt. 57 ist der erste Wert, der weder dort noch in dieser
  Datei (hoechster bisheriger Wert hier: 56, "Clock") bereits vorkommt.
- Mindestens 4-6 Frames mit ZWEI erkennbaren Bewegungen, nicht nur einem pulsierenden Element -
  das war laut Klassendoku ein Kritikpunkt an frueheren Entwuerfen (siehe `starFrames()`: Strahlen
  pulsieren UND ein Twinkle-Punkt wandert reihum; `alarmClockFrames()`: Minutenzeiger dreht UND
  die Glocken wackeln abwechselnd).

**Drei dokumentierte Fallen, die `LibraryAnimationFitTest` automatisiert prueft und die den Lauf
rot werden lassen, wenn sie zutreffen** (siehe die KDoc dieser Testdatei fuer die volle
Begruendung):
1. Die Matrix ist rund - nur der einbeschriebene Kreis (`MatrixGeometry.isActive`) wird
   angezeigt, die vier Ecken des 13x13-Quadrats existieren nicht. Genau das ist "TAMA" passiert,
   das seine Buchstaben in die Ecken gesetzt hatte. Hoechstens 15 % der Punkte ueber alle Frames
   duerfen ausserhalb liegen.
2. Keine wilden Koordinaten - alle Punkte muessen im Bereich -4 bis 17 bleiben (etwas ueber den
   Rand hinaus ist als Gestaltungsmittel erlaubt, z. B. eine Rakete, die aus dem Bild steigt).
3. Punkte auf einer Linie oder gefuellte Flaechen koennen das Motiv unkenntlich machen (siehe
   `EVOLUTION.md`/README zu den Signatur-Animationen) - bevorzuge Umrisse und versetzte Punkte
   gegenueber vollstaendig gefuellten Formen.

**Vierte Falle, NICHT automatisiert pruefbar - zweimal in Folge (Laeufe 74 und 76) der
tatsaechliche Ablehnungsgrund, jeweils ohne jede Kollision:** Ein Motiv, das eine Form INNERHALB
eines selbst erfundenen Gehaeuses/Rahmens zeigt (z. B. eine Linse in einer Kamera, ein Zeiger in
einem Ziffernblatt), braucht mehr als "sieht ungefaehr richtig aus". Berechne fuer JEDEN
verwendeten Radius/jede verwendete Groesse die tatsaechlichen Randkoordinaten (z. B. das Ergebnis
von `circlePoints()` nach `roundToInt`) explizit und vergleiche sie Zahl fuer Zahl gegen die
selbst gewaehlten Gehaeusegrenzen, BEVOR du den Frame uebernimmst - nicht nur fuer den groessten
oder kleinsten Wert, sondern fuer jeden einzelnen in der Sequenz. Lauf 74 hatte eine Linse, die
bei radius=3 unterhalb der Gehaeuse-Unterkante lag; Lauf 76 hatte einen Iris-Ring, der bei
mehreren Radien ueber die Rechteck-Gehaeusegrenzen hinausragte, UND einen Blitzpunkt zu nah an
der Iris, wodurch die geforderten zwei erkennbaren Bewegungen optisch verschmolzen. Beides waren
vermeidbare Rechenfehler, keine Kollisionen. Findest du kein Motiv, dessen Geometrie du sauber
gegenrechnen kannst, waehle stattdessen ein freistehendes Motiv OHNE selbst erfundenes Gehaeuse
(wie `starFrames()`/`waveFrames()`/`batteryFrames()`) - das ist einfacher richtig zu bekommen als
Form-in-Form.

**Zusaetzlich Pflichtteil der Aufgabe, NICHT optional:** Die Klassendoku VON DERSELBEN DATEI nennt
an zwei Stellen die alte Anzahl und muss auf den neuen Stand gebracht werden - sonst widerspricht
die Doku dem Code, den sie beschreibt:
- Zeile 9: "27 Beispiel-Animationen" -> "28 Beispiel-Animationen".
- Zeile 27: "Die 27 allgemeinen Animationen plus die 30 charakterspezifischen" -> "Die 28
  allgemeinen Animationen plus die 30 charakterspezifischen".
Beides steht bereits im Umfang dieser Aufgabe (dieselbe Datei, keine neue Entscheidung) und ist
keine "andere Datei" im Sinne der folgenden Einschraenkung.

Aendere keine andere Datei, keine bestehende Animation, keinen `sortOrder`-Wert einer bestehenden
Animation. `LibraryAnimationFitTest` muss fuer die neue Animation ebenso gruen sein wie fuer die
bestehenden 27 (der Test iteriert automatisch ueber `DefaultLibraryAnimations.seed()`, keine
Testaenderung noetig).

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
