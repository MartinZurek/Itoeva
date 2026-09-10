# Itoeva Living Agent System

Status: freigegebener naechster Architektur-Meilenstein nach der Charakter-Musik  
Stand: 2026-09-10 (Schnitte 2a, 2b, 3 und 4 umgesetzt)

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

## Stand: was Schnitte 2a, 2b, 3 und 4 tatsaechlich gebaut haben

Der Kern liegt in `app-sim/src/main/java/com/notime/glyphsim/living/` in fuenf Dateien:
`LivingWorld.kt` (Welt, Orte, `Requirement`), `LivingNeed.kt` (Beduerfnisse, `Personality`),
`LivingAction.kt` (Handlungen, `ActionOutcome`, Katalog), `LivingPlanner.kt` (Ziele,
`UtilitySelector`, `Plan`, `Planner`) und `LivingAgent.kt` (`AgentState`, `LivingEvent`,
`AgentExplanation`, `LivingSimulation`).

Drei Entscheidungen daraus binden alles Weitere:

- **Eigenes Ortsmodell `LivingSite` mit vier Werten** statt der sechzehn `PlayScene.Place`. Die
  Abbildung gehoert in den Runtime-Adapter (Schnitt 4). Haenge die Entscheidungslogik an
  `PlayScene.kt` (3.672 Zeilen), waere sie ohne Emulator nicht mehr pruefbar.
- **`Requirement` ist ein benanntes Ding, kein `Boolean`.** Nur deshalb kann
  `AgentExplanation.blockedBy` sagen, WORAN es haengt, ohne dass irgendwo ein Satz dafuer
  geschrieben wurde. Ereignisse und Erklaerung enthalten deshalb auch keinen freien Text -
  Sprache macht die Anzeige daraus, nicht die Simulation.
- **`ActionOutcome` ist die einzige Erweiterungsstelle fuer Wirkungen.** Sie traegt heute
  Muenzen, Vorrat, Ort, Zeit, Beduerfnislinderung, gelernte Praeferenz, Erinnerung,
  Beziehungswirkung und Symbolbedeutung. Keine soziale Handlung bekommt dafuer einen
  Sonderweg.

Schnitt 2b ergaenzt begrenzte `Episode`-Listen, `RelationshipState` mit Vertrauen, Naehe und
letzter Interaktion, die feste `SymbolicIntent`-Menge, gelernte Zielpraeferenzen und
`CONNECT_WITH`. Einladungen werden als `PLAY + QUESTION` gesendet; Annahme oder Ablehnung
entsteht aus Energie, sozialem Bedarf, Beziehung, Persoenlichkeit und laufendem Ziel. Der
Mehrtagesbeleg laesst zwei gleich beduerftige Wesen allein durch unterschiedliche Erlebnisse in
Vorlieben und Historie auseinanderlaufen.

Schnitt 3 legt `LivingAgentStore` an der Android-Grenze unter `data/` an. Ein atomarer,
versionierter Snapshot pro `profileId` speichert Agent, Ressourcen, Erinnerungen, Beziehungen und
gelernten Geschmack. Beim Wiedereinstieg wachsen Beduerfnisse und Welt nur ueber die explizit
uebergebene Simulationszeit weiter. Das langlebige Ziel bleibt erhalten; der konkrete Plan wird
verworfen, damit aktuelle Orte und anwesende Wesen vor dem naechsten Schritt erneut geprueft
werden.

Schnitt 4 bindet den Kern ueber `LivingRuntimeAdapter` an die vorhandene Pixelwelt. Die
sechzehn `PlayScene.Place` werden dort und nur dort auf vier `LivingSite` abgebildet;
Simulationszeit kommt aus `PlayTimeLapse`, und Arbeit sowie Markt liefern der Domaene eine
aktuelle Verfuegbarkeitsmenge. Die bisherige gewichtete Themenwahl bleibt als Vielfaltssignal
fuer Freizeit erhalten, darf aber kein dringendes Grundbeduerfnis mehr ueberstimmen.

