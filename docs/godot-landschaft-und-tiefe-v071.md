# Landschaftsausbau und Überdeckungen nach 0.7.0

Basis: `c732fd8cbffb692b3f8ad3d0c8d5c06ce7df2ccf` / PR #363.
Branch: `codex/godot-landscape-depth-v071`. Diese Fassung ergänzt den Quellstand;
die bereits ausgelieferte AndroidTest-APK bleibt zunächst 0.7.0/Code19.

## Sichtbare Änderungen

Der **Mühlenbach** erweitert den hellen Weltteil am Sonnenhain. Die vorhandenen
Landschaften Sonnenhain/Nebelwacht und elf Bewohner wurden zuerst geprüft und
weiterverwendet. Ein neues gemaltes Panorama mit alter Wassermühle, Bach und
Birken folgt ihrem Detailgrad. Ein breiter trockener Weg bleibt frei von Wasser
und Vordergrundfelsen. Wasserbewegung verwendet die vorhandene, eng auf den Bach
begrenzte Ambience-Maske. Hin- und Rückweg führen zum Sonnenhain.

**Miro**, ein Biber als Mühlenwart, ist der zwölfte Bewohner und vierte neue NPC.
Er hat ein eigenes transparentes Blatt mit sechs Stand-, Geh- und Arbeitsposen,
Rolle, Tätigkeitsdialog und Tagesplan: Werkstube nachts, Mühle tagsüber,
Sonnenhain am Abend. Er bleibt nicht im Dunkelwald. Die acht spielbaren Figuren
bleiben dieselben. Weltkarte und sichtbare Bewohner teilen den vorhandenen
Aufenthaltssnapshot. Der Atlas enthält nun 38 Orte in 21 bedienbaren Gruppen;
Mühlenbach besitzt einen eigenen Marker und die passende Verbindung.

## Überdeckungen und Bodenkontakt

- Die Schattenbach-Maske traf zuvor teilweise das **hintere** Geländer und ließ
  vordere Pfosten aus. Neue Konturen folgen dem vorderen Handlauf, den beiden
  Querhölzern und vier Pfosten. Ihre Tiefe folgt der geneigten Brücke; Öffnungen
  zwischen den Hölzern bleiben frei. Der Kopf darf oberhalb des Handlaufs sichtbar
  bleiben, der Körper wird genau im Holzbereich verdeckt.
- Auch die gezeichneten vorderen Pfosten der beiden Küstenstege besitzen nun
  Ausschnittkonturen. Nachgerüstete räumliche Stegpfosten/Handläufe registrieren ebenfalls
  Konturen in derselben Tiefenmaske. Möbel- und Kulissenlogik werden weitergenutzt.
  Hierfür werden die Ränder vor der Maskenerstellung gebaut.
- Bodengebundene Sprite-Posen verwenden die tatsächliche Alpha-Unterkante.
  Besonders Mooslyn/Nocti hatten andere Kartenunterkanten beim Gehen, Rennen und
  Rollen. Sitzposen behalten ausdrücklich ihren vorher kalibrierten festen
  Sitzoffset, unabhängig von der vorausgehenden Bewegung. Bildrahmen und
  Lichtzentrum verwenden die tatsächlichen Zellabmessungen und Offsets.
- Kontaktschatten sitzen leicht über dem Boden, werden nach der gemalten Fläche
  gezeichnet und folgen Körpergröße statt Außenabmessungen der Spritekarte.
  Ein dichterer Kontaktkern und weicher Rand verhindern einen flächigen Fleck.
  Beim Springen/Schweben wird der Schatten größer und schwächer. Weltkollision
  bestimmt weiterhin den Schattenempfänger; Nachbarn können ihn nicht tragen.

Die vorhandene Beschränkung gilt weiter: keine begehbare Brücke zur Dämonenwelt.
Die Waldbrücke am Schattenbach ist ein anderer, bereits bestehender Ort.

## Grafikquellen und Prompts

Eigene integrierte Bildgenerierung, vorhandene Itoeva-Bilder als Stilreferenz.
Originale werden nicht übermalt. Laufzeitquellen:

- `prototypes/godot-hd2d/assets/expansion/mill-brook.png`
- `prototypes/godot-hd2d/assets/expansion/miro-packed.png`
- Zuschnitte: `prototypes/godot-hd2d/data/miro_atlas.json`
- Erste Charakterstudie: `prototypes/godot-hd2d/concept-art/miro-concept-v071.png`
  (durch vorhandenen Exportfilter vom Spielpaket ausgeschlossen).

Promptset: Mühlenbach als eigenes detailliertes Buntstift-/Maler-Panorama im
Sonnenhain-Stil, alte Stein-/Holzmühle links, Bach hinter durchgehend trockenem
Vordergrundweg, Birken und Wildblumen, keine Figuren oder Schrift. Miro als
brauner Biber mit flachem Schwanz, grüner Weste, cremefarbenen Ärmeln, rostrotem
Halstuch, Messing-Wasserradspange und Werkzeugtasche; sechs vollständige Posen,
gleiche Identität/Masse, echte Transparenz. Zweiter Durchgang vergrößert die
transparenten Zwischenräume; keine benachbarten Körperteile in den Zuschnitten.
Die generierten Originaldateien bleiben unverändert; die Runtime liest
Zuschnittrechtecke aus den Alpha-Grenzen.

## Prüfung und Übergabe

Neue Prüfung `landscape_depth_v071.gd`: **259 Checks, 0 Fehler**. Echte
Türaktionen in beide Richtungen, Miro auf begehbarem Boden, Bewegung und Dialog,
24-Stunden-Aufenthalte, acht Spezies mit verschiedenen Posentypen, feste
Sitzanker, Kontaktschatten und Sprungabschwächung, Brüstung/Hohlräume und
Registrierung der zusätzlichen Stegkonturen.

Echtes OpenGL-Rendering `depth_capture_v071.gd`: **2 Pixelvergleichstests,
0 Fehler**. Sichtbarer Kontaktschatten gegenüber abgeschaltetem Schatten;
vorderes Geländer gegenüber unmaskierter Figur und leerer Kulisse. Aufnahmen
von Mühlenbach/Miro, Sonnenhain, Nebelwacht, Schattenbach-Brücke, Küstensteg,
Dialog und Atlas visuell geprüft.

Regressionen: Bewohner/Welt674/0, Vollspiel84/0, Framing112/0, Türmatrix224/0,
Wald22027/0, Licht-/Wassermasken235/0 und bisherige Darstellungspolitur36/0. Die unabhängige zweite Codeprüfung fand
einen von der vorherigen Pose abhängigen Sitzoffset bei den kompakten Atlanten;
der feste Sitzoffset wurde wiederhergestellt und unabhängig nachgeprüft.
Die neue Grafik-/Tiefenprüfung testet diesen Übergang für alle Posen.
Bekannte ObjectDB-/Resource-Shutdown-Warnungen in vorhandenen Testansichten
bleiben von Spiel-/Parsefehlern getrennt. Telefon-GPU, FPS und Geräteabnahme
sind nicht durch das Desktop-Rendering belegt. Keine Spielstandmigration.
Keine Änderung an Signatur, Netzwerk, Releaseworkflow oder `main`.

Nächster Schritt für eine APK: AndroidTest-Metadaten und Runtime-Manifest dieses
konkreten Commits in den bestehenden freigegebenen Build-/Lieferweg übernehmen;
das alte 0.7.0-Manifest darf dafür nicht als Nachweis verwendet werden.
