# Itoeva Godot v0.2.5: Schwung und Begegnungen

Martins Ziel ist eine Welt, in der das Bewegen und Entdecken selbst Freude macht.
Die gemalten Figuren bleiben erhalten. Seine konkrete Rueckmeldung: Aufwaertswischen
springt nicht, Sitzen und Interaktion sind unklar, Ansichten wechseln die Groesse,
Kulissen wirken riesig, Rennen sieht wie schnelleres Gehen aus und Hootlet rollt
unpassend im Flug. Er hat zusaetzlich kreative Konzeptarbeit und Perspektiven durch
weitere Agenten ausdruecklich gewuenscht.

## Gestalterische Richtung

Eine reagierende Bilderbuchwelt: Gehen, Sprinten, ein kurzer Schwung, ein Sprung,
eine Begegnung und eine Pause bilden einen zusammenhaengenden Ablauf. Die Kamera
zeigt etwas vom Weg voraus, Nachbarn wenden sich zu, erreichbare Dinge benennen
ihre Aktion, und die Karte merkt besuchte Orte. Die Bewegung nutzt die vorhandenen
Zeichnungen, den Bodenkontakt und ein direktes Eingabegefuehl.

Die beiden zusaetzlichen Design-/Reviewperspektiven wurden ausgewertet. Priorisiert
wurden ein kurzer lenkbarer Schwung mit Folgesprung, eine vorausschauende Kamera
mit festem Zoom, passende Bewegungen je Wesen und konkrete Entdeckungsreaktionen.
Ein umfangreiches neues Quest-/Besitzsystem oder neue Figurenzeichnung wird durch
diese Testversion nicht vorgetaeuscht. Der einzelne Kieselfund ist ein erster
spielbarer Entdeckungsmoment, nicht ein fertig ausgebautes Sammelsystem.

## Bedienung

| Geste am rechten Kreis | Wirkung |
|---|---|
| Nach oben wischen | Springen, auch bei gehaltenem linken Gehdaumen |
| Seitlich wischen | Kurzer gerichteter Schwung; Hootlet macht eine Luftwende |
| Nach unten wischen | Sitzen bzw. Aufstehen |
| 500 ms halten | Sitzen bzw. Aufstehen; genau einmal je Beruehrung |
| Kurz tippen | Die im Kreis benannte erreichbare Aktion; sonst Sprung |
| Figur, Sprechblase oder Weg-/Tuerzeichen antippen | Das genau gezeigte Ziel ansprechen bzw. betreten |

Sitzen stoppt den Gehdaumen. In der Luft wird es bis zur Landung vorgemerkt, ohne
die vertikale Flugbahn abzubrechen. Ein neuer Sprung ersetzt die Sitzabsicht;
Menue/Pause verwerfen sie. Touch-Cancel erzeugt keine neue Aktion. Das beim
Finger-Down gezeigte Kontextziel bleibt bis zum Loslassen festgehalten; ist es dann
nicht mehr erreichbar, gibt es eine Rueckmeldung statt einer anderen Aktion.

## Bewegung, Darstellung und Entdeckung

- Beschleunigung 22, Bremsen 30 statt 9/14. Analoge Zwischenstufen bleiben erhalten.
- Fennec nutzt weiterhin die originale Vierbeinerfolge 114-121 bzw. die vier
  Front-/Rueckenbilder. Ein 160-ms-Uebergang mit vorhandener gebueckter Pose
  verbindet Gehen und Rennen. Der Schrittzyklus folgt der wirklich zurueckgelegten
  Strecke und ist beim Rennen laenger; Anrennen gegen Hindernisse erzeugt keine
  schnelle Gleitpose.
- Der Schwung dauert 0,46 s statt 0,62 s, Tempo 6,2 bis 3,5, und ist begrenzt
  lenkbar. In den letzten 160 ms kann der Folgesprung unmittelbar einsetzen.
