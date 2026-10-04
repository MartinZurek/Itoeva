"""Rendert alle gemalten Orte nach app-sim/src/game/assets/scenes/ und schreibt den Kotlin-Katalog
GameSceneCatalog.kt aus den Spielangaben der Szenen-Skripte (places/*.py) - so koennen Bild und
Spiel nicht auseinanderlaufen. Aufruf: python3 build_all.py [ort ...]"""
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(ROOT, 'app-sim/src/game/assets/scenes')
META = os.path.join(HERE, 'places', 'meta.json')
CATALOG = os.path.join(ROOT, 'app-sim/src/main/java/com/notime/glyphsim/matrix/GameSceneCatalog.kt')


def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, 'places', name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def f(v):
    return f'{float(v):g}f'


def kotlin(metas):
    lines = [
        '// Generiert von tools/world-art/build_all.py aus den Szenen-Skripten - nicht von Hand aendern.',
        'package com.notime.glyphsim.matrix',
        '',
        'import com.notime.glyphsim.matrix.GameScenes.Box',
        'import com.notime.glyphsim.matrix.GameScenes.Scene',
        'import com.notime.glyphsim.matrix.GameScenes.Spot',
        'import com.notime.glyphsim.matrix.PlayControl.Dir',
        'import com.notime.glyphsim.matrix.PlayScene.Place',
        'import com.notime.glyphsim.matrix.PlayScene.Station',
        '',
        '/** Die gemalten Orte (siehe [GameScenes]) - Gehflaeche, Plaetze und Raender je Bild. */',
        'internal object GameSceneCatalog {',
        '    val ALL: List<Scene> = listOf(',
    ]
    for place in sorted(metas):
        m = metas[place]
        w = m['walk']
        P = place.upper()
        lines.append('        Scene(')
        lines.append(f'            place = Place.{P},')
        lines.append(f'            asset = "scenes/{place}.png",')
        lines.append(f'            farY = {f(w["farY"])}, nearY = {f(w["nearY"])},')
        lines.append(f'            farLeft = {f(w["farLeft"])}, farRight = {f(w["farRight"])}, '
                     f'nearLeft = {f(w["nearLeft"])}, nearRight = {f(w["nearRight"])},')
        lines.append(f'            farHeight = {f(w["farHeight"])}, nearHeight = {f(w["nearHeight"])},')
        spots = ',\n'.join(
            f'                Spot(Station.{sp["station"]}, Box({", ".join(f(v) for v in sp["box"])}), '
            f'{f(sp["standX"])}, {f(sp["standY"])})' for sp in m['spots'])
        lines.append('            spots = listOf(' + ('\n' + spots + '\n            ' if spots else '') + '),')
        blocked = ', '.join(f'Dir.{d} to null' for d in m.get('blocked', []))
        lines.append(f'            exits = mapOf({blocked}),')
        if any(sp['station'] == 'DOOR' for sp in m['spots']):
            lines.append(f'            door = PlayControl.doorTarget(Place.{P}),')
        lines.append(f'            cropTop = {f(m.get("cropTop", 0.8))}')
        lines.append('        ),')
    lines += ['    )', '}', '']
    return '\n'.join(lines)


def main(names):
    metas = json.load(open(META)) if os.path.exists(META) else {}
    os.makedirs(ASSETS, exist_ok=True)
    todo = names or sorted(n[:-3] for n in os.listdir(os.path.join(HERE, 'places'))
                           if n.endswith('.py') and not n.startswith('_'))
    for name in todo:
        print('rendere', name)
        metas[name] = load(name).build(os.path.join(ASSETS, name + '.png'))
    json.dump(metas, open(META, 'w'), indent=1, sort_keys=True)
    open(CATALOG, 'w').write(kotlin(metas))
    print(f'{len(metas)} Orte im Katalog.')


if __name__ == '__main__':
    main(sys.argv[1:])
