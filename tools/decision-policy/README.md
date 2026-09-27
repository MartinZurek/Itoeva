# Decision Policy (V1)

Eine kleine, vollstaendig lokale Entscheidungs-Policy fuer das Living-Agent-System. Kein LLM, keine
Cloud, kein Netz, keine Tokens. App und Stream-APK verwenden dieselbe Datei
`app-sim/src/main/assets/decision_policy_v1.onnx` und denselben Code (`app-sim/.../decision/`).

## Wo sie sitzt

```
Beduerfnisse, Persoenlichkeit, Beziehungen, Erinnerung, Welt, Ort, Zeit, Verlauf
        |
        v
DecisionCandidates   -> zulaessige sichtbare Ablaeufe (Gueltigkeitsschicht)
        |
        v
DecisionPolicy       -> eine Bewertung je Kandidat (OnnxDecisionPolicy, sonst ExistingUtilityPolicy)
        |
        v
DecisionSelector     -> Softmax mit Temperatur, Zufall von aussen (reproduzierbar)
        |
        v
LivingRuntimeAdapter.prepare(preferredRoutine, chosenGoal)  -> bestehender Planer und Runtime
        |
        v
sichtbarer Ablauf -> ActionOutcome -> Zustand, Erinnerung, Beziehung (unveraendert)
```

**Was die Policy ersetzt:** die Wuerfel, die bisher entschieden, WIE eine Absicht aussieht -
Themenwurf (`PlayAmbientActivity.nextTopic`), Ablaufwurf (`PlayRoutines.forTopic`) und die
Gruppenspiel-Umwandlung in DockScreen. Dazu darf sie zwischen dem Lieblingsziel des Kerns und
knapp unterlegenen, zulaessigen Zielen abwaegen.

**Was sie nicht ersetzt:** den Kern. `UtilitySelector` bewertet weiter die Ziele, `Planner` plant,
`LivingSimulation.step` prueft jede Voraussetzung unmittelbar vor der Ausfuehrung, und jede Wirkung
laeuft durch `ActionOutcome`. Keine zweite Weltpipeline, kein StoryManager, kein Freitext.

## Gueltigkeitsregeln, die kein Modell umgehen kann

Sie wirken bei der Erzeugung der Kandidaten, nicht bei der Bewertung (`DecisionCandidates`):

- nur Ablaeufe, die der naechste echte Kernschritt zeigen darf (`LivingRuntimeAdapter.options`):
  Oeffnungszeiten, Vorrat, Geld, Plan;
- Themen nur aus der bisherigen Tagesgewichtung: nachts nur Schlaf, nie Medizin, bei laufendem
  Aufenthalt draussen nur Aussenthemen, bei einem Zuschauerimpuls nur dessen Thema;
- ein laufender Plan wird nie verlassen; draengen Hunger oder Muedigkeit (>= 0,7), gibt es nur
  dieses Ziel; andere Ziele nur mit Druck, Weg und hoechstens 0,25 Abstand zum Lieblingsziel;
- das Bett nur nachts; Gruppenspiel nur tagsueber an einem Spielort mit wirklich Anwesenden - und
  dann Vorrang vor dem Einzelsport (wie bisher).

## Neue Aktionen

Kandidaten entstehen aus `PlayRoutines.allFor`, `PlayGroupGame.Kind` und dem Kern - nicht aus einer
Liste. Ihre Merkmale (`ActionTraits`) werden aus den Schritten, dem Thema und der Kernwirkung
abgeleitet. Ein neuer Ablauf (Tischtennis, Bowling, ein neues Arcade-Spiel) aus vorhandenen
Schritten ist deshalb sofort Kandidat und wird bewertet, ohne das Modell anzufassen. Ein neuer
Schritt-TYP braucht genau eine Zeile in `ActionTraits.stepTraits` (der Compiler verlangt sie). Neu
trainieren lohnt sich danach, ist aber keine Voraussetzung. `DecisionCoverageTest` meldet jeden
sichtbaren Ablauf, jede Spielart, Sonderaktivitaet und Kernhandlung, die nie Kandidat wird.

## Modell

- Eingabe: 127 benannte Merkmale (Schema v1, `DecisionFeatures.NAMES`): Zustand (Beduerfnisse,
  Persoenlichkeit, gelernter Geschmack, Ziel, Tageszeit, Ort, Gesellschaft, Ressourcen,
  Bewegungs-/Draussen-Uhr), Kandidat (abgeleitete Eigenschaften, Kernwirkung je Beduerfnis, Dauer,
  Aufwand, Ziel und Zielabstand) und Zusammenspiel (Need Fit, Personality Fit, Neuheit,
  Wiederholung, Wiederaufnahme, soziale Gelegenheit, Kontinuitaet mit Anwesenden, bisherige
  Wahrscheinlichkeit).
