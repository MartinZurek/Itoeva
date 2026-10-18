# Kartenschmiede

Übersetzt eine monatliche Faktor-Auswertung (Quality, Low Vola, Momentum, Value, Size, Composite
als Quintile, dazu Sektor und Industrie) in die Kartenbasis des Itoeva-Kartenspiels. Die Firmen
selbst tauchen im Spiel nicht auf: Sie liefern nur eine realistische, über Monate stabile
Verteilung von Werten, aus der Wesen der Itoeva-Welt werden.

Stand: Vorschlag vom 17.10.2026. Kampfregeln, Kartenspiel-Oberfläche und Booster-Bezug sind
**OPEN DECISION** (siehe `docs/itoeva2-worldbuilding.md`: Kampfklassen sind noch nicht entschieden).
Die Schmiede legt nur die Daten an, die ein solches Spiel braucht.

Die kritische Bewertung und die zurückgestellte Spieleintegration stehen in
[`PRUEFUNG.md`](PRUEFUNG.md). Der Folgeschritt sichert zunächst Exporte und Kartenidentitäten;
Kampfregeln und Preise werden dadurch nicht entschieden. Die anschließende ausdrückliche
Besitzentscheidung für Variante A steht im Katalogvertrag.

Der technische [Katalogvertrag v1](KATALOGVERTRAG.md) ist implementiert: `schema_version`
im Export, reiner Referenzleser in `katalog.py`, typisierte unveränderliche Modelle und
Validierung vor jedem Schreiben. Für erworbene Monatsausgaben hat Martin Variante A gewählt:
Monatswechsel ändern erworbene Ausgaben nicht automatisch; **gezielte Statkorrekturen
von Martin gelten auch für erworbene Karten**, während Form und Glanz erhalten bleiben.
`edition.py` und seine Zweimonatstests
sichern dies als engineunabhängiges Referenzmodell ab; eine App-Sammlung ist noch nicht eingebaut.

## Die Übersetzung

| Faktor | wird zu | Regel |
|---|---|---|
| Momentum | **Angriff** | Q1 → 5 … Q5 → 1 (Rohwert) |
| Quality | **Leben** | Q1 → 6 … Q5 → 2 (Rohwert) |
| Value | **Kristallkosten** | verschiebt das Wesen auf der Kurve: Q1 −1 Kristall (*Schnäppchen*), Q5 +1 mit *Wachstum* |
| Low Vola | **Temperament** | Q1 *Wächter* (muss zuerst angegriffen werden), Q4 *Flackernd*, Q5 *Ungestüm* (greift sofort an, schwankt) |
| Size | **Seltenheit** | Q1 Gewöhnlich, Q2 Ungewöhnlich, Q3 Selten, Q4 Episch, Q5 **Mythisch** |
| Composite | **Glanz** | Q1 schimmert (Holo), rein kosmetisch |
| Sektor | **Volk** | 13 Völker, je einem der sechs Wesen als Heimvolk zugeordnet (zwei davon aus Industrien gebildet, siehe unten) |
| Industrie | **Art + Rolle** | 67 Arten in drei Formen (Jung, Reif, Ur), Rolle Wächter/Stürmer/Schütze/Helfer |

Quintil 0 (fehlender Wert) wird als neutrales Q3 behandelt. Mehrere Aktiengattungen einer Firma
(GOOG/GOOGL, FOX/FOXA …) werden ein Wesen.

### Balance: jede Karte liegt auf der Kurve

Wie beim Vanilla-Test ist **2K + 1 Wertpunkte** das Budget für eine Karte mit K Kristallen.
Angriff und Leben zählen je 1, Merkmale und Fähigkeiten haben einen Punktpreis (`rules.json`).
Die Schmiede rechnet aus den Faktoren Rohwerte, bestimmt daraus die Kosten (Value verschiebt),
und skaliert Angriff und Leben auf dieses Budget. Durch die Rundung auf ganze Körperwerte
sind bis zu 0,5 Punkte Abweichung möglich. Das Verhältnis Angriff:Leben orientiert sich an den
Daten. Das ist ein rechnerischer Vergleich, kein Nachweis gleicher Effizienz im Kampf.

Seltenere Karten bekommen mehr Wertpunkte und eine Volksfähigkeit (Stufe X wächst mit der
Seltenheit); Mythische zusätzlich eine **Gabe**. Das macht sie teurer, nicht effizienter.

### Völker und Wesen

