# Mooslyn: ruhigere Gliedmassen bei gleichem Weltfortschritt

Martin bestaetigt am24.10.2026 den ersten guten Eindruck von0.15.2, meldet aber
zu schnelle Bein-/Armbewegungen im Verhaeltnis zum sichtbaren Vorankommen.
Ausdruecklich soll die Fortbewegung nach links/rechts unveraendert bleiben.

## Aenderung

Nur die Abspielrate der28gemalten Seitenphasen wird reduziert. Der gemalte
Bogen verwendet jetzt25Prozent der bisherigen Phasenrate. Am vollen Gehtempo
werden dadurch etwa9,4 statt37,6 Quellbilder pro Sekunde erreicht; beim Sprint
etwa12,5 statt50,1. Der ganze28-Bilder-Bogen benoetigt rund2,98 bzw.2,23Sekunden.
Alle Einzelbilder bleiben enthalten; es werden keine Phasen herausgeschnitten.

Die Basisphase `gait` folgt unveraendert der tatsaechlich zurueckgelegten Strecke.
Es gibt keine zusaetzliche zeitgesteuerte Animation: langsameres Vorankommen
verlangsamt die Bilder, Wandkontakt und Stillstand lassen die Phase stehen.
Geh-/Sprintwechsel verwenden dieselbe kontinuierliche Basisphase.

`character_actor.gd` ist bytegleich mit0.15.2. Gehen2,35, Sprint3,85,
Analogsteuerung, Beschleunigung, Ausdauer und Kollisionen bleiben unveraendert.
Auch Groessenregistrierung und Bildanker aus0.15.2 bleiben erhalten. Die
Aenderung gilt nur fuer Mooslyns gemalte Seitenfolgen; andere Wesen und
Front-/Rueckansichten behalten ihre bisherige Darstellung.

## Pruefung

- Godot-Atlas-/Groessen-/Zustandspruefung1164Checks ohne Fehler, einschliesslich
  neuer Strecken-/FPS-Pruefungen bei30/60/120Hz und ohne neuen Wegfortschritt.
- Die aktive OpenGL-Gangpruefung beobachtet Mooslyn jetzt acht statt zwei
  Sekunden bei24Hz, damit der langsamere Bogen ausreichend abgedeckt wird.
  Sie begrenzt die neue Quellbildrate explizit auf9,3-12,7Bilder/Sekunde.
- Bestehende Ausdauerpruefung tastet die gemalten Seitenposen auf dem neuen
  Phasenmassstab ab; Geschwindigkeits-/Ausdauergrenzen werden nicht gelockert.
- Eine numerisch passende Phase ersetzt nicht Martins Telefonabnahme der
  Bewegung. Exakt weltfeste Sohlen und Telefon-GPU/FPS bleiben ungeprueft.

## APK und Ruecksetzen

AndroidTest0.15.3/Code33 verwendet Paket, Zertifikat und Drive-Ziel der bisherigen
Testfassungen. Neues549-Dateien-Manifest; alle Regressionen und Renderpruefungen
bleiben vor Export/Upload vorgeschaltet. Lieferung erst nach erfolgreichem
Build und Remote-Pruefsummenabgleich als abgeschlossen bestaetigen.
Isolierten Korrekturcommit zuruecknehmen; keine Save-Migration notwendig.

## Nachpruefung des ersten APK-Laufs

Lauf40036116987 brach vor APK-Export/Upload ab. Die character_polish-Pruefung
entnahm weiterhin acht Phasen aus dem alten, viermal kuerzeren Zeitfenster und
sah bei Mooslyns seitlichem Gehen deshalb mehrfach dasselbe Bild. Die204
Ausdauerchecks waren bereits erfolgreich; character_polish meldete zwei
Fehler bei869Checks. Test und zugehoerige Kontrollaufnahmen tasten jetzt den
vollen langsameren Bogen ab. Die Acht-Phasen-/Sprint-/Konturpruefungen bleiben
erhalten. Keine Laufzeit-, Manifest-, Geschwindigkeits- oder Ausdaueraenderung.
Erneuter vollstaendiger APK-Prueflauf erforderlich; Drive blieb bis dahin0.15.2.

## APK erfolgreich ausgeliefert

- AndroidTest0.15.3/Code33, Commit `970acb4af3d719647f8112d71981ca05195180dd`.
- Build40072212324 erfolgreich: https://github.com/MartinZurek/Itoeva/actions/runs/40072212324
- Charakterpruefung869/0, Mooslyn-OpenGL1276/0 und allgemeine OpenGL-Gangpruefung4020/0; saemtliche Spiel-/Render-, Export- und Signaturpruefungen bestanden.
- Vollstaendiger Drive-Upload und Remote-Groessen-/Pruefsummenabgleich erfolgreich; Dateiname und Groesse zusaetzlich per Connector rueckgelesen.
- Datei `Itoeva_Godot_Test_v0.15.3.apk`, 196659877Bytes.
- SHA-256: `d91d790d3448cc7714790c3956c920b1549ca7c464b530586faa9fb895e03972`.
- MD5: `721c15b39fd0fa859bc3ebf88aaeffdc`.
- Download: https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk
- Offen: Martins Taktabnahme am Telefon und Telefon-GPU/FPS. Kein Merge in main.
