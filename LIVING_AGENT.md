# Itoeva Living Agent System

Status: freigegebener naechster Architektur-Meilenstein nach der Charakter-Musik  
Stand: 2026-09-15 (Kern-Schnitte 2a bis 5, NT-085 bis NT-090 umgesetzt)

## Was das System heute wirklich ist

Nach den gemergten Schnitten #127 bis #154 und dem vollstaendigen NT-086 ist aus dem Plan ein
laufendes System geworden. Diese
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
| `GoalKind` | Acht langlebige Absichten: `GET_FOOD`, `REST`, `HAVE_FUN`, `DEVELOP`, `CONNECT_WITH`, `EARN_MONEY`, `EXPLORE`, `SEEK_COMFORT`. |
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

NT-073 fuehrte `RoutineStep.Daydream` ein: ein ausdruecklicher Schritt statt einer stillen Regel
im Verweilen, damit im Ablauf selbst steht, wo ein Tagtraum moeglich ist - auf dem Sofa, beim
Innehalten und auf der Bank draussen - und ein Test es nachlesen kann. Der Tagtraum bleibt mit
28 Prozent ein seltenes Aufblitzen.

Die naechste Messung fand zwei weitere Luecken. Erstens hing die einzige Traumdarstellung an
`occupiedStation == BED`; die neu geplanten Sofa- und Banktraeume liefen deshalb, blieben aber
unsichtbar. Zweitens blieb der eigentliche Schlaf trotz 40-Prozent-Regel manchmal eine ganze
Nacht ohne sichtbaren Gedanken. NT-083 trennt deshalb die Regeln klar:

- Ein TAGTRAUM bleibt selten und zeigt ein einzelnes wirklich erlebtes Thema in der kleinen
  Blase, jetzt auch ausserhalb des Betts sichtbar.
- Jeder echte SCHLAF ruft genau einmal einen sicheren Tagesrueckblick auf. Kleine Traumblasen
  oeffnen sich ueber dem Wesen, das Bild geht in die Watch ueber, und die Watch waechst und zieht
  wie in der Mondsequenz nach oben. Dort laufen hoechstens drei der juengsten unterschiedlichen
  Tageserlebnisse mit ihren vorhandenen Charakteranimationen.
- Die Erinnerungsliste wird beim Einschlafen festgehalten, damit Mitternacht den Rueckblick nicht
  austauscht. Schlaf und Medizin werden nicht als Highlights verkauft. Ein ganz neuer Tag ohne
  geeignetes Erlebnis erhaelt trotzdem die Sequenz mit der echten Schlafpose, aber kein
  erfundenes Highlight.

Das ist weiterhin Darstellung vorhandener Erlebnisse, keine zweite episodische Erinnerung und
kein Plot: `Episode` im Living Agent beeinflusst Entscheidungen; `PlayDreamMemory` verdichtet nur
die Bilder, die der Zuschauer im Schlaf wiedersehen kann.

### Reminder sind nicht nur verschieden wirksam, sondern verschieden sichtbar

NT-072 trennte die Wirkungen der Reminder im Kern. Die anschliessende Vollpruefung der sichtbaren
Antworten fand eine zweite Ebene: Alle Reminder loesten zwar Avatarframes aus, aber 29 allgemeine
Bibliotheksmotive erbten noch die Animation eines Geschwisters. `AvatarMotifReactions` gibt ihnen
seit NT-084 eine eigene Requisite, Koerperbahn, Mimik und Taktung. Die 30 Charaktermotive und die
Rocket-Flugfolge bleiben in ihren vorhandenen Spezialkatalogen.

Der Beleg betrachtet nicht nur Funktionsnamen: Das Vorschauwerkzeug misst Bildfolge und
Standzeiten aller 80 Motivknoten. Vorher lagen 26 Knoten in bildgleichen Geschwistergruppen,
jetzt sind es 80 verschiedene Reaktionen. Damit gilt fuer den gesamten Baum: Ein sichtbares
Reminder-Motiv besitzt auch eine sichtbar eigene Antwort des Avatars.

### Erfahrung aus dem Erleben

