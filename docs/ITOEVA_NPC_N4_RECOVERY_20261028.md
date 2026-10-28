# N4 Glutwicht – Wiederaufnahme nach Workspace-Reset

## Verbindlicher Status

Der letzte vollständige Figurencheckpoint bleibt N3, f58562e958569d93afd510cb3814c6f6fbba3a25. N4 wurde in dieser Sitzung lokal fertiggestellt und geprüft, aber der Git-Push konnte sich nicht anmelden. Die anschließende Übertragung über die verbundene GitHub-Schnittstelle wurde nur teilweise abgeschlossen. Nach dem Reset fehlen die Arbeitskopie und die ursprünglichen Imagegen-Dateien im lokalen Dateisystem. Eine Suche nach den Bildern hat sie nicht wiederhergestellt.

Dieser Commit sichert ausschließlich vorhandene, anhand ihrer früher gemessenen Git-Blob-SHAs identifizierte Dateien. Er ist **kein vollständiger N4-Abschluss**. Die ersten drei Figuren und die vier Hauptfiguren aus APK 0.15.8 werden nicht wiederholt.

## Vor dem Reset belegter lokaler Stand

- Lokalcommit: 643f0527f830c3dfb262ac4d7d04cea00f66e10d.
- Vollständiger lokaler Git-Tree: af2a135176a29e943afb20183515a5fe17106391.
- Acht Gehzeichnungen und vier Ruhezeichnungen; gezielte Gegenbeinquelle.
- 120 N4-, 54 N1-, 68 N2-, 120 N3-, 47 Revier- und 1798 M4-Checks bestanden, insgesamt 2207. Vier Python-Assetprüfungen bestanden.
- 600 echte Weltbilder bei 30 fps am Aschenstieg mit Mooslyn. Alle acht Gehphasen links/rechts, alle vier Ruhephasen in beiden Stopps und Wiederanlaufen. Pixelmaßstab 0.00551635539159179, Bodenoffset 168.
- Der Bericht und die Vorschau-Dateien sind erhalten; Rohbilder, Telemetrie und Testlogs wurden nicht vollständig übertragen. Die alten Zahlen dürfen daher nicht als erneute Validierung dieses Recovery-Standes ausgegeben werden.

## Erhaltene Dateien

Unter docs/validation/npc-n4-20261028/: Weltfilm, Detailfilm, Größenvergleich, Phasen-/Übergangsbogen und glut-imp-review.json. Zusätzlich ist der gepixelte Gehatlas assets/npc_motion/pixel/glut-imp-walk-n4.png erhalten. Der historische N4-Text ist mit einem Warnhinweis versehen.

Die frühere Liste aller 39 Dateipfade und Git-Blob-SHAs steht in n4-recovery-manifest.json. 404 bedeutet fehlendes Objekt; ein UnicodeDecodeError bei fetch_blob bedeutet lediglich, dass das Objekt binär ist. Solche Binärobjekte wurden über create_tree anhand ihrer SHA erfolgreich validiert. Eine fremde, abgeschnittene Quellenübertragung mit SHA 3981ebfeb9fe46a31fdac063467fe5c522204f1f wurde bewusst nicht übernommen.

## Nächster konkreter Schritt

1. Neueren Branchstand prüfen, AGENTS.md und Einstiegsdokumente lesen.
2. Fehlende Originalquellen aus einer verifizierten Sicherung wiederherstellen. Falls nicht verfügbar, neue Quellen nach der Glutwicht-Vorlage erstellen und als neue Fassung kennzeichnen. Die alten Prüfresultate gelten nicht automatisch für neue Quellen.
3. Quellen früh als eigenen Recovery-Checkpoint sichern, danach Registrierung, Pixelpass, Metadaten und Tests wiederherstellen. Große Base64-Ausgaben in Stücke unter 700000 Zeichen lesen und jede neue Blob-SHA gegen die lokale Git-SHA prüfen; einzelne exec-Ausgaben können abgeschnitten werden.
4. N4 erneut mit echten Weltbildern, beiden Richtungen, zwei Ruhefenstern und Wiederanlaufen prüfen, vollständigen Diff prüfen und GitHub-Tree mit dem lokalen Tree vergleichen.
5. Erst danach N5 Siegelweber und die gesicherte Liste fortsetzen. Die ausdrückliche Push-Freigabe bleibt bestehen. Kein Merge, keine APK.
