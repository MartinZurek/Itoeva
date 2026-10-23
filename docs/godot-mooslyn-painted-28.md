# Mooslyn: gemalte Seitenfolgen mit 28 Phasen

## Auftrag und Umsetzung

Der Nutzer beanstandet gedehnte Beine, wechselnde Proportionen und einen unter
Tasche/Umhang verschwindenden Arm. Gewuenscht sind sieben Spalten und vier Reihen
fuer Gehen und Sprint, beim Sprint ein leicht geoeffneter Mund sowie nachlaufende
Ohren, Blaetterumhang und Tasche.

Basis: `codex/godot-full-body-gait`, Commit `c6fb73c` (ausgelieferte 0.15.0).
Die neue Gehfolge wurde mit der eingebauten Bildgenerierung nochmals aus dem
Original-Mooslyn und Fennecs Profilgang erzeugt. Die Sprintfolge stammt aus der
vorangegangenen 28-Posen-Iteration. PNGs sind unveraenderte ImageGen-Ausgaben,
je 1659x948 Pixel, echtes RGBA, 7x4 Zellen zu 237 Pixeln. Die angeforderte hoehere
Aufloesung wurde vom Bilddienst nicht geliefert; es gibt kein kuenstliches Upscaling.

`compact_actor.gd` verwendet diese Folgen nur fuer Mooslyns seitliches Gehen und
Sprinten. Sie werden in 28 Schritten streckenabhaengig abgespielt. Der ganze Atlas
wird bei Linksbewegung gespiegelt, einschliesslich Umhang, Tasche und Ohren.
Ein fester Massstab pro Folge vermeidet zusaetzliche Skalierung pro Einzelbild.
Standhoehen stammen aus der sichtbaren Alphakontur. Die alten Netz-/Beinverformungen
sind fuer diese Seitenfolgen abgeschaltet; sonst wuerde die bereits gezeichnete
Bewegung ein zweites Mal verzogen. Beim Anhalten wird direkt zur Standzeichnung
gewechselt, ohne kurz wieder die alte seitliche Netzverformung anzuzeigen.

Front/Ruecken, Springen, Rollen, Sitzen, andere Figuren und Kampfzeichnungen
bleiben auf ihren bisherigen Darstellungen. Es gibt keine neu erfundenen
Front-/Rueckposen aus einer seitlichen Zeichnung. Kein Save-Schemawechsel,
keine Aenderung an Weltgeschwindigkeit, Ausdauer, Paketkennung oder Signatur.

## Pruefung

- Godot 4.5.1: `tests/mosslyn_painted_cycles.gd`: 1032 Checks ohne Fehler im
  Headless-Lauf (Raster, 28 Phasen, beide Richtungen, konstanter Massstab,
  ausgeschaltetes Hautnetz, Licht-/Bildanker, Wechsel zu Tiefe/Sitz/Sprung/Stand).
- `tests/character_polish.gd`: 869 Checks ohne Fehler.
- `tests/stamina_motion.gd`: 204 Checks ohne Fehler.
- `tests/test_mosslyn_painted_assets.py`: Quellhash, RGBA, Raster, alle 56
  eindeutigen Bildausschnitte und Alphakonturen gegen Metadaten bestanden.
- Bestehende OpenGL-Gangpruefung erkennt fuer Mooslyns Seitenansicht jetzt den
  gezeichneten Pfad; sie verlangt dort nicht mehr das absichtlich entfernte Netz.
- Die zwei bestehenden Vollwelttests melden beim Prozessende wie zuvor noch
  Resource-Leak-Hinweise. Diese werden nicht als behobene Fehler ausgegeben.

## Grenzen

Mehr Bilder allein garantieren keinen besseren Bewegungsablauf. Die generierten
Zeichnungen haben weiterhin leichte Form-/Groessenabweichungen (Gehhoehen
223–232, Sprinthoehen 199–239 Quellpixel). Der Code verformt sie nicht, kann aber
unterschiedlich gemalte Anatomie nicht reparieren. Laufrhythmus und exakte
Sohlenhaftung sind keine aus den Tests abgeleiteten Qualitaetszusagen.
Menschliche Animationsabnahme und Android-Geraetepruefung bleiben offen.
Eine neue APK wurde fuer diesen Auftrag nicht erzeugt.

Ruecksetzen: den isolierten Aenderungscommit revertieren; alte Originalbilder,
Metadaten und Rig-Pfade sind weiterhin vorhanden. Keine Nutzerdatenmigration.

## Gerenderte Kontrolle und Uebergabe

Die Grafik-Testumgebung ist wiederhergestellt. Eine unvollstaendig entpackte
Godot-Binaerdatei verursachte den zwischenzeitlichen Startabsturz; der Vergleich
mit dem vollstaendigen ZIP-Inhalt erklaerte ihn. Nach erneutem Entpacken lief
Godot 4.5.1 mit Mesa/llvmpipe im virtuellen X11-Display.

`tests/mosslyn_painted_cycles.gd`: **1144 Checks, 0 Fehler**, jetzt inklusive
aller 112 OpenGL-Bilder (Gehen/Sprint, rechts/links, je 28 Phasen).
Auch die allgemeine OpenGL-Gangpruefung aller vier Startfiguren besteht mit
**3442 Checks, 0 Fehlern**. Charakter- und Ausdauertests wurden am finalen Stand
erneut mit 869 bzw. 204 Checks ohne Fehler ausgefuehrt.
Das ist eine echte Desktop-Renderpruefung, keine Messung auf dem Android-Geraet.

Die Kontrollvorschau zeigte zuvor Umhangfragmente aus Nachbarzellen. Deshalb
ordnet eine aus zusammenhaengenden Alphakonturen abgeleitete technische Maske
jeden sichtbaren Pixel genau einer Pose zu. Die Runtime verwendet die vollstaendige
Kontur statt einer starren Zelle; Original-PNGs bleiben unveraendert. Der
bestehende Pose-Region-Shader wird wiederverwendet. Python prueft alle 28
Zuordnungen, Grenzen und Quellhashes je Folge.

`validation/mooslyn-painted-28-preview.mp4` zeigt die echte Godot-Darstellung:
links Gehen, rechts Sprint, zur Kontrolle jeweils ein Zyklus pro Sekunde,
viermal wiederholt. Es ist eine isolierte Figurenvorschau, keine Weltaufnahme.
Kopf-/Rumpfdetails und Loopqualitaet sind weiterhin menschlich zu beurteilen.

Martin hat den GitHub-Push und den APK-Bau ausdruecklich beauftragt.
Der direkte Git-Push hat keine Shell-Anmeldedaten; die Veroeffentlichung erfolgt
ueber den verbundenen GitHub-Zugang. AndroidTest wird als 0.15.1 / Code31
mit dem bestehenden Paket, Signaturschluessel und Drive-Ziel gebaut. Der bestehende
Workflow wird fuer diesen Branch und das aktualisierte Dateimanifest angepasst.
Eine erfolgreiche APK-Lieferung wird erst nach Build- und Uploadpruefung bestaetigt.