`Needs.wellbeing()` fasst den Zustand zu einer Zahl zusammen - 1 heisst "nichts draengt". Der
Mittelwert und nicht das staerkste Beduerfnis: Ein Wesen, das satt und ausgeruht ist, aber seit
Tagen niemanden gesehen hat, geht es nicht gut; das Maximum wuerde das nicht sehen.

Darauf setzt `PlayModeXp.wellbeingBonus` auf. Bis NT-072 gab jede beantwortete Erinnerung genau
zehn XP - eine Tablette so viel wie ein Nachmittag draussen. Erfahrung entstand aus dem ZAEHLEN
von Erinnerungen, nicht aus dem Erleben. Der Zuschlag kommt oben drauf und misst, was die
Handlung dem Wesen wirklich gebracht hat. Er ist **nie negativ**: Arbeit und Konzentration senken
das Wohlbefinden kurz, und dafuer XP abzuziehen hiesse, das Wesen fuer Anstrengung zu bestrafen.

### Die fuenfte: zwei Beduerfnisse, die nie ein Grund waren

Von sieben Beduerfnissen trieben nur fuenf ein Ziel. `CURIOSITY` und `COMFORT` wuchsen jede
Stunde mit, standen in jeder Erklaerung - und waren nie ein Grund, irgendetwas zu unternehmen.
Sie wurden ausschliesslich nebenbei gestillt, wenn ohnehin gelesen, gegessen oder geruht wurde.

Das ist der Unterschied zwischen einem Wert, den es gibt, und einem Antrieb. **Ein Wesen, das nie
aus Neugier losgeht, wirkt nicht neugierig**, egal wie hoch die Zahl dahinter steht.

NT-074 ergaenzt `GoalKind.EXPLORE` (getragen von `CURIOSITY`) und `GoalKind.SEEK_COMFORT`
(getragen von `COMFORT`). Ein Test haelt von jetzt an fest, dass **jedes** Beduerfnis ein Ziel
traegt; ein achtes ohne Ziel faellt dort auf.

Erkunden verlangt den Aufenthalt draussen - was man zu Hause findet, kennt man schon - und
bevorzugt die Ablaeufe, die den Ort wechseln. Erst dadurch, dass man an etwas VORBEIKOMMT, wird
aus einem Weg eine Strecke. Acht der elf Bewegungsablaeufe tun das, und vier davon enden in einer
Sonderaktivitaet: Drachen, Fussball, Basketball, Training, Angeln. Erkunden trifft also oft auf
etwas Besonderes, ohne dass dafuer eine eigene Ueberraschungsmechanik noetig waere.

### Und einer im Sozialen: allein ging gar nichts

Ohne Gegenueber lieferte der Planer fuer `CONNECT_WITH` **keinen Weg**. Das Ziel galt damit als
unerreichbar und fiel aus der Wahl - ein einsames Wesen konnte gegen seine Einsamkeit nichts
tun und stand daneben, bis zufaellig Besuch kam.

Jetzt faellt es auf `SHOW_AFFECTION` zurueck. Dabei lauerte eine Falle, die ein bestehender Test
aufgedeckt hat: Ohne Muehe war Zuwendung ins Leere **billiger** als jede Freizeitbeschaeftigung,
und damit haette ein Wesen allein bei gleichem Druck immer an jemanden gedacht - die Anwesenheit
eines Freundes haette an der Entscheidung nichts mehr geaendert. Genau die Aussage, die das
Soziale traegt, waere verloren gegangen. Der Aufwand von 0,12 stellt das zurecht, und er ist auch
inhaltlich richtig: An jemanden zu denken, der nicht da ist, ist die schwerere Wahl.

### Der Gast hatte kein Gedaechtnis

NT-085 hat die Begegnung echt gemacht, den Gast aber nicht. Er wurde bei jedem Besuch neu
erfunden, erlebte den Wortwechsel und war danach verworfen; gespeichert wurde nur die Seite des
Bewohners. Die Beziehung, die der Kern auf BEIDEN Seiten rechnet, hielt damit genau so lange wie
der Besuch.

