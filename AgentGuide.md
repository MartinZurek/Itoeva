# AgentGuide.md

Kurzreferenz für KI-Agenten, die an diesem Repository arbeiten. Ersetzt keinen der anderen
Dokumente, sondern verweist auf sie: [Vision.md](Vision.md) für das Warum,
[Architecture.md](Architecture.md) für das Wie, [EVOLUTION.md](EVOLUTION.md) für das
verbindliche Regelwerk samt Entscheidungsprotokoll, [NextTasks.md](NextTasks.md) für konkrete
Arbeitspakete, [CLAUDE.md](CLAUDE.md) für die knappen Arbeitsweise-Regeln dieses Repos und
[CLOUD_CODE_BRIEFING.md](CLOUD_CODE_BRIEFING.md) für den aktuellen, agentenübergreifenden
Produktkontext.

**Bei Widerspruch gilt EVOLUTION.md.** Es ist das einzige Dokument mit dem Anspruch, ein
verbindliches Protokoll zu sein - alle anderen hier sind Orientierung.

## Minimal-Startsequenz

Für eine normale Aufgabe müssen NICHT alle verlinkten Dokumente vollständig gelesen werden. Diese
Liste ist bewusst kurz gehalten, damit sie das auch bleibt:

1. **Dieses Dokument (`AgentGuide.md`) vollständig** - kurz, ist der Router zu allem Weiteren.
2. **`CLAUDE.md` vollständig** - kurz, grundlegende Arbeitsweise-Regeln.
3. **`CLOUD_CODE_BRIEFING.md` vollständig** - kurze Übergabe der aktuellen Produktvision und
   Automatisierungsrichtung; keine Arbeitsliste und keine pauschale Implementierungsfreigabe.
4. **Die eine konkrete Aufgabe**, nicht die ganze Quelle drumherum:
   - Aus `evolutions/BACKLOG.md`: nur den einen betroffenen `## [open] ITO-NNNN`-Eintrag lesen,
     nicht die Datei linear von oben.
   - Beim Tagesablauf-Dauerauftrag: `evolutions/DAILY_LIFE_TASK.md` und
     `evolutions/DAILY_LIFE_LEARNING.md` lesen; danach nur den gewählten Hebel erkunden.
   - Aus `NextTasks.md`: nur die Top-15-Liste, nicht den Future Backlog - außer die gewählte
     Aufgabe stammt explizit von dort.
5. **Alles Weitere nur nach den Regeln im nächsten Abschnitt** - nicht routinemäßig "sicherheits-
   halber" mitlesen. Ungezielt der ganze Satz an Dokumenten kostet bei jedem Lauf Kontext, ohne
   dass er meistens gebraucht wird.

## Wann zusätzlich welches Dokument lesen

- **Vision.md**: nur wenn die Aufgabe Produkterlebnis, UX, Ton/Sprache eines Charakters,
  Spielmechanik oder eine mögliche Grundsatzentscheidung berührt. Für rein technische Aufgaben
  (CI, Tests, Refactoring ohne Verhaltensänderung) nicht nötig.
- **Architecture.md**: nur wenn die Aufgabe Modulgrenzen, Datenfluss oder eine der dort gelisteten
  großen/duplizierten Dateien betrifft. Zuerst das Inhaltsverzeichnis am Dateianfang prüfen und
  gezielt nur den relevanten Abschnitt lesen, nicht die ganze Datei von oben nach unten.
- **EVOLUTION.md**: die Abschnitte "Non-Negotiable Design Principles" und "Protected Areas" nur
  bei Unsicherheit, ob die Aufgabe eine Grundsatzentscheidung berührt. Den Abschnitt
  "Erzählerische Autonomie", wenn es um neue Beziehungen/Lore geht. Die volle "Evolution
  History" nur, wenn ein neuer Historieneintrag geschrieben werden muss - dann reichen die
  letzten ein bis zwei Einträge als Formatvorlage, nicht die gesamte Historie.
- **NextTasks.md**: nur wenn die aktuelle Aufgabe selbst eine Prozessverbesserung ist, nicht bei
  normalen Content-/Feature-Evolutionen aus `BACKLOG.md`.