| Wesen | Heimvölker (Sektor) | Warum |
|---|---|---|
| Puffling | Werkvolk (Industrials) | Bote mit Werkbank |
| Starlet | Kräutervolk (Health Care), Erntevolk (Consumer Staples) | zieht Pflanzen groß |
| Wyrmling | Hortbrut (Banken, Versicherer, Finanzdienste), **Paktvolk** (Dämonen: Capital Markets, Consumer Finance) | Drache mit Nest voller Gesammeltem; Dämonen aus der Glut seiner Esse, die Pakte schließen |
| Fennec | Leuchtfeuer (Utilities), Bauvolk (Real Estate), Erzvolk (Materials) | Leuchtfeuer, Bau, gräbt |
| Gloop | Gauklervolk (Cons. Discretionary), Irrlichter (Comm. Services), Moorglut (Energy) | Sumpf, Backstube, Chaos |
| Hootlet | Glimmerwerk (Halbleiter, Hardware, Elektronik), **Artefakte** (KI-Volk: Software, IT-Services, Interactive Media) | Sternwarte: Mechanik und beseelte Werkzeuge, die zu denken begonnen haben |

Heimvolk ist eine Affinität (+1 Leben), keine Sperre: **alle Wesen können alle Karten spielen.**
Die Pools sind bewusst ähnlich groß (96–110 Karten). Eine Art kann per `"volk"` in `rules.json` einem anderen Volk als ihrem Sektor zugeordnet werden; so entstehen Dämonen und Artefakte.

**Namen** entstehen aus dem Klang des Volkes in drei Bauformen, häufen gleiche Endungen nicht und prüfen eine Sperrliste (`rules.json` → `namen.sperre`, z. B. keine Anklänge wie „-dolf“). Ein vergebener Name bleibt für immer.

## Monatlicher Lauf

1. Neue Excel nach `tools/card-forge/private/` legen (Spaltennamen wie bisher).
2. `python tools/card-forge/forge.py --eingabe tools/card-forge/private/<datei>.xlsx --monat 2026-11`
3. `out/monatswandel-2026-11.md` lesen: wer erwacht ist, wer sich entwickelt hat, wer ruht,
   plus Balancebericht (Kristallkurve je Seltenheit, Pools je Wesen).
4. Optional `--galerie-mit-firmen` schreibt eine Galerie mit Firmennamen nach `private/`.
   Sie enthält zusätzlich die Faktorübersicht aktiver Karten. Die öffentliche Galerie wird
   trotzdem jedes Mal mit dem aktuellen, bereinigten Katalog geschrieben.

Was über Monate gilt:

- **Name und Karten-Id bleiben.** Sie hängen am Datenschlüssel (ISIN, sonst Ticker; Zweitgattungen
  als Alias) und stehen in `private/register.json`. Diese Datei ist das Gedächtnis der Schmiede,
  sie muss von Monat zu Monat erhalten bleiben (z. B. in Drive sichern).
  Fehlt sie bei vorhandenen Ausgaben, ist sie unlesbar oder widerspricht sie den alten
  Karten-IDs/Namen, bricht der Lauf **vor dem Schreiben** ab. Auch ein fehlender Katalog
  neben alten Berichten/Galerien sowie ein zurückgesetzter ID-Zähler führen zum Abbruch.
  Das Register aus einer passenden **privaten** Sicherung wiederherstellen; nicht leer neu anlegen.
- **Werte wandern** mit den Daten. Das Monatszeichen (*im Aufwind*, *unerschütterlich* …) zeigt den
  stärksten Faktor des Monats.
- **Monatsänderungen:** Im aktuellen Katalog können Formen mit Size auf- und absteigen;
  auch Werte, Seltenheit und Glanz ändern sich. Für eine **erworbene Ausgabe** gilt dagegen
  die präzisierte Variante A: vor automatischen Monatsänderungen geschützt, aber gezielt
  in Angriff, Leben und Kristallkosten korrigierbar; Form und Glanz bleiben erhalten. Der
  Referenznachweis ist implementiert, eine App-Sammlung noch nicht.
- **Nichts geht verloren.** Fällt eine Firma aus den Daten, *ruht* ihre Karte mit den letzten
  Werten im Katalog. Kommt sie zurück, erwacht sie mit derselben ID und demselben Namen wieder.
- **Neue Industrie oder neuer Sektor** bricht den Lauf mit Hinweis ab. Dann einen Eintrag in
  `rules.json` ergänzen.

## Dateien

