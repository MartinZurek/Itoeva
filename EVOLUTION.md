# Itoeva Evolution Protocol

Version: 0.5 - seit 2026-09-03 darf der Merge eines fertigen Pull Requests von einer beauftragten
Agentensitzung ausgeführt werden; der unbeaufsichtigte Lauf mergt sein eigenes Ergebnis weiterhin
nie (siehe Evolution History zum 2026-09-03, "Merge-Freigabe"). Version 0.4 - seit 2026-09-03 mit
der strategischen Zielidentität einer öffentlich beobachtbaren
Evolutionswelt. Die lokale persönliche App, eine davon getrennte stille Twitch-Welt und ein von
den Avataren erzähltes YouTube-Evolutionstagebuch sollen verschiedene Perspektiven auf dasselbe
gestalterische Experiment eröffnen. Zuschauer dürfen die öffentliche Welt künftig nur durch
begrenzte Impulse beeinflussen, nicht private oder medizinische Reminder steuern. Technische
Streaming-, Netzwerk-, Konto-, Zahlungs- und Medienarchitektur bleiben bis zu gesonderten
Entscheidungen offen. Der kontrollierte Tagesablauf-Dauerauftrag aus Version 0.3 (2026-09-02), die
enge erzählerische Autonomie aus Version 0.2 (2026-08-18) und alle Sicherheitsgrenzen gelten fort.
Von den Merge-Grenzen ist genau eine gelockert - wer mergen darf; alle übrigen (Branch, Tests,
zweite Prüfung, Pull Request, kein Merge-Recht für die Pipeline, kein direkter Push auf `main`)
bleiben unverändert.

Dieses Dokument legt fest, wie Itoeva weiterentwickelt werden darf, ohne die heute im Repository
erkennbare Identität, bereits getroffene Produktentscheidungen oder nachweisbares Verhalten
stillschweigend zu verändern. Es beschreibt Leitplanken, keinen automatisch auszuführenden
Produktplan.

Wo das Repository keine eindeutige Entscheidung belegt, steht ausdrücklich **OPEN DECISION**.
Ein solcher Punkt darf nicht durch Annahmen, vermeintliche Best Practices oder automatische
Änderungen entschieden werden.

## Core Identity – was Itoeva heute ist

Itoeva ist heute eine lokale Android-Erinnerungsanwendung mit Virtual-Pet- und
Ambient-Life-Elementen. Sie verbindet persönliche Routinen mit einem kleinen Wesen, das visuelle
Erinnerungen begleitet und auf beantwortete Erinnerungen reagiert.

Die Kerninteraktion ist:

1. Eine Erinnerung wird zu einem vorgesehenen Zeitpunkt visuell sichtbar.
2. Der Nutzer reagiert, indem er die Uhr auf das Wesen zieht oder die barrierefreie
   Fütter-Aktion verwendet.
3. Die Auslösung wird als beantwortet markiert.
4. Das Wesen zeigt eine zum Thema und teilweise zur Spezies passende Reaktion.
5. Pflegebuch, Stimmung, Beziehung und – bei Spiel-Ereignissen – XP können daraus abgeleitet
   werden.

Eigene Erinnerungen bestehen aus Bezeichnung, Animation, Wochentagen, Zeitfenster, Intervall,
Aktivstatus und optionalem Tagesziel. Sie gehören dem Nutzer. Pflegebuch, gemeinsame Geschichte
und Spielstand gehören dagegen dem jeweils gewählten Wesen.

Die Simulator-App kennt drei sichtbare Modi:

- **Nur Uhr:** Uhr ohne Wesen und ohne ausgelöste Erinnerungen.
- **Erinnerungen:** persönliche Routinen; das Wesen erscheint bei einer fälligen Erinnerung.
- **Spiel:** das Wesen lebt dauerhaft in seiner Pixelwelt. Persönliche Routinen laufen weiter;
  zusätzlich wird eine charakterabhängige Spiel-Erinnerung aktiv.

Der Spielanteil ist kein getrenntes Minispiel. Er verwendet dieselbe Reminder- und
Fütter-Pipeline wie die Alltagsfunktion und erweitert sie um Ambient-Routinen, Charakterpläne,
XP, Entwicklungspfade, Beziehungskapitel und persönliche Geschichten.

**OPEN DECISION:** Der endgültige Produktname ist nicht eindeutig. Im Repository werden unter
anderem Itoeva, Glyphminder und Tama verwendet; der Nutzer bezeichnet die übergreifende Vision
aktuell zusätzlich als „Toeva“.

**OPEN DECISION:** Es ist nicht abschließend dokumentiert, ob die Nothing-Hardware-App und die
Simulator-/Tama-App dauerhaft zwei Produkte bleiben oder welche davon die primäre Produktfassung
ist.

### Strategische Zielidentität: öffentlich beobachtbare Evolutionswelt (entschieden 2026-09-03)

Der Produktverantwortliche hat die übergreifende Richtung festgelegt: Itoeva soll nicht nur eine
App mit automatisiert erzeugten Inhalten sein, sondern am erlebbaren Produkt selbst zeigen, was
automatisierte Gestaltung von Geschichten und Pixel-Avatar-Leben leisten kann und wo ihre Grenzen
liegen. Angenommene Veränderungen, Rücknahmen und belegte Fehlschläge dürfen Teil der Erzählung
werden; eine erfundene Erfolgsgeschichte ist nicht erlaubt.

Die Zielidentität umfasst drei getrennte Perspektiven:

1. Die **persönliche App** bleibt die lokale, private Beziehung zwischen Nutzer, Remindern und
   eigenem Avatarleben.
2. Eine **öffentliche Twitch-Welt** soll als eigene Instanz dauerhaft und ohne Sprecher das
   laufende Leben der Avatare zeigen. Zuschauer dürfen über klar begrenzte Berechtigungen aus
   Abos, Donations oder vergleichbaren Mechanismen zulässige Weltimpulse geben.
3. Ein **YouTube-Evolutionstagebuch** soll möglichst von den Avataren selbst als Protagonisten
   erzählen lassen, was sich täglich tatsächlich verändert hat, was sie erlebt haben und welche
   Grenze der Evolution sichtbar wurde. Kürzere Highlight-Formate dürfen auf denselben belegten
   Ereignissen aufbauen.

Entschieden ist die gestalterische Richtung, nicht ihre technische Ausführung. Insbesondere sind
Backend, Streaming, Identität, Zahlung, Moderation, Kontingente, Cooldowns, Video- und
Stimmerzeugung, Veröffentlichungsrhythmus sowie Rechte- und Plattformfragen weiterhin
`OPEN DECISION` und kein Auftrag an einen autonomen Lauf.

## Non-Negotiable Design Principles – welche Eigenschaften bei jeder Evolution erhalten bleiben müssen

Diese Prinzipien sind durch aktuellen Code, Dokumentation und Produktoberfläche belegt. Eine
Änderung daran ist keine gewöhnliche Evolution, sondern eine bewusste Produktentscheidung und
bedarf ausdrücklicher Freigabe.

### Sanft statt strafend

- Keine Streaks, Rückstufungen oder Verluste allein wegen Abwesenheit oder ausgelassener Tage.
- Beziehungskapitel dürfen nur wachsen und nicht durch Leistung oder Versäumnisse zurückfallen.
- Texte und Animationen dürfen keine Schuld erzeugen. Die vorhandene Sprache beschreibt niedrige
  Energie oder Trägheit statt Vorwurf, Hunger oder Traurigkeit.

### Nicht gesehen heißt nicht verpasst

- Eine Erinnerung, die bei ausgeschaltetem Bildschirm nicht wahrgenommen werden konnte, darf in
  der Simulator-App nicht als verpasst protokolliert werden.
- Der Nur-Uhr-Modus muss Erinnerungen vollständig ruhen lassen, weil dort keine Möglichkeit zur
  Antwort besteht.

### Tagesziel und Anstupsen bleiben getrennt

- Das Auslöseintervall beschreibt, wie oft eine Erinnerung sichtbar wird.
- Das Tagesziel beschreibt, wie oft der Nutzer die Handlung tatsächlich ausführen möchte.
- Erinnerungen ohne Tagesziel bleiben neutrale Hinweise und dürfen die Stimmung nicht
  verschlechtern.
- Spiel-Erinnerungen erhalten kein Tagesziel und dürfen die Stimmung nicht negativ beeinflussen.

### Routinen gehören dem Nutzer, Erlebnisse dem Wesen

- Ein Avatarwechsel darf den persönlichen Routinen-Satz nicht austauschen.
- Fütterungshistorie, Stimmungserleben, Beziehungskapitel, Lore-Fortschritt und Spielstand bleiben
  je Wesen getrennt, soweit sie heute je Wesen geführt werden.
- Historische Ereignisse dürfen nicht allein deshalb gelöscht werden, weil die zugehörige
  Erinnerung später gelöscht oder verändert wurde.

### Verhalten prägt Entwicklung

- Zufällige Spiel-Ereignisse dürfen nicht als Aussage über die Gewohnheiten des Nutzers gelten.
- Der persönliche Entwicklungspfad wird aus beantworteten echten Erinnerungen abgeleitet.
- Bei zu wenig Daten, fehlender Mehrheit oder Gleichstand darf kein Pfad erfunden werden.

### Charakter vor bloßer Variation

- Figuren unterscheiden sich nicht nur visuell, sondern auch in Stimme, Tempo, Themengewichtung,
  Umgebung, Bewegung und Reaktion.
- Zufall bleibt durch charakterbezogene Spielpläne gerahmt.
- Zufällige Spiel-Ereignisse dürfen niemals wie eine echte Medikamenten-Erinnerung wirken;
  `MEDICINE` bleibt aus den Spielplänen ausgeschlossen.

### Lokal und privat

- Die heute belegte Produktidentität umfasst lokale Speicherung, keine Konten, keine Werbung,
  keine Analyse- oder Absturzübertragung und keine eigene Netzwerkkommunikation.
- Jede geplante Abweichung davon erfordert eine ausdrückliche Produktentscheidung sowie eine
  vorherige Prüfung und Aktualisierung von Store-Texten und Datenschutzerklärung.
- Die öffentliche Streaming-Welt ist eine künftige, getrennte Produktoberfläche. Ihre Vision hebt
  den lokalen und privaten Datenvertrag der persönlichen App nicht auf.

### Öffentlicher Einfluss bleibt begrenzt und getrennt

- Zuschauer dürfen nur die öffentliche, erfundene Welt beeinflussen, niemals persönliche
  App-Reminder, lokale Nutzerhistorien oder private Avatarstände.
- `MEDICINE` und andere medizinisch wirkende Reminder sind für Zuschauerinteraktionen vollständig
  ausgeschlossen.
- Zuschauer geben begrenzte Impulse, etwa auf Zeitpunkt, Aktivitätsgewicht oder Wahlrahmen. Die
  Eigenlogik und Persönlichkeit des Avatars bleibt erhalten; beliebige Fernsteuerung ist nicht
  Teil der Vision.
- Abos, Donations oder häufige Interaktion dürfen keine Schuld-, Verlust- oder Strafmechanik
  erzeugen.
- Ungeprüfter Freitext darf nicht unmittelbar als Dialog, Lore, Code oder ausführbarer Auftrag in
  die Welt übernommen werden.

### Zugängliche Kerninteraktion

- Jede zentrale Drag- oder Gesteninteraktion braucht einen bedienbaren, semantisch passenden
  barrierefreien Ersatz.
- Screenreader-Texte müssen denselben druckfreien Ton wie die sichtbare Oberfläche verwenden.

## Evolution Goals – welche Aspekte verbessert werden dürfen

Innerhalb der geschützten Prinzipien dürfen insbesondere folgende Bereiche weiterentwickelt
werden:

- Verständlichkeit, Auffindbarkeit und Zugänglichkeit der Reminder-, Fütter- und Modusfunktionen.
- Stabilität der Alarmplanung, Auslösung, Kollisionen, Widget-Wiedergabe und
  Prozesswiederherstellung.
- Qualität und Lesbarkeit der Pixelanimationen auf runden und rechteckigen Darstellungen.
- Vielfalt von Ambient-Aktivitäten, Routinen, Szenen, Reaktionen und charaktergerechten
  Dialogen.
- Balancing von Spielintervallen, Themengewichten, XP-Tempo und sichtbarer Entwicklung, sofern
  bestehende Schutzregeln eingehalten werden.
- Aussagekraft des Pflegebuchs und der Gesprächsfunktionen, sofern nur belegte lokale Daten
  verwendet und Unsicherheiten nicht als Tatsachen formuliert werden.
- Performance, Energieverbrauch, Testbarkeit, Fehlerbehandlung und Wartbarkeit.
- Konsolidierung klar duplizierter Logik in gemeinsamen Schichten, wenn app-spezifisches Verhalten
  erhalten bleibt.
- Datenmigrationen und Abwärtskompatibilität, damit bestehende Routinen und Geschichte erhalten
  bleiben.
- Dokumentation widersprüchlicher, veralteter oder noch ungeprüfter Aussagen.
- Wahrheitsgetreue, avatarzentrierte Rückblicke auf angenommene Evolutionen und belegte
  Weltereignisse als Grundlage späterer Video- oder Highlight-Formate.
- Beobachtbarkeit der öffentlichen Welt und klar begrenzte Weltimpulse, sofern sie zunächst ohne
  Netzwerk-, Zahlungs- oder Kontenarchitektur auf der niedrigsten sinnvollen Ebene modelliert und
  getestet werden können.

**OPEN DECISION:** Das Repository definiert kein finales Spielziel, Spielende und keine
Sieg-/Niederlage-Struktur. Eine solche Struktur darf nicht ohne bewusste Entscheidung eingeführt
werden.

**Korrektur (2026-08-18):** Der vorherige Wortlaut dieses Punktes ("Der aktuelle Stand schaltet
solche Inhalte nicht frei") war sachlich falsch. `PlayScene.kt` (`enum class Acquisition`,
`PlayPath`) schaltet bereits heute Wohnungsgegenstände nach Fortschrittspfad frei - vier Pfade
(Aufbrecher, Fürsorglicher, Stiller, Macher) zu je drei Gegenständen, vollständig belegt. Diese
Mechanik ist entschieden, gebaut und getestet (`SceneCompositionTest`, u. a. `jedes erworbene
Stück ist auch tatsächlich zu sehen`); sie ist keine `OPEN DECISION` mehr.

**OPEN DECISION:** Was NICHT entschieden ist: neue Fortschrittspfade oder eine vierte Stufe je
bestehendem Pfad (das ist Balancing/Ökonomie, siehe unten), neue Avatar-Fähigkeiten oder
-Silhouetten, echte Reminder-Funktionen durch Fortschritt, sowie ein eigenständiges
Fertigkeiten-/Skillbaum-System im Sinne von Rollenspiel-Talentbäumen (Nutzeridee vom
2026-08-18, siehe Evolution History) - Letzteres wäre eine neue Spielstruktur mit eigener
Balancing-, UI- und Fortschrittslogik und damit eine Entscheidung mit größerer Tragweite als die
bisher freigegebenen Inhaltsergänzungen. Eine solche Struktur darf nicht ohne bewusste
menschliche Entscheidung eingeführt werden.

**OPEN DECISION:** Eine größere lineare Handlung oder Quest-Struktur ist nicht belegt. Die
vorhandene Story ist episodisches Worldbuilding.

### Kontrollierte Tagesablauf-Evolution (entschieden 2026-09-02)