Der Gast wird jetzt vor der Begegnung aus dem vorhandenen `LivingAgentStore` geladen und danach
unter seiner eigenen Kennung wieder gespeichert - derselbe Store, derselbe Codec, ein zweiter
Schluessel. `restore` traegt seine Beduerfnisse um die verstrichene Simulationszeit weiter: Er hat
nicht gewartet, sondern gelebt, waehrend er weg war.

**Zwei Dinge mussten dafuer getrennt werden, und beide waren vorher unsichtbar:**

1. **Kennung.** `AvatarSpeciesPrefs.profileId` liefert den blossen Speziesnamen - und unter genau
   dem liegt der Zustand des Spielers, sobald er diese Kreatur waehlt. Ein gespeicherter Gast
   waere beim naechsten Speziestausch zum eigenen Avatar geworden, mit fremden Beziehungen und
   fremden Erinnerungen. Die stabilen `resident:`-Kennungen aus `LivingResidents` trennen die
   Namensraeume und bezeichnen Personen statt Spezies.
2. **Welt.** `WorldState` traegt Muenzen und Vorrat, und die gehoeren der sichtbaren Welt des
   Spielers (`PlayWallet`, `PlayPantry`). Der Gast bekommt Zeit, Ort und Anwesende aus der
   gemeinsamen Begegnung, Muenzen und Vorrat aber aus seinem eigenen letzten Stand.

Was damit ausdruecklich NOCH NICHT da ist: Der Einwohner waehlt seinen Tagesablauf nicht selbst,
und es gibt weiterhin hoechstens einen sichtbaren Gast gleichzeitig. Rollen und eine
Verkaufskraft existieren als Bias und Anwesenheitsfenster, noch nicht als Berufssystem.

### Die Einwohner leben zwischen den Besuchen weiter (NT-088)

Seit NT-086 haben die drei Einwohner Identitaet, Erinnerung und eigene Ressourcen - aber ihr
Zustand bewegte sich ausschliesslich, wenn der Hauptavatar ihnen begegnete. Zwischen zwei
Begegnungen standen sie still.

`LivingPopulation` schreibt sie ueber `LivingSimulation.step` fort - **denselben Kern**, keine
zweite Zielwahl und keinen zweiten Planer. Dazu ein read-only `ResidentSnapshot` (Profil, Rolle,
sichtbarer Ort, Domaenenort, Ziel, naechste Handlung, benanntes Hindernis, Muenzen, Vorrat,
Tagesminute). Der Snapshot gibt den `AgentState` ausdruecklich NICHT heraus: Wer den ganzen Kern
bekommt, veraendert ihn irgendwann von der Oberflaeche aus, und dann entsteht Leben an zwei
Stellen.

Fuenf simulierte Tage in Halbstundenschritten, deterministisch wiederholbar:

| Einwohner | oeffentlich im Fenster | Ankerort | dominante Ziele |
| --- | --- | --- | --- |
| Verkaufskraft | 45 von 147 (30 %) | SHOP 36, CITY 9 | GET_FOOD 98, REST 71, HAVE_FUN 16 |
| Parkstammgast | 21 von 129 (16 %) | PARK 21 | GET_FOOD 104, REST 60, DEVELOP 33 |
| Sportler | 42 von 161 (26 %) | SPORT 42 | GET_FOOD 88, **HAVE_FUN 74**, REST 52 |

**Zwei Dinge mussten dafuer stimmen, und beide waren zuerst falsch:**

1. **Der Ankerort vertritt auch die Arbeit.** Die Verkaufskraft arbeitet im Laden, aber die
   Domaene kennt fuer Arbeit nur `WORKPLACE`, und `siteFor(SHOP)` ist `MARKET`. Sie fiel beim
   Arbeiten auf die Kulisse WORK durch und stand im eigenen Laden nur beim EINKAUFEN - neun von
   240 Schnappschuessen. `LivingResident.anchorSites` sagt, welche Domaenenorte der Ankerort fuer
   diese Figur vertritt; danach sechsunddreissig.
2. **Die Oeffnungszeiten muessen mitwandern.** `WorldState.advanced` bewegt die Zeit, aber nicht
   `openSites`. Ohne Nachfuehren bei jedem Schritt truege ein morgens angelegter Einwohner bis in
   die Nacht die Oeffnungszeiten des Morgens mit sich - `SiteOpen` waere als Hindernis wirkungslos.

