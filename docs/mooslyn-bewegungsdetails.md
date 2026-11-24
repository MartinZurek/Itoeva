# Mooslyns Bewegungsdetails

Die Rolle über die Aktionstaste hat 14 Zwischen- und Schlüsselbilder je Ansicht. Seitlich klappt Mooslyn das Geweih zum Körper, rollt über Umhang und Rücken und fängt sich auf den Hufen ab. Vorder- und Rückansicht zeigen dieselbe Aktion in der Tiefe. Links verwendet die gespiegelte Profilfolge. Die Rolle dauert weiterhin 0,46 Sekunden.

Die bestehenden Geh- und Galoppfolgen geben weiterhin das Schritttempo vor. Vorder- und Rückansicht erhalten Zwischenbilder, bei denen Geweih und Umhang nachfedern. Beim Sprung in die Tiefe unterscheiden sich Aufstieg und Abstieg leicht. Seitlich ergänzt eine kleine Oberkörperbewegung den vorhandenen Sprungbogen. Zwei kurze Übergangsbilder führen in die Sitzpose.

Der Zusatzatlas verwendet dieselbe Größe für Mooslyns Figur wie die vorhandenen M4-Streifen. Das gemeinsame Raster hält den unteren Kontakt der Posen fest. Physik, Eingaben und Spielstände verwenden den vorhandenen Codepfad. Die neue Rolle funktioniert auch beim Desktop-Start ohne `MOTION_M4=1`. Die übrigen Details ergänzen die M4-Animationen des Android-Exports und der M4-Desktop-Vorschau.

Pillow wird für die erneute Erzeugung des Atlas benötigt.

```sh
python3 tools/build_mosslyn_motion_details.py
```
