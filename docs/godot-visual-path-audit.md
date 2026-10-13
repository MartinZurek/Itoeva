# Godot: Laufwege, Innenraumfiguren und Ueberdeckung

Stand: 2026-10-11. Zusatzkorrektur zur gemalten Gesamtwelt aus PR #359.
Die separate Godot-App verwendet die vorhandenen 2D-Zeichnungen weiter.

## Gefundene und korrigierte Fehler

| Befund | Korrektur |
|---|---|
| Vertikale Sprites wurden durch die Kamera um 35 Grad in der Hoehe gestaucht und wirkten breit. | Volles Billboard mit originalem Seitenverhaeltnis und Fussanker. |
| Innenraum-Massstab wurde um 109/83 und die Kameraprojektion doppelt vergroessert. | Sichtbare 109-Pixel-Referenz direkt auf die native Raumhoehe abbilden. |
| Hootlet wirkte im gemeinsamen Massstab zu gross. | Einheitliche relative Hoehe 0.70 statt 0.86; keine horizontale Streckung oder neue Zeichnung. |
| Kopierte Moebel-Vordergrundkarten uebermalten Boden und Figuren bei Schwenks. | Kulisse einmal zeichnen; Originalkonturen steuern nur die Verdeckung im Figuren-Shader. |
| Transparente Anschlussbilder verdeckten den Avatar vollstaendig. | Figuren nach den Anschlusskarten zeichnen, ausgeschnitten nur durch die Tiefenmaske. |
| Ein generischer Laufstreifen fuehrte ins Wasser, durch Pflanzen und ueber die Brueckenwand. | Eigene, an den Bildern gepruefte Profile fuer sieben Panoramen und sechs Anschluesse, Brueckenweg hinter der Bruestung. |
| Raum-Seitengrenzen wurden nicht verwendet; Schrankkoerper wurden auf den Laufstreifen verschoben. | Trapezfoermige Seitenwaende, lokale Raumrueckkanten und unverklemmte Moebelanker. |
| Manche Tuer-/Altstandpositionen landeten nach Kollisionskorrektur weiterhin im Moebel. | Radiuspruefung und naechste freie Bodenposition innerhalb der Raumkontur. |
| Generische Aussenbaenke hatten kein passendes Bildmotiv. | Nur sechs im Bild vorhandene Sitzplaetze plus die wirklich hinzugefuegte Parkkiste verwenden. |

Die Bildkonturen liegen separat in `prototypes/godot-hd2d/data/visual_geometry.json`.
Der aus Kotlin erzeugte Katalog und die Originalbilder bleiben unveraendert.
Die gezeichneten Raumseiten erlauben keine freie Kameradrehung; Innenraumzoom
bleibt moeglich, der Schwenk ist auf die Originalblickachse begrenzt.

## Nachweise

- Assetabgleich: 25 Originaldateien byte-identisch, sechs mal 170 Frames pixel-identisch.
- Laufzeittest: 76 Checks fuer Menue, Touch, Bewegungsrollen, Sitzanker und Tueren.
- Geometrie: 2048 Checks, 411 Wegpunkte mit wirklicher Bewegung in jeder Richtung,
  98 negative Randpruefungen sowie 1848 sichere Raumspawns fuer elf Raeume und sechs Wesen.
- Bildrahmen: 66 Raum/Wesen-Kombinationen, inklusive Lauf-, Roll- und Sitzframes.
- Tueren: 132 gerichtete Spezies/Tuerpfade.
- Renderpruefung: 92 Bildpaare mit/ohne Figur. Alle elf Raeume mit sechs Wesen,
  alle 17 Aussenorte, sechs Bildanschluesse und drei Brueckenpositionen.
  Kein gepruefter Avatar wurde vollstaendig uebermalt.

Die Renderbilder entstehen in Godot 4.5.1 mit Desktop-Software-OpenGL,
nicht auf einem Android-Telefon. Ausgewaehlte Beispiele liegen unter
`prototypes/godot-hd2d/preview/visual-*.jpg`. Die vollstaendigen Captures werden
vom Test nach `captures/visual` geschrieben und nicht in APK/Git aufgenommen.

## Charakterwerkzeug und Grenzen

Krita eignet sich fuer gezeichnete Einzelbilder, Referenzebenen und Animation:
[Offizielles Animationshandbuch](https://docs.krita.org/en/user_manual/animation.html).
Krita ist in dieser Arbeitsumgebung nicht installiert. Fuer diese Korrektur war
kein Neuzeichnen erforderlich: Projektion, Raumgroesse und Tiefenverdeckung waren
die belegten Fehlerquellen. Originalatlas und Bewegungszeichnungen bleiben erhalten.

Vermessene Polygonkonturen sind eine Naeherung. Kleine Blaetter und einzelne
Vordergrundpflanzen besitzen noch keine vollstaendige Tiefenmaske. Niedrig aufgeloeste
Innenbilder werden durch lineare Filterung weicher, erhalten aber keine erfundenen
Details. Telefon-GPU, FPS, Temperatur und die Android-Touchabnahme sind noch offen.

## Build und Ruecknahme

APK v0.2.1 / Code 4, Paket `com.itoeva.godot.hd2d.world`, gleicher Debug-Schluessel
wie v0.2.0. Es gibt keine native Datenmigration oder neue Spielstandversion.
78.128.130 Bytes; SHA-256:
`72300f1d4f5e246b283f45ce5f2e96bca0a9a4d03c66bacfd53a3948379fd8e0`.
ZIP, exportierte aktuelle Geometrie und Shader, Ausschluss von Tests/Renderbildern,
ARM64, API 24/35, Querformat, fehlende Internetberechtigung, 16-KiB-ZIP-Ausrichtung
und APK-Signaturen v2/v3 sind geprueft.
Eine Ruecknahme dieses Korrekturcommits stellt v0.2.0 dar, ohne gespeicherte
Wesen-/Raum-IDs zu veraendern. Der native Android-Client bleibt eigenstaendig.
