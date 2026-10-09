"""Raumkoerper/Avatar-Massstab aus Kotlin-Export, Softwarevorschau, keine APK-Aufnahme."""
from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def shade(color, level):
    gain = {-2: .63, -1: .81, 1: 1.12, 2: 1.20, 3: 1.38}.get(level, 1.)
    return tuple(min(255, round(c * gain)) for c in color)


def room(data, rear):
    wall, side, floor, trim, accent = map(rgb, data['palette'])
    image = Image.new('RGBA', (480, 270), side)
    draw = ImageDraw.Draw(image)
    draw.polygon([(48,116),(432,116),(468,270),(12,270)],fill=floor)
    for row in range(18):
        y = 116+154*(row/17)**2
        end = 116+154*((row+1)/17)**2
        draw.rectangle((12,y,468,end), fill=shade(floor,-1 if row%3==0 else 0))
        draw.line((12,y,468,y),fill=shade(floor,-1))
        for x in range(-1,9):
            px=x*70+(35 if row%2 else 0)
            back=240+(px-240)*(.78+row/17*.22)
            draw.line((back,y,px,end),fill=shade(floor,-1))
    draw.polygon([(0,12),(48,28),(48,116),(12,270),(0,270)],fill=side,outline=trim)
    draw.polygon([(432,28),(480,12),(480,270),(468,270),(432,116)],fill=shade(side,-1),outline=trim)
    draw.rectangle((48,28,432,116),fill=wall,outline=trim)
    draw.rectangle((48,109,432,116),fill=trim)
    draw.rectangle((80,44,149,92),fill=trim)
    draw.rectangle((84,48,145,88),fill=(173,199,188))
    draw.rectangle((111,48,114,88),fill=trim)
    draw.rectangle((84,66,145,69),fill=trim)
    draw.rectangle((75,91,154,95),fill=shade(trim,1))
    for x in (76,146):
        for row in range(12):
            draw.rectangle((x,45+row*4,x+6,49+row*4),fill=shade(accent,-1 if row%3==0 else 0))
    draw.rectangle((184,65,253,69),fill=trim)
    for i in range(7):
        draw.rectangle((190+i*8,51+(i%3)*2,195+i*8,65),fill=shade(accent,i%3-1))
    materials={'UPHOLSTERY':accent,'LINEN':(155,166,184),'METAL':(171,191,181),'TILE':(185,206,191),'WOOD':(156,118,83)}
    for body in data['bodies']:
        for face in body['faces']:
            draw.polygon([tuple(p) for p in face['points']],fill=shade(materials[body['material']],face['shade']),outline=trim)
    avatar=data['avatars'][0 if rear else 1]
    sheet=Image.open(ROOT/'app-sim/src/main/assets/creatures/fennec.png').convert('RGBA')
    sprite=sheet.crop((104*128,0,105*128,128))
    size=round(avatar['height']*128/(126-17))
    sprite=sprite.resize((size,size),Image.Resampling.BILINEAR)
    x=round(avatar['x']-size/2);y=round(avatar['y']-size*126/128)
    layer=Image.new('RGBA',image.size)
    layer.alpha_composite(sprite,(x,y))
    if rear:
        # Dieselben sichtbaren Flaechen verdecken die Figur, kein Moebel-Bildrechteck.
        mask=Image.new('L',image.size,255)
        md=ImageDraw.Draw(mask)
        for body in data['bodies']:
            if avatar['y'] < body['ground']+3:
                for face in body['faces']:
                    md.polygon([tuple(p) for p in face['points']],fill=0)
        from PIL import ImageChops
        layer.putalpha(ImageChops.multiply(layer.getchannel('A'),mask))
    image.alpha_composite(layer)
    return image


def preview(folder, target):
    canvas=Image.new('RGB',(1440,612),(36,49,45))
    draw=ImageDraw.Draw(canvas)
    for i,name in enumerate(('living','bedroom','cafe')):
        data=json.loads((folder/f'{name}.json').read_text())
        for row,rear in enumerate((True,False)):
            canvas.paste(room(data,rear).convert('RGB'),(i*480,32+row*292))
            draw.text((i*480+12,12+row*292),f'{name} | '+('hinter den Moebeln' if rear else 'vorderer Boden'),fill=(246,229,207))
    draw.text((12,598),'Produktive Kotlin-Raumkoerper; Softwarevorschau von Tiefe, Verdeckung und Massstab. Keine APK-Aufnahme.',fill=(174,191,181))
    canvas.save(target)


if __name__ == '__main__':
    preview(Path(sys.argv[1]),Path(sys.argv[2]))
