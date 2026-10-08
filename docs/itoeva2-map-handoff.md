# Übergabe: Zwischenorte und Bildanschlüsse – PR #344

Stand: 08.10.2026. Repository: `MartinZurek/Itoeva`.
PR: https://github.com/MartinZurek/Itoeva/pull/344
Remote-Branch: `codex/coherent-painted-world`; Ziel: `main`.

## Auftrag und Ergebnis

Martin möchte die native gemalte Welt vor Merge/APK durch sinnvolle Zwischenorte
verbinden. Der volle Android-Chat wird hier fortgesetzt. Der Arbeitsstand soll auf
GitHub gesichert werden, damit ein neuer Chat ihn ohne alte Scratch-Dateien übernehmen kann.

- Drei begehbare Zwischenorte: Küstenweg/Bachbrücke zwischen Dschungel und Straße,
  Dorfrand/Obstgarten zwischen Wald und Marktplatz, Bergpass zwischen Bergen und Lager.
- 17 Außenorte und elf Innenräume; sieben gemeinsame Außenpanoramen, sechs nachgemalte
  Anschlussbilder. Alte Ortsnummern bleiben stabil; neue Werte stehen am Enum-Ende.
- Der Brückenboden hebt/verengt den Laufweg. Füße und Touchprojektion teilen das Profil.
- Anschlussbilder anhand der Einzeldateien und Bewegung geprüft:
  4 = Gebirge → Bergpass; 5 = Bergpass → Lager. Originalbilder wurden nicht retuschiert.

## Relevante Dateien

- Welt/Boden: `matrix/GameWorld.kt`, `matrix/GameScenes.kt` unter
  `app-sim/src/main/java/com/notime/glyphsim/`.
- Laden/Zeichnen: `ui/GameSceneView.kt`, `ui/GameWorldView.kt`.
- Bilder: `app-sim/src/game/assets/world/{coast-path,village-edge,mountain-pass}.png`
  und `world/seams/0.png` bis `5.png`.
- Dokumentation: `docs/itoeva2-continuous-world.md`, `tools/world-art/README.md`,
  `tools/world-art/source/landscape-transitions-prompt.md`, aktueller EVOLUTION-Eintrag.
- Vorschau: `tools/world-art/coherent-world-overview.png` (alle 28 Orte) und
  `coherent-world-preview.mp4` (70 Sekunden); erzeugt durch `preview_geography.kt`
  und `preview_walk.kt`. Aktualisiert und stichprobenweise visuell geprüft,
  einschließlich beider endgültiger Bergpass-Anschlüsse. Desktop, keine APK-Aufnahme.

## Prüfstand und offene Schritte

Der erneute vollständige lokale Lauf bestätigt 944 bestandene Kotlin-Tests (08.10.).
Der frühere Android-Verify-Lauf 37733498319 ist grün, gilt aber nur für den älteren
PR-Kopf `843c0bf1e5fb91495198f1a4a2d5c2aa7f153cf0`, nicht für die Zwischenorte.
Laufzeitcode und Assets sind auf GitHub gesichert. Sie entsprechen dem korrigierten
Stand `584fe66ba3a64254c8cc04c854470439c3a0c75c`; die folgenden Änderungen erneuern
Vorschauen, deren Prüflogik und diese Übergabe. Maßgeblich ist der **aktuelle PR-Kopf**.
Der abschließende Android-CI-Link und Status stehen in der PR-Beschreibung.
Sicherungscommit `955945b`
enthielt zwischenzeitlich falsch zugeordnete Anschlüsse 4/5; dafür `584fe66` oder
einen neueren PR-Kopf verwenden. Nicht den Sicherungscommit allein übernehmen.

Die erneuerte Vorschau weist für Berge → Bergpass und Bergpass → Lager jeweils
`[RIGHT, LEFT]` aus; beide Grenzen werden mit echter Bewegungsphysik hin und zurück
überquert. Optionaler dritter Parameter `4,5` erneuert nur diese beiden Fälle,
behält die ursprünglichen Frame-Nummern und überschreibt Frames 600–899.
Ohne diesen Parameter werden alle sieben Fälle und 1050 Frames neu erzeugt.

1. `git status` und PR-Kopf prüfen; bei abweichendem Remote-Stand zuerst dessen Diff lesen.
2. `bash tools/reaction-preview/tests.sh` führt die reinen Kotlin-Tests aus.
3. Neuen Verify-Lauf am aktuellen PR-Kopf vollständig prüfen (Tests, Lint, R8, API 26/35).
4. Danach signierte Game-APK über den bestehenden **Deliver APK**-Workflow auf
   `codex/coherent-painted-world` bauen. Signierung/Versionierung nicht neu aufsetzen.
   Die Game-Datei ist `app-sim/build/outputs/apk/game/app-sim-game.apk`;
   bestehendes Drive-Ziel: `1cyDsKNstKS7HFCPZTKsFvWXGGF2Wah10`.
5. Auf dem Telefon: alle sechs Bildgrenzen hin/zurück, Brücke an verschiedenen Tiefen,
   Türen, zwei Daumen, Doppeltipp nach Zoom, Neustart, Speicher/GPU/Bildrate prüfen.

Weitere Möbelkonturen und genaue Anker kleiner Innenräume sind Folgearbeit.
Die Außenbilder/Anschlüsse belegen zusammen etwa 78 MiB dekodierten Speicher;
Geräteleistung ist noch nicht gemessen. Kein Merge in dieser Fortsetzung vorgesehen;
zuerst geprüften PR-Stand sichern und Martins visuelle Abnahme ermöglichen.
Godot-Prototypen #341–343 sind nicht Teil dieses Schnitts.

## Fortsetzung in einem neuen Chat

„Arbeite an MartinZurek/Itoeva, PR #344, Branch codex/coherent-painted-world weiter.
Lies AGENTS.md und docs/itoeva2-map-handoff.md vom aktuellen Branch. Prüfe den dort
vermerkten CI-Stand. Übernimm die drei Zwischenorte und sechs Bildanschlüsse aus diesem
Branch; arbeite nicht vom alten main und generiere die Bilder nicht erneut.
Als Nächstes Android-Prüfung abschließen und eine signierte Game-APK zur Sichtprüfung
über den bestehenden Deliver-APK-Workflow bereitstellen.“
