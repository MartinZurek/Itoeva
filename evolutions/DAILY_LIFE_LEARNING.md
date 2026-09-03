# Lern- und Feedback-Overlay fuer den Tagesablauf

Diese Datei verbindet Nutzerfeedback mit den spaeteren, automatisierten Tagesablauf-Evolutionen.
Der Dauerauftrag steht getrennt in `DAILY_LIFE_TASK.md`; dadurch kann dieses Overlay lernen, ohne
seine eigenen Sicherheitsgrenzen umzuschreiben. Jede Aenderung bleibt ein normaler Pull Request
mit Tests und zweiter KI-Pruefung.

**Wer mergen darf (Entscheidung vom 2026-09-03, siehe EVOLUTION.md, Evolution History).** Der
Merge muss nicht mehr von Hand erfolgen: Eine vom Produktverantwortlichen beauftragte
Agentensitzung darf mergen, sofern die CI vollstaendig gruen ist, kein Merge-Konflikt besteht und
keine offene Review-Anmerkung unbeantwortet ist. Der UNBEAUFSICHTIGTE Lauf selbst mergt weiterhin
NIE - `claude-primary-run.yml` und `runner/` haben kein Merge-Recht, und die Trennung von
Schreibrecht und Merge-Entscheidung bleibt damit erhalten. Das Rueckgaengigmachen bleibt in jedem
Fall beim Menschen: Ein Revert wirkt genauso wie vorher.

Ein einmaliger, bereits vollstaendig formulierter Auftrag kann weiterhin direkt ueber das Feld
`task` von `workflow_dispatch` gestartet werden. Rueckmeldungen, die mehrere spaetere Laeufe
beeinflussen sollen, gehoeren dagegen in den Feedback-Eingang unten. Eine KI in einem Chat kann
sie dort in einem normalen PR eintragen; der GitHub-Lauf liest keine privaten Chats automatisch.

## Aktueller Produktfokus

Der Spielmodus soll Freude am Zuschauen erzeugen: Die Avatare leben erkennbar in ihrer Welt,
gestalten einen plausiblen Tagesablauf und unterscheiden sich dabei. Persoenliche Reminder duerfen
dieses Leben sichtbar und sanft beeinflussen, ohne zu Aufgaben, Strafe oder einer zweiten
Reminder-Logik zu werden.

## Feedback-Eingang

Neue Rueckmeldungen werden oben einsortiert. Format:

    ### [open] FB-JJJJ-MM-TT-NN - Kurztitel
    Beobachtung, gewuenschte Spielerwirkung und - falls vorhanden - reproduzierbare Situation.

Nur eine Evolution, die den Eintrag vollstaendig behandelt, darf `[open]` auf `[done]` setzen.
Unklare oder geschuetzte Wuensche bleiben offen und werden im Journal als `OPEN DECISION`
bezeichnet.

### [open] FB-2026-09-03-01 - Das Avatarleben soll auch ohne Kommentar eine Geschichte tragen

Die Welt soll spaeter in einer stillen, oeffentlichen Twitch-Instanz dauerhaft interessant zu
beobachten sein und belegte Ereignisse fuer ein von den Avataren erzaehltes
YouTube-Evolutionstagebuch liefern koennen. Bevor irgendeine Plattform-, Netzwerk- oder
Videointegration gebaut wird, sollen deshalb schon die lokalen Tagesablaeufe lesbare Absichten,
charaktergerechte Entscheidungen, nachvollziehbare Uebergaenge und erinnerungswuerdige kleine
Folgen zeigen. Eine Evolution darf diese Beobachtbarkeit auf App-Ebene verbessern, aber daraus
keine Freigabe fuer Twitch-, YouTube-, Konto-, Zahlungs- oder Cloud-Funktionen ableiten.

### [open] FB-2026-09-02-01 - Das Avatarleben muss den Kern des Spielerlebnisses tragen

Die automatisierten Jobs sollen sich nicht in isolierten Requisiten, Einzelanimationen oder
anderen leicht zaehlbaren Kleinigkeiten verlieren. Beim Zuschauen soll erkennbar sein, wie ein
Avatar seinen Tag gestaltet, wie sich die sechs Wesen unterscheiden und wie persoenliche
Reminder den weiteren Ablauf sanft beeinflussen. Kleine Codeaenderungen sind ausdruecklich
willkommen, wenn ihre Wirkung in diesem Erlebnis gross und nachvollziehbar ist.

