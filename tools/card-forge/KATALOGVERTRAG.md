# Katalogvertrag v1 und erhaltene Monatseditionen

Stand: 18.10.2026. **Implementiert** sind das versionierte öffentliche Dateiformat,
ein reiner Python-Referenzleser, die Prüfung vor dem Export und die Übernahme des
bisherigen unversionierten Formats. Es gibt keine Android-, Compose- oder Engine-Abhängigkeit.
**Produktentscheidung vom 17.10.2026:** Martin hat ausdrücklich Variante A gewählt:
Erworbene Monatsausgaben mit damaligen Werten, Form und Glanz dauerhaft erhalten.
**Anschließend ausdrücklich präzisiert:** Gezielte Statkorrekturen von Martin gelten auch
für erworbene Karten. Automatische Monatsänderungen bleiben getrennt; Formen und Glanz
bleiben erhalten. Der Schutz vor Monatsänderungen ist kein Verbot bewusster Korrekturen.
`edition.py` beweist diesen Erhalt als engineunabhängiges Referenzmodell mit erfundenen
Zweimonatstests und Snapshot-Dateien. Eine App-Sammlung, Vergabe, Duplikatwirkung und
Kampfzulässigkeit sind damit noch nicht implementiert oder entschieden.

## Was v1 löst

Ein Kartenbuch, ein späterer Kotlin- oder Godot-Leser und Offline-Werkzeuge sollen dieselben
öffentlichen Daten verstehen können. Sie sollen Formatversion und Datenstand unterscheiden,
ruhende Karten nicht verlieren und bei unbekannten Versionen sicher abbrechen. Die
lesbaren Kampftexte dürfen dabei nicht versehentlich zu einer freigegebenen Kampf-Engine werden.

Quelle des Vertrags ist [`katalog.py`](katalog.py). Unveränderliche Referenzmodelle:
`Katalog`, `Karte`, `Faehigkeit`, `Gabe`. Ein handgeschriebenes Beispiel mit vollständig
erfundenen Spielwerten steht in [`fixtures/katalog-v1.json`](fixtures/katalog-v1.json).
Der Referenzleser benötigt nur die Python-Standardbibliothek, keine Schmiede und kein `openpyxl`.

## Öffentliche Datei

JSON, UTF-8 ohne BOM. Wurzel:

| Feld | Vertrag |
|---|---|
| `schema_version` | Ganzzahl **1**; Formatversion, kein Datum und keine Regelbalance-Version |
| `monat` | Gültiger Kalendermonat `JJJJ-MM`; Stand dieses gesamten Katalogs |
| `anzahl` | Nichtnegative Int32-Ganzzahl; exakt die Länge von `karten`, einschließlich ruhender Karten |
| `karten` | Geordnete Liste vollständiger Karten; IDs und Namen ohne Beachtung der Großschreibung eindeutig |

Alle vier Felder sind Pflicht. `1.0`, `true`, `"1"`, eine fehlende Version oder eine
unbekannte Version gelten nicht als v1. Ein leerer Katalog ist als Datenformat lesbar;
die Schmiede lehnt weiterhin eine leere Faktoreingabe ab, um keinen versehentlichen
Monatswechsel auszulösen.

Die Kartenfelder bleiben gegenüber den bereinigten Oktober-Daten unverändert:

| Felder | Typ und Bedeutung |
|---|---|
| `id` | Nichtleerer, stabiler Schlüssel `K` mit mindestens vier Ziffern, numerischer Teil größer null. Als **opaken String** speichern; keine Firmenkennung daraus ableiten. |
| `name`, `art`, `titel` | Nichtleere Unicode-Texte für Anzeige und Monatszeichen |
| `volk`, `wesen` | Nichtleere Bezeichner für Motiv/Affinität; kein Spielzugang oder Besitzbeleg |
| `farbe`, `hex` | Anzeigename und RGB-Farbe im Format `#RRGGBB`, ohne Alpha |
| `rolle`, `reihe` | Nichtleere beschreibende Bezeichner des bisherigen Prototyps; **keine** festgelegte Kampfaufstellung |
| `form` | `Jungform`, `Reife Form` oder `Urform` |
| `seltenheit` | `Gewöhnlich`, `Ungewöhnlich`, `Selten`, `Episch` oder `Mythisch` |
| `kosten`, `angriff`, `leben` | Positive Int32-Ganzzahlen; das Format prüft Typ/Grenzen, keine Spielfairness oder 2K+1-Kurve |
| `merkmale` | Liste nichtleerer Texte, auch leer zulässig |
| `faehigkeit` | Pflichtfeld, entweder `null` oder Objekt aus `name`, positivem Int32-`x` und `text` |
| `gabe` | Pflichtfeld, entweder `null` oder Objekt aus `name` und `text` |
| `glanz` | Echter JSON-Boolean, kein numerischer Ersatz |
| `wertpunkte` | Endliche nichtnegative JSON-Zahl bis zum Int32-Maximum; kein `NaN`/`Infinity` |
| `status` | `aktiv` oder `ruhend`; beide werden geladen und angezeigt |
| `monat` | Letzter Auswertungsmonat **dieser Karte**, nicht automatisch der Katalogmonat |

