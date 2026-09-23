# Itoeva Music

Itoevas Musik soll sich mit Welt und Charakter entwickeln, aber nicht bei jedem App-Start live von einem Modell erzeugt werden. Generierte Stuecke werden deshalb wie andere Spielinhalte versioniert: Prompt -> Open-Weights-Modell -> pruefbare Audiodatei -> Pull Request -> App-Asset.

## Die ersten beiden Tracks

`home-evening-01` / **Quiet Lanterns** ist die erste klangliche Referenz fuer ruhige Home- und Abendszenen. Der Prompt beschreibt Eigenschaften und Instrumente, nicht den Stil eines konkreten lebenden oder verstorbenen Kuenstlers.

`main-day-01` / **Lantern Streets** traegt dieselbe warme, japanisch gefaerbte Jazz-/Lo-Fi-
Sprache in den normalen Tagesablauf. Beide Tracks liegen als gepruefte Ogg/Vorbis-Assets in der
App; weitere Rollen bleiben absichtlich ohne erfundenen Ersatz, bis ein passendes Stueck erzeugt
und gehoert wurde.

Definitionen liegen in:

- `music/manifest.json` - Track-ID, Modell, Dauer, Seed und Android-Ressourcenname.
- `music/prompts/` - versionierte Textprompts.
- `tools/music/generate_music.py` - reproduzierbarer lokaler Stable-Audio-3-Aufruf.
- `.github/workflows/generate-music.yml` - manueller Open-Weights-Lauf auf einem GitHub-Runner.

## Warum Small Music statt der Stability API

Die Pipeline nutzt jetzt `Stable Audio 3 Small Music` als Open-Weights-Modell. Fuer jede Generierung fallen dadurch **keine Stability-API-Credits** an. `small-music` ist fuer Musik, CPU-Inferenz und bis zu 120 Sekunden ausgelegt; das passt zum 90-Sekunden-Referenztrack.

`Stable Audio 3 Medium` bleibt als spaetere Qualitaetsoption denkbar, braucht aber CUDA/Flash Attention und ist deshalb nicht der Standard fuer den normalen GitHub-Runner.

### Verbindlicher Inferenzmodus: 8 Schritte, CFG 1

`small-music` ist ein **post-trainierter** Stable-Audio-3-Checkpoint, kein `*-base`-Modell. Die
Dokumentation der im Manifest gepinnten Runtime nennt dafuer `steps=8` und `cfg_scale=1` als
Inferenzmodus; groessere Schrittzahlen sind fuer die Base-Modelle vorgesehen, ebenso abweichende
Classifier-Free-Guidance-Werte.

