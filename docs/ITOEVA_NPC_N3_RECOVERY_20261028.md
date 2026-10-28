# N3 Rindenwaechter – Wiederaufnahme nach Arbeitsumgebungs-Reset (28.10.2026)

## Massgeblicher gesicherter Stand

Branch: `codex/npc-enemy-motion-20261027`, PR #386.
Letzter vollstaendiger Charakter-Checkpoint: N2 Dornmotte,
`485b841e51303455c973df1b45c6e54d4f4bac36`.
N1 Aschenlaeufer, N2 Dornmotte und die vier Hauptfiguren aus APK 0.15.8 bleiben erhalten.
**N3 ist noch KEIN vollstaendig gesicherter Charakter-Checkpoint. Nicht mit N4 beginnen.**
Keine neue APK, kein Merge.

## Was in der unterbrochenen Sitzung tatsaechlich bearbeitet wurde

N3 bark_guardian erhielt lokal acht gemalte Gehphasen und vier separat
nachgezeichnete Ruheposen. Die erste Ruhefolge richtete sich zu stark auf
und wurde ersetzt. Gesicht, Auge, Brustkristall, Blueten und Zweigkrone
orientierten sich an der untersten Reihe von
`prototypes/godot-hd2d/assets/expansion/enemies.png`.

Registrierungsentwurf:
- Ganze Zeichnungen uniform skalieren, keine Gliedmassenverformung.
- Gehquelle: 1536x1024, 4 Spalten / 3 Reihen; erste acht Figuren nutzen.
  Zweite Reihe blickt rechts und wurde als ganze Zeichnung gespiegelt.
- Separate Ruhequelle: 1536x1024, 2x2 Figuren.
- Zellgroesse 448, vier Atlas-Spalten, head_width 14, head_center_x 180,
  ground 392, reference_height 280, cycle_distance_pixels 200,
  world_height 1.6; Laufzeitoffset 168.
- Irisregionen Gehen: [120,97,16,14], [492,100,16,14],
  [870,90,16,14], [1277,95,16,14], [242,421,16,14],
  [634,422,16,14], [989,418,16,14], [1407,425,16,14].
  Die letzten vier mit mirror_x=true.
- Irisregionen neue Ruhequelle: [333,139,23,20], [993,143,22,19],
  [334,622,23,20], [992,626,22,19].
Diese Annotationen gelten NUR fuer die verlorenen Originaldateien, nicht
unbesehen fuer eine erneute Generierung.

Beobachtete lokale Pruefungen vor dem Reset:
125 N3-, 54 N1-, 68 N2-, 47 Revier- und 1798 M4-Checks ohne Fehler;
drei Python-Assettests bestanden. N1/N2 wurden erneut erzeugt und blieben
bytegleich. Diese Logs fehlen nach dem Reset und sind daher in diesem
Commit **keine reproduzierbare Abschluss-Evidenz**.

Die echte OpenGL-Aufnahme verwendete Godot 4.5.1, Mesa llvmpipe und Mooslyn
als Groessenvergleich am Muehlenbach. Die neue gedrungene Ruhehaltung wirkte
passender als der verworfene erste Entwurf.
Die finale Fassung hatte 600 Bilder / 20 Sekunden bei 30 fps:
Stopps 120–194 und 405–479, Rueckweg-Uhr bei Bild 270 auf period*0.62.
Das erste 600-Bilder-Schema mit zweitem Stopp 480–554 war ungeeignet:
Das Revierziel war vorher erreicht, daher fehlte Wiederanlaufen. Dieses
Schema NICHT als bestandene Wiederanlaufpruefung verwenden.
Die korrigierte 600-Bilder-Aufnahme lief vollstaendig durch; ihre
abschliessende automatische Auswertung und Sichtpruefung waren beim Reset
noch nicht abgeschlossen.

## Verlust und bereits ausgefuehrte Uebertragung

Die lokale Arbeitskopie, die Bildquellen, Atlanten, Aufnahmen, Testlogs
und der temporaere SHA-Katalog waren nach dem Reset der Arbeitsumgebung
nicht mehr vorhanden. 25 Git-Blob-Objekte waren vorher erfolgreich ueber
den GitHub-Connector erzeugt und jeweils gegen den lokalen Git-Blob-SHA
verglichen worden. Es wurde jedoch noch KEIN Tree/Commit daraus erzeugt
und der Branch wurde nicht auf diese Arbeit vorgerueckt.
Die SHAs dieser losen Objekte lagen nur im verlorenen Laufzeitspeicher;
die Existenz loser Blobs ist kein verfuegbarer oder gesicherter Checkpoint.

Direkter Git-Push scheiterte an fehlenden Git-Zugangsdaten
("could not read Username"). Das war keine automatische
Freigabeablehnung. GitHub-Connector-Schreibzugriff funktionierte.
Die ausdrueckliche Nutzerfreigabe zum Sichern ALLER Charakter-Checkpoints
gilt unveraendert, nicht erneut nachfragen.

## Konkreter naechster Schritt

1. AGENTS.md, verlangte Einstiegsdokumente und N1-/N2-Uebergaben lesen.
   Branch/PR zuerst auf neue Aenderungen pruefen.
2. Die im Chat dargestellten N3-Bildquellen nach Moeglichkeit als
   Originaldateien wiederherstellen. Falls sie nicht verfuegbar sind,
   N3 neu zeichnen und neu annotieren; keine alte Quellenpruefsumme erfinden.
3. Quellen und vollstaendige Prompts sofort dauerhaft sichern, zusammen mit
   einem SHA-Katalog. Keine grossen ungesicherten lokalen Arbeitsschritte.
4. prepare_cycle.py braucht optionales Ganzbildspiegeln:
   horizontal_scale = -scale fuer mirror_x, sonst scale;
   dx = head_center_x - (x+w/2)*horizontal_scale;
   warpAffine mit horizontal_scale auf x, scale auf y.
   mirror_x in Registrierungsmetadaten festhalten.
5. N3-Atlanten, Pixelpass, Metadatei, gezielte Zustands-/Assettests und echte
   Weltaufnahme erneut herstellen. Alle acht Phasen rechts/links, alle vier
   Ruhephasen je Stopp, feste Position innerhalb jedes Stoppfensters und
   echte Ortsbewegung nach beiden Fenstern pruefen.
6. Erst den vollstaendigen N3-Checkpoint mit Quellen, Code, Tests, Vorschau
   und Uebergabe committen und Branch-Kopf auf GitHub verifizieren.
7. Erst DANACH N4 Glutwicht, dann die Liste aus dem N1-Savespot weiterfuehren.

Vollstaendige Sohlendruckpunkt-/Schlupfmessung, neue Tiefen-/Wendeposen,
eigene gemalte Ruheverbindungen aus jeder Gehphase sowie Kampfangriffe
waren weiterhin offen. Fester Massstab allein beweist keine Schlupffreiheit.
Hauptfiguren, Spieltempo, Revierwege, Decks und Speicherformat bewahren.