| Datei | Inhalt | Einchecken? |
|---|---|---|
| `rules.json` | alle Stellschrauben: Faktor-Tabellen, Punktpreise, Völker, 67 Arten, Booster | ja |
| `forge.py` | reine Mechanik, nur Standardbibliothek + openpyxl | ja |
| `katalog.py`, `KATALOGVERTRAG.md` | Referenzleser/-export, Formatregeln und beschlossene Variante A | ja |
| `edition.py`, `test_edition.py` | Referenzmodelle und synthetischer Erhalt erworbener Monatsausgaben | ja |
| `statkorrektur.py`, `test_statkorrektur.py` | gezielte Änderung erstellter und erworbener Karten ohne Verlust von Form und Glanz | ja |
| `fixtures/katalog-v1.json`, `test_katalog.py` | handgeschriebene erfundene Karten und Vertragstests | ja |
| `test_forge.py` | Kurve, Monotonie, Folgemonat mit Wegfall/Neuzugang, keine Firmendaten im Ausgang | ja |
| `test_galerie.cjs` | echtes Galerie-Script mit DOM-Stubs: Details ohne Rohfaktoren und sichere private Zusatzansicht | ja |
| `galerie_vorlage.html` | Kartengalerie, die `forge.py` mit Daten füllt | ja |
| `out/karten.json` | öffentliche Spielwerte ohne Firmen, Kennungen oder originale Faktorquintile | ja |
| `private/` | Excel, Register, Firmenübersicht, Galerie mit Firmen | **nein** (`.gitignore`) |

Das Repository ist öffentlich. Die Faktordaten und die Zuordnung Firma → Karte bleiben deshalb
in `private/`.

## Grenze der öffentlichen Ausgabe

`OEFFENTLICHE_KARTENFELDER` in `forge.py` erlaubt nur Spiel- und Darstellungsfelder.
Auch `faehigkeit` und `gabe` haben feste Feldlisten. Sie gelten erneut für alte, ruhende
Karten; unbekannte interne Zusatzfelder werden verworfen. Öffentliche JSON- und
HTML-Ausgabe enthalten weder `herkunft` noch eine Firma-Karte-Zuordnung. Der öffentliche
Monatsbericht nennt bei zusammengelegten Gattungen nur noch die Anzahl der Gruppen.
Spielwerte sind weiterhin aus den Eingabedaten abgeleitet; die Bereinigung verspricht
keine vollständige Nicht-Rückführbarkeit der Spielwerte.

Private und öffentliche Ausgabepfade dürfen sich nicht überlappen; die Eingabetabelle darf
nicht im öffentlichen Ausgabeordner liegen. Leere Eingaben brechen ab, bevor alle Karten
versehentlich ruhend werden. Gleichnamige Firmen werden weiterhin zusammengefasst;
**leere** Namen sind dagegen kein Grund, verschiedene Identitäten zusammenzufassen.

Die Dateien werden einzeln atomar ersetzt, das Register zuerst. Scheitert danach eine
öffentliche Ausgabe, kann der Lauf mit denselben Daten und demselben Monat wiederholt
werden; bereits vergebene Identitäten bleiben im Register erhalten. Die Dateigruppe ist
keine gemeinsame Transaktion; parallele Schmiedeläufe auf denselben Pfaden sind nicht
unterstützt. Das private Register bleibt sicherungsbedürftig.

Die eingecheckten Oktober-Ausgaben wurden ohne neue Faktoreingabe bereinigt: 616 IDs,
Namen und Spielwerte bleiben gleich. Rohquintile und Kennungslisten wurden entfernt und
die Galerie aus dem bereinigten Bestand neu aufgebaut. Bereits veröffentlichte frühere
Git-Versionen werden dadurch nicht gelöscht; eine Historienumschreibung ist nicht Teil
dieses Schritts.

Tests (Python benötigt `openpyxl`; der zusätzliche Script-Test nur Node.js):

```text
python tools/card-forge/test_forge.py
python -m unittest discover -s tools/card-forge -p "test_*.py" -v
python tools/card-forge/katalog.py tools/card-forge/out/karten.json
node tools/card-forge/test_galerie.cjs
```

Die Python-Tests verwenden ausschließlich erfundene Eingabezeilen und prüfen auch die
eingecheckten öffentlichen Artefakte. Der Node-Test führt alle 616 vorhandenen
Kartendetails und die optionale private Ansicht aus; Layout und echte Browserbedienung
werden durch DOM-Stubs nicht geprüft.

`katalog.py` liest und prüft ausschließlich die angegebene öffentliche Datei, meldet
Version, Monat, Kartenanzahl und ihren bytegenauen SHA-256. Es ändert keine Dateien und
benötigt kein `openpyxl`. Unversionierte Altbestände übernimmt nur der ausdrücklich dafür
aufgerufene Importpfad; unbekannte Versionen oder defekte Kataloge überschreibt die
Schmiede nicht. Weitere Details zu Pflichtfeldern, ruhenden Karten und Referenzen stehen
in `KATALOGVERTRAG.md`.