Das ist seit dem Hoertest vom 2026-09-23 eine harte, im Generator gepruefte Regel. Vier mit 50/5
erzeugte Takes (PR #194 bis #197) bestanden nach reiner Pegelkorrektur das technische Loop-/Peak-
Gate, klangen aber wie verzerrtes Computerrauschen. Ihre fertigen Dateien zeigen gegenueber den
hoerbaren 8/1-Referenzen deutlich mehr breitbandige Hochfrequenzenergie und eine dichtere
Signalform. Die vier PRs bleiben deshalb ungemergtes Referenzmaterial. Ein Prompt oder Seed darf
weiterentwickelt werden; fuer dieses Modell darf daraus kein eigener Diffusionsstandard werden.

## Einmalig: kostenlosen Hugging-Face-Zugang freischalten

Die Gewichte sind kostenlos nutzbar, aber auf Hugging Face gated. Einmalig:

1. Bei Hugging Face anmelden.
2. Die Bedingungen fuer `stabilityai/stable-audio-3-small-music` akzeptieren.
3. Einen Read-Token erzeugen.
4. In GitHub unter **Settings -> Secrets and variables -> Actions -> New repository secret** ein Secret `HF_TOKEN` anlegen.

Der Token ist nur fuer den Download der Modellgewichte gedacht. Er darf nie in Issues, Logs, Prompts oder Commits kopiert werden. Ein `STABILITY_API_KEY` wird fuer diese Pipeline nicht mehr benoetigt.

## Generieren

GitHub Actions -> **Generate Itoeva Music** -> Run workflow.

Standardmaessig wird `home-evening-01` erzeugt. Ein erfolgreicher Lauf:

1. validiert Manifest und Prompt ohne Modell-Download,
2. installiert die festgehaltene Stable-Audio-3-Inferenzbibliothek,
3. laedt die freigeschalteten Open Weights ueber Hugging Face,
4. erzeugt den Track lokal auf dem Runner,
5. speichert Ogg/Vorbis plus Metadaten als GitHub-Artifact,
6. legt bei aktiviertem `create_pr` einen neuen Branch an,
7. kopiert das Audio nach `app-sim/src/main/res/raw/`,
8. oeffnet einen PR gegen `main`.

So landet kein ungehoerter Modelloutput automatisch in der App. Erst der Merge des generierten PR macht ihn zum versionierten Spiel-Asset.

## Zugang einmal pruefen, bevor generiert wird

GitHub Actions -> **Stable Audio Hugging Face Check** -> Run workflow.

Der Lauf ist eine reine Diagnose und **keine zweite Musikpipeline**: Er erzeugt nichts, schreibt
nichts und laedt keine Modellgewichte. Er beantwortet in dieser Reihenfolge vier Fragen und bricht
bei der ersten ab, die mit Nein endet:

1. Ist das Repository-Secret `HF_TOKEN` in Actions vorhanden?
2. Wird der Token von Hugging Face akzeptiert (`whoami-v2`)?
3. Erreicht dieser Token das gated Modell aus `music/manifest.json`? Geprueft wird durch Abruf der
   kleinen `model_config.json`, nicht der mehrere Gigabyte grossen Gewichte.
4. Laeuft der vorhandene Generator aus `tools/music/generate_music.py` im `--dry-run`?

Weder der Token noch eine Antwort werden ausgegeben; der Workflow verwirft jeden Antwortkoerper.

**Wenn ein Schritt fehlschlaegt**, sagt die Stelle des Abbruchs, was zu tun ist:

| Fehlgeschlagener Schritt | Bedeutung | Naechster Schritt |
|---|---|---|
| Verify Hugging Face authentication, Meldung `HF_TOKEN is missing` | Secret fehlt | `HF_TOKEN` unter Settings -> Secrets and variables -> Actions anlegen |
| Verify Hugging Face authentication, HTTP 401 | Token ungueltig oder abgelaufen | Neuen Read-Token auf Hugging Face erzeugen und das Secret ersetzen |
| Verify gated Stable Audio model access, HTTP 401/403 | Token gueltig, aber der Account hat das gated Modell nicht freigeschaltet | Auf Hugging Face die Bedingungen fuer `stabilityai/stable-audio-3-small-music` akzeptieren |
| Verify gated Stable Audio model access, HTTP 404 | Zugang besteht, aber die Datei liegt nicht unter diesem Pfad | Dateinamen im Modell-Repository pruefen; Manifest oder Pfad anpassen |
| Confirm Itoeva music tooling dry-run | Manifest oder Prompt sind kaputt | `python tools/music/generate_music.py --track-id home-evening-01 --dry-run` lokal nachvollziehen |

Ein gruener Lauf heisst: Der naechste Klick auf **Generate Itoeva Music** scheitert nicht mehr am
Zugang.

## Lokal pruefen

Die Definition laesst sich ohne PyTorch, Modell-Download oder Kosten pruefen:

```bash
python tools/music/generate_music.py --track-id home-evening-01 --dry-run
```

Fuer eine echte lokale Generierung muss zuerst die offizielle `stable-audio-3`-Runtime installiert und der Hugging-Face-Zugang freigeschaltet sein. Das Manifest pinnt den bei Einrichtung verwendeten Upstream-Commit, damit spaetere Runs nachvollziehbar bleiben.

Beispiel nach installierter Runtime und `hf auth login`:

```bash
python tools/music/generate_music.py --track-id home-evening-01 --device auto
```

## Ablageformat: Ogg/Vorbis

`manifest.json` steht auf `output_format: "ogg"`, und `generate_music.py` schreibt es direkt.
Derselbe Referenztrack waere als WAV **15,88 MB** gross und ist so **1,06 MB** - Faktor 15, ohne
einen Kanal aufzugeben.

Nicht Opus, obwohl es hier kaum kleiner waere: `:app-sim` hat `minSdk = 26`, und Opus in `.ogg`
dekodiert erst ab Android 10. Nicht Mono: das Stereobild des Tracks ist echt (Kanalkorrelation
0,87), Mono naehme wirklich Breite weg.

Umgewandelt wird in der **Pipeline und nicht im Build**, damit das Gehoerte das Ausgelieferte ist.
Ein verlustfreies Archiv braucht es dafuer nicht - das Manifest pinnt Seed, Modell und
Runtime-Commit, eine Neuerzeugung liefert dasselbe.

Das setzt eine Ausnahme von einem Prinzip: `PlayChime.kt` begruendet, warum der Klang dieser App
gerechnet und nicht als Datei ausgeliefert wird. Die Ausnahme hat deshalb eine Grenze - **nur der
Score darf eine Datei sein; alles, was die Welt oder das Wesen selbst von sich gibt, bleibt
gerechnet.** Begruendung, Messwerte und die noch offenen Punkte: `EVOLUTION.md` zum 2026-09-05,
Arbeitspaket NT-055. **Wer das Format aendert, aendert es dort und nicht nur hier.**

## Im Spiel hoeren: Rollen, nicht Dateinamen

Der Schalter in den Einstellungen heisst **Musik im Spielmodus** und bedeutet *"Musik
grundsaetzlich verwenden"* - nicht *"diesen einen Track jetzt abspielen"*. Er ist beim
allerersten Start aus; danach gilt ausschliesslich die zuletzt gespeicherte Entscheidung. Das
Verlassen des Spielmodus haelt die Wiedergabe an, **aendert die Einstellung aber nicht**.

Welcher Track laeuft, entscheidet die Welt:

> Die Welt entscheidet, WAS passen wuerde. Der Nutzer entscheidet, OB ueberhaupt Musik laufen
> darf.

`MusicResolver` bekommt Tageszeit, Ort und die aktuelle Beschaeftigung und liefert eine kurze
Liste von **Rollen**, vom Spezifischsten zum Allgemeinsten. Genommen wird die erste, zu der ein
Track ausgeliefert ist - sonst bleibt es still. Keine vollstaendige Matrix aus Tageszeit mal Ort:
Die waere zu 90 % Wiederholung und muesste bei jedem neuen Ort viermal ergaenzt werden.

| Rolle | wofuer | Track |
|---|---|---|
| `main_day_background` | der normale Tag, musikalische Hauptidentitaet | `main-day-01` / Lantern Streets, `main-day-02` / Paper Bridges |
| `home_evening_background` | ruhiger Abend, Nacht und stille Naturorte | `home-evening-01` / Quiet Lanterns; `home-evening-02` / Late Windows definiert und im Manifest angelegt, aber erst nach einem Lauf von **Generate Itoeva Music** tatsaechlich als Datei da - bis dahin bleibt die Rolle bei genau einem Stueck |
| `morning_background` | frueher Morgen, falls er sich abheben soll | `morning-01` / First Light; `morning-02` / Paper Dawn definiert und im Manifest angelegt, noch nicht erzeugt |
| `sport_background` | Bewegung und Anstrengung | `sport-01` / Full Stride; `sport-02` / Second Wind definiert und im Manifest angelegt, noch nicht erzeugt |
| `dream_background` | Traum-Szenen | noch keiner - `MusicResolver.candidates()` setzt diese Rolle bislang auch noch gar nicht ein; ausser einem Track braucht es dort noch ein eigenes Signal "es wird gerade getraeumt" in `MusicContext` |
| `character_theme_background` | das persoenliche Stueck des anwesenden Wesens, Variante 01-06 fest je Spezies | `theme-puffling` / Puffling's Theme (Variante 01), `theme-starlet` / Starlet's Theme (Variante 02), `theme-wyrmling` / Wyrmling's Theme (Variante 03), `theme-fennec` / Fennec's Theme (Variante 04), `theme-gloop` / Gloop's Theme (Variante 05), `theme-hootlet` / Hootlet's Theme (Variante 06) |

Die Rolle `character_theme_background` ist die einzige, deren Variante **nicht** rotiert: 01 bis 06
gehoeren fest je einem Wesen (`MusicRole.characterThemeVariant`). Sie laeuft auch nicht dauernd,
sondern zu einem Anlass - beim ersten Erscheinen des Wesens im Spielmodus an einem Kalendertag,
ein Stueck lang (`PlayCharacterTheme`). Die sechs Stuecke sollen sich hoerbar voneinander
unterscheiden und eine **singbare** Melodie haben; Fernziel ist, sie je Charakter live zu singen.
Erzeugt werden sie trotzdem instrumental.

## Freigabe-Gate: Loop-Politur und Messung

**Kein Track geht ohne Messung durch.** `tools/music/audio_polish.py` poliert die Erzeugung und
prueft danach die fertige Datei; findet es etwas, scheitert der Lauf, statt eine unbrauchbare
Datei weiterzureichen.

Drei Maengel haben dazu gefuehrt, und alle drei kamen aus der Pipeline, nicht aus dem Modell:

1. **Ein Loch an der Loop-Grenze.** `main-day-01` endete mit 0,50 s Stille, `main-day-02` mit
   0,62 s, `home-evening-01` mit 1,13 s - und alle drei begannen sofort auf vollem Pegel. Die App
   loopt diese Dateien, also fiel die Musik alle 90 Sekunden fuer knapp eine Sekunde aus und setzte
   hart wieder ein.
2. **True Peak ueber 0 dBFS.** Der Erzeuger klemmte auf ±1,0 und gab das an einen
   **verlustbehafteten** Encoder. Vorbis rekonstruiert ueber seinen Eingang hinaus, gemessen
   +0,68 dBFS bei `main-day-01`. Das Klemmen war die Ursache; Kopfraum vor dem Encoder ist die
   Behebung.
3. **Keine Pruefung.** Nichts hat davon etwas gemessen. Beide Maengel erreichten das Telefon.

Die Politur schneidet die Raender, faltet den Schluss ueber den Anfang (damit das Ende IN den
Anfang fuehrt statt an ihn zu stossen) und setzt den Pegel auf −1 dBFS. Gemessen wird danach die
**dekodierte** Datei, nicht der Puffer im Speicher - der Ueberschwinger entsteht erst beim
Dekodieren.

Was das Gate ausdruecklich NICHT tut: bereits erzeugte Dateien anfassen. Erzeugte Binaerdateien
aendern sich nur ueber den Prozess, der sie gemacht hat - die drei vorhandenen Tracks werden also
neu erzeugt, nicht nachbearbeitet.

### Ein vierter Mangel, den keine Messung faengt

Gemeldet am 2026-09-20: `home-evening-01` klinge "wie eine verlangsamte Platte, die extra auf
langsam gezogen wird" - kuenstlich, nicht bloss langsam. Die drei obigen Kennzahlen (Pegel,
Randstille, Nahtsprung) waren dabei alle unauffaellig; das Gate haette diesen Track anstandslos
durchgelassen, und hat es auch.

