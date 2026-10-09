# Itoeva 2: Statur und Stoffnachlauf – 09.10.2026

Basis: PR #350, `431bd178`. Neuer Branch: `codex/character-silhouettes-and-cloth`.
Martin baut die APK separat; dieser Schnitt erfordert eine spaetere eigene APK.

## Was sich sichtbar aendert

| Wesen | Statur und Bewegung |
|---|---|
| Fennec | Kleine Taillenkorrektur; der freie Mantel folgt Tempo und Wind verzoegert und schwingt beim Anhalten aus. |
| Puffling | Deutlichere Taille unter der unveraenderten Kapuze; sanfte Brustbewegung und leichter Stoffnachlauf. |
| Gloop | Keine Taille einer Knochenfigur; gekoppelte Stauch-/Streckbewegung mit kleinem Breitenausgleich. Blattkleidung bleibt leichter als Stoff. |
| Wyrmling | Kleine Taillenkorrektur; schwerere, ruhigere Koerperreaktion und Halstuchnachlauf. |
| Starlet | Sternanatomie bleibt breit; kleine eigene Gewichtsreaktion und Kragennachlauf. |
| Hootlet | Leicht schlankerer Rumpf, langsamer Atemrhythmus und geduldig bewegter Federumhang. |

Das neue 24x32-Texturnetz zeichnet zusammenhaengende Dreiecke, statt Stoffteile
in 12x12-Kacheln um gerundete Quellpixel zu verschieben. Kleine Auslenkungen
bleiben sichtbar und es entstehen keine Luecken zwischen verschobenen Kacheln.
Bitmapfilterung zeichnet Skalierungsstufen weicher; sie ersetzt keine gemalten
Konturen oder fehlenden anatomischen Quellzeichnungen.

Fusszeilen ab 120/128, Kopf und Kragen bleiben fest. Kleidergewichte unterscheiden
Profil von Front/Ruecken. Schlaf und Rolle behalten die kompakte Originalzeichnung.
Das Modell reagiert auf den vorhandenen Gangtakt, Tempo und Wind. Es waehlt keine
Handlungen und veraendert weder Wegphysik noch Beduerfnisse. Die aktuelle Quelle
und Weltgroesse bleiben erhalten. Keine neuen Bilder-/Spielstandformate.

## Umsetzung und Leistung

- `GameCharacterMotion.kt`: reine Netzrechnung, individuelle Material-/Staturwerte,
  exakter Nachlauf zweier Daempfer und begrenzte freie Saeume.
- `GameCharacterPainter.kt`: native Android-Bitmapzeichnung mit interpoliertem
  Ortslicht, bestehendem Alpha und wiederverwendeten Arrays/Paint.
- `AvatarSpriteView.kt`: optionaler Game-Zeichenweg; einmalige Ermittlung des
  oberen Alpha-Punkts je gezeichnetem Bild im bestehenden Cache.

Der neue Weg braucht einen Netz-Zeichenaufruf pro gezeichnetem Sprite statt bis
zu 144 Kachelaufrufen; beim Gangartwechsel zwei wegen der bestehenden Ueberblendung.
Keine neuen Bitmaps oder Texturen pro Bild. Tatsaechliche GPU-Leistung und
Telefon-Bildrate sind noch nicht gemessen.

## Vorschau wiederherstellen

Nach dem Offline-Testlauf (dieser bereitet den Kotlin-Klassenpfad vor):

```bash
bash tools/reaction-preview/tests.sh
tools/reaction-preview/.work/kotlinc/bin/kotlinc \
  tools/reaction-preview/src/CharacterMotionPreview.kt \
  -cp tools/reaction-preview/.work/tests \
  -d tools/reaction-preview/.work/character-preview
tools/reaction-preview/.work/kotlinc/bin/kotlin \
  -cp tools/reaction-preview/.work/character-preview:tools/reaction-preview/.work/tests \
  CharacterMotionPreviewKt . /tmp/itoeva-character-mesh
python3 tools/character-art/game_motion_preview.py /tmp/itoeva-character-mesh \
  tools/character-art/game-motion-preview.gif
```

Die GIF zeigt dieselben Ausgangszeichnungen links/rechts in gleichem Weltmassstab.
Die neue Seite benutzt die produktive Kotlin-Netzrechnung und bilineare
Software-Texturabtastung. Sie ist keine Android-Aufnahme und prueft keine
Touch-/Lifecycle-/GPU-Wirkung. Szenen: ruhiger Wind, Gang, Anhalten/Ausschwingen.

## Pruefstand und naechster Schritt

Neun neue Kotlin-Tests gruen, einschliesslich Extremwind-/Faltpruefung; 67
gezielte Bewegungs-/Massstabs-/Aktionsregressionen gruen. Finaler Gesamtlauf:
1.024 reine Kotlin-Tests gruen. Android-CI-Stand im PR. Zwei neue instrumentierte Tests rendern alle sechs Wesen
mit echten Android-Bitmaps, pruefen transparente Raender/Boden und die beim
Spiegeln unveraenderte physische Lichtseite.

Telefonabnahme: Alle sechs Wesen im Stand, nach kurzem Lauf und unmittelbar beim
Anhalten beobachten. Mit Fennec vorwaerts, rueckwaerts und seitlich gehen;
Wind draussen mit einem Innenraum vergleichen. Pufflings Kopf-/Rumpfverhaeltnis,
Sitzen/Aufstehen, Springen und Rollen beurteilen. Insbesondere auf dauerhafte
Flatterbewegung, abgeschnittene Saeume und Bildrate mit zwei NPCs achten.

App-1-/Stream-Zeichnung, Original-PNGs, Speicherformat, Kollisions-/Kameraregeln
und PR #350 bleiben erhalten. Ruecknahme des gesamten Charaktercommits reicht.

## Fortsetzung nach Uploadfreigabe

Martin hat den Upload und PR ausdruecklich beauftragt. Der PR richtet sich gegen
`codex/fix-world-controls-and-passages` und baut somit auf #350 auf. Zusaetzlich
sind eigenstaendige Laufbewegung, Starlets Bodenkontakt, plausible Weltbewegung
und die gemeldete Stille trotz Music on beauftragt. Diese Fortsetzung und ihr
Pruefstand werden im selben PR dokumentiert. Die separat gebaute APK aus #350
enthaelt diesen Charakterschnitt noch nicht.