**Rolle neigt, sie zwingt nicht.** Eine Verkaufskraft mit Hunger 0,95 verlaesst den Arbeitsplatz,
geht zum Markt, kauft, geht nach Hause und isst. Kein Ablaufskript sagt ihr das; `GET_FOOD`
gewinnt schlicht die Wahl.

Was offen bleibt: Ihr Rollenbias liegt auf `EARN_MONEY`, und das Ziel gewinnt nie (NT-087). Der
Hebel dafuer ist eine Verwendung fuer Geld, kein Schwellwert. Und der Snapshot fuehrt
`minuteOfDay` je Einwohner, weil eine 180-Minuten-Schicht ueber die Zielminute hinausschiesst -
die drei koennen bis zu drei Stunden auseinanderliegen. NT-089 beruecksichtigt das beim Zeichnen.

### Mehrere wirkliche Einwohner im Bild (NT-089)

Die Anzeige erfindet keine Menge neben der Simulation. `DockScreen` stellt die drei
`resident:`-Profile aus dem vorhandenen Store wieder her, laesst `LivingPopulation` sie an der
simulierten Uhr weiterleben und liest fuer die Darstellung nur `ResidentSnapshot`. Ein Besuch
wird aus derselben Liste gewaehlt; waehrend er laeuft, faellt dieses Profil aus dem Hintergrund.

Die Lesbarkeitsgrenze aus NT-087 bestimmt die Form: Bei vierzig Szenenzellen ist der
Hauptavatar sechzehn Zellen breit. Deshalb stehen hoechstens zwei Einwohner halb so gross auf
festen freien Bahnen neben ihm und bleiben gedaempft. Auf Bildschirm, Schnappschuss und Clip
gelten dieselben relativen Positionen. `publiclyPresent` wird nicht aus der Uhr des Hauptavatars
neu berechnet; es traegt bereits den eigenen `minuteOfDay` des Einwohners. Derselbe Wert versetzt
auch seine Ruhebewegung, damit mehrere Wesen nicht im Gleichschritt atmen.

Bis NT-089 wurde bewusst noch keine gemeinsame Aktivitaet behauptet. Die Einwohner zeigten nur
ihre artgerechte Ruhebewegung. NT-091 fuehrt darauf die erste enge Ausnahme ein.

### Gemeinsames Training aus zwei wirklichen Handlungen (NT-091)

Am Sportplatz trainiert der Hauptavatar mit genau einem Einwohner, aber nur wenn vier Aussagen
zugleich wahr sind: Die vorbereitete Hauptaktion schliesst `MOVE_BODY` ab, die bereits gewaehlte
vorhandene Routine ist `TRAINING`, der Einwohner ist dort `publiclyPresent`, und seine eigene
ungehinderte `nextAction` ist ebenfalls `MOVE_BODY`. ATHLETE, WYRMLING oder SPORT allein sind
kein Beleg. Der Hauptavatar hat keinen `ResidentSnapshot`; seine Gegenwart und Handlung kommen
aus dem sichtbaren Play-Ablauf, die des Einwohners aus dem read-only Population-Snapshot.

Waehrend der Choreografie ist der Einwohner fuer Fortschreibung und Besuch beansprucht. Seine
Figur folgt denselben vorhandenen MOVE-/STRETCH-Regungen und wird ueber `residentFigures` in
Bildschirm, Schnappschuss und Clip gleich beschrieben. Erst nach dem sichtbaren Abschluss fuehrt
`LivingPopulation.completeSharedAction` seinen bereits geplanten Schritt durch
`LivingSimulation.step` aus. Damit entstehen Zeit, Beduerfniswirkung, Erinnerung und Geschmack
weiter ausschliesslich aus `ActionOutcome`; der Hauptavatar uebernimmt gleichzeitig seinen schon
vorbereiteten Schritt. Ein Abbruch uebernimmt keinen der beiden Zustaende.