Die Ursache lag nicht im Modell, sondern im **Prompt**: `home-evening-01.txt` verlangte explizit
"subtle tape and vinyl texture", und die drei parallelen Tages-/Sport-Prompts sogar woertlich
"gentle wow and flutter" - der Fachbegriff der Tontechnik fuer genau die Tonhoehen-Schwankung
einer schwankenden Bandgeschwindigkeit oder eines schleifenden Plattentellers. Bei einem
langsamen, sparsam besetzten Stueck mit langen gehaltenen Akkorden (wie dem Abendtrack) ist genau
diese Schwankung am wenigsten maskiert und am ehesten als "kuenstlich" statt "warm" hoerbar.

**Die Lehre:** "Warm" und "nostalgisch" sind erwuenschte Prompt-Eigenschaften; Tonhoehen- oder
Geschwindigkeitsinstabilitaet ist es nicht, auch wenn sie oft im selben Atemzug genannt wird
("tape and vinyl texture"). Ein Prompt darf um Bandrauschen, Vinylknistern oder analoge Waerme
bitten - **nie** um "wow and flutter" oder aehnliche Pitch-Wobble-Beschreibungen, und sollte bei
langsamen, ruhigen Stuecken ausdruecklich das Gegenteil verlangen ("every instrument stays
perfectly in tune and in time"). Diese Regel gehoert seitdem in jeden neuen Prompt dieser
Pipeline, nicht nur in die betroffenen vier.

Weil das Gate diesen Mangel strukturell nicht messen kann (er ist eine Eigenschaft des Klangs,
nicht des Pegels), bleibt die Vorbeugung im Prompt-Text die einzige Verteidigung - siehe
EVOLUTION.md zum 2026-09-20 fuer die vollstaendige Herleitung.

## Mehrere Stuecke je Rolle

**Eine Rolle ist kein Dateiname.** Sie kann von mehreren Stuecken erfuellt werden, und seit
`main-day-02` wird das auch genutzt: Bleibt die Lage stabil, wechselt die Musik nach drei bis
spaetestens fuenf Minuten innerhalb derselben Rolle auf ein anderes Stueck - ueber dieselbe
viersekuendige Ueberblendung wie ein Rollenwechsel.

Der Grund steht in der Meldung, die dazu gefuehrt hat: "Innerhalb eines stabilen Zustands bleibt
dieselbe Musik zu lange unveraendert. Nach ungefaehr fuenf Minuten wirkt ein einzelner
wiederholter Track bereits monoton." Die Rolle war dabei richtig - es fehlte die Moeglichkeit,
sie mit mehr als einem Stueck zu erfuellen.

Die Dateien einer Rolle heissen `<stamm>_01`, `<stamm>_02` und so weiter; die App sucht sie zur
Laufzeit ueber diesen Namen (`MusicRole.variantResource`, hoechstens `MAX_VARIANTS`). Ein neuer
Track wird damit gehoert, sobald er gemergt ist, ohne dass an einer zweiten Stelle etwas
nachzuziehen waere. Eine Luecke ist zugelassen: Fehlt die `02`, wird die `03` trotzdem gefunden.

**Was ausdruecklich NICHT passiert:** Ein Ortswechsel oder ein Taetigkeitswechsel innerhalb
derselben Rolle startet die Musik nicht neu. Und die Uhr fuer den Variantenwechsel gehoert der
ROLLE, nicht der Variante - sonst wuerde aus "spaetestens nach fuenf Minuten" ein "alle fuenf
Minuten wieder von vorn", und das waere als Metronom hoerbar.

**Der heutige Stand deckt fuenf der sechs Rollen ab:** Frueh morgens laeuft jetzt `morning-01` /
First Light, mittags `main_day_background` - dort inzwischen mit zwei Stuecken, zwischen denen bei
laengerem Zusehen gewechselt wird. Abends uebernimmt an ruhigen Orten `home_evening_background`;
Stadt, Strasse, Laden und Arbeitsplatz lassen den Tag noch ausklingen. Park, Wald, Wiese und Teich
gelten abends als ruhige Naturorte. Nachts wird nie auf den Tagestrack zurueckgefallen.

`sport-01` / Full Stride uebernimmt nur, wenn die Figur am Sportplatz **tatsaechlich MOVE
ausfuehrt**. Der Ort allein behauptet keine Handlung.

`dream_background` bleibt die letzte offene Rolle. Der Enum-Eintrag existiert bewusst schon
(`MusicRole.DREAM` in `PlayMusicPlan.kt`), aber `MusicResolver.candidates()` fragt ihn noch nirgends
ab - ein Traum-Track allein wuerde also noch nicht erklingen. Wetter und Stimmung bleiben ebenfalls
vorhandene, aber musikalisch noch ungenutzte Weltsignale, bis ein gepruefter passender Track UND die
zugehoerige Abfrage in `candidates()` beide stehen.

Das Feld `role` ist deshalb Pflicht im Manifest und wird gegen dieselbe Liste geprueft, die
`MusicRole` in `app-sim/.../matrix/PlayMusicPlan.kt` fuehrt. **Wer eine Rolle ergaenzt, ergaenzt
sie an beiden Stellen** - `generate_music.py` laesst einen unbekannten Namen sonst gar nicht erst
durch, was der Sinn der Sache ist.

Ein laufender Track wird bei einem Ortswechsel **nicht** neu gestartet, solange er nach wie vor
die richtige Rolle ist. Ein echter Rollenwechsel blendet den bisherigen und den neuen Player
vier Sekunden lang mit einer Equal-Power-Kurve ineinander. So entsteht weder ein harter Schnitt
noch das Lautstaerke-Loch einer linearen Ueberblendung.

Solange kein Track fuer eine Rolle gemergt ist, sucht die App ihn vergeblich und bleibt still -
sie kommt ohne ihn aus. Damit der Ressourcen-Schrumpfer die vorhandenen Dateien im Release nicht
als unbenutzt entfernt, haelt `app-sim/src/main/res/raw/keep.xml` sie fest.

## Rechte und Herkunft

Zu jedem erzeugten Track wird eine JSON-Metadatendatei mit Modell, Upstream-Commit, Prompt-Hash, Seed, Zeitpunkt und Track-ID abgelegt. Das ist keine Rechtsgarantie, aber eine nachvollziehbare Provenienzspur fuer spaetere Releases.

Stable Audio 3 steht unter der Stability AI Community License und enthaelt zudem Komponenten unter den Gemma Terms. Vor einer kommerziellen Veroeffentlichung muessen die dann aktuellen Bedingungen erneut geprueft werden. Prompts sollen weiterhin konkrete Kuenstler, Songs oder Aufforderungen zur engen Imitation vermeiden.

**Diese Regel gilt auch fuer eine gut gemeinte Stilempfehlung.** Am 2026-09-21 wurde als Inspiration fuer den melancholischen Sound ein konkreter, noch aktiv verwalteter japanischer Produzentenname vorgeschlagen, dazu "japanische Anime-Klassiker" allgemein. Beides blieb bewusst aussen vor: Ein Name im Prompt ist genau die Art von "enger Imitation", die die Regel oben ausschliesst, unabhaengig davon, wie gut die Absicht ist. Was stattdessen in die Prompts einfliesst, sind Eigenschaften, die diesen Klang ausmachen - warmer, japanisch gefaerbter Boom-Bap, Rhodes/Klavier mit pentatonischer Farbe, Bandrauschen, eine klare singbare Melodie -, wie es diese Datei von Anfang an schon vorsieht. Die stilistische Absicht laesst sich so treffen, ohne einen Namen zu nennen.