Eine vorbereitete Kernwirkung wird erst nach der vollstaendig gelaufenen `PlayRoutine`
gespeichert. Wird die Choreografie durch eine echte Erinnerung oder einen Moduswechsel
abgebrochen, bleiben Lohn, Einkauf, Beduerfnisse und Episode auf dem letzten abgeschlossenen
Stand. Arbeitsweg plus Arbeit und die zusammenhaengende Einkaufsfolge werden jeweils auf die
bereits vorhandenen Routinen abgebildet; deren alte globale Nebenwirkungen sind fuer Living-
Schritte abgeschaltet, damit `ActionOutcome` die einzige fachliche Rechnung bleibt. Bestehende
`PlayWallet`-/`PlayPantry`-Werte dienen einmalig als Startwert, danach zeigt auch das Gespraech
die profilbezogenen Weltressourcen. Ausdruecklich erbetene Arbeit, Einkauf und Essen laufen
ebenfalls ueber diese Living-Wirtschaft, ohne das autonome Ziel zu ersetzen. Vor jedem
zusammengefassten Teilschritt wird die Verfuegbarkeit neu bestimmt; schliesst ein Ort unterwegs,
endet die sichtbare Folge dort statt nach Ladenschluss oder Feierabend weiterzurechnen.

## Erster demonstrierbarer Schnitt

Der Kern arbeitet mit zehn tief verbundenen Aktionen und einem echten Ressourcenpfad:

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

`LivingAgentStore` liegt als Android-Grenze unter `app-sim/.../data/`; der reine Kern bleibt
speicherfrei. `SharedPreferencesLivingAgentStorage` schreibt genau einen atomaren, versionierten
Snapshot pro `profileId`. Diese Form passt zum kleinen zusammenhaengenden Zustandsgraphen und
vermeidet eine Room-Schemaaenderung ohne relationalen Nutzen. Weder `:core` noch eine der beiden
Room-Datenbanken werden angefasst.

Codec-Version 2 speichert Weltressourcen, Beduerfnisse, Persoenlichkeit, Ziel, gelernten
Geschmack, begrenzte Episoden, Beziehungen und das letzte wichtige Ereignis. Version 1 wird beim
Lesen mit leeren sozialen Lernfeldern auf Version 2 angehoben; unbekannte Zukunftsversionen und
beschaedigte Pflichtwerte werden abgelehnt. Rohframes und Tick-Protokolle gehoeren nicht in den
Snapshot.

Zeit kommt auch beim Wiederherstellen ausschliesslich als Simulationsminute von aussen. Negative
Differenzen werden zu null begrenzt. Geoeffnete Orte und anwesende Wesen stammen aus dem aktuellen
Runtime-Kontext, nicht aus einem alten Snapshot. Das Ziel ueberlebt, der Plan nicht: So muss der
Planer aktuelle Voraussetzungen erneut pruefen.

Die private lokale Begleiterhistorie bleibt von einer kuenftigen oeffentlichen Stream-Welt
getrennt. Ein Stream exportiert nur den ausdruecklich freigegebenen oeffentlichen
`AgentExplanation`-/Ereignisvertrag.

## Inkrementelle Pull Requests

1. **Plan und Grenzen** (dieses Dokument): Produktentscheidung, Wiederverwendung, Schnittstellen,
   Persistenzweg und Abnahmekriterien festhalten.
2. **Reiner Simulationskern** - aus Groessengruenden in zwei Schnitte geteilt. Der Grund steht
   nicht in der Theorie, sondern in der Geschichte dieses Repositories: An zu gross
   geschnittenen Aufgaben ist die Builder-Sitzung hier schon dreimal am Zugbudget gescheitert
   (siehe ITO-0016/ITO-0023 in `evolutions/BACKLOG.md`). Beide Haelften sind fuer sich gruen
   und ruecksetzbar.
   - **2a - Entscheiden (erledigt, NT-063):** Beduerfnisse, Utility-Wahl mit aufgeschluesselter
     Begruendung, Ziele, Plan und Replanning, sechs Handlungen mit benannten Voraussetzungen,
     typisierte Ereignisse und `AgentExplanation`. Deterministische JVM-Tests inklusive
     Mehrtageslauf.
   - **2b - Sich erinnern und verstaendigen (erledigt, NT-067):** `Episode`,
     `RelationshipState`, `SymbolicIntent`, gelernter Geschmack, das Ziel `CONNECT_WITH` und
     der deterministische Beleg, dass zwei aehnlich gestartete Agenten auseinanderlaufen.
   Neue Testdateien jeweils an beiden Stellen in `tools/reaction-preview/tests.sh`.
3. **Profilbezogene Persistenz (erledigt, NT-064)**: atomarer Version-2-Snapshot pro Profil,
   expliziter Zeitfortschritt zwischen Sitzungen, begrenzte Episoden sowie Roundtrip-,
   Profiltrennungs- und V1-Migrationsbelege. Der alte Plan wird beim Laden verworfen.
4. **Bestehende Welt anbinden (erledigt, NT-065)**: Planaktionen gezielt auf vorhandene `PlayRoutine`-Varianten,
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
