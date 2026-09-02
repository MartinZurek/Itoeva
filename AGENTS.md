# AGENTS.md

Diese Datei ist der Einstiegspunkt für KI-Agenten. Sie enthält bewusst keine zweite Kopie der
Projektregeln.

## Verbindlicher Start

1. [`AgentGuide.md`](AgentGuide.md) vollständig lesen.
2. [`CLAUDE.md`](CLAUDE.md) vollständig lesen.
3. Nur die dort für die konkrete Aufgabe genannten weiteren Dokumente und Codebereiche öffnen.

Bei Widersprüchen gilt [`EVOLUTION.md`](EVOLUTION.md).

## Übergabe an den nächsten Agenten

- Vor jeder Änderung den aktuellen Git-Status prüfen.
- Nie direkt auf `main` arbeiten; für jede Änderung einen eigenen Branch verwenden.
- Code und die dazugehörige Dokumentation im selben Branch aktualisieren.
- Bei Arbeiten am Skillbaum [`SKILLBAUM.md`](SKILLBAUM.md) pflegen: Arbeitspaket abhaken,
  verschobene Anker korrigieren und das Journal für die nächste Sitzung fortschreiben.
- Änderungen an Reminder-Semantik, Game Loop, Charakter, Story, Balancing oder Progression nach
  dem Format der Evolution History in [`EVOLUTION.md`](EVOLUTION.md) dokumentieren.
- Architektur- oder Prozessänderungen bei Bedarf in [`Architecture.md`](Architecture.md) bzw.
  [`NextTasks.md`](NextTasks.md) nachziehen.
- Commit und Pull Request müssen Zweck, betroffene Bereiche, ausgeführte Tests, ungeprüfte Punkte
  und den konkreten nächsten Schritt verständlich festhalten.
- `OPEN DECISION`-Punkte nicht durch Annahmen auflösen.
- Vor jedem Push den vollständigen Diff prüfen.

Die ausführlichen Regeln, Schutzbereiche und Auswahlhinweise stehen ausschließlich in den oben
verlinkten Dokumenten.
