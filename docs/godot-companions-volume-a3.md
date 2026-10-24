# Weitere Spieler als Originalvolumen – A3, 24.10.2026

Basis: Mooslyn A2 / APK 0.15.4, Commit 952a677, PR #382.
Branch: `codex/companions-original-volume`.

## Ergebnis

Nocti, Starlit und Wyrmling verwenden jetzt bei aktivierter Option geschlossene,
riggte 3D-Volumen mit Originalfarben. Jede Figur hat Seiten-/Vorder-/Rueckansicht,
Nocti zusaetzlich drei Flugansichten. Starlits Koerper hat keine Beine: beide unteren
Strahlen bleiben Teil desselben Sternkoerpers, mit Abfedern und Schwebe-Huepfen.
Die bestehende Schwebekurve wird weiterverwendet statt eines zweiten Physikpfads.

Einstellungen: **Charaktere 3D: an**. Der bestehende Schluessel
`settings.mosslyn_volume` bleibt erhalten: bereits aktivierte Spielstaende bleiben
aktiv; sonst weiterhin false. Mooslyn selbst bleibt der A2-Seitenprototyp.
Keine Aenderungen an Tempo, Ausdauer, Kollision, Kartenspiel oder Fortschritt.
Abschalten stellt die bestehenden Zeichnungen wieder her; isolierter Revert
entfernt A3, ohne neue Spielstanddaten zuruecknehmen zu muessen.

## Geprueft

- Generator: geschlossene Kanten, endliche Geometrie, normalisierte Gewichte.
- Godot 4.5.1: Import erfolgreich, 5658 neue Headless-/5706 OpenGL-Pruefungen,
  jeweils 0 Fehler. Alle Blickrichtungen und 32 Phasen, Stern ohne Beingelenke,
  Flugmodell, Bewegungsstopp, Aktionsrueckfall und abschaltbare Darstellung.
- Echter Welteinbau: 22/0; Mooslyn: 334/0 und Welt 13/0.
- Bestehende Charakterpruefungen 869/0, Ausdauer 204/0, Spiel-Smoke 86/0.
- OpenGL-Bilder der drei Seitenzyklen visuell gesichtet: Originalgesichter/-farben
  erhalten, Starlit als geschlossener Stern mit Hoehenbogen. Pfotenverformungen
  sind weiterhin eine Grenze, keine abschliessende Qualitaetsabnahme.
- Bekannte Resource-/ObjectDB-Restobjekte beim Beenden der Welttests bestehen
  wie in A2; keine Skript-/Assertionsfehler in den neuen Laeufen.

## Noch offen

Android-Geraeteleistung und Nutzerabnahme. Freie Kamerawinkel, anatomisch
getrennte verdeckte Gliedmassen und weltweit verriegelte Standpfoten fehlen.
Aktionsknopf-Sprung, Rollen und Sitzen bleiben Originalzeichnungen.
Die APK-Lieferung wird erst nach bestaetigtem CI-Build als fertig gemeldet.

Naechster Schritt: APK auf dem Telefon vergleichen, danach insbesondere
Wyrmlings Pfoten-/Hueftuebergaenge verfeinern; kein erneutes Design der Koepfe.

## Lieferstatus am Ende dieser Sitzung

Lokaler Implementierungscommit: `facc0d7ef96562107b179bed5840ef776b534a23`.
Der Push nach `https://github.com/MartinZurek/Itoeva.git` wurde von der
automatischen Freigabepruefung blockiert: Repository nicht als vertrauenswuerdig
verifiziert, Implementierungsauftrag nicht als Veroeffentlichungsfreigabe gewertet.
Nicht ueber andere Uploadwege umgehen. Kein neuer PR, kein CI-Lauf und keine
APK 0.15.5. Naechster Schritt ist die explizite Freigabe fuer den Push dieses
Branches samt Entwurfs-PR und den bestehenden APK-/Drive-Lieferweg.

Auch der echte OpenGL-Welttest ist abgeschlossen: 22 Checks, 0 Fehler, Bilder
aller drei Spieler unter `captures/companions-volume/world-*.png`.

## Freigabe und Upload

Martin hat Push, Entwurfs-PR und APK-/Drive-Lieferung ausdruecklich freigegeben.
Der direkte Git-Push scheitert an fehlender Anmeldung; der gepruefte Baum wird
ueber die verbundene GitHub-Anbindung hochgeladen. Die fruehere Freigabesperre
ist damit historisch; Build-/Lieferstatus weiterhin erst nach CI bestaetigen.
