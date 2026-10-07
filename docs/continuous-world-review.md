# Reviewvorlage: Durchgehende Landschaft, folgende Kamera und neue Karte

Strasse, Park, Wiese und Wald haben bisher eigene Kulissen mit Schwarzblenden und einem
Bewegungsneustart am Rand. Dieser Schnitt ersetzt sie durch ein gemeinsames gemaltes Panorama.
Wer von der Strasse durch den Park in die Wiese laeuft, bleibt im selben Kameraschwenk;
Fusspunkt, Figurgroesse, Geschwindigkeit und Gangphase bleiben an den inneren Raendern erhalten.

Die Kamera folgt weich mit Blickvorlauf. Bei schnellem Laufen zeigt sie mehr Umgebung,
an nahen Aktionsplaetzen oder Entdeckungen zoomt sie sanft heran. Alle Trefferflaechen,
Spieler, Bewohner und Requisiten verwenden dieselbe Projektion. Die neue Landschaftskarte
zeichnet die echten Ausgaenge, Standort und gewaehlten Weg. Alle bisherigen Orte bleiben
erreichbar; Gebaeude und weitere Gebiete behalten ihre Uebergaenge.

## Umfang und Basis

- Branch: `codex/continuous-world-camera`; Basis ist der aktuelle lokale Charaktercommit
  `4fad98d` auf `codex/ensemble-design-polish`.
- Geplanter PR gegen `codex/ensemble-design-polish`, damit die Charakteraenderungen separat
  reviewbar bleiben. Falls der Charakterstand zuerst gemergt wird, anschliessend auf `main`
  umstellen. Es wurde nichts gemergt.
- Betroffen: Game-Weltgeometrie, Kamera, Renderer/Touch-Projektion, Weltkarte, Sicherheitsanker,
  Offline-Testharness und Dokumentation. Neues Panorama und reproduzierbare Kotlin-Vorschau.
- Orts-IDs und Spielstandformat bleiben erhalten; keine Preference-/Room-Migration.

## Validierung

- **937 Kotlin-Tests gruen**, darunter Ortsnaehte, Momentum/Gangphase, Route zu allen Orten und
  zurueck, Speicherwiederherstellung, sichere Funde, Bewohner-Deduplikation, inverse
  Touch-Projektion, Zoom-Hysterese, Nachlauf und Bildabdeckung nach Formatwechsel.
- `git diff --check` sauber.
- 20-Sekunden-Desktop-Vorschau aus den echten `GameWorld`-/`GameMovement`-/`GameCamera`-Klassen,
  dem neuen Panorama und dem aktuellen Fennec: `tools/world-art/continuous-world-preview.mp4`.
  Dies ist keine Aufnahme der Android-App.
- **Android-Kompilierung offen:** Online-Lauf vor Netzwerkfreigabe beendet; Offline-Lauf
  scheitert vor der Quellcode-Kompilierung an fehlenden Build-Abhaengigkeiten.
- Der erste Push wurde von der automatischen Freigabepruefung wegen nicht ausdruecklich
  freigegebenem GitHub-Ziel `MartinZurek/Itoeva` blockiert. Martin hat das Hochladen und Anlegen
  des Pull Requests am 07.10.2026 freigegeben. Android-/CI-Ergebnisse stehen im Remote-PR;
  eine Telefonabnahme und neue geraetegepruefte APK bleiben offen.

## Naechster Schritt und Grenzen

Die Freigabe zum Hochladen liegt vor. Charakterbasis und diesen Branch nach `MartinZurek/Itoeva`
pushen, den
gestapelten PR anlegen und Android-CI pruefen. Danach am Telefon Kamera/Touch, zwei Daumen,
Bank/Stamm/Kiste, Gespraech, Neustart auf beiden Seiten jeder Naht sowie GPU/Speicher/Bildrate
abnehmen. Diese Fassung ist noch kein geraetegepruefter Release.

Neun Innenraeume und die restliche Wildnis sind Folgepakete. Die neuen Baumkronen sind noch
keine ausgeschnittenen Windebenen; aktuell bewegen sich Gras/Blaetter als verankerte Overlays.
Details und Ruecksetzung: [itoeva2-continuous-world.md](itoeva2-continuous-world.md).
