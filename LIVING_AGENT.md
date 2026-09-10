# Itoeva Living Agent System

Status: freigegebener naechster Architektur-Meilenstein nach der Charakter-Musik  
Stand: 2026-09-10

## Leitidee

Itoeva schreibt keine Geschichten vor. Die Simulation fuehrt Beduerfnisse, Weltzustand,
Erinnerungen, Beziehungen, Persoenlichkeit und Ziele zusammen. Sichtbare Geschichten entstehen
aus den Entscheidungen und Folgen dieser Regeln. Es gibt deshalb keinen `StoryManager`, keine
Plot-Skripte und keine vorgefertigten Gespraechsfolgen.

Der erste Schnitt bleibt klein, deterministisch und erklaerbar. Er ersetzt nicht die bestehende
Welt oder ihre Animationen, sondern entscheidet, welche vorhandene Handlung als naechstes
ausgefuehrt werden soll.

## Was bereits wiederverwendet wird

| Bestand | Rolle im Living Agent System |
| --- | --- |
| `PlayPantry` und `PlayWallet` | Vorhandene Regeln fuer Vorrat, Lohn und Einkauf; werden spaeter durch avatarbezogenen Weltzustand gespeist. |
| `PlayRoutine` / `RoutineStep` | Ausfuehrung und sichtbare Choreografie eines gewaehlten Schritts. |
| `PlayRoutines` | Vorhandene Wege fuer Arbeit, Einkauf, Heimkehr, Essen, Spiel, Musik und Ruhe. |
| `PlayScene` | Orte, Stationen, Besucherlaubnis und spaeter weitere Weltbedingungen. |
| `PlayAmbientActivity` | Kandidaten und bestehende Signale fuer nicht dringende Freizeit; nicht mehr die alleinige Zielwahl. |
| `AvatarSpecies` | Startbias der Persoenlichkeit, niemals unveraenderliches Schicksal. |
| `PlayPresence` und `PlayTimeLapse` | Adapter fuer Ort, Zeit und Wiedereinstieg; die Kerndomaene bekommt Uhrzeit explizit uebergeben. |
| `AvatarActivityPlans` | Bewaehrtes Muster: semantische Absicht getrennt von konkreter Routine. |
| `PlayDreamMemory` | Beleg fuer kompakte, profilbezogene Speicherung; nicht selbst die episodische Agentenerinnerung. |
| `PlayVisitWindow` / `runVisit` | Eintrittspunkt fuer spaetere echte soziale Entscheidungen und symbolische Kommunikation. |

Die heutige Sonderregel in `DockScreen` (`Vorrat leer -> Geld pruefen -> Arbeit/Einkauf`) wird
nicht verdoppelt. Sobald der Runtime-Adapter angeschlossen ist, liefert der neue Planer diese
Entscheidung und `DockScreen` bleibt Ausfuehrer.

## Neue kleine Domaene

Der reine Kotlin-Kern kommt in ein eigenes Paket unter
`app-sim/src/main/java/com/notime/glyphsim/living/`. Er kennt weder Compose noch Android,
Datenbank oder Renderer.

- `AgentState`: avatarbezogener Zustand mit Beduerfnissen, aktuellem Ziel, Plan,
  Praeferenzlernen, kompakten Erinnerungen und Beziehungen.
- `WorldState`: Geld, Vorrat/Inventar, Ort, Zeit, verfuegbare Handlungen und anwesende Wesen.
- `Need`: mindestens Hunger, Energie, soziale Naehe, Spass, Sicherheit/Komfort, Neugier und
  persoenliche Entwicklung. Werte wachsen mit verstrichener Simulationszeit.
- `Goal`: langlebige Absicht wie `GET_FOOD`, `REST`, `HAVE_FUN`, `DEVELOP` oder
  `CONNECT_WITH`.
- `Action`: kleiner Satz tief verbundener Schritte. Der erste Schnitt umfasst
  `INSPECT_FOOD`, `EAT`, `WORK`, `BUY_FOOD`, eine Freizeit-/Entwicklungshandlung sowie
  symbolische Einladung und Antwort.
- `Plan`: geordnete Aktionen mit Ausgangsziel. Vor jedem Schritt werden Voraussetzungen erneut
  geprueft. Scheitert eine Voraussetzung oder aendert sich die Welt, wird der Plan verworfen und
  aus dem weiter bestehenden Ziel neu abgeleitet.
- `UtilitySelector`: nachvollziehbare Punktzahl aus Beduerfnisdruck, Persoenlichkeitsbias,
  gelerntem Geschmack, Langzeitziel, Erinnerung und sozialem Wert minus Kosten, Zeit und Risiko.
  Gleiche Eingaben ergeben dieselbe Wahl; feste Tiebreaker ersetzen versteckten Zufall.
- `LivingEvent`: bedeutungsvolle Fakten aus der Simulation, etwa Mangel entdeckt, Arbeit
  abgeschlossen, Kauf gelungen/gescheitert, gegessen, Aktivitaet gelungen, Einladung gesendet
  oder abgelehnt.
- `Episode`: verdichtete Erinnerung aus wichtigen Ereignissen. Frames und wiederholte
  Leerlaufbewegungen werden nicht gespeichert.
- `RelationshipState`: Vertrauen/Naehe und juengste Interaktion je Wesenpaar.
- `SymbolicIntent`: sprachunabhaengige Symbole wie `FOOD`, `PLAY`, `MUSIC`, `HOME`,
  `WORK`, `AFFECTION`, `QUESTION`, `YES`, `NO`, `TIRED`, `SURPRISE`.
