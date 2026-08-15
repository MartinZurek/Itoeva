# Itoeva Autonomous Evolution Runner v0.1

## Status

Diese Infrastrukturversion ist standardmäßig **deaktiviert**. Sie enthält die State Machine für
Analyse, Branch, Planreview, Implementierung, Tests, Final Review, Commit, Push und Report, führt
aber ohne eine lokale Aktivierungsdatei außerhalb des Repositorys keinen Agenten aus, erstellt
keinen Evolution-Branch und commitet oder pusht nichts.

## Sicherheitsgrenzen

Der Runner darf später ausschließlich neue `origin/evolution/*`-Branches erstellen. Direkte
Arbeit auf `main`, Main-Push, Merge, Rebase, Force-Push, Release, Firebase, Play Store, Keystore
und sonstige Produktionszugriffe sind verboten.

Die pragmatische v0.1 läuft im selben Windows-Konto wie Git Credential Manager und lokale
Builds. Sie schützt gegen Fehlbedienung und normale Agentenfehler, aber nicht gegen absichtlich
kompromittierten Agenten- oder Gradle-Code. Deshalb sind Änderungen an Runner, Workflows,
Build-/Dependency-Infrastruktur, ausführbaren Skripten und Produktionskonfiguration vom
autonomen Scope ausgeschlossen.

## Bekanntes Ruleset-Risiko

Das geplante GitHub-Ruleset für `main` kann auf dem aktuellen privaten Repository/Plan nicht
erzwungen werden. Dies ist für v0.1 ein ausdrücklich akzeptiertes Restrisiko, keine bestätigte
serverseitige Schutzgrenze. Der Runner kompensiert lokal durch exakte Base-SHA-Prüfungen,
festes `evolution/*`-Refspec und das Verbot jedes Main-, Force-, Merge- und Release-Befehls.

`origin/main` wird beim Start sowie vor Branchanlage, Implementierung, Tests, Final Review,
Commit und Push autoritativ geprüft. Bei einer Änderung bricht v0.1 ab; sie führt weder Rebase
noch Merge durch. Eine spätere manuelle oder
planbedingte Änderung der GitHub-Einstellungen wird ohne GitHub-API-Liveprüfung nicht erkannt.

## Lokale Laufzeitdaten

State, Locks, Logs, Reports und das vertrauenswürdige leere Hooks-Verzeichnis gehören unter
`%LOCALAPPDATA%\ItoevaEvolutionRunner`, nicht in den Clone. Eine spätere Aktivierungsdatei dort
muss die Standing Authorization für Commit und Push ausdrücklich einschalten; die eingecheckte
Konfiguration bleibt fail-closed.

Zu jedem finalen JSON-Report wird eine gleichnamige `.sha256`-Begleitdatei geschrieben. Sie dient
der Integritätsprüfung, ist aber keine externe Signatur.

## Aktuell erlaubte Befehle

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File runner\Invoke-ItoevaEvolution.ps1 `
  -Action ValidateConfiguration

powershell -NoProfile -ExecutionPolicy Bypass -File runner\Test-ItoevaRunner.ps1
```

## Windows Task Scheduler

Geplante Läufe verwenden den Logging-Wrapper mit denselben Runner-Parametern:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File runner\Invoke-ItoevaEvolutionScheduled.ps1 `
  -Action DryRun -Repository C:\ItoevaRunner `
  -ActivationPath "$env:LOCALAPPDATA\ItoevaEvolutionRunner\activation.json"
```

Der Wrapper startet weiterhin ausschließlich `Invoke-ItoevaEvolution.ps1`. Seine redigierte
Konsolenausgabe liegt als UTF-8-Text unter `%LOCALAPPDATA%\ItoevaEvolutionRunner\logs`; Logs, die
älter als 30 Tage sind, werden best effort entfernt. JSON-Reports und State-Dateien bleiben davon
unberührt.

Nach dem Lauf schreibt der Wrapper außerdem atomar
`%LOCALAPPDATA%\ItoevaEvolutionRunner\reports\latest-summary.json`. Die Datei enthält nur
allowlist-validierte Status-, Review-, Test-, Branch- und SHA-Angaben sowie den vollständigen
Logpfad. Ein Report gilt nur zusammen mit passendem SHA-256-Sidecar und passendem `state.json` und
`tests.json` als erfolgreich. Ein fehlender oder widersprüchlicher Report wird ausschließlich als
`FAILED` beziehungsweise bei einem entsprechenden State als `QUARANTINED` zusammengefasst. Das
gilt derzeit auch für `NO_SAFE_EVOLUTION`, wenn kein gehashter Report vorhanden ist. Parallele
Wrapper serialisieren die Aktualisierung; der zuletzt abgeschlossene Lauf gewinnt.

Eine spätere morgendliche Benachrichtigung sollte als separater Read-only-Task ausschließlich
dieses Summary lesen, Schema und Alter prüfen und anhand `runId` plus `completedAt` deduplizieren.
Mail-, Push- oder Chat-Transport bleibt dabei vom Runner und dessen Aktivierungsdaten getrennt.

`Preflight` ist für einen separaten sauberen Runner-Clone gedacht und schlägt in einem
Entwicklungsclone mit uncommitteten Änderungen absichtlich fehl.

## Vor dem ersten Dry-Run

1. Vollständigen Infrastruktur-Diff reviewen und committen.
2. Einen separaten lokalen Clone nur für den Runner anlegen.
3. `%LOCALAPPDATA%\ItoevaEvolutionRunner` und ein leeres Hooks-Verzeichnis anlegen.
4. Codex-Login und GCM-Zugriff im geplanten Task-Scheduler-Konto prüfen.
5. Lokale Aktivierungsdatei nur mit `agentExecutionEnabled=true`, aber ohne Publication anlegen.
6. Dry-Run mit Analyse, Planreview, Implementierung, Tests und Final Review ausführen.
7. Dry-Run-Report und quarantänisierten Working Tree menschlich prüfen und bereinigen.
8. Erst danach `publicationEnabled` und `standingAuthorizationEvolutionOnly` lokal aktivieren.

Ein abgebrochener Lauf wird fail-closed als `QUARANTINED` markiert. v0.1 nimmt einen unbekannten
Zwischenzustand nicht automatisch wieder auf; vor einem neuen Lauf muss ein Mensch Branch, HEAD,
Index, Working Tree und State prüfen und den dedizierten Clone auf einen sauberen Base-Stand
zurückführen. Damit wird kein unsicherer Zustand fortgesetzt, allerdings ist automatische
Wiederaufnahme ein bewusst verbleibender Ausbaupunkt.

## Nicht automatisch erzeugte Historie

Ein Run-Report ist ein Vorschlag und noch keine angenommene Evolution History. Eine Datei unter
`evolutions/` wird erst nach der menschlichen Merge-Entscheidung dauerhaft dokumentiert.
