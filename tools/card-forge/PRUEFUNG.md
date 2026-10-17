# Prüfung der Kartenschmiede – 17.10.2026

Geprüfte Basis: `claude/kartenschmiede`, Commit
`bf914ce1816c28e3fa892528dd333c77a4054d9d`. Arbeitsbranch: `codex/karten-beschwoerung`.
Auftrag: `CODEX_AUFTRAG.md`, Teil 1; Abschlussdokumentation gemäß Nutzerauftrag.

## Entscheidung: Teil 2 jetzt zurückstellen

**Kein Einbau in die Kotlin/Compose-Spielvariante zum jetzigen Zeitpunkt.** Die Schmiede
ist ein brauchbarer Daten- und Gestaltungsprototyp, aber noch keine belastbare Spielregel.
Exportgrenze, Monatswechsel und Effektsemantik müssen vor einer Übernahme geklärt werden.
Kampf, Raster, Gruppe und Kampfklassen sind offen; der Nutzer erwägt außerdem 3D/2,5D und
eine andere Engine. Ein Compose-Kartenbuch wäre isoliert zwar rücknehmbar, würde aktuell
aber ungeklärte Daten und Regeltexte als Spielinhalt etablieren. 13 Animationen mit je
drei Formen und spätere Videoanbindung würden zusätzliche Darstellungskosten binden,
bevor klar ist, welche Beschwörung im Spiel tatsächlich gebraucht wird.

Die vorhandene HTML-Galerie reicht zur Sichtung. Ein engineunabhängiger Datenvertrag und
ein kleiner Regelversuch liefern jetzt mehr Erkenntnis. Das ist **keine Entscheidung für
Godot oder gegen Compose**. Es werden weder Kampf noch Sammlung, Besitz, Booster,
Animations- oder Videoschnittstelle implementiert. Regeln und bestehende Exporte werden
in diesem Prüf-PR nicht verändert; die unten genannten Regeländerungen sind Vorschläge.

## Ausgeführte Prüfungen und Grenzen

- **TESTED BEHAVIOR:** `python tools/card-forge/test_forge.py`: **8/8 erfolgreich**.
  Ausschließlich die erfundenen Testzeilen; `private/` wird nicht benötigt.
- **TESTED BEHAVIOR:** Zusätzlicher direkter Durchlauf von `baue_karte`: alle 67 Arten,
  alle Kombinationen von Quality/Low Vola/Momentum/Value/Size aus 1 bis 5,
  Composite fest neutral. **209.375 Kombinationen**, keine Abweichung über 0,51;
  größte Abweichung **0,5 Punkte**. Sektoren entsprechen den Industriegruppen in
  `rules.json`; explizite Volksüberschreibungen bleiben wirksam. Dies prüft die
  Budgetarithmetik, keine Partien oder Gewinnraten.
- **TESTED BEHAVIOR:** Mit temporären erfundenen XLSX-Zeilen reproduziert:
  Kennung im öffentlichen Monatsbericht, Faktorobjekt im Export, Urform → Jungform
  bei Size 5 → 1 sowie Wiederverwendung einer alten Karten-ID bei fehlendem Register.
  Vorgehen: zwei Zeilen gleichen erfundenen Namens schmieden; danach eine Zeile behalten,
  Size erst auf 5, dann 1 setzen; abschließend eine neue Identität mit anderem privatem
  Registerpfad gegen denselben Ausgabeordner schmieden. Assertions jeweils erfolgreich.
  Ein erster Lauf scheiterte beim Windows-Temp-Aufräumen am offenen XLSX-Handle;
  Wiederholung mit `gc.collect()` nach den Schmiedeläufen erfolgreich inklusive Aufräumen.
- **FACT:** Öffentlichen Oktoberbestand und Monatsbericht gegengeprüft: 616 aktive
  Karten, 0 ruhende, 13 Völker, 67 Arten, 123 Mythische, 42 Karten mit Kosten 10.
  Einzelne echte Faktorwerte und Firmenkennungen werden hier nicht wiedergegeben.
- Gradle-Abschluss: siehe unten. Kein Geräte-, Animations- oder Partientest; kein
  Nachweis von Spielbalance, Engineeignung oder langfristiger Sammlungsverträglichkeit.

