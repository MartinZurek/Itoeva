# Auftrag an Codex: Kartenschmiede prüfen, dann Karten und Beschwörungen einbauen

Arbeite im Repository `MartinZurek/Itoeva`, ausgehend vom Branch `claude/kartenschmiede`.
Befolge zuerst `AGENTS.md` (AgentGuide.md, CLAUDE.md, CLOUD_CODE_BRIEFING.md). Nie direkt auf
`main`; eigener Branch, z. B. `codex/karten-beschwoerung`.

## Ausgangslage

Claude hat in `tools/card-forge/` eine Kartenschmiede gebaut. Sie übersetzt eine monatliche
Faktor-Auswertung von rund 620 Unternehmen in 616 Karten für ein geplantes Kartenspiel in
Itoeva 2. Lies vollständig: `tools/card-forge/README.md`, `rules.json`, `forge.py`,
`test_forge.py` und den Bericht `out/monatswandel-2026-10.md`. Die Kartendaten stehen in
`out/karten.json` (öffentlich, ohne Firmennamen). `private/` fehlt absichtlich im Repo.

Kurz: Momentum → Angriff, Quality → Leben, Value → Kristallkosten, Low Vola → Temperament
(Wächter/Flackernd/Ungestüm), Size → Seltenheit (Q5 = Mythisch), Composite Q1 → Glanz,
Sektor/Industrie → 13 Völker und 67 Arten in drei Formen. Jede Karte liegt exakt auf der Kurve
2 × Kristalle + 1. Die sechs Wesen (Puffling, Starlet, Wyrmling, Fennec, Gloop, Hootlet) haben
Heimvölker, können aber alle Karten spielen. Dämonen (Paktvolk) und das KI-Volk (Artefakte) sind
aus Industrien gebildet.

## Teil 1: kritisch prüfen, bevor du baust

Prüfe selbstständig und schreibe das Ergebnis in `tools/card-forge/PRUEFUNG.md`:

1. **Tests:** `python tools/card-forge/test_forge.py` ausführen. Fehlt `private/`, mit erfundenen
   Daten aus dem Test arbeiten.
2. **Balance:** Ist "jede Karte auf der Kurve 2K+1" für ein Spiel tragfähig? Sind die Punktpreise
   der Fähigkeiten und Gaben in `rules.json` plausibel (Zins, Widerhall, Blutpakt, Lernen,
   Flächenbrand …)? Ist der Pool von Hootlet (Ø 6,9 Kristalle, viele Mythische) ein Problem?
   Sind 42 Karten mit 10 Kristallen zu viele? Konkrete Gegenvorschläge als Änderungen an
   `rules.json`, nicht am Code.
3. **Passung zur Welt:** Passen Völker, Arten und Fähigkeiten zu Lore und Charakteren
   (`app-sim/src/main/res/values-de/strings.xml`, `lore_*`, `Vision.md`, `ITOEVA2.md`)?
   Insbesondere: Grundsatz "sanft statt strafend", keine Verlustmechanik, Booster nur in der Welt
   verdient (Bezahlung ist OPEN DECISION).
4. **Einbau-Entscheidung:** Kampf ist laut `docs/itoeva2-worldbuilding.md` noch nicht entschieden
   (Raster, Gruppe, Kampfklassen = OPEN DECISION). Martin überlegt außerdem, Figuren in 3D/2,5D
   umzusetzen und eventuell auf eine Engine wie Godot zu wechseln. Beurteile, ob es jetzt sinnvoll
   ist, Karten in die bestehende Kotlin/Compose-Runtime von `:app-sim` (Build-Typ `game`) einzubauen,
   oder ob ein engine-unabhängiger Zwischenschritt besser ist. Begründe kurz.

Löse keine OPEN DECISION durch Annahmen auf. Wenn du nach der Prüfung zu dem Schluss kommst,
dass der Einbau jetzt nicht sinnvoll ist, höre nach Teil 1 auf, committe `PRUEFUNG.md` und
nenne den empfohlenen nächsten Schritt.

## Teil 2: wenn sinnvoll, mit dem kleinsten Schnitt anfangen

Kleinste rücknehmbare Schritte, jeder mit Tests:

1. **Kartendaten im Spiel:** `out/karten.json` als Asset nur in der Spiel-Variante
   (`app-sim/src/game/assets/cards/`). Reines Kotlin-Datenmodell plus Parser mit JUnit-Tests
   (unbekannte Felder tolerieren, ruhende Karten mitladen, Version/Monat lesen).
2. **Kartenbuch:** ein Menüpunkt im Spielmenü (neben Karte und Rucksack), der alle Karten als
   Raster zeigt, filterbar nach Wesen/Volk/Seltenheit. Kartenbild wie in `out/galerie.html`
   (Kosten, Name, Art, Volk, Angriff/Leben, Seltenheitsstein, Glanz). Noch kein Kampf,
   keine Sammlung, kein Besitz.
3. **Beschwörungsanimationen:** nicht 616 Einzelanimationen, sondern eine Vorlage je Volk
   (13) mit drei Ausbaustufen je Form (Jungform, Reife Form, Urform) und Färbung aus der Karte
   (`hex`, Glanz). Beispiel: Moorglut steigt als Flammensäule aus dem Boden, Paktvolk öffnet ein
   Glutsiegel, Artefakte setzen sich aus schwebenden Glyphen zusammen, Leuchtfeuer zündet ein
   Licht, Hortbrut lässt Kristalle regnen. Mythische Karten bekommen eine längere Sequenz im Geist
   der GF-Beschwörungen aus Final Fantasy VIII. Im Kartenbuch per Antippen abspielbar.
   Halte dich an die vorhandenen Zeichenregeln (`PlayInk`, Kantenlicht, Aussparung; siehe
   SKILLBAUM.md "Zeichenkasten") und an die 480×270-Bildbasis der gemalten Welt. Beginne mit
   **einem** Volk komplett (Vorschlag: Leuchtfeuer, Fennecs Heimvolk), dann die übrigen.
   Martin wünscht sich langfristig hochaufgelöste Video-Beschwörungen für Super-Fähigkeiten;
   baue deshalb eine Schnittstelle, hinter der später ein Video statt der Code-Animation
   abgespielt werden kann.
4. **Dokumentation:** Abschnitt in `ITOEVA2.md` und ein Eintrag in `EVOLUTION.md`
   (Evolution-History-Format), `tools/card-forge/README.md` um den Einbau ergänzen.

## Abschluss

- `./gradlew :app-sim:testDebugUnitTest :app-sim:compileGameKotlin` (JAVA_HOME siehe CLAUDE.md).
- `git diff --check`, vollständigen Diff prüfen, Pull Request gegen `claude/kartenschmiede`.
- PR-Text: Zweck, betroffene Bereiche, ausgeführte Tests, Ungeprüftes (z. B. Gerätetest der
  Animationen), konkreter nächster Schritt.
- Keine echten Faktordaten, Firmennamen oder ISINs einchecken; das Repository ist öffentlich.