Alle aufgeführten Kartenfelder sind Pflicht. Der Kartenmonat liegt nie nach dem
Katalogmonat; bei aktiven Karten ist er gleich. Bei ruhenden Karten kann er älter sein,
weil deren Werte eingefroren bleiben. Ein Beispiel: Katalog November, ruhende Karte mit
Oktober-Werten. Auch bei einem erneuten Lauf im selben Monat kann eine Karte ruhend werden.

Form, Seltenheit, Volk und Regeltexte sagen **nichts** über erworbene Freischaltungen aus.
Die vorgeschlagenen 0/3-Erzsplitter oder 1/1-Werklinge sind keine neuen Katalogkarten;
ein späteres Token-/Kampfmodell wird durch diesen Vertrag nicht eingeführt.

## Leseregeln und Versionswechsel

`lese_katalog(text)` lädt vollständige aktive und ruhende Karten. Fehlende Pflichtfelder,
falsche Typen, doppelte IDs/Namen, abweichende Anzahl und widersprüchliche Monate führen
zu `KatalogFehler`. Doppelte JSON-Schlüssel und nicht endliche Sonderzahlen werden bereits
beim JSON-Lesen abgewiesen. Fehlermeldungen nennen Feldpfade, keine Eingabewerte.

Unbekannte **Zusatzfelder** auf Wurzel-, Karten- und Effektebene werden ignoriert und
beim erneuten Serialisieren nicht ausgegeben. Das ist Vorwärtskompatibilität für
optionale Ergänzungen, keine Erlaubnis für private Exportfelder. Die Schmiede gibt
weiterhin ausschließlich die öffentliche Feldliste aus. Neue Völker/Wesen oder
beschreibende Fähigkeiten brauchen keinen hartkodierten Engine-Katalog; Texte bleiben
darstellbar. Neue Werte für `status`, `form` oder `seltenheit` benötigen dagegen eine
bewusste Vertragsanpassung, weil v1 deren Bedeutung beschränkt.

Eine brechende Änderung an Pflichtfeldern, Typen oder bestehender Bedeutung verlangt eine
neue `schema_version` und einen eigenen getesteten Übergang. Ein v1-Leser darf v2 nicht
als v1 interpretieren. Er darf eine bestehende lokale Datei bei einem Lesefehler nicht
mit einem vermeintlich leeren Katalog überschreiben. Die Schmiede prüft deshalb vor
Register- oder Ausgabeschreibzugriffen. Eine spätere UI braucht eine verständliche
Fehleransicht und muss den bisherigen lesbaren Stand bewahren.

Das bisherige Format ohne Version wird **nur explizit** über
`altbestand_uebernehmen(daten)` oder `lese_katalog(text, erlaube_altbestand=True)` angehoben.
Die Schmiede benutzt diese Option für ihren alten Ausgabeordner. Eine vorhandene
unbekannte Version wird auch mit dieser Option abgewiesen. Die Übernahme validiert
dieselben Karten, entfernt unbekannte/private Zusatzfelder und ändert keine Spielwerte.
Es ist eine Dateiformatübernahme, keine App-Spielstandmigration und keine neue Sammlung.

## Eine genaue Datei referenzieren

`katalog_sha256(raw_bytes)` prüft v1 und liefert SHA-256 über die **unveränderten UTF-8-Bytes**
dieser Datei. Auch andere Zeilenenden, Einrückungen oder ein zusätzlicher Zeilenumbruch
ändern die Referenz. Das ist absichtlich kein engineübergreifend kanonisierter Hash eines
beliebig neu serialisierten JSON-Objekts. Ein fremder Leser kann die empfangene Originaldatei
direkt hashen; er muss dafür keine Python-Zahlendarstellung nachbauen.