Die vorhandenen Tests sichern die Grundkurve, eine Momentum-Beispielreihe, Namen,
Seltenheitszuordnung, Wegfall und einen Folgemonat ab. Es fehlen insbesondere
Exportprüfungen aller Dateien, Wiedererwachen, Registerverlust, leere Tabellen,
Identitätswechsel, Schema-Versionierung und Kampfeffekt-Tests. Der Datenschutztest
prüft nur zwei Zeichenfolgen in `karten.json`; Bericht und Galerie sind nicht erfasst.

## Priorisierte Befunde

| Prüfdimension | Bewertung |
|---|---|
| Sicherheit / Datengrenze | Nicht freigabefähig: öffentliche Faktorwerte und Kennungen. |
| Korrektheit | Grundkurve und Erhalt ruhender Karten gut; Registerverlust und Monatsrückstufung offen. |
| Performance | Für den Generator kein akuter Blocker festgestellt; Registersuche ist linear pro Karte, kein Laufzeitbenchmark. |
| Wartbarkeit | Gute Trennung von Regelwerten und Mechanik; Exportvertrag, Effektsemantik und Randfalltests fehlen. |

### P1 – Öffentliche Ausgabe verletzt die geforderte Datengrenze

`forge.py:270` übernimmt die sechs originalen Faktorquintile als `herkunft` in jede
Karte. Alle 616 vorhandenen Karten enthalten das Objekt; die Galerie bekommt dieselbe
Liste (`forge.py:416–423`) und zeigt die Werte in ihrer Detailansicht
(`galerie_vorlage.html:186`). Fehlende Firmennamen machen diese Werte nicht zu
erfundenen Daten. Der Monatsbericht schreibt außerdem die originalen Kennungen der
zusammengelegten Gattungen (`forge.py:409–411`). Der vorhandene Oktoberbericht
enthält solche Kennungen bereits. `.gitignore` für `private/` schützt diese Exporte nicht.

**Nächste technische Korrektur:** öffentliche Felder ausdrücklich erlauben, `herkunft`
und Identitätsinformationen ausschließlich privat halten; öffentliche Berichte nur mit
Karten-IDs und aggregierten Zahlen. JSON, HTML und Markdown mit erfundenen Markern
gemeinsam auf Lecks testen. Bestehende öffentliche Dateien müssen ebenfalls bereinigt
werden. Eine Historienbereinigung ist ein gesonderter Eingriff; dieser PR schreibt keine
Historie um und reproduziert keine vertraulichen Inhalte. Eine reine `rules.json`-Änderung
kann diesen Exportfehler nicht beheben.

### P1 – Fehlendes Register kann bestehende Identitäten überschreiben

`forge.py:351–375` startet bei fehlendem Register wieder bei `K0001`; vorhandene
`out/karten.json` wird erst danach eingelesen (`386–394`). Ein neuer Datensatz kann
dadurch dieselbe ID wie ein altes Wesen erhalten. Die alte Karte wird dann nicht einmal
als ruhend ergänzt. Reproduziert mit einer neuen erfundenen Identität und leerem
Registerpfad: gleiche ID, anderer Name. Vor einem Monatslauf mit bestehenden Ausgaben
muss ein fehlendes oder unpassendes Register einen klaren Abbruch auslösen. Ein geprüfter
privater Sicherungs- und Wiederherstellungsweg gehört dazu.

### P2 – „Nichts geht verloren“ deckt aktive Monatsänderungen nicht ab

Ruhende Karten werden korrekt behalten. Aktive Karten entstehen dagegen jeden Monat
neu (`forge.py:381–384`), Form und Seltenheit direkt aus Size (`209`, `249`).
Der reproduzierte Wechsel Size 5 → 1 behält ID/Name, ersetzt aber Urform/Mythisch
durch Jungform/Gewöhnlich. Auch Werte und kosmetischer Glanz können sinken oder
verschwinden. Der README-Begriff „Entwicklung statt Austausch“ ist dafür zu einseitig.

**OPEN DECISION:** Monatsausgabe als getrennte Edition mit unverändert erhaltener alter
Karte oder als veränderliche Katalogansicht? Empfehlung: Editionen und dauerhafte
Sammlungsidentität trennen, erworbene Formen/Glanz erhalten. Noch kein Besitzmodell
festlegen. Eine absichtlich gewählte Kampfressource darf nur im Match verbraucht werden;
Karten, Beziehungen und Weltfortschritt dürfen daraus keinen dauerhaften Verlust erleiden.

