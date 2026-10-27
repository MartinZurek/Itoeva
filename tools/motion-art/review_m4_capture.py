"""Echte M4-Spielbilder ohne Retiming auswerten und als Detailfilm zeigen."""
from pathlib import Path
import argparse,json,hashlib,statistics,subprocess
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[2];PROJECT=ROOT/'prototypes/godot-hd2d'
LABELS={'mosslyn':'Mooslyn','nocti':'Nocti','starlet':'Starlight','wyrmling':'Wyrmling'}
NAMES={'rest':'Ruhe','walk':'Gehen','run':'Laufen','lower':'Absenken','rise':'Aufrichten','takeoff':'Abheben','land':'Landen'}
def review(capture,out):
 out.mkdir(parents=True,exist_ok=True);r=json.loads((capture/'telemetry.json').read_text());sp=r['species'];frames=r['frames'];fps=r['fps'];name='mosslyn_m4_cycles.json' if sp=='mosslyn' else sp+'_authored_cycles.json';cycles=json.loads((PROJECT/'data'/name).read_text())
 assert r['motion_m4']=='1';source_size=Image.open(capture/'0000.png').size;assert list(source_size)==r['capture_size']
 coverage={};scales={};hashes={}
 for mode,spec in cycles.items():
  p=PROJECT/spec['asset'].removeprefix('res://');assert hashlib.sha256(p.read_bytes()).hexdigest()==spec['sha256'];hashes[mode]={'original_sha256':spec['sha256'],'pixel_sha256':hashlib.sha256((p.parent/'pixel'/p.name).read_bytes()).hexdigest()}
  samples=[f for f in frames if f['cycle_mode']==mode]
  coverage[mode]=sorted({f['atlas_frame'] for f in samples});scales[mode]=sorted({f['pixel_size'] for f in samples})
 allscales={s for ss in scales.values() for s in ss};assert len(allscales)==1,scales
 for direction in [False,True]:
  for mode in ['walk','run']:
   seen={f['atlas_frame'] for f in frames if f['cycle_mode']==mode and f['flip_h']==direction}
   assert seen==set(range(cycles[mode]['frames'])),(sp,mode,direction,seen)
 if sp=='nocti':assert coverage['takeoff'] and coverage['land'] and coverage['rest']
 if sp=='mosslyn':assert coverage['lower'] and coverage['rise']
 assert frames[-1]['cycle_mode']=='rest',(sp,frames[-1]['cycle_mode'])
 ff=['ffmpeg','-hide_banner','-loglevel','error','-y'];subprocess.run(ff+['-framerate',str(fps),'-i',str(capture/'%04d.png'),'-c:v','libx264','-crf','18','-pix_fmt','yuv420p',str(out/(sp+'-welt.mp4'))],check=True)
 writer=subprocess.Popen(ff+['-f','rawvideo','-pix_fmt','rgb24','-s','720x528','-r',str(fps),'-i','pipe:0','-c:v','libx264','-crf','18','-pix_fmt','yuv420p',str(out/(sp+'-detail.mp4'))],stdin=subprocess.PIPE);font=ImageFont.truetype('DejaVuSans.ttf',20)
 for f in frames:
  im=Image.open(capture/f'{f["frame"]:04d}.png').convert('RGB');x=f['floor_screen'][0]*im.width/r['viewport_size'][0];y=f['floor_screen'][1]*im.height/r['viewport_size'][1]
  # Fester Maßstab und fester Bodenanker, niemals pro Pose passend zoomen.
  px=im.width/1280;half=round(120*px);top=round(120*px);bottom=round(40*px);crop=im.crop((round(x-half),round(y-top),round(x+half),round(y+bottom)))
  result=Image.new('RGB',(720,528),'#18291f');result.paste(crop.resize((720,480),Image.Resampling.NEAREST),(0,48));mode=f['cycle_mode'];label=('Ruhiger Flug' if mode=='walk' else 'Schneller Flug' if mode=='run' else NAMES[mode]) if sp=='nocti' else ('Hüpfen / Schweben' if sp=='starlet' and mode in ['walk','run'] else NAMES[mode]);ImageDraw.Draw(result).text((16,12),LABELS[sp]+' · '+label,font=font,fill='#efe5d3');writer.stdin.write(result.tobytes())
 writer.stdin.close();assert writer.wait()==0
 speeds={}
 for mode,target in [('walk',2.35),('run',3.85)]:
  sample=[f for f in frames if f['cycle_mode']==mode and abs(f['speed']-target)<.02]
  speeds[mode]={'speed_world_per_second':statistics.mean(f['speed'] for f in sample),'gait_cycles_per_second':statistics.mean(f['speed']/f['stride'] for f in sample)}
 if sp=='nocti':
  for mode in ['walk','run']:
   speeds[mode]['distance_cycles_per_second_not_wingbeat']=speeds[mode].pop('gait_cycles_per_second')
   spans=[]
   for left in [False,True]:
    wraps=[cur['frame'] for prev,cur in zip(frames,frames[1:]) if prev['cycle_mode']==mode==cur['cycle_mode'] and prev['flip_h']==left==cur['flip_h'] and prev['atlas_frame']>cur['atlas_frame']]
    spans.extend(b-a for a,b in zip(wraps,wraps[1:]) if b-a<fps)
   speeds[mode]['observed_wingbeats_per_second']=fps/statistics.mean(spans)
 report=dict(species=sp,frame_count=len(frames),fps=fps,seconds=len(frames)/fps,fixed_pixel_size=list(allscales)[0],coverage=coverage,speed=speeds,sources=hashes,pressure_points_verified=False,runtime_slip_verified=False,limitations=['Neue Seitenfolgen; Tiefenansichten weiterhin vorhandene Zeichnungen.','Pfotenwege und Druckpunkte der neuen Zeichnungen noch nicht vollständig geprüft.','Mooslyns Ruhe hält eine gezeichnete Gehkontaktpose.'])
 (out/(sp+'-report.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report,ensure_ascii=False))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('capture',type=Path);p.add_argument('output',type=Path);a=p.parse_args();review(a.capture,a.output)