Der Nutzer hat ausdrücklich freigegeben, dass die zeitgesteuerten GitHub-Läufe bei leerem
Prioritäts-Backlog selbst kleine, rücknehmbare Verbesserungen am beobachtbaren Avatarleben
auswählen. Ziel ist nicht möglichst viel Output, sondern ein spielerisch sichtbares Vorher/Nachher:
Es soll interessanter werden, den Wesen beim Leben zuzusehen, ihre Unterschiede zu erkennen und
zu erleben, wie eigene Erinnerungen ihren Tag sanft beeinflussen.

- Offene `ITO-*`-Einträge in `evolutions/BACKLOG.md` behalten Vorrang. Ohne offenen Eintrag gilt
  `evolutions/DAILY_LIFE_TASK.md` als unveränderlicher Dauerauftrag für genau einen Lauf.
- Ein Lauf soll einen kleinen, hochwirksamen Spielerlebnis-Hebel wählen und möglichst mindestens
  zwei bestehende Einflüsse verbinden, etwa Tageszeit, Persönlichkeit, Ort, Tätigkeit, sanfte
  Reminder-Reaktion oder Kontinuität zwischen Szenen.
- Isolierte Requisiten, bloße weitere Bibliotheksanimationen, reine Lore-Menge, Pixelpolitur und
  generische Refactorings sind kein Ersatz für einen belegbaren Nutzen im Avatarleben.
- Bestehende Mechanismen sollen lesbarer und ausdrucksstärker zusammenspielen. Große neue Systeme,
  neue Produktentscheidungen oder erfundene Nutzerzustände bleiben außerhalb dieser Freigabe.
- `evolutions/DAILY_LIFE_LEARNING.md` ist das versionierte Feedback- und Lern-Overlay. Der Lauf
  darf dort erledigtes Feedback markieren, belegte Beobachtungen protokollieren und höchstens eine
  konservative, aus Tests oder Diff ableitbare Heuristik ergänzen.
- Das Overlay darf `.github/`, `runner/`, den Dauerauftrag, Berechtigungen, Sicherheits-Gates,
  Reviewer-, PR- oder Merge-Regeln niemals selbst verändern. Ideen zur Meta-Automatisierung werden
  nur dokumentiert und brauchen einen separaten, menschlich freigegebenen Prozess-PR.
- Nutzer- oder AI-Feedback wirkt erst, wenn es als normaler, geprüfter Repository-Diff in das
  Overlay gemerged wurde. Die Jobs lesen keine privaten Chats, keine Telemetrie und keine
  außerhalb des Repositorys liegenden Behauptungen.
- Alle Non-Negotiable Design Principles gelten unverändert, insbesondere druckfreie Reminder,
  getrennte Bedeutung von Tagesziel und Intervall, nutzereigene Routinen, lokale Datenhaltung,
  charaktergerahmter Zufall, `MEDICINE`-Ausschluss und die bestehenden XP-Regeln.

## Evolution Classes

Jede vorgeschlagene Evolution wird vor ihrer Bearbeitung als **CONTENT** oder **CODE**
klassifiziert. Die Klasse bestimmt die erforderliche Tiefe von Review, Tests und Releaseprozess,
ändert aber nichts an den Non-Negotiable Design Principles oder Protected Areas.

### CONTENT

Zur Klasse **CONTENT** gehören:

- Dialoge
- Lore
- Charakterreaktionen
- Balancing innerhalb bereits freigegebener Grenzen
- Konfigurationen

CONTENT darf später nach erfolgreichen automatisierten Prüfungen über einen schnelleren
Veröffentlichungsweg laufen. Die Einstufung erlaubt keine neuen Produktentscheidungen: Änderungen
außerhalb bereits freigegebener Grenzen, Eingriffe in geschützte Bereiche und strategisch offene
Fragen benötigen weiterhin eine ausdrückliche Review und Entscheidung. **Ausgenommen davon** sind
ausschließlich die unter "Character Evolution" → "Erzählerische Autonomie" benannten Punkte
(weitere Beziehungen, weitere Lore-Stücke) - dort ist die kreative Richtung bereits entschieden,
Review und Tests bleiben trotzdem verpflichtend.

### CODE

Zur Klasse **CODE** gehören:

- Kotlin-Code
- Datenbankschema und Migrationen
- Scheduling und Reminder-Auslösung
- Architektur und Modulgrenzen
- UI-Logik

CODE benötigt einen strengeren Review-, Test- und Releaseprozess. Insbesondere bleiben das Review
durch einen zweiten Agenten, die Prüfung gegen den aktuellen Stand von `main`, die relevanten
automatisierten Tests und gegebenenfalls Geräte- oder Migrationstests verpflichtend.

Enthält eine Evolution sowohl CONTENT- als auch CODE-Anteile, wird die gesamte Evolution für
Review, Tests und Release als **CODE** behandelt.

### Versionierung und Rücksetzbarkeit

- Jede automatisch veröffentlichte Evolution erhält eine eindeutige Version.
- Für jede solche Version wird der letzte bekannte stabile Zustand eindeutig referenziert.
- Vor der automatischen Veröffentlichung muss ein Rücksetzweg auf diesen stabilen Zustand
  vorhanden und geprüft sein.
- Eine Evolution darf nicht automatisch veröffentlicht werden, wenn ihre Version, ihr stabiler
  Vorgänger oder ihr Rücksetzweg nicht eindeutig bestimmt werden kann.
- Die Rücksetzung darf bestehende Nutzerdaten nicht stillschweigend verwerfen. Falls eine sichere
  Datenrücksetzung nicht möglich ist, wird nicht automatisch zurückgesetzt; der Fall wird
  abgebrochen und zur Entscheidung vorgelegt.

**OPEN DECISION:** Die konkreten automatisierten Prüfungen und Freigabegrenzen für den schnelleren
CONTENT-Veröffentlichungsweg sind noch nicht festgelegt.

**OPEN DECISION:** Der konkrete strengere Review-, Test- und Releaseprozess für CODE ist über die
bereits in diesem Protokoll festgelegten Mindestregeln hinaus noch nicht vollständig definiert.

**OPEN DECISION:** Versionsschema, Kennzeichnung des letzten bekannten stabilen Zustands und
technischer Rücksetzmechanismus für automatisch veröffentlichte Evolutionen sind noch nicht
festgelegt.

## Protected Areas – welche Bereiche nicht automatisch verändert werden dürfen

Folgende Bereiche dürfen weder durch automatisches Refactoring noch durch eine tägliche
Evolution ohne gesonderte Review und ausdrückliche Entscheidung semantisch verändert werden:

- Reminder-Semantik: Wochentage, Zeitfenster, Intervalle, Tagesziele, Pausen und Verhalten über
  Mitternacht.
- Die Regel, dass persönliche Routinen auch im Spielmodus weiterlaufen.
- Bildschirm-an-/Bildschirm-aus-Semantik und die Bedeutung eines verpassten Ereignisses.
- Nur-Uhr-/Quiet-Mode-Verhalten.
- Trennung zwischen Routinen-Besitzer und anwesendem Wesen.
- Zuordnung vorhandener Datenbank- und Preference-Schlüssel sowie ihre Bedeutung.
- Room-Schemas, Migrationen und historische Fütterereignisse.
- XP-Vergabe ausschließlich für beantwortete Spiel-Ereignisse.
- Berechnung des Levels aus XP statt paralleler Speicherung.
- Ausschluss von `MEDICINE` aus zufälligen Spielplänen.
- Lore-Texte, Charakterpersönlichkeiten, Beziehungen und Signaturthemen. **Teilausnahme seit
  2026-08-18:** neue Lore-Stücke und neue Beziehungen dürfen ergänzt werden, siehe "Character
  Evolution" → "Erzählerische Autonomie". Bestehende Texte, die drei etablierten Beziehungen und
  die sechs Persönlichkeiten bleiben unverändert geschützt.
- Datenschutz-, Netzwerk- und Store-Versprechen.
- Trennung zwischen persönlicher App-Instanz und öffentlicher Streaming-Welt.
- Ausschluss persönlicher und medizinischer Reminder aus jeder Zuschauerinteraktion.
- Twitch-/YouTube-, Streaming-, Netzwerk-, Konto-, Zahlungs-, Moderations- und
  Medienerzeugungsarchitektur, solange dafür keine eigene menschliche Entscheidung vorliegt.
- Paketnamen, Application IDs, Signatur-/Keystore-Konfiguration und Releasevarianten.
- Hardware-spezifische Glyph-SDK-Integration und Gerätekennungen.
- Barrierefreiheitssemantik der Kerninteraktionen.

Generierte Schemas, Binärdateien, das eingebundene Glyph-SDK und Releaseartefakte dürfen nicht
manuell oder beiläufig bearbeitet werden. Änderungen an ihnen müssen aus dem jeweils vorgesehenen
Build-, Migrations- oder Abhängigkeitsprozess entstehen.

## Character Evolution – Regeln für Persönlichkeit, Story und Beziehungen

### Persönlichkeiten

- Puffling bleibt der neugierige Optimist mit allgemeinem Schwerpunkt.
- Starlet bleibt die freundliche, ruhige Träumerin mit Schwerpunkt Achtsamkeit.
- Wyrmling bleibt der positive, nicht hektische Motivator mit Schwerpunkt Bewegung.
- Fennec bleibt der gelassene, verlässliche Beschützer mit Schwerpunkt Trinken.
- Gloop bleibt der gemütliche, leicht chaotische Entschleuniger mit Schwerpunkt Erholung.
- Hootlet bleibt der stille, geduldige Beobachter mit Schwerpunkt Fokus.
- Neue Texte und Handlungen müssen zur belegten Stimme der jeweiligen Figur passen.
- Charakterentwicklung darf eine Persönlichkeit erweitern, aber nicht ohne erzählerische
  Begründung in ihr Gegenteil verkehren.

### Story und gemeinsame Welt

- Die vorhandenen sieben Lore-Stücke je Wesen bilden eine Kennenlernfolge: Identität/Beruf,
  Wohnen, Hobby, Beziehung, Außenwelt, persönliche Offenbarung und gemeinsame Welt.
- Die Beziehungen Puffling–Gloop, Wyrmling–Fennec und Starlet–Hootlet bleiben Teil der
  gemeinsamen Welt.
- Gemeinsame Orte und Ereignisse müssen zwischen Figuren widerspruchsfrei bleiben.
- Lore darf erfundenes Worldbuilding enthalten. Aussagen über den Nutzer oder seinen Zustand
  dürfen dagegen nur aus tatsächlich vorhandenen Daten folgen.
- Lore-Freischaltung bleibt angesammelt und verlustfrei: Nicht abgeholte Teile verfallen nicht.
- Ein erneutes Anhören darf keinen erneuten Zeit-Zwang erzeugen.

### Beziehung

- Beziehungskapitel messen gemeinsame Zeit ab der ersten beantworteten Erinnerung mit diesem
  Wesen, nicht Leistung, Installation oder bloße Auslösung.
- Sie sind kein Rang, keine Serie und kein Wettbewerb.
- Ein neu gewähltes Wesen beginnt seine eigene Beziehungsgeschichte, ohne die Routinen des
  Nutzers zurückzusetzen.

### Erzählerische Autonomie (entschieden 2026-08-18)

Die beiden vorherigen `OPEN DECISION`-Punkte dieses Abschnitts sind nicht mehr offen. Entschieden
vom Produktverantwortlichen: Der automatisierte Evolutionslauf darf Welt und Beziehungen der
sechs Wesen eigenständig weiterentwickeln, ohne vor der einzelnen Änderung eine menschliche
Freigabe der kreativen Richtung einzuholen. Begründung: Ergebnisse sind über den üblichen Weg
(eigener Branch, Review durch einen zweiten Agenten, PR, menschlicher Merge) jederzeit prüfbar und
mit einem gewöhnlichen Revert genauso rücknehmbar wie jede andere Evolution - eine falsche
kreative Entscheidung kostet also keine Vorabprüfung wert, weil die Nachprüfung genauso wirksam
ist. Am Ablauf selbst ändert diese Entscheidung nichts, nur an der einen Rückfrage davor.

**Ohne Rückfrage erlaubt:**

- **Weitere Beziehungen** zwischen den sechs Wesen, zusätzlich zu Puffling–Gloop, Wyrmling–Fennec
  und Starlet–Hootlet. Diese drei bleiben bestehen und werden durch keine neue Beziehung ersetzt
  oder abgeschwächt - Ergänzung, nicht Austausch.
- **Weitere Lore-Stücke** über die bestehenden sieben je Wesen hinaus, als Fortsetzung des
  bisherigen episodischen Erzählens. Erhöht sich die Anzahl, ist das eine CODE-Evolution:
  `PlayLore.PIECES` sowie die neuen Textressourcen aller sechs Wesen müssen in derselben Evolution
  zusammen ergänzt werden (`values/strings.xml` und `values-de/strings.xml`, beide Sprachen
  inhaltlich gleich) - sonst hat ein Wesen mehr zu erzählen als ein anderes, und genau das prüft
  `PlayLoreTest` bereits automatisiert.

**Weiterhin `OPEN DECISION`, also weiterhin mit ausdrücklicher menschlicher Entscheidung:**

- Eine größere lineare Handlung, ein Quest-Ziel oder ein Sieg-/Niederlage-Zustand - daran ändert
  diese Freigabe nichts, siehe die entsprechenden `OPEN DECISION`-Punkte unter "Evolution Goals".
- Neue Fähigkeiten, Silhouetten oder Animationen, die durch Fortschritt freigeschaltet werden.
- Alles, was Produktidentität, Datenschema, Persistenzverträge oder andere in "Protected Areas"
  gelistete Bereiche berührt.

Jede so entstandene Änderung bleibt an die übrigen Regeln dieses Abschnitts gebunden - Stimme und
Persönlichkeit der Figuren, Widerspruchsfreiheit zu bestehenden Orten und Ereignissen, keine
Aussage über den Nutzer ohne Datenbeleg - und wird in der Evolution History wie jede andere
Evolution dokumentiert, zusätzlich ausdrücklich als **autonome kreative Entscheidung**
gekennzeichnet.

## Gameplay Evolution – Regeln für Balancing, Progression und Game Loop

### Game Loop

- Der Kernloop bleibt beobachtbar und verständlich: Ereignis erscheint, Nutzer antwortet,
  Wesen reagiert, Zustand wird korrekt protokolliert.
- Ambient-Aktivität darf die Kerninteraktion unterstützen, aber eine fällige Erinnerung nicht
  verdecken oder unbedienbar machen.
- Home, Dock und Widget müssen dasselbe offene Ereignis konsistent behandeln.
- Eine beantwortete Erinnerung darf nicht in einer anderen Oberfläche sichtbar weiterlaufen.

### Balancing

- Änderungen an Intervallen und Themengewichten müssen je Spezies begründet und mit Tests gegen
  ungültige Pläne abgesichert werden.
- Charakterunterschiede dürfen nicht zu irreführender Zuverlässigkeit oder zu medizinisch
  wirkenden Zufallsereignissen führen.
- Persönliche Reminder-Intervalle und Spieltempo bleiben getrennte Konzepte.
- Kollisionen dürfen echte Slots nicht stillschweigend verschieben; die Verarbeitung muss
  deterministisch und verlustfrei bleiben.

### XP und Level

- Aktueller Ausgangspunkt: 10 XP je beantwortetem Spiel-Ereignis, 50 XP je Level.
- Level werden aus XP abgeleitet und nicht unabhängig gespeichert.
- XP und Level werden je Wesen geführt.
- Balancingwerte dürfen nach Messung und Review geändert werden; Migration, bestehende Spielstände
  und Auswirkungen auf Levelgrenzen müssen dabei explizit bewertet werden.

### Entwicklungspfad

