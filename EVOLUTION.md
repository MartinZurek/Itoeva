# Itoeva Evolution Protocol

Version: 0.6 - seit 2026-09-06 mit dem langfristigen Betriebsziel eigener echter
24/7-Charakterinstanzen, die auf YouTube Live und Twitch ausgespielt werden und später
serverseitig unabhängig von Martins Geräten laufen sollen. Der nächste freigegebene Schritt ist
nur ein begrenzter Emulator-plus-OBS-PoC; Cloud-Anbieter, Produktionslaufzeit, Multi-Instanz-
Orchestrierung und Zuschauerintegration bleiben offen. Version 0.5 - seit 2026-09-03 darf der
Merge eines fertigen Pull Requests von einer beauftragten Agentensitzung ausgeführt werden; der
unbeaufsichtigte Lauf mergt sein eigenes Ergebnis weiterhin nie (siehe Evolution History zum
2026-09-03, "Merge-Freigabe"). Version 0.4 - seit 2026-09-03 mit der strategischen Zielidentität
einer öffentlich beobachtbaren Evolutionswelt. Die lokale persönliche App, davon getrennte
öffentliche Charakter-Streams und ein von den Avataren erzähltes YouTube-Evolutionstagebuch
sollen verschiedene Perspektiven auf dasselbe gestalterische Experiment eröffnen. Zuschauer
dürfen die öffentliche Welt künftig nur durch begrenzte Impulse beeinflussen, nicht private oder
medizinische Reminder steuern. Der kontrollierte Tagesablauf-Dauerauftrag aus Version 0.3
(2026-09-02), die enge erzählerische Autonomie aus Version 0.2 (2026-08-18) und alle
Sicherheitsgrenzen gelten fort.

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
2. Jedes Wesen soll langfristig eine **eigene öffentliche 24/7-Charakterinstanz** besitzen. Diese
   echte laufende Itoeva-Instanz wird gleichzeitig für YouTube Live und Twitch ausgespielt und
   führt mit Schlaf, Essen, Sport, Ausflügen und Musikwechseln ihr virtuelles Leben statt eine
   Videoschleife abzuspielen. Der Betrieb soll später serverseitig und unabhängig von Martins PC
   oder Smartphone möglich sein. Zuschauer dürfen künftig nur über klar begrenzte
   Berechtigungen zulässige Weltimpulse geben.
3. Ein **YouTube-Evolutionstagebuch** soll zusätzlich zum dauerhaften YouTube-Livestream möglichst
   von den Avataren selbst als Protagonisten erzählen lassen, was sich täglich tatsächlich
   verändert hat, was sie erlebt haben und welche Grenze der Evolution sichtbar wurde. Kürzere
   Highlight-Formate dürfen auf denselben belegten Ereignissen aufbauen.

Entschieden sind die gestalterische Richtung, getrennte echte Instanzen je Charakter,
Zielausspielung auf YouTube Live und Twitch sowie langfristig ein serverseitiger Betrieb ohne
Martins Geräte. Der nächste freigegebene technische Schritt ist ausschließlich ein begrenzter
PoC aus einer vorhandenen `:app-sim`-Instanz im Android-Emulator und OBS. Cloud-Anbieter,
Produktionslaufzeit, Encoder-/Relay-Topologie, Identität, Zahlung, Moderation, Kontingente,
Cooldowns, Video- und Stimmerzeugung, Veröffentlichungsrhythmus sowie Rechte- und weitere
Plattformfragen bleiben `OPEN DECISION` und kein Auftrag an einen autonomen Lauf.

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

### Living Agent System (entschieden 2026-09-10)

Der Produktverantwortliche hat ein kleines, erweiterbares Living Agent System als naechsten
groesseren Architektur-Meilenstein freigegeben. Voller Umfang und PR-Grenzen stehen in
[LIVING_AGENT.md](LIVING_AGENT.md).

- Geschichten entstehen aus Beduerfnissen, Weltbedingungen, Zielen, Erinnerungen,
  Persoenlichkeit, Beziehungen und Folgen. Ein `StoryManager`, Plot-Skripte oder vorgefertigte
  Gespraechsfolgen sind nicht Teil der Freigabe.
- Der erste Schnitt ist deterministisch und inspizierbar. Dringende Grundbeduerfnisse duerfen
  Freizeit verdraengen; bei gedeckten Grundlagen schaffen Utility-Regeln Raum fuer Spiel, Musik,
  Neugier, soziale Naehe und Entwicklung.
- Ziele und konkrete Handlungen sind getrennt. Plaene duerfen scheitern, unterbrochen und gegen
  den aktuellen Weltzustand neu gebildet werden.
- Spezies und bestehende Persoenlichkeit liefern Startbias, keine unveraenderliche
  Verhaltensvorschrift. Bedeutungsvolle Erfahrungen duerfen Praeferenzen und Beziehungen langsam
  veraendern.
- Kommunikation ist intern typisiert und sprachunabhaengig. Darstellung als Icon, Emoji oder
  Pixelsymbol ist eine spaetere UI-Aufgabe; ungepruefter Freitext bleibt ausgeschlossen.
- Bedeutungsvolle Simulationsereignisse sind die Wahrheit fuer spaetere Rueckblicke und
  Stream-Praesentation. Eine Beobachterschicht darf Muster erkennen, aber nichts erfinden oder
  steuern.
- Der lokale private Begleiterstand bleibt von kuenftigen oeffentlichen Stream-Instanzen getrennt.
  Diese Entscheidung gibt keine Cloud-, Konto-, Netzwerk-, Zahlungs- oder Plattformintegration
  frei.
- Umsetzung erfolgt in kleinen PRs: reiner Kern, profilbezogene Persistenz,
  vorhandene-Routinen-Adapter, dann read-only Stream-Vertrag.

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
- Produktionsreife Twitch-/YouTube-, Streaming-, Cloud-, Netzwerk-, Konto-, Zahlungs-,
  Moderations- und Medienerzeugungsarchitektur. Die menschliche Entscheidung vom 2026-09-06
  erlaubt nur das dokumentierte Zielbild und den begrenzten Emulator-plus-OBS-PoC, nicht dessen
  stillschweigende Ausweitung auf Cloud- oder Zuschauerfunktionen.
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

Münzen und Vorrat sind fuer den freigegebenen Living-Agent-Schnitt als reale
Weltbedingungen entschieden: Der Agent darf daraus Arbeit, Einkauf und Essen planen. **OPEN
DECISION** bleibt, ob daraus langfristig eine sichtbare, groessere Spieloekonomie mit weiteren
Guetern, Preisen oder Progressionsfolgen wird.

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

### 2026-09-04 - Spezies-Signatur faerbt jetzt auch die autonome Zwischen-Regung

- **Version:** Protokoll bleibt 0.5. Reine Verhaltensaenderung im Spielmodus ohne Datenmodell-,
  Schema- oder Preference-Aenderung; Ruecksetzweg ist der Revert dieses PRs, ohne Nutzerdatenfolge.
