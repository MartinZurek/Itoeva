# Godot: sichtbare Streckenanschlüsse

Stand: 20.10.2026. Basis ist die ausgelieferte Testfassung0.13.0,
Branch `codex/godot-stamina-motion-fix`/PR374, nicht das ältere `main`.
Arbeitsbranch: `codex/godot-visible-route-connections`.

## Verhalten

Der Waldeingang liegt im rechten Teil des Waldrands bei Orts-UV(0.90,0.55)
statt(0.52,0.64). Sein Zugang, antippbares Schild, automatische Annäherung und
Rückkehrpunkt verwenden denselben Endpunkt. Das Schild „Waldpfad →“ ist vom
mitwandernden Richtungspfeil am Bildschirmrand deutlich zu unterscheiden.

Alle acht Streckenpaare bleiben erhalten: Waldrand/Waldpfad,
Waldrand/Sonnenhain, Grüner Wald/Sonnenhain, Tiefer Schattenwald/Nebelwacht,
Sonnenhain/Mühlenbach, Sonnenhain/Nebelwacht, Nebelwacht/Aschenstieg und
Aschenstieg/Siegelhof. Die gemalten Kulissen werden weiterverwendet.

- Sonnenhain: links zurück zum Waldrand, daneben Mühlenbach, mittlerer Abzweig
  zum Grünen Wald, rechts weiter zur Nebelwacht.
- Nebelwacht: links zurück zum Sonnenhain, mittlerer Abzweig zum Schattenwald,
  rechts weiter zum Aschenstieg. Der Rückweg lag zuvor rechts neben dem Weiterweg.
- Aschenstieg: links zurück zur Nebelwacht, rechts weiter zum Siegelhof.
- Mühlenbach und Siegelhof zeigen am jeweiligen Rückweg das passende Gegenziel.

Lesbare, feste Wegschilder mit Richtung und Pfosten ersetzen an diesen Anschlüssen
die blassen Türsymbole. Die ganze Tafel kann angetippt werden. Gebäude behalten
bestehende Türsymbole. Die Waldverbindungen stehen nun einmalig in
`forest_trail.json`; beide Bühnen erzeugen daraus ihre gegenseitigen Türen.

Bestehende Orts-/Profil-/Kampfschlüssel bleiben erhalten. Alte Standorte werden
wie bisher auf freien Boden gesetzt. Es gibt keine automatische Reise beim
bloßen Vorbeilaufen. Tempo, Ausdauer und Figurenstand0.13.0 bleiben enthalten.

## Prüfung

`route_connections.gd` prüft die drei Wald-Verbindungspaare mit allen acht Wesen,
beide Reiserichtungen und die Übereinstimmung der Ankunft mit dem Rückweg.
Dazu kommen ein tatsächlicher Tipp außerhalb des alten Icon-Radius mit
Annäherung und Waldwechsel, erreichbare Zugänge sowie getrennte Schilder in
allen sechs angeschlossenen Außenorten.

`route_capture.gd` rendert Waldrand, Waldsaum und die fünf Erweiterungsorte
mit echtem OpenGL. Sichtprüfung der ersten korrigierten Ansichten bestanden.
181 neue Streckenprüfungen,256 bestehende Spezies-/Türpfade und363
Ausdauer-/Bewegungschecks bestanden. Alle sieben korrigierten OpenGL-Ansichten
visuell geprüft. Katalogressourcen bleiben bei diesen bestehenden Tests teilweise
bis zum Prozessende gehalten; keine funktionalen Fehler.

PR375: https://github.com/MartinZurek/Itoeva/pull/375
APK-Baulauf: https://github.com/MartinZurek/Itoeva/actions/runs/39425272411
Laufzeit-Commit: `d7bf81cbe6dfb39a57a03b3ba3683a6c585f9855`.
Test-APK0.13.1/Code26 wurde über den bestehenden signierten GitHub-/Drive-Weg
erfolgreich gebaut und ausgeliefert. Vollständige Spielregressionen, OpenGL,
Export, Version, bestehendes Zertifikat und Remote-Prüfsummenabgleich bestanden.
Schlüssel, Paket und Drive-Ziel bleiben gleich; als Update zu0.13.0 installierbar.
Das 525-Dateien-Manifest hat SHA-256
`295e375dfba8330c99d75271ad45fb46cc28083791ccf8a6a3b58682505724e6`.
Telefon-Touchgefühl und menschliche Abnahme der neuen Wege bleiben offen.

## Liefernachweis

- Download: https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk
- Datei: `Itoeva_Godot_Test_v0.13.1.apk`,191730312 Bytes.
- SHA-256: `8f6eb60d2a5f4a2e87094ec32e2719de90887dfd5f9bdcb8b112431cc8c54132`.
- Paket: `com.itoeva.godot.hd2d.worldtest`,Code26,ARM64.
- Zertifikat-SHA-256: `30dbcf9daa7c26e4f76a1c2da674ee8b5e732186a058081e2dabd34eb82d58dc`.
- Bericht: `docs/validation/godot-v0131-build-report.json`.
- GitHub-APK-Artefakt12265654172; Prüfprotokolle12266073418,14 Tage aufbewahrt.
- Allgemeiner Verify-Lauf39425565075: API26/API35 und Release-Torwächter bestanden;
  Gradle-Tests/Lint/R8 zum Dokumentationskopf liefen bei dieser Eintragung noch.

Keine Zusammenführung ins ältere `main`. Nächster Schritt:0.13.1 über0.13.0
installieren und Waldeingang, Sonnenhain, Nebelwacht und Aschenstieg auf dem
Telefon abgehen; Schild antippen und jeden Abzweig auch zurück benutzen.
