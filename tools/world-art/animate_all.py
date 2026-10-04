"""Erzeugt die Bewegungsstreifen (<ort>_anim.png) fuer alle vorhandenen Ortsbilder, ohne die Orte
neu zu malen. build_all.py macht dasselbe nach jedem Rendern."""
import os
import sys
import animate
from build_all import ANIM, ASSETS

names = sys.argv[1:] or sorted(f[:-4] for f in os.listdir(ASSETS) if f.endswith('.png') and not f.endswith('_anim.png') and not f.endswith('_glow.png') and f != 'hero.png')
for n in names:
    animate.animate_file(os.path.join(ASSETS, n + '.png'), os.path.join(ASSETS, n + '_anim.png'), **ANIM.get(n, {}))
    animate.glow_file(os.path.join(ASSETS, n + '.png'), os.path.join(ASSETS, n + '_glow.png'), **ANIM.get(n, {}))
    print('bewegt', n)
