# Itoeva Evolution Protocol

Version: 0.2 - seit 2026-08-18 mit einer engen, im Abschnitt "Character Evolution" →
"Erzählerische Autonomie" beschriebenen Ausnahme: Welt und Beziehungen der sechs Wesen dürfen
seitdem ohne vorherige menschliche Freigabe der kreativen Richtung weiterentwickelt werden. Der
übrige Ablauf (Branch, zweiter Agent als Reviewer, PR, menschlicher Merge) bleibt unverändert.

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

**OPEN DECISION:** Der endgültige Produktname ist im Repository nicht eindeutig. Verwendet werden
unter anderem Itoeva, Glyphminder und Tama.

**OPEN DECISION:** Es ist nicht abschließend dokumentiert, ob die Nothing-Hardware-App und die
Simulator-/Tama-App dauerhaft zwei Produkte bleiben oder welche davon die primäre Produktfassung
ist.

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
