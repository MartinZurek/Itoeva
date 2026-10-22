# Ganzkoerperbewegung 0.15.0

Martins Rueckmeldung vom 22.10.2026 verwirft die Bewegungsqualitaet von 0.14.2:
fast nur die Fuesse bewegen sich, der Koerper wirkt wie ein Sticker; Tempo und
Schritttakt wurden zu stark beruhigt. Starlit soll schweben und huepfen statt
auf wechselnden Sternspitzen zu laufen. Diese Rueckmeldung ist die visuelle
Abnahme von 0.14.2 und hat Vorrang vor den damaligen Testzahlen.

## Nachvollziehbare Ursache und Referenz

`compact_actor.gd` / `painted_character.gdshader` von PR378 halten Kopf und Rumpf
absichtlich auf einer festen Zeichnung und bewegen vorwiegend Gliedmassen.
Die damalige `gait_review.gd` verlangte sogar nahezu unveraenderte Kopfbereiche.
Das war eine ueberzogene Reaktion auf die zuvor zappelnden, wechselnden Zeichnungen.

Referenz fuer Fennec unmittelbar vor der Viererauswahl: PR372,
`1c82903118a4d92381e738ba652957be6070d5d8`,
`assets/characters/fennec.png`, seitliche Gehbilder9–16. Der damalige
`character_actor.gd` schaltet gezeichnete Ganzkoerperposen; Arme, Becken,
Kopfhaltung, Mantel und Schweif nehmen an der Bewegung teil. Diese Achtphasen-
Folge wurde als Kontaktbogen betrachtet, nicht nur aus einem alten Bericht zitiert.
Die neuen gemalten Figuren behalten ihre eigene Identitaet und Proportionen.

## Umsetzung

Die vier Spieler verwenden ein zusammenhaengendes, fein unterteiltes Hautnetz
ueber ihrer gemalten Zeichnung. Echte Geometrie folgt Hueften, Knien und Pfoten;
Hals/Kopf drehen gegen den Rumpf, Schultern und Ellbogen bewegen sich getrennt.
Ohren, Schweif, Umhang beziehungsweise Fluegel folgen mit Phasenversatz.
Es gibt weder optischen Fluss zwischen Farbzeichnungen noch ausgeschnittene
bewegliche Fuesse unter einem festgehaltenen Oberkoerper.

- Mooslyn: aufrechter Gang mit Gewichtsverlagerung, gegenlaeufigen Armen,
  federndem Becken, Kopfstabilisierung und nachlaufendem Blaetterumhang/Schweif.
- Nocti: koerperlicher Bodengang; beim Fliegen bewegen sich Rumpf, Kopf und
  gegliederte Fluegel gemeinsam. Flug bleibt bei1,8 Zyklen/Sekunde.
- Wyrmling: eigener tieferer Rumpftakt, vier versetzte seitliche Pfoten,
  beim Joggen diagonale Paarung, mitbewegter Schweif und Fluegel.
- Starlit: kurzer Abstoess-/Federkontakt und laengerer Schwebe-Huepfbogen.
  Der ganze Stern hebt ab; keine abwechselnden Sternbeine. Eine kleine
  elastische Kompression und verzogener Schal begleiten den Bogen. Der
  Schatten folgt der sichtbaren Hoehe; der physische Sprungknopf bleibt echt.

Gehen2,0→2,35; Sprint3,3→3,85 statt frueher4,6. Die Tempokorrektur gilt ausschliesslich dem Spieler; Bewohner behalten2,0/3,3
und damit ihren bisherigen Spaziergangstakt. Die Laufhysterese liegt
relativ zum neuen Gehtempo, damit Erschoepfung nicht in einer Laufpose haengen
bleibt. Die Schrittstrecke1,75/2,15 ergibt bei Volltempo etwa1,34/1,79
Zyklen/Sekunde. Der Rhythmus bleibt streckenabhaengig und stoppt an Hindernissen.
Ein kurzer Auslauf blendet die Gelenkbewegung vor der Standpose ab.

Fennec, Gloop, Puffling und Hootlet bleiben Bewohner mit den bestehenden
Darstellungen. Die vertiefte Bewegungsueberarbeitung und ihre Abnahme betreffen
wie beauftragt die vier Spielerfiguren. Alte Original-PNGs bleiben bytegleich;
technische neue Hautgewichte belegen bei allen fuenf geladenen Varianten
zusammen3,75MiB. Netz und Gewichtstexturen werden wiederverwendet.

## Pruefung und Grenzen

- Reproduzierbarkeit, Quellhashes und weiche Gelenkgewichte mit Python geprueft.
- Ausdauer, Analogtempo, Wechsel zur Erschoepfung, Sitz, vier Startfiguren,
  alte Profile und Helferidentitaeten in den bestehenden Spieltests geprueft.