### P2 – Mehrere Effekttexte beschreiben noch keine ausführbare Regel

`Aufrüsten` lässt offen, ob nur die nächste passende Beschwörung oder jede spätere
profitieren soll; „einmalig“ ist dafür nicht eindeutig. Bei `Zins` ist unklar, ob ein
Kristall temporär oder ein zusätzliches dauerhaftes Maximum gemeint ist. `Blutpakt`
definiert weder den Fall ohne anderes eigenes Wesen noch Schaden/Sterben/Ziehen-Reihenfolge.
`Erwachen` braucht Kopierzeitpunkt, X, Ersetzen/Ergänzen und die Behandlung von
Beschwörungseffekten. `Panzer` nennt keine Reihenfolge gegenüber Schild und
Flächenschaden. Mehrere `Zuflucht`-Quellen brauchen eine gemeinsame Auslöseregel.
`Flackernd`/`Ungestüm` brauchen Verteilung und Untergrenze des Schadens.

Weitere technische Risiken: Entdoppeln allein nach Firmenname (`forge.py:79–92`) kann
alle leeren Namen zusammenlegen; eine vollständig leere Eingabe greift auf
`uebersicht[0]` zu (`400`) und kann nach bereits geschriebenem JSON/Register abbrechen.
Ausgaben sind nicht atomar. Diese Fälle sind durch Codeinspektion festgestellt, hier
nicht als zusätzlich bestandene Regressionstests behauptet. `uralt_optional` wird im
Generator nicht ausgewertet; Aktivieren des Schalters allein hätte keine Wirkung.

## Balance: 2K+1 ist ein Budget, kein Balancebeweis

**FACT:** Bei halben Effektpreisen rundet `baue_karte` die Körperwerte auf ganze Zahlen.
„Exakt“ ist daher unzutreffend: bis zu +0,5 Punkte sind erlaubt und beobachtet. Die
Kurve bleibt ein sinnvoller erster Vergleich für einfache Körper und einmalige Effekte.
Ein 1/8-Körper und ein 4/5-Körper haben trotzdem andere Tauschmöglichkeiten. Sofortiger
Angriff, Kartenziehen, Ressourcenvorsprung und wiederholte Auslöser sind kontextabhängig.
Die Karte bezahlt außerdem hohe Effektpreise über einen kleineren Körper oder höhere
Kosten; ein größerer Preis allein löst unbeschränkte Synergien nicht.

`Wachstum` verspricht insgesamt +2/+2 für 2 Punkte. Das ist gegenüber einmaligem
`Knospend` (+1 Angriff für 1) nur mit einem deutlichen Überlebensabschlag plausibel.
`Heimvolk` schenkt +1 Leben für 0 und ist weder in `wertpunkte` noch als angewandter
Bonus im Export enthalten. Die Kurvenbehauptung gilt daher ohnehin vor dieser Affinität.
Rolle `Wächter` und Merkmal `Wächter` sind zudem verschieden: die Rolle allein setzt
keinen Angriffszwang. Das muss eine spätere Darstellung deutlich machen.

### Konkrete Änderungen für einen Regelversuch in `rules.json`

**UNVERIFIED / Vorschlag:** Folgende JSON-Pfade und Texte sind ein prüfbarer Kandidat,
kein angewandtes Balancing und keine Auflösung der Kampfentscheidung. Referenz sind
Szenarien mit 1, 3 und 5 Zielen sowie 0, 1 und 2 tatsächlich erlebten Folgezügen.
Das legt weder Brettgröße noch Matchdauer fest. Zuerst klar begrenzen, dann bepreisen.

