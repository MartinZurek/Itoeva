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
