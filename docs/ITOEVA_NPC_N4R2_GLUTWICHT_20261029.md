> **Größenkorrektur nach Martins Feedback:** Aktuell gilt [ITOEVA_NPC_N4_GROESSE_20261029.md](ITOEVA_NPC_N4_GROESSE_20261029.md): Glutwicht ist 1,5–1,7-mal so hoch wie Mooslyn. Der folgende Text dokumentiert den ersten, zu kleinen N4R2-Stand.

# N4R2 – Glutwicht nach Wiederherstellung

Arbeitsstand auf Basis des Remote-Commits `c7c90bd` im Branch
`codex/npc-enemy-motion-20261027`, PR #386. N1–N3 und die vier abgenommenen
Hauptfiguren aus APK 0.15.8 bleiben erhalten. Keine APK, kein Merge.

## Neue Quellen und Registrierung

Die verlorenen N4-Originale wurden nicht rekonstruiert. N4R2 verwendet neue,
früh gesicherte Bildgenerierungsquellen nach `assets/expansion/glut-imp.png`:

- `glut-imp-guided-n4r2.png`: erste vier Gehzeichnungen.
- `glut-imp-counter-n4r2.png`: vier gezielte Gegenbeinzeichnungen.
- `glut-imp-motion-n4r2.png`: vier Ruhezeichnungen aus der letzten Reihe.

Alle Originale, vollständigen Prompts und der verwendete Beinplan liegen unter
`tools/motion-art/sources/`. Die zweite Hälfte des ersten Gehbogens wiederholte
die vordere Beinstellung zu stark; sie wird nicht eingebaut. Die gezielte
Gegenbeinquelle ersetzt sie. Acht ganze Zeichnungen, keine Laufzeitverformung.

Annotationen unter `tools/motion-art/glut-imp-{walk,rest}-n4r2.json` fixieren
Irisgröße 14 px, x-Anker 150, Zelle 448, Bodenlinie 392 und Referenzhöhe 280.
Jede ganze Zeichnung wird einmal uniform skaliert und verschoben. Gehen und
Ruhe verwenden denselben Laufzeitmaßstab, Bodenoffset 168 und die bestehende
Weltgröße 0.86. Der bestehende N1-Pfad zählt den tatsächlich zurückgelegten
Revierweg; Zyklusstrecke 180 registrierte Pixel, Ruhe nach 0,14 s Stillstand.
Die Datenregistrierung aktiviert die vorhandene Weltintegration ohne neue
Wegsteuerung. Tempo, Revierwege, Kampfdaten, Decks und Speicherformat bleiben.

## Erneute Prüfungen

120 N4-, 54 N1-, 68 N2-, 120 N3-, 47 Revier- und 1.798 M4-Prüfungen bestanden
(2.207 Checks, null Fehler). Vier Python-Assetprüfungen bestanden. Die Logs
und Exitcodes stehen unter `docs/validation/npc-n4r2-20261029/`.
Die historischen N4-Zahlen und Filme sind kein Nachweis für N4R2.

600 echte Weltbilder / 20 Sekunden bei 30 fps am Aschenstieg ausgewertet:
alle acht Gehphasen in beiden Richtungen, alle vier Ruhezeichnungen in beiden
Stopps [120,195) und [405,480), unveränderte Position während beider Stopps
und tatsächliches Wiederanlaufen danach. Mooslyn dient als Größenvergleich.
Durchgehend Pixelmaßstab 0.00551635539159179 und Bodenoffset 168.
Phasen-/Übergangsbogen und Größenvergleich visuell geprüft: keine starke
Größenumschaltung. Weltfilm und Detailfilm behalten die echte Bildfolge und
30 fps; kein Retiming und keine interpolierten Zwischenbilder. Telemetrie,
Auswertung, Filme und Capture-Log liegen im genannten Prüfverzeichnis.

## Reproduktion

```bash
python3 tools/motion-art/prepare_npc.py glut-imp --height 0.86 --checkpoint n4r2
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_imp.gd
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p 'test_npc*assets.py' -v
MOTION_M4=1 NPC_ID=glut_imp NPC_CAPTURE_RUN=n4r2_final godot \
  --path prototypes/godot-hd2d --audio-driver Dummy \
  --rendering-method gl_compatibility --fixed-fps 30 --resolution 640x360 \
  --script res://tests/npc_motion_capture.gd
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_glut_imp_n4r2_final \
  docs/validation/npc-n4r2-20261029
```

Vor der Aufnahme den Import beenden; `captures/.gdignore` und einen eigenen
Aufnahmeordner verwenden. Software-GPU Mesa llvmpipe, Godot 4.6.1.

## Grenzen und nächster Schritt

Vollständige Sohlendruckpunkte und Schlupffreiheit sind nicht nachgewiesen.
Neue Tiefen-/Wendeposen, gezeichnete Verbindungen aus jeder möglichen Gehphase,
Kampfangriffe und Telefontest bleiben offen. Die bekannten Ressourcenreste
beim Beenden des Welt-/Reviertests bestehen weiter.

Erst nach abgeschlossener Weltprüfung und vollständiger GitHub-Sicherung
dieses Checkpoints folgt N5 Siegelweber (`seal_weaver`). Martins ausdrückliche
Freigabe für alle Figuren-Checkpoints gilt weiter.

## Sicherungsstatus dieser Sitzung

Vollständiger geprüfter lokaler Implementierungscommit:
`cf4013abe57ba725994fdbbbefdaeb69524253de`, Tree
`d665f8d20c65de4139383011ae9bd63ed40b545a`.
Der Push wurde von der automatischen Freigabeprüfung abgelehnt. Nach erneuter
Prüfung des verbundenen Eigentümers MartinZurek, des Repositorys und von
PR #386 wurde auch der zweite Versuch abgelehnt: Die Prüfung akzeptierte
die frühere dokumentierte Freigabe nicht als direkte Veröffentlichungsfreigabe
für diesen neuen Commit im öffentlichen Repository. Nicht über einen anderen
Übertragungsweg umgehen. Zuerst direkte Nutzerbestätigung für diesen Push
einholen. Remote-Kopf zuletzt `c7c90bd`; N5 noch nicht begonnen.

Lokaler Worktree: `/workspace/scratch/8db2aa5faa6b/Itoeva`, lokaler Branch
`codex/n4-completion-20261029`; Ziel bleibt der bestehende PR-Branch
`codex/npc-enemy-motion-20261027`. Die getrennte ursprüngliche Arbeitskopie
wurde nicht zurückgesetzt oder überschrieben.
