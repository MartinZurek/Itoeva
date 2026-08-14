# Evolution #001 – Idempotente XP-Vergabe

- **Datum:** 2026-08-14
- **Evolutionsklasse:** `CODE`
- **Branch:** `evolution/001-idempotent-xp`
- **Finaler Commit:** `ff455009ff7a0b158ac8bb0542fa4d75df18298e`
- **Letzter bekannter stabiler Ausgangsstand:**
  `51ae6e20cd90ba72dd6a31d396680f24bf786797`
- **Entscheidung:** angenommen und per Fast-Forward nach `main` übernommen

## Ausgangsproblem und Spielerwirkung

Vor Evolution #001 führte `AvatarFeeding.logFeedEvent` die Feed-Markierung und die
XP-Vergabe als zwei getrennte Datenbankoperationen aus. `AvatarFeedEventDao.markFed` setzte
`fedAtMillis` bei jedem Aufruf erneut, und ein Play-Mode-Ereignis vergab anschließend bei jedem
Aufruf erneut XP.

Damit war die Fortschrittsintegrität nicht abgesichert: Ein wiederholter oder konkurrierender
Feed-Aufruf konnte für dasselbe Ereignis mehr als einmal XP vergeben. Zusätzlich bestand ein
Prozessabbruchfenster zwischen Feed-Markierung und XP-Vergabe, in dem nur eine der beiden
Wirkungen dauerhaft werden konnte. Für Spieler hätte dies einen zu hohen oder zu niedrigen
XP-Stand und damit einen unzutreffenden, aus XP abgeleiteten Levelstand bedeuten können.

## Evidenz und ursprüngliche Risikoeinschätzung

### FACT

- `AvatarFeeding.kt` markierte das Ereignis zuerst und vergab danach für `isPlayMode` XP.
- `AvatarFeedEventDao.kt` überschrieb `fedAtMillis` ohne Bedingung und lieferte keinen
  Erfolgsindikator.
- Home und Dock konnten denselben zentralen Feed-Pfad aufrufen.
- Der Play-State erhöht XP atomar per SQL; das Level wird weiterhin ausschließlich aus XP
  abgeleitet.

Ein konkret reproduzierbarer doppelter UI-Aufruf war vor der Implementierung nicht belegt. Die
Mehrfach- und Race-Möglichkeit war daher eine aus den vorhandenen Schreibpfaden abgeleitete
technische Gefahr, kein als aufgetreten dokumentierter Spielerfehler.

Die ursprüngliche Bewertung war **niedrige bis mittlere Komplexität** bei hohem Nutzen für die
Fortschrittsintegrität. Betroffen waren geschützte Progressionsregeln: XP darf nur aus
beantworteten Play-Ereignissen entstehen, historische Feed-Ereignisse müssen erhalten bleiben,
und der Levelstand bleibt aus XP abgeleitet.

## Gewählte Lösung

`AvatarFeedEventDao.markFedIfUnfed` führt als erste Datenbankoperation ein bedingtes
`UPDATE ... WHERE id = :id AND fedAtMillis IS NULL` aus und liefert die Anzahl geänderter Zeilen.
Dieser Wechsel von offen zu beantwortet ist der lineare Annahmepunkt: Genau ein Aufruf erhält
`1`, weitere Aufrufe erhalten `0`.

`AvatarFeedingRepository.feed` umfasst Feed-Markierung und gegebenenfalls die unveränderte
Vergabe von 10 XP in einer gemeinsamen Room-Transaktion. Eine Exception oder Cancellation vor
dem Commit rollt beide Wirkungen zurück. Ein Retry nach dem Commit ergibt `ALREADY_FED` und
vergibt keine weiteren XP. `NOT_FOUND` unterscheidet eine unbekannte ID von einem bereits
beantworteten Ereignis.

Bei einem Play-Ereignis ohne vorhandenen Play-State bricht die Transaktion kontrolliert ab. Es
wird kein Play-State mit einem erfundenen `startedAtMillis` angelegt. Home und Dock starten Ton,
Zähler und Reaktion erst nach bestätigtem `FIRST_FEED` oder `ALREADY_FED`; Fehler lassen ein noch
offenes sichtbares Ereignis wiederholbar.