## Gelernte Heuristiken

Dieser Abschnitt darf nur um eine kurze Regel erweitert werden, wenn ein Test, ein reproduzierbarer
Fehler oder mindestens zwei unabhaengige Journaleintraege sie belegen. Eine Heuristik veraendert
keine Produktentscheidung und keine Sicherheitsgrenze.

- Stehen in einer Tagesphase drei oder mehr Themen mit nahezu gleichem Gewicht nebeneinander (siehe
  `PlayAmbientActivity.weightsFor`, MIDDAY: WORK/FOCUS/DRINK/MOVE je 3), faellt bei unabhaengiger
  Ziehung ohne Gedaechtnis eine spuerbare sofortige Wiederholung - rechnerisch und testbelegt rund
  jede sechste Runde (Summe der quadrierten Anteile). **Belegt durch Test:**
  `PlayAmbientActivityTest`, `ohne Daempfer wiederholt sich ein Thema spuerbar oft` und
  `der Daempfer macht eine sofortige Wiederholung seltener` (2026-09-03).

## Lernjournal

Pro Evolution genau ein neuer Eintrag direkt unter dieser Einleitung. Ein Eintrag nennt Datum,
betroffenes Feedback, beobachtetes Problem, Spielerwirkung, Evidenz, Ergebnis und den naechsten
sinnvollen Hebel. Vermutungen werden als **Hypothese**, nicht als Tatsache, markiert. Das Journal
ist eine Uebergabe, keine zweite Commit-Liste.

### 2026-09-03 - Der im Journal notierte Wiederholungs-Daempfer wurde belegt und umgesetzt

- **Betroffenes Feedback:** FB-2026-09-03-01 und FB-2026-09-02-01, beide **weiterhin `[open]`**.
  Diese Evolution ist ein weiterer Hebel darauf, erledigt aber keinen der beiden Eintraege
  vollstaendig - beide bleiben umfassende, mehrere Evolutionen uebergreifende Ziele.
- **Beobachtetes Problem:** Der vorherige Journaleintrag vom selben Tag vermerkte als naechsten
  Hebel, ausdruecklich unbelegt: "ein gerade gespieltes Thema fuer wenige Runden geringer
  gewichten", weil unklar war, ob eine sofortige Wiederholung nach dem Stundenplan-Signal
  ueberhaupt noch stoert.
- **Evidenz:** `TESTED BEHAVIOR` statt laenger nur Hypothese. `PlayAmbientActivity.weightsFor`
  gibt fuer MIDDAY vier Themen (WORK/FOCUS/DRINK/MOVE) je Gewicht 3 - bei unabhaengiger Ziehung
  ohne Gedaechtnis faellt dieselbe Handlung dort rechnerisch in rund 16,4 % der Runden zweimal
  hintereinander (Summe der quadrierten Anteile). Ein neuer Test
  (`ohne Daempfer wiederholt sich ein Thema spuerbar oft`) bestaetigt das empirisch mit 5000
  Ziehungen, bevor die Aenderung ueberhaupt einsetzt - die Blockade ist damit belegt, nicht nur
  vermutet.
- **Ergebnis:** `nextTopic`/`combinedWeights` erhalten `justPlayed` als fuenftes Signal.
  `REPEAT_MALUS = 2` senkt das Gewicht des zuletzt gespielten Themas fuer die naechste Ziehung, mit
  Bodenwert 1 (nie ausgeschlossen) und ohne Wirkung, wenn das Thema die einzige Moeglichkeit im
  Pool ist (Nachtruhe bleibt unangetastet). `DockScreen` reicht dafuer sein ohnehin vorhandenes
  `currentTopic` durch, kein neuer Zustand noetig. Ein zweiter Test belegt die Verringerung auf
  rund 7 % derselben 5000 Ziehungen, zwei weitere schuetzen Nachtruhe- und Phasen-Garantie.
- **Spielerwirkung:** Mittags folgt seltener dieselbe Handlung zweimal direkt hintereinander (z. B.
  zweimal DRINK), waehrend eine Wiederholung weiterhin moeglich bleibt - kein hartes Verbot, nur
  eine seltenere Ausnahme. Verbindet die bestehende Aktivitaetsauswahl mit dem, was die Figur
  gerade erst getan hat (`currentTopic`), ohne Ort, Neigung, Stundenplan oder offene Gewohnheiten
  zu veraendern.
