# NextTasks.md

Priorisierte, unabhängige Arbeitspakete für Itoeva - jedes einzeln in 30-90 Minuten erledigbar,
mit klarem Erfolgskriterium und (wo vorhanden) Abhängigkeit zu einem anderen Eintrag. Grundlage
ist die Analyse in [Architecture.md](Architecture.md); der Rahmen dafür steht in
[Vision.md](Vision.md) und [AgentGuide.md](AgentGuide.md).

## Die eine Regel über allen anderen

**Keine neuen, größeren Gameplay-Features beginnen, bevor Build-Prozess, Testabdeckung,
Agentenfreundlichkeit und Tokenverbrauch der Entwicklung selbst spürbar besser sind.** Das
betrifft neue Systeme (Skillbaum, Quest-Struktur, neue Fortschrittspfade) - nicht bereits
begonnene, klar abgegrenzte Arbeit wie PR #20 (Speicherplätze), die zu Ende gebracht werden darf.
Inhaltliche Ergänzungen im Rahmen der bestehenden "Erzählerischen Autonomie" (weitere Beziehungen/
Lore, siehe EVOLUTION.md) sind von dieser Regel ausdrücklich **nicht** betroffen - sie sind klein,
unabhängig und bereits erlaubt.

Reihenfolge-Empfehlung: **A (Build/CI) vor B (Tests) vor C (Agentenfreundlichkeit) vor D
(Tokenverbrauch)**, weil spätere Kategorien von einem stabilen, gut getesteten Repository stärker
profitieren. Innerhalb einer Kategorie sind die Aufgaben weitgehend unabhängig und können parallel
von mehreren Evolutionsläufen bearbeitet werden, sofern keine Abhängigkeit vermerkt ist.

Format je Aufgabe: **ID** - Aufgabe. *Erfolgskriterium.* (Abhängigkeit; Aufwand)

## A. Build-Prozess & GitHub Actions verbessern

- **NT-001** - Acht leere Stimmungs-/Prozent-Dateien im Wurzelverzeichnis entfernen (`0%`, `16%`,
  `33%`, `50%`, `100%`, `gluecklich`, `hungrig`, `traurig`, `zufrieden`), Reste eines
  fehlgeschlagenen Shell-Kommandos. *`git rm`, Commit, `git status` sauber, kein Code verweist
  darauf.* (keine; 30 min)
- **NT-002** - Klären, ob `runner/` (PowerShell/Windows-Task-Scheduler) noch gebraucht wird oder
  von `claude-primary-run.yml` abgelöst ist. *Entscheidung in Architecture.md nachgetragen; falls
  obsolet, per PR entfernt (menschliche Freigabe nötig, Protected Area).* (keine; 60 min)
- **NT-003** - Prüfen, ob `claude-auth-smoke.yml`/`claude-builder-smoke.yml`
  (Machbarkeitstests) nach dem produktiven Einsatz von `claude-primary-run.yml` noch gebraucht
  werden. *Workflows begründet behalten oder entfernt.* (keine; 45 min)
- **NT-004** - Timeout-Werte aller Jobs in `verify.yml` prüfen/ergänzen, nicht nur beim
  `changes`-Job. *Jeder Job hat einen begründeten `timeout-minutes`-Wert.* (keine; 30 min)
- **NT-005** - Gradle-Abhängigkeits-Caching in `verify.yml`/`claude-primary-run.yml` prüfen und
  ggf. einrichten. *Gemessene Laufzeitverkürzung eines typischen Jobs dokumentiert.* (keine; 60 min)
