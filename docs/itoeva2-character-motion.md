# Itoeva 2: Charaktere, Bewegung, Raum und Musik – 09.10.2026

Basis: PR #350, `431bd178`. Neuer Branch: `codex/character-silhouettes-and-cloth`.
Martin baut die APK separat; dieser Schnitt erfordert eine spaetere eigene APK.

## Was sich sichtbar aendert

| Wesen | Statur und Bewegung |
|---|---|
| Fennec | Kleine Taillenkorrektur; der freie Mantel folgt Tempo und Wind verzoegert und schwingt beim Anhalten aus. |
| Puffling | Deutlichere Taille unter der unveraenderten Kapuze; sanfte Brustbewegung und leichter Stoffnachlauf. |
| Gloop | Keine Taille einer Knochenfigur; gekoppelte Stauch-/Streckbewegung mit kleinem Breitenausgleich. Blattkleidung bleibt leichter als Stoff. |
| Wyrmling | Kleine Taillenkorrektur; schwerere, ruhigere Koerperreaktion und Halstuchnachlauf. |
| Starlet | Sternanatomie bleibt breit; kleine eigene Gewichtsreaktion und Kragennachlauf. |
| Hootlet | Leicht schlankerer Rumpf, langsamer Atemrhythmus und geduldig bewegter Federumhang. |

Das neue 24x32-Texturnetz zeichnet zusammenhaengende Dreiecke, statt Stoffteile
in 12x12-Kacheln um gerundete Quellpixel zu verschieben. Kleine Auslenkungen
bleiben sichtbar und es entstehen keine Luecken zwischen verschobenen Kacheln.
Bitmapfilterung zeichnet Skalierungsstufen weicher; sie ersetzt keine gemalten
Konturen oder fehlenden anatomischen Quellzeichnungen.

Bei der Sekundaerbewegung bleiben Fusszeilen ab 120/128, Kopf und Kragen fest.
Der Gang hebt gezielt die freie Fussspitze und beim Rennen auch die ganze Figur an. Kleidergewichte unterscheiden
Profil von Front/Ruecken. Schlaf und Rolle behalten die kompakte Originalzeichnung.
Das Modell reagiert auf den vorhandenen Gangtakt, Tempo und Wind. Es waehlt keine
Handlungen. Die Fortsetzung koppelt Gang und Kontakte an denselben Motor;
Innenraeume haben einen kleineren Figurenmassstab. Beduerfnisse bleiben erhalten. Keine neuen Bilder-/Spielstandformate.

## Umsetzung und Leistung

- `GameCharacterMotion.kt`: reine Netzrechnung, individuelle Material-/Staturwerte,
  exakter Nachlauf zweier Daempfer und begrenzte freie Saeume.
- `GameCharacterPainter.kt`: native Android-Bitmapzeichnung mit interpoliertem
  Ortslicht, bestehendem Alpha und wiederverwendeten Arrays/Paint.
- `AvatarSpriteView.kt`: optionaler Game-Zeichenweg; einmalige Ermittlung des
  oberen Alpha-Punkts je gezeichnetem Bild im bestehenden Cache.

Der neue Weg braucht einen Netz-Zeichenaufruf pro gezeichnetem Sprite statt bis
zu 144 Kachelaufrufen; beim Gangartwechsel zwei wegen der bestehenden Ueberblendung.
Keine neuen Bitmaps oder Texturen pro Bild. Tatsaechliche GPU-Leistung und
Telefon-Bildrate sind noch nicht gemessen.

## Vorschau wiederherstellen

Nach dem Offline-Testlauf (dieser bereitet den Kotlin-Klassenpfad vor):

```bash
bash tools/reaction-preview/tests.sh
tools/reaction-preview/.work/kotlinc/bin/kotlinc \
  tools/reaction-preview/src/CharacterMotionPreview.kt \
  -cp tools/reaction-preview/.work/tests \
  -d tools/reaction-preview/.work/character-preview
tools/reaction-preview/.work/kotlinc/bin/kotlin \
  -cp tools/reaction-preview/.work/character-preview:tools/reaction-preview/.work/tests \
  CharacterMotionPreviewKt . /tmp/itoeva-character-mesh
python3 tools/character-art/game_motion_preview.py /tmp/itoeva-character-mesh \
  tools/character-art/game-motion-preview.gif
```

Die GIF zeigt dieselben Ausgangszeichnungen links/rechts in gleichem Weltmassstab.
Die neue Seite benutzt die produktive Kotlin-Netzrechnung und bilineare
Software-Texturabtastung. Sie ist keine Android-Aufnahme und prueft keine
Touch-/Lifecycle-/GPU-Wirkung. Szenen: ruhiger Wind, Gehen, Rennen, Anhalten/Ausschwingen und Schwimmzug.
Die Schwimmvorschau zeigt die Gliedmassen ohne Wasserverdeckung und Auftrieb;
das Game zeichnet beides zusaetzlich.

