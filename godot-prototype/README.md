# Itoeva Godot-Prototyp

Probe, ob sich die Räume von Itoeva 2 in **Godot 4.4** als echte 3D-Welt besser bauen lassen.
Getrennt von der App: eigener Ordner, eigene APK (`com.notime.glyphminderwatch.godotproto`),
nichts in `:app` oder `:app-sim` ist verändert.

## Was drin ist

- **Wohnzimmer aus Grundformen** (`room.gd`): Dielen, Tapete mit Holzvertäfelung, Teppich, Sofa,
  Couchtisch, Bücherregal, Sessel, Stehlampe (echtes Licht mit Schatten), Pflanzen, Bilder,
  Fenster. Die Texturen entstehen beim Start als kleine Pixelbilder (64 px je Meter).
- **Licht**: Nachmittagssonne fällt nur durchs Fenster (unsichtbare Decke hält sie ab),
  Staub im Lichtkegel, warmes Umgebungslicht, Glühen.
- **Fennec als Billboard** (`fennec.gd`): der vorhandene 138-Bilder-Bogen aus
  `app-sim/src/main/assets/creatures/fennec.png`, umgepackt in ein 12×12-Raster
  (`tools/pack_sheet.py`, weil Godot keine Texturen über 16384 px lädt). Die Ansicht
  (Seite/vorn/hinten) richtet sich nach der Laufrichtung *relativ zur Kamera*; Gang- und
  Rennbilder laufen nach zurückgelegtem Weg, damit die Füße nicht rutschen.
- **Kamera** (`main.gd`): folgt weich, dreht in 45°-Schritten (Knöpfe) oder mit zwei Fingern,
  zoomt per Spreizen. Wände zwischen Kamera und Raum werden ausgeblendet (Puppenhaus-Schnitt),
  werfen aber weiter Schatten.
- **Pixel-Look**: Die 3D-Welt rendert in halber Auflösung (~640×360) und wird pixelgenau
  hochskaliert. Knopf „Pixel“ schaltet zum Vergleich auf volle Auflösung.
- **FPS-Anzeige** oben, damit die Leistung auf dem Telefon sichtbar ist.

## Steuerung

Tippen/Halten = hingehen (folgt dem Finger) · Doppeltipp = rennen · Fennec antippen =
Freudensprung · zwei Finger = drehen/zoomen.

## Bauen

- APK: `.github/workflows/godot-apk.yml` läuft bei jedem Push auf `godot-prototype/**` und legt
  die APK nach Drive (`Itoeva-Godot-Prototyp.apk`) sowie als Artefakt ab.
- Lokal (Linux, ohne Handy): `godot --headless --path godot-prototype --import`, dann
  `xvfb-run godot --path godot-prototype --rendering-driver opengl3 -- --probe` schreibt
  Probebilder nach `user://probe_*.png`.
- Neuer Bogen eines Wesens: `python3 godot-prototype/tools/pack_sheet.py <wesen>`.
