# Itoeva Music

Itoevas Musik soll sich mit Welt und Charakter entwickeln, aber nicht bei jedem App-Start live von einem Modell erzeugt werden. Generierte Stuecke werden deshalb wie andere Spielinhalte versioniert: Prompt -> Open-Weights-Modell -> pruefbare Audiodatei -> Pull Request -> App-Asset.

## Erster Track

`home-evening-01` / **Quiet Lanterns** ist die erste klangliche Referenz fuer ruhige Home- und Abendszenen. Der Prompt beschreibt Eigenschaften und Instrumente, nicht den Stil eines konkreten lebenden oder verstorbenen Kuenstlers.

Definitionen liegen in:

- `music/manifest.json` - Track-ID, Modell, Dauer, Seed und Android-Ressourcenname.
- `music/prompts/` - versionierte Textprompts.
- `tools/music/generate_music.py` - reproduzierbarer lokaler Stable-Audio-3-Aufruf.
- `.github/workflows/generate-music.yml` - manueller Open-Weights-Lauf auf einem GitHub-Runner.

## Warum Small Music statt der Stability API

Die Pipeline nutzt jetzt `Stable Audio 3 Small Music` als Open-Weights-Modell. Fuer jede Generierung fallen dadurch **keine Stability-API-Credits** an. `small-music` ist fuer Musik, CPU-Inferenz und bis zu 120 Sekunden ausgelegt; das passt zum 90-Sekunden-Referenztrack.

`Stable Audio 3 Medium` bleibt als spaetere Qualitaetsoption denkbar, braucht aber CUDA/Flash Attention und ist deshalb nicht der Standard fuer den normalen GitHub-Runner.

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
5. speichert WAV plus Metadaten als GitHub-Artifact,
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

## Offen: Ablageformat und Groesse

`manifest.json` legt heute `output_format: "wav"` fest. Der erste erzeugte Track ist damit
**15,88 MB** gross - dieselben 90 Sekunden waeren als Vorbis 44,1 kHz stereo **1,06 MB**.

Das ist noch nicht entschieden und soll hier auch nicht nebenbei entschieden werden, denn es
haengt an einer Grundsatzfrage: `PlayChime.kt` begruendet ausdruecklich, warum der Klang dieser
App gerechnet und nicht als Datei ausgeliefert wird. Musik als Asset ist davon eine Ausnahme, und
die gehoert benannt.

Messwerte, Randbedingungen (`minSdk = 26` schliesst Opus in `.ogg` aus) und die vier offenen
Fragen stehen in `EVOLUTION.md` zum 2026-09-05; das Arbeitspaket ist NT-055 in `NextTasks.md`.
**Wer das Format aendert, aendert es dort und nicht nur hier.**

## Rechte und Herkunft

Zu jedem erzeugten Track wird eine JSON-Metadatendatei mit Modell, Upstream-Commit, Prompt-Hash, Seed, Zeitpunkt und Track-ID abgelegt. Das ist keine Rechtsgarantie, aber eine nachvollziehbare Provenienzspur fuer spaetere Releases.

Stable Audio 3 steht unter der Stability AI Community License und enthaelt zudem Komponenten unter den Gemma Terms. Vor einer kommerziellen Veroeffentlichung muessen die dann aktuellen Bedingungen erneut geprueft werden. Prompts sollen weiterhin konkrete Kuenstler, Songs oder Aufforderungen zur engen Imitation vermeiden.
