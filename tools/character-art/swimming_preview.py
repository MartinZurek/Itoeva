"""Sichtpruefung der gezeichneten Posen an produktiver Kotlin-Wasserlinie; keine APK."""
from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw, ImageChops

ROOT=Path(__file__).resolve().parents[2]
NAMES=('fennec','puffling','wyrmling','gloop','starlet','hootlet')


def preview(folder,target):
    coast=Image.open(ROOT/'app-sim/src/game/assets/world/coast.png').convert('RGBA').resize((1920,640))
    mask=Image.new('L',coast.size)
    md=ImageDraw.Draw(mask)
    for left,right,y in json.loads((folder/'mask.json').read_text()): md.rectangle((left,y,right-1,y+3),fill=255)
    sheets={n:Image.open(ROOT/f'app-sim/src/main/assets/creatures/{n}-swim.png').convert('RGBA') for n in NAMES}
    frames=[]
    for tick in range(60):
        data=json.loads((folder/f'{tick}.json').read_text())
        water=coast.copy();marks=Image.new('RGBA',coast.size);wd=ImageDraw.Draw(marks)
        for x,y,w,light in data['waves']:
            wd.rectangle((x+2,y+2,x+w+3,y+3),fill=(21,61,82,round(light*.6*255)))
            wd.rectangle((x,y,x+w-1,y+1),fill=(195,231,220,round(light*255)))
            wd.line((x+2,y,x+max(2,w*.4),y),fill=(238,244,198,round(light*.7*255)))
        marks.putalpha(ImageChops.multiply(marks.getchannel('A'),mask));water.alpha_composite(marks)
        canvas=Image.new('RGBA',(1440,632),(36,49,45,255));d=ImageDraw.Draw(canvas)
        d.text((12,12),f'Gezeichnete Schwimmzuege | {data["direction"]} | identischer Massstab / Wasserlinie wie Kotlin',fill=(246,229,207))
        byname={r['name']:r for r in data['characters']}
        for i,name in enumerate(NAMES):
            panel=water.crop((600,355,1080,635));row=byname[name]
            drawn=row['drawn'];water_y=220;feet_y=water_y+row['rise']
            source=sheets[name].crop((row['frame']*256,0,(row['frame']+1)*256,256))
            size=round(drawn*2);sprite=source.resize((size,size),Image.Resampling.BILINEAR)
            x=round(240-size/2);y=round(feet_y-drawn*126/128-drawn/2-row['lift']*drawn/128)
            layer=Image.new('RGBA',panel.size);layer.alpha_composite(sprite,(x,y))
            import numpy as np
            rgba=np.asarray(layer).copy();rgba[water_y:,:,0]=(rgba[water_y:,:,0]*.55).astype('uint8');rgba[water_y:,:,1]=(rgba[water_y:,:,1]*.82).astype('uint8')
            rgba[water_y:,:,2]=np.minimum(rgba[water_y:,:,2].astype('int')+12,255).astype('uint8');rgba[water_y:,:,3]=(rgba[water_y:,:,3]*.26).astype('uint8');layer=Image.fromarray(rgba,'RGBA')
            panel.alpha_composite(layer)
            pd=ImageDraw.Draw(panel);pd.arc((210,water_y-3,270,water_y+7),0,170,fill=(218,239,227),width=1)
            px=i%3*480;py=40+i//3*292;canvas.alpha_composite(panel,(px,py));d.text((px+12,py+12),name.capitalize(),fill=(255,242,215))
        d.text((12,618),'Software-Sichtpruefung: gezeichnete Assets, Kotlin-Phasen, Pixelwellen und Wasserverdeckung. Keine Telefonaufnahme.',fill=(188,205,192))
        frames.append(canvas.convert('RGB'))
    small=[f.resize((960,422),Image.Resampling.LANCZOS) for f in frames]
    palette=small[24].quantize(colors=192)
    animated=[f.quantize(palette=palette,dither=Image.Dither.NONE) for f in small]
    animated[0].save(target,save_all=True,append_images=animated[1:],duration=45,loop=0,optimize=True)
    frames[24].save(target.with_suffix('.jpg'),quality=93)


if __name__=='__main__': preview(Path(sys.argv[1]),Path(sys.argv[2]))