`katalog_json(katalog)` ist der reproduzierbare Referenzexport mit einem abschließenden
LF-Zeilenumbruch. Die lokale `.gitattributes` hält `out/*.json` auch beim Windows-Checkout
auf LF. Die Referenz identifiziert ein technisches Artefakt. Sie ist weder ein
Besitznachweis noch eine Signatur, eine Freischaltung oder automatisch eine Spiele-Edition.
Auch `schema_version` und `monat` allein identifizieren keine unveränderliche Edition:
derselbe Monat kann mehrfach mit korrigierten Daten geschmiedet werden.

## Monatseditionen und Besitz: Variante A ist entschieden

**Ausdrücklich gewählt vom Nutzer am 17.10.2026:** Erwerb soll eine bestimmte freigegebene Ausgabe
einer Karte festhalten. Eine spätere Monatsdatei ergänzt weitere Ausgaben, ersetzt aber
keine bereits erworbene Form, keinen Glanz und keine dauerhaft erworbene Darstellung.
Wegfall aus den Faktordaten entfernt keinen Besitz. Eine Rückkehr verknüpft sich über
dieselbe Karten-ID. Booster werden nur in der Welt verdient; Bezahlung bleibt offen.

Das Referenzmodell `ErworbeneAusgabe` hält `karten_id` plus Referenz auf einen bewahrten
`KatalogSnapshot` mit vollständigem Inhalt. Der Snapshot selbst muss lokal oder im ausgelieferten
Inhalt vorhanden sein; ein Hash allein kann ihn **nicht** wiederherstellen. Dieselbe ID
kann in verschiedenen Snapshots unterschiedliche Monatswerte besitzen. Katalogsicht und
Besitzsicht bleiben getrennt: alle Karten ansehen ist keine Vergabe aller Karten.

`KatalogSnapshot` validiert und bereinigt die Eingabe über den Referenzleser, speichert
dann den öffentlichen Referenzexport und berechnet **dessen** Datei-SHA256. Unbekannte
private Zusatzfelder werden auch im Archiv nicht behalten. Die Referenz gehört zu den
gespeicherten Bytes, nicht zu möglicherweise anders formatierten Eingangsbytes. Mehrere
erworbene Karten können denselben Snapshot teilen; ein späterer Store muss ihn nur einmal
je Hash aufbewahren. Das heutige Modell hat keine Vergabe- oder Sammlungs-UI.

| Möglichkeit | Wirkung | Bewertung |
|---|---|---|
| **A: Erworbene Ausgabe erhalten** | Monatswechsel ersetzen keine erworbene Ausgabe; gezielte Statkorrekturen sind erlaubt, Form und Glanz bleiben | **Gewählt und präzisiert**, Erhalt und Korrektur im Referenzmodell getestet; spätere Kampfzulässigkeit bleibt offen |
| B: Aktuelle Werte, erworbene Darstellung erhalten | Monatswerte wechseln, freigeschaltete Formen/Glanz bleiben nutzbar | Nicht gewählt; könnte erworbene Spielwerte senken |
| C: Vorerst nur Katalog | Monatsdatei zeigt den aktuellen Bestand, es gibt keinen Besitz | Die App besitzt weiterhin nur diesen Entwicklungsstand; A ist jetzt der beschlossene spätere Besitzvertrag |

Beispiel für A: Im Oktober wird eine mythische Urform mit Glanz erworben. Im November
zeigt der Katalog dieselbe ID als gewöhnliche Jungform ohne Glanz. Die erworbene
Oktoberausgabe bleibt erhalten. Keine automatische Rückstufung, kein Duplikatverlust und
kein Verfall durch Abwesenheit. Die aktuelle Katalogansicht darf trotzdem den Novemberstand
zeigen. Dieser Erhalt ist im reinen Referenzmodell getestet, noch kein Spielerlebnis am Gerät.

## Bewusste Statkorrekturen sind möglich

`statkorrektur.korrigiere_stats(katalog, karten_id, ...)` ändert gezielt Angriff, Leben
und/oder Kristallkosten einer bereits erstellten Karte. `ErworbeneAusgabe.mit_statkorrektur(...)`
wendet denselben Vorgang auf eine bereits erworbene Ausgabe an. Das ist ein ausdrücklich
angeforderter Korrekturschritt, kein automatischer Monatsimport und kein neuer Erwerb.

```python
from statkorrektur import korrigiere_stats

katalog = korrigiere_stats(katalog, "K0001", angriff=4, leben=5, kosten=3)
erworbene_ausgabe = erworbene_ausgabe.mit_statkorrektur(angriff=4, leben=5, kosten=3)
```

