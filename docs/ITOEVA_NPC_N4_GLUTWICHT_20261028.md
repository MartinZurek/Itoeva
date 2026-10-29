> **Aktueller Abschluss:** Neue Fassung N4R2 und erneute Prüfung stehen in [ITOEVA_NPC_N4R2_GLUTWICHT_20261029.md](ITOEVA_NPC_N4R2_GLUTWICHT_20261029.md). Die folgende N4-Fassung ist historisch.

> **Wiederaufnahmestand nach Workspace-Reset:** N4 war vor dem Reset lokal umgesetzt und geprüft, ist aber noch kein vollständig wiederhergestellter Figurencheckpoint. Die folgende Beschreibung dokumentiert den damaligen lokalen Stand. Der Branch enthält derzeit nur erhaltene Vorschauen, Bericht und einen Pixelatlas; vollständige gemalte Quellen, Metadaten und Tests fehlen noch. Maßgeblich ist `docs/ITOEVA_NPC_N4_RECOVERY_20261028.md`. N5 nicht beginnen, bevor N4 wieder vollständig gesichert ist.

# N4 – Glutwicht: lebhafter Zweibeinergang

Branch `codex/npc-enemy-motion-20261027`, PR #386. Vor Beginn Remote-Kopf
`f58562e958569d93afd510cb3814c6f6fbba3a25` geprüft; keine neueren Branchänderungen.
N1–N3 und die Hauptfiguren aus APK 0.15.8 bleiben abgeschlossen.

## Zeichnungen und Einbindung

Acht ganze gemalte Gehzeichnungen und vier eigene ruhige Standzeichnungen nach
`assets/expansion/glut-imp.png`: Widderhörner, Glutauge, dunkle Lavaplatten,
Flammenmähne, Kettengürtel und segmentierter Flammenschwanz bleiben erkennbar.
Der erste Bogen wiederholte die Beinstellung der zweiten Schritthälfte zu stark.
Daher stammen die Phasen 4–7 aus einem gezielt nachgezeichneten Gegenbeinbogen;
dessen ganze Zeichnungen werden ohne Spiegelung registriert. Originale und volle
Prompts des eingebauten Imagegen liegen unter `tools/motion-art/sources/glut-imp-*-n4.*`.
Keine 3D-Figuren, Gliedmaßenverformung oder interpolierten Bewegungsbilder.

Gemeinsame Irisreferenz 14 px, x-Anker 150, Zelle 448, Bodenlinie 392,
Referenzhöhe 280 und bisherige `world_height=0.86`. Pixelmaßstab und
Bodenoffset sind für Gehen/Ruhe fest. Die vorhandene N1-Darstellung zählt den
tatsächlich zurückgelegten Weg; Zyklusstrecke 180 registrierte Pixel. Nach
0,14 s Stillstand beginnt die eigene Standfolge. Keine neue Wegsteuerung und
keine Änderung an Tempo, Revierwegen, Kampfdaten, Decks oder Speicherformat.

## Reproduktion

```bash
python3 tools/motion-art/prepare_npc.py glut-imp --height 0.86 --checkpoint n4
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_imp.gd
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p 'test_npc*assets.py' -v
MOTION_M4=1 NPC_ID=glut_imp NPC_CAPTURE_RUN=n4_final godot --path prototypes/godot-hd2d \
  --audio-driver Dummy --rendering-method gl_compatibility --fixed-fps 30 \
  --resolution 640x360 --script res://tests/npc_motion_capture.gd
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_glut_imp_n4_final docs/validation/npc-n4-20261028
```

Godot 4.6.1, OpenGL/Mesa llvmpipe. Vor Aufnahme Import vollständig beenden;
`captures/.gdignore` und ein eigener Laufordner verhindern vermischte Bilder.
600 echte Weltbilder / 20 s am Aschenstieg, Mooslyn als Größenvergleich.
Alle acht Gehphasen rechts/links, alle vier Standphasen in beiden Stopps
[120,195) und [405,480), kein Ortsweg während der Stopps und tatsächliches
Wiederanlaufen nach beiden Fenstern. Durchgehend Pixelmaßstab
0.00551635539159179 und Bodenoffset 168. Phasen-/Übergangsbogen und Weltbilder
visuell geprüft; Welt-/Detailfilm ohne Retiming. Prüfprotokolle und Telemetrie
stehen unter `docs/validation/npc-n4-20261028/`.

Bestanden: 120 N4-, 54 N1-, 68 N2-, 120 N3-, 47 Revier- und 1.798 M4-Checks
(2.207 insgesamt) sowie vier Python-Assetprüfungen. Alle Exitcodes 0; die
bekannten Ressourcenreste beim Beenden bleiben im Welt-/Revierlog sichtbar.

## Grenzen und Fortsetzung

Fester Laufzeitmaßstab ist kein vollständiger anatomischer Kontakt-/Schlupfnachweis.
Die gezeichneten Haltungen unterscheiden sich in Gewichtsverlagerung und Rumpfneigung.
Neue Tiefen-/Wendeposen, zusätzliche gemalte Verbindungen aus jeder Gehphase und
Kampfangriffe bleiben offen. Kein Telefontest, keine neue APK, kein Merge.
Bekannte Ressourcenreste beim Beenden der Weltprüfung bestehen.

Nächster Einzelcheckpoint nach GitHub-Sicherung: N5 Siegelweber (`seal_weaver`),
danach Tangkrabbler, Harzsammler, Schlackenträger, Vharok, Nachtwurz, Fennec,
Gloop, Puffling, Hootlet, Nella, Brumm, Tilla und Miro. Die ausdrückliche
Push-Freigabe für alle Figuren gilt weiter.