- Netz: 127 -> 32 -> 16 -> 1, ReLU, **4 641 Parameter**, ONNX **21 042 Byte**.
  Das groessere 64-32 (10 305 Parameter) war kaum besser und wurde deshalb nicht genommen
  (`training-report.json`).
- Metadaten im Modell: `policy_model_version=1`, `feature_schema_version=1`, alle Merkmalsnamen,
  Temperatur. Stimmt eines nicht, wird das Modell nicht benutzt.
- Inferenz in reinem Kotlin (`OnnxModel`: Protobuf-Teilmenge, Gemm/MatMul/Add/Relu/Identity).
  Keine onnxruntime-Abhaengigkeit in der App - das haette mehrere MB je ABI gekostet, eine
  Build-Aenderung verlangt und die Offline-Tests ohne Android unmoeglich gemacht. Die Paritaet mit
  onnxruntime prueft `OnnxDecisionPolicyTest` gegen `app-sim/src/test/decision-policy-reference.csv`.

## Rueckfall

`DecisionPolicies.load` liefert die `ExistingUtilityPolicy`, wenn das Modell fehlt, unlesbar ist,
eine unbekannte Operation enthaelt, Version/Schema/Merkmalsnamen nicht passen oder auf einem
Referenzmoment keine endliche Zahl liefert. `DecisionEngine` faellt fuer einen einzelnen Moment
zurueck, wenn die Policy wirft oder NaN liefert. Die `ExistingUtilityPolicy` bewertet jeden
Kandidaten mit dem Logarithmus der Wahrscheinlichkeit, mit der die bisherige Kette ihn gezogen
haette - mit Temperatur 1 ist das **exakt** das bisherige Verhalten (`DecisionCandidatesTest`
wuerfelt die alte Kette nach).

## Training (reproduzierbar)

```
pip install -r tools/decision-policy/requirements.txt   # numpy, onnx, onnxruntime
bash tools/decision-policy/train.sh
```

1. **Phase A - Lehrer.** `DecisionTeacher` (Testbaum, nicht in der App) bewertet jeden Kandidaten
   aus: vorhandener Utility (Log-Wahrscheinlichkeit der bisherigen Kette, Zielabstand), Need Fit,
   Personality Fit, Neuheit, Wiederholungsstrafe, sozialer Gelegenheit, Kontinuitaet und Kosten.
2. **Datensatz.** `DecisionSimulation` laesst alle sechs Spezies je 24 Tage mit drei Startwerten
   durch den echten Kern, die echte Bevoelkerung und den echten Adapter leben (Verhalten: Lehrer
   mit 25 % Erkundung, dazu gestoerte Kopien der Lage). 9 554 Entscheidungen, 97 764 Zeilen.
3. **Phase B - Netz.** `train_policy.py` lernt listenweise (Softmax-Kreuzentropie gegen die
   Lehrerverteilung plus kleiner zentrierter MSE), numpy, ein Rechenfaden, fester Startwert.
   Export ueber das offizielle `onnx`-Paket, `onnx.checker`, Paritaet mit onnxruntime.
4. Vergleich (`baseline-vs-policy.md`) und Messung (`benchmark.txt`).

Zwei Laeufe ergeben byte-gleich dasselbe Modell (SHA-256 in `training-report.json`).

## Ergebnisse (V1)

Auf ungesehenen Entscheidungen trifft das Netz die Lehrerverteilung mit mittlerer KL 0,00055 und
93 % gleicher Erstwahl. Baseline gegen Policy ueber 14 Tage x 6 Spezies x 3 Startwerte steht in
`baseline-vs-policy.md`: direkte Wiederholungen 10,4 % -> 5,1 %, verschiedene Ablaeufe je Tag
9,8 -> 11,3, Entropie 3,86 -> 4,24 Bit, bei gleichem Beduerfnisdruck und 92 % Zielen des Kerns.

Laufzeit (JVM, Entwicklungsrechner, `benchmark.txt`): Laden und Pruefen 0,2 ms; Kandidaten
erzeugen im Mittel 0,3 ms (p95 1,1 ms); Merkmale + Netz fuer alle Kandidaten 0,3 ms (p95 1,0 ms);
rund 15 KB Heap je geladenem Modell. Gerechnet wird nur an Entscheidungspunkten (etwa alle 50 s),
nie je Bild.