IDs, Namen, Form, Glanz, Seltenheit, Effekte und Monatsangaben bleiben unverändert.
Nicht angegebene Stats und andere Karten werden nicht verändert. `wertpunkte` folgen
der Änderung von Angriff und Leben; die bestehenden Effektpreise bleiben dabei gleich.
Eine Kostenänderung skaliert keine anderen Werte automatisch um. Eine bewusst korrigierte
Karte kann deshalb neben der ursprünglichen 2K+1-Kurve liegen; das ist gesondert zu prüfen.

Die Operation gibt ein neues validiertes Modell zurück. Der aufrufende Code muss dieses
Ergebnis übernehmen und gegebenenfalls speichern; nur den Aufruf zu ignorieren ändert
keinen Bestand. Für mehrere erworbene Ausgaben derselben Karten-ID wird die gezielte
Korrektur auf jede betroffene Ausgabe angewendet, ohne deren jeweilige Darstellung zu
ersetzen. Eine automatische Verteilung auf eine App-Sammlung gibt es noch nicht.

Die korrigierte Ausgabe erhält eine neue Snapshot-Dateireferenz, behält aber dieselbe
Karten-ID. Der aufbewahrte alte Snapshot bleibt als Historie lesbar. Unveränderliche
Snapshot-Bytes bedeuten damit nachvollziehbare Fassungen, keine unveränderlichen Spielwerte.
Eine wiederholte gleiche Korrektur ist idempotent; ungültige Werte lassen das alte Modell
unangetastet. Wiederherstellung aus dem korrigierten Snapshot behält die neuen Stats.

Ein späterer Monatsimport überschreibt auch eine bewusst korrigierte erworbene Ausgabe
nicht automatisch. Änderungen an `rules.json` beziehungsweise ein neuer Schmiedelauf
ändern zunächst den aktuellen Katalog; eine Korrektur bereits erworbener Ausgaben muss
als gezielter Vorgang übernommen werden. Es wird hier keine globale Korrekturhistorie,
App-Verteilung oder neue Vergabeentscheidung eingeführt.

Vor Sammlung oder Kampfkern bleiben ausdrücklich zu entscheiden:

1. Wie freigegebene Monatsausgaben von Korrekturen desselben Monats unterschieden werden,
   wer sie freigibt und welche Snapshots dauerhaft ausgeliefert/aufbewahrt werden.
2. Was mit erneutem Erwerb derselben Ausgabe passiert. Empfehlung: kein Zwang zum Tauschen,
   Vernichten oder Bezahlen; konkrete Duplikatwirkung bleibt offen.
3. Welche Ausgabe später im Kampf gültig ist. Alte starke Werte dürfen nicht unbeabsichtigt
   einen Vorteil erzeugen; Matchbalance wird nicht durch den Besitz- oder JSON-Vertrag gelöst.

Die Referenzmodelle und Tests beweisen: zwei Monatsdateien mit derselben Karten-ID sind
getrennt lesbar; die erworbene Oktoberausgabe bleibt beim Novemberwechsel, beim Ruhen und
nach Wiederherstellung aus ihrer Snapshot-Datei erhalten. Der historische Status im
Snapshot ist keine heutige Kampfzulassung. Ein `ErworbeneAusgabe`-Objekt hält einen bereits
gewährten Erwerb fest; es ist selbst keine Berechtigung zum Erwerb. Es gibt keine automatische
Vergabe beim Laden eines Katalogs, keine Liste vergebener Karten und keine Duplikatregel.
Diese Bausteine ersetzen keinen getesteten App-Spielstand mit Lebenszyklus-/Fehlerbehandlung.

## Nächster begrenzter Schritt

Variante A samt bewussten Statkorrekturen ist entschieden und mit synthetischen Tests abgesichert.
Vor einer App-Sammlung Freigabe/Korrekturen und Duplikate konkretisieren. Kampf bleibt
ein eigener Vorschlag: höchstens zwei gegen zwei, Leuchtfeuer als erster vollständig beschriebener Versuchsbestand,
begrenzte Effekte und Vergleich der Preiskandidaten aus `PRUEFUNG.md`. Das ist erst nach
Kampfregelentscheidung umzusetzen. Engine, Raster, Gruppe und Kampfklassen bleiben offen.

```text
python -m unittest discover -s tools/card-forge -p "test_*.py" -v
python tools/card-forge/katalog.py tools/card-forge/out/karten.json
node tools/card-forge/test_galerie.cjs
```

Die Tests verwenden erfundene Eingaben plus den bereits bereinigten öffentlichen Bestand.
Noch kein Android-/Godot-Parser, keine App-Sammlung, kein Kampf und keine Beschwörungswiedergabe.
