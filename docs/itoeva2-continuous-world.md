# Itoeva 2 - Gemeinsame Landschaft und Kamera

Stand 07.10.2026, `continuous-world-v1`, auf dem aktuellen Charakterstand `4fad98d`.

## Spielerlebnis

Strasse, Park, Wiese und Wald bilden ein einziges Panorama. Links/rechts laeuft man ohne
Schwarzblende, Stillstand am Rand oder neue Gangphase zwischen diesen Abschnitten. Die
Ortsnamen bleiben fuer Bewohner, Entdeckungen und Spielstand erhalten. Die Abzweige zu allen
anderen Orten sind weiter begehbar; Gebiets- und Gebaeudeeingaenge behalten die Ueberblendung.

Eine sanft folgende Kamera schaut in Laufrichtung voraus, zeigt bei schnellem Laufen mehr
Umgebung und zoomt bei nahen Aktionsplaetzen/Entdeckungen behutsam heran. Getrennte Ein- und
Austrittsschwellen verhindern Wechsel an der Reichweitengrenze. Menues und Hintergrund
pausieren auch die Kamera. Das Panorama wird einmal geladen, nicht bei jedem Ortswechsel.

Die neue Karte zeigt Landschaft, Gebaeude, benannte Orte, den Standort und Wege aus den
wirklichen Game-Ausgaengen. Die gemeinsame Promenade ist ein durchgehender breiter Weg.
Der alte Stream behaelt seine eigene Kartenansicht.

## Technik und bestehende Staende

- `GameWorld.scene` ist die Game-spezifische Geometrie. Der urspruengliche `GameScenes`-Katalog
  bleibt fuer die alte Welt unveraendert. Ein gemeinsamer Boden und dieselbe Figurgroesse
  sichern den Randuebertritt; die beiden aeusseren Eingaenge haben Platz fuer den ganzen Sprite.
- `GameCamera` rechnet eine immutable Projektion. Bild, Schatten, Spieler, Bewohner, Requisiten,
  Materialkontakte, Sprunghoehe und Touch-Rueckprojektion benutzen diese Projektion.
- Die vorhandene `GameMovement`-Physik bleibt die einzige Physik. Nur das Warten am gemeinsamen
  Rand entfaellt; Geschwindigkeit, Blickrichtung, Laufphase und Gangart bleiben erhalten.
- Vier Bewohnerdarstellungen werden aus denselben Population-Snapshots fortgeschrieben.
  Ein Profil wird beim Ortswechsel genau einmal sichtbar. Es gibt keine neue NPC-KI.
- Orts-IDs und relative Bodenpositionen bleiben das gespeicherte Format. Der Codec prueft
  Sicherheitsanker gegen die aktive Weltgeometrie. Es gibt keine neue Preference, kein
  Room-Schema und keine Migration. Die relative Position bleibt erhalten, ihre Kulisse ist neu.
- Das Panorama hat 2172 x 724 Pixel, etwa 6 MiB dekodiertes RGBA. Gras, wenige fallende Blaetter,
  Materialkontakte, vorhandene Tageslichtrechnung und Regen werden zur Laufzeit gezeichnet.
  Die Baumstamm-/Laubsilhouetten des neuen Bildes sind noch keine ausgeschnittenen Bewegungsebenen.

## Validierung

Am 07.10.2026 liefen alle **937 Kotlin-Tests erfolgreich**, einschliesslich der neuen Welt-
und Kameratests. `git diff --check` ist sauber. Die lokale Android-Kompilierung erreichte hier
den Quellcode nicht: der Online-Lauf wurde vor der Netzwerkfreigabe beendet, offline fehlten
Gradle-/Android-Abhaengigkeiten. Es gibt noch keine neue APK
und keine Telefonabnahme dieses Schnitts.

Remote-CI im PR #340: App-Kompilierung und instrumentierte Tests auf API 26/35 bestehen.
Der erste Verify-Lauf #842 fand im neuen Asset-Test einen Android-Test-Klassenpfadfehler:
`javax.imageio` ist dort nicht vorhanden. Der Test liest deshalb jetzt die PNG-Signatur und
IHDR-Abmessungen direkt, ohne Desktop-Bildbibliothek. Der finale Verify-Stand steht im PR.

Die Kotlin-Strecke prueft geometrisch identische Naehte, Momentum/Gangphase, Kameraprojektion
und inverse Touch-Koordinaten, Nachlauf bei verschiedenen Bildraten, Zoom-Hysterese,
Bildabdeckung nach Formatwechsel, Speicherwiederherstellung, Funde und Erreichbarkeit.
Ein Bewohnerwechsel darf kein Profil verdoppeln. Der bisherige Test fuer moebelfreie
Game-Anker wurde auf die tatsaechlich benutzte Game-Geometrie umgestellt.

`tools/world-art/preview_world.kt` rendert die echte Kotlin-Bewegung und Kamera mit dem neuen
Panorama und dem aktuellen Fennec-Bogen in `continuous-world-preview.mp4`. Das ist eine
Desktop-Darstellung, keine Android-Aufnahme. Reproduzierbar nach der Kotlin-Teststrecke mit:

```bash
CACHE=tools/reaction-preview/.work
mkdir -p /tmp/world-preview/classes /tmp/world-preview/frames
"$CACHE/kotlinc/bin/kotlinc" tools/world-art/preview_world.kt -cp "$CACHE/tests" -Xfriend-paths="$CACHE/tests" -d /tmp/world-preview/classes
java -Djava.awt.headless=true -cp "/tmp/world-preview/classes:$CACHE/tests:$CACHE/kotlinc/lib/kotlin-stdlib.jar" com.notime.glyphsim.matrix.Preview_worldKt "$PWD" /tmp/world-preview/frames
ffmpeg -y -framerate 15 -i /tmp/world-preview/frames/frame-%03d.png -c:v libx264 -crf 23 -pix_fmt yuv420p -movflags +faststart tools/world-art/continuous-world-preview.mp4
```

Die lokale Reviewvorlage steht in [continuous-world-review.md](continuous-world-review.md).
Der erste Push zu `MartinZurek/Itoeva` wurde durch die automatische Freigabepruefung blockiert.
Martin hat das Hochladen und Anlegen des Pull Requests am 07.10.2026 ausdruecklich freigegeben.
Der finale Android-/CI-Stand wird im Pull Request festgehalten.
Am Telefon offen: zwei Daumen gleichzeitig, Doppeltipp nach einem Zoom, Bank/Stamm/Kiste,
Gespraech und Neustart auf beiden Seiten eines Ortsrandes, seitliche Abzweige, GPU/Speicher/Bildrate.

## Folgeschritte und Ruecksetzung

Dieser Schnitt verbindet die erste Gegend. Neun Innenraeume nutzen weiterhin die alten Bilder;
Sportplatz, Teich, Stadtmitte und Wildnis bleiben einzelne Kulissen. Sie brauchen eigene
Bild-/Boden-/Interaktionsabnahmen, bevor weitere gemeinsame Gebiete entstehen. Fuer das neue
Panorama folgen ausgeschnittene Wind-/Wolkenschichten und eine genaue Abnahme der Moebelanker.

Ruecksetzung: den Welt-/Kameracommit nach `4fad98d` zuruecknehmen. Es gibt keine Datenmigration;
Orts-IDs, Fund-/Chronik- und Inventarformat werden weiterhin vom vorherigen Client gelesen.
Eine Position kann dabei gegen die alten Moebel auf einen sicheren Bodenpunkt verschoben werden.
