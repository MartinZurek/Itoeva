# CLAUDE.md

## Projekt

Dieses Repository enthält das Projekt **Itoeva**.

## Arbeitsweise

- Vor Änderungen immer zuerst den aktuellen Git-Status prüfen (`git status`).
- Vor jedem Push den aktuellen Diff prüfen.

## Laufende Arbeit: der Skillbaum

**[SKILLBAUM.md](SKILLBAUM.md) ist die Arbeitsliste und ersetzt die Erkundung.**

Dort steht alles: was schon fertig ist, was als Nächstes ansteht, die vollständige Zuordnung
aller 67 Motive zum Animations-Baum, geprüfte Zeilennummern für jede relevante Datei, und ein
Journal mit dem, was die jeweils nächste Sitzung wissen muss.

Wer an diesem Thema arbeitet, liest zuerst dort den Kopf bis „Arbeitspakete" — **nicht** den Code
danach absuchen. Dieselbe Datei nennt auch, welche Dateien man ausdrücklich *nicht* ganz öffnen
soll (`PlayScene.kt` hat 3.672 Zeilen, gebraucht werden zwei Enums).

Am Ende jeder Sitzung: Haken setzen, Journal fortschreiben, verschobene Zeilennummern korrigieren.

## Bauen

`JAVA_HOME` ist auf diesem Rechner nicht gesetzt — ohne diese Zeile bricht jeder Gradle-Aufruf ab:

```
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"   # Bash
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # PowerShell
```

Scheitert ein Build an einem Pfad unter `G:\Meine Ablage\…`, obwohl das Projekt unter `C:\Notime`
liegt: `gradlew.bat --stop`. Das ist ein Gradle-Daemon mit einem Dateisystem-Abbild aus der Zeit
vor dem Umzug (siehe [UMZUG.md](UMZUG.md)); der Pfad steht nirgends auf der Platte, Suchen ist
vergeblich.

## Fallstrick: zwei Datenbanken

`:app` und `:app-sim` haben **je eine eigene Room-Datenbank**, teilen sich aber die Entities aus
`:core` (`GlyphReminder`, `LibraryAnimation`, `BuiltInAnimationSelection`). Room vergleicht beim
Öffnen das **ganze** Schema, nicht nur die benutzten Spalten.

Jede Änderung an einer `:core`-Entity braucht deshalb eine Migration in **beiden** Modulen —
sonst stürzt die andere App beim nächsten Start ab. Das ist hier schon einmal passiert, siehe den
KDoc von `MIGRATION_18_19` in `app/…/data/AppDatabaseMigrations.kt`.