Mehr wird daraus noch nicht abgeleitet. `MOVE_BODY` sagt nicht Fussball, Basketball, Drachen oder
Angeln. Diese Szenen bleiben offen, bis eine wirkliche Handlung oder eine ebenso enge belegte
Zuordnung sie unterscheidet. Ob zwei unterschiedlich grosse Figuren beim Training auf vierzig
Szenenzellen am Geraet klar zusammengehoeren, ist weiterhin `UNVERIFIED`.

### Vier Orte reichen - nachgemessen statt behauptet (NT-087)

Hier stand zuerst, die Grenze liege im Ortsmodell: Die Domaene fuehrt vier Orte, PARK, POND,
SPORT, FOREST, MEADOW, CITY und STREET seien darin alle `OUTSIDE`, und "im Park stehen ein
Spaziergaenger und ein Sportler" sei deshalb nicht formulierbar. **Das war falsch, und die
Messung hat es widerlegt.**

Sechs unabhaengig entscheidende Wesen, je 120 Schritte durch den vorhandenen Adapter, ohne eine
einzige Aenderung an der Domaene:

| Frage | Messung |
| --- | --- |
| Begegnungsgelegenheiten (gleicher sichtbarer Ort, 30 Minuten Fenster) | **1 662** |
| Paare ganz ohne Gelegenheit | **keines** |
| Verschiedene sichtbare Orte | **12 von 16** |
| Schritte, in denen die Figur draussen steht und der Kern HOME sagt | **1 von 720** |

