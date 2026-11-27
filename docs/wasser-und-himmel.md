# Wasser und Himmel

Die Wasserwirkung bleibt auf die von Hand nachgezeichneten nassen Bildteile
beschränkt. Das gilt für Küste, Küstenpfad, Dschungelübergang, Sonnenhain,
Schattenbach, Mühlenbach, Erlenfurt, Ringwehr und alten Mühlenzulauf sowie die
Kampfarena an der Lichtung der drei Wurzeln. Der Steinweg der Erlenfurt und
die Wehrbrücken liegen zwischen getrennten Wasserkonturen. Wenn eine
verzerrte Abtastung ans trockene Ufer reichen würde, nutzt der Shader den
ursprünglichen Bildpunkt. Das Abschwächen der Küste über dem trockenen
Strandbild bleibt erhalten.

Ferne Flächen erhalten einen ruhigen Schimmer bei festen Bildkoordinaten.
Im Vordergrund kommen kurze Glitzerpunkte und gebrochene Spiegelungen
hinzu. Fließgewässer und Fälle nutzen stärkere vertikale Strömung.
Mühlenhebel und Ringwehr übernehmen weiterhin die Wassermenge.

Tag, Dämmerung und Nacht nutzen dieselbe Oberfläche mit verschiedenen
Lichtanteilen. Am Tag glitzern kurze Wellenkämme. In der Dämmerung wechseln
die Reflexe zu warmem Gold. Nachts überwiegt ein kühler Schimmer, während
Laternen und Fackeln kurze warme Reflexe unterhalb ihrer Position
werfen. Ein weicher zusätzlicher Lichthof bleibt bei der jeweiligen
Lichtquelle. Wolken ändern sanft die Helligkeit im Himmel. Wind bewegt nur
kleine, eigens markierte Blattspitzen. Er verändert weder Stämme noch
die Form der gesamten Krone.

## Daten und Kosten

`data/water_surfaces.json` enthält Uferhöhe, Reflexstärke, Tempo,
Strömungsrichtung, Tiefenbereich, Glitzermenge und Nachtintensität. Die
Konturen ergeben kleine RG8-Masken. In `data/environment_dynamics.json`
sind Fensterlicht, Himmelsbereiche und einzelne Blattspitzen markiert.
Die Effekte verwenden vorhandene Texturen im bestehenden Shader.

`water_surfaces_play.gd` vergleicht zehn Wasserkulissen in OpenGL bei
verschiedenen Zeitpunkten. Der Wasser-Ein/Aus-Vergleich kontrolliert
auch Stichproben über den gesamten trockenen Bereich. Weitere Vergleiche
prüfen Vorder- und Hintergrund, Nachtlicht, einen starren Baumstamm und
bewegte Blattspitzen. Die Sichtprüfung und Messung auf Android stehen aus.

## Android-Testbuilds

`tools/stage_android_preview.py <Projektkopie> <Buildnummer> <Version>`
erstellt ein eigenes Android-Paket im vorhandenen Hauptspielprofil.
Der Launcher zeigt für jeden Build eine eigene Zahl und Hintergrundfarbe.
Die getrennte Paket-ID lässt vorher installierte Fassungen und deren
Spielstände bestehen. Das Werkzeug schreibt ausschließlich in die
angegebene Projektkopie. Es benötigt Python mit Pillow.
