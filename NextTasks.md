# NextTasks.md

Priorisierte, unabhängige Arbeitspakete für Itoeva. Diese Datei enthält **maximal 15 aktuell
relevante Aufgaben**, geordnet nach tatsächlichem Hebel für belastbare Produktentscheidungen,
Entwicklungsgeschwindigkeit und Token-/Actions-Effizienz - alles Weitere steht kompakt im
[Future Backlog](#future-backlog) unten.
Grundlage ist die Analyse in [Architecture.md](Architecture.md); der Rahmen dafür steht in
[Vision.md](Vision.md) und [AgentGuide.md](AgentGuide.md).

**Wann diese Datei lesen:** nur wenn die aktuelle Aufgabe eine Prozessverbesserung ist, nicht bei
normalen Content-/Feature-Evolutionen aus `evolutions/BACKLOG.md` (siehe AgentGuide.md, Abschnitt
"Minimal-Startsequenz").

## Die eine Regel über allen anderen

Der Produktverantwortliche hat am 2026-09-10 das
[Itoeva Living Agent System](LIVING_AGENT.md) als naechsten groesseren Architektur-Meilenstein
ausdruecklich freigegeben. Die fruehere pauschale Sperre fuer neue groessere Gameplay-Systeme gilt
fuer diesen klar begrenzten Rahmen nicht mehr. Weiterhin gilt: kein monolithischer Umbau, keine
Quest-/Plotmaschine, keine zweite Renderpipeline und keine Cloud- oder Plattformarchitektur ohne
eigene Entscheidung. Jeder Schnitt muss klein, deterministisch testbar und einzeln ruecksetzbar
sein.

Der lokale Stream-Client NT-068 ist als Build-Variante derselben `:app-sim`-Runtime umgesetzt.
Der Messlauf NT-058 bleibt wichtig: Er soll nun nicht nur ein nachvollziehbares virtuelles Leben,
sondern auch Auto-Save und begrenzte Viewer-Impulse ueber einen laengeren Lauf beobachten.

## Top 15 - nach Hebel geordnet

Format je Aufgabe: **Rang - ID** - Aufgabe. *Erfolgskriterium.* (Abhaengigkeit; Aufwand)

1. **NT-058** - Begrenzten End-to-End-PoC fuer genau einen oeffentlichen Charakter durchfuehren:
   `:app-sim:assembleStream` im Emulator, Spielmodus dauerhaft sichtbar, Bild und App-Audio ueber OBS
   mindestens zwei Stunden lokal aufzeichnen. *`docs/streaming-poc.md` dokumentiert Stabilitaet,
   Vielfalt, Musik, Auto-Save, lokale Viewer-Impulse, erklaerbaren Agentenzustand,
   Ressourcenverbrauch und Neustartverhalten.* (NT-068; 120 min plus Beobachtungszeit)
   **Runbook und Messbogen sind fertig; der Lauf selbst braucht einen Menschen an einem Rechner
   mit Android Studio und OBS.** Eine Agentenumgebung hat weder SDK noch Emulator noch
   Aufzeichnung - siehe den Abschnitt "Warum NT-058 weiterhin offen ist" im Runbook.
2. **NT-069** - Den Wunsch und das Hindernis des Living Agents als Symbol ueber dem Kopf
   zeigen und den gewuerfelten Satz aus `PlaySpeech` dort ersetzen. *Beim Zusehen ist
   erkennbar, WAS das Wesen will und WORAN es haengt; kein freier Text mehr ueber dem Kopf, der
   Text beim Antippen bleibt.* (LivingSymbols aus NT-063ff.; 1 PR - braucht zwei neue
   Pixel-Symbole fuer QUESTION und NO, die vor dem Merge angesehen werden sollten)

   **Der Halbsatz verschwindet mit** (`PlaySpeech.habitHint`, "das hast du dir heute auch
   vorgenommen"). Ausdrueckliche Entscheidung des Auftraggebers am 2026-09-11, nachgefragt und
   beantwortet. Ueber dem Kopf bleibt damit ausschliesslich Symbolik - kein Resttext, auch kein
   kurzer. Die Verbindung zwischen seinem Tag und deinem geht dadurch nicht verloren: Sie steht
   weiterhin im Gespraech beim Antippen (`PlayTalk`, Nachahm-Vorschlag `Offer.Add`), und dort
   ist sie eine Antwort auf eine Frage statt ein ungefragter Hinweis. Genau das war der
   Einwand.
3. **NT-051** - Pruefen, ob der Bereichsdetektor aus `verify.yml` auch der Builder-Session einen
   kleineren relevanten Kontext geben kann. *Machbarkeit bewertet, gegebenenfalls prototypisch
   umgesetzt.* (keine; 90 min)
4. **NT-046** - Empirisch pruefen, welchen Kontext die Reviewer-Session erhaelt. *Gemessene
   Tokenersparnis oder begruendete Beibehaltung.* (keine; 60 min)
4. **NT-047** - Standard-Modellwahl je Aufgabentyp pruefen. *Auswahl begruendet dokumentiert oder
   angepasst.* (keine; 45 min)
5. **NT-005** - Gradle-Abhaengigkeits-Caching in den Hauptworkflows pruefen. *Gemessene
   Laufzeitwirkung.* (keine; 60 min)
6. **NT-004** - Timeout-Werte aller Jobs pruefen und ergaenzen. *Jeder Job hat einen begruendeten
    Grenzwert.* (keine; 30 min)
7. **NT-002** - Klaeren, ob `runner/` noch gebraucht wird. *Doppelte Pflege ist begruendet
    beendet oder dokumentiert.* (menschliche Freigabe fuer Entfernen; 60 min)
8. **NT-029** - `README.md` in kurzen Einstieg und Themendokumente aufteilen. *Unter 300
    Zeilen, keine verlorenen Inhalte oder toten Links.* (keine; 90 min)
9. **NT-030** - Kurze Zustaendigkeitsnotiz je Gradle-Modul ergaenzen. *Drei Dateien unter je 50
    Zeilen.* (keine; 60 min)
10. **NT-009** - Flaky-Rate des Emulator-Jobs ueber die letzten 20 Laeufe messen. *Kennzahl und
    gegebenenfalls neue Schwelle dokumentiert.* (keine; 60 min)
11. **NT-050** - Tokenverbrauch der letzten zehn Evolutionslaeufe als Baseline dokumentieren.
    *Zahl, Quelle und Datum stehen in der Architektur-/Evolutionsdokumentation.* (keine; 45 min)

12. **NT-007** - Die durchschnittlichen Minutenkosten der letzten zehn abgeschlossenen
    `evolve`-Laeufe ermitteln. *Zahl, Stichprobe und Messdatum bilden eine belastbare Baseline
    fuer weitere Workflow-Entscheidungen.* (keine; 60 min)

13. **NT-008** - Die API-Level-Matrix gegen `minSdk` und `targetSdk` pruefen. *Unterstuetzte
    Geraete, instrumentierte CI-Abdeckung und bewusst offene Luecken sind dokumentiert.*
    (keine; 45 min)

14. **NT-010** - PLAY_STORE.md-Signierungsdokumentation gegen den `release`-Job abgleichen.
    *Dokumentation und tatsaechlicher Release-Ablauf widersprechen sich nicht.* (keine; 45 min)

15. **NT-011** - Selbsttest fuer `backlog-select.sh` ergaenzen. *Die Auswahl des obersten offenen
    Eintrags und die leere Warteschlange sind reproduzierbar belegt.* (keine; 60 min)

## Future Backlog

Kompakt, ohne volle Erfolgskriterien - wichtig, aber aktuell nicht unter den 15 mit dem größten
Hebel auf Geschwindigkeit/Effizienz. Wird eine dieser Aufgaben durch veränderte Umstände
hebelstärker als eine Top-15-Aufgabe, rückt sie beim nächsten Pflegedurchlauf nach (siehe unten).

**Build/CI, weitere:** NT-012 Gradle/AGP-
Versionskonsistenz zwischen Workflows · NT-014 Branch-Protection-Stand nach Public-Wechsel
prüfen · NT-001 acht leere Stimmungs-/Prozent-Dateien im Wurzelverzeichnis entfernen.

**Tests ausbauen (alle zurückgestellt, nicht unwichtig - siehe Hinweis unten):** NT-015
Characterization-Tests für `DockScreen.kt` · NT-016 Compose-Tests für `app/ui/ReminderScreen.kt`
· NT-017 Migrationstest über alle Schema-Versionen · NT-018 Test "alle 4 Speicherplätze belegt"
(PR #20) · NT-019 Test TalkBack-Zusatzaktionen Speicherplätze (PR #20) · NT-020
Spezies-Testabdeckung `AvatarAnimationsTest` · NT-021 `PlayPath`-Testabdeckung `PlaySceneTest` ·
NT-022 Golden-/Snapshot-Test für eine `PlayScene`-Szene · NT-023 Instrumentierungstest
`ReminderWatchdogWorker` · NT-024 Platzhalter-Konsistenz in `StringResourceParityTest` · NT-025
Unit-Test `archiveActiveReminderIfExpired()` (PR #20) · NT-026 Langsamste Testklassen per
`--profile` ermitteln · NT-027 Property-Test `PlayGamePlan` schließt Medizin aus · NT-028
Zeitzonenwechsel während laufender Erinnerung.

**Agentenfreundlichkeit, weitere:** NT-031 `PlayScene.kt` aufteilen · NT-032 `DockScreen.kt`
aufteilen · NT-033 Zweite Room-Datenbank in `app-sim` begründen · NT-034 Recherche
`ReminderScreen`-Duplikat hebbar? · NT-035 Recherche `ReminderAnimations`-Duplikat hebbar? ·
NT-036 Zweck-Kommentare `SettingsCatalog.kt` vervollständigen · NT-037 `AgentGuide.md` vs.
Standard-Dateiname klären · NT-038 `runner/schemas/*.json` gegen `claude-primary-run.yml`
abgleichen · NT-039 Querverweise zwischen Doku-Dateien prüfen (wiederkehrend) · NT-040
`evolutions/001-idempotent-xp.md` gegen Backlog-Format prüfen · NT-041 Kommentarstil in den drei
größten Dateien stichprobenhaft prüfen · NT-042 Fehlende `@Preview` für HomeScreen/DockScreen ·
NT-054 Skills, Level und Jahreszeiten als gewichtete Erweiterungen des in `Tagesablauf.md`
definierten Systems spezifizieren und validieren, bevor dazu größerer Gameplay-Code entsteht.

**Produktentscheidungen, offen:** NT-053 Animations-/Habit-Slots vor jedem Gameplay-Code als
strategische Mechanik spezifizieren und mit Nutzern oder einem schlanken Prototyp validieren;
Auswahlwirkung, Sichtbarkeit und die Entscheidung "jetzt einsetzen oder für später aufheben?"
müssen vor einer größeren Umsetzung belegt sein. · NT-055 Musik - *Format (Ogg/Vorbis), Ort der Umwandlung
(Pipeline), die Grenze der Ausnahme (nur der Score) sowie die kontextabhaengige Auswahl ueber
`MusicRole`/`MusicResolver` sind am 2026-09-05 entschieden und umgesetzt; siehe EVOLUTION.md.*
`main-day-01` und `home-evening-01` werden inzwischen ausgeliefert; Rollenwechsel sind seit der
nachfolgenden Musik-Evolution vier Sekunden lang ueberblendet und Sport setzt Ort plus echte
MOVE-Beschaeftigung voraus. **Offen bleibt:** (a) Lautheit, True Peak, lange Stille und
Loop-Grenzen als reproduzierbares Freigabe-Gate der Erzeugungspipeline definieren; (b) mehrere
gepruefte Varianten je Rolle ermoeglichen, ohne die Weltlogik an Dateinamen zu binden; (c) ein
gemeinsames musikalisches Leitmotiv festlegen, bevor Morgen-, Sport- und Traumtrack unabhaengig
auseinanderlaufen; (d) ob realistische generierte Musik ueber einer 16x16-Welt aesthetisch
richtig ist; (e) die Lizenzpruefung aus `music/README.md` vor einer kommerziellen
Veroeffentlichung.

**Öffentliche Streams, gestufte Roadmap:** NT-059 Ergebnis von NT-058 auswerten und ausdrücklich
zwischen drei Wegen entscheiden: Emulator-basierter Einzelbetrieb weiter testen, zuerst
`:app-sim` für Dauerbetrieb härten oder eine reine Weltengine untersuchen; ohne Messdaten keine
Auswahl. · NT-060 danach genau eine Charakterinstanz auf einem isolierten Cloud-Host mit
Checkpoint, Prozessaufsicht, Secret-Verwaltung und Gesundheitsprüfung erproben; Kosten pro
24 Stunden messen, noch keine Sechser-Flotte. · NT-061 erst nach stabilem Einzelbetrieb
parametrisierte Instanzen je Wesen und einen typisierten, auditierbaren Ereigniskanal für
Begegnungen spezifizieren; keine privaten Daten und kein ungeprüfter Freitext. · NT-062 vor einem
öffentlichen Simulcast die dann aktuellen YouTube-/Twitch-Regeln, Musikrechte, KI-Transparenz,
Moderation, Beschreibungstexte und dezente App-Hinweise als eigenes Freigabe-Gate prüfen.

**Welt und Tagesablauf, offen:** NT-056 **Begegnung waehrend eines Aufenthalts draussen** -
*am 2026-09-05 umgesetzt; siehe EVOLUTION.md.* Die Bedingung ist als `PlayVisitWindow.isOpen`
aus `DockScreen` herausgeloest und geprueft; ein Besuch ist waehrend eines `RoutineStep.Linger`
unter freiem Himmel moeglich, und der Ablauf wartet danach auf das Ende des Besuchs. **Offen
bleibt:** ob ein Besuch auch dann kommen darf, wenn die Figur draussen auf einer Bank SITZT
(`occupied`) - die laengsten Aussenpausen liegen genau dort, aber "wer sitzt, faellt heraus" war
eine bewusste Entscheidung und gehoert nicht nebenbei umgedreht. · NT-057
**Mindestdauer eines dynamischen Zustands** - *am 2026-09-06 umgesetzt; siehe EVOLUTION.md.* Die
Entwurfsfrage ist zugunsten des ZUSTANDS entschieden: `PlayOutdoorStay` haelt einen Aufenthalt
unter freiem Himmel mindestens neunzig Sekunden, und die Musik folgt daraufhin von selbst. Eine
Regel im Player haette das Symptom behandelt und ausserdem Ton und Bild entkoppelt. **Offen
bleibt:** ob neunzig Sekunden richtig sind - das ist eine Zahl und erst am Geraet zu beurteilen.

**Stream und Zuschauer:** NT-070 **Kostenlose Zuschauer-Interaktion ueber den Twitch-Chat** -
*am 2026-09-12 umgesetzt; siehe `docs/streaming-interaction.md`.* Zuschauer loesen mit `!drop A`
bis `!drop D` einen gespeicherten Platz aus. Die Kette Chat -> Provider -> normalisierte
`StreamInteraction` -> bestehende Auswahl -> Living Agent steht; ein Test schickt dasselbe
Angebot aus drei Quellen durch und verlangt dasselbe Ergebnis, damit die spaetere Bits-Anbindung
die Spielschicht nicht anfassen muss. Anonyme Leseverbindung ohne Token, Netzberechtigung nur im
Stream-Quellsatz. **Offen bleibt:** der Lauf an einem echten Kanal mit echtem Publikum (gehoert
zu NT-058) und die Frage, ob 60 s je Zuschauer und 8 s gemeinsam die richtigen Abstaende sind -
das sind Vermutungen und erst vor Publikum zu beurteilen.

**Living Agent, weiter:** NT-072 **Jeder Reminder bekommt eine eigene Handlung** - *am
2026-09-12 umgesetzt; siehe LIVING_AGENT.md.* Acht der zwoelf Reminder-Typen liefen im Kern
durch eine einzige generische Handlung und stillten damit exakt dasselbe. Jeder hat jetzt seine
eigene Wirkung; Fuersorge stillt nur Behaglichkeit, Bewegung macht hungrig. Die gewichtete
Themenwahl der Laufzeitschicht bleibt unangetastet und wird dem Planer als Ausformung
hereingereicht. Dazu `Needs.wellbeing()` und ein XP-Zuschlag nach tatsaechlicher Wirkung.
**Offen bleibt:** ob die Zahlen sich am Geraet richtig anfuehlen - sie sind begruendet, aber
nicht erprobt; und ob das Wohlbefinden auch die sichtbare Stimmung faerben soll (heute stammt
die ausschliesslich aus dem Pflegebuch, siehe AvatarMoodState).

**Welt, weiter:** NT-073 **Draussen als Ziel, Traeume ausserhalb der Nacht** - *am 2026-09-12
umgesetzt; siehe LIVING_AGENT.md.* Beide Beobachtungen vom Geraet hatten dieselbe Art Ursache:
Die Sache gab es im Modell nicht. `LivingSite.OUTSIDE` wurde von keinem Requirement verlangt und
stand deshalb in keinem Plan; Traeume liefen nur im Nachtschlaf ab 23 Uhr und waren abends
unerreichbar. Jetzt verlangt `MOVE_BODY` den Aufenthalt draussen, und `RoutineStep.Daydream`
traegt eine Traumgelegenheit auf dem Sofa, beim Innehalten, auf der Bank und im Nickerchen.
**Offen bleibt:** ob es sich am Geraet nach genug anfuehlt - 28 Prozent Tagtraum und der
Ausgleich fuer den Weg nach draussen sind begruendete Schaetzungen, keine Messungen.

**Living Agent, weiter:** NT-074 **Jedes Beduerfnis wird ein Antrieb; Stimmung aus dem
Wohlbefinden** - *am 2026-09-12 umgesetzt; siehe LIVING_AGENT.md.* Von sieben Beduerfnissen
trieben nur fuenf ein Ziel: Neugier und Behaglichkeit wuchsen mit, ohne je ein Grund zu sein.
Neu sind `EXPLORE` und `SEEK_COMFORT`; ein Test haelt fest, dass jedes Beduerfnis ein Ziel
traegt. `CONNECT_WITH` faellt ohne Gegenueber auf `SHOW_AFFECTION` zurueck, statt unerreichbar
zu sein. Die Stimmung mittelt Pflegebuch und `Needs.wellbeing()`; ohne Tagesziele entscheidet
allein das Wohlbefinden. **Offen bleibt:** ob sich die Gewichtung 50/50 richtig anfuehlt, und ob
Erkunden und Bewegung beim Zusehen wirklich unterscheidbar sind - beides ist begruendet, nicht
gemessen.

**Living Agent, sozial:** NT-085 **Der sichtbare Besuch verwendet die echte symbolische
Verstaendigung** - *am 2026-09-14 umgesetzt.* `runVisit` spielt nicht mehr drei feste
Pseudo-Wortwechsel. Der Gast sendet `PLAY + QUESTION`; der Living Agent antwortet aus Energie,
sozialem Bedarf, Beziehung, Persoenlichkeit und aktuellem Ziel. Einladung und Antwort stehen als
Pixelsymbole ueber dem wirklichen Sprecher, einschliesslich eines neuen Ja-Hakens. Die Beziehung
und Episode des Bewohners werden ueber `ActionOutcome` gespeichert. **Offen bleibt:** Der Gast
ist mit NT-086 zum persistenten Einwohner geworden; sein eigener Tagesablauf folgt in NT-088.

**Living Agent, sozial:** NT-086 **Persistente Weltbewohner statt zufaelliger Kulissengaeste** -
*am 2026-09-14 umgesetzt.* Eine Verkaufskraft, ein Parkstammgast und ein Sportler besitzen eigene
nicht waehlbare `resident:`-Profil-IDs, Rollenbias, Ankerort und Anwesenheitsfenster. Der
Besuchstakt waehlt sie deterministisch nach Ort, Zeit und fester Rotation. Beide Seiten einer
sichtbaren Begegnung werden getrennt gespeichert; Muenzen und Vorrat bleiben je Profil getrennt,
verschiedene letzte Simulationsminuten werden vor dem Austausch vorwaerts synchronisiert.
**Offen bleibt:** Die Einwohner entscheiden ihren Tagesablauf zwischen Besuchen noch nicht
selbst, und der Renderer zeigt weiterhin nur einen Gast zugleich.

**Living Agent, sozial:** NT-088 **Einwohner entscheiden Aufenthalt und Aktivitaet selbst** -
*am 2026-09-15 umgesetzt.* Die drei persistenten Einwohner ueber denselben Living-Agent-Kern in kleinen
deterministischen Zeitschritten fortschreiben und einen read-only Population-Snapshot mit
Profil, Ort, Ziel, naechster Handlung und Rolle bereitstellen. Rolle ist nur Bias und
Weltvoraussetzung: Auch die Verkaufskraft darf bei dringendem Hunger oder Muedigkeit den SHOP
verlassen. *Nach mehreren simulierten Tagen sind Aufenthalt und Aktivitaet jedes Einwohners aus
Beduerfnissen, Ressourcen und Oeffnungszeiten erklaerbar; zwei Bewohner entwickeln verschiedene
Historien.* Noch kein Mehrfach-Renderer. NT-089 zeigt danach mehrere tatsaechlich anwesende
Bewohner gleichzeitig; gemeinsame Sport-, Drachen- und Angelhandlungen folgen getrennt.

`LivingPopulation` schreibt die drei ueber denselben `LivingSimulation.step` fort und liefert
einen read-only `ResidentSnapshot`. Ueber fuenf simulierte Tage in Halbstundenschritten: Die
Verkaufskraft ist 30 Prozent ihres Fensters oeffentlich zu sehen (SHOP 36, CITY 9), der
Parkstammgast 16 Prozent (PARK 21), der Sportler 26 Prozent (SPORT 42) - und die Rollen sind an
den Zielen ablesbar, ohne Ablaufskript (Sportler HAVE_FUN 74, Parkgast DEVELOP 33). Zwei Laeufe
ergeben denselben Zustand. **Der Fund unterwegs:** Die Verkaufskraft arbeitet im Laden, aber die
Domaene kennt fuer Arbeit nur `WORKPLACE` und `siteFor(SHOP)` ist `MARKET` - sie fiel beim
Arbeiten auf die Kulisse WORK durch und stand im eigenen Laden nur beim EINKAUFEN, neun von 240
Schnappschuessen. Mit `anchorSites` sind es sechsunddreissig. **Offen bleibt:** Der Rollenbias der
Verkaufskraft liegt auf `EARN_MONEY`, und das Ziel gewinnt nie (NT-087: -0,43 bei Kosten 58) - ihr
Bias ist wirkungslos, und der Hebel dafuer ist eine Verwendung fuer Geld, kein Schwellwert. Und
der Schnappschuss fuehrt `minuteOfDay` je Einwohner, weil eine 180-Minuten-Schicht ueber die
Zielminute hinausschiesst; die drei koennen bis zu drei Stunden auseinanderliegen, was NT-089
beim Zeichnen beruecksichtigen muss.

**Living Agent, sozial:** NT-089 **Mehrere tatsaechlich anwesende Einwohner sind sichtbar** -
*am 2026-09-15 umgesetzt.* `DockScreen` stellt die drei `resident:`-Profile aus demselben
`LivingAgentStore` wieder her, schreibt sie ueber `LivingPopulation` an der simulierten Uhr fort
und zeichnet an einem Ort hoechstens zwei wirklich `publiclyPresent` gemeldete Wesen. Bei der
Normalbreite von vierzig Szenenzellen stehen sie halb so gross und gedaempft neben dem
Hauptavatar; feste Bahnen verhindern Ueberdeckung. Ihr eigener `minuteOfDay` versetzt den
Ruhetakt, statt drei Kopien im Gleichschritt zu zeigen. Ein aktiver Besuch kommt aus derselben
Anwesenheitsliste und wird im Hintergrund ausgeschlossen. Bildschirm, Schnappschuss und Clip
verwenden dieselbe relative Figurenbeschreibung. **Offen bleibt:** Die Einwohner zeigen vorerst
nur ihre artgerechte Ruhebewegung. NT-091 verbindet als eigener Schnitt kompatible wirkliche
Handlungen zu gemeinsamen Sport-, Drachen- oder Angelszenen; ihre Anwesenheit allein darf keine
Aktivitaet vorspielen.

**Living Agent, sozial:** NT-091 **Gemeinsame Aktivitaet aus wirklichen Handlungen** - *am
2026-09-16 als kleinster Schnitt umgesetzt.* Hauptavatar und genau ein `publiclyPresent`
gemeldeter Einwohner trainieren gemeinsam am SPORT, wenn der vorbereitete Hauptschritt wirklich
`MOVE_BODY` abschliesst, seine bereits gewaehlte Choreografie `TRAINING` ist und der Einwohner
am selben Ort selbst ungehindert `MOVE_BODY` als `nextAction` traegt. Die Population pausiert
waehrend des sichtbaren Ablaufs; erst nach dessen vollstaendigem Abschluss laufen beide
Handlungen ueber den vorhandenen `ActionOutcome`-Weg in ihre getrennten Zustaende und eine
gemeinsame SharedPreferences-Transaktion.
Ein Abbruch verbucht keine von beiden. Rolle, Spezies und Ort allein reichen ausdruecklich nicht.
Bildschirm, Schnappschuss und Clip verwenden dieselbe mitbewegte Einwohnerfigur. **Offen bleibt:**
Drachen, Fussball, Basketball und Angeln sind aus `MOVE_BODY` nicht unterscheidbar und werden
nicht geraten. Sie brauchen entweder eine spezifischere wirkliche Handlung oder eine ebenso eng
belegte Zuordnung; die Lesbarkeit am Geraet bleibt fuer die erste Szene `UNVERIFIED`.

**Living Agent, sozial:** NT-092 **Zweite gemeinsame Aktivitaet gepruefte und bewusst NICHT
umgesetzt** - *am 2026-09-16 abgeschlossen als dokumentierter Negativbefund.* Gepruefte
Ausgangsfrage: Der Living-Agent-Kern kennt fuer Einwohner ausschliesslich `ActionKind.MOVE_BODY`,
keine einzelne Sportart, und `LivingPopulation.interestFor` leitet nie eine Sportart her. Ein
erster Entwurf erweiterte `sharedTrainingPartner` um BASKETBALL mit der Begruendung, dieselbe in
NT-091 akzeptierte, asymmetrische Regel (Hauptavatar-Seite real, Einwohner-Seite generisch)
trage mechanisch eine zweite Aktivitaet. Ein automatisches Review (Codex, P1, PR #160) hat das
widerlegt: Die Einwohner-Seite blieb fuer TRAINING und BASKETBALL identisch - dieselbe Regel
haette ebenso Fussball, Drachen oder Angeln "belegt", obwohl `EVOLUTION.md` (NT-091) genau diese
als aus `MOVE_BODY` nicht herleitbar benennt. Der Entwurf wurde vollstaendig zurueckgenommen; der
Code entspricht wieder exakt dem gemergten NT-091-Stand (nur TRAINING). **Offen bleibt:** Eine
zweite gemeinsame Aktivitaet braucht zuerst ein echtes, deterministisches Einwohner-Signal
(Feld am geplanten Schritt oder ein neues `Requirement`), das eine konkrete Sonderaktivitaet aus
dem wirklichen Plan ausdrueckt - nicht per Rolle oder Zufall geraten, und keine allgemeine
Aktivitaets- oder Skillplattform. Bis dahin bleibt TRAINING die einzige gemeinsame
Sportplatz-Aktivitaet.

**Living Agent, sozial:** NT-093 **Basketball wird ehrlich gemeinsam: ein echtes
Einwohner-Signal** - *am 2026-09-16 umgesetzt.* Schliesst genau die in NT-092 offen gelassene
Luecke. `LivingPopulation.specialActivityFor` leitet deterministisch aus Einwohner-Index und
Simulationstag her, ob ein geplantes `MOVE_BODY` `TRAINING` oder `BASKETBALL` meint - nach
demselben Muster wie `interestFor`s Themenrotation, nicht per Rolle oder Zufall.
`ResidentSnapshot.nextSpecialActivity` traegt diesen Wert; `LivingPopulationLayout.
sharedSportPartner` verlangt jetzt zusaetzlich, dass er mit der Aktivitaet des Hauptavatars
uebereinstimmt.
Beide Seiten leiten ihre konkrete Absicht damit unabhaengig voneinander her, eine gemeinsame
Szene entsteht nur bei echter Koinzidenz. **Offen bleibt:** Fussball, Drachen und Angeln brauchen
weiterhin entweder eine eigene Ortszuordnung (Drachen: PARK, Angeln: POND) oder eine Loesung fuer
hauptavatarbezogenen Zusatzzustand (Fussballtrick), bevor sich dasselbe Muster ein drittes Mal
anwenden laesst. Die Lesbarkeit von Training UND Basketball gemeinsam auf vierzig Szenenzellen
bleibt `UNVERIFIED`.

**Darstellung:** NT-075 **Die Kreaturen bekommen Volumen** - *am 2026-09-12 umgesetzt.* Jede
beleuchtete Zelle stand auf voller Helligkeit; die Figur war eine reine An/Aus-Flaeche.
`AvatarShading` legt beim Zeichnen einen Verlauf darueber, von oben links hell nach unten rechts
dunkel. Bewusst ein Verlauf und keine Kantenerkennung: Der erste Entwurf streifte duenne Ohren
hell-dunkel, gab abgesetzten Fuessen das hellste Licht der Figur und liess Aussenspalten
zeilenweise flimmern. Angewandt beim Zeichnen, nicht in den Animationsdaten - die Posen bleiben
reine Punktmengen und die Golden-Datei der Reaktionspruefung gueltig. **Offen bleibt:** ob 0,42
am unteren Ende und 0,18 Seitenlicht am Geraet richtig wirken; beurteilt wurde es an
ausgedruckten Silhouetten, nicht auf dem Bildschirm.

**Darstellung:** NT-076 **Jede Kreatur bekommt ihre eigene Farbe** - *am 2026-09-12 umgesetzt.*
Alle sechs waren derselbe warme Weisston. `AvatarPalette` gibt jeder den Akzentton ihres
Schwerpunkts aus `ui/AnimationVisuals.kt` - FOCUS-Violett fuer den weisen Beobachter,
MOVE-Orange fuer den Motivator -, also keine neue Farbquelle, sondern die vorhandene. Angehoben
wurde nur die Helligkeit, nicht der Farbton: Die Akzentfarben sind fuer Beschriftungen auf
hellem Grund gemacht und waeren als leuchtende Figur auf Schwarz zu dunkel. Dazu
`AvatarShading` - siehe NT-078, der diesen Verlauf spaeter ersetzt hat. Eingefaerbt wird nur die Kreatur;
Kulisse, Glyph-Matrix und die Zeichen in Wunsch- und Traumblase bleiben weiss. **Offen bleibt:**
ob die sechs Toene auf dem Geraet gefallen - geprueft ist bisher nur, dass sie gleich hell,
unten lesbar und voneinander unterscheidbar sind.

**Darstellung:** NT-078 **Der Schatten wird sichtbar und zeigt die Laufrichtung** - *am
2026-09-13 umgesetzt.* Die Schattierung aus NT-075 war ein weicher Verlauf ueber die Hoehe, und
man sah sie nicht: Dreissig Prozent Unterschied auf sechzehn Zeilen sind zwei Prozent von einer
Zeile zur naechsten - kein Auge trennt das. Nachgezaehlt am Vorbild: Dessen Kreatur hat **genau
zwei Toene** (8876 zu 1896 Pixel, kein Zwischenwert), und der dunkle liegt als **Band auf einer
Flanke**, ueber die aeusseren 30 Prozent der Breite. Was ein Auge als Koerper liest, ist eine
Kante zwischen zwei Flaechen, kein sanfter Uebergang. `AvatarShading` macht das jetzt so - und
weil der Schatten auf EINER Seite liegt, traegt er zugleich die Richtung: Beim Gang nach links
springt die Kante auf die andere Seite, und die Figur dreht sich sichtbar um, obwohl das Sprite
nirgends gespiegelt wird. Dazu ein echter Schritt: Die Fuesse spreizten sich bisher symmetrisch
(ein Huepfen auf der Stelle), jetzt hebt abwechselnd einer ab, waehrend der andere steht.
**Offen bleibt:** ob 0,875 als Tonabstand am Geraet reicht - es ist der Wert des Vorbilds, aber
dessen Figur ist groesser als unsere.

**Darstellung:** NT-079 **Die Zeichen ueber dem Kopf waren zerschert** - *am 2026-09-13
umgesetzt.* In der Wunsch- und der Traumblase war nur Rauschen zu sehen. Die Ursache ist keine
Gestaltungsfrage: Die Zeichen liegen auf dem 13x13-Raster der Matrix, gezeichnet wurden sie aber
von `AvatarSpriteView`, und die liest mit der Zeilenbreite des Avatars, also 16. Jede Zeile
rutschte dadurch um drei Spalten weiter - das Bild wurde diagonal zerschert. Dazu kam das
erzwungene Seitenverhaeltnis 16:20, das ein quadratisches Zeichen zusaetzlich stauchte. Beide
Blasen benutzen jetzt `SimulatedMatrixView` - genau die Ansicht, mit der die Speicherplaetze
dieselben Zeichen schon immer gezeichnet haben, und damit auch die Gleichheit, die
`LivingSymbolFrames` ausdruecklich anstrebt. **Offen bleibt:** ob die Zeichen jetzt, richtig
gezeichnet, auch verstaendlich sind - das war bisher gar nicht zu beurteilen.

**Darstellung:** NT-080 **Schatten nur in Bewegung, Farbe nur im Gesicht** - *am 2026-09-13
umgesetzt.* Zwei Rueckmeldungen vom Geraet, beide berechtigt. Erstens lag der Schatten staendig
an derselben Stelle - damit ist er ein Muster auf der Haut, an das man sich in Sekunden
gewoehnt, und er sagt nichts, weil er im Stand derselbe ist wie im Lauf. Jetzt steht die Kreatur
ohne Schatten da, und er erscheint nur waehrend eines Gangs auf der Flanke, von der sie KOMMT.
Zweitens war die ganze Figur eingefaerbt: eine einfarbige Flaeche in Kreaturform, die zwar sagt,
welches Wesen es ist, aber nichts darueber, was daran ein Gesicht ist. Der Koerper ist wieder
weiss wie die Welt; die Farbe sitzt im Gesicht, und zwar in genau den kraeftigen Toenen der
Kreise aus der Erinnerungsliste (die aufgehellten aus NT-076 waren fuer eine grosse Flaeche auf
Schwarz gerechnet - auf weissem Koerper zaehlt der Kontrast andersherum, gemessen 2,9 bis 5,7
gegenueber durchgehend 2,7). Das Gesicht wird beim Zeichnen GEFUNDEN statt uebergeben: Augen
sind Loecher in der Silhouette, und durch `FrameCrossfade` kommt nur An, Aus und Ueberblendung
an - eine Flutfuellung vom Bildrand her findet die umschlossenen Stellen. Ab zwei
zusammenhaengenden Zellen, weil WYRMLINGs Fluegel sonst einzelne Randzellen aufblitzen liesse.
**Offen bleibt:** ob der Akzent gross genug ist - bei vier der sechs Kreaturen sind es vier
Zellen.

**Darstellung:** NT-081 **Das Fussballspielen war kaum zu erkennen** - *am 2026-09-13
umgesetzt.* Gemeldet als "das Fussball spielen kann man kaum erkennen", und daran war alles
wahr. Die Szene offline gerendert und nachgezaehlt ergab fuenf Befunde, von denen jeder einzeln
gereicht haette. **Der Ball lag hinter dem Tor:** Die Zellenliste endete auf `distinctBy`, das
den ERSTEN Eintrag behaelt, und das Tor stand vorne - zwischen "er zielt" und "der Ball liegt im
Tor" aenderten sich 10 von 72 Zellen. Der Treffer, auf den die ganze halbe Minute zulaeuft, war
unsichtbar. **Die Figur stand im Tor:** Bei der kleinsten Bildbreite (40 Zellen - auf einem
Telefon im Hochformat der Normalfall, nicht der Grenzfall) lag der Ball bei x=22 und der linke
Pfosten bei x=25; der "Schuss" war ein Ball, der drei Zellen weit umfiel. **Das Tor war ein
geschlossenes Rechteck** mit Punktraster darin - also ein Fenster; ein Tor hat zwei Pfosten,
eine Latte und ist unten offen. **Der Schuss hatte keinen Flug** - der Ball stand am Fuss und im
naechsten Takt im Netz. **Und der Ball war ein Ei:** fuenf breit, sechs hoch, was am Boden
niemand sah, weil die unterste Zeile unter dem Boden weggeschnitten wurde, in der Luft aber
dastand; er drehte sich ueber vierzig Takte nie und dribbelte in zwei Stellungen auf gleicher
Hoehe im Sekundentakt - dasselbe Blinken zweier Bilder, das beim Basketball nebenan schon einmal
auffiel. Jetzt: Ball vorn in der Liste (seine Freistellung schneidet ihn sauber aus dem Netz),
Tor unten offen und breiter als hoch mit schraegen Maschen, garantiertes Feld zwischen Ball und
Pfosten, ein Schuss mit Bogen unter der Latte, ein runder Ball, dessen Naht sich mit dem
zurueckgelegten WEG dreht - rueckwaerts, wenn er zurueckrollt, und gar nicht, wenn er liegt.
Damit der Flug ueberhaupt einmal stattfindet, zaehlt `footballCells` jetzt die Takte SEIT
BEGINN DER PHASE statt seit Beginn der Szene; ein freilaufender Zaehler traefe ihn nur
zufaellig. Nebenbei: `PlayEffectsTest` und `PlayInkTest` liefen bis hierher NUR in der CI und
stehen jetzt im Offline-Lauf (459 Tests) - genau die Luecke, die in NT-078 schon einmal einen
roten Lauf erzeugt hat. **Offen bleibt:** ob die Maschen am Geraet als Netz lesbar sind und ob
der Ball mit fuenf Zellen neben einer sechzehn Zellen breiten Figur nicht zu gross wirkt.

**Living Agent, weiter:** NT-087 **Vier Orte reichen; Behaglichkeit war unerreichbar** - *am
2026-09-14 umgesetzt.* Vor der zweiten Haelfte von NT-086 stand die Architekturentscheidung, ob
`LivingSite` feiner werden muss. Ich hatte das selbst als die eigentliche Huerde bezeichnet - die
Messung sagt: **keines von beiden**. Sechs unabhaengig entscheidende Wesen, je 120 Schritte durch
den vorhandenen Adapter: **1 662 Begegnungsgelegenheiten**, **kein Paar** ohne Gelegenheit, **12
von 16** sichtbaren Orten, und in **1 von 720** Schritten wich der Kern vom sichtbaren Ort ab.
Nicht der Ort platziert ein Wesen, sondern das Thema; `PlayScene.forTopic` fuehrt zwei Wesen mit
demselben Vorhaben schon heute an denselben Ort. `LivingSite` bleibt bei vier, und die
Begruendung im KDoc ist jetzt belegt statt behauptet. **Der eigentliche Fund:** Von acht Zielen
gewannen zwei NIE. `SEEK_COMFORT` kam nie ueber Rang 3 und nie ueber 0,157 Punkte - bei den
geringsten Kosten aller acht Ziele. Behaglichkeit waechst mit 0,02 je Stunde (in 80
Simulationsstunden also 1,6) und wurde im selben Lauf um rund 11 erleichtert, weil Essen, Ruhen,
Zuwendung und Bewegung alle nebenbei daran zogen - genau der Befund, den NT-074 selbst
aufgeschrieben, aber nur auf der Zielseite behoben hat. Mit kleineren Zahlen war das nicht zu
heilen (Versuch 0,08/0,12/0,10/0,05: Verhaeltnis blieb 3,2 zu 1, Punktzahl 0,157 -> 0,174).
Jetzt gilt die Regel statt der Zahl: Behaglichkeit stillt nur, was ihr gilt (`SETTLE`,
`TEND_SELF`). `SEEK_COMFORT` wird seitdem 30 Mal im Tageslauf gewaehlt statt nie, `SETTLE` 30 Mal
ausgefuehrt statt 6; DEVELOP faellt dabei von 39 auf 24, weil es sich den langsam wachsenden
Bereich jetzt teilt. **Offen bleibt:** `EARN_MONEY` gewinnt weiterhin nie (-0,434 bei Kosten 58)
- kein Fehler, solange Muenzen ausschliesslich Essen zahlen; ein eigener Antrieb fuer Geld
braucht erst etwas, wofuer sich Sparen lohnt. Und fuer die Bevoelkerung ist die naechste Huerde
die Lesbarkeit, nicht das Ortsmodell: Bei `MIN_SCENE_CELLS = 40` und einer 16 Zellen breiten
Figur passen drei bis vier Wesen nicht nebeneinander.

**Living Agent, weiter:** NT-086 **Der Gast wird ein Wesen mit Gedaechtnis** - *erste Haelfte am
2026-09-14 umgesetzt.* NT-085 hat die Begegnung echt gemacht, den Gast aber nicht: Er wurde bei
jedem Besuch neu erfunden und danach verworfen, gespeichert wurde nur die Seite des Bewohners.
Die Beziehung, die der Kern auf BEIDEN Seiten rechnet, hielt damit genau so lange wie der Besuch.
Jetzt wird er aus dem vorhandenen `LivingAgentStore` geladen und unter eigener Kennung wieder
gespeichert - derselbe Store, derselbe Codec, ein zweiter Schluessel; `restore` traegt seine
Beduerfnisse um die verstrichene Zeit weiter, er hat also gelebt statt gewartet. Zwei Funde
mussten dafuer erst getrennt werden: **Die Kennung** - `AvatarSpeciesPrefs.profileId` ist der
blosse Speziesname, und darunter liegt der Zustand des SPIELERS, sobald er diese Kreatur waehlt;
ein gespeicherter Gast waere beim naechsten Speziestausch zum eigenen Avatar geworden. **Die
Welt** - `WorldState` traegt Muenzen und Vorrat, die der sichtbaren Welt des Spielers gehoeren;
unveraendert uebernommen haette der Gast Geldbeutel und Speisekammer geerbt. Beleg: dieselbe
Einladung an denselben ausgeruhten Bewohner ergibt mit mitgebrachtem Gast vier Interaktionen und
mehr Naehe als mit einem Gast ohne Gedaechtnis mit zwei. **Offen bleibt:** die zweite Haelfte -
Einwohner, die ihren Aufenthalt selbst waehlen. Davor steht eine Architekturentscheidung, die im
bisherigen Plan fehlt: Die Domaene fuehrt VIER Orte (`LivingSite`), und PARK, POND, SPORT,
FOREST, MEADOW, CITY und STREET sind darin alle `OUTSIDE`. "Im Park stehen zwei Einwohner" ist
dort nicht formulierbar. Nebenbei aufgefallen und nicht geaendert: Ein HUNGRIGER Bewohner sagt zu
einer Spieleinladung trotzdem zu - `FOOD + NO` entsteht in der Praxis also seltener, als die
Dokumentation nahelegt.

**Darstellung:** NT-082 **Die anderen Szenen standen still** - *am 2026-09-13 umgesetzt.*
Dieselbe Messung wie beim Fussball ueber alle Mehrphasen-Szenen laufen lassen: Wie viele
verschiedene Bilder ergibt eine Phase ueber vierzig Takte, also acht Sekunden? Basketball AIM,
SHOOT und SCORE je EINS, Training REST eins, Musik TUNE eins, Angeln CAST und CATCH je eins, alle
drei Malphasen eins. Zusammen ueber fuenfzig Sekunden, in denen die Welt bewegungslos dasteht,
waehrend eine Figur angeblich wirft, malt oder angelt. **Die Malszene war der schlimmste Fund:**
Die Staffelei stand fest rechts neben der Figur und klappte nach links um, wenn dort kein Platz
war - ob LINKS Platz ist, hat niemand gefragt. Bei 40 Zellen lagen von 19 Spalten 13 im Bild, der
Rest davor, und der Pinsel ganz; dazu lag er bei lokal x 13 bis 16 auf der ABGEWANDTEN Seite der
Leinwand, wohin keine Hand reicht, und stand zuletzt in der Zellenliste, verlor also jede Zelle
an den Rahmen. Vierundzwanzig Sekunden Malen ohne eine einzige bewegte Zelle. **Beim Basketball
dieselben drei Fehler wie beim Fussball:** Der Korb stand vor dem Ball (`distinctBy` behaelt den
ersten Eintrag), der Ball war fuenf breit und sieben hoch, und der Wurf hatte keinen Flug - der
Ball hing dreieinhalb Sekunden in der Luft und stand dann sechs Sekunden unter dem Korb. Jetzt
fliegt er in einem Bogen und faellt nach dem Treffer heraus; wie beim Fussball zaehlt
`basketballCells` dafuer die Takte seit Beginn der Phase. **Training** kannte nur `pulse` 0/1 im
Sekundentakt - jetzt eine Wiederholung mit Mitte, und in der Ruhephase wandert wenigstens der
Glanz auf der Flasche. **Musik/TUNE** war ausdruecklich vom Notenversatz ausgenommen; **Angeln**
warf einen Schwimmer aus, der genau auf der Rutenspitze sass (fuenf helle Zellen), und der Fang
zappelte nicht. Ein Test haelt jetzt fuer JEDE Phase JEDER Szene fest, dass in acht Sekunden
mindestens drei verschiedene Bilder entstehen - gemessen werden Muster, nicht Stellungen, weil
ein wandernder Glanzpunkt keine Zelle bewegt und trotzdem Bewegung ist. Nebenbei mass
`Basketball prellt und landet sichtbar im Korb` in Wahrheit das KORBBRETT: `minOf { it.y }` ueber
die ganze Szene ist immer dessen Oberkante, und die steht in jeder Phase gleich hoch. **Offen
bleibt:** Angeln ist mit acht bis zehn hellen Zellen ueber zweiundzwanzig Sekunden Wartezeit die
duennste Szene - strukturell richtig (Rute, Schnur, Schwimmer, Wellen), aber sehr leise. Und die
GETRAGENEN Gegenstaende (Buch, Becher, Gitarre, Staffelei, Essen) kennen die Zeit gar nicht: Sie
stehen in der Hand still, auch waehrend die Figur laeuft.

**Darstellung:** NT-083 **Jeder Schlaf erzaehlt den wirklichen Tag in der Watch** - *am
2026-09-13 umgesetzt.* Der bisherige Nachttraum war eine 40-Prozent-Gelegenheit und konnte eine
ganze Schlafsequenz auslassen. Jetzt oeffnen sich bei jedem `SleepUntilMorning` kleine
Traumblasen; ihr erstes Bild geht in die Watch ueber, die sich wie bei der Mondsequenz vergroessert
und nach oben zieht. Darin laufen deterministisch hoechstens drei der juengsten unterschiedlichen
wirklich erlebten Themen mit der vorhandenen Charakteranimation. Bei einem leeren ersten Tag
laeuft die Sequenz mit der echten Schlafpose, ohne ein Highlight zu erfinden. Nebenbefund der
Messung: Seit NT-073 geplante Tagtraeume auf Sofa und Bank waren wegen einer Bettbedingung in der
Darstellung unsichtbar; die Projektion folgt jetzt dem traeumenden Wesen statt dem Moebel.
**Offen bleibt:** Uebergang, Groesse und Tempo am Geraet ansehen. Der Tagtraum bleibt bewusst
zufaellig; nur echter Schlaf garantiert den Rueckblick. LOVE, SLEEP und BOOK mit je zwei
Weltmotiv-Stellungen sowie die ruhige Angel-Szene sind keine Fehlerkorrekturen. Der damals
naechste belegte Animationshebel - getragene Gegenstaende ohne Gangtakt - ist in NT-090 behoben.

**Darstellung:** NT-084 **Jeder Reminder hat eine motiveigene Avatarreaktion** - *am
2026-09-13 umgesetzt.* Die Vollpruefung ergab: Alle Reminder spielten bereits Avatarframes, aber
29 allgemeine Bibliotheksmotive erbten noch eine bildgleiche Gruppenreaktion. Sie haben jetzt
jeweils eine eigene kleine Choreografie aus Motiv-Requisite, Koerperbahn, Mimik und Timing. Der
Katalogtest verlangt vollstaendige Abdeckung aller 69 Bibliotheksmotive: 30 Charaktermotive,
38 allgemeine Motive und die vorhandene Rocket-Sonderfolge. Der Bildvergleich misst nun fuer 80
Motivknoten 80 verschiedene Reaktionen; ein Geschwister-Rueckfall ist damit ein Testfehler statt
eine fortzuschreibende Bestandszahl. Kontaktboegen fuer PUFFLING und GLOOP wurden geprueft.
**Offen bleibt:** Tempo und Lesbarkeit im laufenden Spiel am Geraet ansehen. Der damals naechste
gemessene Animationshebel aus NT-082 ist mit NT-090 umgesetzt.

**Darstellung:** NT-090 **Getragene Dinge folgen dem Gang** - *am 2026-09-15 umgesetzt.* Buch,
Becher, Essen, Gitarre und Staffelei folgten zwar der Position der Figur, blieben relativ zum
Koerper aber starr und glitten dadurch sichtbar mit. Sie erhalten jetzt beim Gehen einen ruhigen
Sechsertakt zwischen Grundstellung und zwei angehobenen Hoehen. Im Stand bleibt die Hand ruhig;
Bildschirm und Clip nutzen dieselbe Phase und dieselbe Bewegungsbedingung. Ein Verhaltenstest
prueft alle fuenf Gegenstaende auf mindestens drei Gangbilder, genau ein Standbild und darauf,
dass die Freistellung nie unter die ruhige Grundstellung und damit in den Boden wandert.
**Offen bleibt:** Den Hub am Geraet auf Natuerlichkeit beurteilen.

**Darstellung:** NT-077 **Die Haeuser in der Ferne bekommen Masse** - *am 2026-09-12 umgesetzt.*
Sie waren Umrisse: Dach, Waende, Laibungen, Tuer - sorgfaeltig gezeichnet und trotzdem
durchsichtig. Man sah durch sie hindurch auf den schwarzen Grund, vier davon nebeneinander waren
ein Drahtgitter statt einer Stadt. Jetzt sind es gefuellte Flaechen; Fenster und Geschosse
rechnet `facadeCell` aus Breite und Hoehe heraus. Gemessen an der Stadt: Die Flaeche, die
deutlich sichtbar ist, faellt von 45 auf 21 Prozent - eine gefuellte Fassade ist also nicht
lauter, sondern ruhiger als ein Umriss, weil die Wand dunkel wird und nur die Fenster den
Kontrast tragen. Zwei Funde nebenbei: Die Sterne standen VOR den Haeusern (bei einem Umriss
unsichtbar, bei einer Wand ein blinkendes Loch), und die Fensterlichter kamen aus einer zweiten
Liste, die von Hand zu den gezeichneten Laibungen passen musste - beides zusammengefuehrt.
**Offen bleibt:** Im Vorbild sind die Haeuser klein und weit oben, bei uns reichen sie ueber
zwei Drittel der Bildhoehe; ob die Ferne wirklich fern wirkt, entscheidet die Groesse, nicht die
Fuellung.

**Tokenverbrauch, weitere:** NT-048 `runner/prompts/*.md` gegen `claude-primary-run.yml` auf
Redundanz prüfen · NT-049 Kompakteres Backlog-Format für mechanische Aufgaben erproben · NT-052
Wiederkehrende Datei-Header-Boilerplate prüfen.

**Hinweis zu den zurückgestellten Tests (NT-015 bis NT-028):** Diese Aufgaben sind nicht
unwichtig - sie betreffen Korrektheit und Datensicherheit (siehe Vision.md: kein Cloud-Backup),
nicht Geschwindigkeit/Effizienz. Sie folgen der Top-15-Liste, sobald deren wichtigste Punkte
erledigt sind, statt hier um denselben Hebel-Maßstab zu konkurrieren, der für sie nicht das
richtige Kriterium ist.

## Pflege dieser Liste

IDs bleiben stabile Referenzen, auch nach dem Verschieben zwischen Top 15 und Future Backlog.
Wird eine Top-15-Aufgabe erledigt, rückt die höchste noch offene Future-Backlog-Aufgabe nach dem
Hebel-Maßstab oben in die Top 15 nach - die Liste bleibt dadurch dauerhaft auf maximal 15
Einträge begrenzt. Neue Aufgaben, die während der Arbeit entdeckt werden, kommen mit
fortlaufender neuer ID (ab NT-058) direkt in den Future Backlog, nicht ungeprüft in die Top 15.