- **NT-006** - `deliver-apk.yml` denselben Pfad-Filter wie `verify.yml` (PR #21) spendieren, falls
  es aktuell auch bei reinen Doku-Merges läuft. *Job überspringt sich nachweislich bei
  Text-only-Änderungen.* (keine, PR #21 als Vorlage; 45 min)
- **NT-007** - Durchschnittliche Minutenkosten eines `claude-primary-run.yml`-`evolve`-Jobs über
  die letzten 10 Läufe per Actions-API ermitteln. *Zahl mit Beleg in Architecture.md
  nachgetragen.* (keine; 45 min)
- **NT-008** - API-Level-Wahl der Emulator-Matrix (`:app` API 35, `:app-sim` API 26) gegen
  `minSdk`/`targetSdk` im Projekt prüfen. *Begründung dokumentiert oder Matrix angepasst.*
  (keine; 45 min)
- **NT-009** - Tatsächliche Flaky-Rate des Emulator-Jobs über die letzten 20 Läufe auswerten
  (Retry+KVM-Diagnose existiert bereits). *Kennzahl dokumentiert, Schwelle bei Bedarf angepasst.*
  (keine; 60 min)
- **NT-010** - `PLAY_STORE.md`-Signierungs-Doku gegen den tatsächlichen `release`-Job in
  `verify.yml` abgleichen. *Abweichungen gefunden+behoben oder Übereinstimmung bestätigt.*
  (keine; 45 min)
- **NT-011** - Kleinen Selbsttest für `runner/backlog-select.sh` ergänzen (mind. ein bekannter
  Fehlerfall: fehlender Status, doppelte ID). *Test schlägt bei simuliertem Fehlerfall sichtbar
  an.* (keine; 60 min)
- **NT-012** - Prüfen, ob `verify.yml` und `claude-primary-run.yml` konsistent dieselbe
  Gradle-/AGP-Version aus `gradle/libs.versions.toml` annehmen. *Bestätigt oder Abweichung
  behoben.* (keine; 30 min)
- **NT-013** - Actions-Minutenverbrauch der letzten 30 Tage nach Workflow aufschlüsseln und als
  Tabelle in Architecture.md ergänzen. *Tabelle mit Quelle/Datum vorhanden.* (keine; 60 min)
- **NT-014** - Aktuellen Stand von Branch Protection/Required Checks auf `main` nach dem Wechsel
  zu public prüfen (`runner/README.md` nennt das für privat als nicht erzwingbar). *Stand
  bestätigt und dokumentiert.* (keine; 30 min)

## B. Automatisierte Tests ausbauen

- **NT-015** - Erste Characterization-Test-Datei für `DockScreen.kt` (2925 Zeilen, bisher ohne
  eigene Testdatei vom Umfang von `HomeScreenCharacterizationTest.kt`) für die Kernabläufe (Zeit
  anzeigen, Zoom/Pan, Fütter-Kollision). *Mind. 3 Kernpfade abgedeckt, grün in CI.* (keine; 90 min)
- **NT-016** - Erste Compose-Testfälle für `app/ui/ReminderScreen.kt` (bisher nur
  `StringResourceParityTest`), analog zu `app-sim/ReminderScreenTest.kt`. *Mind. 3 Testfälle,
  grün.* (keine; 90 min)
- **NT-017** - Migrationstest so erweitern, dass er über ALLE exportierten Schema-Versionen
  iteriert statt nur die neueste zu prüfen, für `app` und `app-sim`. *Test schlägt nachweislich
  fehl, wenn ein Schema-Sprung ohne passende Migration simuliert wird.* (keine; 90 min)
- **NT-018** - Automatisierten Test für "alle vier Speicherplätze belegt, Erinnerung läuft
  ungenutzt aus" ergänzen (in PR #20 nur als manueller Testplan-Schritt vorhanden). *Test ersetzt
  den manuellen Schritt.* (PR #20 gemerged; 60 min)
- **NT-019** - Test für die TalkBack-Zusatzaktionen der Speicherplätze
  (`a11y_action_slot_*`-Strings) ergänzen. *Test prüft Vorhandensein und Wirkung der
  Custom-Accessibility-Actions.* (PR #20 gemerged; 60 min)
- **NT-020** - Prüfen, ob jede der sechs Spezies in `AvatarAnimations.kt` einen eigenen benannten
  Testfall hat, nicht nur eine gemeinsame `AvatarAnimationsTest.kt`. *Lücke identifiziert und für
  fehlende Spezies geschlossen.* (keine; 60 min)
- **NT-021** - Prüfen, ob jeder der vier `PlayPath`-Fortschrittspfade einen eigenen Testfall in
  `PlaySceneTest.kt`/`SceneCompositionTest.kt` hat. *Lücken dokumentiert oder geschlossen.*
  (keine; 60 min)
- **NT-022** - Mindestens einen Golden-/Snapshot-Test für eine gerenderte `PlayScene.kt`-Szene
  ergänzen (bisher nur Struktur-, keine visuelle Prüfung erkennbar). *Test existiert und schlägt
  bei absichtlicher Änderung sichtbar an.* (keine; 90 min)
- **NT-023** - Prüfen, ob `ReminderWatchdogWorker` (core) einen echten WorkManager-
  Instrumentierungstest hat, nicht nur reine Logiktests. *Lücke bestätigt oder geschlossen.*
  (keine; 60 min)
- **NT-024** - `StringResourceParityTest`-Familie (core/app/app-sim) um einen Vergleich der
  Platzhalter-Reihenfolge (`%1$s`, `%2$d`) zwischen `values/` und `values-de/` erweitern.
  *Mit einem absichtlich kaputten Testfall verifiziert.* (keine; 60 min)
- **NT-025** - Unit-Test für `archiveActiveReminderIfExpired()` (PR #20) inkl. Randfall "alle vier
  Plätze belegt" auf Logik-Ebene statt nur UI-Ebene ergänzen. (PR #20 gemerged; 60 min)
- **NT-026** - Langsamste Testklassen per `--profile` messen, Top-5-Liste als
  Parallelisierungs-Kandidaten dokumentieren. *Liste vorhanden.* (keine; 45 min)
- **NT-027** - Property-based-Test für `PlayGamePlan` ergänzen: über viele zufällige Seeds prüfen,
  dass zufällige Spielpläne nie Medizin enthalten (laut EVOLUTION.md Pflichtregel). *Test läuft
  mit z. B. 1000 Seeds durch.* (keine; 60 min)
- **NT-028** - `ReminderSchedulerTimeZoneTest` um einen Fall für Zeitzonenwechsel WÄHREND einer
  bereits laufenden Erinnerung ergänzen (nicht nur beim Planen). *Lücke bestätigt oder Test
  ergänzt.* (keine; 60 min)

## C. Entwicklung agentenfreundlicher machen

- **NT-029** - `README.md` (829 Zeilen) in einen kurzen Einstieg plus themenspezifische Dateien
  aufteilen (z. B. `docs/persistence.md`, `docs/release-process.md`). *`README.md` unter 300
  Zeilen, alle Inhalte weiterhin auffindbar, keine toten Links.* (keine; 90 min)
- **NT-030** - Kurze Zuständigkeits-Notiz je Gradle-Modul ergänzen ("was gehört hierher, was
  nicht") für `core`, `app`, `app-sim`. *Drei Dateien, je unter 50 Zeilen.* (keine; 60 min)
- **NT-031** - `PlayScene.kt` nach Verantwortungsbereich aufteilen (z. B. Requisiten-Katalog von
  Aufbaulogik trennen), reine Verschiebung ohne Verhaltensänderung. *Teil-Dateien < 1000 Zeilen,
  alle bestehenden Tests weiterhin grün.* (keine; 90 min)
- **NT-032** - `DockScreen.kt` analog aufteilen (z. B. Gesten-/Zoom-Logik von Rendering trennen).
  *Wie NT-031.* (keine; 90 min)
- **NT-033** - Dokumentieren, warum `:app-sim` eine zweite, von `core` unabhängige Room-Datenbank
  führt. *Begründung in `AppDatabase.kt`-Kommentar und/oder Architecture.md.* (keine; 45 min)
- **NT-034** - Recherche (ohne Verschiebung): wie viel von `app/ui/ReminderScreen.kt` und
  `app-sim/ui/ReminderScreen.kt` ist tatsächlich identisch genug für `core` oder ein gemeinsames
  Modul? *Klare Ja/Nein/Teilweise-Antwort mit Beispielen in Architecture.md.* (keine; 60 min)
- **NT-035** - Dieselbe Recherche für `app/glyph/ReminderAnimations.kt` vs.
  `app-sim/matrix/ReminderAnimations.kt`. *Wie NT-034.* (keine; 45 min)
- **NT-036** - `SettingsCatalog.kt` auf durchgängige Zweck-Kommentare je Eintrag prüfen (Vorbild:
  `UsedActionSlot`). *Jeder Eintrag hat einen Kommentar oder ist offensichtlich
  selbsterklärend.* (keine; 45 min)
- **NT-037** - Prüfen, ob `AgentGuide.md` als alleinige Agenten-Kurzreferenz ausreicht oder ein
  zusätzlicher Standard-Dateiname sinnvoll ist, falls künftig auch andere Tools als Claude Code
  hier arbeiten. *Bewusste Entscheidung dokumentiert.* (keine; 30 min)
- **NT-038** - `runner/schemas/*.json` gegen das tatsächliche Prompt-Format in
  `claude-primary-run.yml` abgleichen, mögliche Verwaisung feststellen. (NT-002; 45 min)
- **NT-039** - Nach jeder größeren Änderung an Vision/Architecture/EVOLUTION/NextTasks/AgentGuide:
  gegenseitige Querverweise auf Korrektheit prüfen. *Alle Verweise stimmen.* (keine,
  wiederkehrend; 30 min)
- **NT-040** - `evolutions/001-idempotent-xp.md` gegen das aktuelle `BACKLOG.md`-Format prüfen -
  behalten, archivieren oder umformatieren? *Entscheidung dokumentiert.* (keine; 30 min)
- **NT-041** - Kommentardichte/-stil in `PlayScene.kt`, `DockScreen.kt`, `HomeScreen.kt`
  stichprobenhaft gegen die Stilregeln in AgentGuide.md prüfen - diese Dateien werden am
  häufigsten von Agenten verändert. *Stichprobenergebnis dokumentiert, Lücken geschlossen.*
  (keine; 60 min)
- **NT-042** - Fehlende `@Preview`-Compose-Previews für die wichtigsten Bildschirme
  (`HomeScreen`, `DockScreen`) ergänzen - verkürzt die Einschätzungszeit für UI-Änderungen ohne
  vollen App-Build. *Lückenliste erstellt, mind. für HomeScreen/DockScreen ergänzt.* (keine; 90 min)

## D. Tokenverbrauch senken

- **NT-043** - Prompt-Anteile in `claude-primary-run.yml` (1786 Zeilen) identifizieren und nach
  dem Muster von `runner/prompts/*.md` in separate, bei Bedarf geladene Dateien auslagern.
  *Gemessener Anteil dokumentiert, mind. eine Auslagerung testweise umgesetzt.* (keine; 90 min)
- **NT-044** - Prüfen, ob die Builder-Session bei jedem Lauf das komplette `README.md`/
  `EVOLUTION.md` laden muss oder ein kompakterer Auszug reicht. *Gemessener Unterschied im
  Prompt-Umfang, Empfehlung dokumentiert.* (NT-029 hilft direkt; 60 min)
- **NT-045** - Erledigte (`[done]`)-Einträge aus `evolutions/BACKLOG.md` (25 KB) nach einer
  gewissen Zeit in ein Archiv (`BACKLOG-ARCHIVE.md`) auslagern, damit nicht bei jedem Lauf die
  komplette Historie geladen wird. *Umgesetzt oder bewusst verworfen mit Begründung.* (keine; 60 min)
- **NT-046** - Prüfen, ob die Reviewer-Session denselben vollen Kontext wie die Builder-Session
  bekommt, obwohl sie nur den Diff bewerten muss. *Gemessene Tokenersparnis pro Lauf.* (keine; 60 min)
- **NT-047** - Standard-Modellwahl (`model`/`review_model` in `claude-primary-run.yml`) gegen
  Aufgabentyp prüfen - läuft standardmäßig ein teureres Modell für einfache Aufgaben (z. B. reine
  Lore-Ergänzung)? *Standardwahl begründet dokumentiert oder angepasst.* (keine; 45 min)
- **NT-048** - `runner/prompts/*.md` gegen die tatsächlichen Prompts in `claude-primary-run.yml`
  auf Redundanz prüfen (zwei parallele Automatisierungswege, siehe Architecture.md). *Redundanz
  bestätigt/aufgelöst.* (NT-002; 45 min)
- **NT-049** - Für wiederkehrende, mechanische Backlog-Aufgaben (z. B. "ein weiteres Lore-Stück
  ergänzen") ein kompakteres, strukturierteres Aufgabenformat in `BACKLOG.md` prototypisch
  erproben. *Ein Beispiel-Eintrag im neuen Format erstellt, Größenvergleich dokumentiert.*
  (keine; 60 min)
- **NT-050** - Durchschnittlichen Tokenverbrauch je Evolutionslauf der letzten 10 Läufe als
  Baseline in Architecture.md/EVOLUTION.md festhalten, als Vergleichswert für NT-043 bis NT-049.
  *Baseline-Zahl mit Quelle dokumentiert.* (keine; 45 min)
- **NT-051** - Prüfen, ob sich der "Betroffene Bereiche bestimmen"-Job aus `verify.yml` (PR #21)
  auch für die Builder-Session eignet, um ihr vorab zu sagen, welche Bereiche für die aktuelle
  Aufgabe relevant sind, statt das gesamte Repository zu scannen. *Machbarkeit bewertet, ggf.
  prototypisch umgesetzt.* (keine; 90 min)
- **NT-052** - Wiederkehrende Datei-Header-Boilerplate (falls vorhanden) auf unnötige
  Wiederholung über viele Dateien hinweg prüfen - summiert sich bei einem Volltext-Scan durch
  einen Agenten. *Bestand geprüft, ggf. vereinheitlicht.* (keine; 30 min)

## Pflege dieser Liste

Erledigte Aufgaben werden nicht gelöscht, sondern mit `[erledigt]` markiert und dürfen nach einer
Weile in ein Archiv wandern (siehe NT-045-Muster) - dieselbe Logik wie bei
`evolutions/BACKLOG.md`. Neue Aufgaben, die während der Arbeit an einer bestehenden entdeckt
werden, gehören mit fortlaufender ID an das Ende der jeweiligen Kategorie, nicht dazwischen -
bestehende IDs bleiben damit stabile Referenzen.