- **Tagesablauf.md** und **`evolutions/DAILY_LIFE_LEARNING.md`**: bei dem automatisierten
  Tagesablauf-Dauerauftrag oder wenn Aktivitaetswahl, Uebergaenge, Wiedereinstieg,
  Charakterunterschiede bzw. die sichtbare Wirkung von Remindern betroffen sind.

## Projektziel

Itoeva ist eine lokale Android-Erinnerungsapp mit einem begleitenden Wesen, das nicht bestraft,
sondern sanft auf gemeinsam verbrachte Zeit reagiert - keine Streaks, keine Schuld, kein Server.
Details und die nicht verhandelbaren Eigenschaften dieser Erfahrung: siehe Vision.md.

**Aktuelle Priorität:** Das beobachtbare Leben der Avatare innerhalb der bestehenden Mechanik
spürbar verbessern: glaubwürdiger Tagesablauf, individuelle Reaktionen, natürliche Übergänge und
eine sanft sichtbare Reminder-Wirkung. Kleine Änderungen sollen nach ihrem Spielerlebnis-Hebel,
nicht nach ihrer leichten Zählbarkeit gewählt werden. Parallel bleiben Build-Prozess,
Testabdeckung und Agentenfreundlichkeit wichtig; neue große Systeme bleiben bis zu einer
ausdrücklichen Entscheidung ausgeschlossen (siehe NextTasks.md und Tagesablauf.md).

## Stilregeln

- **Deutsch zuerst.** Kommentare, Commit-Nachrichten, interne Dokumentation und `values-de/` sind
  primär Deutsch. `values/` (Englisch) wird mitgepflegt, ist aber Zweitsprache.
- **Kommentare erklären das Warum, nicht das Was.** Ein Kommentar, der nur wiederholt, was der
  Code offensichtlich tut, gehört nicht ins Repository. Ein Kommentar, der eine nicht
  offensichtliche Einschränkung, einen Trade-off oder einen verworfenen Alternativweg erklärt
  (siehe z. B. Kommentare in `PlayScene.kt` oder `GlyphClockWidgetProvider.kt`), gehört hinein.
- **Bestehende Mechanik wiederverwenden statt neu erfinden.** Vor einer neuen Zieh-, Kollisions-
  oder Animationslogik prüfen, ob `AvatarFeeding`, `feedOccurrence` oder vorhandene
  Reaktionsanimationen bereits das Gewünschte leisten (siehe Architecture.md, Datenfluss-
  Abschnitt).
- **Keine stillen Verhaltensänderungen.** Jede Änderung an Reminder-Semantik, Game Loop, Balancing
  oder Charakter/Story braucht einen dokumentierten Eintrag nach dem Format in EVOLUTION.md,
  Abschnitt "Evolution History".
- **Keine Abkürzungen bei Migrationen.** Da es kein Server-Backup gibt (siehe Vision.md), macht
  ein unsauberer Room-Migrationsschritt Nutzerdaten unwiederbringlich kaputt. Jede
  Schema-Änderung braucht eine echte `Migration`, keinen `fallbackToDestructiveMigration`, und
  einen zugehörigen Test.