- **Naechster sinnvoller Hebel (Hypothese):** Der Daempfer wirkt nur auf die EXAKTE Wiederholung
  desselben `AnimationType`. Unklar und **nicht belegt** ist, ob auch eine Abfolge NAHE verwandter
  Themen (z. B. FOCUS direkt nach WORK, beides am selben Ort) beim Zuschauen aehnlich repetitiv
  wirkt - das braucht zuerst eine Beobachtung, bevor daraus eine Regel wird.
- **Anmerkung zur Herkunft:** Diese Evolution entstand im automatisierten, unbeaufsichtigten
  Dauerauftrag (`claude-primary-run.yml` / `evolutions/DAILY_LIFE_TASK.md`), ausgeloest durch den
  im vorherigen Journaleintrag notierten, damals unbelegten Hebel.

### 2026-09-03 - Der vorhandene Stundenplan war im laufenden Tag wirkungslos

- **Betroffenes Feedback:** FB-2026-09-03-01 und FB-2026-09-02-01, beide **weiterhin `[open]`**.
  Diese Evolution ist ein Hebel darauf, erledigt aber keinen der beiden Eintraege vollstaendig.
- **Beobachtetes Problem:** `PlayPresence.topicFor` haelt einen exakten 24-Stunden-Plan bereit,
  wurde aber nur beim Betreten des Spielmodus gelesen. Die Regungs-Schleife arbeitete allein mit
  den vier `DayPhase`-Bloecken. Von 11 bis 17 Uhr ist alles gleich "Mittag", also konnten Essen,
  Arbeit und Fokus in beliebiger Reihenfolge beliebig oft kommen - und da jede `PERFORM`-Regung
  den Ort wechselt, sah das aus wie Pendeln statt wie ein Tag.
- **Evidenz:** `FACT` - die Aufrufstellen der Tabelle waren vor dieser Aenderung `PlayPresence.entry`
  und der Play-Modus-Einstieg in `DockScreen`, sonst keine. Phasengrenzen 6-10/11-17/18-22/Nacht
  sind in `currentDayPhase` belegt und durch einen bestehenden Test fixiert.
- **Ergebnis:** Der Plan ist jetzt das vierte additive Signal der Themenwahl (`PLAN_BONUS = 4`),
  gleich stark wie das Gewohnheits-Signal und schwaecher als der Verweil-Bonus. Er darf ein Thema
  auch einfuehren, was genau einmal am Tag greift: den Arbeitsbeginn um zehn, den die Morgenphase
  bisher nicht kannte. Charakterneigung, offene Gewohnheiten und Weltzustand gewichten unveraendert
  dagegen, damit sich die sechs Wesen weiterhin unterscheiden.
- **Spielerwirkung:** Um sieben wird eher gefruehstueckt, um zehn eher gearbeitet, um acht abends
  eher gelesen - erkennbar, aber ohne Gewissheit (im Beispiel Fruehstueck: 21% auf 39%). Die
  Ueberraschung bleibt bei den Sonderaktivitaeten und den Weltzustands-Ueberstimmungen.
- **Naechster sinnvoller Hebel (Hypothese):** Ein kurzer Wiederholungs-Daempfer - ein gerade
  gespieltes Thema fuer wenige Runden geringer gewichten. Der Verdacht ist, dass danach noch
  auffaellt, wenn dasselbe Thema zweimal kurz hintereinander faellt. **Nicht belegt:** Es fehlt die
  Beobachtung am Geraet, ob das nach dieser Aenderung ueberhaupt noch stoert.
- **Anmerkung zur Herkunft:** Diese Evolution entstand in einer Cloud-Code-Sitzung auf
  ausdruecklichen Nutzerauftrag, nicht im automatisierten Lauf. Sie folgt denselben Leitplanken.


## Offene Meta-Ideen

Hier darf ein Lauf Verbesserungen an Auswahl, Messbarkeit oder Automationsqualitaet vorschlagen,
aber nicht selbst in `.github/`, `runner/`, `DAILY_LIFE_TASK.md`, Berechtigungen, Secrets,
Publikations- oder Review-Gates umsetzen. Eine Meta-Idee braucht Problembeleg, erwarteten Nutzen,
Risiko und eine pruefbare Erfolgsmessung; Umsetzung nur in einem getrennten, ausdruecklich
menschlich freigegebenen Prozess-PR.

Noch keine offene Meta-Idee.
