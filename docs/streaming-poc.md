# Vorbereitung fuer den Streaming-PoC: Emulator, OBS und Twitch

Status: **separates Vorbereitungsartefakt; NT-058 ist weiterhin offen und nicht durchgefuehrt**  
Einordnung: Voraussetzung fuer das Arbeitspaket NT-058, kein NT-058-Ergebnis  
Letzte Regelpruefung: 2026-09-10

Dieses Dokument ist Runbook und leerer Messbogen fuer den ersten begrenzten End-to-End-Test
einer oeffentlichen Itoeva-Instanz. Dieser PR liefert nur die Vorbereitung. NT-058 ist erst
erledigt, wenn ein tatsaechlicher Zwei-Stunden-Lauf samt Messwerten, Screenshots,
Wiederanlaufbefund und begruendeter GO/CHANGE/STOP-Empfehlung dokumentiert ist. Leere
`TODO`-Felder sind kein positives Testergebnis.

## Ziel und feste Grenze

Eine unveraenderte `:app-sim`-Instanz laeuft in einem Android-Emulator. Der Spielmodus bleibt
mindestens zwei Stunden sichtbar und OBS zeichnet Bild und ausschliesslich den App-Ton lokal auf.
Erst danach darf optional ein Twitch-Bandbreitentest folgen.

Der PoC prueft:

- ob das Avatarleben ueber zwei Stunden sichtbar interessant und technisch stabil bleibt;
- ob Musik, Aktivitaeten und Szenenwechsel fuer Zuschauer zusammenpassen;
- welches Seitenverhaeltnis den runden Pixelbildschirm lesbar zeigt;
- ob Emulator und Encoder auf dem Testrechner genuegend Reserven haben;
- ob sich App, Emulator und Aufnahme kontrolliert wieder starten lassen.

Nicht enthalten sind Cloud-Betrieb, sechs parallele Instanzen, eine neue Weltengine,
Zuschauerinteraktion, Monetarisierung oder ein oeffentlicher Simulcast. Der PoC verwendet eine
frische, rein erfundene Instanz und keine privaten Reminder, Nutzerhistorien, Konten oder
medizinischen Inhalte.

## Geheimnisse und Konten

- Niemals Twitch-Stream-Key, Zugangsdaten oder ausgefuellte OBS-Profile committen, fotografieren
  oder in Logs beziehungsweise PR-Beschreibungen kopieren.