- **Keine Optimierung aufgrund einer vermuteten Tokenquelle.** Vor jeder Token-, Kontext- oder
  Performance-Optimierung erst empirisch feststellen, was tatsächlich in den Modellkontext gelangt
  - nicht von Dateigröße, Dateiname oder Vermutung ausgehen. Konkretes Beispiel: NT-043 nahm an,
  die 1786-Zeilen-`claude-primary-run.yml` sei "die größte Tokenquelle" - tatsächlich gehen nur die
  ~35-45 Zeilen der beiden `PROMPT`-Blöcke je Lauf an das Modell, der Rest ist Bash-Orchestrierung,
  die den Modellkontext nie erreicht (siehe Architecture.md, Abschnitt "Die Evolution-Pipeline",
  und PR #23). Eine Auslagerung nach der falschen Annahme hätte nichts gespart.
- **Kleine, unabhängige Schritte statt große Sprünge.** Eine Evolution pro Lauf, ein PR pro
  Evolution - Grundprinzip der aktiven Pipeline (`claude-primary-run.yml`), gilt sinngemäß auch
  für manuell angestoßene Arbeit.

## Was niemals ohne menschliche Freigabe verändert werden darf

Diese Liste ist eine Kurzfassung von EVOLUTION.md, Abschnitte "Non-Negotiable Design Principles"
und "Protected Areas" - im Zweifel gilt dort die ausführliche, verbindliche Fassung.

- Die fünf nicht verhandelbaren Erfahrungs-Eigenschaften aus Vision.md (sanft statt strafend,
  nicht gesehen heißt nicht verpasst, getrennte Besitzverhältnisse Routine/Erlebnis,
  Auslösehäufigkeit ≠ Tagesziel, geteilte Pipeline für Alltag und Spiel).
- Produktname, ob `:app` und `:app-sim` ein oder zwei Produkte bleiben, finales Spielziel,
  Quest-Struktur, ein Skillbaum-System - alles als `OPEN DECISION` in EVOLUTION.md markiert.
- Release-/Signierungs-Infrastruktur, Keystore, Play-Store-Veröffentlichung, Firebase.
- Der Runner/die Pipeline-Infrastruktur selbst (Workflows, `runner/`, Build-/Dependency-
  Konfiguration) - Änderungen daran sind laut `runner/README.md` explizit vom autonomen Scope
  ausgeschlossen und brauchen menschliche Prüfung.
- Direkte Arbeit auf `main`: kein Push, kein Rebase, kein Force-Push außerhalb des in
  `claude-primary-run.yml` festgelegten, streng getrennten `evolve`/`publish`-Ablaufs. **Der
  Merge eines fertigen Pull Requests ist davon seit dem 2026-09-03 ausgenommen** (siehe
  EVOLUTION.md, Evolution History): Eine beauftragte Agentensitzung darf mergen, wenn die CI
  vollständig grün ist, kein Konflikt besteht und keine offene Review-Anmerkung unbeantwortet
  ist. Für den unbeaufsichtigten Lauf gilt das NICHT - er mergt sein eigenes Ergebnis nie, und
  ein direkter Push auf `main` bleibt auch für eine Sitzung ausgeschlossen.

Erlaubt ohne vorherige Rückfrage zur kreativen Richtung (seit EVOLUTION.md v0.2, "Erzählerische
Autonomie"): weitere Beziehungen und Lore-Stücke zwischen den sechs bestehenden Wesen, innerhalb
des dort beschriebenen Rahmens.

## Wie Agenten vorgehen sollen

1. **Vor jeder Änderung:** Minimal-Startsequenz oben durchlaufen, `git status` prüfen (siehe
   CLAUDE.md), dann nur bei tatsächlichem Bedarf (siehe "Wann zusätzlich welches Dokument
   lesen") das betroffene Modul in Architecture.md nachschlagen bzw. prüfen, ob ein
   `OPEN DECISION`- oder Protected-Area-Punkt aus EVOLUTION.md berührt wird.
2. **Aufgabe wählen:** aus `evolutions/BACKLOG.md` (automatisierte Pipeline) oder NextTasks.md
   (Prozessverbesserung) - nicht die "eine Regel über allen anderen" aus NextTasks.md
   ignorieren, wenn keine explizite anderslautende Anweisung vorliegt.
3. **Umsetzen:** klein, getestet, mit Kommentaren nach obigem Stil. Bestehende Muster im
   jeweiligen Modul imitieren statt neue Konventionen einzuführen.
4. **Testen:** mindestens die Tests des betroffenen Moduls lokal/CI grün bekommen; bei
   Datenmodell-Änderungen Migrationstest nicht vergessen (siehe oben).
5. **Dokumentieren:** bei Verhaltens-, Charakter-, Story- oder Balancing-Änderungen einen Eintrag
   in EVOLUTION.md nach dessen Vorlage ergänzen. Bei reinen Prozess-/Infrastruktur-Änderungen
   reicht eine gute PR-Beschreibung plus ggf. eine Aktualisierung von Architecture.md.
6. **Vor jedem Push:** Diff prüfen (siehe CLAUDE.md). Bei Unsicherheit über eine kreative oder
   architektonisch bedeutsame Entscheidung: menschliche Freigabe einholen statt zu raten -
   `OPEN DECISION` ist in EVOLUTION.md kein Mangel, sondern die korrekte, ausdrückliche Antwort.
