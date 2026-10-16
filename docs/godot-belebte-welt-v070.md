# Godot 0.7.0: Bewohner, Waldwege und Weltatlas

Basis: 0.6.2 / 96e8a0c69dcdd23198661fa0966746aa9ea47b65.
AndroidTest 0.7.0 / Code 19. Branch: codex/godot-inhabited-world-v070.

## Sichtbares Verhalten

Elf Bewohner verteilen sich über Häuser, Küste, Markt, Dorfwege und helle
Waldorte. Kein Aufenthalt liegt im Dunkelwald. Die gewählte Spielfigur erscheint
nicht zusätzlich als Bewohner. Die acht spielbaren Wesen bleiben erhalten.

Nella ist eine Otter-Kartenzeichnerin im Schilfmantel, Brumm ein Dachs-Dorfbäcker
mit Brotkorb und Tilla eine Igel-Kräuterkundige mit Blütenstab. Diese drei neuen
NPCs wurden zuerst als eigene Concept Art entworfen und daraus mit je sechs
Ruhe-, Geh- und Arbeitsposen integriert. Auch die bisherigen acht Figuren haben
individuelle Rollen, Aufenthalte und Tätigkeitsdialoge.

Fünf Tagesabschnitte verwenden die vorhandene Berliner Spielzeit. Welt und Karte
teilen einen Aufenthaltssnapshot. Szenenwechsel, Kartenöffnung, Lesen und Schlafen
aktualisieren ihn. Eine neue Uhrstunde teleportiert keine sichtbare Figur mitten
im freien Spiel. Kurze Spaziergänge und Pausen bleiben auf begehbaren Flächen.
Dialoge nennen die Tätigkeit am tatsächlichen Aufenthaltsort; keine erfundenen
abgeschlossenen Lieferungen, persönlichen Erinnerungen oder LAS-Migration.

Sonnenhain erweitert den hellen Wald; Nebelwacht liegt jenseits des tiefen Walds.
Sonnenhain ist vom bisherigen Waldrand und Grünen Wald erreichbar. Sein Weiterweg
und der Tiefe Wald treffen an der Nebelwacht zusammen. Dahinter sieht man die
Schlucht und ferne vulkanische Berge: **keine neue Brücke, kein begehbarer Übergang
und kein Dämonenwelt-Raum**. Rückwege landen im passenden Waldabschnitt. Der alte
Nachtwurz-Trigger wurde auf die Arena begrenzt, damit die Rückkehr aus Nebelwacht
nicht rückwärts zur Arena teleportiert.

Der Atlas ersetzt die Namensliste: eigenes gemaltes Gelände, deterministisch
gezeichnete Routen, 20 bedienbare Gruppen für alle 37 Orte, aktueller Standort,
Besuche, Verbindungen und Bewohner. Marker öffnen Informationen, keinen Schnelltransport.

## Gegner und Fortschritt

| Gegner | Ort / LP | Eigenes Verhalten |
|---|---|---|
| Dornmotte | Sonnenhain / 100 | Pollen gegen beide; alle drei Runden Verstummen; ein ungebrochener Lichtschirm schützt |
| Aschenläufer | Nebelwacht / 150 | Aschenbiss und jeder dritte Zug kräftiger Glutsprung |
| Rindenwächter | Nebelwacht / 190 | Wurzelschlag und Heilung alle drei Runden; Brennen verhindert Regeneration |

Gegner sind sichtbar und zuerst beobachtbar; Kampf startet bewusst. Eigene Posen
für Ruhe, Vorbereitung, Angriff, Treffer, Niederlage und Wache. Die vorhandenen
40-Karten-, Helfer-, Klang- und Siegesabläufe bleiben integriert. Wiederholung und
Neustart nach Rückzug verwenden das gerade bearbeitete Deck.

Regionale Zustände liegen in forest_encounters[species:enemy_id]. Der alte
Nachtwurz-Schlüssel bleibt. battle_progress[species] teilt XP, Vorrat und Siege
zwischen den Begegnungen derselben Leitfigur und übernimmt bisherige Werte aus
dem alten Boss-Snapshot. Quittungen bleiben beim Kampf, damit Laden nicht doppelt
belohnt. Fortschritt und Kampfzustand werden zusammen im bisherigen Godot-Save
abgelegt. Native Android-/Room-/Reminder-Daten bleiben unberührt. Vor Rückbau
sichern: 0.6.2 liest neue regionale Gewinne nicht vollständig.

## Grafikquellen und Prüfung

Sechs eigene Imagegen-Bilder in assets/expansion: concepts.png (Entwurfsblatt,
vom APK ausgeschlossen), citizens.png und enemies.png (transparente 6×3-Rohatlanten),
sun-glade.png, mist-watch.png und world-atlas.png. Referenzen waren die eigenen
Itoeva-Park-/Fennec-Bilder und daraus erzeugte Entwürfe. Keine fremden Spielgrafiken.
Rohbilder unverändert; Zuschnittrechtecke anhand der Alpha-Grenzen in den Atlas-JSONs.
Genaue Wege und Labels werden im Code gezeichnet, nicht aus der Illustration geraten.

Geprüft: 623 neue Integrationschecks und 82 Vollspielchecks ohne Fehler. Bestehende
Regressionen: Kartenmodell95, Regeln15, Spezial82, Sieg96, Karten-UI69, Helfer24,
Wald22027, acht gewonnene vollständige Kartenkämpfe, Framing104, Türmatrix208,
Kartenerweiterung441, Deck-Touch8, Lichtkoordinaten225, Kampfklang29, Politur36.
Echte OpenGL-Aufnahmen von Markt, beiden neuen Orten, Nellas Dialog, Atlas,
Gegnerbeschreibung und Kampf geprüft. Zweiter Agent prüfte den Code; Standort-,
Belohnungs- und Deckbefunde sind korrigiert und gezielt erneut geprüft.

Actions wiederholt die Tests und prüft Paket, arm64, Signatur und Hashes, bevor
die freigegebene Drive-Datei per uploadType=resumable ersetzt wird. Telefon-GPU,
FPS, Klang und tatsächliche Geräte-Touch-Abnahme bleiben offen. Einzelne bekannte
Godot-Shutdown-Warnungen von Testansichten sind keine Geräteprüfung.