- Nur beantwortete echte Erinnerungen bestimmen die Richtung.
- Aktueller Ausgangspunkt: mindestens 20 Antworten, mindestens 40 Prozent Anteil, kein Gleichstand.
- Die vier belegten Richtungen und ihre Themenzuordnung dürfen nicht automatisch umgedeutet werden.
- Pfadstufen liegen derzeit bei Level 2, 5 und 9 und schalten sichtbare Gegenstände sowie Aussagen
  frei.
- Bei Änderungen muss geprüft werden, ob vorhandene Nutzer rückwirkend einen anderen Pfad oder
  andere Gegenstände erhalten würden.

### Stimmung

- Stimmung bleibt optional.
- Nur persönliche Erinnerungen mit Tagesziel fließen ein.
- Erwartung wird anteilig zum bereits vergangenen Zeitfenster berechnet.
- Ein neuer Tag beginnt ohne übertragene Schuld.

**OPEN DECISION:** Das endgültige Balancing von Intervallen, XP, Levelgrenzen, Pfadschwellen,
Münzen und Vorrat ist nicht als final bestätigt.

**OPEN DECISION:** Das Verhalten bei einem Moduswechsel während einer bereits offenen Erinnerung
ist nicht entschieden, insbesondere die nachträgliche XP-Vergabe.

**OPEN DECISION:** Es ist nicht entschieden, ob Münzen und Vorrat langfristig eine sichtbare
Spielökonomie bleiben, erweitert werden oder nur Ambient-Simulation unterstützen sollen.

## Technical Evolution – Regeln für Code- und Architekturänderungen

### Modulgrenzen

- `:core` bleibt die gemeinsame, darstellungsfreie Schicht für Datenmodell, Repositories,
  Validierung und Reminder-Planung.
- `:app` bleibt für die echte Nothing-Glyph-Hardware zuständig.
- `:app-sim` enthält Simulator-, Avatar-, Widget-, Dock- und Spielverhalten.
- App-spezifische Datenbanken, Alarmempfänger und Play-Reroll werden über `ReminderHost`
  eingebunden; diese Grenze darf nur bewusst verändert werden.

### Zustands- und Datenregeln

- Persistierte Schlüssel und IDs sind Teil des Datenvertrags. Umbenennungen benötigen eine
  Migration oder eine ausdrücklich akzeptierte Rücksetzung.
- Room-Schemaänderungen benötigen echte Migrationen und Migrationstests.
- Historische Daten dürfen nicht stillschweigend gelöscht werden, um neue Beziehungen oder
  Fremdschlüssel zu vereinfachen.
- Kalte Prozesse müssen Modus-, Quiet- und Scheduling-Zustände ohne gestartete Activity korrekt
  lesen können.
- Prozesslokale StateFlows und persistierte Werte dürfen nicht auseinanderlaufen.
- Zeit, Zeitzone, Reboot, Doze, fehlende Alarmberechtigung und ausgeschalteter Bildschirm sind bei
  jeder Scheduling-Änderung mitzudenken.

### Änderungsqualität

- Vor jeder Änderung ist `git status` zu prüfen.
- Vor jedem Push ist der aktuelle Diff zu prüfen.
- Änderungen sollen klein, nachvollziehbar und auf einen Zweck begrenzt sein.
- Bestehende Tests müssen vor einer Verhaltensänderung als Charakterisierung gelesen werden.
- Neue oder geänderte Regeln benötigen Tests auf der niedrigsten sinnvollen Ebene.
- Hardware- oder Android-Lifecycle-Verhalten darf nicht allein aus JVM-Tests als bestätigt gelten.
- Dokumentation, Store-Texte und Datenschutztexte müssen mit tatsächlichem Verhalten
  übereinstimmen.
- Keine automatische Architekturmodernisierung darf belegte Produktsemantik als bloßes
  Implementierungsdetail behandeln.

### Branch- und Review-Regeln für automatisierte Agenten

- Automatisierte Agenten arbeiten niemals gleichzeitig direkt auf `main`.
- Jede Evolution erfolgt auf einem eigenen Branch.
- Ein zweiter Agent reviewt den Branch, bevor ein Merge erfolgt.
- Bei Konflikten oder einem seit Beginn der Evolution veränderten `main` muss der arbeitende Agent
  den Evolutions-Branch auf den aktuellen Stand neu basieren oder die Evolution abbrechen. Ein
  Merge auf Grundlage eines überholten `main` ist nicht zulässig.

**OPEN DECISION:** Die langfristige Aufteilung und mögliche Konsolidierung von `:app` und
`:app-sim` ist nicht entschieden.

**OPEN DECISION:** Eine künftige Netzwerk-, Konto-, Cloud-, KI- oder Telemetriearchitektur ist
nicht beschlossen.

## Evidence & Uncertainty – Umgang mit Unsicherheit und fehlenden Informationen

Jede Evolutionsentscheidung muss ihre Grundlage kennzeichnen:

- **FACT:** direkt aus aktuellem ausführbarem Code, Ressourcen, Manifest oder Datenbankschema
  ableitbar.
- **DOCUMENTED INTENT:** in aktueller Dokumentation oder Codekommentaren ausdrücklich erklärt,
  aber nicht vollständig durch ausführbares Verhalten bewiesen.
- **TESTED BEHAVIOR:** durch vorhandene automatisierte Tests charakterisiert.
- **UNVERIFIED:** implementiert oder dokumentiert, aber laut Repository noch nicht auf dem
  relevanten Gerät oder in der relevanten Umgebung bestätigt.
- **OPEN DECISION:** strategisch, gestalterisch oder technisch nicht entschieden.

Bei Widersprüchen gilt:

1. Aktueller ausführbarer Code und aktuelle Tests bestimmen das derzeitige Verhalten.
2. Kommentare erklären die Absicht, dürfen widersprechenden Code aber nicht unsichtbar machen.
3. README-, Store- und Planungsdokumente werden auf Aktualität geprüft.
4. Ein Widerspruch wird dokumentiert und nicht durch Vermutung aufgelöst.
5. Produktsemantik wird nur nach ausdrücklicher Entscheidung geändert.

Fehlende Information darf nicht erfunden werden. Ein Vorschlag darf Optionen und Auswirkungen
formulieren, aber keine offene Option als bereits beschlossene Richtung darstellen.

## Daily Evolution Cycle – Analyse → Vorschlag → Review → Tests → Entscheidung

### 1. Analyse

- Git-Status prüfen und vorhandene Änderungen anderer Arbeiten respektieren.
- Vor Beginn einen eigenen Evolutions-Branch vom aktuellen `main` anlegen und dessen Ausgangsstand
  festhalten. Automatisierte Agenten arbeiten nicht gleichzeitig direkt auf `main`.
- Die Evolution als `CONTENT` oder `CODE` klassifizieren. Gemischte Änderungen gelten für den
  weiteren Ablauf als `CODE`.
- Relevanten Code, Tests, Ressourcen, Schemas und Dokumentation lesen.
- Aktuelles Verhalten, dokumentierte Absicht und offene Fragen getrennt festhalten.
- Betroffene geschützte Bereiche und Persistenzverträge identifizieren.

### 2. Vorschlag

- Problem, beobachtbare Evidenz und gewünschtes Ergebnis beschreiben.
- Kleinste sinnvolle Änderung vorschlagen.
- Auswirkungen auf Charakter, Reminder, Daten, Privatsphäre, Barrierefreiheit und bestehende
  Nutzerstände nennen.
- Alternativen und `OPEN DECISION`-Punkte sichtbar machen.
- Noch keine strategische Entscheidung als gegeben voraussetzen.

### 3. Review

- Vorschlag gegen Core Identity, Non-Negotiable Design Principles und Protected Areas prüfen.
- Den fertigen Evolutions-Branch vor einem Merge durch einen zweiten Agenten reviewen lassen.
- Widersprüche zu Code, Tests, Dokumentation und Store-Versprechen suchen.
- Produktentscheidungen ausdrücklich von technischen Implementierungsentscheidungen trennen.
- Bei einem geschützten oder offenen Punkt eine menschliche Entscheidung einholen.

### 4. Tests

- Bestehende relevante Tests ausführen.
- Bei `CONTENT` die dafür freigegebenen automatisierten Prüfungen ausführen.
- Bei `CODE` den strengeren, für die betroffenen Schichten erforderlichen Testprozess ausführen.
- Neue Regeln mit Unit-, Datenbank-, UI- oder Instrumentierungstests absichern.
- Migrationen mit bestehenden Schemas testen.
- Zeit-, Modus-, Prozess- und Kollisionsfälle berücksichtigen.
- Geräteabhängiges Verhalten auf dem vorgesehenen Gerät prüfen und bis dahin als `UNVERIFIED`
  kennzeichnen.

### 5. Entscheidung

- Erst nach Review und ausreichender Evidenz annehmen, überarbeiten oder verwerfen.
- Die Veröffentlichung entsprechend der Evolutionsklasse behandeln: `CONTENT` darf nur nach den
  freigegebenen automatisierten Prüfungen den schnelleren Weg verwenden; `CODE` durchläuft den
  strengeren Review-, Test- und Releaseprozess.
- Vor jeder automatischen Veröffentlichung die eindeutige Evolutionsversion, den letzten bekannten
  stabilen Zustand und den geprüften Rücksetzweg dokumentieren.
- Vor der Merge-Entscheidung prüfen, ob `main` seit Beginn der Evolution verändert wurde. Bei
  Änderungen oder Konflikten den Evolutions-Branch neu basieren und erneut prüfen oder die
  Evolution abbrechen.
- Die Entscheidung einschließlich Begründung und Auswirkungen dokumentieren.
- Nicht entschiedene Punkte als `OPEN DECISION` bestehen lassen.
- Vor Commit und Push den vollständigen Diff prüfen; Commit und Push erfolgen nur nach der dafür
  vorgesehenen Freigabe.

## Evolution History – dauerhaft zu dokumentierende Änderungen und Erkenntnisse

Für jede angenommene Evolution müssen mindestens folgende Informationen dauerhaft auffindbar
sein:

- Datum und kurze Bezeichnung.
- Eindeutige Evolutionsversion und, bei automatischer Veröffentlichung, Referenz auf den letzten
  bekannten stabilen Zustand.
- Ausgangsproblem und Nutzerwirkung.
- Evidenzklassifikation: `FACT`, `DOCUMENTED INTENT`, `TESTED BEHAVIOR`, `UNVERIFIED` oder
  `OPEN DECISION`.
- Getroffene Produktentscheidung und verworfene Alternativen.
- Betroffene Module, Datenmodelle, Preference-Schlüssel und öffentliche Texte.
- Änderungen an Reminder-Semantik, Game Loop, Charakter, Story, Balancing oder Progression.
- Migrationsauswirkungen und Umgang mit bestehenden Nutzerständen.
- Verwendbarer und geprüfter Rücksetzweg sowie bekannte Grenzen der Rücksetzbarkeit.
- Ausgeführte Tests sowie noch ausstehende Geräteprüfungen.
- Neu entstandene oder weiterhin offene Entscheidungen.
- Falls eine frühere Dokumentationsaussage überholt ist: alte Aussage, neuer Stand und Beleg.

Die Historie darf nicht zu einer bloßen Commit-Liste werden. Sie soll erklären, warum sich Itoeva
verändert hat, welche Identität dabei geschützt wurde und welche Unsicherheit weiterhin besteht.

### 2026-08-18 - Erzählerische Autonomie freigegeben

- **Version:** Protokoll 0.1 → 0.2. Kein Rücksetzweg im technischen Sinn nötig - eine
  Protokolländerung betrifft keine Nutzerdaten; Rücknahme ist ein gewöhnlicher Revert dieses
  Commits.
- **Ausgangsproblem:** Zwei `OPEN DECISION`-Punkte unter "Character Evolution" → "Beziehung"
  verhinderten, dass der automatisierte Evolutionslauf Welt oder Beziehungen der sechs Wesen
  überhaupt weiterentwickelt - jede Erweiterung hätte vorab eine menschliche Entscheidung
  gebraucht, obwohl das Ergebnis über Branch, Review und PR ohnehin prüfbar und per Revert
  rücknehmbar gewesen wäre.
- **Evidenzklassifikation:** `OPEN DECISION` (jetzt entschieden, siehe unten).
- **Getroffene Produktentscheidung:** Weitere Beziehungen zwischen den sechs Wesen und weitere
  Lore-Stücke über die bestehenden sieben hinaus dürfen ohne vorherige Rückfrage zur kreativen
  Richtung entstehen; der bestehende Ablauf (Branch, zweiter Agent, PR, menschlicher Merge) bleibt
  Pflicht. Verworfene Alternative: dieselbe Freigabe auch für Spielziel/Quest-Struktur oder
  freischaltbare Fähigkeiten zu erteilen - verworfen, weil beides tiefer in Balancing bzw.
  Pixel-Art-Gestaltung eingreift und die automatisierte Prüfung dafür (noch) nicht ausreicht.
- **Betroffene Module/Texte:** `EVOLUTION.md` selbst; vorbereitend `evolutions/BACKLOG.md` um
  ITO-0004 und ITO-0005 ergänzt, damit die Freigabe nicht folgenlos bleibt.
- **Änderungen an Charakter/Story:** keine inhaltliche Änderung durch diesen Eintrag selbst -
  reine Prozessänderung. Die drei bestehenden Beziehungen und alle 42 vorhandenen Lore-Texte
  bleiben unverändert.
- **Migrationsauswirkungen:** keine - keine Datenbank, keine Preference-Schlüssel betroffen.
- **Getestet:** nicht zutreffend, reine Dokumentation.
- **Neu entstandene offene Punkte:** keine. Weiterhin offen bleiben Spielziel/Quest-Struktur,
  Fortschritts-Freischaltungen sowie alle übrigen zuvor schon offenen Punkte.

### 2026-08-18 - Sachkorrektur Fortschritts-Freischaltung, Skillbaum-Idee festgehalten

- **Version:** Protokoll bleibt 0.2, keine neue Versionsnummer - reine Sachkorrektur eines
  bestehenden `OPEN DECISION`-Punktes plus Ergänzung, keine neue Freigabe.
- **Ausgangsproblem:** Der bisherige Wortlaut unter "Evolution Goals" behauptete, der aktuelle
  Stand schalte keine Inhalte durch Fortschritt frei. Das war falsch: `PlayScene.kt`
  (`enum class Acquisition`, `PlayPath`) tut das bereits produktiv - vier Pfade zu je drei
  Wohnungsgegenständen, vollständig belegt und durch `SceneCompositionTest` abgesichert.
- **Evidenzklassifikation:** Tatsachenfehler in bestehender Dokumentation, kein `OPEN DECISION`.
- **Getroffene Korrektur:** Der Abschnitt beschreibt jetzt den tatsächlichen Stand (Acquisition
  ist entschieden und gebaut) und grenzt ihn scharf von dem ab, was weiterhin offen ist: neue
  Fortschrittspfade, eine vierte Stufe je Pfad, neue Fähigkeiten/Silhouetten, echte
  Reminder-Freischaltungen. Keine Code- oder Verhaltensänderung, nur Dokumentation.
- **Nutzerwunsch festgehalten, nicht umgesetzt:** Der Nutzer brachte die Idee eines
  Skillbaums für die Avatare ein (Rollenspiel-Talentbaum-artig). Bewusst NICHT in dieselbe
  Kategorie wie ITO-0004/0005/0006/0007/0008 eingeordnet: Ein Skillbaum wäre eine neue
  Spielstruktur mit eigener Balancing-, UI- und Fortschrittslogik, kein einzelner Inhalt
  innerhalb eines bereits entschiedenen Rahmens. Bleibt `OPEN DECISION`, siehe "Evolution Goals".