## Pruefstand und naechster Schritt

Der erste Charakterschnitt hatte 1.024 reine Kotlin-Tests gruen. Die Fortsetzung
erweitert auf 1.039 Tests; der finale Lauf und Android-CI stehen im PR.
Die instrumentierten Zeichentests rendern alle sechs Wesen
mit echten Android-Bitmaps, pruefen transparente Raender/Boden und die beim
Spiegeln unveraenderte physische Lichtseite.

Telefonabnahme: Alle sechs Wesen im Stand, nach kurzem Lauf und unmittelbar beim
Anhalten beobachten. Mit Fennec vorwaerts, rueckwaerts und seitlich gehen;
Wind draussen mit einem Innenraum vergleichen. Pufflings Kopf-/Rumpfverhaeltnis,
Sitzen/Aufstehen, Springen und Rollen beurteilen. Insbesondere auf dauerhafte
Flatterbewegung, abgeschnittene Saeume und Bildrate mit zwei NPCs achten.

App-1-/Stream-Zeichnung, Original-PNGs und Speicherformat bleiben erhalten.
Ruecknahme der PR-Commits auf #350 (`431bd178`) braucht keine Migration.

## Fortsetzung nach Uploadfreigabe

Martin hat den Upload und PR ausdruecklich beauftragt. Der PR richtet sich gegen
`codex/fix-world-controls-and-passages` und baut somit auf #350 auf. Zusaetzlich
sind eigenstaendige Laufbewegung, Starlets Bodenkontakt, plausible Weltbewegung
und die gemeldete Stille trotz Music on beauftragt. Diese Fortsetzung und ihr
Pruefstand werden im selben PR dokumentiert. Die separat gebaute APK aus #350
enthaelt diesen Charakterschnitt noch nicht.


## Eigenstaendige Gangarten und Schwimmen

Der Motor verlaengert bei hoeherem Tempo den Schritt: Die Schrittfrequenz steigt
nur um 42 Prozent des zusaetzlichen Tempos. Die vorhandenen Geh-/Rennzeichnungen
bleiben die anatomischen Quellen. Das Netz ergaenzt Laufneigung, Belastungsphase,
Abdruck und Flugphase; beim Gehen bleibt mindestens eine Stuetzspitze unten.
Starlet hebt seine freie untere Spitze sichtbar an. Fusskontakte/Spuren folgen
demselben 760-ms-Gangtakt statt vielen abstandsgetakteten Kontakten beim Rennen.

Schwimmen bekommt eine eigene `Motion.SWIM` auf neutralen Seiten-/Front-/Rueckposen.
Die Arme ziehen und holen zurueck, die unteren Gliedmassen schlagen unter der
Wasserlinie. Die starke 32-Grad-Neigung der bisherigen Greifpose wird durch neun
Grad ersetzt; Auftrieb, Wasserverdeckung und Wassertreten bleiben gekoppelt.
Dies ist eine kleine Laufzeitverformung vorhandener Zeichnungen, kein neu
gezeichneter anatomischer Schwimmatlas. Alle sechs Arten werden geprueft.

Laub wird in kleinen gecachten 8x16-Netzen bewegt; Rand und Ansatz federn in die
unveraenderte Malerei aus. Wetterwind erreicht Laub, Gras und Kleidung gemeinsam.
Wasser zeichnet eine stetige Texturverschiebung als Netz unter einer festen
Wasser-/Ufermaske, keine abrupt versetzten 64-Pixel-Streifen. Die nachgemalten
Weltnaehte bleiben die Texturquelle. Pro Bild entstehen keine neuen Bitmaps.

## Rechter Aktionsknopf

Der UI-Befehl wird bis zum naechsten Motorbild behalten und erst **nach** dem
Orts-/Positionsabgleich verbraucht. So kann dieser Abgleich einen gerade
angetippten Sprung nicht mit einem frischen Bewegungszustand verwerfen. Menue,
unsichtbarer Bildschirm und beschaeftigte Figur verwerfen noch offene Befehle,
so dass nach einer Pause kein verspaeteter Sprung folgt. Ein freigegebener Befehl
beendet eine noch vorhandene Sitzbindung. Tipp/Schnellloslassen springt,
horizontales Wischen rollt, langes Halten bleibt die bisherige Ruheaktion.

Ein nativer Pointertest nimmt den sichtbaren rechten Knopf und fuehrt seinen
Befehl im produktiven Motor bis zu einer messbaren Hoehe aus. Die **genaue Ursache
auf Martins Telefon ist noch nicht reproduziert**; der Test startet keinen
vollstaendigen DockScreen und ersetzt die Telefonabnahme nicht.

