"""Gleiche Weltgroesse: alter und neuer Wyrmling in den echten Bildfolgen.

Keine APK-Aufnahme. Der optionale alte Bogen wird aus Git vor dem Umbau
ausgelesen; das Skript veraendert keine Assets.
"""
import argparse
import subprocess
from io import BytesIO
from pathlib import Path
from PIL import Image, ImageDraw
import ensemble_motion as E


def preview(output, old_ref):
    root = E.HERE.parent.parent
    before = Image.open(BytesIO(subprocess.check_output([
        'git', 'show', f'{old_ref}:app-sim/src/main/assets/creatures/wyrmling.png'
    ], cwd=root))).convert('RGBA')
    after = Image.open(E.ASSETS/'wyrmling.png').convert('RGBA')
    clips = [[95]*4+[92]*4+[93]*12+[94]*4+[95]*8,
             [104]*4+[100]*4+[103]*12+[100]*4+[104]*8,
             [111]*4+[107]*4+[110]*12+[107]*4+[111]*8,
             [9+i%8 for i in range(32)]]
    labels = ['Profil: hinsetzen / aufstehen', 'Vorn: hinsetzen / aufstehen',
              'Hinten: hinsetzen / aufstehen', 'Gang mit gebeugten Gliedern']
    frames = []
    for tick in range(32):
        bg = Image.new('RGBA', (640, 620), (49, 65, 57, 255))
        draw = ImageDraw.Draw(bg)
        draw.text((24, 10), 'Vorher', fill=(231, 216, 185))
        draw.text((344, 10), 'Ueberarbeitet', fill=(231, 216, 185))
        for row, clip in enumerate(clips):
            top = 35+row*140
            draw.text((12, top), labels[row], fill=(231, 216, 185))
            ground = top+125
            for col, (sheet, scale) in enumerate(((before, 1.71), (after, E.WORLD_SCALE['wyrmling']))):
                index = clip[tick]
                size = round(128*scale)
                sprite = sheet.crop((128*index, 0, 128*(index+1), 128))
                sprite = sprite.resize((size, size), Image.Resampling.NEAREST)
                bg.alpha_composite(sprite, (col*320+(320-size)//2, ground-round(126*scale)))
                draw.line((col*320+12, ground, (col+1)*320-12, ground), fill=(103, 122, 104))
        draw.text((12, 606), 'Gerenderte Spritefolgen, keine APK-Aufnahme', fill=(192, 197, 180))
        frames.append(bg.convert('RGB'))
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=110, loop=0)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('output', type=Path)
    parser.add_argument('--old-ref', default='79cd92e7f5a0702a11ecc5908c6f16c19a9fe7f4')
    args = parser.parse_args()
    preview(args.output, args.old_ref)