| JSON-Pfad unter `voelker` | Fähigkeit: aktueller → vorgeschlagener Preis pro X | Gabe: aktueller → vorgeschlagener Preis | Begründung / notwendige Textänderung |
|---|---|---|---|
| `Information Technology` | Aufrüsten 1 → 1 | Sternennetz 2 → 3 | Fähigkeit gilt nur für die **nächste** passende Beschwörung, einmal insgesamt, solange die Quelle anwesend ist. Aura mit 3 Empfängern liefert bereits 3 Angriff. |
| `Communication Services` | Widerhall 2 → 2 | Tausend Stimmen 2 → 2 | Ziehen ist ohne Hand-/Deckregeln nicht genauer bepreisbar; Gabe auf insgesamt zwei Auslösungen begrenzen. |
| `Consumer Discretionary` | Glück 1 → 1 | Volksfest 2 → 3 | Bei fairer Münze sind erwartete Körperpunkte X; Aura wie Sternennetz, aber für alle Völker. |
| `Energy` | Glut 1,5 → 1,5 | Flächenbrand 3 → 4 | Gezielter Sofortschaden ist flexibel. 3 Ziele bedeuten 3 Gesamtschaden plus mögliche Mehrfachentfernung; 5 Ziele gesondert prüfen. |
| `Financials` | Zins 2 → 2,5 | Schatzkammer 2 → 3 | Zins je Folgezug nur temporär nutzbarer Kristall, kein dauerhaft erhöhtes Maximum; Gabe nur an den ersten zwei Zugenden, jeweils höchstens +2 Leben. |
| `Industrials` | Baukunst 1 → 1 | Fließband 3 → 4 | Einmaliger Lebensbonus plausibel; Gabe nur zu Beginn der nächsten zwei eigenen Züge, je ein 1/1-Werkling. Ohne Platz keine spätere Nachlieferung. |
| `Health Care` | Heilung 1,5 → 1,5 | Wiedererblühen 2 → 3 | Heilung nur zu Beginn der nächsten zwei eigenen Züge, nur bis zum Lebensmaximum; Wiederkehr löst Beschwörungseffekte nicht erneut aus. |
| `Consumer Staples` | Nähren 1,5 → 2 | Sauerteig 3 → 4 | Nähren auf höchstens zwei andere gewählte Wesen begrenzen; Gabe nur zu Beginn der nächsten zwei eigenen Züge. Sonst skalieren beide mit beliebig vielen Zielen. |
| `Utilities` | Schutzlicht 1 → 1 | Ewiges Feuer 3 → 3 | Schild fängt insgesamt X Schaden ab; Gabe heilt bis zum Maximum und nur an den nächsten zwei eigenen Zugenden. |
| `Real Estate` | Eingraben 1 → 1,5 | Zuflucht 3 → 4 | Leben nur an den nächsten zwei eigenen Zugenden ohne eigenen Angriff; Gabe insgesamt einmal, danach verbraucht. |
| `Materials` | Panzer 2 → 2,5 | Kristallherz 2 → 3 | Wiederholter Schutz hängt von Trefferzahl ab. 0/3 plus Wächter entsprechen schon 4 Körper-/Merkmalpunkten, verspäteter Nutzen rechtfertigt einen Abschlag. |
| `Paktvolk` | Blutpakt 1 → 2 | Höllenpforte 2 → 3 | Textkandidat: anderes eigenes Wesen **erschöpfen statt verletzen**, dann X Karten ziehen; nur spielbar mit bereitem Ziel. Gabe auf zwei Auslösungen begrenzen. Das Erschöpfen braucht erst eine definierte Matchregel. |
| `Artefakte` | Lernen 1,5 → 1 | Erwachen 2 → 3 | Lernen liefert verzögert höchstens X Angriff; 1,5 ist gegenüber sofortigem Baukunst eher teuer. Kopie nur einer Fähigkeit bis Stufe 1, keine Gabe und keine erneuten Beschwörungsauslöser. |

Zusätzliche konkrete Pfade:

- `/merkmale/Wachstum/punkte`: **2 → 3** als konservativer Startwert für verzögert vier
  Körperpunkte; `/merkmale/Heimvolk/text`: zunächst **rein kosmetische Zugehörigkeit,
  kein +1 Leben**. Nur den Preis des unberechneten Heimbonus zu ändern würde nichts lösen.
- `/seltenheit/4/bonus`: **2 → 1**, `/seltenheit/5/bonus`: **3 → 1**,
  `/seltenheit/5/stufe`: **2 → 1** für eine separate Kurvenvariante mit weniger teuren
  Spitzen. Formen, Namen und Völker beibehalten. Erst nach Vergleich mit dem
  Preiskandidaten oben beurteilen; dessen höhere Effektpreise können Kosten wieder erhöhen.
