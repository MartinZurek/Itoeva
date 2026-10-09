"""Raumkoerper/Avatar-Massstab aus Kotlin-Export, Softwarevorschau, keine APK-Aufnahme."""
from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def paint_art(image, marks):
    """Rastert die produktive Pixelliste; keine von der App getrennte Raumgestaltung."""
    from PIL import ImageChops
    import math
    for mark in marks:
        points = mark['points']
        color = (*rgb(mark['color']), (mark['color'] >> 24) & 255)
        if not mark['clip'] and color[3] == 255:
            draw = ImageDraw.Draw(image)
            if mark['oval']:
                draw.ellipse(tuple(v for p in points for v in p), fill=color)
            else:
                draw.polygon([tuple(p) for p in points], fill=color)
            continue
        x0 = math.floor(min(p[0] for p in points))
        y0 = math.floor(min(p[1] for p in points))
        x1 = math.ceil(max(p[0] for p in points)) + 1
        y1 = math.ceil(max(p[1] for p in points)) + 1
        patch = Image.new('RGBA', (max(1,x1-x0),max(1,y1-y0)))
        local = [(x-x0,y-y0) for x,y in points]
        draw = ImageDraw.Draw(patch)
        if mark['oval']:
            draw.ellipse(tuple(v for p in local for v in p),fill=color)
        else:
            draw.polygon(local,fill=color)
        if mark['clip']:
            mask = Image.new('L',patch.size)
            ImageDraw.Draw(mask).polygon([(x-x0,y-y0) for x,y in mark['clip']],fill=255)
            patch.putalpha(ImageChops.multiply(patch.getchannel('A'),mask))
        image.alpha_composite(patch,(x0,y0))


def room(data, rear):
    image = Image.new('RGBA',(480,270))
    paint_art(image,data['art'])
    draw = ImageDraw.Draw(image)
    for x in (76,146):
        for row in range(11):
            draw.rectangle((x,45+row*4,x+5,48+row*4),fill=(195,173,135) if row%3==0 else (216,195,156))
            draw.rectangle((x+1,45+row*4,x+1,48+row*4),fill=(235,220,187))
    for door in data.get('doorArt',[]):
        x0,y0,x1,y1=door['box']
        draw.rectangle((x0-1,y0-1,x1+1,y1+1),fill=(98,73,54))
        leaf=Image.new('RGBA',(round(x1-x0),round(y1-y0)))
        paint_art(leaf,door['marks'])
        image.alpha_composite(leaf,(round(x0),round(y0)))
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
            if avatar['y'] < body['front']:
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
    before=ROOT/'tools/character-art/room-space-preview.png'
    if before.exists():
        previous=Image.open(before).convert('RGB')
        comparison=Image.new('RGB',(1440,624),(36,49,45))
        cd=ImageDraw.Draw(comparison)
        cd.text((12,10),'Vorher | Raeumliche Grundkoerper',fill=(246,229,207))
        comparison.paste(previous.crop((0,324,1440,594)),(0,32))
        cd.text((12,318),'Jetzt | Konzeptfarben, Holz, Bogenfenster, Polster, Textilien und Pflanzen',fill=(246,229,207))
        comparison.paste(canvas.crop((0,324,1440,594)),(0,342))
        cd.text((12,612),'Softwarevorschau aus produktiver Geometrie. Keine APK-Aufnahme; Tageslicht/Atmosphaere nicht enthalten.',fill=(174,191,181))
        comparison.save(target.with_name('room-concept-comparison.png'))
    all_rooms=sorted(folder.glob('*.json'))
    atlas=Image.new('RGB',(1440,32+302*((len(all_rooms)+2)//3)),(36,49,45))
    draw=ImageDraw.Draw(atlas)
    draw.text((12,10),'Alle elf begehbaren Innenorte | produktive Pixelgruppen, Softwarevorschau, keine APK-Aufnahme',fill=(246,229,207))
    for i,source in enumerate(all_rooms):
        data=json.loads(source.read_text())
        x=i%3*480;y=32+i//3*302
        draw.text((x+12,y),source.stem,fill=(246,229,207))
        atlas.paste(room(data,False).convert('RGB'),(x,y+20))
    atlas.save(target.with_name('room-concept-all-preview.png'))


if __name__ == '__main__':
    preview(Path(sys.argv[1]),Path(sys.argv[2]))
