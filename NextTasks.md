# NextTasks.md

Priorisierte, unabhängige Arbeitspakete für Itoeva. Diese Datei enthält **maximal 15 aktuell
relevante Aufgaben**, geordnet nach tatsächlichem Hebel für belastbare Produktentscheidungen,
Entwicklungsgeschwindigkeit und Token-/Actions-Effizienz - alles Weitere steht kompakt im
[Future Backlog](#future-backlog) unten.
Grundlage ist die Analyse in [Architecture.md](Architecture.md); der Rahmen dafür steht in
[Vision.md](Vision.md) und [AgentGuide.md](AgentGuide.md).

**Wann diese Datei lesen:** nur wenn die aktuelle Aufgabe eine Prozessverbesserung ist, nicht bei
normalen Content-/Feature-Evolutionen aus `evolutions/BACKLOG.md` (siehe AgentGuide.md, Abschnitt
"Minimal-Startsequenz").

## Die eine Regel über allen anderen

Der Produktverantwortliche hat am 2026-09-10 das
[Itoeva Living Agent System](LIVING_AGENT.md) als naechsten groesseren Architektur-Meilenstein
ausdruecklich freigegeben. Die fruehere pauschale Sperre fuer neue groessere Gameplay-Systeme gilt
fuer diesen klar begrenzten Rahmen nicht mehr. Weiterhin gilt: kein monolithischer Umbau, keine
Quest-/Plotmaschine, keine zweite Renderpipeline und keine Cloud- oder Plattformarchitektur ohne
eigene Entscheidung. Jeder Schnitt muss klein, deterministisch testbar und einzeln ruecksetzbar
sein.

Der lokale Stream-Client NT-068 ist als Build-Variante derselben `:app-sim`-Runtime umgesetzt.
Der Messlauf NT-058 bleibt wichtig: Er soll nun nicht nur ein nachvollziehbares virtuelles Leben,
sondern auch Auto-Save und begrenzte Viewer-Impulse ueber einen laengeren Lauf beobachten.

## Top 15 - nach Hebel geordnet

Format je Aufgabe: **Rang - ID** - Aufgabe. *Erfolgskriterium.* (Abhaengigkeit; Aufwand)

1. **NT-058** - Begrenzten End-to-End-PoC fuer genau einen oeffentlichen Charakter durchfuehren:
   `:app-sim:assembleStream` im Emulator, Spielmodus dauerhaft sichtbar, Bild und App-Audio ueber OBS
   mindestens zwei Stunden lokal aufzeichnen. *`docs/streaming-poc.md` dokumentiert Stabilitaet,
   Vielfalt, Musik, Auto-Save, lokale Viewer-Impulse, erklaerbaren Agentenzustand,
   Ressourcenverbrauch und Neustartverhalten.* (NT-068; 120 min plus Beobachtungszeit)
   **Runbook und Messbogen sind fertig; der Lauf selbst braucht einen Menschen an einem Rechner
   mit Android Studio und OBS.** Eine Agentenumgebung hat weder SDK noch Emulator noch
   Aufzeichnung - siehe den Abschnitt "Warum NT-058 weiterhin offen ist" im Runbook.
2. **NT-051** - Pruefen, ob der Bereichsdetektor aus `verify.yml` auch der Builder-Session einen
   kleineren relevanten Kontext geben kann. *Machbarkeit bewertet, gegebenenfalls prototypisch
   umgesetzt.* (keine; 90 min)
3. **NT-046** - Empirisch pruefen, welchen Kontext die Reviewer-Session erhaelt. *Gemessene
   Tokenersparnis oder begruendete Beibehaltung.* (keine; 60 min)
4. **NT-047** - Standard-Modellwahl je Aufgabentyp pruefen. *Auswahl begruendet dokumentiert oder
   angepasst.* (keine; 45 min)
5. **NT-005** - Gradle-Abhaengigkeits-Caching in den Hauptworkflows pruefen. *Gemessene
   Laufzeitwirkung.* (keine; 60 min)
6. **NT-004** - Timeout-Werte aller Jobs pruefen und ergaenzen. *Jeder Job hat einen begruendeten
    Grenzwert.* (keine; 30 min)
7. **NT-002** - Klaeren, ob `runner/` noch gebraucht wird. *Doppelte Pflege ist begruendet
    beendet oder dokumentiert.* (menschliche Freigabe fuer Entfernen; 60 min)
8. **NT-029** - `README.md` in kurzen Einstieg und Themendokumente aufteilen. *Unter 300
    Zeilen, keine verlorenen Inhalte oder toten Links.* (keine; 90 min)
9. **NT-030** - Kurze Zustaendigkeitsnotiz je Gradle-Modul ergaenzen. *Drei Dateien unter je 50
    Zeilen.* (keine; 60 min)
10. **NT-009** - Flaky-Rate des Emulator-Jobs ueber die letzten 20 Laeufe messen. *Kennzahl und
    gegebenenfalls neue Schwelle dokumentiert.* (keine; 60 min)
11. **NT-050** - Tokenverbrauch der letzten zehn Evolutionslaeufe als Baseline dokumentieren.
    *Zahl, Quelle und Datum stehen in der Architektur-/Evolutionsdokumentation.* (keine; 45 min)

12. **NT-007** - Die durchschnittlichen Minutenkosten der letzten zehn abgeschlossenen
    `evolve`-Laeufe ermitteln. *Zahl, Stichprobe und Messdatum bilden eine belastbare Baseline
    fuer weitere Workflow-Entscheidungen.* (keine; 60 min)

13. **NT-008** - Die API-Level-Matrix gegen `minSdk` und `targetSdk` pruefen. *Unterstuetzte
    Geraete, instrumentierte CI-Abdeckung und bewusst offene Luecken sind dokumentiert.*
    (keine; 45 min)

14. **NT-010** - PLAY_STORE.md-Signierungsdokumentation gegen den `release`-Job abgleichen.
    *Dokumentation und tatsaechlicher Release-Ablauf widersprechen sich nicht.* (keine; 45 min)

15. **NT-011** - Selbsttest fuer `backlog-select.sh` ergaenzen. *Die Auswahl des obersten offenen
    Eintrags und die leere Warteschlange sind reproduzierbar belegt.* (keine; 60 min)

## Future Backlog

Kompakt, ohne volle Erfolgskriterien - wichtig, aber aktuell nicht unter den 15 mit dem größten
Hebel auf Geschwindigkeit/Effizienz. Wird eine dieser Aufgaben durch veränderte Umstände
hebelstärker als eine Top-15-Aufgabe, rückt sie beim nächsten Pflegedurchlauf nach (siehe unten).

**Build/CI, weitere:** NT-012 Gradle/AGP-
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

**Produktentscheidungen, offen:** NT-053 Animations-/Habit-Slots vor jedem Gameplay-Code als
strategische Mechanik spezifizieren und mit Nutzern oder einem schlanken Prototyp validieren;
Auswahlwirkung, Sichtbarkeit und die Entscheidung "jetzt einsetzen oder für später aufheben?"
müssen vor einer größeren Umsetzung belegt sein. · NT-055 Musik - *Format (Ogg/Vorbis), Ort der Umwandlung
(Pipeline), die Grenze der Ausnahme (nur der Score) sowie die kontextabhaengige Auswahl ueber
`MusicRole`/`MusicResolver` sind am 2026-09-05 entschieden und umgesetzt; siehe EVOLUTION.md.*
`main-day-01` und `home-evening-01` werden inzwischen ausgeliefert; Rollenwechsel sind seit der
nachfolgenden Musik-Evolution vier Sekunden lang ueberblendet und Sport setzt Ort plus echte
MOVE-Beschaeftigung voraus. **Offen bleibt:** (a) Lautheit, True Peak, lange Stille und
Loop-Grenzen als reproduzierbares Freigabe-Gate der Erzeugungspipeline definieren; (b) mehrere
gepruefte Varianten je Rolle ermoeglichen, ohne die Weltlogik an Dateinamen zu binden; (c) ein
gemeinsames musikalisches Leitmotiv festlegen, bevor Morgen-, Sport- und Traumtrack unabhaengig
auseinanderlaufen; (d) ob realistische generierte Musik ueber einer 16x16-Welt aesthetisch
richtig ist; (e) die Lizenzpruefung aus `music/README.md` vor einer kommerziellen
Veroeffentlichung.

**Öffentliche Streams, gestufte Roadmap:** NT-059 Ergebnis von NT-058 auswerten und ausdrücklich
zwischen drei Wegen entscheiden: Emulator-basierter Einzelbetrieb weiter testen, zuerst
`:app-sim` für Dauerbetrieb härten oder eine reine Weltengine untersuchen; ohne Messdaten keine
Auswahl. · NT-060 danach genau eine Charakterinstanz auf einem isolierten Cloud-Host mit
Checkpoint, Prozessaufsicht, Secret-Verwaltung und Gesundheitsprüfung erproben; Kosten pro
24 Stunden messen, noch keine Sechser-Flotte. · NT-061 erst nach stabilem Einzelbetrieb
parametrisierte Instanzen je Wesen und einen typisierten, auditierbaren Ereigniskanal für
Begegnungen spezifizieren; keine privaten Daten und kein ungeprüfter Freitext. · NT-062 vor einem
öffentlichen Simulcast die dann aktuellen YouTube-/Twitch-Regeln, Musikrechte, KI-Transparenz,
Moderation, Beschreibungstexte und dezente App-Hinweise als eigenes Freigabe-Gate prüfen.

**Welt und Tagesablauf, offen:** NT-056 **Begegnung waehrend eines Aufenthalts draussen** -
*am 2026-09-05 umgesetzt; siehe EVOLUTION.md.* Die Bedingung ist als `PlayVisitWindow.isOpen`
aus `DockScreen` herausgeloest und geprueft; ein Besuch ist waehrend eines `RoutineStep.Linger`
unter freiem Himmel moeglich, und der Ablauf wartet danach auf das Ende des Besuchs. **Offen
bleibt:** ob ein Besuch auch dann kommen darf, wenn die Figur draussen auf einer Bank SITZT
(`occupied`) - die laengsten Aussenpausen liegen genau dort, aber "wer sitzt, faellt heraus" war
eine bewusste Entscheidung und gehoert nicht nebenbei umgedreht. · NT-057
**Mindestdauer eines dynamischen Zustands** - *am 2026-09-06 umgesetzt; siehe EVOLUTION.md.* Die
Entwurfsfrage ist zugunsten des ZUSTANDS entschieden: `PlayOutdoorStay` haelt einen Aufenthalt
unter freiem Himmel mindestens neunzig Sekunden, und die Musik folgt daraufhin von selbst. Eine
Regel im Player haette das Symptom behandelt und ausserdem Ton und Bild entkoppelt. **Offen
bleibt:** ob neunzig Sekunden richtig sind - das ist eine Zahl und erst am Geraet zu beurteilen.

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
fortlaufender neuer ID (ab NT-058) direkt in den Future Backlog, nicht ungeprüft in die Top 15.
