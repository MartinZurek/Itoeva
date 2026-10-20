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
Test-APK0.13.1/Code26 wird über den bestehenden signierten GitHub-/Drive-Weg
gebaut; Schlüssel, Paket und Drive-Ziel bleiben gleich. Noch nicht ausgeliefert.
Das 525-Dateien-Manifest hat SHA-256
`295e375dfba8330c99d75271ad45fb46cc28083791ccf8a6a3b58682505724e6`.
Telefon-Touchgefühl und menschliche Abnahme der neuen Wege bleiben offen.
