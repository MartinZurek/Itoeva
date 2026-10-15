"""Originales 12-Sekunden-Siegsthema: Glockenmelodie, gezupfte Saiten und weiches Pad.

Vier Takte in C-Dur, 80 BPM; keine Samples oder Melodie aus anderen Spielen.
Erzeugt deterministisches PCM und Ogg mit einer leisen, klickfreien Schleifengrenze.
"""
from pathlib import Path
import subprocess
import tempfile
import wave
import numpy as np
RATE=44100
DURATION=12.0
OUT=Path(__file__).resolve().parents[2]/'prototypes/godot-hd2d/assets/music/victory-theme.ogg'
t=np.arange(round(RATE*DURATION))/RATE
mix=np.zeros((len(t),2))
def note(midi,start,length,gain,kind='bell',pan=0):
    f=440*2**((midi-69)/12)
    u=t-start
    age=np.clip(u,0,length)
    gate=(u>=0)&(u<length)
    attack=1-np.exp(-age*(110 if kind=='bell' else 9))
    tail=np.minimum(1,np.maximum(0,length-age)/.18)
    if kind=='bell':
        sound=(np.sin(2*np.pi*f*age)+.22*np.sin(2*np.pi*f*2.003*age)+.08*np.sin(2*np.pi*f*3.98*age))*np.exp(-age*2.0)
    else:
        sound=(np.sin(2*np.pi*f*age)+.15*np.sin(2*np.pi*f*2*age)+.08*np.sin(2*np.pi*f*.998*age))*.65
    sound*=gate*attack*tail*gain
    mix[:,0]+=sound*(1-pan*.3)
    mix[:,1]+=sound*(1+pan*.3)
for bar,chord in enumerate([(48,55,60,64),(45,52,60,64),(41,53,57,60),(43,55,59,62)]):
    for j,n in enumerate(chord):note(n,bar*3+.02,2.9,.042,'pad',(j-1.5)/2)
    for beat in range(4):note(chord[beat%4]+12,bar*3+beat*.75,1.0,.045,'bell',(-1)**beat*.4)
melody=[(72,0,.65),(76,.75,.65),(79,1.5,.65),(84,2.25,1),(81,3.75,.7),(79,4.5,.7),(76,5.25,.8),(77,6,.65),(81,6.75,.65),(84,7.5,1.1),(79,9,.6),(74,9.75,.65),(71,10.5,.65),(79,11.25,.65)]
for midi,start,length in melody:note(midi,start,length,.105,'bell',.12)
dry=mix.copy()
for delay,gain in [(.13,.18),(.27,.12),(.41,.07)]:
    shift=round(delay*RATE);mix[shift:]+=dry[:-shift]*gain
fade=np.minimum(1,t/.06)*np.minimum(1,(DURATION-t)/.14)
mix*=fade[:,None]
peak=np.abs(mix).max()
assert peak<.8
with tempfile.TemporaryDirectory() as td:
    wav=Path(td)/'victory.wav'
    with wave.open(str(wav),'wb') as w:
        w.setnchannels(2);w.setsampwidth(2);w.setframerate(RATE);w.writeframes((mix*32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(OUT)],check=True)
print(f'Siegsthema: {DURATION}s, Spitze {peak:.3f}, {OUT.stat().st_size} Bytes')
