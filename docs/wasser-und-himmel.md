# Wasser und Himmel

Küste, Küstenpfad, Dschungelübergang, Sonnenhain, Schattenbach, Mühlenbach,
Erlenfurt, Ringwehr und alter Mühlenzulauf nutzen die bewegte Wasseroberfläche.
Die Kampfarena an der Lichtung der drei Wurzeln nutzt denselben Wassereffekt.
Kräftigere Strömung an den Wehren und ruhigere Wellen in den Tümpeln stammen
aus denselben Shaderfunktionen. Das Ringwehr und der Mühlenhebel behalten
ihre bisherige Regelung der Wassermenge.

Der vorhandene Geländeshader verschiebt nur maskierte Wasserpixel. Für die
Spiegelung tastet er die gemalte Szenerie ein weiteres Mal oberhalb der
Uferlinie ab. Nachts übernimmt der Reflex die Tönung der Szene. Die Küste
blendet den Effekt über dem trockenen Strandbild aus. Die Erlenfurt hat
getrennte Konturen ober- und unterhalb des Steinwegs.

Über den Außenpanoramen ziehen maskierte Wolken. Baumkronen bewegen sich
lokal, und Licht wandert über Fensterflecken am Boden sowie über die
Kristalle in der Grotte. Die Bewegung bleibt an der jeweiligen Kulisse
verankert.

## Daten und Kosten

- `data/water_surfaces.json` enthält pro Kulisse Uferhöhe, Reflexstärke,
  Tempo und vertikalen Strömungsanteil. Neue Konturen werden aus normierten
  Bildkoordinaten in 240 × 135 Pixel große RG8-Masken umgewandelt.
- `data/environment_masks.json` liefert die weiterhin genutzten
  Wasser- und Wolkenmasken von Schattenbach, Sonnenhain, Ringwehr und
  Mühlenzulauf. Bestehende Wolkenfenster bleiben in Grün erhalten.
- Eine Wolken- und Laubmaske braucht 18 KiB. Kulissen ohne Himmel und Laub
  brauchen dafür keine Textur. Die Reflexion nutzt das bestehende Panorama
  im selben Renderdurchlauf. Es kommen keine Bilddateien, Spiegelkameras,
  Bildschirmkopien oder Plugins hinzu.

## Prüfung

`world_passages_play.gd` prüft die Erreichbarkeit aller 45 Orte im Atlas,
54 gerichtete Türen und Wege sowie die begehbaren Abschnitte dazwischen.
`water_surfaces_play.gd` prüft zehn Wasserkulissen und nutzt mit einem
OpenGL-Renderer zusätzlich echte Framevergleiche für Bewegung, Spiegelung
und trockene Stellen. Der Gerätetest für Android-Bildrate und Darstellung
steht aus.