- `/arten/Consumer Finance/formen`: **["Paktfunke", "Siegelhüter", "Bundwächter"]**;
  `/voelker/Paktvolk/bild`: freiwillige Bündnisse aus der Glut der Esse statt Schulden.
- `/kurve/max_kosten` zunächst **10 belassen**. Bloß auf 8 zu kappen schrumpft die
  Körper und versteckt die Kostenkonzentration; die Ursache bleibt bestehen.
- `/booster` unverändert als Vorschlag markieren. Keine Zahlung, keine täglichen
  Pflichtpakete, keine verfallenden Ansprüche. Das ist noch keine implementierte Vergabe.

Alle Änderungen lassen sich an bestehenden Regelpfaden ausdrücken; kein hartkodierter
Preis gehört in `forge.py`. Ein späterer Kampfkern muss dieselbe begrenzte Semantik
ausführen. Unausgewertete neue JSON-Schalter wären jetzt irreführend.

### Hootlet und die 10-Kristall-Spitze

| Heimaffinität | Karten | Ø Kristalle | Kosten 1–3 | Kosten 10 | Mythisch |
|---|---:|---:|---:|---:|---:|
| Puffling | 101 | 5,525 | 22 | 4 | 14 |
| Starlet | 96 | 5,333 | 25 | 6 | 26 |
| Wyrmling | 101 | 5,287 | 18 | 3 | 22 |
| Fennec | 108 | 5,241 | 25 | 4 | 9 |
| Gloop | 110 | 5,609 | 21 | 2 | 19 |
| Hootlet | 100 | 6,860 | 14 | 23 | 33 |

42/616 = **6,8 %** liegen bei 10 Kristallen; 38 davon sind Mythisch. Hootlet stellt
23/42 der Spitzen und hat 33 % Mythische im eigenen Pool. Gleiche Poolgröße bedeutet
also keine gleich gute Startkurve. Alle Wesen dürfen alle Karten spielen, deshalb ist
das kein Zugangsausschluss. Der kostenlose Heimbonus würde aber die ungleichen Kurven
spielmechanisch relevant machen. Ohne ihn bleibt zunächst eine thematische Affinität.

Ob 42 Spitzenkarten zu viel sind, hängt von Kristallwachstum, Startressourcen, Deckgröße,
Zugdauer, Ziehen und gewünschter Spieldauer ab. Ohne solche Regeln ist weder „zu viele“
noch „passt“ belegt. Für einen Versuch identische, frei verfügbare Testdecks pro Wesen
vergleichen: Anteil spielbarer Starthände, Züge ohne sinnvolle Aktion, erstmalige
Nutzbarkeit von Kosten 10 und Wiederholungseffekt-Ketten. Keine Deckzahl als Produktregel
erfinden. Booster mit 6 % Mythisch im letzten Platz sind von der Poolhäufigkeit unabhängig;
ein einzelner solcher Treffer benötigt im Mittel rund 16,7 Pakete, ohne Garantie für eine
bestimmte Karte. Seltenheit ist hier auch keine gemessene tatsächliche Verfügbarkeit.

## Passung zu Lore, Vision und Itoeva 2

Die sechs Affinitäten haben konkrete Anker in `app-sim/src/main/res/values-de/strings.xml`:
Puffling liefert und baut (`lore_puffling_1/3`), Starlet zieht Pflanzen groß
(`lore_starlet_1/5`), Wyrmling sammelt und arbeitet an der Esse (`lore_wyrmling_2/3`),
Fennec hält Licht und gräbt (`lore_fennec_1–3`), Gloop lebt mit Backstube, Ton und Moor
(`lore_gloop_1/3/5`), Hootlet beobachtet Sterne und spielt Windharfe
(`lore_hootlet_1/3`). Leuchtfeuer, Kräuter-, Ernte-, Werk-, Bau- und Erzvolk passen direkt.
Glimmerwerk und denkende Artefakte sind eine mögliche Erweiterung der Sternwarte,
keine bereits belegte KI-Zivilisation. Ebenso sind Dämonen aus der Esse neue Lore,
keine nachgewiesene bestehende Beziehung. 67 dreistufige Arten sind ein konsistentes
Motivangebot, noch kein kanonischer Kampfklassenkatalog.