## Begehbare Innenraeume

Alle elf `interiors/`-Orte werden aus Boden, Rueck-/Seitenwaenden, Fenster,
Tueren und einzelnen Moebelkoerpern gezeichnet. `GameRoomSpace` liefert sichtbare
Ober-/Seiten-/Vorderflaechen; `GameFurniture` liefert dazu dieselben Stand-,
Sitz-, Landeflaechen und Grundrisse. Bett und Tische haben echte Tiefe; der
Schlafzimmerschrank steht ausserhalb der Liegeflaeche. Materialdetails, Fenster,
Wandbilder, Buecher, Cafe-Tafel und Kaffeemaschine unterscheiden die Orte.

Der begehbare Boden reicht von y=116 bis 256 statt eines schmalen Streifens. Die
Figur ist hinten kleiner, vorne groesser; der zusaetzliche Innenraum-Nahzoom
entfaellt. Man kann hinter das Sofa und zwischen Tischbeinen sehen: Verdeckung
und Licht verwenden die **sichtbaren Koerperflaechen**, kein Hintergrundrechteck.
Tueren oeffnen als eigene Blaetter. Die alten Illustrationen bleiben als Assets
kompatibel, werden in diesen Raeumen jedoch nicht als Kulisse gezeichnet.

Es ist eine raeumliche 2.5D-Projektion mit festem Blickwinkel; keine frei drehbare
3D-Kamera. Der Aussenhuetten-Grundriss wird nicht neu gemalt. Die Aenderung behebt
den zu nahen Figurenmassstab und den engen Boden **innerhalb** der Raeume.

[Raumvorschau](../tools/character-art/room-space-preview.png): produktive Kotlin-
Moebelflaechen und Figurenmassstab, vereinfachte Softwarezeichnung ohne das
native Licht-/Tuer-/Materialfinish; keine APK-Aufnahme. Regeneration:

```bash
tools/reaction-preview/.work/kotlinc/bin/kotlinc tools/reaction-preview/src/RoomSpacePreview.kt \
  -cp tools/reaction-preview/.work/tests -d tools/reaction-preview/.work/room-preview
tools/reaction-preview/.work/kotlinc/bin/kotlin \
  -cp tools/reaction-preview/.work/room-preview:tools/reaction-preview/.work/tests \
  RoomSpacePreviewKt /tmp/itoeva-rooms
python3 tools/character-art/room_space_preview.py /tmp/itoeva-rooms \
  tools/character-art/room-space-preview.png
```

## Musik trotz Music on

`PlayMusic.createScorePlayer` setzt MEDIA/MUSIC bereits beim Erstellen des Players,
bevor Android ihn vorbereitet. Ein nicht mehr laufender Decoder wird beim Abgleich
freigegeben und neu gestartet. Aktivierte, voruebergehend gesperrte Musik wird nach
zwei Sekunden erneut geprueft. Im Game bestimmt die **Medienlautstaerke** die Stille;
Klingelmodus stumm/vibrieren blockiert ausdruecklich eingeschaltete Spielmusik nicht.
App 1 und Stream behalten ihre Klingelsperre. Musik-Aus, fremde Wiedergabe und
Medienlautstaerke null bleiben wirksam; Systemlautstaerke wird nicht verstellt.

Zwei reine Tests pruefen diese Trennung. Ein instrumentierter Test startet den
ausgelieferten Tagestrack mit dem produktiven Medienplayer und prueft Decoderlauf
und Zeitfortschritt. Er ist bewusst stumm und belegt **keinen Hoertest**. Auf dem
Telefon: Medienlautstaerke >0, Music on/off, Klingeln stumm, Ortswechsel sowie
App verlassen/zurueckkommen pruefen.

## Abnahme auf dem Telefon

Rechts tippen im Stand und mit linkem Daumen laufen; Hoehe, Landung und erneuten
Sprung beobachten. Gehen/Rennen in vier Richtungen mit allen Wesen vergleichen.
Schwimmen vor/zurueck/seitlich und ohne Eingabe pruefen. In Wohnzimmer, Schlafzimmer
und Cafe hinter/zwischen Moebeln gehen, sitzen, auf Tisch/Bett springen, Tueren
wechseln und Licht an/aus pruefen. Pixelstil, Raumweite und Figurenmassstab beurteilen.
Der native Canvas-Test prueft die Raumzeichnung ohne Hintergrundbitmap.
Android-CI auf `3987cf4`: 116 instrumentierte Game-Tests auf API 26 und die
App-Tests auf API 35 bestanden; finale Nachpruefung der kleinen Tuer-/Schrank-
Korrektur auf dem aktuellen PR-Head. Der Schlafzimmerschrank steht neben dem
Badezimmerdurchgang, Wandregal und Spiegel lassen die Tuerblaetter frei. Bildrate,
Klang am Lautsprecher und die gemeldete Bedienung auf Martins APK bleiben offen.


