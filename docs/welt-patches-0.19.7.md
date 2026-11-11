# Weltpatches · Kartenschmiede 0.19.7 (Build 69)

Die beiden separat begonnenen Patches vom 8. November wurden auf den gepushten
Quellstand 0.19.6, Build 68 (`45102c11e5dd8593d044fc2a93b57cdb787cf6d8`)
übertragen. Das Godot-Projekt liegt hier direkt im Hauptverzeichnis.
Die Kampfskripte, Einspieler, Figurenbilder und die M4-Aktivierung bleiben auf
dieser Basis. Android-Paketname und Spielstandstruktur bleiben erhalten.

## Staub und Fußabdrücke

- Langsames Gehen löst auf Erde und Sand bereits ab 0,55 Welteinheiten/s Staub aus.
- Acht der bestehenden zwölf MultiMesh-Plätze gehören den Abdrücken, vier dem
  Staub. Staub kann die Sandspur dadurch nicht mehr überschreiben.
- Huf- und Krallenformen sind größer, deutlicher und lokal zur Laufrichtung
  gedreht. Staub reagiert auf Untergrund und Tempo und vergeht nach 0,62 Sekunden.
- Holz, Stein, Flug und Luftphasen bleiben ohne diese Bodenspuren.
- Keine zusätzlichen Texturen oder Partikelsysteme. Laufzeitänderungen liegen in
  `scripts/ground_steps.gd` und `shaders/mud_steps.gdshader`.

Die ursprüngliche Ursachenanalyse steht in
[ITOEVA_BODENKONTAKT_20261108.md](ITOEVA_BODENKONTAKT_20261108.md).

## Mühlenchronik

Am Mühlenbach bewegt der erreichbare Uferhebel zuerst das Wasser in die
Nebenrinne und beim nächsten Zug zum Rad. Ein kurzer Bedienabstand schützt
vor Doppeltipps. Der aktive Wasserlauf wird sichtbar heller, die Speichen
nehmen Schwung auf und der vorhandene Wasserklang bestätigt die Stellung.
Anschließend lässt sich die Chronik lesen und die Karte `brook_oath` samt acht
Wegmünzen einmalig bergen. Die frühere Multiple-Choice-Frage entfällt.

Der vorhandene Fundnachweis `find:muehlenchronik` bleibt maßgeblich. Ein alter
abgeschlossener Fund zeigt bei Rückkehr das laufende Rad, ohne erneut zu
belohnen. Unfertige Versuche beginnen beim erneuten Betreten neutral.
Die zusätzliche Animation nutzt ein lokales Mühlenmaterial und vorhandene
Bild- und Audiodateien. Außerhalb der Mühle wird der Controller abgebaut.

## Prüfung dieses Quellstands am 11. November 2026

Mit Godot 4.6.3 im Headless-Modus geprüft:

| Prüfung | Ergebnis |
| --- | --- |
| `tests/ground_contact_play.gd` | 21 Prüfungen, 0 Fehler |
| `tests/mill_chronicle_play.gd` | 23 Prüfungen, 0 Fehler |
| `tests/practice_duels.gd` | 0 Fehler |
| `tests/practice_cinematic_ui.gd` | Zieltreffer, Beschwörung, voller Platz und Überspringen bestanden |
| Neue Shader | Godot liest die Shader und ihre Uniforms fehlerfrei ein |

Die Spieltests bewegen die normalen Spielfiguren in der Welt. Der Mühlentest
prüft Touchziel, beide Hebelstellungen, Entfernung, Stummschaltung,
Profilwechsel, Speichern, Rückkehr und einmalige Belohnung. Bestehende
ObjectDB-/Ressourcenhinweise beim Beenden bleiben bestehen.

Eine neue OpenGL-Bildprüfung war in dieser Umgebung ohne verfügbare
Grafikanzeige nicht möglich. Die Bildbelege der ursprünglichen Patches stammen
vom 8. November und gelten nicht als neue Abnahme dieses Quellstands.
Eine neue APK wurde in diesem Durchgang nicht erstellt. Auf Android sind
Darstellung, Lautstärke, Bildrate und der Update-Export mit dem bisherigen
Signaturschlüssel noch zu prüfen.

Vom Projektverzeichnis aus:

```sh
godot --headless --fixed-fps 60 --script res://tests/ground_contact_play.gd
godot --headless --fixed-fps 60 --script res://tests/mill_chronicle_play.gd
godot --headless --script res://tests/practice_duels.gd
godot --headless --fixed-fps 60 --script res://tests/practice_cinematic_ui.gd
```

Für Bildvergleiche lassen sich die ersten beiden Tests mit einem normalen
OpenGL-Display und einem Ausgabeordner hinter `--` starten.
