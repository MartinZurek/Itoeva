"""Vorher/Nachher mit gleicher Weltgroesse, ohne Aufnahme einer APK."""
import argparse
import subprocess
from io import BytesIO
from pathlib import Path
from PIL import Image, ImageDraw
import ensemble_motion as E
import refined_motion as R

OLD_SCALE = dict(gloop=.92, puffling=.97, starlet=.90, hootlet=1.05, fennec=.82)


def preview(output, old_ref):
    names = (*R.NAMES, 'fennec')
    before = {name: Image.open(BytesIO(subprocess.check_output([
        'git', 'show', f'{old_ref}:app-sim/src/main/assets/creatures/{name}.png'
    ], cwd=E.HERE.parent.parent))).convert('RGBA') for name in names}
    after = {name: Image.open(E.ASSETS/f'{name}.png').convert('RGBA') for name in names}
    sequences = [[95]*4+[92]*4+[93]*12+[94]*4+[95]*8,
                 [104]*4+[100]*4+[103]*12+[100]*4+[104]*8,
                 [111]*4+[107]*4+[110]*12+[107]*4+[111]*8,
                 [9+i%8 for i in range(32)]]
    titles = ['Profil: sitzen / aufstehen', 'Vorn: sitzen / aufstehen',
              'Hinten: sitzen / aufstehen', 'Gehen: acht gezeichnete Phasen']
    rendered = []
    for tick in range(128):
        phase, step = divmod(tick, 32)
        bg = Image.new('RGBA', (520, 955), (49, 65, 57, 255))
        d = ImageDraw.Draw(bg)
        d.text((12, 9), titles[phase], fill=(241, 226, 199))
        d.text((12, 27), 'Vorher', fill=(241, 226, 199))
        d.text((272, 27), 'Ueberarbeitet', fill=(241, 226, 199))
        for row, name in enumerate(names):
            top = 49+row*178
            ground = top+163
            d.text((12, top), name.capitalize(), fill=(241, 226, 199))
            index = sequences[phase][step]
            for col, sheet in enumerate((before[name], after[name])):
                scale = (OLD_SCALE[name] if col == 0 else E.WORLD_SCALE.get(name, .82))*1.5
                size = round(128*scale)
                sprite = sheet.crop((128*index, 0, 128*(index+1), 128))
                sprite = sprite.resize((size, size), Image.Resampling.NEAREST)
                bg.alpha_composite(sprite, (col*260+(260-size)//2, ground-round(126*scale)))
                d.line((col*260+12, ground, (col+1)*260-12, ground), fill=(101, 121, 106))
        d.text((12, 939), 'Spritevergleich in gleicher Weltgroesse, 1.5x Ansicht', fill=(199, 204, 187))
        rendered.append(bg.convert('RGB'))
    rendered[0].save(output, save_all=True, append_images=rendered[1:], duration=110, loop=0)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('output', type=Path)
    parser.add_argument('--old-ref', default='3a85d370adc74a54f57e196b76a57a3a6cc11ad8')
    args = parser.parse_args()
    preview(args.output, args.old_ref)
