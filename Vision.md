# Vision.md

Langfristige Vision, Design-Philosophie und Kernprinzipien von Itoeva. Dieses Dokument beschreibt
das *Warum* und das *Was darf nie verloren gehen* - nicht das *Wie*. Technische Details gehören in
[Architecture.md](Architecture.md), einzelne Entscheidungen mit Datum und Begründung in
[EVOLUTION.md](EVOLUTION.md), konkrete Arbeitspakete in [NextTasks.md](NextTasks.md).

Wo dieses Dokument etwas als Prinzip festschreibt, ist es durch heutigen Code oder Produkttext
belegt - siehe die Verweise. Wo etwas noch offen ist, steht ausdrücklich **OPEN DECISION** statt
einer stillschweigenden Annahme, im selben Sinn wie in EVOLUTION.md.

**Wann diese Datei lesen:** laut AgentGuide.md nur bei Aufgaben, die Produkterlebnis, UX,
Charakter-Ton oder Spielmechanik berühren - nicht bei rein technischen Aufgaben. Am häufigsten
gebraucht: der Abschnitt "Die Erfahrung, die nicht verhandelbar ist" - der reicht für die meisten
Fälle, in denen nur geprüft werden muss, ob eine geplante Änderung ein Prinzip verletzt.

## Was Itoeva ist

Itoeva ist eine lokale, Android-basierte Erinnerungsanwendung mit einem kleinen begleitenden
Wesen. Sie hat keinen Server, kein Konto und keine Cloud-Synchronisierung - alles, was ein Nutzer
aufbaut, lebt ausschließlich auf seinem Gerät. Die Kerninteraktion ist einfach genug, um in einem
Satz zu passen: Eine Erinnerung erscheint sichtbar, der Nutzer zieht sie auf das Wesen, das Wesen
reagiert. Alles andere - Beziehungskapitel, Stimmung, Spielfortschritt, Lore - entsteht als Folge
dieser einen Handlung, wiederholt über Zeit.

Es gibt zwei Produktfassungen mit gemeinsamem Kern (`:core`, siehe Architecture.md):

- Eine Hardware-Fassung für Nothing-Geräte mit physischer Glyph-Matrix (`:app`).
- Eine Simulator-Fassung für gewöhnliche Android-Geräte (`:app-sim`), die dieselbe Matrix als
  Rundbild auf dem Bildschirm nachbildet und zusätzlich einen "Spiel"-Modus mit eigener
  Pixelwelt, Avatar-Entwicklung und Beziehungen zwischen sechs Wesen enthält.

**OPEN DECISION** (aus EVOLUTION.md übernommen, hier nicht neu entschieden): ob beide Fassungen
dauerhaft nebeneinander bestehen bleiben oder eine davon die primäre Richtung ist, und wie der
endgültige Produktname lautet (Itoeva, Glyphminder, Tama sind aktuell alle im Repository in
Gebrauch).

## Die Erfahrung, die nicht verhandelbar ist

Die folgenden Eigenschaften sind keine Feature-Liste, sondern das, was Itoeva *anders* macht als
gewöhnliche Erinnerungs- oder Tamagotchi-artige Apps. Jede davon ist heute im Code oder Produkttext
nachweisbar (Details und Belege in EVOLUTION.md, Abschnitt "Non-Negotiable Design Principles").
Eine Änderung an einer dieser Eigenschaften ist keine gewöhnliche Weiterentwicklung, sondern eine
bewusste Produktentscheidung mit menschlicher Freigabe.

**Sanft statt strafend.** Itoeva bestraft nicht. Keine Streaks, die reißen. Keine Beziehung, die
durch Abwesenheit schrumpft. Keine Sprache, die Schuldgefühle erzeugt ("du hast mich vergessen").
Ein Wesen, das lange nicht besucht wurde, ist träge oder ruhig - nicht traurig, nicht hungrig im
strafenden Sinn, nicht wütend. Das ist die zentrale Differenzierung gegenüber dem klassischen
Tamagotchi-Genre, das über Verlustangst funktioniert. Itoeva funktioniert bewusst nicht so.

**Nicht gesehen heißt nicht verpasst.** Eine Erinnerung, die bei ausgeschaltetem Display niemand
sehen konnte, darf nicht als versäumt gelten. Die App überwacht sich selbst nicht gegen den
Nutzer - sie zeigt nur, wenn ohnehin schon hingeschaut wird.

**Eigene Routinen und gemeinsames Erleben bleiben getrennte Besitzverhältnisse.** Erinnerungen
(Bezeichnung, Zeitfenster, Intervall) gehören dem Nutzer und sind jederzeit frei änderbar.
Pflegebuch, Beziehungskapitel und Spielstand gehören dem gewählten Wesen und wachsen nur additiv.
Diese Trennung ist der Grund, warum sich Itoeva anfühlt wie eine Beziehung und nicht wie eine
To-do-Liste mit Maskottchen.

**Auslösehäufigkeit und Nutzerziel sind zwei verschiedene Zahlen.** Wie oft eine Erinnerung
sichtbar wird, ist unabhängig davon, wie oft der Nutzer die Handlung tatsächlich ausführen will.
Wer keine Tagesziel-Zahl gesetzt hat, bekommt neutrale Hinweise, die die Stimmung nicht
verschlechtern können.