- **Ausgangsproblem und Nutzerwirkung:** `PlayGamePlan` gewichtet die ECHTEN Erinnerungs-
  Ausloesungen laengst nach Avatar-Charakter (Hootlet liest gern, Wyrmling bewegt sich gern) -
  `PlayAmbientActivity`, die autonome Zwischen-Regung zwischen zwei solchen Ausloesungen, tat das
  ausdruecklich noch nicht (`DOCUMENTED INTENT` im eigenen Klassendoc: "Bewusst noch OHNE
  Spezies-Charakter in der Gewichtung"). Dadurch liefen alle sechs Wesen beim taeglichen
  Zwischendurch-Verhalten identisch, solange noch kein ueber Wochen erworbener Entwicklungspfad
  (`PlayPath`/`leaning`) entstanden war - beim frischen Auswaehlen eines Avatars und in den ersten
  Tagen also praktisch immer. Fuer einen normalen Besuch bedeutet das: Wer einen Fennec waehlt,
  sieht ihn beim Zuschauen ab sofort haeufiger trinken; wer einen Hootlet waehlt, sieht ihn
  haeufiger fokussiert arbeiten - derselbe dokumentierte Charakterzug, den `AvatarSpecies` und
  `PlayGamePlan` ohnehin schon kennen, wird jetzt auch dort sichtbar, wo der Avatar die meiste Zeit
  tatsaechlich verbringt.
- **Evidenzklassifikation:** `DOCUMENTED INTENT` fuer die Luecke selbst (siehe Klassendoc-Zitat
  oben) plus `TESTED BEHAVIOR` fuer die Wirkung: ein neuer Test zeigt, dass die Signatur die
  Trefferquote ihres Themas erhoeht, ohne den Tag zu uebernehmen (analog zum bestehenden Test fuer
  `leaning`), ein zweiter, dass sie kein Thema ausserhalb der Tagesphase einfuehrt (Nachtruhe bleibt
  unangetastet), ein dritter, dass zwei Spezies mit gleichem Kontext (selbe Uhrzeit, kein
  Verweilen, keine offene Gewohnheit, kein Pfad) messbar unterschiedliche Themenverteilungen
  zeigen.
- **Getroffene Produktentscheidung:** `nextTopic`/`combinedWeights` erhalten `signatureTopic` als
  sechstes, additives Signal - nach demselben Andock-Muster wie `boostedTopics`, `leaning`,
  `stayAt`, `plannedTopic` und `justPlayed`. `SIGNATURE_BONUS = 2`, bewusst gleich gross wie
  `LEANING_BONUS`: beide sagen "das mag er von Natur aus", nur mit anderem Ursprung (Spezies von
  Geburt an, Pfad erst nach Wochen) - keines soll das andere uebertoenen, sobald beide gleichzeitig
  gelten. Wie bei `leaning` wirkt der Bonus nur auf ein Thema, das in der jeweiligen Tagesphase
  ohnehin ein Grundgewicht hat - er faerbt, erfindet aber nichts. `DockScreen` reicht dafuer
  `avatar.species.signatureTopic` durch, das bereits vorhandene Feld aus `AvatarSpecies` - kein
  neuer Zustand, keine neue Persistenz.
- **Verworfene Alternativen:** Den vollen `PlayGamePlan.topicWeights` (mehrstufig nach Level)
  ebenfalls in die Zwischen-Regung zu uebernehmen wurde verworfen - das waere ein deutlich groesserer
  Eingriff (Level-Abfrage, Stufenzuordnung, doppelte Quelle fuer denselben Charakterzug) fuer
  denselben sichtbaren Effekt, den die einzelne `signatureTopic` schon liefert. Das Zusammenlegen
  von `signatureTopic` in dieselbe `leaning`-Menge (statt eines eigenen Parameters) wurde verworfen,
  weil `leaning` dokumentiert ausschliesslich die ueber Wochen ERWORBENE Pfad-Neigung meint - ein
  eigener, klar benannter Parameter haelt beide Herkuenfte unterscheidbar und einzeln testbar.
- **Betroffene Bereiche:** `matrix/PlayAmbientActivity.kt` (neuer Parameter `signatureTopic`, neue
  Konstante `SIGNATURE_BONUS`, erweiterte `combinedWeights`, aktualisierte Klassendoku),
  `ui/DockScreen.kt` (reicht `species.signatureTopic` bei der PERFORM-Regung durch),
  `matrix/PlayVarietyTest.kt` (drei neue Tests).
- **Reminder-Semantik, Datenmodell, Migration:** unveraendert. Keine Room-, Preference- oder
  Textaenderung, keine Auswirkung auf bestehende Nutzerstaende.
- **Geschuetzte Grenzen:** `MEDICINE` bleibt ausgeschlossen (kein `AvatarSpecies.signatureTopic` ist
  je MEDICINE, zusaetzlich defensiv aus dem Signal gefiltert wie bei `boostedTopics`/`plannedTopic`).
  Die Nachtruhe-Garantie bleibt unangetastet, weil der Bonus nur auf ein Thema wirkt, das in
  `weightsFor(phase)` bereits ein Grundgewicht hat (nachts nur SLEEP) - mit eigenem Test
  abgesichert. Keine geschuetzte Charaktereigenschaft wurde umgedeutet: `signatureTopic` bestand
  bereits und ist unveraendert aus `AvatarSpecies.personalityRes` abgeleitet.
- **Ausgefuehrte Tests:** Drei neue Tests in `PlayVarietyTest`: Faerbewirkung ohne Uebernahme des
  Tages, kein phasenfremdes Thema durch die Signatur, messbarer Unterschied zwischen zwei Spezies
  im gleichen Kontext.
- **Ausstehende Geraeteprueung:** Ob der Unterschied zwischen zwei Spezies beim Zuschauen auffaellt,
  laesst sich nur am Geraet beurteilen und steht noch aus. Die Aenderung wurde im automatisierten,
  unbeaufsichtigten Lauf nicht lokal gebaut (kein Werkzeug dafuer im Lauf); den Nachweis fuehrt die
  CI.
- **Weiterhin offen:** Ob eine Abfolge NAHE verwandter Themen (z. B. FOCUS direkt nach WORK)
  repetitiv wirkt, bleibt unbelegt und damit weiterhin offen (siehe Lernjournal). Der naechste
  sinnvolle Hebel ist dort vermerkt.

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

### 2026-09-05 - Schlaf bleibt im Bett; Tageserlebnisse werden zu seltenen Traeumen

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. `BEOBACHTET`: Im bisherigen Nachtablauf
  enthielt `AnimationType.SLEEP` absichtlich Varianten mit `Rise -> LOOK_AROUND -> Occupy(BED)`;
  dadurch stand das Wesen nachts wiederholt auf. `ABGELEITET`: Weil jede Schlafroutine danach
  endete, konnte die normale Ambient-Schleife erneut FIDGET/WANDER/PERFORM waehlen. `ENTSCHIEDEN`:
  Nach dem Hinlegen ist Schlaf nachts ein exklusiver Zustand bis zum Morgen; Traeume sind eine
  visuelle Projektion darueber und bewegen den realen Avatar nicht.
- **Architekturentscheidung:** Kein zweiter Avatar- oder Weltzustand. `PlayRoutine` bekommt nur den
  Gate-Schritt `SleepUntilMorning`; `DockScreen.runRoutine` bleibt waehrend der Nacht aktiv und
  blockiert damit die bereits vorhandene autonome Schleife. Die normale Avatar-Idle-Schleife wird
  am Gate angehalten, damit die Schlafpose nicht von offenen Augen/Fidgets ueberschrieben wird.
  Traumsequenzen verwenden vorhandene `AvatarAnimations.reactionFor`-Frames in `PlayDreamBubble`.
- **Erinnerungsmodell:** `PlayDreamMemory` speichert hoechstens zwoelf semantische
  `AnimationType`-Erlebnisse je Begleiter und simuliertem Tag. Es speichert keine Screenshots,
  Videos oder zweite Weltkopie. Erfasst wird einmal am gemeinsamen `runRoutine`-Eingang, damit
  auch spezialisierte Football-/Training-/Music-/Painting-/Fishing-/Kite-Routinen beruecksichtigt
  werden. `SLEEP` und `MEDICINE` bleiben ausgeschlossen.
- **Zeitsemantik:** Tagesschluessel kommen aus `PlayTimeLapse.dayKey()`. OFF folgt dem echten
  Kalenderdatum; FAST/TURBO zaehlen jeden simulierten 24h-Zyklus getrennt. Damit vermischen
  beschleunigte Testtage ihre Traumerinnerungen nicht.
- **Persistenz / Migration:** Keine Room-Migration. Neu sind SharedPreferences unter
  `play_dream_memory`; Schluessel sind mit der bestehenden `presenceProfileId` namespaced. Es gibt
  keine Alt-Daten aus einem Release zu migrieren. Unscoped Schluessel aus dem unveroeffentlichten
  ersten PR-Entwurf werden nicht mehr gelesen und sind damit harmlos verwaist.
- **Rollback:** `SleepUntilMorning`, `PlayDreamMemory`/`PlayDreams` und `PlayDreamBubble` koennen
  gemeinsam entfernt und die vorherigen SLEEP-Routinen wiederhergestellt werden; keine Room-Daten
  oder externen Formate muessen zurueckmigriert werden. Die Preference-Datei kann beim Rollback
  liegenbleiben, da kein anderer Pfad sie liest.
- **Tests / CI:** `SleepRoutineTest` schuetzt genau eine Schlafroutine sowie "kein Rise/LookAround
  zwischen Bettbelegung und Morgen-Gate". `PlayDreamsTest` schuetzt die Auswahlregeln. Der PR muss
  zusaetzlich den bestehenden `gradlew verify`, API-26-/API-35-Instrumentierung und Release-Gate
  bestehen.
- **Weiter offen:** Die erste Erinnerung ist absichtlich nur grob (`MOVE` statt z. B. konkretem
  Dribbling). Reichere Ereignis-IDs, mehrere verfremdete Ausschnitte pro Traum und das gewuenschte
  Luftballon-Easter-Egg bleiben `OPEN DECISION`/Folgeschnitte. Fuer den Ballon wird kein paralleles
  Item-System erfunden; er wird erst an einen echten Ballon-Node/Gegenstand angeschlossen.

### 2026-09-06 - Die naechtlichen Pull Requests koennen endlich selbst gepruefte Arbeit abliefern

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Prozessaenderung, kein Anwendungscode. Eine
  Token-Quelle wird ausgetauscht; der Ablauf davor und danach bleibt Zeile fuer Zeile derselbe.
- **BEOBACHTET:** Von `claude-primary-run.yml` eroeffnete Pull Requests trugen nie ein einziges
  gelaufenes Pruefergebnis. PR #72 lag vom 04.09. bis 06.09. so da, wurde konfliktbehaftet und
  musste am Ende von Hand auf einen anderen Branch gerettet werden (#94). #82, #85 und #88 traf
  dasselbe. Es war viermal umgangen worden, ohne die Ursache anzusehen.
- **URSACHE, belegt statt vermutet:** Auf dem Head von #72 lag genau ein Workflow-Lauf:
  `Verify | completed | action_required | event: pull_request | actor: github-actions[bot]` - mit
  **null Jobs**. Der PR wurde in Zeile 1885 mit `gh pr create` und
  `GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}` eroeffnet und gehoerte damit `github-actions[bot]`.
  GitHub laesst dessen Ereignisse nicht frei laufen; der Lauf wartete auf einen menschlichen Klick
  auf "Approve and run". Der Grund dafuer ist Rekursionsschutz - ein PR-erzeugender Workflow
  koennte sich sonst endlos selbst ausloesen.
- **Die Korrektur einer eigenen Fehlannahme:** Die Arbeit in diesen PRs war NICHT ungeprueft.
  `claude-primary-run.yml` fuehrt `./gradlew verify` (Zeile 933) und die instrumentierten
  Emulator-Suiten (989-1026) aus, BEVOR der PR entsteht. Die Pruefung fand statt - sie stand nur
  als Fliesstext in der PR-Beschreibung statt als Haekchen. Fliesstext ist kein Nachweis, aber die
  Unterstellung "ungeprueft" war ebenfalls falsch.
- **Umgesetzt:** Der PR wird mit `EVOLUTION_PR_TOKEN` eroeffnet - einem fein granulierten Token
  fuer nur dieses Repository mit `Contents` und `Pull requests` auf "Read and write". Damit gehoert
  der PR einem Menschen, die Rekursionssperre greift nicht, und die CI laeuft von allein an.
- **Der Rueckfall ist absichtlich weich, aber laut:** Fehlt das Secret, wird der PR weiterhin mit
  `GITHUB_TOKEN` eroeffnet - eine fertige Evolution zu verlieren waere schlimmer als ein wartender
  Lauf. Der Schritt setzt dann aber eine `::warning::`, die genau benennt, was passiert und wo zu
  klicken ist. Ein stiller Rueckfall haette denselben Fehler unsichtbar wiederhergestellt.
- **Warum nicht ueber die Repository-Einstellung:** Die Genehmigungspflicht unter
  Settings > Actions > General war der billigere Verdacht, liess sich aber nicht pruefen - die
  API-Pfade dorthin sind aus dieser Umgebung gesperrt -, und der Abschnitt heisst
  "fork pull request workflows", waehrend #72 aus demselben Repository kam. Der Token-Weg haengt
  nicht davon ab, wie GitHub intern Bots einordnet.
- **NICHT geloest:** Ob es wirkt, zeigt erst der naechste naechtliche Lauf. Der Beleg ist, dass
  "Verify" auf dem neuen PR von allein anlaeuft, statt bei `action_required` zu stehen. Bis dahin
  bleibt diese Aenderung eine begruendete Vermutung mit Rueckfallebene.

### 2026-09-06 - ITO-0015: Sichtbare Vielfalt im Tagesablauf und in der Musik

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Zwei getrennte Aenderungen mit einer
  gemeinsamen Ursache: Was es gibt, ist zu selten oder zu kurz zu sehen und zu hoeren.
- **BEOBACHTET:** Buchlesen unklar, der Drachen "schwach und schwer verstaendlich", Basketball
  beim normalen Zuschauen ueberhaupt nie sichtbar, viele Aktivitaeten so selten, dass die
  Vielfalt nicht wahrnehmbar ist - und innerhalb eines stabilen Zustands dieselbe Musik zu lange.
- **URSACHE, gerechnet statt vermutet:** Keine der drei gemeldeten Aktivitaeten ist gesperrt.
  Mittags hat MOVE Gewicht 3 von 16, davon fuehren 70 % zu einer Sonderaktivitaet, und die wird
  unter fuenf gleichberechtigten gezogen: rund 2,6 % je Ablauf. Bei einem Regungstakt von 18 bis
  36 Sekunden und `PERFORM` in der Haelfte der Faelle laufen in fuenf Minuten etwa drei Ablaeufe -
  knapp 8 % Chance auf Basketball. Buchlesen liegt aehnlich. Sie sind zu selten, um bemerkt zu
  werden, nicht unerreichbar.
- **Warum kein pauschaler Zuschlag:** Basketball zu bevorzugen haette nur die naechste Aktivitaet
  unsichtbar gemacht. Stattdessen zwei Verlaufserinnerungen, beide nur im Arbeitsspeicher:
  `recentTopics` gegen dasselbe Thema, `recentSpecials` gegen dieselbe der fuenf
  Sonderaktivitaeten - die teilen sich alle das Thema MOVE und waeren auf Themenebene
  ununterscheidbar. Der Zuschlag ist bewusst der kleinste im Haus: +1 fuer lange nicht Gezeigtes,
  -2 je Vorkommen im Fenster von vier. Abwechslung ist ein Tiebreaker, kein Taktgeber.
- **Die Grenzen, die bleiben:** Nichts wird aus dem Pool geworfen, nachts bleibt SLEEP das
  einzige Thema, MEDICINE bleibt aus autonomen Regungen heraus, eine laufende Routine wird nie
  fuer einen kuenstlichen Wechsel abgebrochen, und eine ausdrueckliche Bitte des Nutzers wird
  nicht auf Abwechslung getrimmt - wer zweimal dasselbe erbittet, bekommt zweimal dasselbe.
- **Animationen, vier belegte Ursachen beim Drachen:** ein fast durchgehend gefuellter Klumpen
  statt einer Raute; eine in Zweierschritten abgetastete und dadurch gepunktete Schnur; ein
  Schweif auf festen Versaetzen, der bei Wind stillstand; und `sin(...).roundToInt() * 2`, das von
  einer Sinuskurve genau drei Werte uebrig liess. Alle vier behoben.
- **Buch:** Dieselbe Lehre, die das Weltmotiv nebenan schon gezogen hatte - zwei parallele Rahmen
  lesen sich als zwei Steine, erst Diagonalen ergeben die Rundung. Das GEHALTENE Buch hatte sie
  nie mitbekommen und besass oben nicht einmal eine Kante. Dazu die Dauer: Die Reaktion lief in
  2 200 ms durch; darin nimmt man eine Bewegung wahr und hat das Buch verpasst.
- **Basketball:** Zeichnung und Phasen waren in Ordnung; die Ursache war ausschliesslich die
  Haeufigkeit. Geblieben war derselbe Fehler wie beim Drachen - der Ball prellte zwischen genau
  zwei Hoehen.
- **Musik, Produktentscheidung umgesetzt:** Eine Rolle ist kein Dateiname mehr. `MusicRole` traegt
  den Ressourcen-Stamm, die Dateien haengen eine zweistellige Nummer daran, und die App findet sie
  zur Laufzeit. Die ROLLE bestimmt weiterhin allein `MusicResolver` aus der Weltlage; erst danach
  waehlt `PlayMusicRotation` ein Stueck daraus. Zeitsteuerung und Variantenwahl sind reine
  Funktionen - am Geraet waere ein Wechsel nach fuenf Minuten kaum von Zufall zu unterscheiden.
- **Die Uhr gehoert der Rolle, nicht der Variante.** Sonst wuerde aus "spaetestens nach fuenf
  Minuten" ein "alle fuenf Minuten wieder von vorn", und das waere als Metronom hoerbar.
- **NICHT geloest, ausdruecklich:** Der Track `main-day-02` "Paper Bridges" ist als Prompt,
  Manifest-Eintrag und Code vollstaendig vorbereitet und validiert (`--dry-run` laeuft durch), die
  **Audiodatei selbst ist nicht erzeugt** - das braucht den Workflow `Generate Itoeva Music` mit
  Modellgewichten und HF_TOKEN. Bis dahin hat `main_day_background` genau ein Stueck, und die
  Rotation greift dort schlicht nicht; die Wiedergabe verhaelt sich exakt wie zuvor.
- **Weiterhin offen:** Lautheit, True Peak, Stille und Loop-Grenze des neuen Tracks sind erst nach
  seiner Erzeugung pruefbar. Die Lizenzfragen aus `music/README.md` bleiben unveraendert offen und
  werden hier nicht als geloest behauptet.


### 2026-09-06 - Eigene 24/7-Livestream-Instanz je Charakter als langfristiges Betriebsziel

- **Version:** Protokoll 0.5 → 0.6. Strategische Produkt- und Architekturdokumentation ohne
  Änderung an App-Laufzeit, Datenmodell, GitHub-Workflow oder externer Infrastruktur.
- **Ausgangsproblem und Nutzerwirkung:** Die 2026-09-03 beschlossene öffentliche Twitch-Welt
  benannte weder die Anzahl der laufenden Welten noch klar, ob YouTube nur Tagebuch oder auch
  Live-Ziel ist. Ebenso blieb offen, ob der Betrieb von Martins PC oder Smartphone abhängen darf.
  Dadurch hätten Agenten eine gemeinsame Welt, Videoschleifen oder lokale Dauerhardware als
  gleichwertige Zielbilder behandeln können.
- **Evidenzklassifikation:** `DOCUMENTED INTENT` für eigene echte 24/7-Instanzen je Charakter,
  YouTube Live plus Twitch und langfristig serverseitigen Betrieb. Dauerstabilität,
  Plattformbetrieb, Kosten und Skalierung sind `UNVERIFIED`; konkrete Produktionsarchitektur
  bleibt `OPEN DECISION`.
- **Getroffene Produktentscheidung:** Jeder veröffentlichte Itoeva-Charakter soll langfristig eine eigene fortlaufende
  öffentliche Itoeva-Instanz erhalten. Die Figur schläft, isst, treibt Sport,
  unternimmt Ausflüge und erlebt Musik- und Zustandswechsel aus laufender Weltlogik. Der Stream
  ist keine aufgezeichnete Schleife. Spätere Interaktionen zwischen Charakterinstanzen gehören
  zum Ziel, aber noch nicht zum ersten Schnitt.
- **Architektureinordnung:** `:app-sim` ist der Wiederverwendungsanker für den ersten Test:
  vorhandener Android-Emulator plus OBS statt neuer Renderer. Für Produktion werden
  Instanzisolation, Checkpoints, Prozessaufsicht, Frame-/Audio-Gesundheit, Secret-Verwaltung und
  Kostenmessung benötigt. Ob Emulatoren bleiben oder eine Headless-Weltengine entsteht, wird erst
  nach dem PoC entschieden.
- **Nächster freigegebener Schritt:** NT-058 nimmt genau eine `:app-sim`-Instanz mindestens zwei
  Stunden lokal über OBS auf und dokumentiert Stabilität, Vielfalt, Musik, Bildformat,
  Ressourcenverbrauch und Wiederanlauf. Kein Cloud-Deployment, keine Sechser-Flotte, keine
  Zuschauersteuerung und keine neuen Konten in diesem Schritt.
- **Trennung und Datenschutz:** Öffentliche Instanzen verwenden ausschließlich erfundene
  öffentliche Zustände. Persönliche Reminder, lokale Historien, private Avatarstände und
  `MEDICINE` werden weder gelesen noch übertragen. Die persönliche App bleibt lokal und ohne
  Konto oder Cloud.
- **App-Hinweis und Plattformen:** Gewünscht sind zunächst ein sachlicher Android-App-Link und
  eine kurze Erklärung in den Kanal-/Streambeschreibungen. Aggressive Werbung und ein
  plattformübergreifend zusammengeführter Chat sind nicht beschlossen. Vor öffentlicher
  Ausspielung werden aktuelle YouTube-/Twitch-Regeln, Musikrechte, KI-Transparenz und Moderation
  als eigenes Gate geprüft.
- **Roadmap:** NT-059 entscheidet anhand der PoC-Messwerte über den nächsten Laufzeitschnitt;
  NT-060 begrenzt einen ersten Cloud-Test auf einen Charakter; NT-061 behandelt erst danach
  parametrisierte Instanzen und typisierte Begegnungsereignisse; NT-062 ist das Plattform- und
  Rechte-Gate vor öffentlichem Simulcast.
- **Daten, Migration und Rücksetzweg:** Keine App-Daten, Room-Schemas, Preferences, Secrets oder
  Migrationen betroffen. Rücksetzweg ist ein gewöhnlicher Revert dieses Dokumentations-PRs.
- **Tests:** Dokumentstruktur, Links, Task-Anzahl und `OPEN DECISION`-Grenzen werden im Diff
  geprüft. Android- und Emulator-Tests sind für diesen reinen Planungsstand nicht einschlägig.
- **Weiter offen:** Cloud-Anbieter, Region, SLA, Kostenobergrenze, Encoder/Relay, Zeitmodell,
  Checkpoint-Format, Anzahl Streams pro Host, Kanalstruktur, Stream-Key-Lebenszyklus, konkrete
  Interaktionen, Moderation, Monetarisierung, Musik-/Medienrechte und der Zeitpunkt einer
  öffentlichen Ausspielung.

### 2026-09-06 - NT-057: Die Mindestdauer gehoert dem Zustand, nicht der Musik

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Eine neue Regel in der autonomen Themenwahl;
  Musik, Player und Resolver bleiben unangetastet.
- **BEOBACHTET:** Gemeldet am 2026-09-05 zusammen mit den zu kurzen Aussenphasen - "der Wechsel
  war viel zu schnell, dann wieder in diese ruhige Stimmung ... die dynamische Zeit sollte auch
  eine Mindestdauer haben".
- **Die Entwurfsfrage, jetzt entschieden:** Naheliegend waere eine Regel im Player gewesen - ein
  Track laeuft mindestens X. Das waere die Behandlung des Symptoms: Die Musik wechselte richtig,
  sie folgte nur einer Welt, in der die Figur nach zwanzig Sekunden wieder hineinging. Eine
  Player-Regel haette ausserdem die schlechtere Eigenschaft gehabt, Ton und Bild zu entkoppeln -
  Musik fuer draussen, waehrend die Figur schon am Kuehlschrank steht.
- **Umgesetzt:** `PlayOutdoorStay.holdsOutdoors` haelt einen Aufenthalt unter freiem Himmel
  mindestens neunzig Sekunden - ungefaehr drei Regungen. Die Musik tut daraufhin von selbst das
  Richtige, ohne eine zweite Zeitregel zu kennen.
- **Die einzige Regel in `nextTopic`, die AUSWAEHLT statt zu gewichten.** Alle sieben Signale
  davor verschieben Wahrscheinlichkeiten; eine Mindestdauer, die sich fortwuerfeln laesst, ist
  aber keine. Sie steht deshalb ganz am Ende und filtert.
- **Vier Ausnahmen, alle absichtlich:** Nachts greift sie nie - ein Wesen, das um drei Uhr auf der
  Strasse festgehalten wird, weil es dort die Tageszeit gewechselt hat, waere ein schlimmerer
  Fehler als der behobene. Gibt es zur Tageszeit gar kein Aussenthema, gewinnt die Tageszeit.
  Echte Erinnerung, ausdrueckliche Bitte des Nutzers und der Arbeitszwang bei leerem Vorrat laufen
  ohnehin an `nextTopic` vorbei. Und MEDICINE bleibt auch hier ausgeschlossen.
- **Die Uhr laeuft nicht neu bei jedem Schritt:** Der Wechsel von der Strasse in den Park setzt
  sie nicht zurueck - sonst wuerde aus "mindestens neunzig Sekunden" ein "nie wieder hinein". Sie
  haengt am Uebergang drinnen/draussen, nicht am einzelnen Ort, und lebt nur im Arbeitsspeicher.
- **NICHT belegt:** Ob neunzig Sekunden richtig sind. Das ist eine Zahl, sie ist am Geraet zu
  beurteilen, und sie ist eine Zeile. Acht neue Tests sichern die Regel und ihre Ausnahmen ab;
  keiner davon kann sagen, ob sich der Aufenthalt richtig anfuehlt.

### 2026-09-06 - NT-055 (a): Ein Freigabe-Gate fuer erzeugte Musik

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich an der
  Erzeugungspipeline; kein Anwendungscode, keine vorhandene Audiodatei angefasst.
- **BEOBACHTET, gemessen:** Alle drei ausgelieferten Tracks enden mit Stille und beginnen sofort
  auf vollem Pegel - `main-day-01` 0,50 s, `main-day-02` 0,62 s, `home-evening-01` 1,13 s. Die App
  loopt sie (`MediaPlayer.isLooping = true`), also faellt die Musik alle 90 Sekunden fuer knapp
  eine Sekunde aus und setzt hart wieder ein. Dazu ein True Peak von +0,68 bzw. +0,41 dBFS bei den
  beiden Tagestracks.
- **URSACHE, praezise:** Der Erzeuger klemmte mit `clamp(-1, 1)` auf Vollaussteuerung und gab das
  an einen **verlustbehafteten** Encoder. Vorbis rekonstruiert beim Dekodieren ueber seinen
  Eingang hinaus - ein Signal, das exakt bei 0 dBFS lag, dekodiert darueber. Das Klemmen war also
  nicht der Schutz, fuer den man es halten koennte, sondern die Ursache.
- **Was das ueber die bisherige Bewertung sagt:** Diese Maengel standen seit dem 2026-09-05 als
  NT-055 (a) im Backlog, und ich hatte sie zweimal als "offen, aber nicht neu" abgehakt. Das war
  richtig und trotzdem zu bequem: Ein Loch alle 90 Sekunden ist keine Feinheit, sondern genau die
  Sorte Stoerung, die als "nach fuenf Minuten wirkt der Track monoton" gemeldet wird.
- **Umgesetzt:** `tools/music/audio_polish.py` schneidet die Raender, faltet den Schluss ueber den
  Anfang - damit das Ende IN den Anfang fuehrt statt an ihn zu stossen - und setzt den Pegel auf
  −1 dBFS Kopfraum. Danach wird die **dekodierte** Datei gemessen und der Lauf scheitert bei
  jedem Befund. Die Messung nach dem Kodieren ist der Punkt: Im Speicher waere der Ueberschwinger
  unsichtbar geblieben, weil er erst beim Dekodieren entsteht.
- **Belegt:** Das Gate beanstandet alle drei vorhandenen Dateien; die Politur behebt jeden Befund
  (Peak −1,00 dBFS, Nahtsprung ~0, Restbefunde 0) und kostet dabei weniger als eine Sekunde
  Spielzeit. Zwoelf Tests in `tools/music/test_audio_polish.py`, in der CI ohne Modellgewichte.
- **NICHT geloest:** Die drei ausgelieferten Dateien sind unveraendert - erzeugte Binaerdateien
  aendern sich nur ueber ihren Prozess. Sie muessen neu erzeugt werden, und ob ein Stueck nach dem
  Schnitt noch dasselbe ist, entscheidet ein Mensch beim Hoeren.

### 2026-09-06 - Ein Pruefschritt, der seine eigene Voraussetzung mitbringt, prueft nichts

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich an der
  Erzeugungspipeline und ihren Tests; kein Anwendungscode, keine Audiodatei angefasst.
- **BEOBACHTET:** Der erste Erzeugungslauf nach dem Freigabe-Gate (Lauf 34031057429) starb nach
  sieben Sekunden am ersten Schritt `Validate track without model download` mit
  `ModuleNotFoundError: No module named 'numpy'`.
- **URSACHE:** Das Gate wurde in `generate_music.py` auf Modulebene importiert. Der Trockenlauf
  ist aber der erste Schritt des Workflows und laeuft absichtlich VOR jeder Installation - damit
  ein Tippfehler im Manifest in Sekunden auffaellt statt nach einem Modell-Download. Der Import
  hat genau diesen Zweck zerstoert.
- **Warum die CI es durchliess - das ist die eigentliche Lehre:** `verify-music-tooling.yml`
  installiert numpy, BEVOR sie den Trockenlauf aufruft. Dort war der Import immer erfuellbar. Ein
  Pruefschritt, der seine eigene Voraussetzung mitbringt, prueft die Voraussetzung nicht. Die
  Luecke war also nicht die falsch platzierte Zeile, sondern eine Pruefung, die eine andere
  Umgebung herstellt als die, die sie absichern soll.
- **Umgesetzt:** Der Import steht jetzt bei `torch` und `stable_audio_3`, hinter dem Ausstieg fuer
  `--dry-run`, mit einem Kommentar, der erklaert warum - damit ihn niemand nach oben "aufraeumt".
  Die Fehlermeldung nennt die tatsaechlich fehlende Abhaengigkeit.
- **Belegt:** `tools/music/test_dry_run.py` startet den Trockenlauf als Unterprozess, in dem
  numpy, soundfile, torch, torchaudio und stable_audio_3 durch Stubs unimportierbar sind - fuer
  jeden Track im Manifest, weil der Workflow mit beliebiger Id angestossen wird. Ein zweiter Test
  belegt, dass die Stubs wirklich greifen; ohne ihn waere die Absicherung bei einem Tippfehler im
  Stub-Namen stillschweigend wertlos. Gegenprobe: mit dem alten Import fallen 4 von 15 Tests.
- **Fuer den naechsten Lauf:** Eine Umgebung, die eine andere Umgebung absichern soll, muss deren
  Kargheit nachstellen, nicht die eigene Bequemlichkeit. Wo das nicht geht, gehoert der karge Fall
  in einen Test, der ihn herstellt.

### 2026-09-06 - Der Nachklang: eine beantwortete Erinnerung faerbt den Tag, statt spurlos zu enden

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Neue reine Funktion plus eine Abfrage; keine
  Aenderung an bestehendem Verhalten ausser der Gewichtung selbst.
- **BEOBACHTET, im Code belegt:** Eine beantwortete Erinnerung lief bisher so ab - kurze Reaktion,
  einmal an den zum Thema passenden Ort, dort EINE Routine (`requestedTopic` in `DockScreen`), und
  danach war sie spurlos. Der naechste Wurf wusste nichts mehr davon.
- **URSACHE, und sie ist schlimmer als "es fehlt etwas":** Die einzige Spur, die blieb, zeigte in
  die falsche Richtung. `PlayHabitSignal.underfulfilledTopics` gewichtet, was heute noch NICHT
  erreicht ist (+`HABIT_BOOST` = 4). Sobald das Tagesziel erfuellt war, fiel das Thema aus dem
  Zuschlag heraus. **Wer seine Erinnerung beantwortete, machte damit genau dieses Thema fuer den
  Rest des Tages seltener.** Der Nachklang war negativ.
- **Umgesetzt:** `PlayAfterglow` mit zwei Zeitskalen, weil zwei verschiedene Dinge gemeint sind.
  Der **Nachklang** (`ECHO_MS` = 3 min, `ECHO_BONUS` = 5, so gross wie `STAY_BONUS`) haelt etwa ein
  halbes Dutzend Regungen lang - aus der einen angeforderten Routine wird eine zusammenhaengende
  Weile. Danach bleibt die **Tagesfarbe** (`DAY_BONUS` = 2, so gross wie `LEANING_BONUS`) bis zum
  Tagesende. Dass eine heutige Bitte genauso schwer wiegt wie eine ueber Wochen gewachsene
  Neigung, ist die Aussage und kein fehlender Feinschliff.
- **Belegt, in Zahlen:** Mittags hat BOOK Grundgewicht 1 von 16 (6,3 %). Mit dem frischen
  Nachklang sind es 6 von 21 (28,6 %), mit der Tagesfarbe 3 von 18 (16,7 %). DRINK steigt von
  3/16 (18,8 %) auf 5/18 (27,8 %). Fuenfzehn neue Tests, Gesamtstand 226 in der Offline-Strecke;
  Gegenprobe gemacht: ohne die Verdrahtung fallen zwei davon.
- **Die wichtigste Grenze:** Der Nachklang darf kein Thema EINFUEHREN, nur verstaerken, was zur
  Tageszeit ohnehin vorkommt - dieselbe Zurueckhaltung wie bei der Neigung und anders als beim
  Stundenplan. Sonst haette eine nachmittags beantwortete Erinnerung das Wesen um drei Uhr nachts
  aus dem Bett geholt. Ein eigener Test prueft das ueber 2.000 Ziehungen.
- **NICHT gebaut, weil es das schon gibt:** Eine dritte Zeitskala fuer "aendert die ganze
  Geschichte". `PlayPath` leitet aus ALLEN je beantworteten Erinnerungen den Entwicklungspfad ab,
  faerbt darueber dauerhaft die Themenwahl und moebliert die Zimmer sichtbar
  (`PlayScene.Acquisition`). Das dort noch einmal zu bauen waere Verdopplung. Es braucht
  allerdings `MIN_FEEDS` = 20 Antworten und bewegt sich absichtlich langsam - wer den Effekt
  sucht, sieht ihn dort nicht nach einem Tag.
- **NICHT belegt:** Ob drei Minuten die richtige Laenge sind und ob 5 und 2 sich beim Zusehen
  richtig anfuehlen. Das sind drei Zahlen, sie sind am Geraet zu beurteilen. Die Tests sichern die
  Regeln und ihre Grenzen ab; keiner kann sagen, ob der Tag sich dadurch anders anfuehlt.

### 2026-09-08 - MOVE und GENERAL hatten keinen Moment zum Innehalten

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Geaendert wurden ausschliesslich Standzeiten;
  kein einziges Bild ist neu, keins entfaellt.
- **BEOBACHTET, gemessen statt geschaetzt:** Die Gesamtdauer der zwoelf Themen-Reaktionen reicht
  von 1 390 ms (GENERAL) bis 3 920 ms (BOOK) - Faktor 2,8. Entscheidend ist aber nicht die Summe,
  sondern die Mitte. Jede Reaktion endet mit 480 ms Ausklang; davor hielten **MOVE und GENERAL als
  einzige nirgends laenger als 140 ms**:

      MOVE      90,90,90,90,90,90,90,140,90,90,480    (9 von 11 Bildern auf 90 ms)
      GENERAL   90,90,90,90,90,140,140,90,90,480      (7 von 10)
      BOOK      140,90,620,620,90,90,620,620,...      (zwei Lese-Verweiler je Seite)

- **URSACHE:** 90 ms ist die kuerzeste Standzeit der Datei. Bei diesem Tempo loest das Auge die
  einzelnen Posen nicht auf - aus drei Spruengen wird ein Flimmern, aus einem Laeuten ein Klicken.
  Das ist gemeint, wenn eine Reaktion "zu kurz" wirkt: nicht die Dauer, das fehlende Verweilen.
- **Warum ausgerechnet diese beiden schwer wiegen:** MOVE ist mit Grundgewicht 5/3/4 das
  meistgesehene Thema des Tages. GENERAL ist die Rueckfall-Reaktion fuer JEDE Bibliotheks-
  Animation ohne eigenen Typ - wer sich etwas Eigenes gezeichnet hat, sah bisher genau dort am
  wenigsten.
- **Umgesetzt:** Jeder Sprungscheitel bekommt SLOW_MS, jede Landung BEAT_MS, der Abschlusssprung
  ein neues LEAP_MS (420 ms). Bei GENERAL bekommt der Moment "Kopf hoch, Mund auf" - der einzige,
  der "ich habe dich gehoert" sagt - dieselben 420 ms, statt so lang zu stehen wie eine
  Zwischenschwingung. MOVE 1 430 -> 2 420 ms, GENERAL 1 390 -> 2 160 ms; schnelle Bilder von 9/11
  auf 2/11 bzw. 7/10 auf 2/10, wobei die verbleibenden zwei je Reaktion der gemeinsame Abschluss
  sind.
- **Belegt:** `ReactionFingerprintTest` meldete GENAU zwei geaenderte Reaktionen und keine dritte -
  der Nachweis, dass nichts anderes mitgewandert ist. Dazu `ReactionDwellTest`: jede Reaktion jeder
  Spezies muss vor dem Ausklang irgendwo mindestens SLOW_MS verweilen. Gesamtstand 228 Tests.
- **Was ausdruecklich NICHT geprueft wird:** Eine Mindestdauer oder eine Angleichung der Laengen.
  BOOK darf dreimal so lang sein wie CREATIVITY - Lesen ist eine laengere Handlung als ein
  Einfall.
- **Ein Test, den ich wieder entfernt habe:** "keine Reaktion besteht ueberwiegend aus dem
  schnellsten Takt" faellt auch fuer FOCUS (6/8), WORK (8/11), MEDICINE (7/11), DRINK (10/17) und
  LOVE (5/11) - Reaktionen, die den eigentlichen Fehler gar nicht haben, sondern nur flott
  geschnitten sind. Einen Test zu schreiben und danach den Code an die eigene nachtraegliche
  Erfindung anzupassen waere die falsche Richtung. Die Zahlen stehen im KDoc von
  `ReactionDwellTest`, damit die naechste Sitzung sie nicht neu erheben muss; WORK waere der
  naechste Kandidat.
- **NICHT belegt:** Ob 2 420 ms fuer MOVE richtig sind. Das ist am Geraet zu beurteilen.

### 2026-09-08 - Ein geschlossener Pull Request legte die Evolutionsstrecke zwei Tage still

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich am Waechter in
  `claude-primary-run.yml`; kein Anwendungscode.
- **BEOBACHTET:** Fuenf Zeitplan-Laeufe in Folge (34100064301, 34136307577, 34154567283,
  34202740728, 34233269761) haben `Builder, Tests, Reviewer` uebersprungen. Alle fuenf melden
  `success` - das Ueberspringen ist ein vorgesehener Zustand, also faellt es niemandem auf. Erst
  ein von Hand angestossener Lauf (34263907769) hat es sichtbar gemacht.
- **URSACHE:** Der Waechter fragt, ob ein `claude-evolution/*`-Branch noch nicht in main enthalten
  ist, und prueft das ueber `compare/main...<branch>` mit `identical|behind` als "enthalten".
  `claude-evolution/33849938800-1` gehoert zu PR #72, der am 2026-09-06 **geschlossen statt
  gemergt** wurde; sein Inhalt kam ueber einen ANDEREN Commit nach main (PR #94, gerettet). Damit
  ist der Branch auf ewig `diverged` - und auf ewig eine "laufende Evolution".
- **Der Denkfehler, genau benannt:** "In main enthalten" ist nicht dasselbe wie
  `identical|behind`. Der Kommentar an der Stelle nahm bereits vorweg, dass Branches nie geloescht
  werden; uebersehen wurde der Fall, dass Arbeit ueber einen anderen Commit landet. Cherry-Pick,
  Rettung und Squash erzeugen ihn alle.
- **Umgesetzt:** Ein Branch, der nicht in main ist, blockiert nur noch dann, wenn zu ihm
  **ueberhaupt kein Pull Request existiert** - dann haengt er wirklich in der Luft (etwa weil ein
  Lauf den Branch pushen konnte, das Oeffnen des PR aber scheiterte). Ein geschlossener Pull
  Request heisst: Ein Mensch hat bereits entschieden, und genau darauf wartet der Waechter. Offene
  Pull Requests blockieren unveraendert ueber die Abfrage darunter.
- **Belegt, gegen das echte Repository:** Die neue Schleife wortgleich lokal gegen die GitHub-API
  ausgefuehrt - 14 Branches, der einzige `diverged` wird als "Pull Request vorhanden (closed) -
  bereits entschieden" eingestuft, `PENDING` bleibt leer, Ergebnis GO. Gegenprobe fuer die andere
  Richtung: Die Abfrage liefert fuer einen Branch ohne Pull Request eine leere Liste, er wuerde
  also weiterhin blockieren. Dazu `bash -n` ueber das aus dem YAML extrahierte Skript.
- **NICHT geloest:** Der Rest-Branch `claude-evolution/33849938800-1` existiert weiter. Loeschen
  war aus der Sitzung heraus nicht moeglich - der Git-Proxy bricht Lösch-Pushes ab und die
  REST-Route `git/refs` ist gesperrt (403). Nach dieser Aenderung stoert er allerdings nicht mehr.

### 2026-09-08 - Eine Diagnose, an die man nicht herankommt, ist keine

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich an der
  Diagnose-Ausgabe in `claude-primary-run.yml`; kein Anwendungscode, keine Gates beruehrt.
- **BEOBACHTET:** Die Builder-Sitzung von Lauf 34273049230 ist nach 11,4 Minuten mit Exitcode 1
  ausgestiegen. Die Ursache steht in `builder-diagnostics.json` (930 Bytes, also ein echter
  Envelope mit `subtype`, `num_turns` und `stop_reason`) - und war trotzdem nicht zu ermitteln:
  - Das Job-Protokoll ist ueber die API nur vom Ende her abrufbar; die Mitte, in der die Diagnose
    per `cat` steht, wird abgeschnitten.
  - Das Diagnose-Artefakt liegt auf `productionresultssa1.blob.core.windows.net`, und der
    Egress-Proxy dieser Umgebung lehnt die Verbindung ab (`connect_rejected`).
  - Die Annotation des fehlgeschlagenen Schritts lautete `Process completed with exit code 1.` -
    fuer jede Ursache dieselbe.
- **URSACHE:** Die Forensik war vollstaendig, aber nur mit Browser und Maus erreichbar. Fuer einen
  Lauf, dessen Sinn gerade darin besteht, unbeaufsichtigt zu arbeiten, ist das die falsche
  Zustellung.
- **Umgesetzt:** Builder- und Reviewer-Diagnose gehen zusaetzlich als `::notice`-Annotation
  heraus. Annotationen sind ueber `/check-runs/<id>/annotations` abrufbar, also genau ueber den
  Weg, der auch ohne Browser offensteht. Bewusst einzeilig und nur die strukturierten Felder
  (`subtype`, `is_error`, `num_turns`, `stop_reason`, `permission_denials`) - keine Modellausgabe
  und kein Freitext: Eine Annotation ist oeffentlich sichtbar und soll nichts weitertragen, was
  nicht ohnehin Metadaten sind.
- **Belegt:** `bash -n` ueber beide aus dem YAML extrahierten Skripte, YAML-Gueltigkeit, und die
  jq-Zeile mit einem echten Envelope sowie mit kaputtem JSON durchgespielt - im zweiten Fall
  faellt sie auf `{"note":"nicht auswertbar"}` zurueck statt eine leere Annotation zu erzeugen.
- **NICHT geloest:** Warum die Sitzung ausgestiegen ist. Das beantwortet erst der naechste Lauf,
  und genau dafuer ist diese Aenderung da. Blind neu anzustossen waere die Alternative gewesen -
  und ein Fehlschlag ohne bekannte Ursache, den man einfach noch einmal wuerfelt, ist kein
  Befund, sondern eine Hoffnung.

### 2026-09-08 - Das Turn-Budget geht fuers Lesen drauf, nicht fuers Arbeiten

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich am Backlog-Text;
  kein Code, kein Workflow.
- **BEOBACHTET, jetzt endlich lesbar:** Die Builder-Sitzungen der Laeufe 34273049230 und
  34279293146 sind beide mit demselben Befund ausgestiegen:

      {"subtype":"error_max_turns","is_error":true,"num_turns":81,
       "stop_reason":"tool_use","permission_denials":[]}

  81 Turns bei einem Budget von 80. `permission_denials` leer - der Builder hat sich an keiner
  Sperre aufgerieben, ihm ging schlicht das Kontingent aus, mitten im Werkzeuggebrauch.
- **URSACHE, gemessen:** ITO-0016 buendelte zwei Dinge. Die Mechanik (Rolle, Zuordnung Spezies ->
  Variante) beruehrt kleine Dateien: `PlayMusicPlan.kt` 198 Zeilen, `AvatarSpecies.kt` 103. Die
  Frage "wann laeuft das Thema" zwingt dagegen zum Lesen von `DockScreen.kt` mit **3.984 Zeilen**,
  dazu `PlayMusic.kt` mit 334 - und das zusaetzlich zu den drei Pflichtdokumenten am Anfang jeder
  Sitzung. Das Budget war aufgebraucht, bevor die eigentliche Arbeit begann.
- **Warum Lauf 1 durchkam:** 10,2 Minuten, knapp innerhalb der Grenze. Die Aufgabe lag genau auf
  der Kante; zwei von drei Anlaeufen fielen darueber.
- **Umgesetzt:** ITO-0016 auf die Mechanik reduziert und ausdruecklich mit dem Verbot versehen,
  `DockScreen.kt` und `PlayMusic.kt` anzufassen. Die Abspiel-Entscheidung steht jetzt als
  ITO-0023 direkt darunter, mit dem Hinweis, `DockScreen.kt` nicht als Ganzes zu lesen, sondern
  gezielt nach `PlayMusic`, `MusicRole` und `currentPlace` zu greppen.
- **Warum nicht einfach das Budget hochsetzen:** Das waere die schnellere Antwort und die
  schlechtere. Der Builder-Prompt verlangt selbst "Genau eine Evolution. Aendere so wenige Dateien
  wie moeglich" - mein Eintrag hat gegen genau diesen Grundsatz verstossen, indem er Mechanik und
  Anbindung buendelte. Ein groesseres Budget haette den Fehler im Auftrag bezahlt statt ihn zu
  beheben, und jeder Lauf waere teurer geworden.
- **Was die Annotation aus dem Eintrag darueber wert war:** Ohne sie stuende hier eine Vermutung.
  Die Diagnose lag zweimal vor und war zweimal unerreichbar; erst der dritte Lauf konnte sie
  ausliefern. Das ist der ganze Unterschied zwischen "wahrscheinlich das Turn-Limit" und 81.

### 2026-09-09 - Auch der Build muss sagen duerfen, woran er gescheitert ist

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich an der
  Fehlerausgabe von `gradlew verify` in `claude-primary-run.yml`; keine Gates verschoben, kein
  Anwendungscode.
- **BEOBACHTET:** Lauf 34346668497 hat ITO-0023 gezogen, und der Builder hat geliefert -
  `is_error: false`, 27 kB Diff, 404 Bytes Dateiliste. Gescheitert ist danach `gradlew verify`
  nach 6,5 Minuten. **Welcher** Uebersetzungsfehler oder welcher Test, war nicht zu ermitteln: Das
  Protokoll gibt die API nur vom Ende her heraus, die Testberichte liegen im selben
  Blob-Speicher wie das Diagnosepaket.
- **Dieselbe Wand, eine Ebene tiefer.** Am Vortag war es die Builder-Diagnose, jetzt die
  Verifikation. Die Lehre ist dieselbe: Was ein unbeaufsichtigter Lauf nicht ueber die API
  ausliefert, existiert fuer die naechste Sitzung nicht.
- **Umgesetzt:** `gradlew verify` schreibt seine Ausgabe zusaetzlich nach `$RUNNER_TEMP/verify.log`
  (`tee` mit `set -o pipefail`, damit der Exitcode der von Gradle bleibt und nicht der von `tee`).
  Ein Folgeschritt greift bei Fehlschlag die Zeilen heraus, die Kotlin und Gradle ohnehin als
  Fehler markieren - `e: `, `... FAILED`, `* What went wrong`, `Execution failed` - und gibt sie
  als `::error`-Annotation aus, hoechstens 25 Zeilen.
- **Belegt:** `bash -n` ueber beide Schritte, YAML-Gueltigkeit, und die Extraktion an einer echten
  Kotlin-/Gradle-Ausgabe durchgespielt: Dateiname, Zeile, Spalte und Meldung kommen vollstaendig
  an. Gegenprobe mit einer Ausgabe ohne erkennbare Fehlerzeile - dort erscheint ein
  ausdruecklicher Hinweis statt einer leeren Annotation.
- **Was das kostet, wenn man es NICHT hat:** Bei rotem `evolve` wird nichts gepusht. Die 27 kB
  fertige Arbeit sind weg, und der naechste Lauf faengt von vorn an. Ein Fehlschlag, dessen
  Ursache man nicht lesen kann, kostet deshalb nicht einen Blick ins Protokoll, sondern einen
  ganzen Lauf.
- **NICHT geloest:** Woran ITO-0023 konkret gescheitert ist. Das beantwortet der naechste Lauf.

### 2026-09-09 - Die Verify-Annotation hat sich beim ersten Einsatz bezahlt gemacht

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Aenderung ausschliesslich am Backlog-Text.
- **BEOBACHTET:** Der erste Lauf nach dem Merge der Verify-Annotation (34352208047) ist wieder an
  `gradlew verify` gescheitert - diesmal aber lesbar:

      > Task :app-sim:compileDebugUnitTestKotlin FAILED
      e: .../PlayCharacterThemeTest.kt:15:15 Redeclaration:
      e: .../PlayLoreTest.kt:22:60 Cannot access 'class InMemoryPrefsContext': it is private in file.
      (und siebzehn weitere derselben Art)

- **URSACHE, am Bestand geprueft statt aus der Meldung geschlossen:**
  `app-sim/src/test/java/com/notime/glyphsim/ui/PlayLoreTest.kt:17` deklariert bereits
  `private class InMemoryPrefsContext : ContextWrapper(null)`. Der Builder hat im selben Paket
  eine zweite Klasse desselben Namens angelegt. Dass beide `private` sind, hilft nicht: In Kotlin
  kollidieren zwei Top-Level-Klassen gleichen Namens im selben Paket unabhaengig von ihrer
  Sichtbarkeit - und nach der Kollision ist auch der urspruengliche Verweis nicht mehr eindeutig.
- **Umgesetzt:** ITO-0023 nennt den Stolperstein jetzt beim Namen, mit Datei und Zeile, und sagt,
  was stattdessen zu tun ist (eigener Name statt Verschieben oder Oeffentlichmachen der
  vorhandenen Klasse - Letzteres waere eine zweite Aenderung in einer fremden Datei).
- **Was das ueber die Annotation sagt:** Beim vorigen Lauf war exakt derselbe Fehlertyp
  aufgetreten und blieb unauffindbar; ich konnte nur feststellen, DASS `gradlew verify` fiel. Der
  Unterschied zwischen "der Bau ist rot" und achtzehn Zeilen mit Datei, Zeile und Spalte ist der
  Unterschied zwischen einem verlorenen Lauf und einer behebbaren Aufgabe.
- **Weiterhin offen:** Der Builder liegt mit 83 Turns ueber dem Budget von 80. Das ist laut
  Workflow rein diagnostisch - die Sitzung lieferte ein vollstaendiges Ergebnis - aber es zeigt,
  dass ITO-0023 auch nach dem Zuschnitt am oberen Rand arbeitet.

### 2026-09-09 - Der Testname allein sagt nicht, warum der Test fiel

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. Ein erweitertes Filtermuster in der
  Verify-Annotation; sonst nichts.
- **BEOBACHTET, und es ist zweifacher Fortschritt:** Der dritte Anlauf auf ITO-0023 (Lauf
  34377934139) hat den Namenskonflikt vermieden - der benannte Stolperstein hat gewirkt. Zwei
  Zahlen belegen es: **83 -> 61 Turns**, und der Kompilierfehler mit achtzehn Zeilen ist weg.
  Gescheitert ist jetzt ein einzelner Test:

      PlayCharacterThemeTest > das Zeitfenster verklingt von selbst FAILED

- **Was fehlte:** Die Begruendung. JUnit schreibt den Testnamen mit `FAILED` an den linken Rand,
  die Ursache (`expected:<...> but was:<...>`) aber EINGERUECKT in die naechste Zeile. Das
  Filtermuster kannte nur linksbuendige Formen und lieferte deshalb "Test X ist gefallen", ohne
  zu sagen, was erwartet wurde.
- **Umgesetzt:** Das Muster nimmt zusaetzlich `AssertionError`, `expected:`, `but was:` und
  `Caused by:` auf und laesst 40 statt 25 Zeilen durch - ein Stapel mit Kompilierfehlern UND
  Testausgabe braucht mehr Platz.
- **Belegt:** An einer echten JUnit-Ausgabe durchgespielt; `expected:<false> but was:<true>` kommt
  jetzt mit. Dazu `bash -n` und YAML-Gueltigkeit.
- **Die Einsicht dahinter:** Instrumentierung ist nicht einmal fertig, sondern folgt dem Fehler.
  Erst war unklar, WARUM die Sitzung abbrach; dann, DASS der Bau rot war; dann, WELCHE Datei; jetzt
  WARUM ein Test faellt. Jede Stufe wurde erst sichtbar, als die darueber behoben war - und jede
  hat einen Lauf gekostet, weil bei rotem `evolve` nichts gepusht wird.

### 2026-09-10 - Fennecs persoenliches Musikstueck vorbereitet

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. `DOCUMENTED INTENT` fuer die vom
  Produktverantwortlichen gewuenschten eigenen, spaeter singbaren Charakterstuecke und `FACT`
  fuer Fennecs bestehende Persoenlichkeit und `signatureTopic = DRINK`.
- **Ausgangsproblem und Nutzerwirkung:** Die feste Charaktermusik-Rolle und ihr taeglicher Anlass
  waren vorhanden, fuer FENNEC fehlten aber Prompt und Manifest-Eintrag. Ein spaeterer Stream
  konnte den gelassenen, verlaesslichen Beschuetzer deshalb musikalisch noch nicht von den
  anderen Wesen unterscheiden.
- **Getroffene Entscheidung:** Variante 04 ist das waermste und sozialste der sechs Themen:
  mittleres Tempo, gebuerstetes Schlagzeug, weiches Rhodes, warme Bassbegleitung und eine eigene
  singbare Acht-Takt-Melodie. Die Energie bleibt ruhig, einladend und verlaesslich statt kalt,
  sentimental oder hastig. Das erweitert nicht Fennecs Persoenlichkeit, sondern uebersetzt die
  bereits geschuetzte Stimme in Musik.
- **Verworfene Alternativen:** Kein Austausch der gemeinsamen Tagesmusik, keine Rotation der
  persoenlichen Variante, kein Gesang im erzeugten Asset und keine zweite oder kostenpflichtige
  Musikpipeline. Ein rein schlaefriges Ambient-Stueck wurde verworfen, weil es Fennecs soziale
  Waerme und die geforderte singbare Melodie verdecken wuerde.
- **Betroffene Bereiche:** `music/prompts/theme-fennec.txt`, `music/manifest.json`,
  `music/README.md`, `evolutions/BACKLOG.md` und die aktuelle `UEBERGABE.md`. Kein
  Anwendungscode, keine oeffentlichen App-Texte und keine ausgelieferte Audiodatei.
- **Daten, Migration und Ruecksetzweg:** Keine Room-, Preference- oder Nutzerdatenaenderung.
  Ruecksetzweg ist ein gewoehnlicher Revert dieses PRs.
- **Ausgefuehrte Tests:** Der lokale
  `python3 tools/music/generate_music.py --track-id theme-fennec --dry-run` war erfolgreich;
  danach liefen `Verify Music Tooling` und die vollstaendige Android-CI auf dem PR.
- **Noch unverified:** Klang, Loop und subjektive Passung koennen erst nach einem einzelnen
  manuellen `Generate Itoeva Music`-Lauf gehoert und ueber das bestehende Freigabe-Gate
  beurteilt werden. Es wurde bewusst kein Audio erzeugt oder gemergt.

### 2026-09-10 - Gloops persoenliches Musikstueck vorbereitet

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. `DOCUMENTED INTENT` fuer die vom
  Produktverantwortlichen gewuenschten eigenen, spaeter singbaren Charakterstuecke und `FACT`
  fuer Gloops bestehende Persoenlichkeit und `signatureTopic = REST`.
- **Ausgangsproblem und Nutzerwirkung:** Die feste Charaktermusik-Rolle und ihr taeglicher Anlass
  waren vorhanden, fuer GLOOP fehlten aber Prompt und Manifest-Eintrag. Ein spaeterer Stream
  konnte den gemuetlichen, leicht chaotischen Entschleuniger deshalb musikalisch noch nicht von
  den anderen Wesen unterscheiden.
- **Getroffene Entscheidung:** Variante 05 ist das langsamste und weichste der sechs Themen:
  runder Bass, verwaschene warme Pads, gedaempfte Brush-Percussion und eine klare singbare
  Acht-Takt-Melodie. Leicht verspaetete Phrasen und ein wanderndes Detail zeigen freundliches
  Chaos, ohne Ruhe, Wiedererkennbarkeit oder Loop-Stabilitaet aufzugeben. Das erweitert nicht
  Gloops Persoenlichkeit, sondern uebersetzt die bereits geschuetzte Stimme in Musik.
- **Verworfene Alternativen:** Kein konturloser Ambient-Drone, keine harten Kanten, kein Austausch
  der gemeinsamen Tagesmusik, keine Rotation der persoenlichen Variante, kein Gesang im
  erzeugten Asset und keine zweite oder kostenpflichtige Musikpipeline.
- **Betroffene Bereiche:** `music/prompts/theme-gloop.txt`, `music/manifest.json`,
  `music/README.md`, `evolutions/BACKLOG.md` und die aktuelle `UEBERGABE.md`. Kein
  Anwendungscode, keine oeffentlichen App-Texte und keine ausgelieferte Audiodatei.
- **Daten, Migration und Ruecksetzweg:** Keine Room-, Preference- oder Nutzerdatenaenderung.
  Ruecksetzweg ist ein gewoehnlicher Revert dieses PRs.
- **Ausgefuehrte Tests:** Der lokale
  `python3 tools/music/generate_music.py --track-id theme-gloop --dry-run` war erfolgreich;
  danach laufen `Verify Music Tooling` und die vollstaendige Android-CI auf dem PR.
- **Noch unverified:** Klang, Loop und subjektive Passung koennen erst nach einem einzelnen
  manuellen `Generate Itoeva Music`-Lauf gehoert und ueber das bestehende Freigabe-Gate
  beurteilt werden. Es wurde bewusst kein Audio erzeugt oder gemergt.

### 2026-09-10 - Hootlets persoenliches Musikstueck vorbereitet

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. `DOCUMENTED INTENT` fuer die vom
  Produktverantwortlichen gewuenschten eigenen, spaeter singbaren Charakterstuecke und `FACT`
  fuer Hootlets bestehende Persoenlichkeit und `signatureTopic = FOCUS`.
- **Ausgangsproblem und Nutzerwirkung:** Die feste Charaktermusik-Rolle und ihr taeglicher Anlass
  waren vorhanden, fuer HOOTLET fehlten aber Prompt und Manifest-Eintrag. Ein spaeterer Stream
  konnte den stillen, geduldigen Beobachter deshalb musikalisch noch nicht von den anderen Wesen
  unterscheiden.
- **Getroffene Entscheidung:** Variante 06 ist das klarste und geordnetste der sechs Themen:
  stetiger ruhiger Puls, ein sauberes Piano-/Vibraphon-Motiv und genau eine kleine kontrollierte
  Verschiebung je Wiederholung. Die Acht-Takt-Melodie bleibt sofort erkennbar und singbar;
  Resonanz und warme offene Harmonie halten die Praezision aufmerksam statt kalt. Das erweitert
  nicht Hootlets Persoenlichkeit, sondern uebersetzt die bereits geschuetzte Stimme in Musik.
- **Verworfene Alternativen:** Kein steriler Metronomcharakter, keine virtuosen Laeufe, kein
  konturloses Ambient-Stueck, kein Austausch der gemeinsamen Tagesmusik, keine Rotation der
  persoenlichen Variante, kein Gesang im erzeugten Asset und keine zweite oder kostenpflichtige
  Musikpipeline.
- **Betroffene Bereiche:** `music/prompts/theme-hootlet.txt`, `music/manifest.json`,
  `music/README.md`, `evolutions/BACKLOG.md` und die aktuelle `UEBERGABE.md`. Kein
  Anwendungscode, keine oeffentlichen App-Texte und keine ausgelieferte Audiodatei.
- **Daten, Migration und Ruecksetzweg:** Keine Room-, Preference- oder Nutzerdatenaenderung.
  Ruecksetzweg ist ein gewoehnlicher Revert dieses PRs.
- **Ausgefuehrte Tests:** Der lokale
  `python3 tools/music/generate_music.py --track-id theme-hootlet --dry-run` war erfolgreich;
  danach laufen `Verify Music Tooling` und die vollstaendige Android-CI auf dem PR.
- **Noch unverified:** Klang, Loop und subjektive Passung koennen erst nach einem einzelnen
  manuellen `Generate Itoeva Music`-Lauf gehoert und ueber das bestehende Freigabe-Gate
  beurteilt werden. Es wurde bewusst kein Audio erzeugt oder gemergt.

### 2026-09-10 - Living Agent System als naechster Architektur-Meilenstein freigegeben

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. Ausdrueckliche menschliche
  Produktentscheidung; die bisherige pauschale Prozess-vor-Gameplay-Sperre ist fuer diesen
  begrenzten Rahmen aufgehoben.
- **Ausgangsproblem:** Itoeva besitzt bereits Tagesplaene, gewichtete Aktivitaeten, Orte,
  Vorrat, Geld, Arbeit, Einkauf, Essen, Besuche und kurze Traumerinnerungen. Der entscheidende
  Ressourcenpfad steckt jedoch als Sonderfall in `DockScreen`; es gibt kein persistentes
  avatarbezogenes Beduerfnis, kein langlebiges Ziel, keinen erklaerbaren Plan, keine
  entscheidungswirksame episodische Erinnerung und keine semantische Kommunikation.
- **Entscheidung:** Ein kleiner reiner Kotlin-Kern fuehrt Beduerfnisse, Weltzustand,
  Utility-Auswahl, Ziele, Replanning, Ereignisse, Episoden, lernende Praeferenzen, Beziehungen
  und Symbole zusammen. Geschichten entstehen ausschliesslich als Folge der Simulation.
- **Erster Beleg:** Der vertikale Slice muss den ressourcenbedingten Pfad
  `WORK -> BUY_FOOD -> EAT`, eine optionale Freizeit-/Entwicklungshandlung, eine
  zustandsabhaengige symbolische Interaktion und auseinanderlaufende Mehrtageshistorien
  deterministisch pruefen.
- **Architekturentscheidung:** Bestehende `PlayRoutine`-/`PlayScene`-Mechanik bleibt
  Ausfuehrungsebene. Zuerst kommt die reine Domaene, danach profilbezogene Persistenz, danach die
  gezielte Runtime-Anbindung und zuletzt ein read-only Stream-Zustandsvertrag. Kein grosser
  Umbau, keine zweite Pipeline und vorerst keine neue `:core`-Entity.
- **Persistenzfolge:** Ressourcen und Agentenzustand muessen pro `profileId` getrennt werden.
  Die konkrete Android-Speicherung wird erst im Persistenz-PR entschieden; Room-Aenderungen
  erfordern Migration und Migrationstest im selben PR.
- **Abgrenzung:** Kein `StoryManager`, keine lineare Queststruktur, kein freier Dialog, keine
  Twitch-Oberflaeche, keine Cloud-/Netzwerk-/Konto-/Zahlungsarchitektur und keine Aenderung des
  Musik-Freigabe-Gates.
- **Betroffene Dokumente:** `LIVING_AGENT.md`, `Vision.md`, `Architecture.md`,
  `NextTasks.md`, `CLOUD_CODE_BRIEFING.md`, `UEBERGABE.md` und dieses Protokoll.
- **Tests dieses Planungs-PRs:** Dokument-/Diff-Pruefung und Repository-CI; Anwendungscode und
  Datenvertraege bleiben unveraendert.


### 2026-09-10 - Living-Agent-Kern: Entscheiden, Planen, Neuplanen (Schnitt 2a)

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. Erster Code-Schnitt des am selben Tag
  freigegebenen Meilensteins; deterministische JVM-Tests, keine Geraetepruefung noetig.
- **Ausgangsproblem:** Der Ressourcenpfad `Vorrat leer -> Geld pruefen -> Arbeit/Einkauf` steckte
  als Sonderfall in `DockScreen`. Es gab kein langlebiges Ziel, keinen Plan, keine benannte
  Voraussetzung und damit keine Antwort auf die Frage, warum ein Wesen gerade das tut, was es
  tut.
- **Entscheidung:** Ein reiner Kotlin-Kern unter `app-sim/.../living/` in fuenf Dateien:
  `LivingWorld.kt`, `LivingNeed.kt`, `LivingAction.kt`, `LivingPlanner.kt`, `LivingAgent.kt`.
  Kein Android, keine Uhr, kein Zufall - Zeit wird als `day`/`minuteOfDay` hereingereicht.
- **Aufteilung des geplanten Schnitts:** Der in `LIVING_AGENT.md` als PR 2 beschriebene
  Simulationskern wurde in 2a (entscheiden) und 2b (sich erinnern und verstaendigen) geteilt.
  Grund ist kein Prinzip, sondern die Geschichte dieses Repositories: An zu gross geschnittenen
  Aufgaben ist die Builder-Sitzung hier dreimal am Zugbudget gescheitert (ITO-0016/ITO-0023).
  Beide Haelften sind fuer sich gruen und ruecksetzbar.
- **Erster Beleg:** `Planner.planFor` leitet aus derselben Bedingung drei verschiedene Wege zu
  Essen ab - nachsehen/essen, einkaufen/essen und arbeiten/einkaufen/essen. Die Kette
  `WORK -> BUY_FOOD -> EAT` steht nirgends als Kette im Code; sie faellt aus der Ressourcenlage
  ab und verschwindet, sobald Geld da ist. Ein Testlauf ueber sieben Schritte fuehrt sie
  vollstaendig aus und belegt den gesunkenen Hunger, den ausgegebenen Lohn und den Restvorrat.
- **Architekturentscheidungen mit Bindung fuer alles Weitere:**
  - Voraussetzungen sind benannte Dinge (`Requirement.Coins`, `.Portions`, `.At`, `.SiteOpen`)
    und keine Wahrheitswerte. Nur deshalb kann `AgentExplanation.blockedBy` sagen, woran es
    haengt, ohne dass irgendwo ein Satz dafuer geschrieben wurde. Ereignisse und Erklaerung
    enthalten aus demselben Grund keinen freien Text - Sprache macht die Anzeige daraus.
  - Die Pruefung der Voraussetzungen steht **vor der Ausfuehrung, nicht beim Planen**. Faellt
    sie, faellt der Plan und das Ziel bleibt; der naechste Schritt leitet aus derselben Absicht
    einen anderen Weg ab. Daran haengt der Unterschied zwischen einem lebendigen Wesen und einer
    festen Animationsfolge.
  - `ActionOutcome` ist die einzige Erweiterungsstelle fuer Wirkungen und traegt heute schon
    mehr als Beduerfnisbefriedigung (Muenzen, Vorrat, Ort, Zeit). Wissen, Erinnerung,
    Beziehungswirkung und Faehigkeitsfortschritt kommen als weitere Felder derselben Klasse
    dazu, ohne dass eine Handlung einen Sonderweg bekommt.
  - `Personality` liegt als Datum im `AgentState` und wird nicht bei jeder Entscheidung aus
    `AvatarSpecies` nachgeschlagen. Das ist die Bedingung dafuer, dass sich ein Wesen spaeter
    ueberhaupt veraendern kann; ein Wert aus einem Enum koennte es nie. Der Bias ist bei jeder
    Spezies kleiner als `UtilitySelector.MIN_PRESSURE` und kann den Beduerfnisdruck damit
    verschieben, aber nicht ueberstimmen - ein Test haelt das fest.
  - Eigenes Ortsmodell `LivingSite` mit vier Werten statt der sechzehn `PlayScene.Place`. Die
    Abbildung gehoert in den Runtime-Adapter (NT-065). Haenge die Entscheidungslogik an
    `PlayScene.kt` mit 3.672 Zeilen, waere sie ohne Emulator nicht mehr pruefbar.
- **Zwei Testerwartungen waren falsch, nicht der Code.** Ein Agent lief nach dem Essen weiter
  (richtig - der Testlauf war zu lang angesetzt), und ein ruhefreudiges Wesen waehlte Lernen
  statt Ruhe, weil Ruhe doppelt so viel Zeit kostet. Beide Male wurde der Test korrigiert und
  nicht die Rechnung; die Kostenrechnung war in beiden Faellen die bessere Antwort.
- **Abgrenzung:** Kein `StoryManager`, kein allgemeiner KI-Planer, kein freier Dialog, keine
  Persistenz, keine Aenderung an `DockScreen`, keine Room-Entity und keine Zeile in `:core`.
- **Betroffene Dokumente:** `LIVING_AGENT.md` (Abschnitt "Stand" und die Aufteilung von PR 2),
  `Architecture.md`, `NextTasks.md`, `UEBERGABE.md` und dieses Protokoll.
- **Tests:** `bash tools/reaction-preview/tests.sh` - 265 Tests gruen (vorher 243, davon 22 neu
  in `LivingAgentTest`). `python3 -m unittest discover --start-directory tools/music` - 15 Tests
  gruen. Die neue Testdatei ist an beiden Stellen in `tools/reaction-preview/tests.sh`
  eingetragen; ohne den zweiten Eintrag waere sie gruen gewesen, ohne je gelaufen zu sein.
- **Naechster Schritt:** NT-067 (Schnitt 2b) - Episoden, Beziehungen, symbolische
  Verstaendigung, gelernter Geschmack und `CONNECT_WITH`, wieder als reine Domaene.

### 2026-09-10 - Living-Agent-Kern: Erinnern und Verstaendigen (Schnitt 2b)

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. Zweiter reiner Kotlin-Schnitt des
  freigegebenen Meilensteins; deterministische JVM-Belege, keine Geraetepruefung erforderlich.
- **Ausgangsproblem:** Schnitt 2a konnte wollen, planen, scheitern und neu planen, aber Erlebtes
  veraenderte spaetere Entscheidungen nicht. Andere Wesen waren weder als Beziehung noch als
  semantisches Gegenueber vorhanden; ein Stream haette eine soziale Szene deshalb nicht aus dem
  Zustand erklaeren koennen.
- **Entscheidung:** `AgentState` traegt nun begrenzte verdichtete `Episode`-Listen, gelernte
  Zielpraeferenzen und `RelationshipState` je Gegenueber. Beziehungen trennen Vertrauen und
  Naehe und behalten die letzte typisierte Interaktion. `SymbolicIntent` bildet die feste
  sprachunabhaengige Bedeutungsmenge; `CONNECT_WITH` wird wie jedes andere Ziel durch
  `UtilitySelector` bewertet und vom kleinen regelbasierten Planer in eine Einladung
  ueberfuehrt.
- **Erster Beleg:** STARLET sendet aus echtem sozialem Druck `PLAY + QUESTION`. Derselbe
  WYRMLING antwortet bei hoher Energiebelastung mit `TIRED + NO`, bei Kraft und sozialem
  Bedarf mit `PLAY + YES`; ein dringendes laufendes `GET_FOOD` fuehrt zu `FOOD + NO`.
  Zwei mit gleicher Persoenlichkeit und gleicher Bedarfslage gestartete Agenten sammeln durch
  verschiedene Begegnungen ueber mehrere simulierte Tage unterschiedliche Praeferenzen und
  Episoden, ohne dass eine Handlungskette als Plot vorgegeben ist.
- **Architekturentscheidungen mit Bindung fuer alles Weitere:**
  - `Requirement.Near` macht Anwesenheit zu einem benannten, erklaerbaren Hindernis statt zu
    einem Wahrheitswert.
  - Erinnerung, Praeferenz, Beziehung und Symbolbedeutung sind Felder von `ActionOutcome`.
    `Action.applyTo` bleibt die einzige Stelle, die Wirkungen berechnet; es gibt keinen
    sozialen Sonderweg und keine zweite Ereignispipeline.
  - Episoden speichern nur wichtige typisierte Ereignisse und eine kleine Wertung. Die feste
    Grenze von 24 verhindert Rohframe- und Tick-Historien.
  - Die Antwortbereitschaft ist eine lesbare Rechnung aus Energie, sozialem Druck, Beziehung,
    Persoenlichkeitsbias und dem Druck eines kollidierenden laufenden Grundziels. Gleiche
    Eingaben ergeben dieselbe Antwort; es gibt weder Uhr noch Zufall noch Dialogtabelle.
  - Gelernter Geschmack liegt als Datum im `AgentState` und bleibt auf einen kleinen Bereich
    begrenzt. Der bestehende Test haelt weiterhin fest, dass jeder Startbias kleiner als
    `UtilitySelector.MIN_PRESSURE` bleibt.
- **Abgrenzung:** Keine Persistenz und kein `LivingAgentStore`, keine Aenderung an Room,
  `:core`, `DockScreen`, `PlayScene` oder `PlayRoutine`; kein `StoryManager`, kein freier
  Textdialog, kein allgemeiner Planer, kein Twitch-UI und keine Musik- oder Audioumbaute.
- **Betroffene Bereiche:** Die bestehenden fuenf Kerndateien unter
  `app-sim/src/main/java/com/notime/glyphsim/living/`, die bestehende
  `LivingAgentTest.kt`, `LIVING_AGENT.md`, `NextTasks.md`, `UEBERGABE.md` und dieses
  Protokoll. Keine neue Quell- oder Testdatei, deshalb keine Aenderung an der ausdruecklichen
  Dateiliste in `tools/reaction-preview/tests.sh`.
- **Tests:** Sechs neue Verhaltensfaelle erweitern `LivingAgentTest` von 22 auf 28 Tests und
  die Offline-Strecke von 265 auf 271 Tests. Geprueft werden Symbolaustausch, zwei
  zustandsabhaengige Antworten, Vorrang eines laufenden Grundziels, Episodengrenze,
  Entscheidungseinfluss und auseinanderlaufende Mehrtageshistorien. Zusaetzlich bleibt die
  Musikstrecke mit 15 Tests unveraendert; vor dem Merge entscheidet die vollstaendige Head-CI.
- **Naechster Schritt:** NT-064 - profilbezogene, versionierte Persistenz. Falls dafuer Room
  gewaehlt wird, liegen Migration und Migrationstest im selben PR; `:core` bleibt
  unangetastet.

### 2026-09-10 - Living-Agent-Zustand versioniert und profilbezogen gespeichert (Schnitt 3)

- **Version / Evidenzklasse:** Protokoll bleibt 0.6. Android-Grenze mit deterministischen
  JVM-Belegen; keine Geraetepruefung fuer den Speicherkern erforderlich.
- **Ausgangsproblem:** Der Kern konnte Erinnerungen, Beziehungen, Ressourcen und gelernten
  Geschmack bilden, verlor diesen Zustand aber beim Prozessende. Ein alter Plan durfte beim
  Wiedereinstieg zugleich keine inzwischen geschlossene Welt oder abwesende Figur umgehen.
- **Entscheidung:** `LivingAgentStore` speichert genau einen atomaren, versionierten Snapshot
  pro `profileId`. Der reine Kern bleibt speicherfrei; ein kleines `LivingAgentStorage` trennt
  Codec und Android-`SharedPreferences`-Adapter. Snapshot-Version 2 umfasst Agent, Ressourcen,
  Persoenlichkeit, Ziel, gelernte Praeferenzen, begrenzte Episoden, Beziehungen und das letzte
  wichtige Ereignis.
- **Erster Beleg:** Zwei Profile werden mit verschiedenen Hungerwerten und Muenzen gespeichert
  und getrennt wiederhergestellt. Ein V1-Snapshot wird mit leeren neuen Lernfeldern gelesen und
  anschliessend als V2 geschrieben. Derselbe Snapshot mit derselben expliziten Simulationsminute
  ergibt zweimal exakt denselben Zustand.
- **Architekturentscheidungen mit Bindung fuer alles Weitere:**
  - SharedPreferences statt Room: Der kleine zusammenhaengende Snapshot ist nicht relational.
    Eine Room-Migration wuerde hier Schema- und Zwei-Datenbank-Risiko ohne fachlichen Nutzen
    erzeugen. `:core` und beide Room-Datenbanken bleiben unveraendert.
  - Zeit wird als `currentSimulationMinute` uebergeben. Es gibt keinen Zugriff auf Systemuhr
    oder Zufall; Beduerfnisse und Welt laufen nur um die nichtnegative Differenz weiter.
  - Das langlebige Ziel wird gespeichert, der konkrete Plan absichtlich nicht. Aktuell
    geoeffnete Orte und anwesende Profile kommen beim Laden aus der Runtime; danach plant der
    Kern gegen die neue Wirklichkeit.
  - Unbekannte Zukunftsversionen und beschaedigte Pflichtwerte werden abgelehnt statt geraten.
    V1-Felder fuer Episoden, Beziehungen und Geschmack werden leer und nachvollziehbar migriert.
- **Abgrenzung:** Keine Runtime-Anbindung, keine Aenderung an `DockScreen`, `PlayRoutine`,
  Room oder `:core`; keine zweite Simulationspipeline, kein Stream-UI und kein Audio.
- **Betroffene Bereiche:** `app-sim/.../data/LivingAgentStore.kt`,
  `LivingAgentStoreTest.kt`, die ausdrueckliche Offline-Testliste sowie `LIVING_AGENT.md`,
  `NextTasks.md`, `UEBERGABE.md` und dieses Protokoll.
- **Tests:** Sechs neue Verhaltensfaelle erhoehen die Offline-Strecke von 271 auf 277 Tests:
  vollstaendiger Roundtrip, Profiltrennung, Zeitfortschritt, deterministische Wiederholung,
  echte V1-Migration und Ablehnung einer Zukunftsversion. Die neue Quell- und Testdatei stehen
  in `SRCS`, `TEST_SRCS` und `TEST_CLASSES`; die vollstaendige Head-CI entscheidet vor
  dem Merge.
- **Naechster Schritt:** NT-065 - Ziel und jeweils naechste Living Action ueber einen kleinen
  Adapter auf die vorhandenen `PlayRoutine`-, Vorrats-, Geld- und Besuchsmechaniken abbilden;
  den harten Notfall-Sonderfall dabei ersetzen und keine zweite Choreografie bauen.

