# NextTasks.md

Priorisierte, unabhängige Arbeitspakete für Itoeva. Diese Datei enthält **maximal 15 aktuell
relevante Aufgaben**, geordnet nach tatsächlichem Hebel für Entwicklungsgeschwindigkeit und
Token-/Actions-Effizienz - alles Weitere steht kompakt im [Future Backlog](#future-backlog) unten.
Grundlage ist die Analyse in [Architecture.md](Architecture.md); der Rahmen dafür steht in
[Vision.md](Vision.md) und [AgentGuide.md](AgentGuide.md).

**Wann diese Datei lesen:** nur wenn die aktuelle Aufgabe eine Prozessverbesserung ist, nicht bei
normalen Content-/Feature-Evolutionen aus `evolutions/BACKLOG.md` (siehe AgentGuide.md, Abschnitt
"Minimal-Startsequenz").

## Die eine Regel über allen anderen

**Keine neuen, größeren Gameplay-Features beginnen, bevor Build-Prozess, Testabdeckung,
Agentenfreundlichkeit und Tokenverbrauch der Entwicklung selbst spürbar besser sind.** Das
betrifft neue Systeme (Skillbaum, Quest-Struktur, neue Fortschrittspfade) - nicht bereits
begonnene, klar abgegrenzte Arbeit wie PR #20 (Speicherplätze), die zu Ende gebracht werden darf.
Inhaltliche Ergänzungen im Rahmen der bestehenden "Erzählerischen Autonomie" (weitere Beziehungen/
Lore, siehe EVOLUTION.md) sind von dieser Regel ausdrücklich **nicht** betroffen.

## Top 15 - nach Hebel geordnet

Format je Aufgabe: **Rang - ID** - Aufgabe. *Hebel:* warum das gerade jetzt am meisten bringt.
*Erfolgskriterium.* (Abhängigkeit; Aufwand)

**Vor jeder Aufgabe aus dem Bereich Tokenverbrauch (NT-045 bis NT-050 unten):** erst empirisch
feststellen, was tatsächlich in den Modellkontext gelangt, nicht von Dateigröße oder vermuteter
Quelle ausgehen - siehe AgentGuide.md, Stilregeln, und das Beispiel in Architecture.md (frühere
NT-043-Annahme, korrigiert in PR #23).

1. **NT-045** - Erledigte (`[done]`)-Einträge aus `evolutions/BACKLOG.md` (25 KB) nach einer
   gewissen Zeit in ein Archiv (`BACKLOG-ARCHIVE.md`) auslagern. *Hebel: die Datei wächst mit
   jeder Evolution weiter und wird sonst bei jedem Lauf komplett mitgeladen.* *Umgesetzt oder
   bewusst verworfen mit Begründung.* (keine; 60 min)
2. **NT-051** - Prüfen, ob sich der "Betroffene Bereiche bestimmen"-Job aus `verify.yml` (PR #21)
   auch für die Builder-Session eignet, um ihr vorab zu sagen, welche Bereiche für die aktuelle
   Aufgabe relevant sind, statt das gesamte Repository zu scannen. *Hebel: spart Scan-Overhead bei
   jedem einzelnen Lauf, Muster hat sich in PR #21/#22 bereits bewährt.* *Machbarkeit bewertet,
   ggf. prototypisch umgesetzt.* (keine; 90 min)
3. **NT-046** - Prüfen, ob die Reviewer-Session denselben vollen Kontext wie die Builder-Session
   bekommt, obwohl sie nur den Diff bewerten muss. *Hebel: zweite Session pro Lauf, doppelter
   Effekt jeder Einsparung.* *Gemessene Tokenersparnis pro Lauf.* (keine; 60 min)
4. **NT-047** - Standard-Modellwahl (`model`/`review_model` in `claude-primary-run.yml`) gegen
   Aufgabentyp prüfen - läuft standardmäßig ein teureres Modell für einfache Aufgaben (z. B. reine
   Lore-Ergänzung)? *Hebel: direkter Kostenhebel unabhängig vom Tokenumfang.* *Standardwahl
   begründet dokumentiert oder angepasst.* (keine; 45 min)
5. **NT-005** - Gradle-Abhängigkeits-Caching in `verify.yml`/`claude-primary-run.yml` prüfen und
   ggf. einrichten. *Hebel: beschleunigt JEDEN CI-Lauf, unabhängig vom Änderungstyp.* *Gemessene
   Laufzeitverkürzung eines typischen Jobs dokumentiert.* (keine; 60 min)
6. **NT-004** - Timeout-Werte aller Jobs in `verify.yml` prüfen/ergänzen, nicht nur beim
   `changes`-Job. *Hebel: ein hängender Job ohne Timeout kann überproportional viel Zeit
   verbrauchen, geringer Aufwand.* *Jeder Job hat einen begründeten `timeout-minutes`-Wert.*
   (keine; 30 min)
7. **NT-006** - `deliver-apk.yml` denselben Pfad-Filter wie `verify.yml` (PR #21) spendieren,
   falls es aktuell auch bei reinen Doku-Merges läuft (wie diesem PR gerade). *Hebel: analoger,
   bereits bewiesener Effekt wie bei `verify.yml`.* *Job überspringt sich nachweislich bei
   Text-only-Änderungen.* (PR #21 als Vorlage; 45 min)
8. **NT-002** - Klären, ob `runner/` (PowerShell/Windows-Task-Scheduler) noch gebraucht wird oder
   von `claude-primary-run.yml` abgelöst ist. *Hebel: beendet doppelte Pflege und Verwirrung -
   jeder Agent, der beide Systeme prüft, verliert dabei Zeit und Kontext.* *Entscheidung in
   Architecture.md nachgetragen; falls obsolet, per PR entfernt (menschliche Freigabe nötig,
   Protected Area).* (keine; 60 min)
9. **NT-029** - `README.md` (829 Zeilen) in einen kurzen Einstieg plus themenspezifische Dateien
   aufteilen (z. B. `docs/persistence.md`, `docs/release-process.md`). *Hebel: senkt strukturell
   die Kontextgröße, die für eine "normale Aufgabe" laut AgentGuide.md nötig ist.* *`README.md`
   unter 300 Zeilen, alle Inhalte weiterhin auffindbar, keine toten Links.* (keine; 90 min)
10. **NT-030** - Kurze Zuständigkeits-Notiz je Gradle-Modul ergänzen ("was gehört hierher, was
    nicht") für `core`, `app`, `app-sim`. *Hebel: Agent kann Modul-Zugehörigkeit ohne Volltextsuche
    klären.* *Drei Dateien, je unter 50 Zeilen.* (keine; 60 min)
11. **NT-009** - Tatsächliche Flaky-Rate des Emulator-Jobs über die letzten 20 Läufe auswerten
    (Retry+KVM-Diagnose existiert bereits). *Hebel: weniger Retries bedeuten schnelleren
    PR-Turnaround.* *Kennzahl dokumentiert, Schwelle bei Bedarf angepasst.* (keine; 60 min)
12. **NT-003** - Prüfen, ob `claude-auth-smoke.yml`/`claude-builder-smoke.yml`
    (Machbarkeitstests) nach dem produktiven Einsatz von `claude-primary-run.yml` noch gebraucht
    werden. *Hebel: räumt ungenutzte Läufe/Dateien auf, geringer Aufwand.* *Workflows begründet
    behalten oder entfernt.* (keine; 45 min)
13. **NT-050** - Durchschnittlichen Tokenverbrauch je Evolutionslauf der letzten 10 Läufe als
    Baseline dokumentieren. *Hebel: ohne Vorher-Wert lässt sich die Wirkung von NT-045 bis NT-047
    nicht belegen - Messinstrument für den gesamten Fokusbereich.* *Baseline-Zahl mit Quelle in
    Architecture.md oder EVOLUTION.md dokumentiert.* (keine; 45 min)
14. **NT-013** - Actions-Minutenverbrauch der letzten 30 Tage nach Workflow aufschlüsseln.
    *Hebel: Geschwister-Messinstrument zu NT-050, aber für Actions-Zeit statt Tokens - ohne diese
    Zahl bleibt unklar, welcher Workflow tatsächlich die meiste Zeit kostet.* *Tabelle mit
    Quelle/Datum in Architecture.md ergänzt.* (keine; 60 min)
15. **NT-053** - Animations-/Habit-Slots vor jedem Gameplay-Code als strategische Mechanik
    spezifizieren und mit Nutzern oder einem schlanken Prototyp validieren: Auswahlwirkung,
    Sichtbarkeit/mentale Präsenz und die Entscheidung "jetzt einsetzen oder für später aufheben?".
    *Hebel: verhindert, dass Slot-Anzahl oder Umsetzung festgeschrieben werden, bevor der
    Kernnutzen belegt ist.* *Validierte Mechanikbeschreibung mit offenen Entscheidungen und
    klaren Kriterien für einen späteren Implementierungs-PR.* (Vision.md; 90 min)

## Future Backlog

Kompakt, ohne volle Erfolgskriterien - wichtig, aber aktuell nicht unter den 15 mit dem größten
Hebel auf Geschwindigkeit/Effizienz. Wird eine dieser Aufgaben durch veränderte Umstände
hebelstärker als eine Top-15-Aufgabe, rückt sie beim nächsten Pflegedurchlauf nach (siehe unten).

**Build/CI, weitere:** NT-007 Ø-Minutenkosten eines `evolve`-Jobs ermitteln · NT-008
API-Level-Matrix gegen minSdk/targetSdk prüfen · NT-010 PLAY_STORE.md-Signierungsdoku gegen
`release`-Job abgleichen · NT-011 Selbsttest für `backlog-select.sh` · NT-012 Gradle/AGP-
Versionskonsistenz zwischen Workflows · NT-014 Branch-Protection-Stand nach Public-Wechsel
prüfen · NT-001 acht leere Stimmungs-/Prozent-Dateien im Wurzelverzeichnis entfernen.

**Tests ausbauen (alle zurückgestellt, nicht unwichtig - siehe Hinweis unten):** NT-015
Characterization-Tests für `DockScreen.kt` · NT-016 Compose-Tests für `app/ui/ReminderScreen.kt`
· NT-017 Migrationstest über alle Schema-Versionen · NT-018 Test "alle 4 Speicherplätze belegt"
(PR #20) · NT-019 Test TalkBack-Zusatzaktionen Speicherplätze (PR #20) · NT-020
Spezies-Testabdeckung `AvatarAnimationsTest` · NT-021 `PlayPath`-Testabdeckung `PlaySceneTest` ·
NT-022 Golden-/Snapshot-Test für eine `PlayScene`-Szene · NT-023 Instrumentierungstest
`ReminderWatchdogWorker` · NT-024 Platzhalter-Konsistenz in `StringResourceParityTest` · NT-025
Unit-Test `archiveActiveReminderIfExpired()` (PR #20) · NT-026 Langsamste Testklassen per
`--profile` ermitteln · NT-027 Property-Test `PlayGamePlan` schließt Medizin aus · NT-028
Zeitzonenwechsel während laufender Erinnerung.

**Agentenfreundlichkeit, weitere:** NT-031 `PlayScene.kt` aufteilen · NT-032 `DockScreen.kt`
aufteilen · NT-033 Zweite Room-Datenbank in `app-sim` begründen · NT-034 Recherche
`ReminderScreen`-Duplikat hebbar? · NT-035 Recherche `ReminderAnimations`-Duplikat hebbar? ·
NT-036 Zweck-Kommentare `SettingsCatalog.kt` vervollständigen · NT-037 `AgentGuide.md` vs.
Standard-Dateiname klären · NT-038 `runner/schemas/*.json` gegen `claude-primary-run.yml`
abgleichen · NT-039 Querverweise zwischen Doku-Dateien prüfen (wiederkehrend) · NT-040
`evolutions/001-idempotent-xp.md` gegen Backlog-Format prüfen · NT-041 Kommentarstil in den drei
größten Dateien stichprobenhaft prüfen · NT-042 Fehlende `@Preview` für HomeScreen/DockScreen ·
NT-054 Skills, Level und Jahreszeiten als gewichtete Erweiterungen des in `Tagesablauf.md`
definierten Systems spezifizieren und validieren, bevor dazu größerer Gameplay-Code entsteht.

**Tokenverbrauch, weitere:** NT-048 `runner/prompts/*.md` gegen `claude-primary-run.yml` auf
Redundanz prüfen · NT-049 Kompakteres Backlog-Format für mechanische Aufgaben erproben · NT-052
Wiederkehrende Datei-Header-Boilerplate prüfen.

**Hinweis zu den zurückgestellten Tests (NT-015 bis NT-028):** Diese Aufgaben sind nicht
unwichtig - sie betreffen Korrektheit und Datensicherheit (siehe Vision.md: kein Cloud-Backup),
nicht Geschwindigkeit/Effizienz. Sie folgen der Top-15-Liste, sobald deren wichtigste Punkte
erledigt sind, statt hier um denselben Hebel-Maßstab zu konkurrieren, der für sie nicht das
richtige Kriterium ist.

## Pflege dieser Liste

IDs bleiben stabile Referenzen, auch nach dem Verschieben zwischen Top 15 und Future Backlog.
Wird eine Top-15-Aufgabe erledigt, rückt die höchste noch offene Future-Backlog-Aufgabe nach dem
Hebel-Maßstab oben in die Top 15 nach - die Liste bleibt dadurch dauerhaft auf maximal 15
Einträge begrenzt. Neue Aufgaben, die während der Arbeit entdeckt werden, kommen mit
fortlaufender neuer ID (ab NT-054) direkt in den Future Backlog, nicht ungeprüft in die Top 15.