- **Betroffene Module/Texte:** `EVOLUTION.md` selbst; vorbereitend `evolutions/BACKLOG.md` um
  ITO-0007 (Wald-Beiwerk) und ITO-0008 (zuletzt erzähltes Lore-Stück bleibt sichtbar) ergänzt -
  beide unter bereits entschiedenem Rahmen (Szenenvielfalt bzw. reine Leseanzeige bestehender
  Werte), keine neue Produktentscheidung.
- **Änderungen an Charakter/Story:** keine.
- **Migrationsauswirkungen:** keine.
- **Getestet:** nicht zutreffend, reine Dokumentation; ITO-0007/ITO-0008 tragen ihre eigenen
  Testanforderungen im Aufgabentext.
- **Neu entstandene offene Punkte:** Skillbaum/Talentbaum-System ausdrücklich als `OPEN DECISION`
  vermerkt (siehe oben) - nicht neu im Sinne des Prinzips, sondern erstmals benannt.

### 2026-08-22 - Actions-Kontingent, öffentliches Repository, Kosten-Gate und Prozessdokumentation

- **Version:** Protokoll bleibt 0.2, keine neue Freigabe unter "Character Evolution" - reine
  Prozess- und Historien-Ergänzung.
- **Ausgangsproblem:** Am 19.-21.08. stand die Evolutionskette zwölf Läufe lang still, weil das
  monatliche GitHub-Actions-Kontingent des damals privaten Repositorys erschöpft war (Jobs
  starben jeweils nach zwei Sekunden ohne ausgeführten Schritt, ohne Logs). Nachgerechnet an den
  Job-Laufzeiten kostete `verify.yml` zusätzlich strukturell zu viel: Auslösung an sowohl
  `pull_request` als auch `push` ohne Pfad-Filter bedeutete ~52 Minuten je Evolution, auch bei
  reinen Text-/Backlog-Änderungen ohne App-Code-Bezug.
- **Evidenzklassifikation:** `FACT` (Kontingent-Erschöpfung durch den Nutzer bestätigt,
  Kostenrechnung anhand realer historischer Job-Laufzeiten nachvollzogen).
- **Getroffene Entscheidung:** Repository von privat auf öffentlich umgestellt - eine bewusste
  menschliche Entscheidung außerhalb dieses Protokolls, kein automatisierter Schritt (öffentliche
  Repositories haben unbegrenzte GitHub-Actions-Minuten auf Standard-Runnern; Rücknahme jederzeit
  möglich). Zusätzlich, unabhängig davon: PR #21 fügt `verify.yml` einen vorgeschalteten
  "Betroffene Bereiche bestimmen"-Job hinzu, der reine Text-/Backlog-Änderungen an den teuren
  Jobs (Emulator-Tests, Lint/R8) vorbeischleust, ohne die Checks aus der PR-Ansicht verschwinden
  zu lassen (bewusst `if:`/Skip statt `paths-ignore`, damit sie sichtbar bleiben). Eine externe
  Bot-Review (Codex) fand vor dem Merge zwei reale Probleme an diesem PR, beide angenommen und
  behoben: (1) `git diff --name-only` erkennt Umbenennungen nicht korrekt und hätte verschobenen
  App-Code fälschlich als reine Dokumentation eingestuft - behoben mit `--no-renames`; (2) ein
  ursprünglich geplanter Verzicht auf Emulator-Tests beim `push`-Event war unsicher, weil der
  tatsächliche Merge-Baum vom zuletzt per PR geprüften Baum abweichen kann, sobald zwischenzeitlich
  ein anderer PR gemerged wurde - nachweislich bereits bei PR #16 so geschehen. Diese Optimierung
  wurde vor dem Merge zurückgenommen.
- **Betroffene Module/Texte:** `.github/workflows/verify.yml`; neu `Vision.md`,
  `Architecture.md`, `NextTasks.md`, `AgentGuide.md` als begleitende, dauerhafte
  Prozessdokumentation neben diesem Protokoll.
- **Änderungen an Charakter/Story:** keine.
- **Migrationsauswirkungen:** keine.
- **Getestet:** `verify.yml`-Änderung mangels verfügbarer CI-Minuten zunächst gegen sieben reale
  Merge-Commits der bisherigen Historie simuliert; nach Wiederherstellung der Minuten durch einen
  echten CI-Lauf auf PR #21 bestätigt (alle Jobs grün, inklusive beider Emulator-Matrizen).
- **Gelernte Lektion:** Ein mehrfach täglich laufender automatisierter Prozess ist gegen ein
  knappes CI-Kontingent nicht von selbst stabil - Kostenwächter (Pfad-Filterung,
  Zweitrigger-Vermeidung) sind keine optionale Politur, sondern Voraussetzung für die
  Zuverlässigkeit der Pipeline selbst. Sicherheitsrelevante Repository-Einstellungen (hier:
  Sichtbarkeitswechsel) über die mobile GitHub-Weboberfläche zu ändern erwies sich als
  unzuverlässig (404-Fehler nach korrekter Eingabe der Bestätigung); `gh repo edit --visibility
  public --accept-visibility-change-consequences` über die GitHub CLI war der zuverlässige Weg
  und sollte für vergleichbare Fälle bevorzugt werden.
- **Neu entstandene offene Punkte:** Ob `runner/` (PowerShell-/Windows-Task-Scheduler-basierte
  Automatisierung, laut eigenem `runner/README.md` standardmäßig deaktiviert und nirgends sonst
  referenziert) noch gebraucht wird oder von `claude-primary-run.yml` vollständig abgelöst wurde,
  ist ungeklärt (siehe Architecture.md). Ob sich die Duplizierung zwischen
  `app/ui/ReminderScreen.kt` und `app-sim/ui/ReminderScreen.kt` bzw. den beiden
  `ReminderAnimations.kt`-Dateien verlustfrei nach `core` oder ein gemeinsames UI-Modul heben
  lässt, ist ebenfalls ungeklärt - siehe NextTasks.md für den zugehörigen, bewusst kleinen
  Rechercheauftrag.

### 2026-08-22 - Persistente Aktions-Slots als überprüfbarer MVP

- **Version:** Protokoll bleibt 0.2; menschlich freigegebener, rücknehmbarer Gameplay-MVP.
- **Ausgangsproblem:** Eine laufende Erinnerung ließ sich nur sofort anwenden. Der Nutzer wollte
  Aktionen sichtbar für später aufheben und bewusst zwischen sofortigem Einsatz und Speichern
  wählen können.
- **Getroffene Entscheidung:** Der Simulator zeigt im Modus „Spiel“ vorläufig vier runde, rechts
  oberhalb der Umgebungswelt angeordnete Slots; außerhalb dieses Modus sind Anzeige und
  Speicheraktionen nicht aktiv. Eine Aktion lässt sich dort ablegen und später über dieselbe
  Fütter-Pipeline einsetzen; unbeantwortet auslaufende Aktionen belegen im Spiel den ersten freien
  Platz. Die Belegung bleibt je Wesen über App-Neustarts erhalten. Vier ist eine MVP-Hypothese,
  keine Festlegung der langfristigen Slot-Anzahl; deren Validierung bleibt NT-053.
- **Betroffene Module/Texte:** `app-sim`-Startbildschirm, neue Slot-UI und lokaler Slot-Speicher,
  Onboarding-/Barrierefreiheitstexte; `:app` und Dock-Modus bleiben unverändert.
- **Migrationsauswirkungen:** keine Room-Migration; maximal vier kleine, profilgetrennte
  UI-Snapshots liegen in der privaten Preference-Datei `action_slots`.
- **Getestet:** `:app-sim:compileDebugKotlin` und `:app-sim:testDebugUnitTest`; CI deckt Lint,
  R8 und beide Emulator-Matrizen ab.
- **Neu entstandene offene Punkte:** optimale Slot-Anzahl, Kontextboni, Kombinationen und
  zeitabhängige Situationen bleiben offen und dürfen nicht aus diesem MVP abgeleitet werden.

### 2026-08-22 - Automatisches Ablegen ausgelaufener Erinnerungen in Speicherplätze wieder entfernt

- **Version:** Protokoll bleibt 0.2, reine Verhaltenskorrektur einer bereits gemergten
  Produktentscheidung - keine neue Freigabe unter "Character Evolution".
- **Ausgangsproblem:** PR #20 (Speicherplätze) führte bewusst ein: läuft eine Erinnerung ab, ohne
  dass darauf reagiert wurde, wandert sie automatisch in den ersten freien Speicherplatz statt
  verloren zu gehen. Der Nutzer hat dieses automatische Verhalten nach Ausprobieren ausdrücklich
  abgelehnt: "kein automatisches Auffüllen".
- **Evidenzklassifikation:** `FACT` (direkte Nutzeräußerung, per Rückfrage auf den Umfang
  bestätigt: überall, nicht nur im gerade neu gebauten Spielmodus-Bildschirm).
- **Getroffene Korrektur:** Automatisches Ablegen vollständig entfernt, sowohl in
  `HomeScreen.kt` (`archiveActiveReminderIfExpired()` umbenannt zu `clearExpiredReminder()`, tut
  jetzt nur noch, was der Name sagt) als auch in `DockScreen.kt` (dort erst mit demselben PR
  eingeführt, das die Speicherplätze auf den Spielmodus-Bildschirm portiert hat, und im selben
  Zug wieder entfernt, bevor es gemergt war). Eine ausgelaufene, unbeantwortete Erinnerung ist
  damit wieder wie vor PR #20 verloren. Speicherplätze füllen sich seitdem ausschließlich durch
  die bewusste Zieh-Geste (Uhr auf einen freien Platz), nie von selbst.
- **Betroffene Module/Texte:** `app-sim/src/main/java/com/notime/glyphsim/ui/HomeScreen.kt`,
  `app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt`.
- **Änderungen an Charakter/Story:** keine.
- **Migrationsauswirkungen:** keine - `ActionSlotStore` (SharedPreferences) und `avatar_feed_events`
  bleiben in ihrer Struktur unverändert, nur ein Schreibpfad entfällt.
- **Getestet:** nicht per Emulator nachvollzogen (kein Android SDK in dieser Sitzung verfügbar,
  siehe Architecture.md/NextTasks.md zur Testlücke bei den Speicherplätzen) - über CI geprüft.
- **Neu entstandene offene Punkte:** keine neuen. Die bereits als Future Backlog vermerkte
  Testlücke bei den Speicherplätzen (NT-018/NT-019/NT-025) besteht unverändert fort.

### 2026-08-23 - Drei weitere Freizeit-Beschäftigungen: Angeln, Musizieren, Malen

