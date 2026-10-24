# Mooslyn Originalvolumen: A2-Seitenrig

`python3 tools/mosslyn-3d/build.py` erzeugt das GLB und `rig.json` deterministisch
mit numpy, scipy und Pillow. Quelle ist das A1-Checkpointvolumen vom 24.10.2026,
keine frei modellierte Ersatzfigur. `source/original.png` ist die erste Zelle von
`mosslyn.png` aus Commit `3bc8ad7fb9c22531546d3bef1a431e19ea01dad8`.
Die Quelldatei und Volumendaten wurden aus `Mooslin_3D_Prototyp_Checkpoint_A1.zip`
wiederhergestellt. SHA-256 steht im generierten `rig.json`.

Die Projektionsfarben werden nur ausserhalb der Originalfreistellung aufgefuellt;
rote Freistellungsreste werden wie in A1 ausgeschlossen. Keine neuen Fell-, Kopf-
oder Kleidungszeichnungen. Das Volumen wird auf 14120 Vertices / 28584 Dreiecke
verdichtet (A1: 63672 / 127970). Zwölf echte glTF-Gelenke und normalisierte Gewichte;
keine gerenderten Einzelbilder als Ersatz fuer das Volumen.

Die Skelettbewegung kommt zur Laufzeit aus `mosslyn_volume.gd`. Inverse Kinematik
verwendet feste Segmentlaengen, die Kopf-/Geweihgeometrie bleibt starr. Das
verkleinerte Volumen ist weiterhin nur fuer die feste Seitenkamera geeignet.
Verdeckte Gliedmassen und die verschnittenen Hueftbereiche sind nicht neu modelliert.

## Wichtige Grenze

A2 ist ein sichtbarer, zuschaltbarer Integrationsprototyp, keine fertige
Animationsabnahme. Die Geometrie am Bauch/Beinansatz kann sich noch verziehen.
Die IK-Zielpunkte sind geometrisch geprueft, aber **nicht weltweit verriegelt**:
Die Schrittweite aus dem kurzen Originalbein passt noch nicht physikalisch zum
bestehenden Weltfortschritt. Deshalb ist Bodengleiten moeglich. Den Weltfortschritt
nicht still verlangsamen und das Problem nicht als geloest ausgeben.

Als Naechstes Hueft-/Beingeometrie anatomisch trennen, verdeckte Flaechen sauber
schliessen und dann Standphase/Schrittlänge gegen die echte Wegstrecke abstimmen.
Das ist Voraussetzung fuer Vierfuesser-Sprint und eine spaetere Standardaktivierung.
