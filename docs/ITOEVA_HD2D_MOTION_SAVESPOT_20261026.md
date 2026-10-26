# Itoeva – HD-2D-Bewegungen, Savespot M3 (M1/M2 erhalten, 26.10.2026)

## APK-Auftrag nach M3-Vorschau (27.10.2026)

Martin hat nach Lieferung von Pixelbogen und echter Testanimation ausdrücklich
„Bitte APK bauen“ beauftragt. Testbau 0.15.7 / Code 37 auf demselben Branch;
bestehendes AndroidTest-Paket, bestehender Signierschlüssel und Drive-Lieferweg.
Exportmerkmal `itoeva_motion_m3` aktiviert M3 auch ohne Desktop-Umgebungsvariable.
Das Standardpreset Android bleibt unverändert. Keine neue Kunstabnahme:
Gehkontaktfehler, vollständige Druckpunktprüfung, Wenden und Tiefenansichten
bleiben offen. Keine Spieltempoänderung, keine anderen Figuren umgestellt.

608 Quelldateien in `docs/godot-v0157-runtime-manifest.json` werden vor Import
bytegenau geprüft. Der Build prüft zusätzlich die M3-Metadaten, alle vier
Pixelatlanten und das Aktivierungsmerkmal in der tatsächlich exportierten APK.
Workflow ausschließlich für den Lieferbranch aktiviert; Tests, Signatur,
Paketversion und Drive-Prüfsumme müssen erfolgreich sein, bevor geliefert wird.
**Ergebnis:** [Build 40417640717](https://github.com/MartinZurek/Itoeva/actions/runs/40417640717)
erfolgreich. APK aus Commit `3c00a64`, 200475949 Bytes; Signatur und
M3-Paketinhalt geprüft, Drive-Dateigröße und Prüfsummen stimmen überein.
[APK herunterladen](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view).
SHA-256: `77f5be50effd6d53f345f3fb68113e3f4c7c10acc453f1ea4c28dccb68962528`.
Nachweis: `docs/validation/motion-m3-20261026/apk-v0157-build.json`.
Kein Telefontest und kein Merge. Die nachstehenden M1–M3-Abschnitte dokumentieren
den historischen Stand vor der APK-Freigabe; die aktuelle Test-APK aktiviert M3.

## Aktuell M3: Vierbeiner-Galopp und Gangwechsel

Fortsetzung auf `codex/hd2d-motion-phases-20261026`, PR #385, ab dem gesicherten
M2-Commit `715b72c`. Benutzerauftrag: Galopp und Übergänge implementieren,
beurteilen und wahlweise als APK oder kurze Testanimation zeigen. Gewählt ist
die Testanimation. Die frühere Taktentscheidung für das Gehen bleibt bestehen.

- Zehn neue gemalte Galoppphasen, davon zwei zusätzliche Hinterstützphasen,
  sowie vier gezeichnete Absenk- und vier Aufrichtphasen eingebunden.
  Bilderzeugung, Quellen, Prompts, Registrierung, Masken und Pixelpass gesichert.
- Gleicher fester Maßstab für Gehen, Galopp und Verbindungen; identische
  Endkacheln im Original und in der Pixelfassung. Keine zweite Verformung.
  M3-Gehzeichnungen selbst entsprechen M2, separate Dateien für die gemeinsame
  Palette und Pixeldichte. M1/M2-Dateien erhalten.
- Neue Übergangssteuerung: vor dem langsamer Weitergehen passende Vorderstütze
  abwarten; direktes Stoppen richtet auch ohne Wegfortschritt auf. Abgebrochener
  Sprintimpuls kehrt um; Sprung, Sitzen, Rollen und Tiefenansichten brechen ab.
- `tools/motion-art/README.md`, Abschnitt „Aktuell M3“, enthält Dateizuweisung,
  Wiederholungsbefehle und Grenzen. Das neue Messverfahren kann die sichtbaren
  Stützen sowie absichtlich freie Phasen auswerten. Es behauptet keine vier
  separat vermessenen Druckpunkte.
- `docs/validation/motion-m3-20261026/`: Pixelbogen, echte 333-Bilder-Weltaufnahme
  (11,1 s), daraus vergrößerter Detailfilm sowie Aufnahme-/Kontaktberichte.
  Alle zehn Galoppphasen und vier Phasen jedes Übergangs rechts/links erreicht.
  `CLEAN_VIEW=1` versteckt nur die HUD-Ebene; Bewegung kommt aus echter Steuerung.
- Geh-Takt weiter ca. 1,066 Hz, Spieltempo ca. 2,35/3,85 Welt/s. Galopp ca.
  2,07 Hz mit gewichteten Phasendauern. Keine geänderte Physik oder Ausdauer.
- Tests: 272 Zyklus-, 126 Übergangs- und 160 Actor-Checks, 1276 bestehende
  Mooslyn-Checks unter OpenGL sowie acht Python-Tests grün. Gleicher Maßstab
  in der tatsächlichen Aufnahme geprüft. Bekannte Lecks beim Beenden bestehen.
- Standarddarstellung unverändert; M3 nur mit `MOTION_DRAFT=1`. Kein Merge,
  keine Version/Workflow geändert und keine APK erstellt.

**Offen:** M2-Gehkontaktwechsel und vollständige Gehschrittlänge, vier einzeln
belastbare Galopp-Druckpunkte, Wendefolgen und Front-/Rückansichten. Kein
Schlupffreiheitsnachweis. Die Vorschau behebt diese übrigen Punkte nicht.
Weitere Figuren erst nach Mooslyns vollständiger Qualitätsprüfung umstellen.

## Historischer Nachtrag M2 am selben Branch

M1 bleibt unten als historischer Ausgangspunkt dokumentiert. Der neuere
Prüfentwurf heißt `mosslyn-walk-m2`; Befehle und Details stehen in
`tools/motion-art/README.md`, Abschnitt „Fortsetzung M2“.

- Acht Gehphasen neu gemalt, Beinwechsel und gegengleiche Arme besser
  unterscheidbar; Originalquelle, Poseführung, Prompts, Kopf-/Pfotenannotation,
  Pixelpass und aus dem gemalten Original erzeugte Metadaten gesichert.
- Nur `MOTION_DRAFT=1` verwendet M2. M1-Assets und Standarddarstellung erhalten;
  Galopp unverändert. Kein Merge, keine APK, keine Workflow-/Versionsänderung.
- Pixelbogen, echte 216-Bilder-Spielaufnahme (7,2s) und Berichte unter
  `docs/validation/motion-m2-20261026/`. `WALK_ONLY=1` beschränkt den vorhandenen
  Aufnahmeablauf auf Gehen rechts/links. Alle acht Phasen in beiden Richtungen.
- Gemessen: ca.2,3543Welt/s,1,066Hz, konstanter Pixelmaßstab0,0077488162;
  Bildhaltezeiten überwiegend0,100/0,133s bei30fps. Keine Tempoänderung.
- **Weiterhin nicht fertig:** Nahpfote in Phase5 noch5px über Boden, deshalb
  kein belegter Kontaktweg Phase4→5. Sieben messbare Abschnitte zusammen203,5px;
  Wege6,5–50px pro Phase. Einschließlich des ungeprüften Abschnitts wären es
  233px statt285px. Kein Nachweis eines vollständigen oder schlupffreien Gangs.
  Phasen nicht aus diesem unvollständigen Datensatz zeitlich „repariert“.
- Neue Messung `measure_annotated_contacts.py` benennt die anatomische Pfote
  explizit und lässt fehlende Kontakte leer. Die alte M1-Zuordnung darf M2
  nicht auswerten. Zwei Regressionstests verhindern den Ersatz durch Gegenbein.
- Tests:244 Godot-Entwurfschecks,1276 bestehende Mooslyn-Checks unter OpenGL,
  sechs Python-Prüfungen grün. Die Weltaufnahme meldet die bekannten
  ObjectDB-/Ressourcenlecks beim Ende weiterhin. Vollimport des Editors stürzte
  hier im Vulkan-Initialisierungspfad ab; isolierter Atlasimport erfolgreich,
  echte Weltaufnahme und Renderprüfung unter OpenGL erfolgreich.
- **Als Nächstes weiter am Gehen:** Kontaktwechsel zu Phase5 zeichnerisch
  schließen, längeren vollständigen Standweg/Abrollen ausarbeiten und erst nach
  vollständiger Kontaktprüfung Phasendauern koppeln. Dann Galopp, Übergänge,
  Wenden und vorne/hinten. Weitere Figuren erst anschließend.

Die bestätigte Entscheidung gilt unverändert: ruhiger Takt, längere natürliche
Schritte, gleiches Spieltempo. Keine neue Richtungsfrage und keine 3D-Arbeit.

## Verbindlicher Einstieg

Repository `MartinZurek/Itoeva`, Arbeitsbranch
`codex/hd2d-motion-phases-20261026`, Draft-PR #385:
https://github.com/MartinZurek/Itoeva/pull/385

Der Branch wurde von `claude/hd2d-pixel-sprites` (Basis `cb1c2b5`) angelegt.
Nicht von `main`, `codex/hd2d-sprite-motion-20261026` oder einem 3D-Branch starten.
Vor Änderungen Git-Status und aktuellen Remote-Stand prüfen. Branch fortsetzen;
bei parallel eingegangenen Änderungen erst vergleichen, nichts überschreiben.

Zuerst `AGENTS.md`, die dort vorgeschriebenen Startdokumente und
`docs/hd2d-sprites-statt-3d.md` lesen. Dann diese Übergabe und
`tools/motion-art/README.md`. Aufräumen ist erledigt und kein Arbeitsauftrag.
Die Figuren bleiben 2D-Sprites; keine 3D-Modelle, Volumen oder Blender-Rigs.

## Neue Entscheidung von Martin – nicht erneut abfragen

Martin hat nach der Empfehlung ausdrücklich zugestimmt:

**Ruhigeren Geh-Takt beibehalten und die gezeichneten Schritte verlängern.**

- Spieltempo unverändert lassen: Gehen etwa2,35, Sprint3,85Welteinheiten/s.
- Nicht auf1,58Gehzyklen/s beschleunigen. Aktueller Seitenentwurf etwa1,07Hz.
- Größere Schritte anatomisch zeichnen: Hüfte, Abrollen, Abstoß und
  gegengleiche Arme. Nicht einfach Beine auseinanderziehen oder das Bild dehnen.
- Etwa48Prozent mehr Schrittlänge ist nur eine aus Sohlenmitten abgeleitete
  Arbeitsgröße, kein starres Ziel und keine Freigabe unnatürlicher Proportionen.
  Nach dem Nachzeichnen neu messen und Phasendauern an Kontaktwege anpassen.
- Der Vierbeiner-Galopp darf deutlich dynamischer sein als das Gehen.

Die grundsätzliche Umsetzung ist weiter freigegeben. **Keine APK-Freigabe.**
Vor einer signierten Test-APK zuerst neue Spritesheets und echte Spielaufnahmen
zeigen und Martins ausdrückliche APK-Freigabe abwarten.

## Tatsächlich gesichert

Vor diesem Übergabecommit liegen auf dem Remote drei Arbeitsschnitte:

- `ae9f3f7`: Galoppentwurf, Originalregistrierung, Masken/Bounds, Pixelpass.
- `138e138`: acht seitliche Geh- und acht Galoppphasen, isolierte Vorschau,
  feste Geometrie, Laufzeittests, Spielaufnahme.
- `4e09615`: Kontaktmessung und dokumentierte Streckenabweichung.

Kein Merge, keine APK, keine Versionserhöhung, keine Workflow-Aktivierung.
Die neuen Folgen sind nur mit `MOTION_DRAFT=1` aktiv. Ohne diese Variable bleibt
die abgenommene Standarddarstellung erhalten. Ein fertiger Gangzyklus oder
Schlupffreiheit ist **nicht** nachgewiesen.

### Dateien

- Gestaltungsreferenzen: `docs/approved-3d-designs-20261025/`; Dateinamen mit3D
  sind historische Namen, die Bilder dienen ausschließlich als 2D-Vorlage.
- Unveränderte verwendete Bildgenerierungen: `tools/motion-art/sources/`.
- Annotationen: `tools/motion-art/mosslyn-walk-8.json` und
  `tools/motion-art/mosslyn-gallop-8.json`.
- Aufbereitung: `tools/motion-art/prepare_cycle.py`.
- Messung: `tools/motion-art/measure_walk_contacts.py`.
- Gemalte Atlanten: `prototypes/godot-hd2d/assets/characters/mosslyn-walk-8.png`
  und `mosslyn-gallop-8.png`; Pixelfassungen im Unterordner `pixel/`.
- Metadaten: `prototypes/godot-hd2d/data/mosslyn_authored_cycles.json`.
- Vorschauintegration: `prototypes/godot-hd2d/scripts/compact_actor.gd`.
- Aufnahme: `prototypes/godot-hd2d/preview_pixel/play_capture.gd`.
- Vorschau-Manifest: `docs/godot-motion-preview-runtime-manifest.json` (587Dateien).
  Das historische0.15.6-Manifest und der deaktivierte APK-Workflow bleiben erhalten.

### Vorschauen und Messung

Unter `docs/validation/motion-20261026/`:

- `Mooslyn_Seitenphasen_Entwurf.png`: tatsächlich verwendeter Pixelbogen.
- `Mooslyn_Weltvorschau_Entwurf.mp4`:9,4s echte Welt, rechts gehen, Sprint,
  anhalten und links gehen. Kein Nachweis neuer Front-/Rückansichten.
- `capture-report.json`: Auswertung der vollständigen390-Bilder-Aufnahme.
- `contact-estimate.json`: Sohlenumrissmessung am gemalten Original.

Gehentwurf:2,2084Welt/Zyklus bei2,3543Welt/s, etwa1,066Hz.
Die sichtbaren Standpfotenwege ergeben nur etwa1,4878Welt/Zyklus (192statt285px).
Daher ist noch Rutschen zu erwarten. Intervalle im Original:16/33,5/34/7,5/
19/38/44/0px. Der letzte Übergang hat keinen erkennbaren Standpfotenrückweg.
Die Schätzung verwendet Sohlenmitten statt echter Druckpunkte.

Galoppentwurf:1,5323Welt/Zyklus bei3,8498Welt/s, etwa2,512Hz. Kontaktfolge und
Schrittlänge des Galopps ebenfalls noch nicht abschließend geprüft.

## Tests und Grenzen

Ausgeführt und erfolgreich:

- Godot `tests/authored_cycles.gd`:244Checks, feste Skalierung/Bodenanker,
  Raster, Spiegelung, Phasen und Verlassen der Seitenfolge bei anderen Zuständen.
- Godot `tests/mosslyn_painted_cycles.gd`:1164bestehendeChecks.
- Python `test_authored_cycles.py`:2Tests.
- Python `test_mosslyn_painted_assets.py`:2bestehendeTests.
-587Manifestdateien bytegenau geprüft; echte OpenGL-Aufnahme erstellt.

Das ist keine komplette erneute CI-Abnahme und keine Kunstabnahme. Der Basis-
und der Vorschau-Renderlauf melden beim Beenden bestehende ObjectDB-Leaks und
zwei noch gebundene Ressourcen. Keine Behauptung, das sei behoben.

Die Frontgenerierung wurde wegen einer zwischen den Reihen wandernden Tasche
verworfen. Eine gezielte Bildkorrektur löste das nicht zuverlässig. Rückansicht
noch nicht registriert/eingebunden. Diese Versuche sind keine fertigen Assets.
Die verwendeten Seitenquellen sind im Repository; weitere Generierungsversuche
aus dem alten Chat nicht als vorhandene Repo-Dateien voraussetzen.

Außenaufnahmen der alten Tiefenansichten wurden am schmalen Weg teilweise durch
Kollisionen blockiert. `DEPTH_ONLY=1` im Aufnahmeskript ermöglicht einen separaten
Prüfablauf in einem breiteren echten Innenraum; dieser Ablauf ist noch nicht
als verbesserte Tiefenbewegung abgenommen.

## Nächste Arbeit, in dieser Reihenfolge

1. Mooslyns seitliches Gehen nachzeichnen: vollständiger Wechsel beider Beine,
   längere anatomische Schritte bei ruhigem Takt, Kontakt/Abfedern/Durchschwung/
   Hochpunkt pro Bein. Beide Beine und gegengleiche Arme klar erkennbar; Umhang,
   Tasche, Ohren und Schwanz folgen. Abschlussphase besonders nachbessern.
2. Gesicht, Proportionen und Ausstattung über alle Phasen erhalten. Feste Zellen,
   ein Maßstab und feste Bodenlinie. Keine pulsierenden Gesichter oder verzogenen
   Gliedmaßen. Neue gemalte Originale per Imagegen, danach Pixelpass; Metadaten
   aus den Originalen neu erzeugen. Keine bloße numerische „Reparatur“ der Messung.
3. Standpfotenwege komplett prüfen, Takt/Phasendauern bei unverändertem Spieltempo
   an die neu gezeichnete Strecke koppeln; Pixelbogen und echte Aufnahme prüfen.
4. Mooslyns Vierbeiner-Galopp mit Vorderpfotenstütze, Hinterbeinabstoß, Streck-/
   Sammel- und Flugphase, ruhigem Kopf, angelegten Ohren und leicht offenem Mund
   nachprüfen. Übergänge Anlaufen/Anhalten, zwei→vier Beine und zurück, Wenden
   zeichnen. Gehen vorne/hinten ergänzen; Sprint mindestens seitlich.
5. Erst danach dieselbe Qualität für Nocti (Schritte, Absprung, anatomische
   Flügelschläge, tiefer ruhiger Flug mit angezogenen Füßen), Wyrmling
   (Vierbeiner-Gang, Trab, Flügel/Schwanz dezent) und Starlight (zusammenhängender
   Stern ohne Beine, Abstoß, langes Schweben, sanfte Landung, geringe Stauchung,
   nachfolgender Schal). Keine neuen Charakterdesigns.

Nach jedem abgeschlossenen Teil Diff prüfen, committen und pushen. Manifest und
relevante Tests mitziehen. Vorschauen vor APK, dann erst nach Freigabe signierten
Testbau mit erhöhter Version und nur für den Lieferbranch aktiviertem Workflow.
Ehrlich zwischen technischem Test und optischer Abnahme unterscheiden.

## Umgebungshinweise

Direktes Git-Lesen/Clone funktionierte, direktes Push scheiterte an fehlender
Anmeldung. Die Commits wurden deshalb über die verbundene GitHub-API gespeichert;
Blobs und Tree-SHAs wurden gegen Git geprüft. Nicht erneut minutenlang blind pushen.

Ein frischer Godot-Import erzeugte viele ungetrackte `.import`-/`.uid`-Dateien und
lokale Aufnahmen. Nur gezielt zur Aufgabe gehörende Dateien committen, kein
`git add .`, keine neue Aufräumrunde und keine fremden Dateien löschen.
