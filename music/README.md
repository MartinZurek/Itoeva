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

## Rechte und Herkunft

Zu jedem erzeugten Track wird eine JSON-Metadatendatei mit Modell, Upstream-Commit, Prompt-Hash, Seed, Zeitpunkt und Track-ID abgelegt. Das ist keine Rechtsgarantie, aber eine nachvollziehbare Provenienzspur fuer spaetere Releases.

Stable Audio 3 steht unter der Stability AI Community License und enthaelt zudem Komponenten unter den Gemma Terms. Vor einer kommerziellen Veroeffentlichung muessen die dann aktuellen Bedingungen erneut geprueft werden. Prompts sollen weiterhin konkrete Kuenstler, Songs oder Aufforderungen zur engen Imitation vermeiden.