Gloops Moorflammen sind plausibel; dauernder Brand und Blutopfer passen weniger zu
seinem ruhigen, gestaltenden Charakter. Wyrmlings offene Sammlung („Anfassen darfst du
alles“) trägt ein Hortmotiv, aber keine Schuldwirtschaft. „Schuldendämon“, „Schuldenfürst“
und „bezahlt wird später“ setzen einen unnötigen strafenden Akzent. Fantasy-Konflikt
ist dadurch nicht pauschal verboten: freiwillige, begrenzte Matchkosten sind von Verlust
des eigenen Spielstands zu unterscheiden. Die konkreten Folgen und Texte brauchen eine
bewusste Regelentscheidung; sie dürfen nicht auf Reminder oder Beziehung durchschlagen.

`Vision.md` fordert sanfte, additive Erfahrung. `ITOEVA2.md` und der spätere
Evolutionseintrag vom 03.10. lösen für **game** die alte Reminderbindung ausdrücklich:
aktives Spiel ohne Reminder/Uhr. Deshalb wäre die alte „gemeinsame Reminder-Pipeline“
hier kein Argument gegen Karten. Das ist aber keine Freigabe für beliebige Ökonomie,
Kampf oder Bezahlung. `docs/itoeva2-worldbuilding.md` priorisiert Weltabnahme,
Begegnung und Ressourcenwege vor einem separaten kleinen Kampfprototyp; Raster,
Gruppe, Fähigkeiten und Klassen bleiben offen. Eine Engine wird hier nicht ausgewählt.

## Empfohlener nächster Schritt

1. **Zuerst ein eigener Export-Sicherungs-PR:** öffentliche Ausgabe per Feldliste begrenzen,
   vorhandene JSON/HTML/Markdown-Ausgaben bereinigen, Registerverlust vor jeder Ausgabe
   abfangen. Nur erfundene Testdaten; alle öffentlichen Dateitypen auf Identitäts- und
   Faktorlecks sowie Erhalt alter IDs testen. Entscheidung zum bereits veröffentlichten
   Verlauf gesondert behandeln.
2. Danach einen engineunabhängigen, versionierten Katalogvertrag vorschlagen
   (`schema_version`, Datenmonat, stabile ID, Edition, Status, Volk, Form, Farbe,
   strukturierte Effekte), einschließlich ruhender Karten und unbekannter Felder.
   Monatsänderungen/Sammlungsbesitz vorher ausdrücklich klären; Version fehlt derzeit
   im JSON. Bestehende Galerie als Ansicht verwenden.
3. Erst nach einer begrenzten Kampfregelentscheidung mit erfundenen Leuchtfeuer-Karten
   einen kleinen deterministischen Regelversuch vergleichen. Die Preiskandidaten oben
   gegen die unveränderten Regeln prüfen. Anschließend Engine-/Darstellungseignung
   bewerten und gegebenenfalls **ein Volk** mit drei Formen plus austauschbarem
   Animations-/Videoplayer umsetzen. Keine 39 Vorlagen vor dieser Prüfung.

## Gradle und Abschluss

Der erste Gradle-Aufruf fand ohne SDK-Umgebungsvariable keinen SDK-Pfad. Wiederholung
mit den lokal vorhandenen Installationen (PowerShell):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\marti\AppData\Local\Android\Sdk'
.\gradlew.bat :app-sim:testDebugUnitTest :app-sim:compileGameKotlin --console=plain
```

**TESTED BEHAVIOR:** `BUILD SUCCESSFUL in 6m 35s`, 64 Tasks ausgeführt.
`:app-sim:testDebugUnitTest`: **1.415 Tests in 146 Testsuiten**, keine Fehler,
Fehlschläge oder übersprungenen Tests (JUnit-XML ausgewertet).
`:app-sim:compileGameKotlin` erfolgreich. Vorhandene Compilerwarnungen sind keine
neuen Befunde dieser reinen Dokumentationsänderung.

`ITOEVA2.md` und `EVOLUTION.md` halten die zurückgestellte Integration fest.
Dieser PR ändert keine Laufzeit, Assets, Faktordaten oder private Zuordnungen.
Vollständiger Diff, lokale Markdown-Dateilinks, UTF-8 und `git diff --check` geprüft;
Ziel des PR ist `claude/kartenschmiede`.