- `AgentExplanation`: stabiler Vertrag fuer UI und Stream-Overlay: aktuelle Handlung, Wunsch,
  Begruendung, Plan, Hindernis, wirksame Erinnerungen und letztes wichtiges Ereignis.

Die Praesentationsschicht darf spaeter Ereignisfolgen wie Wunsch -> Hindernis -> Versuch ->
Anpassung -> Erfolg erkennen. Sie darf keine Ereignisse erfinden oder die Simulation steuern.

## Erster demonstrierbarer Schnitt

Der Kern beginnt mit etwa sieben Aktionen und einem echten Ressourcenpfad:

1. Hunger wird dringlich und `GET_FOOD` gewinnt die Zielwahl.
2. Der Agent prueft den eigenen Vorrat.
3. Ist Essen vorhanden, isst er.
4. Fehlt Essen und reicht das Geld, kauft er ein und isst danach.
5. Fehlen Essen und Geld, leitet der Plan `WORK -> BUY_FOOD -> EAT` ab.
6. Aendert sich Geld, Vorrat, Ort oder Verfuegbarkeit, scheitert der naechste Schritt sichtbar und
   der Agent plant neu.
7. Bei gedeckten Grundbeduerfnissen kann Musik/Spiel/Lernen gewinnen.
8. Zwei Agenten koennen `PLAY + QUESTION` austauschen; die Antwort entsteht aus Energie,
   sozialem Bedarf, Beziehung, Persoenlichkeit und aktuellem Ziel.

Ein deterministischer Mehrtages-Test beginnt mit zwei aehnlichen Agenten. Unterschiedliche
Erlebnisse veraendern ihre gelernten Praeferenzen und Beziehungen, sodass ihre Historien
auseinanderlaufen, ohne eine Geschichte vorzugeben.

## Persistenz

Die Domaene spricht nur mit einem `LivingAgentStore`-Interface. Damit bleiben Tests rein und
ein spaeterer Speicher austauschbar.

Fuer den ersten App-Adapter wird der Zustand versioniert und pro `profileId` gespeichert.
Ressourcen duerfen nicht wie heute global zwischen Profilen geteilt werden. Episoden werden
begrenzt und verdichtet; Rohframes und endlose Tick-Protokolle werden nicht gespeichert.

Die erste Kern-PR aendert keine Room-Entity. Die Persistenz-PR entscheidet anhand der
Zuverlaessigkeitsanforderung zwischen einer versionierten profilbezogenen Datei/Preference und
eigenen `:app-sim`-Room-Entities. Bei Room gilt: Migration und Migrationstest im selben PR.
`:core` wird dafuer nicht geaendert, damit keine Migration in beiden Apps ausgeloest wird.

Die private lokale Begleiterhistorie bleibt von einer kuenftigen oeffentlichen Stream-Welt
getrennt. Ein Stream exportiert nur den ausdruecklich freigegebenen oeffentlichen
`AgentExplanation`-/Ereignisvertrag.

## Inkrementelle Pull Requests

1. **Plan und Grenzen** (dieses Dokument): Produktentscheidung, Wiederverwendung, Schnittstellen,
   Persistenzweg und Abnahmekriterien festhalten.
2. **Reiner Simulationskern**: Beduerfnisse, Utility-Wahl, Ziele, Plan/Replan, kleiner
   Aktionssatz, Ereignisse, Episoden, Beziehungen, Symbole und Erklaerung. Deterministische
   JVM-Tests inklusive Mehrtagesgeschichte; neue Testdatei an beiden Stellen in
   `tools/reaction-preview/tests.sh`.
3. **Profilbezogene Persistenz**: versionierter Store, Zeitfortschritt zwischen Sitzungen,
   begrenzte Episoden und belastbare Roundtrip-/Migrations-Tests.
4. **Bestehende Welt anbinden**: Planaktionen gezielt auf vorhandene `PlayRoutine`-Varianten,
   `PlayPantry`, `PlayWallet`, `PlayPresence` und Besuchsfenster abbilden. Nur gezielte
   Aenderungen an `DockScreen`; keine zweite Choreografie-Pipeline.
5. **Stream-Vertrag beobachten**: read-only Snapshot/Event-Quelle fuer spaetere Overlays,
   Langlauftest ueber mehrere simulierte Tage und Geraetepruefung der sichtbaren Ablaeufe. Noch
   kein komplexes Twitch-UI und keine Plattformintegration.

Jede PR muss fuer sich klein, ruecksetzbar und gruen sein. Eine spaetere PR darf erst beginnen,
wenn die vorherige gemergt und der neue `main`-Stand gelesen ist.

## Abnahme des ersten Meilensteins

Programmgesteuert muss nach mehreren simulierten Tagen ablesbar sein:

- Was will der Agent gerade, warum, und was tut er?
- Aus welchen Schritten besteht sein Plan?
- Welche Voraussetzung blockiert den naechsten Schritt?
- Welche Erinnerung und Beziehung beeinflusst die Entscheidung?
- Hat ein Agent einen ressourcenbedingten Mehrschrittplan erfolgreich abgeschlossen?
- Haben zwei Agenten symbolisch kommuniziert und passend zu ihrem echten Zustand reagiert?
- Haben zwei aehnlich gestartete Agenten unterschiedliche Vorlieben und Historien entwickelt?
- Laesst sich aus den tatsaechlichen Ereignissen eine kleine Geschichte erkennen, ohne dass ein
  Plot im Code steht?

Nicht Teil dieses ersten Meilensteins sind ein allgemeiner KI-Planer, freie Textdialoge, ein
`StoryManager`, Cloud-Synchronisation, kostenpflichtige APIs, eine zweite Render-/Musikpipeline
oder eine komplexe Twitch-Oberflaeche.
