# Itoeva Living Agent System

Status: freigegebener naechster Architektur-Meilenstein nach der Charakter-Musik  
Stand: 2026-09-11 (Schnitte 2a, 2b, 3, 4 und 5 umgesetzt)

## Was das System heute wirklich ist

Nach sieben Schnitten (#127 bis #133) ist aus dem Plan ein laufendes System geworden. Diese
Uebersicht beschreibt den IST-Stand; der Rest des Dokuments bleibt der Plan, an dem er gemessen
wird.

**Der Kern rechnet, die alte Welt zeigt.** Unter `app-sim/.../living/` liegt eine Domaene ohne
Android, ohne Uhr und ohne Zufall. Sie entscheidet; die vorhandene Pixelwelt fuehrt aus. Es gibt
keine zweite Engine und keinen `StoryManager`.

| Baustein | Was er beitraegt |
| --- | --- |
| `Needs` / `NeedKind` | Sieben Beduerfnisse, die mit der Zeit wachsen: Hunger, Energie, Spass, Naehe, Behaglichkeit, Neugier, Entwicklung. |
| `Personality` | Startbias je Spezies - **veraenderbar**, weil er als Datum im `AgentState` liegt und nicht in einem Enum. Er verschiebt die Wahl, er ueberstimmt sie nie. |
| `UtilitySelector` | Vergibt je Ziel eine Punktzahl aus Beduerfnisdruck, Bias und Kosten - **aufgeschluesselt**, damit "warum will er das?" beantwortbar bleibt. |
| `GoalKind` | Sechs langlebige Absichten: `GET_FOOD`, `REST`, `HAVE_FUN`, `DEVELOP`, `CONNECT_WITH`, `EARN_MONEY`. |
| `Planner` / `Plan` | Leitet aus einem Ziel einen mehrschrittigen Weg ab - je nach Ressourcenlage `EAT`, oder `BUY_FOOD -> EAT`, oder `WORK -> BUY_FOOD -> EAT`. |
| `Requirement` | Voraussetzungen als **benannte Dinge** (`Coins`, `Portions`, `At`, `SiteOpen`, `Near`), nicht als Wahrheitswerte. Daran haengt die ganze Erklaerbarkeit. |
| `ActionOutcome` | Der eine Wirkungsweg. Traegt Muenzen, Vorrat, Ort, Zeit, Beduerfnisse, Episoden, Beziehungen. |
| `Episode` / `RelationshipState` | Begrenzte Erinnerung und Naehe je Wesenpaar - beides wirkt auf die Zielwahl zurueck. |
| `SymbolicIntent` | Sprachunabhaengige Verstaendigung. Kein freier Text, nirgends. |
| `AgentExplanation` | Der Schnappschuss: Ziel, Begruendung, Plan, aktuelle Handlung, Hindernis, wirksame Erinnerungen. |
| `LivingAgentStore` | Versionierter Snapshot je `profileId` in SharedPreferences. |
| `LivingRuntimeAdapter` | Bildet 16 sichtbare Orte auf 4 Domaenenorte ab und waehlt fuer jede Kernhandlung eine vorhandene `PlayRoutine`. |

**Der Ablauf eines Schrittes**, und die Reihenfolge ist die Aussage:

1. Ist das Ziel gestillt oder fehlt es, waehlt der `UtilitySelector` ein neues.
2. Fehlt ein Plan, sucht der `Planner` einen Weg aus der WIRKLICHEN Lage.
3. **Vor** der Ausfuehrung werden die Voraussetzungen erneut geprueft - nicht beim Planen.
4. Traegt die Lage den Schritt nicht, faellt der PLAN und das ZIEL bleibt. Der naechste Schritt
   leitet aus derselben Absicht einen anderen Weg ab.

Punkt 3 und 4 sind der Unterschied zwischen einem lebendigen Wesen und einer Animationsfolge.
Eine feste Kette liefe blind weiter, auch wenn der Laden inzwischen zu hat.

### Die Luecke, die am 11.09. gefunden wurde

**Das alles lief - und war unsichtbar.** Die `AgentExplanation` entsteht bei jedem Schritt und
geht in `LivingObservationFeed`; der ist fuer ein kuenftiges Overlay gedacht und wurde von
niemandem gelesen. Das Einzige ueber dem Kopf war ein zufaellig gewuerfelter Satz aus
`PlaySpeech`, der mit dem Ziel des Wesens nichts zu tun hatte.

Wer zusah, konnte deshalb nicht unterscheiden, ob die Figur etwas WOLLTE oder ob der Wuerfel es
ergab - obwohl sie es seit Wochen wirklich will. Gemeldet wurde das als "ich sehe das Living
Agent System in der APK nicht", und das war genau richtig beobachtet.

`LivingSymbols` schliesst diese Luecke an der Stelle, an der die Entscheidung entsteht: Es
uebersetzt eine `AgentExplanation` in **zwei** Symbole - den Wunsch und, falls vorhanden, das
Hindernis.

Ueber dem Kopf bleibt danach **ausschliesslich Symbolik**. Auch der Halbsatz "das hast du dir
heute auch vorgenommen" (`PlaySpeech.habitHint`) faellt weg - ausdrueckliche Entscheidung vom
2026-09-11. Die Verbindung zwischen dem Tag des Wesens und dem des Nutzers geht dadurch nicht
verloren, sie wechselt nur den Ort: Sie steht im Gespraech beim Antippen, wo sie eine Antwort
auf eine Frage ist statt ein ungefragter Hinweis. Zusammen ergeben sie die kleinste Geschichte, die diese Welt erzaehlen kann; "ich
will essen" plus "mir fehlt Geld" ist bereits ein Konflikt. Die Anzeige dieser Symbole ueber dem
Kopf ist der naechste Schnitt (NT-069) und ersetzt dabei den gewuerfelten Satz.

### Die zweite Luecke, gefunden am 12.09.: acht Reminder, eine Wirkung

Die erste Luecke war, dass man den Agenten nicht SAH. Die zweite war, dass er fuer den groessten
Teil des Tages **nicht unterschied**.

`LivingRuntimeAdapter.requestedActions` uebersetzte eine beantwortete Erinnerung in eine
Kernhandlung - aber nur fuer vier der zwoelf Typen (Arbeit, Essen, Ausruhen, Schlafen). Die
anderen acht endeten samt und sonders in `else -> PURSUE_INTEREST`. Buch, Fokus, Kreativitaet,
Achtsamkeit, Liebe, Bewegung, Medizin und Allgemein stillten damit **exakt dasselbe**: Spass
0,5, Neugier 0,4, Wachstum 0,3. Eine Tablette machte das Wesen vergnuegt und neugierig.

Sichtbar war der Unterschied laengst - der Avatar ging ans Regal, an die Staffelei, nach
draussen. Nur im Kern kam davon nichts an. Dasselbe galt autonom: `Planner.planFor` plante fuer
`HAVE_FUN` **und** `DEVELOP` dieselbe eine Handlung.

Das ist nicht nur ungenau, es nimmt dem Tag seine Struktur. Wenn jede Beschaeftigung dasselbe
stillt, kann keine die naechste nach sich ziehen, und die Erinnerung lernt aus allen dasselbe.

NT-072 gibt jeder der acht ihre eigene Handlung mit eigener Wirkung. Zwei Entscheidungen dabei
sind erwaehnenswert:

- **Fuersorge stillt ausschliesslich Behaglichkeit**, ausdruecklich keinen Spass. Medizin laeuft
  seither unter dem Ziel `REST` statt `HAVE_FUN` - sonst lernte das Wesen mit jeder Tablette,
  dass Vergnuegen schoen ist, und traefe spaeter deshalb andere Entscheidungen.
- **Bewegung macht hungrig.** Erst dadurch zieht ein Nachmittag draussen ein Abendessen nach
  sich, ohne dass irgendwo eine Regel "nach Sport kommt Essen" stuende.

**Die Vielfalt bleibt, wo sie war.** Welche Beschaeftigung zu dieser Stunde, dieser Spezies und
diesem Entwicklungspfad passt, weiss weiterhin die gewichtete Tagesablaufwahl der Laufzeitschicht
- mitsamt Wiederholungsdaempfer und Nachklang. Der Kern entscheidet nach wie vor nur, OB Freizeit
gerade traegt; die Ausformung wird ihm als `interest` hereingereicht (`Planner.planFor`), statt
dass er sie ein zweites Mal und schlechter nachbaut.

### Die dritte Luecke: Draussen war kein Ziel

Gemeldet als "er hockt hauptsaechlich in seinem Zimmer". Naheliegend waere gewesen, an den
Gewichten der Tagesablaufwahl zu drehen - dort steht Bewegung abends mit 4 aber ohnehin schon
ganz oben. Die Ursache lag eine Schicht tiefer:

**Kein einziges `Requirement` im ganzen Kern nannte `LivingSite.OUTSIDE`.** Der Ort existierte in
der Welt, in der Ortsabbildung und in den Oeffnungszeiten - aber in keinem einzigen Plan.
Draussen zu sein war damit nie eine Absicht, sondern immer nur eine Nebenwirkung davon, welches
Thema die Oberflaeche gerade gezogen hatte.

Seit NT-073 verlangt `MOVE_BODY`, draussen zu sein. Damit muss der Planer einen Weg vor die Tuer
voranstellen - der Gang hinaus ist ein sichtbarer Planschritt geworden statt einer Kulissenfrage.
`Planner.planFor` stellt den Weg allgemein voran, sobald eine Freizeitbeschaeftigung ein `At`
traegt; eine kuenftige mit eigenem Ort braucht dort nichts mehr.

Der Ausgleich gehoert dazu: Weil der Weg jetzt Zeit kostet, stillt Bewegung etwas mehr als das
Herumsitzen daheim. Ohne das waere Hinausgehen unterm Strich teurer geworden - die Aenderung
haette das Gegenteil bewirkt.

### Die vierte: der Traum war nicht selten, er war unerreichbar

Gemeldet als "die Traumsequenz hab ich noch nie gesehen". Auch hier war die Wahrscheinlichkeit
nicht das Problem: 40 Prozent je Gelegenheit, alle sechs bis fuenfzehn Minuten.

Traeume liefen aber ausschliesslich in `RoutineStep.SleepUntilMorning` und dort nur in
`DayPhase.NIGHT` - also ab 23 Uhr. Tagsueber war dieselbe Schlafhandlung ein Nickerchen von acht
Sekunden Stille. Wer abends zusieht, konnte keinen Traum sehen, egal wie lange.

NT-073 fuehrt `RoutineStep.Daydream` ein: ein ausdruecklicher Schritt statt einer stillen Regel im
Verweilen, damit im Ablauf selbst steht, wo ein Traum moeglich ist - auf dem Sofa, beim
Innehalten und auf der Bank draussen - und ein Test es nachlesen kann. Dazu traeumt jetzt auch
das Nickerchen. Der Tagtraum ist mit 28 Prozent bewusst seltener als der Nachttraum; er soll ein
Aufblitzen bleiben.

### Erfahrung aus dem Erleben

`Needs.wellbeing()` fasst den Zustand zu einer Zahl zusammen - 1 heisst "nichts draengt". Der
Mittelwert und nicht das staerkste Beduerfnis: Ein Wesen, das satt und ausgeruht ist, aber seit
Tagen niemanden gesehen hat, geht es nicht gut; das Maximum wuerde das nicht sehen.

Darauf setzt `PlayModeXp.wellbeingBonus` auf. Bis NT-072 gab jede beantwortete Erinnerung genau
zehn XP - eine Tablette so viel wie ein Nachmittag draussen. Erfahrung entstand aus dem ZAEHLEN
von Erinnerungen, nicht aus dem Erleben. Der Zuschlag kommt oben drauf und misst, was die
Handlung dem Wesen wirklich gebracht hat. Er ist **nie negativ**: Arbeit und Konzentration senken
das Wohlbefinden kurz, und dafuer XP abzuziehen hiesse, das Wesen fuer Anstrengung zu bestrafen.

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

## Stand: was Schnitte 2a, 2b, 3, 4 und 5 tatsaechlich gebaut haben

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

Schnitt 5 stellt mit `LivingObservationSource` einen reinen Lesevertrag bereit. Sein flacher
`LivingObservation`-Snapshot nennt Simulationsminute, staerkstes Beduerfnis, Wunsch, dessen
aufgeschluesselten Grund, Plan, naechste Handlung, benanntes Hindernis, wirksame Episoden,
Beziehungen und das wichtigste juengste Ereignis. `LivingObservationFeed` erhaelt nur bereits
abgeschlossene Runtime-Ergebnisse; eine Anzeige kann weder Agent noch Welt veraendern. Das
zugehoerige Ereignisfenster ist fest begrenzt und laesst `IDLE`-Ticks aus.

Der Langlauf geht ueber denselben `LivingRuntimeAdapter` wie die Pixelwelt. Ueber vier
simulierte Tage bleibt er deterministisch und erzeugt aus der Startlage die belegbare Folge
Wunsch -> geschlossener Arbeitsplatz -> Warten/Neuplanung -> Arbeit -> Einkauf -> Essen. Der
Test beschreibt nur die Startlage und die erwarteten Bedeutungen, keinen Plot.

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
5. **Stream-Vertrag beobachten (erledigt, NT-066)**: read-only Snapshot/Event-Quelle fuer spaetere Overlays,
   Langlauftest ueber mehrere simulierte Tage. Noch
   kein komplexes Twitch-UI und keine Plattformintegration.

Nach dem Meilenstein nutzt NT-068 diesen Vertrag in einer separat installierbaren Stream-
Build-Variante derselben `:app-sim`-Runtime. Ein `ExternalImpulse` wird als begrenzter,
fluechtiger `GoalInfluence` bewertet; er setzt weder Bedarf noch Ziel oder Agentenzustand direkt.
Die echte Plattform- und Netzwerkstrecke bleibt ausserhalb des Living-Agent-Kerns.

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