- Hootlet nutzt Fluegelzeichnungen und eine dezente Schwebelage beim schnellen
  Fortbewegen. Seine 0,64-s-Luftwende zeigt Seiten-/Front-/Rueckenansichten und
  erhaelt eine bereits laufende Flugbahn. Die eingerollten Bilder 130-137 werden
  dafuer nie verwendet. Auch die Luftwende vom Sitz wird korrekt verlassen.
- Kamera blickt horizontal hoechstens 0,95 Einheiten voraus, innen 0,20. Kein
  Geschwindigkeitszoom und kein verzerrender Kulissenschwenk. Bei Ortswechsel reset.
- Aussen Standardzoom 8,8 statt 7,6; Kulisse rund 14 Prozent kleiner im Bild.
  Innen 11 statt 10; Kulisse rund 9 Prozent kleiner, bewusst schmaler Bildrahmen.
  Grundzeichnung 0,0145 statt 0,0115 aussen; Fennecs Ruhehoehe bei 1280x720 rund
  129 statt 119 Pixel, innen/aussen identisch. Hootlet bleibt relativ 0,62 gross.
- Ruheansichten sind auf die tatsaechliche opake Referenzhoehe abgestimmt. Gehfolgen
  erhalten einen festen Faktor pro Richtung, Rennfolgen eine massenbasierte
  Korrektur. Innerhalb einer Folge wird kein Gesicht pro Bild neu skaliert.
  Die Vierbeinerhaltung bleibt bewusst niedriger als aufrechtes Stehen.
- Zeichnung und physischer Fussabdruck sind getrennt. Die Capsule bleibt auf den
  bisherigen trockenen Stegen passend; groessere Zeichnungen verbreitern keine
  Laufbaender. Innen werden ganze Karten an den Bildraendern beruecksichtigt.
- Tueren tragen dezente Pfeile und Ortsnamen auch ausserhalb der Aktionsreichweite.
  Blasen verschwinden nicht mehr hinter dem Aktionskreis. Entferntes Antippen
  gibt einen verstaendlichen Naeherungshinweis.
- Freie Nachbarn wenden sich bei einer langsamen Annaeherung zu. Ansprechen stoppt
  gehaltene Laufgesten; der vorhandene Leitsatz bleibt die Gespraechsgrundlage.
  Hindernispruefung gilt auch fuer Sitze und den Fund, damit man nicht durch
  andere Moebel hindurch interagiert.
- Ein kleiner gemalter Kieselfund auf dem Parkweg laesst sich einmal aufheben.
  Sein vorhandenes Steinasset wird unveraendert verkleinert dargestellt.
  Besuchte Orte und Fundzahl erscheinen auf der Karte. Dies sind optionale
  lokale Felder `discovered`/`collected` im bisherigen Godot-Spielstand;
  alte Daten ohne diese Felder funktionieren mit leeren Listen.
- Die 34 Rasterassets dieses Godot-Projekts bleiben byteidentisch, darunter
  alle 6x170 Figurenzeichnungen. Keine neue Farbpalette, keine 3D-Figur.
  Metriken werden je Art einmal ermittelt und waehrend der Sitzung wiederverwendet.

## Pruefung und Grenzen

Pruefstand: Godot 4.5.1, Desktop-Software-OpenGL fuer Bilder/Video und deterministische
60-Hz-Physik fuer die automatisierten Funktionspruefungen. Die aktualisierte
Raumrandpruefung mountet jetzt wirklich jede Art mit ihrem richtigen Atlas.

Die neue `movement_flow.gd` prueft 324 Bedingungen: echte Touchgesten mit beiden
Daumen und schraegen Wischrichtungen, Sitzvormerkung, Menueabbruch, Folgesprung,
Kamera/Stoppen, sichtbare Hoehen, feste Folgenfaktoren, unveraenderten Fussabdruck,
originalen Vierbeinerlauf, Hootlets Flug-/Sitzwende und einen tatsaechlichen,
persistenten Kontextfund inklusive Zielverlust waehrend des Haltens.

