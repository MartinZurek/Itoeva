# Ausdauer und vollständige Gehzyklen – Godot 0.13.0

Martins Auftrag vom 20.10.2026: Sprint drosseln und begrenzen, sichtbare Ausdauer mit
schnellerer Erholung beim Sitzen und auf Bänken; abgeschnittene Körperteile und
rudimentäre Gehfolgen aller acht Figuren verbessern.

## Verhalten

Sprint3,3 statt4,6 Welteinheiten/Sekunde (rund28% langsamer), Gehen weiterhin2,0.
100 Ausdauerpunkte,16 pro voller Sprintsekunde: gut6 Sekunden Sprint. Verbrauch steigt
mit tatsächlich erreichtem Tempo über Gehtempo. Bei leerem Vorrat bleibt Gehen möglich;
erst ab30 Punkten wieder Sprint, damit ein gehaltener Stick nicht hektisch umschaltet.
Erholung pro Sekunde: Gehen7, Stehen12, Bodensitz25, echter Möbel-/Banksitz50.
Nach Anstrengung0,65 Sekunden Verzögerung; Sitzen hilft sofort. Rollen/Luftwenden kosten14.
Menü, Dialog und Kampf pausieren den Vorrat; Türen behalten denselben Vorrat.
Bewohner verbrauchen keine Spielerausdauer. Nur Sitzungszustand, keine Abwesenheitsstrafe,
keine Änderung der dauerhaften Fortschrittsdaten. Nocti landet bei Erschöpfung.

## Darstellung

Acht neue transparente Gehfolgen mit je24 Bildern: acht Phasen rechts/vorn/hinten,
links gespiegelt. Beide Schritte bilden einen Zyklus. Mooslyn behält den Laufbogen,
Nocti den Flug; übrige Wesen verwenden die neuen vollständigen Phasen auch beim Joggen.
Die bekannten Ruhe-, Sitz-, Sprung- und Kampfdesigns bleiben erhalten.

Überstehende Zeichnungen wurden durch starre181-/256-Pixel-Quads abgeschnitten.
Geometrie und UVs folgen nun vollständigen Alphakonturen statt dem Raster.
Technische R8-ID-Masken trennen ineinandergreifende Nachbarrechtecke. Kleine getrennte
Pfoten bleiben zugeordnet, zusätzliche ganze Figuren werden ausgeschlossen.
Fußanker, Spiegelung, Licht und Möbelverdeckung verwenden dieselben Konturen.

Originalgrafiken: `prototypes/godot-hd2d/assets/characters/{species}-walk.png`, für
mosslyn, nocti, starlet, wyrmling, fennec, gloop, puffling und hootlet.
Mit integrierter Bildgenerierung erstellt, vorhandene Figuren als Identitätsreferenzen.
Promptset: Identität, Farben und Kleidung erhalten; 8×3 transparente Rasterplätze;
ruhiger geerdeter voller Gehzyklus mit linkem/rechtem Schritt; rechts/vorn/hinten;
keine Richtungswechsel, Sprünge, Flugposen, Effekte oder Kulisse. Starlets untere Zacken
sind Beine, Wyrmling bleibt vierbeiniger Drache, Hootlet Eule mit Brille und Flügelarmen.
`prepare_character_polish.py` erzeugt ausschließlich Ausschnitt-/Maskenmetadaten;
Original-PNGs werden nicht durch das Werkzeug bearbeitet.

## Validierung und Wiederherstellung

Vor der Umgebungsunterbrechung bestanden362 Ausdauerprüfungen,1984 OpenGL-Kontur-/
Ankerprüfungen,1073 Figuren-/Helferchecks,88 Vollwelt-,128 Raum-/Spezies-,271 Tiefen-,
115 Bewegungs-/Licht-,690 Bewohner-,25 Hang- und36 Darstellungschecks. Echte Weltbilder
aller Wesen einschließlich leerem Balken und tatsächlicher Parkbankaktion wurden geprüft.
Nach dem lokalen Reset sind die acht PNGs anhand identischer Git-Blob-Hashes gesichert;
Code und reproduzierbare Metadaten werden erneut geprüft. Maßgeblich ist der finale Baulauf.
Synthetische Regression: Schweif über Rasterrand und getrennte Pfote bleiben enthalten,
eine zusätzliche große Figur wird ausgeschlossen. Einige Vollwelttests melden beim
Prozessende bekannte Katalogressourcen. Telefon-FPS/GPU und menschliche Animationsabnahme offen.

## Lieferung

Nächste Testfassung: AndroidTest0.13.0/Code25, bestehendes Paket, Testzertifikat und
Drive-Ziel aus dem APK-Auftrag vom19.10.2026. Noch keine erfolgreiche neue APK behauptet.
Bauweg prüft Runtime-Hashes, Regressionen, OpenGL, exportierte Gehfolgen/Masken,
Paket/Signatur und Remote-Prüfsummen. Liefernachweis nach erfolgreichem Upload ergänzen.
Rückbau: diesen Bewegungs-PR zurücknehmen. Vorherige Grafiken/Spielstandschlüssel bleiben
vorhanden. Branch ist auf dem ausgelieferten Figurenstand aufgebaut, kein Merge ins ältere main.

## Erneute Prüfung abgeschlossen

Nach Wiederherstellung erneut362 Ausdauerprüfungen,1073 Figurenchecks,88 Vollwelt-,
128 Framing- und25 Hangprüfungen bestanden. Erneut1984 Kontur-/Ankerprüfungen unter
echtem Mesa/OpenGL ohne Fehler. Neues Runtime-Manifest:525 Dateien,
SHA-256 `1ce46d30e8763e5a068825c7034416bbedbb3cf85362a0de92f587840c2fd005`.
YAML, alle eingebetteten Python-Skripte und Bash-Blöcke des APK-Lieferwegs validiert.