## Innenraeume nach den Konzeptstudien (room-concept-pixels-v4)

Martin beauftragt am 09.10. explizit den Konzept-Art-Stil fuer die Innenorte.
Die Referenzen sind [Cozy Home](https://github.com/MartinZurek/Itoeva/blob/art/concept-studies-2026-10-04/docs/concept-art/world-studies/cozy-home-world-study-v1.png)
und [Park, Lake, Home](https://github.com/MartinZurek/Itoeva/blob/art/concept-studies-2026-10-04/docs/concept-art/world-studies/itoeva-park-lake-home-concept-v1.png).
Ihre Farb-/Materialwelt wird in gesetzte Pixelgruppen uebertragen: cremefarbene
Holzwaende, warme Balken, Bogenfenster mit kuehler Ferne, bernsteinfarbene Lampen,
Salbei-/Terrakottatextilien, Teppichmuster, Buecherruecken und kleine Pflanzen.
Sofa/Lehnstuhl und Bettkissen erhalten abgeschraegte weiche Kanten; die Wanne
einen offenen Rand und eine gerundete Schale. Holzpaneele, Messinggriffe,
Kaffeegeraet und Spielautomat unterscheiden die vorhandenen Moebel.

`GameRoomArt` beschreibt die Materiallagen als reine Pixelprimitive. Der Android-
Renderer rastert sie einmal auf 480x270 und zeichnet den Cache ohne Bitmapfilter;
Tuerblaetter verwenden dieselbe Holzpalette und folgen weiter dem Aufschwingen.
Bewegte Vorhaenge sowie Tageslicht/Ortslichter bleiben eigene Laufzeitlagen.
Es werden keine originalen Raum-PNGs als flacher Ersatz geladen. Alle elf
Innenorte behalten Raumtiefe, ihre vorhandenen Lauf-/Sitz-/Landeflaechen und
Tueroeffnungen. Die neuen grossen Koerperkonturen sind zugleich Verdeckungskonturen.
Materialdekore werden auf ihre sichtbaren Moebelflaechen begrenzt; Wandpflanzen
und flach liegende Teppiche fuehren keine unsichtbaren Bodenhindernisse ein.

[Vorher/Nachher](../tools/character-art/room-concept-comparison.png),
[drei Raeume mit Verdeckung](../tools/character-art/room-concept-preview.png) und
[alle elf Innenorte](../tools/character-art/room-concept-all-preview.png) werden
jetzt aus denselben produktiven Pixelprimitiven wie der Android-Renderer erzeugt.
Es sind Softwarevorschauen bei geschlossenen Tueren, ohne die zusaetzlichen
Licht-/Atmosphaerenlagen und ohne animierte Vorhangphase, keine APK-Aufnahmen.
Pillow und Android unterscheiden sich an einzelnen Rasterkanten.

Regeneration nach `bash tools/reaction-preview/tests.sh`:

```bash
tools/reaction-preview/.work/kotlinc/bin/kotlinc \
  tools/reaction-preview/src/RoomSpacePreview.kt \
  -cp tools/reaction-preview/.work/tests -d tools/reaction-preview/.work/room-concept
tools/reaction-preview/.work/kotlinc/bin/kotlin \
  -cp tools/reaction-preview/.work/room-concept:tools/reaction-preview/.work/tests \
  RoomSpacePreviewKt tools/reaction-preview/.work/rooms-concept
python3 tools/character-art/room_space_preview.py \
  tools/reaction-preview/.work/rooms-concept tools/character-art/room-concept-preview.png
```

Pruefstand: 1.041 reine Kotlin-Tests gruen; nach der visuellen Verfeinerung erneut
acht Raumtests gruen. Der native Canvas-Test umfasst jetzt alle elf Raeume und
prueft sichtbare Materialvariation. Android-CI und Telefonabnahme stehen im PR.
Der lokale Android-Versuch konnte wegen des fehlenden Gradle-Wrapper-JARs nicht
starten. Auf dem Telefon besonders schmale Vorhang-/Nahtpixel, Wannenrand,
Sofa-Verdeckung, Landungen, Tueroeffnung und den ersten Raumwechsel pruefen.
Keine Spielstandmigration. Ruecknahme nur des v4-Commits erhaelt die vorherigen
Bewegungs-/Musik-/Eingabefixes, insbesondere Claudes frische Freigabepruefung
vom aktuellen Basis-Head `fba7a969`.