Weitere Regressionen: 694 Figuren-/Lesbarkeitschecks, 117 Gesten-/Wegechecks,
76 Laufzeitchecks, 132 Tuerpfade, 66 Raum/Art-Kombinationen, 291 Randchecks,
59 unabhaengige Uferchecks sowie 2048 Geometriechecks mit je 411/411 physisch
angelaufenen Wegpunkten in beiden Richtungen. Einzelprotokolle im Quellpaket.
17 Spielansichten wurden gerendert und relevante Innenraum-/Park-/Stegansichten
visuell geprueft. Die kurze Bewegungsaufnahme zeigt echte Desktop-Laufzeit,
keine APK-Aufnahme und keine Leistungsprognose fuer das Telefon.

Die vorhandenen Headless-Harnesses melden beim abrupten Beenden weiterhin
ObjectDB-Restobjekte; full_smoke meldet zusaetzlich zwei Ressourcen beim Shutdown.
Das trat bereits im v0.2.4-Harness auf. Funktionale Assertions werden gesondert
bewertet; ein bestandener Desktoptest ist keine Telefonabnahme.

Telefoninstallation, Touchgefuehl, GPU/FPS und die subjektive Freude beim Spielen
bleiben offen. Die gerichteten gemalten Reliefkulissen, naehrungsweise
Vordergrundmasken und zum Teil nur 480x270 grossen Innenbilder bleiben Grenzen.
Mehr gerichtet gemalte Uebergangs-/Fluegelbilder koennen spaeter zusaetzlichen
Gewinn bringen; diese Version behauptet kein neu animiertes Profisprite-Set.

## Auslieferung

AndroidTest: v0.2.5 / Code 8, `com.itoeva.godot.hd2d.worldtest`, Anzeigename
Itoeva Godot Test. Derselbe freigegebene Testschluessel wie v0.2.4, also als Update
installierbar. Das alte Android-Preset bleibt unveraendert. APK im bisherigen
Drive-Ordner Notime; vollstaendiges Godot-Quellpaket und Abnahme separat gesichert.
Keine privaten Schluessel in APK-Quellpaket oder Git. Kein GitHub-Push.

Naechste Abnahme: Parkweg, Nachbar, Fund, Bank, Steg und Innenraum auf Martins
Telefon zusammen spielen. Dabei besonders Wischrichtungen, Blickwechsel, Pause,
Bildmassstab und Sprintgefuehl beurteilen. Revert des isolierten v0.2.5-Commits
setzt die Godot-Fassung auf v0.2.4 zurueck; native Daten bleiben unberuehrt.

### Verifizierte APK-Auslieferung

- Code-Buildcommit: `f19a9326b9bfaa5a300b1101a57fad92d9286a1c`.
- Datei: `Itoeva_Godot_HD2D_v0.2.5_Test.apk`, 78.767.398 Bytes.
- SHA-256: `7a1e7a06a09bb9d1e779c88978aab474341d83a3d727098f5a566ecf092b1359`.
- Zertifikat-SHA-256: `fe124bbf6e3682280f62d0ba7e0619c0fc3c266adfa74b6e99c77d4ed6318582`,
  identisch zur freigegebenen v0.2.4-Test-APK.
- Android-Signatur v2/v3 und ZIP-Alignment verifiziert; ARM64, API 24/35,
  Querformat, keine Internetberechtigung. Gepackter Katalog und Weggeometrie
  bytegleich; Tests, Previewbilder und Capture-Dateien ausgeschlossen.
- 3.807 funktionale Bedingungen ohne Assertionfehler; bekannte Shutdownmeldungen
  des Desktop-Harnesses gesondert dokumentiert.
- Drive-Datei-ID: `1KIIZ08M4WIoxhltsKur13wJOlmBoNVag`, im Ordner Notime
  `1_8rk3o_iSfuK0kniFqGq5CO0Vv0YAM7c`. Upload erfolgreich; Metadatenabgleich
  bestaetigt Name, Zielordner und Dateigroesse. Kein Remote-Hash behauptet.
- APK: https://drive.google.com/file/d/1KIIZ08M4WIoxhltsKur13wJOlmBoNVag/view