- Aktive OpenGL-Sequenzen bei24Hz fuer vier Spieler, drei Ansichten und
  Gehen/Joggen/Flug. Geprueft werden sichtbare Ganzkoerperbewegung, kontinuierliche
  Folge, vollstaendige Kontur, Starlits Huepfbogen und der Wechsel zum Sitzen.
  Eine unbewegliche Kopfzeichnung ist ausdruecklich kein Qualitaetskriterium mehr.
- Die reine Originalkonturpruefung bleibt mit deaktiviertem Hautnetz bestehen.
- Bekannte Katalogressourcenmeldungen beim Testprozessende bleiben dokumentiert.

Grenzen: stilisierte Kontakte, keine exakt weltfesten Sohlen, keine echten
3D-Drehungen und keine neu gezeichneten verdeckten Koerperseiten. Die Vorschau
ist eine echte Desktop-OpenGL-Darstellung; sie belegt keine Telefon-GPU/FPS.
Martins menschliche Animationsabnahme bleibt erforderlich; technische Checks
werden weiterhin nicht als Beleg fuer endgueltig natuerliche Bewegung gewertet.

## Wiederaufnahme und Lieferung

Branch `codex/godot-full-body-gait` baut auf PR378 auf, ohne das aeltere main zu
mergen. AndroidTest0.15.0/Code30 verwendet dieselbe Paketkennung, dasselbe
hinterlegte Debug-Zertifikat und die bisherige freigegebene Drive-Zieldatei.
Ein erfolgreicher Build samt Remote-Pruefsummenabgleich muss die Lieferung
belegen; vorbereitete Versionsnummern bedeuten noch keine fertige APK.

Kein Save-Schemawechsel. Zum Ruecksetzen den isolierten Aenderungscommit
revertieren; fuer ein APK-Downgrade den lokalen Godot-Stand vorher sichern.

## Nachpruefung nach erstem vollstaendigen Baulauf

Der erste Baulauf39693465912 stoppte vor dem Export am Bewohnertest: Das neue
Spielertempo wurde durch die gemeinsame Basisklasse auch fuer Nella verwendet.
Die Tempoableitung trennt jetzt Spieler und Bewohner. Der unveraenderte
Bewohnertest besteht lokal wieder mit690/0; Ausdauer204/0 bleibt erfolgreich.
Die allgemeine Projektpruefung39693485456 war bereits erfolgreich.

Reviewbefund zur Sichtbarkeit: Das Hautnetz speichert Rohpixel; ein reines
Skalieren im Vertex-Shader laesst seinen CPU-Begrenzungskasten viel zu gross.
Ein zum aktuellen Pixelmassstab passender, konservativer `custom_aabb` umfasst
jetzt Gelenkausschlaege und Billboard-Drehungen. Die aktive OpenGL-Pruefung
kontrolliert zusaetzlich die sinkende Zahl eingereichter Objekte, wenn ein Wesen
weit ausserhalb des Kamerabildes steht. Keine Telefon-FPS aus dieser Pruefung abgeleitet.

## Lieferung abgeschlossen

Test-APK0.15.0/Code30 ist fertig, signiert und vollstaendig am
[bisherigen Downloadziel](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk)
abgelegt. Paketkennung und bisheriges Zertifikat erlauben das Update zu0.14.2.

- Quellcommit: `42d6ff7fb3f0447ff78193bc957763069c54d2ee`.
- [Baulauf39749041384](https://github.com/MartinZurek/Itoeva/actions/runs/39749041384),
  Job119724660278: Spielregressionen, Renderansichten, Export, Signatur und Upload erfolgreich.
- 193005716 Bytes; SHA-256 `2276e3ce7d82f194f4995616f8a0ed6355c758eeb2be418721daf6e2ebdf6faa`.
- Remote-Groesse und MD5 stimmen mit der vollstaendigen lokalen APK ueberein.
- 3540 aktive OpenGL-Bewegungs-/Ausblendchecks und1088 Originalkonturchecks bestanden.
- Die allgemeine Projektpruefung39749047189 einschliesslich Gradle/Lint/R8,
  API26 und API35 ist ebenfalls erfolgreich.
- Zwoelfsekundiges Video mit vier Spielern, drei Ansichten und24Bildern/Sekunde
  erstellt, vollstaendig dekodiert und dauerhaft gesichert.

Liefernachweis: `docs/validation/godot-v0150-build-report.json`.
Der zuerst abgebrochene Baulauf lieferte keine Zwischen-APK aus. Kein Merge in main.
Weiterhin offen: Martins menschliche Bewegungsabnahme und Telefon-GPU/FPS.
