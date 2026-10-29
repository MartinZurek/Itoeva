> **Veröffentlichungsnachtrag 29.10.2026:** Martin hat den GitHub-Push mit „Hier mein ok“ ausdrücklich bestätigt. Der vollständige N4-/N5-Quellenstand ist unter `bfef7dad670dc737fff6ef71b8497ca19238e817` auf dem bestehenden PR-Branch veröffentlicht; sein Tree entspricht exakt dem geprüften lokalen Tree `581ed93f7e3cb825209122e1fa1e20e662cb6445`. Frühere Angaben zur Push-Blockierung und zur notwendigen Archivwiederherstellung sind historisch. Aktueller Einstieg: [ITOEVA_NPC_N5_FORTSETZUNG_20261029.md](ITOEVA_NPC_N5_FORTSETZUNG_20261029.md).

# N4 – Glutwicht als großer Bösewicht (29.10.2026)

## Maßgebliches Nutzerfeedback

Martin lehnt die kleine Größe der ersten N4R2-Vorschau ab: Glutwicht soll
als großer Bösewicht mindestens 1,5–1,7-mal so groß wie eine normale Figur
wirken, gegebenenfalls fast doppelt so groß. Ziel dieser Korrektur ist etwa
1,6-fache sichtbare Konturhöhe gegenüber Mooslyn, einschließlich der Hörner.
Die bisherigen kleinen N4/N4R2-Vorschauen sind dafür überholt.

## Umsetzung

`data/regional_enemies.json` und `data/npc_motion/glut_imp.json` verwenden
jetzt denselben festen Weltgrößenfaktor **1.75** statt 0.86. Die erste Datei
bestimmt unter anderem den Bodenschatten, die zweite die gemalte Weltfigur.
Das entspricht etwa 2,03-facher Größe gegenüber dem verworfenen kleinen Stand.

Der Datenwert 1.75 ist nicht selbst das sichtbare Größenverhältnis: Mooslyns
Kontur ist 170/168 seiner Referenzhöhe bei Weltfaktor 1.02; Glutwichts
Ruhekontur ist 262–265/280 seiner Referenzhöhe. Daraus ergeben sich etwa
1,59–1,61 Mooslyn-Höhen im Stand und 1,62–1,69 beim Gehen. Alle ganzen
Zeichnungen behalten ihre Proportionen, die gemeinsame Irisregistrierung,
Zellgeometrie und Bodenlinie. Derselbe Laufzeitmaßstab gilt in allen Phasen.

Der HD-2D-Pixelpass wurde mit der neuen Größe aus denselben registrierten
Originalatlanten erneut erzeugt, damit die größere Figur die passende
Pixeldichte erhält. Originalzeichnungen und ihre Annotationen sind unverändert.
Die Wegbindung skaliert die Schrittlänge mit der Figur; es gibt keinen
Eingriff in Reviertempo, Wege, Hauptfiguren, Kampfwerte, Decks oder Spielstände.

## Erneute Validierung

- 145 Glutwicht-, 54 N1-, 68 N2-, 120 N3-, 47 Revier- und 1.798 M4-Checks:
  **2.232 Checks, null Fehler**, alle Exitcodes 0.
- Vier Python-Assetprüfungen bestanden.
- 600 echte Weltbilder / 20 Sekunden bei 30 fps am Aschenstieg mit Mooslyn.
  Alle acht Gehphasen rechts/links, alle vier Ruhephasen in beiden Stopps
  [120,195) und [405,480), feste Position während der Stopps, Wiederanlaufen
  nach beiden Fenstern. Konturverhältnis über alle Bilder: **1.5865–1.6894**.
- Pixelmaßstab **0.0112251415848732**, Bodenoffset 168 und Schattenmaßstab
  durchgehend fest. Ein neuer Check hält Welt-/Schattenfaktor synchron;
  jeder Phasencheck begrenzt die Höhe auf 1,5–1,7 Mooslyn-Höhen.
- Phasen-/Übergangsbogen und echte Spielbilder visuell geprüft. Prüf-Ausschnitt
  für die größere Figur erweitert, damit Hörner und Schwanz darin Platz haben.
  Welt-/Detailfilm ohne Retiming und ohne interpolierte Zwischenbilder.

Filme, zwei Größenvergleiche, Telemetrie, Bericht und Logs liegen unter
`docs/validation/npc-n4-size-20261029/`. `Glutwicht_Groessenvergleich.png`
ist das unveränderte echte Weltbild 42 mit Abstand zwischen beiden Figuren;
der ältere Dateiname `Glutwicht_N4R2_Groessenvergleich.png` zeigt den Stopp.
Bekannte Ressourcenreste beim Beenden bestehen. Druckpunkt-/Schlupffreiheit,
Tiefen-/Wendeposen, zusätzliche gemalte Verbindungen, Angriffe und Telefontest
bleiben offen. Keine APK, kein Merge.

## Reproduktion

```bash
python3 tools/motion-art/prepare_npc.py glut-imp --height 1.75 --checkpoint n4r2
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_imp.gd
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p 'test_npc*assets.py' -v
MOTION_M4=1 NPC_ID=glut_imp NPC_CAPTURE_RUN=n4_size_final godot \
  --path prototypes/godot-hd2d --audio-driver Dummy \
  --rendering-method gl_compatibility --fixed-fps 30 --resolution 640x360 \
  --script res://tests/npc_motion_capture.gd
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_glut_imp_n4_size_final \
  docs/validation/npc-n4-size-20261029
```

## Sicherung und Fortsetzung

Lokal auf `codex/n4-completion-20261029`, bestehendes Ziel ist weiterhin
`codex/npc-enemy-motion-20261027`, PR #386. Der im vorherigen Stand automatisch
abgelehnte GitHub-Push wird nicht erneut oder über einen anderen Weg versucht:
Martin hat zunächst die Figurengröße korrigiert. Vor Veröffentlichung der
korrigierten Fassung ist die vom automatischen Gate verlangte direkte
Bestätigung noch offen. Remote zuletzt `c7c90bd`, N5 noch nicht begonnen.

Nächster Schritt: neue Größe anhand der Vorschau abnehmen, den korrigierten
N4-Stand auf dem bestehenden PR-Branch sichern, erst danach N5 Siegelweber.