Diese Lösung wurde gewählt, weil nur eine gemeinsame Transaktion sowohl Mehrfachvergabe bei
Konkurrenz als auch das Prozessabbruchfenster zwischen Markierung und XP schließt, ohne das
Datenbankschema oder die Progressionswerte zu ändern.

## Zweitreview und Korrekturen

Der erste Planentwurf erhielt im Zweitreview **FAIL** mit behebbaren Punkten:

- Der bedingte Schreibzugriff musste die erste Datenbankoperation sein; ein vorheriges Lesen
  hätte ein Read-before-write-Race beziehungsweise einen SQLite-Upgrade-Konflikt ermöglicht.
- Home und Dock durften das Ereignis nicht vor dem Datenbankergebnis optimistisch entfernen und
  keine Erfolgsreaktion starten.
- Ein fehlender Play-State musste vollständig zurückrollen und als kontrollierter Fehler
  behandelt werden, statt unvollständigen Erfolg anzuzeigen.
- Der Konkurrenztest brauchte ein echtes Start-Gate, getrennte Coroutines und ein Timeout.
- Widget- und Reaktions-Side-Effects mussten ausdrücklich aus der Exactly-once-Garantie
  ausgegrenzt werden.

Der korrigierte Plan erhielt **PASS**. Nach der Implementierung prüfte ein zweiter Agent Code und
vollständigen Diff erneut gegen EVOLUTION.md, den Plan und die Protected Areas. Das Ergebnis war
**PASS** ohne Critical-, High- oder Medium-Findings.

## Tatsächlich geänderte Systeme

- Feed-Ereignis-DAO: bedingte, zählende Feed-Markierung
- Play-State-DAO: Rückgabe der Zahl aktualisierter XP-Zeilen
- neues transaktionales `AvatarFeedingRepository`
- zentraler Feed-Aufruf in `AvatarFeeding`
- Feed-Ergebnisbehandlung in Home und Dock
- Entfernung des alten nicht-transaktionalen `PlayModeXp.award`-Pfads
- Charakterisierungs-, Entscheidungs- und Room-Transaktionstests
- KDoc-Verweise auf die neue Zuständigkeit

Nicht geändert wurden Balancingwerte, Reminder-Semantik, Charaktere, Story, öffentliche Texte,
Preference-Schlüssel, Datenbankschema oder Migrationen. Bestehende Feed-Ereignisse und
Play-State-Daten bleiben im selben Schema erhalten.

## Testergebnisse

- JVM-Tests für `app-sim` und `core`: **PASS**, 434/434 Tests
- `:app-sim:lintDebug`: **PASS**
- Kompilierung/Paketierung der instrumentierten Tests: **PASS**
- `AvatarFeedingTransactionTest` auf `tama-test`, Android 35: **PASS**, 7/7 Tests
- `FeedingChainCharacterizationTest` auf `tama-test`, Android 35: **PASS**, 5/5 Tests
- isolierter `FirstRunGreetingTest`: **PASS**, 4/4 Tests

Die Transaktionstests belegen ersten Feed, sequenziellen Retry, zwei konkurrierende Aufrufe,
Non-Play-Ereignisse, unbekannte IDs, Rollback und erfolgreichen Retry bei zunächst fehlendem
Play-State sowie getrennte XP-Vergabe für zwei unterschiedliche Ereignisse.

## TESTED BEHAVIOR

Für die vorhandene einzelne Prozess-/`AppDatabase`-Instanz gilt als **TESTED BEHAVIOR**:

- Ein Play-Ereignis vergibt bei seiner ersten bestätigten Beantwortung genau einmal 10 XP.
- Sequenzielle und konkurrierende Wiederholungen vergeben keine weiteren XP.
- Der Zeitpunkt der ersten Feed-Markierung bleibt bei einem Retry erhalten.
- Feed-Markierung und XP-Vergabe werden bei einem Transaktionsfehler gemeinsam zurückgerollt.
- Ein Non-Play-Ereignis wird beantwortet, verändert den XP-Stand aber nicht.