**Nicht der Ort platziert ein Wesen, sondern das Thema.** `PURSUE_INTEREST` und die benannten
Beschaeftigungen aus NT-072 gehen ueber `PlayScene.forTopic` an den Ort, der zur Taetigkeit
gehoert. Zwei Wesen, die dasselbe tun wollen, stehen deshalb schon heute am selben Ort - dafuer
muss die Domaene keinen fuenften Ort kennen. Die Begruendung im KDoc von `WorldState` ("fuer eine
Entscheidung zaehlt davon fast nichts") stimmt und ist jetzt belegt.

Die offene Frage fuer eine Bevoelkerung ist damit nicht das Ortsmodell, sondern die
**Lesbarkeit**: Bei `PlayScene.MIN_SCENE_CELLS = 40` und einer 16 Zellen breiten Figur passen
drei bis vier Wesen nicht nebeneinander.

### Behaglichkeit war ein Ziel ohne Weg (NT-087)

Dieselbe Messung hat gezeigt, dass von acht Zielen **zwei nie gewinnen**. `SEEK_COMFORT` kam nie
ueber Rang 3 und nie ueber 0,157 Punkte - bei den GERINGSTEN Kosten aller acht Ziele. Es lag also
nicht am Aufwand, sondern am Druck: Behaglichkeit kam nie ueber 0,257, waehrend Hunger und Ruhe
1,0 erreichten.

Behaglichkeit waechst mit 0,02 je Stunde, dem langsamsten Wert von sieben - in achtzig
Simulationsstunden um 1,6. Erleichtert wurde sie im selben Lauf um rund 11, weil Essen, Ruhen,
Zuwendung und Bewegung alle nebenbei daran zogen und zusammen ueber zweihundertfuenfzig Mal
vorkamen. **Das ist genau der Befund, den NT-074 oben selbst aufschreibt** - behoben wurde damals
nur die Zielseite.

Mit kleineren Zahlen war das nicht zu heilen: Ein Versuch mit 0,08 / 0,12 / 0,10 / 0,05 liess das
Verhaeltnis bei 3,2 zu 1 und die beste Punktzahl bei 0,174. Deshalb eine Regel statt einer Zahl -
**Behaglichkeit stillt nur, was ihr gilt**: `SETTLE` und `TEND_SELF`. Seitdem wird `SEEK_COMFORT`
dreissig Mal im Tageslauf gewaehlt statt nie.

`EARN_MONEY` gewinnt weiterhin nie, und das ist kein Fehler: Wer weder hungrig noch knapp bei
Kasse ist, hat fuer Geld heute keine Verwendung - Muenzen zahlen ausschliesslich Essen.

### Die soziale Rechnung war echt, der sichtbare Besuch war es nicht

Seit NT-067 konnte der Kern eine Einladung aus `PLAY + QUESTION` zustandsabhaengig beantworten.
Im laufenden Spiel kam diese Rechnung jedoch nie an: `runVisit` spielte immer drei fest
wechselnde Sprechpunkte und Koerperregungen ab. Energie, sozialer Bedarf, Beziehung,
Persoenlichkeit und laufendes Ziel des Bewohners blieben fuer die Begegnung bedeutungslos.

NT-085 schliesst genau diese Laufzeitluecke. `LivingSimulation.exchangePlayInvitation` verbindet
die drei bereits vorhandenen sozialen Handlungen auf einer gemeinsamen Simulationszeit:
Einladung senden, aus dem Zustand antworten, Antwort wahrnehmen. Jede Wirkung laeuft weiter ueber
`ActionOutcome`. `runVisit` zeigt anschliessend nur die beiden tatsaechlichen Nachrichten ueber
dem jeweiligen Sprecher. Eine Annahme erscheint als `PLAY + YES`; Muedigkeit als `TIRED + NO`,
ein dringendes Essensziel als `FOOD + NO`. Der Bewohner behaelt die Beziehung und Episode in
seinem profilbezogenen Zustand.

Der Schnitt behauptet bewusst noch keine lebende Bevoelkerung. Der heutige Gast wird weiterhin
vom vorhandenen Besuchstakt erzeugt und nach der Begegnung verworfen. Persistente, nicht
waehlbare Einwohner mit eigenem Alltag, Beruf und Aufenthaltsort sind NT-086. Erst darauf bauen
mehrere gleichzeitig sichtbare Wesen, Verkaeufer und gemeinsame Aktivitaeten auf; neue Orte sind
nicht der erste Hebel, solange PARK, SPORT, SHOP, CITY, FOREST, MEADOW und POND noch leer sind.

NT-086 ersetzt den verworfenen Zufallsgast durch den kleinsten dauerhaften Einwohnerbestand:
eine Verkaufskraft mit Anker im SHOP, einen Parkstammgast und einen Sportler. Sie verwenden die
vorhandenen sechs Silhouetten, besitzen aber eigene `resident:`-Profil-IDs, sind damit nicht
waehlbar und teilen weder Geld noch Vorrat mit dem Hauptavatar. Ort und Uhrzeit bestimmen
deterministisch, wer fuer eine Begegnung in Frage kommt; eine feste Rotation ersetzt Zufall.
Nach dem sichtbaren Austausch werden beide Agenten samt Beduerfnissen, Episoden und Beziehung
gespeichert. Verschiedene letzte Handlungsminuten werden vor der Begegnung auf die spaetere Zeit
fortgeschrieben, nie zurueckgedreht.

Darauf bauen inzwischen zwei getrennte Schnitte auf: NT-088 schreibt den unsichtbaren Alltag
ueber denselben Living-Agent-Kern fort; NT-089 projiziert mehrere der daraus wirklich anwesenden
Wesen zugleich in die vorhandenen Orte. Gemeinsame Aktivitaeten bleiben NT-091.

### Die Stimmung kommt jetzt auch aus dem Wesen

Bis NT-074 stammte sie ausschliesslich aus dem Pflegebuch, also daraus, wie der NUTZER seinen Tag
gemacht hat. Zwei Folgen stoerten beim Zusehen:

1. Wer keine Tagesziele gesetzt hat, sah **immer** `NEUTRAL` - ein Wesen ohne jede Regung.
2. Dass ein hungriges, muedes, einsames Wesen gut gelaunt aussah, solange die Haekchen stimmten,
   war der sichtbarste Bruch zwischen dem, was das Modell weiss, und dem, was das Bild zeigt.

`AvatarMood.of(progress, wellbeing)` mittelt beide Quellen; ohne Tagesziele entscheidet allein
das Wohlbefinden. Am Grundsatz aendert das nichts: kein Verhungern, keine Strafe. Auch bei
vollstaendigem Elend ist `SAD` das Ende der Skala - trueber, nie verloren.

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
| `PlayVisitWindow` / `runVisit` | Zeigt seit NT-085 echte symbolische Einladung und zustandsabhaengige Antwort; Bevoelkerung folgt getrennt. |

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