**Dieselbe Pipeline für Alltag und Spiel.** Der Spielanteil ist kein separates Minispiel mit
eigener Erinnerungslogik. Er nutzt exakt denselben Auslöse-, Zieh- und Fütter-Mechanismus wie
persönliche Erinnerungen (siehe `feedOccurrence` in `HomeScreen.kt`) und erweitert ihn nur um
zusätzliche Inhalte. Diese Wiederverwendung ist kein Implementierungsdetail, sondern Teil des
Gefühls: Alltag und Spiel fühlen sich wie ein einziges, kohärentes Ritual an, nicht wie zwei Apps
in einer.

**Lokal, ohne Konto, ohne Cloud.** Alles, was ein Nutzer über Wochen aufbaut - Pflegebuch,
Beziehungen, Fortschritt - existiert ausschließlich auf seinem Gerät. Das bedeutet zugleich: Ein
Datenverlust durch einen fehlerhaften Room-Migrationsschritt ist nicht wiederherstellbar. Diese
Tatsache prägt die technischen Prioritäten in Architecture.md und NextTasks.md direkt (Migrations-
Testabdeckung hat strukturell höheres Gewicht als bei einer Cloud-Anwendung).

## Design-Philosophie

**Silhouette vor Detail.** Die Wesen leben auf einer 16x16-Zell-Matrix. Design-Entscheidungen
orientieren sich konsequent an dem, was Game & Watch und Tamagotchi aus demselben Grund gewählt
haben: bei so wenig Auflösung bleibt nur eine klare Seitenansicht auf niedriger Zellzahl lesbar,
Schrägen und Perspektive werden zu nicht unterscheidbaren Treppenstufen (siehe Kommentar in
`PlayScene.kt`). Diese Beschränkung ist keine technische Notlösung, sondern eine bewusst
gewählte, stilprägende Grenze.

**Prozedural statt gemalt, wo es die Größe verlangt.** Die Kulisse im Spielmodus ist
prozedural aufgebaut statt aus vorgefertigten Hintergrundbildern zusammengesetzt, weil sie über
den gesamten Bildschirm reicht (~50x100 Zellen) und als volles Bild zu über 95 % leer wäre.

**Bestehende Mechanik wiederverwenden statt neue erfinden.** Jede neue Interaktion (siehe z. B.
PR #20, die Speicherplätze für Aktionen) baut nachweislich auf der bestehenden Zieh-/
Kollisionslogik und den bestehenden Reaktionsanimationen auf, statt eigene Parallel-Implementierungen
zu schaffen. Das ist nicht nur Code-Hygiene - es hält die Interaktionssprache der App konsistent:
Ziehen bedeutet in Itoeva immer dasselbe.

**Animations-/Habit-Slots schaffen strategische Auswahl.** Eine begrenzte Zahl sichtbarer Slots
soll den Nutzer planen lassen: Gespeicherte Animationen oder Habits bleiben präsent und damit
mental verfügbar; die Kernentscheidung lautet "jetzt einsetzen oder für später aufheben?".
Kontextboni, Kombinationen und zeitabhängige Situationen dürfen später darauf aufbauen. Konkrete
Slot-Anzahl und Umsetzung bleiben **OPEN DECISION** und werden vor Gameplay-Code erst spezifiziert
und validiert (siehe NT-053 in NextTasks.md).

**Deutsch als Erstsprache des Codes.** Kommentare, Commit-Nachrichten, interne Dokumentation und
das primäre `values-de/strings.xml` sind Deutsch; `values/strings.xml` (Englisch) wird
mitgepflegt, ist aber die Zweitsprache. Agenten, die an diesem Repository arbeiten, sollten sich
danach richten (siehe AgentGuide.md).

## Was bewusst noch nicht entschieden ist

Diese Punkte sind laut EVOLUTION.md **OPEN DECISION** und dürfen nicht durch Annahmen, "Best
Practices" oder automatisierte Weiterentwicklung stillschweigend festgelegt werden:

- Endgültiger Produktname.
- Ob `:app` (Hardware) und `:app-sim` (Simulator/Spiel) dauerhaft zwei Produkte bleiben.
- Größere Spielstruktur: finales Spielziel, Quest-Struktur, ein Skillbaum-/Talentbaum-System.
- Langfristige Wirtschaft/Balancing jenseits der heutigen additiven Fortschrittspfade.

Erlaubt ohne vorherige Rückfrage zur kreativen Richtung ist dagegen, seit EVOLUTION.md v0.2,
das Ergänzen weiterer Beziehungen und Lore-Stücke innerhalb des bestehenden Rahmens der sechs
Wesen ("Erzählerische Autonomie") - das bleibt in EVOLUTION.md die maßgebliche, laufend
gepflegte Quelle für den genauen Umfang dieser Freigabe.

## Warum diese Priorität gerade jetzt: Prozess vor neuen Features

Itoeva wird seit dem 17.08. von einer automatisierten Pipeline weiterentwickelt
(`claude-primary-run.yml`, siehe Architecture.md). Zwei Tage dieser kurzen Geschichte gingen bereits
durch ein erschöpftes CI-Kontingent verloren, mehrere große Dateien sind über 1500-3600 Zeilen
gewachsen, und Kernlogik existiert zwischen `:app` und `:app-sim` teils dupliziert statt geteilt
(Details in Architecture.md). Eine Vision, die nur neue Spielinhalte beschreibt, ohne die
Tragfähigkeit des Prozesses zu sichern, der sie bauen soll, wäre unvollständig. Deshalb gilt bis
auf Weiteres: **keine neuen, größeren Gameplay-Features, bevor Build-Prozess, Testabdeckung,
Agentenfreundlichkeit und Tokenverbrauch der Entwicklung selbst spürbar besser sind** - siehe
NextTasks.md für die konkrete, priorisierte Umsetzung dieser Regel.
