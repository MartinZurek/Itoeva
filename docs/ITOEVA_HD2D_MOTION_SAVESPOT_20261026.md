# Itoeva – HD-2D-Bewegungen, Savespot M1 (26.10.2026)

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