## Unabhängiger Instrumentierungs-Flake

Der vollständige Lauf `:app-sim:connectedDebugAndroidTest` endete mit **FAIL**, nachdem der
Instrumentation-Prozess während `FirstRunGreetingTest` abstürzte. Der Bericht enthielt nur
`Instrumentation run failed due to Process crashed` und keinen verwertbaren Java-/Kotlin-
Stacktrace. Vor dem Absturz waren alle Tests von `AvatarFeedingTransactionTest` und
`FeedingChainCharacterizationTest` bereits erfolgreich gelaufen.

`FirstRunGreetingTest` bestand anschließend isoliert mit 4/4 Tests. Er prüft Begrüßungs- und
Settings-Verhalten, löst keinen Feed aus und verwendet die neue XP-Transaktion nicht. Der Flake
wurde deshalb als wahrscheinlich **UNRELATED** zu Evolution #001 bewertet. Ohne einen
Baseline-Bericht vor Evolution #001 ist nicht belegt, ob exakt derselbe Suite-Absturz schon zuvor
auftrat.

## Verbleibende Einschränkungen und Risiken

- Die Exactly-once-Garantie ist auf die bestehende einzelne Prozess-/Datenbankinstanz begrenzt;
  ein Mehrprozess-Szenario wurde nicht beansprucht oder getestet.
- Widget-Stopp, Ton und Animation liegen außerhalb der SQLite-Transaktion. Ein Prozessabbruch
  nach dem Commit, aber vor diesen Side-Effects, kann deren Ausführung verhindern; ein Retry
  erkennt das Ereignis dennoch als `ALREADY_FED` und vergibt keine weiteren XP.
- Es gibt keinen automatisierten Compose-Test für die genaue Side-Effect-Reihenfolge in Home und
  Dock.
- Historische Ereignisse aus der früheren nicht-atomaren Implementierung enthalten keine
  Information, mit der sich nachträglich feststellen ließe, ob bei einem damaligen Prozessabbruch
  XP bereits vergeben wurden.
- Die Semantik eines Moduswechsels zwischen Auslösung und Beantwortung blieb außerhalb dieses
  Scopes und damit unverändert.

## Verworfene Alternativen

- **Nur bedingtes Markieren ohne gemeinsame Transaktion:** verworfen, weil das
  Prozessabbruchfenster zwischen Markierung und XP bestehen geblieben wäre.
- **Ereignis zuerst lesen, danach bedingt aktualisieren:** im Zweitreview verworfen, weil der
  bedingte Schreibzugriff der lineare Annahmepunkt sein muss und ein Read-before-write-Race
  vermieden werden sollte.
- **Fehlenden Play-State stillschweigend anlegen:** verworfen, weil dafür ein nicht belegter
  `startedAtMillis`-Wert erfunden worden wäre.
- **Optimistische UI-Reaktion vor dem Commit:** verworfen, weil `NOT_FOUND` oder ein Rollback
  sonst als erfolgreiche Fütterung erschienen wären.
- **Schema- oder Migrationsänderung zur Reparatur historischer Fälle:** nicht in Evolution #001
  aufgenommen, da historische Exactly-once-Zustände aus den vorhandenen Daten nicht zuverlässig
  rekonstruierbar sind und der freigegebene Scope ausdrücklich ohne Schemaänderung auskam.

## Rücksetzweg

Der unmittelbare stabile Vorgänger von `ff45500` ist
`51ae6e20cd90ba72dd6a31d396680f24bf786797`. Da Evolution #001 per Fast-Forward als ein einzelner
Commit übernommen wurde, ist ein Revert von
`ff455009ff7a0b158ac8bb0542fa4d75df18298e` der identifizierte nicht-historienverändernde
Rücksetzweg. Dieser Revert wurde nicht ausgeführt und ist daher als Rücksetzoperation
**UNVERIFIED**. Da keine Schema- oder Migrationsänderung erfolgte, ist keine Datenbank-Downgrade-
Migration erforderlich; bereits historisch entstandene XP-Abweichungen werden durch einen
Code-Revert weder erzeugt noch repariert.
