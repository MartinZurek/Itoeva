# Itoeva – freigegebene 3D-Charaktere – Savespot A4 (25.10.2026)

## Zweck und Status

Martin moechte den aktuellen Stand in einem neuen Chat fortsetzen koennen.
Dies ist ein **gesicherter Modellierungsversuch**, keine abgeschlossene
originalgetreue Umsetzung und keine neue Test-APK.

Branch: `codex/moosling-spatial-rig-20261025`.
Basis: `e1c74245ae738388681f8275ccba359e30e8e574`
(`codex/approved-3d-designs-20261025`).
Vor Beginn geprueft: `main` bei `124186c77f20954321008b0bf373a1527562f11a`,
65 Commits hinter der Referenzbasis; keine fremden Aenderungen ueberschrieben.
Der letzte bestaetigte Lieferstand bleibt 0.15.5 / Code35 bei
`66dd18814c7fb6d157e6f581c4cbdc47391ec48d`, PR383,
Actions-Lauf40119857177. Der bisherige Prototyp ist dort standardmaessig aus.

## Verbindliche Vorlagen und Freigabe

Alle vier Dateien liegen auch in diesem Branch unter
`docs/approved-3d-designs-20261025/`:

1. `Moosling_Bewegungsentwurf.png`
2. `Starlight_3D_Entwurf_FREIGEGEBEN.png` – nur diese letzte korrigierte Fassung.
3. `Wyrmling_3D_Entwurf.png`
4. `Nocti_3D_Entwurf.png`

README und Manifest wurden gelesen; alle vier Bilder heruntergeladen, visuell
geoeffnet und per SHA-256 bestaetigt. Martin muss sie nicht erneut hochladen.
Es sind visuelle Entwuerfe, keine fertigen 3D-Modelle. Keine neuen Designs erfinden.

Martin hat Modelle, Texturen, Skelette, Animationen, Codeaenderungen auf eigenem
Branch, GitHub-Push, Entwurfs-PR und spaeter eine signierte Test-APK ueber den
bestehenden Actions-/Drive-Weg ausdruecklich erlaubt. Bestehende Signierung und
Zugangsdaten weiterverwenden. Keine erneute allgemeine Freigabe einholen.
Echte Modellrender und Clips vor dem naechsten APK-Einbau zeigen.

## In dieser Sitzung umgesetzt

Unter `tools/moosling-spatial/` existiert erstmals in diesem Arbeitszweig ein
einzelnes frei betrachtbares Moosling-Mesh mit 20 Knochen, Farbtexturen,
gewichteter Haut sowie getrennten Geh- und Vierfuesser-Laufclips.
Editierbare Blender-Datei, GLB, echte Renderbilder und Generator sind gesichert.
Der alte Projektionsvolumen-Code wurde nicht in die APK ersetzt.

Mehrere Renderiterationen: hervortretende Augen reduziert, Fellstraehnen statt
runder Schuppen, durchgehende Haut statt sichtbarer Gelenksteckverbindungen,
Vierfuesserhaltung korrigiert. **Trotzdem reicht die sichtbare Gestaltung noch
nicht an die genehmigte Moosling-Tafel heran.** Besonders Gesicht, Ohren,
Geweih, Fell und Tasche brauchen weitere Arbeit; nicht als Erfolg verkaufen.

Godot4.5.1 kann GLB und beide Clips importieren. Aktuell 4566 Einzelchecks,
14 Fehler bei der zusaetzlichen Kontaktpruefung; Details samt Messwerten in
`tools/moosling-spatial/output/godot-validation.json`.
Die aeltere Aussage4024/0 betrifft nur Import/Endlichkeit/Skalierung und reicht
nicht als Bewegungsnachweis. Noch keine Welt-/Android-Abnahme.

## Naechster konkreter Arbeitsschritt

1. `AGENTS.md` und seine Startdokumente lesen, Remote-Stand vergleichen.
2. Diese Uebergabe und `tools/moosling-spatial/README.md` lesen.
3. Freigegebene Moosling-Tafel neben die echten Render stellen. Modell deutlich
   naeher an Gesicht, Proportionen, Fell, Kleidung und Schmuck der Vorlage bringen.
   Den aktuellen Versuch nicht als neue Designrichtung behandeln.
4. Exportierte Animationen an den 14 fehlschlagenden Standkontakt-Stichproben
   untersuchen. Start-/Schleifenrand und Sample-/Exportinterpolation vergleichen.
   Den Test nicht lediglich lockern. Bodenkontakt auch an deformierter Geometrie
   pruefen, nicht nur an Gelenkankern.
5. Schrittlaenge und Frequenz gegen den echten Weltmassstab kalibrieren; bestehendes
   Fortbewegungstempo nicht pauschal reduzieren. Anfahren, Stoppen, Wenden und
   Uebergang zweibeinig/vierbeinig fehlen noch.
6. Erst bei ausreichendem Moosling-Ergebnis denselben Qualitaetsmassstab fuer
   Starlight, Wyrmling und Nocti anwenden. Diese drei sind in A4 noch nicht gebaut.
7. Vorschauen zeigen, dann erst Spielintegration und signierte APK. Upload pruefen,
   bevor die APK als fertig gemeldet wird.

## Unveraenderte Bewegungsanforderungen

- Moosling: ruhiges zweibeiniges Gehen; schneller Lauf auf allen vieren mit
  tragenden Vorderpfoten, Hinterbeinschub und Flugphasen. Starres Geweih/Kopf;
  Umhang, Ohren, Tasche und Schwanz folgen Bewegung; beim Sprint leicht atmen.
- Starlight: zusammenhaengender Stern, keine Beine/Fuesse. Wenig Stauchung,
  Abstoessen, langes Schweben, sanftes Landen; Schal folgt; Eigenlicht im Dunkeln.
- Wyrmling: vier echte Beine, nachvollziehbare Schulter-/Hueftkontakte,
  dezente Schwanz-/Fluegelbewegung.
- Nocti: ganze Schritte, glaubwuerdiger Absprung, anatomische Fluegelschlaege,
  ruhiger tiefer Flug mit angezogenen Fuessen.

## Kopierprompt fuer einen neuen Chat

> Setze „Itoeva – freigegebene 3D-Charaktere, Savespot A4 vom25.10.2026“ fort.
> Repository MartinZurek/Itoeva, Branch codex/moosling-spatial-rig-20261025.
> Lies zuerst AGENTS.md und docs/ITOEVA_3D_SAVESPOT_20261025.md sowie
> tools/moosling-spatial/README.md. Die vier freigegebenen PNGs liegen im selben
> Branch unter docs/approved-3d-designs-20261025/; oeffne sie visuell. Kein erneuter
> Upload noetig. Der vorhandene Moosling-A4-Versuch ist frei raeumlich und animiert,
> aber optisch noch nicht nah genug an der Vorlage;14 Kontaktchecks sind offen.
> Verbessere zuerst Moosling als Qualitaetsmassstab. Die Gestaltung bleibt
> verbindlich. Meine Freigabe fuer Modellierung, eigenen Branch, Push, Entwurfs-PR
> und spaeter gepruefte signierte Test-APK gilt weiter. Echte Modellrender und
> Bewegungsclips vor APK-Einbau zeigen; noch keine neue APK ist fertig.
