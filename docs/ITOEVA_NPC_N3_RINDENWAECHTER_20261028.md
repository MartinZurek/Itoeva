# N3 – Rindenwächter: schwerer zweibeiniger Gang

Branch `codex/npc-enemy-motion-20261027`, PR #386. Basis: N2 und der
Wiederaufnahmestand `2d70684`; Quellensicherungen `c004601` und `3697bde`.
Die Freigabe zum Sichern aller Figuren gilt weiter. Keine neue APK, kein Merge.

## Zeichnungen und Registrierung

- Acht ganze gemalte Gehzeichnungen, vier separat gemalte gedrungene
  Ruhehaltungen. Große Wurzelfüße, Beinüberschneidung und Gegenbewegung der
  Arme/Schultern; Blatt-/Zweigbewegung bleibt in den Zeichnungen.
- Originalvorlage: unterste Reihe von `assets/expansion/enemies.png`.
  Gesicht, cyanfarbenes Auge, Brustkristall, weiße Blüten und Zweigkrone
  orientieren sich daran. Keine 3D-Figur und keine Laufzeitverformung.
- Der erste Bogen wiederholte dieselbe Beinstellung zu stark und richtete
  die Ruhefigur zu weit auf. Daher werden nur seine ersten vier Gehbilder
  verwendet. Zweite Quelle liefert nur die vier neuen Ruhezeichnungen;
  dritte Quelle den Gegenbeinschritt. Alle Originale und vollständigen
  Prompts sind unter `tools/motion-art/sources/bark-guardian-*-n3.*` gesichert.
- Die dritte Quelle schaut rechts. Ganze Zeichnungen werden bei der
  Registrierung horizontal gespiegelt; das ist keine Gliedmaßenverformung.
  `mirror_x` ist in den Annotationen und Metadaten nachvollziehbar.
- Gemeinsame Irisbreite 14 px, x-Anker 180, Zelle 448, Bodenlinie 392,
  Referenzhöhe 280 und `world_height=1.6`. Laufzeitoffset 168 und
  Pixelmaßstab bleiben in Bewegung und Ruhe unverändert.
- Zyklusstrecke 200 registrierte Pixel. Der vorhandene N1-Laufzeitpfad
  zählt ausschließlich den tatsächlich zurückgelegten Revierweg. Nach
  0,14 s Stillstand beginnt die eigene ruhige Folge. Kein Eingriff in
  Spieltempo, Hauptfiguren, Revierpfade, Kampfwerte, Decks oder Speicherformat.

## Reproduktion und Prüfungen

```bash
python3 tools/motion-art/prepare_npc.py bark-guardian --height 1.6 --checkpoint n3
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_guardian.gd
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p 'test_npc*assets.py' -v
MOTION_M4=1 NPC_ID=bark_guardian NPC_CAPTURE_RUN=n3_final godot --path prototypes/godot-hd2d \
  --audio-driver Dummy --rendering-method gl_compatibility --fixed-fps 30 \
  --resolution 640x360 --script res://tests/npc_motion_capture.gd
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_bark_guardian_n3_final docs/validation/npc-n3-20261028
```

Vor der Aufnahme den Import vollständig beenden. Rohaufnahmen mit lokaler
`captures/.gdignore` vom Editor ausschließen. Nur den abgeschlossenen Lauf
auswerten, sonst vermischt ein Ordner alte und neue Zwischenbilder.

Reproduziert: 120 N3-, 54 N1-, 68 N2-, 47 Revier- und 1798 M4-Prüfungen ohne
Fehler; drei Python-Assetprüfungen bestanden. Logs und Exitcodes unter
`docs/validation/npc-n3-20261028/`. Die bekannten Ressourcenreste beim
Beenden des Welt-/Reviertests bestehen weiterhin.

Die finale Aufnahme umfasst 600 echte Spielbilder / 20 Sekunden bei 30 fps
am Mühlenbach mit Mooslyn als Größenvergleich. Stopps [120,195) und
[405,480); bei Bild 270 wird die Revieruhr auf den Rückweg gestellt.
Keine interpolierten Bewegungsbilder und kein Retiming.

Abschluss: Die Aufnahme zeigt alle acht Gehphasen rechts/links, alle vier
Ruhephasen in jedem Stopp, konstante Position während beider Stopps und
tatsächliche Ortsbewegung nach beiden Fenstern. Durchgehend Pixelmaßstab
0,0102629866451025 und Bodenoffset 168. Original-Weltfilm, unverändert
getakteter Detailausschnitt und Phasen-/Übergangsbogen wurden geprüft.
Der vorher zu aufrechte Ruhebogen und die wiederholte Beinstellung wurden
vor der finalen Aufnahme ersetzt. Keine sichtbar starke Größenumschaltung;
feine Sohlendruckpunkte werden damit weiterhin nicht als nachgewiesen erklärt.

Alle drei Quellen wurden nach einem automatischen Workspace-Reset erhalten
bzw. wiederhergestellt und früh auf GitHub gesichert. Atlanten, Zustands-
und Assetprüfungen sowie die finale Spielaufnahme wurden danach neu erzeugt.
Ein zu früh während des Imports gestarteter Lauf und ein vermischter
Aufnahmeordner wurden verworfen. Die finale Fassung liegt in einem eigenen
Aufnahmeordner; die Auswertung lehnt gemischte Auflösungen und einen fehlenden
Mooslyn-Vergleich ab. Rohaufnahmen sind reproduzierbare Zwischenbilder.

## Grenzen

Fester Maßstab/Bodenanker beweist noch keine vollständige Sohlendruckpunkt-
oder Schlupffreiheit. Neue Tiefen-/Wendeposen, zusätzliche gemalte
Ruheverbindungen aus jeder Gehphase und Kampfangriffe bleiben offen.
Kein Telefontest und keine neue APK.

## Nächster Schritt

Nach dem vollständigen geprüften N3-Checkpoint: N4 Glutwicht (`glut_imp`).
Danach Siegelweber, Tangkrabbler, Harzsammler, Schlackenträger, Vharok,
Nachtwurz, Fennec, Gloop, Puffling, Hootlet, Nella, Brumm, Tilla, Miro.
Jede Figur einzeln bis zu Tests, echter Spielvorschau, Übergabe und eigenem
GitHub-Commit abschließen. Hauptfiguren aus APK 0.15.8 bleiben abgenommen.
