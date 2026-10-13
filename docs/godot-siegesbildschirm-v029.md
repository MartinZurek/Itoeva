# Itoeva Godot v0.2.9 – Siegesbildschirm und sicherer Kampfabschluss

## Änderung

Nach der letzten Trefferanimation erscheint eine eigene Ergebnisansicht. Beide Gruppenmitglieder erhalten je 120 XP; der Gesamterfahrungsstand zählt sichtbar hoch. Pro Sieg gibt es einen Heiltrank und einen Mondsplitter. Die Figuren verwenden ihre vorhandene Siegerpose und eine kurze individuelle Reaktion. „Weiter in den Wald“ schließt den Kampf ab, entfernt die Kampfansicht und aktiviert die normale Bewegung, Touchsteuerung und Weltoberfläche.

Das ist ein begrenzter Ausbau des vorhandenen ersten Kampfes. XP sind gespeicherte Erfahrungspunkte, noch kein Level- oder Skillbaumsystem. Mondsplitter sind vorerst Sammelgegenstände ohne weitere Funktion. Gefundene Heiltränke sind nach den drei vorhandenen Starttränken in weiteren Kämpfen tatsächlich verwendbar. Die Startausstattung jedes Versuchs bleibt erhalten.

## Speichervertrag und Unterbrechung

Das bestehende `forest_encounters[species]`-Objekt erhält additive Schema-3-Felder: `experience`, `inventory`, `victories`, `rewards_claimed` und `result`. Der Fortschritt gehört zum lokalen Spielstand des gewählten Hauptcharakters; er wird nicht zwischen unterschiedlichen Hauptcharakter-Spielständen zusammengeführt. Beide Teilnehmer einschließlich eines erschöpften Begleiters erhalten dieselben XP.

Schaden und Aktionskosten werden wie bisher vor der Animation gespeichert. Die XP-/Beutequittung entsteht beim Abschluss der letzten Animation und wird zusammen mit dem Belohnungsmarker gespeichert. Menüwechsel, erneutes Laden und zusätzliche Abschlussaufrufe vergeben das Ergebnis nicht doppelt. Eine tatsächliche neue Kampfwiederholung behält vorhandene Erfahrung und Beute und kann eine neue Belohnung verdienen. Niederlage und Rückzug löschen diese Werte nicht.

V0.2.7-/0.2.8-Spielstände bleiben lesbar. Ein noch offener gespeicherter Sieg erhält die neue Ergebnisansicht und genau eine Belohnung. Bereits abgeschlossene frühere Siege werden nicht nachträglich neu abgerechnet.

Der Encounter wird vor dem Entfernen seiner Oberfläche suspendiert. Aktionssperren, sichere Rückkehrposition, Geschwindigkeit und gehaltene Eingaben werden beim Abschluss bereinigt. Inaktive oder entfernte Kampfansichten werden nicht weiter aktualisiert. Die Ergebnisansicht hat eine kurze Durchtippsperre.

## Evidenz

Der Nutzer meldete einen Abbruch/Absturz nach dem Kampf in v0.2.8. Der alte Desktopablauf erreichte in einer gezielten Prüfung bei allen sechs Figuren bereits einen freien Weltzustand (30 Checks). Ein Android-Crash samt Stacktrace konnte hier deshalb nicht als konkrete Ursache reproduziert werden. Die neue Version ersetzt den unvollständigen Siegesablauf und sichert dessen Lebenszyklus ab; eine endgültige Aussage zur gemeldeten Geräteursache setzt den Telefonversuch voraus.

| Prüfung | Checks | Fehler |
|---|---:|---:|
| Kampfmodell, Fähigkeiten, XP/Beute, Migration, Wiederholung, Rückzug, Brenn-/Unterstützungssieg | 149 | 0 |
| Vorhandene Kampf-UI und Touchmenüs | 144 | 0 |
| Siegesabschluss aller sechs Figuren, echte Touch-Doppeltipps, Menü, Speicherdatei neu laden, zweiter Sieg | 72 | 0 |
| Waldpfad und vorhandener Kampf-/Weltablauf | 16.525 | 0 |
| Originalwelt, Steuerung, Türen, Möbel und Speicherung | 76 | 0 |

Die hohe Waldanzahl umfasst systematische Wegpunkt-/Kollisionschecks. Ergebnisansichten mit Fennec, Gloop und Hootlet wurden in Godot 4.5.1 mit echtem OpenGL/Mesa gerendert und visuell kontrolliert. Geräte-GPU, reales Touchgefühl, Hintergrundwechsel und der ursprünglich gemeldete Android-Absturz bleiben am Telefon zu prüfen.

## Android und Übergabe

AndroidTest: Version 0.2.9, Versionscode 12, Paket `com.itoeva.godot.hd2d.worldtest`, ARM64, API 24 / Target 35. Signatur v2/v3 mit demselben privaten Testzertifikat wie v0.2.8. Als Update über die bestehende Test-App installieren; die lokale Speicherung liegt weiterhin im gleichen Paket. Keine Deinstallation erforderlich.

Die SDK-Buildwerkzeuge im Prüfcontainer sind 29.0.3; Godot nutzt die vorhandene Android-35-Vorlage. Der bestehende fehlende Projekticon-Eintrag erzeugt beim Export einen nicht fatalen Hinweis; die APK wurde erfolgreich exportiert und ihre Signatur und Paketdaten separat geprüft.

Quellprojekt mit Godot 4.5.1 öffnen: `prototypes/godot-hd2d/project.godot`. Relevante Dateien: `scripts/forest_battle.gd`, `scripts/forest_encounter.gd`, `scripts/battle_view.gd`, `tests/turn_battle.gd` und `tests/victory_flow.gd`. Tests aus dem Projektverzeichnis: `godot --headless --path . --script res://tests/victory_flow.gd`. Privater Signierschlüssel, lokale Spielstände, Importcache und QA-Aufnahmen sind nicht im Quellarchiv.

Rücksetzung: ursprüngliches v0.2.8-Quellarchiv wiederverwenden; für einen Android-Rücksetzbuild eine höhere Versionsnummer und denselben Testschlüssel nutzen. Die alte Version ignoriert die neuen Zusatzfelder, zeigt aber XP/Beute nicht und kann sie bei erneutem Speichern verlieren. Vor Rücksetzung den lokalen Spielstand privat sichern.

Nächster konkreter Schritt: v0.2.9 über v0.2.8 installieren, einen normalen Sieg spielen, auf der Ergebnisansicht ins Menü wechseln/fortsetzen, „Weiter in den Wald“ tippen und anschließend laufen. Eine Wiederholung prüft den Erhalt der ersten 120 XP und des Gegenstandsvorrats.