- **Version:** Protokoll bleibt 0.2; Gameplay-Erweiterung nach demselben, bereits etablierten
  Muster wie Drachensteigen (PR #32) und Fußball (PR #36).
- **Ausgangsproblem:** Nutzerwunsch, weitere "Doings" wie Drachensteigen und Fußball zu ergänzen.
  Rückfrage ergab: nicht diese beiden selbst (schon vorhanden), sondern weitere Beschäftigungen
  in ihrem Stil. Nutzer wählte "Angeln am Teich" als erste und "gleich mehrere (2-3)" als Umfang,
  mit kreativer Freiheit bei der Ausgestaltung.
- **Getroffene Entscheidung:**
  - **Angeln am Teich** - vollständig neuer Ort `Place.POND` (Schilf und ein Steg-Pfosten als
    Hintergrund, kein Kollisionsrisiko dank `behind = true`, wie beim Sportplatz), neue
    dreiphasige Szene (`FishingPhase.CAST/WAIT/CATCH`) analog zu Drache/Fußball, in
    `AnimationType.MOVE` mit 40 % Wahrscheinlichkeit eingehängt (Drache 55 %, Fußball 65 %,
    bewusst niedriger, damit der Teich eine von mehreren Möglichkeiten bleibt, kein Pflichttermin).
  - **Musizieren im Park** und **Malen auf der Wiese** - bewusst NICHT als weitere eigene Orte mit
    neuer Einrichtung gebaut (Kollisionsrisiko beim Einfügen in bereits bestückte Räume ohne
    lokale Kompilierbarkeit, siehe unten), sondern als neue `PlayEffects.Carried`-Gegenstände
    (Gitarre, Staffelei) in zwei zusätzlichen `AnimationType.CREATIVITY`-Abläufen, die den
    bestehenden Park bzw. die bestehende Wiese aufsuchen - dieselbe Reaktionsanimation, nur
    unterwegs statt an der Werkbank.
- **Betroffene Module:** `PlayScene.kt` (neuer Ort POND, zwei neue Hintergrund-Requisiten),
  `PlayEffects.kt` (neue `FishingPhase`, `fishingCells()`, zwei neue `Carried`-Einträge),
  `PlayRoutine.kt` (neuer `RoutineStep.Fishing`, `fishingRoutine()`, zwei neue CREATIVITY-Abläufe),
  `DockScreen.kt` (Phasenzustand, Dispatch, Aufräumen, Rendering-Merge für die Angel-Szene -
  Musizieren/Malen brauchen dort keine Änderung, da `Take`/`Drop`/`carriedCells()` bereits generisch
  über jeden `Carried`-Wert arbeiten), `PlayRoutineTest.kt` (neuer Test analog zum
  Fußball-/Drachen-Test).
- **Änderungen an Charakter/Story:** keine.
- **Migrationsauswirkungen:** keine.
- **Getestet:** nicht lokal kompiliert - kein Android SDK und kein Zugriff auf das Gradle-Plugin-
  Repository in dieser Umgebung (`com.android.application` ließ sich nicht auflösen). Stattdessen:
  Architektur vollständig über einen Recherche-Durchlauf kartiert (alle `when(place)`-Dispatchpunkte
  gezielt gegengeprüft, nicht nur die anfangs gefundenen), jede neue Stelle gegen das bestehende
  Drache-/Fußball-Vorbild abgeglichen, Klammern-/Klammern-Bilanz aller geänderten Dateien geprüft.
  CI ist hier die eigentliche Verifikation.
- **Neu entstandene offene Punkte:** Beim Lesen des bestehenden Codes aufgefallen (nicht behoben,
  da außerhalb dieser Aufgabe): Der `KITE_CHANCE_PERCENT`-Zweig in `PlayRoutines.forTopic()` sucht
  die Drachen-Routine über `pool.firstOrNull { it is RoutineStep.Kite }` - `pool` schließt Drachen-
  Routinen aber vorher bereits aus (`everyday`-Filter), wodurch dieser Zweig die Drachen-Routine
  vermutlich nie tatsächlich zurückgibt. Fußball und die neue Angel-Routine umgehen das, indem sie
  bei Treffer direkt die jeweilige Funktion zurückgeben statt über `pool` zu suchen. Verdient eine
  eigene, gezielte Prüfung und ggf. Korrektur.

### 2026-09-02 - Kontrollierte Tagesablauf-Evolution und lernendes Feedback-Overlay

- **Version:** Protokoll 0.2 → 0.3. Prozessänderung ohne App-Laufzeit- oder Nutzerdatenänderung;
  Rücksetzweg ist der Revert dieses Prozess-PRs. Der letzte bekannte stabile Produktstand bleibt
  der Basiscommit dieses PRs.
- **Ausgangsproblem und Nutzerwirkung:** Der feste Backlog lenkte die täglichen Läufe zuletzt auf
  leicht abzählbare Einzelstücke wie Requisiten, Lore-Sätze und Bibliotheksanimationen. Der Nutzer
  möchte stattdessen mehr Freiraum für kleine Stellschrauben, durch die Tagesablauf,
  Persönlichkeit, Übergänge und Reminder-Einfluss beim Zuschauen spürbar interessanter werden,
  ohne dass der Lauf sich in Kleinigkeiten verzettelt.
- **Evidenzklassifikation:** `DOCUMENTED INTENT` für den ausdrücklich erteilten Nutzerauftrag;
  `FACT` für die bisherige Backlog-Reihenfolge und die vorhandene Trennung von Modell- und
  Push-Rechten; künftige Nutzenbehauptungen müssen je Evolution als `TESTED BEHAVIOR` oder klar
  begrenzte Beobachtung belegt werden.
- **Getroffene Produktentscheidung:** Offene `ITO-*`-Aufgaben bleiben die menschlich gesetzte
  Priorität. Ist keine offen, liefert `evolutions/DAILY_LIFE_TASK.md` einen kontrollierten
  Dauerauftrag für genau eine kleine, spielerisch sichtbare Verbesserung innerhalb bestehender
  Mechanik. `evolutions/DAILY_LIFE_LEARNING.md` führt Nutzerfeedback, Evidenzjournal und wenige
  konservative Heuristiken versioniert zwischen angenommenen Läufen weiter.
- **Verworfene Alternativen:** Kein unbeschränkter Selbstumbau der Automatisierung, kein Zugriff
  auf private Chats oder Telemetrie, kein automatischer Merge und kein Freibrief für große neue
  Systeme. Die offenen Mikroaufgaben ITO-0012, ITO-0013 und ITO-0014 werden bewusst ohne Umsetzung
  geschlossen; ihre Spezifikationen bleiben als verworfene Alternativen erhalten.
- **Betroffene Bereiche:** Workflow-Auswahl und Prompts in
  `.github/workflows/claude-primary-run.yml`, Runner-Schutzkonfiguration und Strukturtest,
  `evolutions/BACKLOG.md`, die zwei neuen Dauerauftrag-/Lern-Dateien sowie begleitende Agenten-,
  Architektur-, Aufgaben- und Tagesablauf-Dokumentation.
- **Geschützte Grenzen:** Modell-Token und Push-Recht bleiben in getrennten Jobs; der Builder darf
  Workflow, Runner, Backlog und Dauerauftrag nicht schreiben. Zweite Review-Session, PR und
  menschlicher Merge bleiben Pflicht. Änderungen an diesen Grenzen dürfen aus dem Lern-Overlay
  nur als Idee hervorgehen und brauchen einen separaten, menschlich freigegebenen Prozess-PR.
- **Daten und Migration:** Keine App-Datenbank, Preferences, Reminder-Daten oder öffentlichen
  App-Texte betroffen; keine Migration. Das Overlay enthält nur versionierte Repository-Texte,
  keine Nutzungsdaten.
- **Reminder, Game Loop, Charakter und Progression:** Durch diesen Prozess-PR selbst unverändert.
  Künftige Daueraufträge dürfen ausschließlich innerhalb der oben dokumentierten, bereits
  entschiedenen Grenzen arbeiten und müssen ihren sichtbaren Vorher/Nachher-Nutzen im jeweiligen
  PR belegen.
- **Tests:** Workflow-YAML und Runner-JSON werden syntaktisch geparst; Backlog-Selektor,
  Leer-Backlog-Fallback, Schreibschutz und Token-/Push-Trennung werden strukturell geprüft.
  Android-Verifikation ist für diesen reinen Prozess-Diff nicht erforderlich; jeder spätere
  App-Code-PR durchläuft weiterhin seine üblichen Prüfungen.
- **Offene Punkte:** Welche Heuristiken tatsächlich bessere Beobachtbarkeit und Freude erzeugen,
  kann ohne Produktfeedback nicht abschließend entschieden werden. Das Overlay darf deshalb nur
  kleine, überprüfbare Hypothesen sammeln; menschliches Feedback kann sie in späteren PRs
  bestätigen, korrigieren oder entfernen.

### 2026-09-03 - Öffentliche Evolutionswelt und verbindliche Cloud-Code-Übergabe

- **Version:** Protokoll 0.3 → 0.4. Strategische Produkt- und Dokumentationsentscheidung ohne
  Änderung an App-Laufzeit, Datenmodell oder GitHub-Workflow; Rücksetzweg ist ein gewöhnlicher
  Revert dieses Dokumentations-PRs.
- **Ausgangsproblem und Nutzerwirkung:** Nach dem in PR #62 verankerten Tagesablauf-Dauerauftrag
  war zwar geklärt, woran tägliche Jobs arbeiten sollen, aber nicht vollständig dokumentiert,
  warum die Evolution selbst Teil des Produkterlebnisses ist. Die gewünschte Verbindung aus
  persönlicher App, öffentlichem stillem Twitch-Beobachtungsraum und avatarerzähltem
  YouTube-Tagebuch fehlte. Neue Cloud-Code-Sitzungen hätten diese Richtung nur aus einem Chat,
  nicht aus dem Repository erfahren.
- **Evidenzklassifikation:** `DOCUMENTED INTENT` auf Grundlage der ausdrücklichen
  Nutzerentscheidung vom 2026-09-03. Alle Plattform- und Betriebsannahmen bleiben `UNVERIFIED`
  beziehungsweise, wo noch keine Entscheidung getroffen wurde, `OPEN DECISION`.
- **Getroffene Produktentscheidung:** Itoeva soll als öffentlich nachvollziehbares Experiment die
  Möglichkeiten und Grenzen automatisierter Geschichten- und Avatarwelt-Gestaltung zeigen. Die
  drei Zielperspektiven sind persönliche App, getrennte Twitch-Welt und avatarzentriertes
  YouTube-Evolutionstagebuch. Zuschauer dürfen die öffentliche Welt künftig über begrenzte
  Impulse beeinflussen, aber nicht beliebig steuern.
- **Verworfene Alternativen:** Kein Fernzugriff auf persönliche Reminder oder App-Daten, kein
  Zuschauerzugriff auf `MEDICINE`, keine unmittelbare Übernahme ungeprüften Freitexts und keine
  stillschweigende Freigabe einer kompletten Streaming-/Backend-/Bezahlinfrastruktur durch die
  Vision allein.
- **Agentenübergabe:** Neu `CLOUD_CODE_BRIEFING.md` als kurzer, versionierter Startprompt. Sowohl
  `AGENTS.md` als auch `CLAUDE.md` verweisen verbindlich darauf, damit Claude Code und andere
  Agenten den Tagesablauf-Dauerauftrag aus PR #62, die neue Zielidentität und deren Grenzen vor
  produktbezogener Arbeit kennen.
- **Betroffene Bereiche:** Ausschließlich `Vision.md`, `EVOLUTION.md`, `AGENTS.md`,
  `AgentGuide.md`, `CLAUDE.md` und das neue Briefing. Keine Quelltexte, Ressourcen, Workflows,
  Secrets, Store-Texte oder Abhängigkeiten betroffen.
- **Daten und Migration:** Keine App-Daten, Room-Schemas, Preferences oder Migrationen betroffen.
  Die öffentliche Welt erhält durch diesen PR noch keine Datenquelle und keine Verbindung zur
  persönlichen App.
- **Reminder, Game Loop und Monetarisierung:** Bestehende Reminder-Semantik und der aktuelle Game
  Loop bleiben unverändert. Abos/Donations sind nur als künftiger Berechtigungsrahmen für
  öffentliche Weltimpulse entschieden; Anbieter, Preise, Kontingente, Moderation, rechtliche
  Prüfung und technische Umsetzung bleiben offen.
- **Tests:** Dokumentstruktur, Links, `OPEN DECISION`-Abgrenzungen und Diff werden statisch
  geprüft. Android- oder Migrationstests sind für diesen reinen Dokumentationsstand nicht
  einschlägig; spätere Implementierungen benötigen die für ihren tatsächlichen Scope vorgesehenen
  Tests.
- **Offene Punkte:** Konkrete Streaming-, Backend-, Konto-, Zahlungs-, Moderations-, Sprach- und
  Videoarchitektur; Plattformregeln und Medienrechte; Verfügbarkeit des Dauerstreams; Form und
  Frequenz der Avatarberichte; konkrete Zuschauerimpulse und ihre Limits; mögliche, derzeit nicht
  erlaubte Verbindung öffentlicher Ereignisse mit persönlichen App-Instanzen.

### 2026-09-03 - Der Stundenplan wirkt jetzt auch im laufenden Tag

- **Version:** Protokoll bleibt 0.4. Reine Verhaltensaenderung im Spielmodus ohne Datenmodell-,
  Schema- oder Preference-Aenderung; Ruecksetzweg ist der Revert dieses PRs, ohne Nutzerdatenfolge.
- **Ausgangsproblem und Nutzerwirkung:** Gemeldet als "der Avatar laeuft die ganze Zeit repetitiv
  zwischen den Raeumen hin und her" und "ein voller Tagesablauf soll erkennbar sein". Die Ursache
  war nicht ein fehlender Tagesablauf, sondern ein ungenutzter: `PlayPresence.topicFor` enthaelt
  einen exakten 24-Stunden-Plan, wurde aber ausschliesslich beim BETRETEN des Spielmodus gelesen.
  Die Regungs-Schleife danach kannte nur die vier groben `DayPhase`-Bloecke - von 11 bis 17 Uhr ist
  alles gleich "Mittag", also konnten Mittagessen, Arbeit und Fokus in beliebiger Reihenfolge
  beliebig oft kommen. Weil jede `PERFORM`-Regung den Ort wechselt, entstand daraus der Eindruck
  eines Wesens, das pendelt statt einen Tag zu haben.
- **Evidenzklassifikation:** `FACT` fuer die bisherige Nutzung der Tabelle (nur `entry` und der
  Play-Modus-Einstieg lasen sie) und fuer die Phasengrenzen; `TESTED BEHAVIOR` fuer die neue
  Gewichtung und die beiden geschuetzten Zusagen (vier Tests in `PlayAmbientActivityTest`);
  `DOCUMENTED INTENT` fuer den Nutzerwunsch nach lesbarem Tagesablauf bei erhaltener Ueberraschung.
- **Getroffene Produktentscheidung:** Der Stundenplan wird als VIERTES additives Signal in
  `PlayAmbientActivity.nextTopic` gegeben - nach demselben Muster, mit dem `boostedTopics`,
  `leaning` und `stayAt` bereits andocken. Der Zuschlag `PLAN_BONUS = 4` ist bewusst so gross wie
  `HABIT_BOOST` (beide sagen "das steht jetzt an", aus Uhr bzw. Nutzer) und bewusst kleiner als
  `STAY_BONUS` (5), damit die 2026-08-22 behobene Beschwerde ueber staendige Raumwechsel nicht
  zurueckkehrt. Anders als Neigung und Verweilen darf der Plan ein Thema auch EINFUEHREN; sichtbar
  wird das genau einmal am Tag, naemlich beim Arbeitsbeginn um zehn, den die Phase MORNING sonst
  nicht kennt.
- **Verworfene Alternativen:** Ein eigener Tagesplan mit Reihenfolge, Einmal-pro-Tag-Bloecken und
  persistiertem Gedaechtnis wurde erwogen und verworfen - er waere die von `DAILY_LIFE_TASK.md`
  ausgeschlossene "neue grosse Spielstruktur" und haette den Tag ausserdem vorhersehbar gemacht,
  also dem Ziel "beim Oeffnen ueberrascht werden" widersprochen. Ebenfalls verworfen: den Plan die
  Phasengewichte ERSETZEN zu lassen; Charakterneigung und offene Gewohnheiten muessen weiter
  dagegen gewichten koennen, sonst spulen alle sechs Wesen denselben Tag ab. Ein Vorgabewert, der
  die Uhr selbst liest, wurde verworfen, weil er jedem Aufrufer mit ausdruecklicher Phase den Plan
  der echten Uhrzeit untergeschoben haette.
- **Betroffene Bereiche:** `matrix/PlayAmbientActivity.kt` (neue `plannedTopicFor`, erweiterte
  `nextTopic`/`combinedWeights`), `ui/PlayPresence.kt` (delegiert die Tabelle statt sie zu
  duplizieren, Verhalten unveraendert), `ui/DockScreen.kt` (reicht den Plan durch, ueber
  `PlayTimeLapse` statt der Systemuhr, damit Plan und Phase derselben Zeit folgen).
- **Reminder-Semantik, Datenmodell, Migration:** unveraendert. Keine Room-, Preference- oder
  Textaenderung, keine Auswirkung auf bestehende Nutzerstaende.
- **Geschuetzte Grenzen:** `MEDICINE` bleibt ausgeschlossen - die Tabelle nennt es nie, und
  `combinedWeights` filtert es zusaetzlich defensiv aus dem uebergebenen Plan. Die Nachtruhe
  ("durchgehend schlafen") bleibt unangetastet, weil der Plan nachts ausschliesslich `SLEEP`
  liefert und damit nur verstaerkt, was ohnehin gilt. Beides ist mit einem Test abgesichert.
- **Ausgefuehrte Tests:** Vier neue Tests in `PlayAmbientActivityTest`: Vollstaendigkeit und
  MEDICINE-Freiheit des Stundenplans, deutliche aber nicht sichere Anhebung des geplanten Themas,
  Einfuehrung eines phasenfremden Themas (WORK morgens), sowie der doppelte Schutz von
  MEDICINE-Ausschluss und Nachtruhe. Die Verteilungsgrenzen der beiden statistischen Tests wurden
  vorab gegen dieselbe Gewichtungsformel nachgerechnet und liegen mehr als sechs
  Standardabweichungen von den Schranken entfernt.
- **Ausstehende Geraeteprueung:** Der subjektive Eindruck "der Tag ist jetzt lesbar" laesst sich
  nur am Geraet beurteilen und steht noch aus. Die Aenderung wurde in der Cloud-Sitzung nicht
  lokal gebaut (kein Netzzugang zum Android-Gradle-Plugin); den Nachweis fuehrt die CI.
- **Weiterhin offen:** Wie stark Nutzergewohnheiten den autonomen Ablauf praegen duerfen, bleibt
  `OPEN DECISION`. Ob zusaetzlich ein Wiederholungs-Daempfer noetig ist, ist als naechster Hebel
  im Lernjournal vermerkt, aber noch nicht belegt.

### 2026-09-03 - Merge-Freigabe für eine beauftragte Agentensitzung

- **Version:** Protokoll 0.4 → 0.5. Reine Prozessänderung ohne Code-, Schema- oder
  Nutzerdatenwirkung. Rücksetzweg ist der Revert dieses PRs; danach gilt wieder ausschließlich der
  menschliche Merge.
- **Ausgangsproblem und Nutzerwirkung:** Die Dokumente verlangten an vier Stellen einen
  menschlichen Merge. In der Praxis blieben dadurch fertige, vollständig grüne Pull Requests
  liegen - und weil die APK-Auslieferung an einem Merge auf `main` hängt, bekam der Nutzer keine
  neue Fassung seiner App, obwohl die Arbeit fertig war. Er hat mehrfach gefragt, warum keine neue
  APK kommt; die Antwort war jedes Mal "weil niemand gemergt hat".
- **Evidenzklassifikation:** `DOCUMENTED INTENT` - der Produktverantwortliche hat auf die
  ausdrückliche Rückfrage, ob die im Repository stehende Merge-Sperre überschrieben werden soll,
  mit "du kannst immer mergen" geantwortet. `FACT` für die vier Fundstellen und dafür, dass
  zwischen dem 03.09. 06:58 und 15:15 keine Auslieferung entstand, obwohl zwei fertige grüne Pull
  Requests vorlagen.
- **Getroffene Produktentscheidung:** Eine vom Produktverantwortlichen beauftragte Agentensitzung
  darf einen Pull Request mergen, wenn drei Bedingungen zugleich erfüllt sind: CI vollständig grün,
  kein Merge-Konflikt, keine offene Review-Anmerkung unbeantwortet. Sind sie nicht erfüllt, ist
  Beheben die Aufgabe - nicht Warten und nicht Mergen.
- **Was ausdrücklich NICHT gelockert wurde:** Der unbeaufsichtigte Lauf mergt sein eigenes Ergebnis
  weiterhin nie. `claude-primary-run.yml` und `runner/` haben kein Merge-Recht, und die Trennung
  zwischen Schreibrecht und Merge-Entscheidung bleibt damit als Sicherheitseigenschaft erhalten.
  Ein direkter Push auf `main` bleibt auch für eine Sitzung ausgeschlossen; gemergt wird über einen
  Pull Request. Branch, Tests, zweite Prüfung und PR bleiben Pflicht.
- **Verworfene Alternativen:** Die Sperre unangetastet zu lassen und den Widerspruch zwischen
  Dokument und gelebter Praxis bestehen zu lassen - verworfen, weil jeder andere Agent und Codex
  selbst die Dokumente lesen und ihnen folgen würden, während eine Sitzung mit mündlicher Freigabe
  anders handelt. Genau diese Art stehengebliebener Regel hat am selben Tag schon zu einem Fehler
  geführt: Eine Pflegeroutine aus der Zeit vor PR #62 verlangte ein Auffüllen des absichtlich
  geleerten Backlogs. Ebenfalls verworfen: die Freigabe auf den unbeaufsichtigten Lauf auszudehnen
  - der Nutzer hat einer Sitzung Vertrauen gegeben, nicht der Pipeline, und die Rechtetrennung ist
  zu teuer erkauft, um sie beiläufig aufzugeben.
- **Betroffene Texte:** `evolutions/DAILY_LIFE_LEARNING.md`, `Tagesablauf.md`,
  `CLOUD_CODE_BRIEFING.md`, `AgentGuide.md` und der Kopf dieses Dokuments. Die Protokolleinträge
  vom 2026-08-18 und 2026-09-02 bleiben im Wortlaut unverändert - sie beschreiben, was DAMALS
  entschieden wurde, und Historie wird nicht umgeschrieben. Wo dort "menschlicher Merge" steht,
  gilt seit heute der vorliegende Eintrag.
- **Geschützte Grenzen:** unverändert. Insbesondere bleiben die fünf nicht verhandelbaren
  Erfahrungs-Eigenschaften, der `MEDICINE`-Ausschluss, die Room-Migrationspflicht und alle
  `OPEN DECISION`-Punkte unberührt. Diese Entscheidung betrifft ausschließlich, WER den
  Merge-Knopf drücken darf.
- **Ausgeführte Tests:** Keine automatisierten - reine Dokumentenänderung. Geprüft wurde
  stattdessen die Vollständigkeit: Alle vier normativen Fundstellen wurden gesucht und
  angeglichen, damit kein Dokument dem anderen widerspricht.
- **Weiterhin offen:** Ob der unbeaufsichtigte Lauf jemals selbst mergen darf, bleibt
  `OPEN DECISION` und ist mit dieser Entscheidung NICHT beantwortet.

### 2026-09-03 - Wiederholungs-Daempfer: dieselbe Handlung faellt seltener zweimal hintereinander

- **Version:** Protokoll bleibt 0.5. Reine Verhaltensaenderung im Spielmodus ohne Datenmodell-,
  Schema- oder Preference-Aenderung; Ruecksetzweg ist der Revert dieses PRs, ohne Nutzerdatenfolge.
- **Ausgangsproblem und Nutzerwirkung:** Das Lernjournal vom selben Tag notierte als naechsten
  Hebel, aber ausdruecklich unbelegt: "ein gerade gespieltes Thema fuer wenige Runden geringer
  gewichten". Diese Sitzung hat das belegt statt nur vermutet: `PlayAmbientActivity.weightsFor`
  gibt WORK/FOCUS/DRINK/MOVE mittags je Gewicht 3 - bei unabhaengiger Ziehung ohne Gedaechtnis
  faellt dieselbe Handlung dort rechnerisch in etwa jeder sechsten Runde (Summe der quadrierten
  Anteile ≈ 16,4 %) zweimal hintereinander. Sichtbar wirkt das wie ein Wesen, das zweimal
  hintereinander dieselbe Tasse trinkt, statt einen Tag mit Abwechslung zu haben.
- **Evidenzklassifikation:** `TESTED BEHAVIOR` - ein neuer Test belegt die Basis-Wiederholungsrate
  vor der Aenderung (`ohne Daempfer wiederholt sich ein Thema spuerbar oft`), ein zweiter die
  Verringerung danach, zwei weitere schuetzen die Nachtruhe- und Phasen-Garantien.
- **Getroffene Produktentscheidung:** `nextTopic`/`combinedWeights` erhalten `justPlayed` als
  fuenftes, additives (in diesem Fall subtraktives) Signal - nach demselben Andock-Muster wie
  `boostedTopics`, `leaning`, `stayAt` und `plannedTopic`. `REPEAT_MALUS = 2` senkt das Gewicht des
  zuletzt gespielten Themas fuer die naechste Ziehung, mit einem Bodenwert von 1 (nie ausgeschlossen)
  und ohne Wirkung, wenn das Thema die einzige Moeglichkeit im kombinierten Pool ist. `DockScreen`
  reicht dafuer sein bereits vorhandenes `currentTopic` durch - kein neuer Zustand noetig.
- **Verworfene Alternativen:** Eine harte Sperre ("dasselbe Thema darf nicht zweimal hintereinander
  fallen") wurde verworfen - sie waere die in `Tagesablauf.md` ausgeschlossene harte Sonderregel und
  haette echte, plausible Wiederholungen (zweimal hintereinander DRINK bei grossem Durst) unmoeglich
  gemacht. Eine laenger anhaltende Sperre ueber mehrere Runden (eigener Zaehler wie `stayedRounds`)
  wurde ebenfalls verworfen: `currentTopic` daempft bereits jede Runde neu, solange sich das Thema
  nicht aendert, und braucht dafuer keinen zusaetzlichen persistierten Zustand.
- **Betroffene Bereiche:** `matrix/PlayAmbientActivity.kt` (neuer Parameter `justPlayed`, neue
  Konstante `REPEAT_MALUS`, erweiterte `combinedWeights`), `ui/DockScreen.kt` (reicht `currentTopic`
  als `justPlayed` durch).
- **Reminder-Semantik, Datenmodell, Migration:** unveraendert. Keine Room-, Preference- oder
  Textaenderung, keine Auswirkung auf bestehende Nutzerstaende.
- **Geschuetzte Grenzen:** `MEDICINE` bleibt ausgeschlossen (der Daempfer wirkt nur auf Themen, die
  bereits im gefilterten Pool stehen). Die Nachtruhe ("durchgehend nur SLEEP") bleibt unangetastet,
  weil der Daempfer bei genau einem moeglichen Thema im Pool (`combined.size > 1`-Wache) gar nicht
  greift - mit einem eigenen Test abgesichert.
- **Ausgefuehrte Tests:** Fuenf neue Tests in `PlayAmbientActivityTest`: Beleg der urspruenglichen
  Wiederholungsrate, Nachweis der Verringerung durch den Daempfer, Nachweis dass eine Wiederholung
  weiterhin moeglich bleibt, Schutz der Nachtruhe-Garantie und Schutz davor, dass der Daempfer ein
  phasenfremdes Thema einfuehrt.
- **Ausstehende Geraeteprueung:** Ob eine unmittelbare Wiederholung beim Zuschauen ueberhaupt noch
  auffaellt, laesst sich nur am Geraet beurteilen und steht noch aus. Die Aenderung wurde in der
  Cloud-Sitzung nicht lokal gebaut (kein Netzzugang zum Android-Gradle-Plugin); den Nachweis fuehrt
  die CI.
- **Weiterhin offen:** Wie stark Nutzergewohnheiten den autonomen Ablauf praegen duerfen, bleibt
  `OPEN DECISION`. Der naechste sinnvolle Hebel ist im Lernjournal (`DAILY_LIFE_LEARNING.md`)
  vermerkt.

### 2026-09-04 - Dritter Schnitt, erstmals ausserhalb von `sport`: Musizieren

Zwei Sport-Familien konnten noch dieselbe Familie zweimal sein. Dieser Schnitt beantwortet, was
die beiden offen lassen mussten: Traegt das Muster auch eine Beschaeftigung mit einem anderen
Themen-Typ und einem anderen Ortsprofil?

Es traegt. `AvatarActivityPlans.resolve` bleibt der einzige Eingang; dazugekommen sind eine dritte
Verzweigung und ein dritter Schritt-Bauer (`resolveMusic`).

**Der Beleg liegt im Themen-Typ.** Beide Sport-Familien melden `MOVE`, weil `sport` so angelegt
ist; Musik meldet `CREATIVITY`, weil `kreativ` so angelegt ist. Der Resolver waehlt den Typ
nirgends selbst - er liest ihn aus dem Baum ab. Genau das ist der Nachweis, dass hier kein
zweites Regelwerk neben `AnimationTree` entsteht.

**Erneut kein neuer `RoutineStep`, keine neue Phase.** `PlayEffects.MusicPhase { TUNE, PLAY,
FINALE }` und `RoutineStep.Music` gab es laengst; `PlayRoutines` benutzt sie sogar bereits in
einer eigenen CREATIVITY-Routine ("Musizieren im Park"), und `DockScreen.runRoutine` zeichnet den
Schritt. Die Phase war im Alltag also verbunden - nur nicht mit einer *Absicht*. Diese eine Luecke
schliesst der Schnitt. Unangeschlossen warten weiterhin `BasketballPhase`, `PaintingPhase`,
`FishingPhase` und `KitePhase`.

**Eine Freischaltung, mehr wird nicht behauptet.** `TUNE` und `PLAY` bilden zusammen die Basis,
die auch ohne jeden Knoten vollstaendig ist: Er stimmt und er spielt. `FINALE` erscheint
ausschliesslich mit tatsaechlich freigeschaltetem `kreativ/musik/singen` - auf keinem Level und
bei keiner Spezies sonst. Erfundene Level-Schwellen bleiben ausgeschlossen, solange
`Tagesablauf.md` sie offen laesst.

Warum `PLAY` zur Basis gehoert: Beim Fussball ist `TOUCH` fuer sich eine ganze kleine Szene, beim
Training braucht `WARM_UP` den Ausklang `REST`. Stimmen allein waere dasselbe Stueck Stumpf - man
saehe jemanden ein Instrument richten und dann aufhoeren. Das Koennen liegt im Abschluss, den
`FINALE` mit fuenf statt drei Noten sichtbar macht, nicht darin, ueberhaupt einen Ton zu spielen.

**Eine bewusste Ortsentscheidung, keine abgeleitete Tatsache.** Beim Sport zwingt die Sache selbst
nach draussen - ein Ball und ein Tor gehoeren nicht ins Wohnzimmer. Ein Instrument braucht dagegen
nur Platz zum Sitzen, und die Choreografie zeichnet Gitarre, Noten und Buehnenlinie neben dem
Avatar statt in die Kulisse. `LOCAL_MUSIC_PLACES` umfasst deshalb Park, Wiese, Wohnzimmer und
Leseecke; Schlafzimmer, Bad, Kueche, Werkstatt und alles Oeffentliche bleiben aussen vor. Von dort
fuehrt derselbe sichtbare `GoToPlace(PARK)`-Weg hinaus - kein Teleport.

**`drum` und `bolt` bleiben absichtlich aussen vor.** Beide haengen unter `kreativ/musik`, aber die
vorhandene Choreografie kennt keine Phase, die sie voneinander unterscheiden koennte. Sie als
Absicht zu fuehren und dann dasselbe zu zeigen wie `singen` waere eine Behauptung ohne Deckung.

**Nebenbefund aus dem Duplikat-Bericht:** `kreativ/musik` und `kreativ/musik/singen` spielten auf
dem alten Reaktionsweg Bild fuer Bild dasselbe. Der Schnitt macht sie unterscheidbar, ohne eine
einzige Zeile Reaktionscode anzufassen - dieselbe Beobachtung wie bei `sport/ballsport` in P16,
nur ueber den Handlungsweg geloest statt ueber neue Motive.

### 2026-09-04 - Die Unit-Tests laufen jetzt auch ohne Gradle und Geraet

Die Werkzeuge in `tools/reaction-preview/` konnten den Code bisher nur uebersetzen und seine
Ausgabe zeigen. Ob die vorhandenen Tests dazu gruen sind, liess sich lokal gar nicht beantworten -
die Antwort kam erst Minuten spaeter aus der CI. Damit war jeder Push eine Wette, und genau daran
sind in diesem Repository schon Befunde entstanden, die vorher haetten auffallen koennen.

`tools/reaction-preview/tests.sh` fuehrt die reinen Kotlin-Unit-Tests jetzt tatsaechlich aus: 94
Tests aus acht Klassen in unter einer Sekunde, mit JUnit 4 aus Maven Central und demselben
Kotlin-Compiler, den die Nachbarskripte schon holen. Kein Android, kein Gradle, kein Emulator.

Die Grenze ist ausdruecklich benannt und nicht geraten: Alles, was Android, Room, Compose oder
einen Emulator braucht, bleibt Sache der CI. Die Liste der ausgefuehrten Klassen steht deshalb
wortwoertlich im Skript statt als Platzhalter - aus demselben Grund, aus dem `render.sh` seit dem
Codex-Befund eine ausdrueckliche Dateiliste fuehrt.

Ausserdem stand `tools/reaction-preview/.work/` bisher nicht in `.gitignore`. Das fiel nie auf,
weil die Werkzeuge nie im Repository selbst ausgefuehrt wurden; beim ersten echten Lauf lagen
dort der geholte Kotlin-Compiler (~80 MB), JUnit und die uebersetzten Klassen als unversionierte
Dateien. Jetzt ignoriert.

### 2026-09-04 - Zweiter Schnitt nach demselben Muster: Kraft & Ausdauer

Der Fussball-Schnitt hat die Frage beantwortet, WIE eine Absicht zu einer sichtbaren Handlung
wird. Dieser Eintrag beantwortet die Anschlussfrage: Traegt das Muster einen zweiten Fall, ohne
dass daneben ein zweites System entsteht?

Es traegt. `AvatarActivityPlans.resolve` bleibt der einzige Eingang; dazugekommen ist eine
Verzweigung nach der Wirtsbeschaeftigung und ein zweiter Schritt-Bauer. Kein neuer Zustand, kein
neuer Bus, keine zweite Entscheidungsschicht.

**Was dabei NICHT gebaut wurde, und warum das der Punkt ist:** kein einziger neuer
`RoutineStep`, keine neue Phase in `PlayEffects`. `TrainingPhase { WARM_UP, LIFT, REST }` und
`RoutineStep.Training` gab es laengst - `PlayRoutines` benutzt sie in einer MOVE-Routine, und
`DockScreen.runRoutine` zeichnet sie. Gefehlt hat nur die Verbindung zur Absicht. Wer den
naechsten Schnitt baut, sollte deshalb zuerst nachsehen, was schon da ist:
`BasketballPhase`, `MusicPhase`, `PaintingPhase`, `FishingPhase` und `KitePhase` warten
unangeschlossen.

**Die Regel aus dem Fussball-Schnitt gilt unveraendert weiter:** Die Freischaltung veraendert die
HANDLUNG, nicht bloss eine Zugabe danach. Ohne `sport/kraft-ausdauer/heben` gibt es kein Heben -
auf keinem Level, bei keiner Spezies. Erfundene Level-Schwellen bleiben ausgeschlossen, solange
`Tagesablauf.md` sie offen laesst.

**Eine bewusste Abweichung vom Fussball:** Der Ausklang (`REST`) haengt an keiner Freischaltung.
Beim Fussball ist `TOUCH` fuer sich schon eine vollstaendige kleine Szene; Aufwaermen ohne
Ausklang waere dagegen ein Stumpf - man saehe jemanden anfangen und dann abbrechen. Ein Abschluss
ist kein Koennen.

`LOCAL_FOOTBALL_PLACES` heisst jetzt `LOCAL_SPORT_PLACES`. Die Menge ist unveraendert, nur ihr
Geltungsbereich - Wiese und Park taugen fuers Ballspielen wie fuers Ueben. Eine zweite, identische
Menge daneben waere eine Kopie, die auseinanderlaeuft.

### 2026-09-04 - Reminder werden zu kontextabhaengigen Handlungen: Fussball als erster Schnitt

- **Version:** Protokoll bleibt 0.5. Die Aenderung erweitert das bestehende Verhalten im Spielmodus;
  kein neues Datenmodell, keine Room-Migration und keine neue persistente Weltarchitektur.
- **Ausgangsproblem und Nutzerwirkung:** Reminder und freigeschaltete Skills endeten bislang trotz
  vorhandener mehrstufiger Play-Routinen oft in einer generischen Reaktion oder einer zufaelligen
  Einlage NACH der eigentlichen Alltagshandlung. Insbesondere schrumpfte eine Bibliotheksanimation
  wie `Football` nach dem Fuettern auf das grobe Thema `MOVE`; der folgende Ablauf kannte dadurch
  weder die genaue Absicht noch den Ort, an dem sie ausgesprochen wurde. Skillfortschritt war so
  nur eingeschraenkt am tatsaechlichen Verhalten des Wesens ablesbar.
- **Getroffene Architekturentscheidung:** Kein paralleler `ContextualActionResolver` und kein
  zweites Handlungssystem. Die bereits vorhandenen `AvatarActivityPlans`/`AvatarActivityBus`
  bilden die Entscheidungsebene; ihr Ergebnis ist eine vorhandene `PlayRoutine`, die weiterhin
  von `DockScreen.runRoutine`, `PlayScene`, `RoutineStep` und `PlayEffects` ausgefuehrt wird.
  `ReactionTrigger` behaelt den exakten Skillbaum-Knoten eines Reminders bis zu dieser Entscheidung.
- **Fussball als vertikaler Schnitt:** `sport/ballsport`, `dribbling` und `schuss` werden anhand
  von aktuellem `PlayScene.Place` und echten Freischaltungen konkretisiert. Auf Sportplatz, Park
  und Wiese bleibt die Handlung lokal; aus ungeeigneten Innenraeumen verwendet sie den bestehenden
  `GoToPlace(SPORT)`-Schritt und damit den sichtbaren Weg statt eines Teleports. Ein Anfaenger zeigt
  nur Ballkontakt; freigeschaltetes Dribbling fuegt eine erkennbare Dribbling-Sequenz hinzu; nur ein
  freigeschalteter Schuss darf `AIM`/`KICK` erzeugen. Avatar-Level wird bewusst noch NICHT fuer
  Varianten benutzt: Welche Level welche Ablaufe freischalten, ist in `Tagesablauf.md` weiterhin
  eine `OPEN DECISION` und wird durch diesen Schnitt nicht vorweggenommen.
- **Laufende Weltaktivitaet:** Eine ausgewaehlte Ballsport-Handlung wird im bereits vorhandenen
  `AvatarActivityBus` als aktuelle Aktivitaet gesetzt. Damit kann ein weiterer Stufe-3-Skill an
  eine laufende Ballsport-Aktivitaet anschliessen; der Zustand bleibt sitzungsgebunden und laeuft
  wie zuvor nach fuenf Minuten ab, statt als zweite Persistenzschicht gespeichert zu werden.
- **Rueckwaertskompatibilitaet:** Andere Skillbereiche behalten Claudes bisherigen
  `SkillRepertoire`-Flourish-Weg. Auch kontextuell aufloesbare Fussballknoten bleiben im autonomen
  PERFORM-Repertoire sichtbar, solange dieser Pfad keinen exakten Skill-Intent besitzt. Eine
  Doppelung entsteht nicht: Der exakte Reminder-/Skill-Pfad fuehrt die kontextuelle Routine aus und
  kehrt davor zurueck, statt danach noch `SkillRepertoire.pick` aufzurufen. Die generische
  `requestedTopic`-Logik bleibt der Rueckfall fuer alle nicht unterstuetzten Intents.
- **Spezies:** Im ersten Fussball-Schnitt bewusst kein Entscheidungsfaktor. Die vorhandenen
  Speziesanimationen werden weiterhin von `runRoutine` verwendet, aber es existiert noch keine
  belegte Produktregel, nach der eine Spezies andere Fussballfaehigkeiten besitzen darf als eine
  andere. Eine solche Regel waere eine gesonderte Produktentscheidung.
- **Tests:** `AvatarActivityPlansTest` prueft lokale Park-Ausfuehrung, sichtbaren Wechsel aus einem
  ungeeigneten Innenraum sowie striktes Freischalt-Gating fuer Dribbling und Schuss. Ein eigener
  Regressionstest in `SkillRepertoireTest` schuetzt, dass diese Unlocks im autonomen MOVE-Alltag
  weiterhin sichtbar bleiben. Level-Schwellen werden ausdruecklich nicht getestet oder erfunden.
- **Daten und Migration:** Keine Schema-, Migration- oder neue Preference-Aenderung. Verwendet
  werden ausschliesslich bestehende Freischaltungen, `PlayScene.Place` und der sitzungsgebundene
  `AvatarActivityBus`.
- **Ausstehende Geraetepruefung:** Die objektiven Auswahlregeln sind automatisiert pruefbar; ob
  sich Anfaenger, Dribbling und Schuss beim Zuschauen deutlich genug voneinander unterscheiden,
  muss zusaetzlich am Geraet beurteilt werden.

### Initialer Erkenntnisstand

- Persönliche Routinen laufen im aktuellen Play-Modus weiter; die Spiel-Erinnerung kommt hinzu.
- Routinen gehören dem Nutzer, gemeinsame Erlebnisse und Spielstand dem gewählten Wesen.
- Stimmung ist optional und berücksichtigt nur persönliche Erinnerungen mit Tagesziel.
- Beziehungskapitel beruhen auf gemeinsam vergangener Zeit, nicht auf Leistung.
- Spiel-XP stammen ausschließlich aus beantworteten Spiel-Ereignissen.
- Persönliche Entwicklungspfade beruhen ausschließlich auf beantworteten echten Erinnerungen.
- Zufällige Spielpläne schließen Medizin aus.
- Lore besteht aktuell aus sieben angesammelten, verlustfrei freigeschalteten Teilen je Wesen.
- Mehrere reale Geräte- und Moduswechsel-Fälle sind laut Repository noch nicht abschließend
  verifiziert.
- Produktname, primäre App-Fassung, finales Spielziel, größere Storystruktur und langfristige
  Ökonomie bleiben **OPEN DECISION**.

### 2026-09-05 - Der erste Audio-Asset trifft auf ein Prinzip, das es ausschliesst

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Dieser Eintrag entscheidet nichts, er legt
  eine Entscheidung offen, die bisher unbemerkt getroffen worden waere.
- **BEOBACHTET:** Die Musik-Pipeline (PR #77, #78) ist in `main` und hat mit
  „Quiet Lanterns" (PR #82) ihren ersten Track erzeugt: 90 s, 44,1 kHz, stereo, **15,88 MB
  unkomprimiertes WAV**. PR #82 legt damit `app-sim/src/main/res/raw/` an - **das Verzeichnis
  existierte bisher nicht, die App hat heute keinen einzigen Audio-Asset.**
- **BEOBACHTET:** `PlayChime.kt` begruendet in seiner Klassendoku ausdruecklich, warum der Klang
  der App **gerechnet und nicht abgespielt** wird, mit drei Gruenden: keine Lizenzfrage, es passt
  zur gerechneten Welt („Ein aufgenommener Klang daneben waere derselbe Bruch wie eine
  fotografierte Blume in einer Pixel-Kulisse"), und - woertlich - „Kein Asset, keine Groesse im
  Paket, kein Dekoder". Das ist das klangliche Gegenstueck zu „Prozedural statt gemalt" in
  `Vision.md`.
- **BEOBACHTET:** Die Musik-Pipeline ist in `EVOLUTION.md`, `Vision.md`, `Architecture.md` und
  `NextTasks.md` **an keiner Stelle erwaehnt**. Ihre einzige Dokumentation ist `music/README.md`.
  Laut `AGENTS.md` gilt bei Widerspruechen `EVOLUTION.md` - und dort steht zu diesem Thema nichts,
  woraus sich der Widerspruch aufloesen liesse.
- **ABGELEITET:** Ein Merge von PR #82 wuerde damit zwei Dinge zugleich tun, von denen nur eines
  sichtbar ist: einen Track hinzufuegen, und stillschweigend entscheiden, dass Itoeva kuenftig
  Audio als Asset ausliefert. Genau das schliesst dieses Dokument fuer `OPEN DECISION`-Punkte aus.
- **GEMESSEN (2026-09-05, an genau dieser Datei):** Die Groesse ist kein Naturgesetz des Tracks,
  sondern eine Folge des gewaehlten Formats. Alle Werte nachgerechnet, alle Ergebnisdateien
  zurueckgelesen und auf 90,0 s geprueft:

  | Format | Groesse | Anteil am WAV |
  |---|---|---|
  | WAV 44,1 kHz stereo (heute) | 15,88 MB | 100 % |
  | FLAC 44,1 kHz stereo (verlustfrei) | 3,48 MB | 21,9 % |
  | **Vorbis 44,1 kHz stereo** | **1,06 MB** | **6,7 %** |
  | Opus 48 kHz stereo | 1,37 MB | 8,6 % |
  | Vorbis 44,1 kHz mono | 0,63 MB | 4,0 % |
  | Vorbis 22,05 kHz mono | 0,41 MB | 2,6 % |

- **ABGELEITET (Formatwahl ist nicht frei):** `:app-sim` hat `minSdk = 26`. Vorbis in `.ogg` wird
  seit den ersten Android-Versionen unterstuetzt; **Opus in `.ogg` erst ab Android 10 (API 29)**
  und faellt damit fuer API 26-28 aus, obwohl es hier kaum kleiner waere. Das ist vor einer
  Festlegung gegen die aktuelle Android-Formattabelle zu pruefen und nicht aus diesem Eintrag zu
  uebernehmen.
- **GEMESSEN (Stereo ist echt):** Kanalkorrelation L/R 0,870, Differenz-RMS 0,0385 gegen
  Signal-RMS 0,0753. Der Track ist nicht breitgezogenes Mono - eine Mono-Ablage naehme
  tatsaechlich Breite weg und ist deshalb eine Klangentscheidung, keine reine Sparmassnahme.
  Spitzenpegel 0,782, also ohne Uebersteuerung.
- **ENTSCHIEDEN am 2026-09-05 (Format):** Ausgeliefert wird **Ogg/Vorbis**, 44,1 kHz, stereo.
  `manifest.json` steht auf `output_format: "ogg"`, `generate_music.py` schreibt es direkt.
  Nicht Opus (minSdk 26), nicht Mono (das Stereobild ist echt), nicht FLAC (verlustfrei ohne
  Empfaenger unter einer 16x16-Figur).
- **ENTSCHIEDEN am 2026-09-05 (Ort der Umwandlung):** In der **Pipeline**, nicht im Build. Die
  Provenienz-Kette lebt davon, dass das Gehoerte das Ausgelieferte ist - `music/README.md` sagt
  "Erst der Merge des generierten PR macht ihn zum versionierten Spiel-Asset"; ein
  nachtraeglich neu kodierender Build wuerde etwas ausliefern, das niemand gehoert hat. Das
  uebliche Gegenargument - ein verlustfreies Archiv fuer spaetere Neukodierung - greift hier
  nicht: Das Manifest pinnt Seed, Modell und Runtime-Commit, eine Neuerzeugung kostet zwei
  Minuten und liefert dasselbe. Ein WAV-Archiv waere die Sicherung von etwas jederzeit
  Rekonstruierbarem.
- **ENTSCHIEDEN am 2026-09-05 (die Grenze der Ausnahme):** Audio als Asset ist zugelassen, aber
  begrenzt: **Alles, was die Welt oder das Wesen selbst von sich gibt, bleibt gerechnet. Nur der
  Score darf eine Datei sein.** Eine Filmmusik war noch nie aus demselben Material wie das
  Buehnenbild; die Stimme einer Figur schon. `PlayChime` behaelt damit sein Prinzip vollstaendig
  fuer den Bereich, in dem es traegt, und Musik bekommt eine benannte Spur statt einer Ausnahme
  ohne Rand. Wer spaeter Schritte, Tueren oder Wetter als Sample ergaenzen will, verletzt sie.
- **OPEN DECISION - weiterhin NICHT entschieden:**
  1. Ob **realistische** generierte Musik ueber einer 16x16-Welt aesthetisch richtig ist. Das ist
     der eigentliche Kern von `PlayChime`s zweitem Grund und laesst sich nicht wegkomprimieren -
     es ist eine Hoerentscheidung, keine technische.
  2. Wie viele Tracks langfristig vorgesehen sind. Die Rechnung beantwortet die Frage allerdings
     teilweise selbst: Vier Tageszeiten mal drei Stimmungen waeren als WAV rund 191 MB und damit
     nicht mehr auslieferbar, als Vorbis rund 13 MB.
  3. Die Lizenzlage. `music/README.md` haelt fest, dass Stability-Community-Lizenz und
     Gemma-Terms **vor einer kommerziellen Veroeffentlichung erneut zu pruefen** sind. Solange
     `PLAY_STORE.md` existiert, ist das ein offener Punkt und keine Formalie.
- **Umgesetzt:** Die Musikschicht `PlayMusic` spielt den Track als Schleife, solange der
  Spielmodus sichtbar ist - standardmaessig AUS, nie ueber fremdem Ton, nicht bei stumm
  gestelltem Geraet, ohne Audio-Focus zu greifen. Nachts wird **nicht** gesperrt (anders als bei
  `PlaySound`): Musik laeuft nur, solange jemand den eingeschalteten Bildschirm ansieht und sie
  eingeschaltet hat, und ein Abendtrack, den man abends nicht hoeren darf, waere sinnlos.
- **Bewusst ueber Namen statt `R.raw` aufgeloest:** Die Audiodatei kommt aus einem eigenen
  erzeugten Pull Request und ist kein fester Bestandteil des Quellbaums. Ein direkter Verweis
  wuerde jeden Build brechen, in dem noch kein Track gemergt ist; so bleibt die Welt still, bis
  es etwas zu hoeren gibt. Arbeitspaket: NT-055.

### 2026-09-05 - Musik richtet sich nach Tageszeit und Ort; der Schalter bleibt der des Nutzers

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. `ENTSCHIEDEN` durch Produktvorgabe, im Code
  umgesetzt und mit Tests belegt.
- **Die Trennung, um die es geht:** *Die Welt entscheidet, WAS passen wuerde. Der Nutzer
  entscheidet, OB ueberhaupt Musik laufen darf.* Beides liegt jetzt in getrennten Dateien.
  `MusicResolver` (in `PlayMusicPlan.kt`) kennt weder die Einstellung noch einen Player noch
  Android; `PlayMusic` fragt ihn **erst**, wenn der Nutzer Musik erlaubt hat. Ein Szenenwechsel
  kann Musik deshalb nicht eigenmaechtig einschalten - der Resolver kennt den Schalter nicht
  einmal.
- **BEOBACHTET (Persistenz war bereits richtig):** `SettingsCatalog.MusicEnabled` liegt in
  SharedPreferences, und die einzige schreibende Stelle ist der Schalter in den Einstellungen.
  `stop()` fasst die Einstellung nicht an. Die Anforderung "die letzte Nutzerentscheidung
  ueberlebt App-Start, Moduswechsel, Szenen- und Tageszeitwechsel" war damit schon erfuellt;
  neu ist nur, dass Tests das jetzt festhalten statt es zu unterstellen. **Nicht neu gebaut.**
- **Sparse und hierarchisch statt Matrix:** Vier Tageszeiten mal sechzehn Orte waeren
  vierundsechzig Felder, von denen fast alle dasselbe enthielten, und jeder neue Ort verlangte
  vier neue Entscheidungen. Stattdessen liefert `MusicResolver.candidates` je Lage eine kurze,
  **vom Spezifischsten zum Allgemeinsten** geordnete Liste von Rollen; `resolve` nimmt die erste,
  zu der es einen ausgelieferten Track gibt. Fehlt alles, bleibt es still. Ein neuer Track wird
  dadurch gehoert, sobald er gemergt ist, ohne dass eine Zeile Entscheidungslogik zu aendern ist.
- **Rollen statt Dateinamen:** `MusicRole` fuehrt `main_day_background`,
  `home_evening_background`, `morning_background`, `sport_background` und `dream_background` -
  dieselben Namen wie das Feld `role` im Manifest, dazu den passenden `android_resource`.
  `generate_music.py` verlangt `role` jetzt als Pflichtfeld, prueft es gegen dieselbe Liste und
  schreibt es in die Provenienz. Ein Tippfehler scheitert damit beim Erzeugen statt sich als
  Stille zu zeigen.
- **Der heutige Stand ist ausdruecklich unvollstaendig, und das ist richtig so:** Es gibt genau
  einen Track (`HOME_EVENING`). Morgen und Mittag ergeben deshalb **Stille**. Ein Abendstueck den
  ganzen Tag zu spielen waere schlechter als nichts - und es waere der bequeme Fehler, den ein
  Rueckfall "irgendwas ist besser als Stille" genau hier erzeugt haette. Nachts gibt es aus
  demselben Grund keinen Rueckfall auf den Tages-Track.
- **Kein Zerhacken durch kurzfristige Bewegung:** `PlayMusic.apply` tut nichts, solange sich die
  aufgeloeste ROLLE nicht aendert. Ein Avatar, der zwischen Kueche und Wohnzimmer wechselt, laesst
  die Musik weiterlaufen. Ein echter Trackwechsel ist heute ein harter Schnitt und liegt in genau
  einer Methode (`switchTo`); eine Ueberblendung waere derzeit Architektur fuer ein Verhalten, das
  mit einem einzigen Track gar nicht auftreten kann. Naechster Schritt, siehe NT-055.
- **Vorbereitet, aber NICHT erzeugt:** `main-day-01` / "Lantern Streets" steht mit Prompt und
  Manifest-Eintrag bereit - japanisch gepraegter jazzy Lo-Fi-Hip-Hop, instrumental, rund 82 BPM,
  warme Rhodes-Akkorde, staubiger Boom-Bap-Groove, dezente pentatonische Faerbung. Der Prompt
  beschreibt **Eigenschaften und Instrumente**, keinen Kuenstler und kein Stueck, wie es
  `music/README.md` verlangt. Erzeugt wird er erst nach dieser Arbeit.
- **Fehler behoben, der noch nicht sichtbar war:** `app-sim` baut Release mit
  `isShrinkResources = true`. Da die Tracks nur ueber ihren Namen gesucht werden, gab es keine
  statische Referenz - der Schrumpfer haette sie entfernen duerfen, und die Musik waere
  ausgerechnet im signierten Release still gewesen. Im Debug faellt das nie auf, und die CI baut
  kein geschrumpftes Release (der Job "Release-Torwaechter beisst" prueft nur, dass
  `bundleRelease` OHNE Keystore scheitert). `app-sim/src/main/res/raw/keep.xml` haelt jetzt
  `@raw/itoeva_*` fest - bewusst mit Platzhalter, damit kuenftige Tracks nicht vergessen werden.
- **Weiterhin OPEN DECISION:** ob realistische generierte Musik ueber einer 16x16-Welt
  aesthetisch richtig ist, und die Lizenzpruefung vor einer kommerziellen Veroeffentlichung.

### 2026-09-05 - Zwei Tracks werden zu einem sanften Zustandswechsel

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Kleine, ruecknehmbare Verfeinerung der am
  selben Tag entschiedenen Rollen-Architektur; keine neue Musikrolle und kein neues Asset.
- **BEOBACHTET:** Seit PR #89 liegen nicht mehr einer, sondern zwei Tracks im Paket:
  `HOME_EVENING` / Quiet Lanterns und `MAIN_DAY` / Lantern Streets. README, Manifest-Kommentar,
  Tests und KDoc beschrieben noch den Zustand vor diesem Merge. Vor allem war der als "spaeter"
  dokumentierte harte Rollenwechsel damit erstmals wirklich erreichbar.
- **Spielerwirkung:** Ein echter Rollenwechsel bricht den bisherigen Track nicht mehr ab. Alter
  und neuer Player laufen vier Sekunden nebeneinander und folgen einer Equal-Power-Kurve; dadurch
  bleibt die wahrgenommene Energie in der Mitte erhalten. Gleiche Rollen starten weiterhin nicht
  neu. Wird die Lage waehrend der Ueberblendung erneut gewechselt, beginnt der naechste Verlauf
  bei der gerade hoerbaren Lautstaerke statt wieder auf die Ziellautstaerke zu springen. Die
  Nutzerentscheidung und alle bisherigen Audio-Sperren bleiben unveraendert.
- **Aktivitaet statt Kulissenbehauptung:** Ein kuenftiger `SPORT`-Track steht nur dann vor dem
  Tagestrack, wenn die Figur am Sportplatz tatsaechlich `MOVE` ausfuehrt. Der Ort allein reicht
  nicht mehr. `DockScreen.currentTopic` existierte bereits und wird nur an den kleinen
  `MusicContext` durchgereicht; kein zweiter Aktivitaetszustand entsteht.
- **Abendliche Natur:** Park, Wald und Wiese folgen abends nun wie der bereits enthaltene Teich
  der ruhigen Abendrolle. Stadt, Strasse, Laden und Arbeitsplatz bleiben beim ausklingenden
  Tagestrack. Das entspricht der vorhandenen Weltsemantik: Wald, Wiese und Teich lassen keine
  zufaelligen Besucher zu und sind Rueckzugsorte; die Stadt und Strasse sind Wege und Begegnungs-
  raeume.
- **Nicht stillschweigend geloest:** Die beiden erzeugten Ogg-Dateien wurden nicht nachbearbeitet.
  Die technische Analyse ergab rund 6 LU Lautheitsunterschied, einen True Peak ueber 0 dB beim
  Tagestrack sowie lange leise Passagen und keine belegte nahtlose Loop-Grenze beim Abendtrack.
  Weil generierte Binaerdateien nur ueber ihren vorgesehenen Prozess veraendert werden duerfen,
  bleiben Normalisierung, Peak-Grenze, Stille- und Loop-Pruefung ein eigenes Pipeline-Paket in
  NT-055. Ebenso offen bleiben mehrere Varianten je Rolle, ein gemeinsames Leitmotiv, die
  aesthetische Grundsatzfrage und die Lizenzpruefung.

### 2026-09-05 - Draussen wird ein Aufenthalt statt eines Durchgangs

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Reine Verlaengerung vorhandener Ablaeufe -
  keine neue Requisite, keine neue Animation, keine neue Regel. Ruecknehmbar Zeile fuer Zeile.
- **BEOBACHTET:** Beim Zusehen wechselte die Musik hoerbar, als die Figur nach draussen ging - und
  wechselte Sekunden spaeter zurueck. Gemeldet als "die Zeit draussen ist viel zu kurz, man sieht
  fast nur drinnen, und draussen macht er nichts ausser zum Laden zu laufen".
- **URSACHE, belegbar:** Der Einkaufsablauf in `PlayRoutines.allFor(DRINK)` enthielt **kein
  einziges `RoutineStep.Linger`**. Jeder Schritt ging unmittelbar in den naechsten ueber; der Weg
  zum Laden bestand aus einem `Stroll`, der Laden aus Regal und Kasse im Vorbeigehen, und aus dem
  Laden fuehrte der Ablauf ohne Rueckweg direkt in die Kueche. Der Arbeitsweg war derselbe Fall in
  kleiner: ein `Stroll` je Richtung, danach Schnitt.
- **Warum der vorhandene Test das nicht gefunden hat:** `der Avatar kommt regelmaessig nach
  draussen` zaehlt, WIE OFT eine Regung unter freien Himmel fuehrt, und war die ganze Zeit gruen.
  Die Beobachtung betraf die DAUER, und dafuer gab es keine Zusicherung. Der neue Test
  `wer nach draussen geht, bleibt auch eine Weile draussen` summiert je Ablauf die Verweilzeit,
  die anfaellt, waehrend der mitgefuehrte Ort draussen liegt, und verlangt mindestens zwoelf
  Sekunden. Zwoelf ist kein runder Wert, sondern der Abstand zum jetzt kuerzesten Aufenthalt
  (Arbeitsweg, vierzehn Sekunden).
- **Spielerwirkung:** Der Einkauf nimmt sich Zeit - stehen bleiben auf dem Hinweg, Suchen am
  Regal, Ueberlegen mit dem Gefundenen in der Hand, Warten an der Kasse - und geht ueber die
  Strasse zurueck statt aus dem Laden in die Kueche zu springen. Arbeitswege haben Hin- und
  Rueckweg mit Aufenthalt. Spaziergaenge, Wald, Wiese und der Block sitzen laenger auf der Bank.
  Musizieren im Park und Malen auf der Wiese dauern etwa doppelt so lang. Der kuerzeste Aufenthalt
  draussen liegt bei 14 s, der laengste bei 45 s (Drachensteigen, unveraendert).
- **Hoerbare Nebenwirkung, beabsichtigt:** Weil die Musikauswahl am Ort haengt, hoert der
  Rollenwechsel damit auf, ein Aufblitzen zu sein. Der Tagestrack laeuft draussen jetzt lang
  genug, um als Stueck wahrgenommen zu werden, statt in die Ueberblendung zurueckzufallen.
- **NICHT geloest, ausdruecklich:** Zwei Punkte derselben Meldung bleiben offen und sind als
  NT-056 und NT-057 eingetragen. (a) Waehrend eines Ablaufs kann **kein** Besuch stattfinden:
  `visitPossible()` in `DockScreen.kt` verlangt `!routineRunning`. Da die Figur ausschliesslich
  innerhalb von Ablaeufen nach draussen kommt, ist "draussen jemanden treffen" derzeit strukturell
  ausgeschlossen - laengere Ablaeufe verschieben Besuche sogar noch weiter nach hinten. (b) Eine
  Mindestdauer fuer den dynamischen Zustand selbst gibt es weiterhin nicht; ob sie zur Musik oder
  zum Zustand gehoert, ist eine offene Entwurfsfrage und soll erst nach der Messung dieser
  Aenderung am Geraet beantwortet werden.
- **Nebenbei:** `tools/reaction-preview/tests.sh` fuehrt `PlayRoutineTest` jetzt mit aus - 149
  statt 129 Tests, weiterhin unter einer Sekunde und ohne Gradle.

### 2026-09-05 - Draussen kann man jetzt jemandem begegnen

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Eine Bedingung wird herausgeloest und um
  einen Fall erweitert; keine neue Figur, kein neues Ereignis, keine neue Animation. Der Besuch
  selbst (`runVisit`) ist unveraendert.
- **BEOBACHTET / BELEGT:** Gewuenscht war, dass die Figur draussen auch mal jemanden trifft. Das
  war **strukturell ausgeschlossen**: `visitPossible()` verlangte `!routineRunning`, und unter
  freien Himmel kommt die Figur ausschliesslich INNERHALB eines Ablaufs. Uebrig blieb der schmale
  Rest zwischen zwei Ablaeufen - und auch der nur, wenn der letzte zufaellig an einem Ort endete,
  an dem man Leute trifft (Wald und Wiese lassen keine Besucher zu). Die laengeren Aussenphasen
  vom selben Tag haetten die Lage sogar verschlechtert: Sie verschieben jeden Besuch weiter nach
  hinten.
- **Warum das niemandem aufgefallen ist:** Der Fehler war nur an einer AUSBLEIBENDEN Sache zu
  bemerken. Ein Besuch kommt ohnehin nur alle anderthalb bis dreieinhalb Minuten in Frage; ob er
  ausblieb, weil die Regel ihn verbot oder weil der Wuerfel anders fiel, sieht beim Zusehen
  identisch aus.
- **Die Aenderung:** Ein laufender Ablauf sperrt einen Besuch weiterhin - mit genau einer
  Ausnahme, dem `RoutineStep.Linger` unter freiem Himmel. Das ist die eine Stelle, an der der
  urspruengliche Einwand ("der Gast streitet sich mit dem Ablauf um dieselbe Figur") nicht greift,
  weil die Figur dort nichts vorhat ausser dazustehen. Damit sie dem Gast nicht nach zwei Sekunden
  davonlaeuft, **wartet der Ablauf nach dem Linger auf das Ende des Besuchs**. Drinnen bleibt
  alles wie zuvor; die Ortsregel (`PlayScene.allowsVisitors`) bleibt uneingeschraenkt vorrangig.
- **Aus dem Unpruefbaren ins Pruefbare geholt:** Die Bedingung stand als lokale Funktion mitten in
  einer Compose-Funktion und war nur am Geraet zu beobachten. Sie ist jetzt
  `PlayVisitWindow.isOpen` - reine Wahrheitswerte, dieselbe Trennung wie zwischen `MusicResolver`
  und `PlayMusic`: Die Entscheidung wandert heraus, die Wiedergabe bleibt. Sechs neue Tests halten
  fest, dass das Fenster draussen aufgeht, drinnen nicht, die Ortsregel nicht aushebelt und dass
  jede einzelne Sperre (sitzt, geht, setzt sich, nicht im Bild, offene Erinnerung) es wieder
  schliesst. `tests.sh`: 155 statt 149.
- **NICHT geloest, ausdruecklich:** Wer draussen auf einer BANK sitzt, bekommt weiterhin keinen
  Besuch - `occupied` sperrt unveraendert. Ausgerechnet dort liegen die laengsten Aussenpausen
  (zwoelf bis sechzehn Sekunden). "Wer sitzt, faellt heraus" war aber eine bewusste Entscheidung
  des Besuchstakts und wird nicht nebenbei umgedreht; der Punkt bleibt als NT-056-Rest notiert.
- **Grenze der Pruefung:** Das Zusammenspiel der beiden Flags ist Compose-Verhalten und offline
  nicht pruefbar. Geprueft ist die REGEL, nicht der Ablauf um sie herum. Der Beleg muss am Geraet
  erfolgen - am ehesten daran, dass waehrend eines Aufenthalts auf der Strasse jemand vorbeikommt
  und die Figur ihm nicht mitten im Gruss davonlaeuft.
