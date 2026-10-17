# Kartenschmiede

Übersetzt eine monatliche Faktor-Auswertung (Quality, Low Vola, Momentum, Value, Size, Composite
als Quintile, dazu Sektor und Industrie) in die Kartenbasis des Itoeva-Kartenspiels. Die Firmen
selbst tauchen im Spiel nicht auf: Sie liefern nur eine realistische, über Monate stabile
Verteilung von Werten, aus der Wesen der Itoeva-Welt werden.

Stand: Vorschlag vom 17.10.2026. Kampfregeln, Kartenspiel-Oberfläche und Booster-Bezug sind
**OPEN DECISION** (siehe `docs/itoeva2-worldbuilding.md`: Kampfklassen sind noch nicht entschieden).
Die Schmiede legt nur die Daten an, die ein solches Spiel braucht.

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

Wie der Vanilla-Test aus Hearthstone: Eine Karte mit K Kristallen hat genau **2K + 1 Wertpunkte**.
Angriff und Leben zählen je 1, Merkmale und Fähigkeiten haben einen Punktpreis (`rules.json`).
Die Schmiede rechnet aus den Faktoren Rohwerte, bestimmt daraus die Kosten (Value verschiebt),
und skaliert Angriff und Leben dann so, dass die Summe exakt passt. Das Verhältnis Angriff:Leben
bleibt das der Daten. Eine Firma mit starkem Momentum bleibt also angriffslastig, wird aber nie
billiger als ihre Kraft. Gleich effiziente Karten, verschieden geformt.

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

Was über Monate gilt:

- **Name und Karten-Id bleiben.** Sie hängen am Datenschlüssel (ISIN, sonst Ticker; Zweitgattungen
  als Alias) und stehen in `private/register.json`. Diese Datei ist das Gedächtnis der Schmiede,
  sie muss von Monat zu Monat erhalten bleiben (z. B. in Drive sichern).
- **Werte wandern** mit den Daten. Das Monatszeichen (*im Aufwind*, *unerschütterlich* …) zeigt den
  stärksten Faktor des Monats.
- **Entwicklung statt Austausch:** Steigt eine Firma in der Größe auf, wächst das Wesen in die
  nächste Form (Funkenkäfer → Siliziumspinne → Siliziumdrache).
- **Nichts geht verloren.** Fällt eine Firma aus den Daten, *ruht* ihre Karte mit den letzten
  Werten und bleibt in jeder Sammlung. Kommt sie zurück, erwacht sie wieder. (Vision: sanft statt
  strafend.)
- **Neue Industrie oder neuer Sektor** bricht den Lauf mit Hinweis ab. Dann einen Eintrag in
  `rules.json` ergänzen.

## Dateien

| Datei | Inhalt | Einchecken? |
|---|---|---|
| `rules.json` | alle Stellschrauben: Faktor-Tabellen, Punktpreise, Völker, 67 Arten, Booster | ja |
| `forge.py` | reine Mechanik, nur Standardbibliothek + openpyxl | ja |
| `test_forge.py` | Kurve, Monotonie, Folgemonat mit Wegfall/Neuzugang, keine Firmendaten im Ausgang | ja |
| `galerie_vorlage.html` | Kartengalerie, die `forge.py` mit Daten füllt | ja |
| `out/karten.json` | öffentliche Kartendaten ohne Firmen und ISINs | ja |
| `private/` | Excel, Register, Firmenübersicht, Galerie mit Firmen | **nein** (`.gitignore`) |

Das Repository ist öffentlich. Die Faktordaten und die Zuordnung Firma → Karte bleiben deshalb
in `private/`.

Tests: `python tools/card-forge/test_forge.py`