- Fuer den lokalen Zwei-Stunden-Lauf ist kein Twitch-Konto erforderlich.
- Twitch bietet fuer diesen Zweck keinen mit YouTube vergleichbaren nicht gelisteten Livestream.
  Fuer einen unsichtbaren Verbindungstest den offiziellen
  [Twitch Inspector](https://inspector.twitch.tv/) verwenden und lokal
  `?bandwidthtest=true` an den Stream-Key anhaengen. Laut Inspector erscheint der Kanal damit
  nicht online und es werden keine Benachrichtigungen gesendet.
- Vor jedem spaeteren Simulcast die dann aktuellen
  [Twitch-Simulcasting-Regeln](https://help.twitch.tv/s/article/simulcasting-guidelines) und
  [Twitch-Nutzungsbedingungen](https://legal.twitch.com/legal/terms-of-service/) erneut pruefen.
- Der erste oeffentliche Test bekommt noch keinen zusammengefuehrten Chat, keine Donations und
  keine Zuschauerbefehle.

## 1. Saubere Testinstanz vorbereiten

### Rechner und Emulator erfassen

| Feld | Wert |
|---|---|
| Datum und lokale Uhrzeit | TODO |
| Betriebssystem | TODO |
| CPU / GPU | TODO |
| RAM gesamt | TODO |
| OBS-Version | TODO |
| Android-Studio-Version | TODO |
| Emulator / API-Level | TODO |
| getesteter Git-Commit | TODO |
| getestete Spezies | TODO |

Eine neue AVD ohne Google-Konto verwenden. Keine persoenlichen Reminder oder echten Namen
anlegen. Netzwerkbenachrichtigungen anderer Programme vor der Aufnahme deaktivieren.

Die Debug-App bauen und installieren. Unter Windows vorher `JAVA_HOME` gemaess
`CLAUDE.md` setzen.

PowerShell unter Windows:

```powershell
.\gradlew.bat :app-sim:installDebug
adb.exe shell am start -n com.notime.glyphminderwatch/com.notime.glyphsim.ui.MainActivity
```

Bash unter macOS oder Linux:

```bash
./gradlew :app-sim:installDebug
adb shell am start -n com.notime.glyphminderwatch/com.notime.glyphsim.ui.MainActivity
```

Im Emulator den Bildschirm nur fuer den Test wach halten; die Einstellung danach wieder
zuruecksetzen. Unter PowerShell `adb.exe`, unter Bash `adb` verwenden:

```text
adb shell svc power stayon true
# Nach dem Test:
adb shell svc power stayon false
```

In der App:

1. eine einzige Spezies fuer den gesamten Lauf waehlen und oben dokumentieren;
2. Musik im Spielmodus einschalten;
3. den Spielmodus oeffnen und keine persoenlichen Reminder konfigurieren;
4. Systemlautstaerke und App-Ton vor OBS einmal mit Kopfhoerern pruefen;
5. Benachrichtigungsleiste, Emulator-Bedienelemente und Mauszeiger aus dem Bild halten.

## 2. Bildformat vor dem Langzeittest entscheiden

Der erste PoC nutzt eine OBS-Leinwand mit **1920 x 1080 bei 30 fps**. Das ist ein bewusst
konservatives Testprofil und keine Behauptung ueber eine dauerhafte Plattformvorgabe.

Vor dem Zwei-Stunden-Lauf zwei je zehnminuetige Probeaufnahmen vergleichen:

| Variante | Aufbau | Lesbarkeit | Avatar korrekt | ungenutzte Flaeche | Entscheidung |
|---|---|---:|---:|---:|---|
| A | App im Hochformat, auf volle Hoehe skaliert und mittig auf 16:9 | TODO | TODO | TODO | TODO |
| B | App-interner Querformatmodus, ohne nachtraegliches Strecken | TODO | TODO | TODO | TODO |

Die Quelle niemals ungleichmaessig strecken. Wenn Variante B den bekannten Fehler ausloest, bei
dem der Avatar nach einer Drehung falsch oder schwebend steht, Variante A verwenden und den
Fehler als Produktbefund dokumentieren. Die Zwei-Stunden-Messung soll nicht durch wiederholte
Orientierungswechsel verfremdet werden.

Im ersten PoC keine Werbe-, Chat- oder Spenden-Overlays einblenden. Freie Seitenflaechen bleiben
neutral. Ein spaeterer sachlicher App-Link gehoert zunaechst nur in die Kanalbeschreibung.

## 3. OBS-Profil

Eine Szene `Itoeva PoC` mit genau diesen Quellen anlegen:

1. Fensteraufnahme des Emulators, auf den App-Inhalt zugeschnitten;
2. App-/Fenster-Audio des Emulators;
3. keine Mikrofonquelle und kein uebriger Desktop-Ton.

Ausgangsprofil fuer vergleichbare Messungen:

| Einstellung | Startwert |
|---|---|
| Basis- und Ausgabeaufloesung | 1920 x 1080 |
| Bildrate | 30 fps |
| Videoencoder | Hardware-H.264, falls stabil; sonst Software-H.264 |
| Bitratensteuerung | CBR |
| Videobitrate | 4.500 kbit/s |
| Keyframe-Intervall | 2 Sekunden |
| Audio | AAC, 48 kHz, Stereo, 160 kbit/s |
| Lokales Aufnahmeformat | MKV |
| Automatisches Remuxen | optional nach MP4, Original-MKV behalten |

Diese Werte sind ein reproduzierbarer Ausgangspunkt fuer den PoC. Vor einem oeffentlichen Stream
die aktuellen [Twitch-Broadcasting-Hinweise](https://help.twitch.tv/s/article/broadcasting-guidelines)
erneut gegen das dann verwendete OBS-Profil pruefen.

Vor Start zehn Sekunden aufnehmen und kontrollieren:

- Bild scharf, vollstaendig und ohne Emulatorrahmen;
- App-Musik auf beiden Kanaelen, kein Mikrofon und kein Systemton;
- keine rote Pegelanzeige beziehungsweise hoerbares Clipping;
- OBS-Statistik zeigt keine Render- oder Encoding-Ueberlastung;
- Dateipfad hat mindestens 20 GB freien Speicher.

## 4. Zwei-Stunden-Lauf

Zuerst **120 Minuten lokal aufnehmen**, ohne Twitch-Verbindung. Die Anwendung waehrenddessen
nicht bedienen, ausser ein klar definierter Wiederanlauftest folgt nach der abgeschlossenen
Aufnahme.

Alle 15 Minuten einen Messpunkt erfassen. Zusaetzlich jedes Ereignis protokollieren, das laenger
als fuenf Sekunden Bild oder Ton sichtbar beeintraechtigt.

| Minute | Ort / sichtbare Aktivitaet | Musik / Wechsel | CPU gesamt | RAM gesamt | OBS-Ausfaelle | Auffaelligkeit |
|---:|---|---|---:|---:|---|---|
| 0 | TODO | TODO | TODO | TODO | TODO | TODO |
| 15 | TODO | TODO | TODO | TODO | TODO | TODO |
| 30 | TODO | TODO | TODO | TODO | TODO | TODO |
| 45 | TODO | TODO | TODO | TODO | TODO | TODO |
| 60 | TODO | TODO | TODO | TODO | TODO | TODO |
| 75 | TODO | TODO | TODO | TODO | TODO | TODO |
| 90 | TODO | TODO | TODO | TODO | TODO | TODO |
| 105 | TODO | TODO | TODO | TODO | TODO | TODO |
| 120 | TODO | TODO | TODO | TODO | TODO | TODO |

Nach dem Lauf aus Aufnahme und Notizen ermitteln:

| Kennzahl | Ergebnis |
|---|---|
| App-Abstuerze | TODO |
| laengster Bildstillstand ohne beabsichtigte Ruhe | TODO |
| Audioausfaelle ueber 5 Sekunden | TODO |
| OBS: verlorene Frames | TODO |
| OBS: wegen Rendering verpasste Frames | TODO |
| OBS: wegen Encoding verpasste Frames | TODO |
| CPU Durchschnitt / Spitze | TODO |
| RAM Start / Ende / Spitze | TODO |
| verschiedene Orte | TODO |
| verschiedene Aktivitaeten | TODO |
| sichtbare Reaktionen / Animationen | TODO |
| Musikstuecke und Wechselzeitpunkte | TODO |
| laengste Phase ohne sichtbare Veraenderung | TODO |
| harte oder unpassende Musikwechsel | TODO |
| Avatar- oder Szenenfehler | TODO |

Nicht nur Mengen zaehlen. Zu jedem problematischen Abschnitt den genauen Zeitstempel der
Aufnahme notieren. Besonders pruefen:

- Bleibt die Figur zu lange nur drinnen oder sichtbar untatig?
- Wirkt ein Aussenaufenthalt wie ein echter Aufenthalt statt wie ein kurzer Durchgang?
- Aendert sich die Musik innerhalb einer stabilen Rolle nach etwa drei bis fuenf Minuten?
- Passt der Musikwechsel zur sichtbaren Situation und ist die Ueberblendung weich?
- Sind Aktivitaeten, Text und wichtige Pixel auf einem normalen Laptopbildschirm erkennbar?
- Gibt es innerhalb von zehn Minuten mindestens einen Grund, weiter zuzusehen?

## 5. Wiederanlauf separat pruefen

Nach der vollstaendigen Zwei-Stunden-Aufnahme:

1. App per Android-Einstellungen beenden und wieder oeffnen;
2. OBS-Aufnahme stoppen und neu starten;
3. Emulator kontrolliert neu starten;
4. App erneut in den Spielmodus bringen.

| Schritt | Zeit bis wieder sichtbares Leben | Zustand plausibel fortgesetzt | manueller Eingriff | Befund |
|---|---:|---:|---|---|
| App-Neustart | TODO | TODO | TODO | TODO |
| OBS-Neustart | TODO | TODO | TODO | TODO |
| Emulator-Neustart | TODO | TODO | TODO | TODO |

Ein zufaelliger oder sichtbar widerspruechlicher Neustart ist kein Cloud-Problem, sondern ein
Befund fuer NT-059. Noch keinen neuen Checkpoint-Dienst bauen.

## 6. Optionaler Twitch-Inspector-Test

Nur nach erfolgreicher lokaler Aufnahme:

1. Stream-Key ausschliesslich lokal in OBS eintragen.
2. Gemaess Twitch Inspector `?bandwidthtest=true` anhaengen.
3. 30 Minuten mit demselben Profil senden.
4. Inspector-Sitzung auf Stabilitaet, Bitrate und Verbindungsabbrueche pruefen.
5. Stream-Key nie in Screenshot, Export oder Repository aufnehmen und danach aus dem OBS-Profil
   entfernen.

| Kennzahl | Ergebnis |
|---|---|
| Ingest-Region | TODO |
| Testdauer | TODO |
| durchschnittliche / schwankende Bitrate | TODO |
| Verbindungsabbrueche | TODO |
| Inspector-Warnungen | TODO |

## 7. Screenshots und Nachweise

Nur Bilder ohne Kontodaten oder Stream-Key ablegen:

- `docs/streaming-poc/01-portrait.png`
- `docs/streaming-poc/02-landscape.png`
- `docs/streaming-poc/03-obs-stats-start.png`
- `docs/streaming-poc/04-obs-stats-end.png`
- `docs/streaming-poc/05-twitch-inspector.png` nur beim optionalen Test

Die zweistuendige Aufnahme ist zu gross fuer Git und bleibt lokal. Im Ergebnis nur Dateiname,
Dauer, Groesse und SHA-256 notieren.

## 8. Entscheidung fuer NT-059

Dieser Abschnitt wird erst im tatsaechlichen NT-058-Lauf ausgefuellt. Vorher darf dieses
Vorbereitungsartefakt keine Entscheidung fuer NT-059 ausloesen.

Nach dem Lauf genau eine Empfehlung markieren und mit Messwerten begruenden:

- **GO:** technisch stabil, Bild und Ton tragfaehig, Aktivitaeten und Musik bieten ueber zwei
  Stunden genuegend beobachtbare Veraenderung. NT-059 darf den emulatorbasierten Einzelbetrieb
  weiter bewerten.
- **CHANGE:** technisch grundsaetzlich stabil, aber ein klar benannter Produkt- oder
  Darstellungshebel muss zuerst verbessert werden, etwa lange Leerlaufphasen, Bildformat,
  Musikmonotonie oder falsche Avatarposition.
- **STOP:** Abstuerze, Datenschutzrisiko, anhaltende Bild-/Audioausfaelle oder Ressourcenlast
  machen selbst den Einzelbetrieb unbrauchbar. Vor weiterer Streaming-Infrastruktur zuerst diese
  Ursache beheben.

**Auswahl:** TODO  
**Begruendung mit Messwerten und Zeitstempeln:** TODO

NT-060, Cloud-Host und mehrere Charakterinstanzen bleiben gesperrt, bis NT-059 die Ergebnisse
dieses Dokuments ausgewertet hat.
