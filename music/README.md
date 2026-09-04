# Itoeva Music

Itoevas Musik soll sich mit Welt und Charakter entwickeln, aber nicht bei jedem App-Start live von einem Modell erzeugt werden. Generierte Stuecke werden deshalb wie andere Spielinhalte versioniert: Prompt -> API -> pruefbare Audiodatei -> Pull Request -> App-Asset.

## Erster Track

`home-evening-01` / **Quiet Lanterns** ist die erste klangliche Referenz fuer ruhige Home- und Abendszenen. Der Prompt beschreibt Eigenschaften und Instrumente, nicht den Stil eines konkreten lebenden oder verstorbenen Kuenstlers.

Definitionen liegen in:

- `music/manifest.json` - Track-ID, Modell, Dauer und Android-Ressourcenname.
- `music/prompts/` - versionierte Textprompts.
- `tools/music/generate_music.py` - reproduzierbarer Stability-API-Aufruf.
- `.github/workflows/generate-music.yml` - manueller, kostenpflichtiger Generator.

## Secret einmalig hinterlegen

In GitHub unter **Settings -> Secrets and variables -> Actions -> New repository secret** ein Secret namens

`STABILITY_API_KEY`

anlegen. Den Key niemals in Issues, Prompts, Logs oder das Repository kopieren.

## Generieren

GitHub Actions -> **Generate Itoeva Music** -> Run workflow.

Standardmaessig wird `home-evening-01` erzeugt. Ein erfolgreicher Lauf:

1. validiert Manifest und Prompt ohne API-Aufruf,
2. erzeugt den Track mit Stable Audio,
3. speichert Audio plus Metadaten als GitHub-Artifact,
4. legt bei aktiviertem `create_pr` einen neuen Branch an,
5. kopiert das Audio nach `app-sim/src/main/res/raw/`,
6. oeffnet einen PR gegen `main`.

So landet kein ungehoerter Modelloutput automatisch in der App. Erst der Merge des generierten PR macht ihn zum versionierten Spiel-Asset.

## Lokal pruefen

Ohne Kosten/API-Aufruf:

```bash
python tools/music/generate_music.py --track-id home-evening-01 --dry-run
```

Mit lokal gesetztem `STABILITY_API_KEY` kann derselbe Befehl ohne `--dry-run` verwendet werden. Das Secret selbst darf nicht in Shell-History, Dateien oder Commits landen.

## Rechte und Herkunft

Zu jedem erzeugten Track wird eine JSON-Metadatendatei mit Modell, Prompt-Hash, Zeitpunkt und Track-ID abgelegt. Das ist keine Rechtsgarantie, aber eine nachvollziehbare Provenienzspur fuer spaetere Releases.

Vor kommerzieller Veroeffentlichung gelten immer die dann aktuellen Bedingungen des verwendeten Providers sowie das allgemeine Gebot, keine Rechte Dritter zu verletzen. Prompts sollen deshalb konkrete Kuenstler, Songs oder Aufforderungen zur engen Imitation vermeiden.
