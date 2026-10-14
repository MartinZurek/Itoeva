"""Eigene kurze Spielklänge: Quarz, Holz, Luft und Elementtexturen; keine Fremdsamples."""
from pathlib import Path
import wave
import numpy as np
from scipy.signal import butter, sosfilt

RATE = 44100
OUT = Path(__file__).resolve().parents[2] / 'prototypes/godot-hd2d/assets/audio'
OUT.mkdir(parents=True, exist_ok=True)
rng = np.random.default_rng(61014)

def make(key, seconds, notes=(), noise=0., low=1600, thump=0.):
    t = np.arange(round(seconds * RATE)) / RATE
    signal = np.zeros_like(t)
    for n, (frequency, start, strength) in enumerate(notes):
        u = np.maximum(0, t-start)
        env = (1-np.exp(-u*300))*np.exp(-u*(5 if key not in ['victory','defeat'] else 2.5))*(t>=start)
        # Leicht verstimmte Obertöne wirken eher wie angeschlagene Körper als Sinuspieper.
        tone = sum(np.sin(2*np.pi*frequency*(h+1)*(1+.001*h)*u)/(h+1)**2 for h in range(4))
        signal += strength * env * tone
    air = sosfilt(butter(2, low, fs=RATE, output='sos'), rng.normal(size=t.size))
    signal += noise * air * (np.sin(np.pi*np.clip(t/seconds,0,1))**1.2) * np.exp(-t*5)
    signal += thump*np.sin(2*np.pi*(58*t+12*(1-np.exp(-t*25))))*np.exp(-t*30)*(1-np.exp(-t*900))
    signal *= np.minimum(1,t/.006)*np.minimum(1,(seconds-t)/.035)
    signal = np.tanh(signal*.8)
    # Verschiedene frühe Reflexionen; Ende jedes Clips geht exakt auf Null.
    dry = signal.copy()
    for delay, amount in [(0.029,.16),(.057,.10),(.091,.05)]:
        offset=round(RATE*delay)
        signal[offset:] += dry[:-offset]*amount
    signal *= np.minimum(1,(seconds-t)/.035)
    peak=np.max(np.abs(signal))
    if peak>.72:signal*=.72/peak
    stereo=np.stack([signal,np.roll(signal,17)*.97],axis=1)
    stereo[:20]*=np.linspace(0,1,20)[:,None];stereo[-40:]*=np.linspace(1,0,40)[:,None]
    with wave.open(str(OUT/(key+'.wav')),'wb') as f:
        f.setnchannels(2);f.setsampwidth(2);f.setframerate(RATE)
        f.writeframes((stereo*32767).astype('<i2').tobytes())

make('select',.18,[(740,0,.20),(1110,.035,.08)])
make('crystal',.48,[(1046,0,.22),(1568,.04,.13),(2093,.09,.06)])
make('summon',.72,[(196,0,.18),(294,.08,.15),(392,.16,.13)],.45,2200,.3)
make('attack',.28,[],.95,3600,.35)
make('impact',.32,[(145,0,.20)],1.0,1200,1.0)
make('nature',.60,[(246,0,.15),(369,.05,.1)],.60,1300,.25)
make('fire',.65,[(98,0,.13)],1.25,3500,.65)
make('water',.70,[(523,0,.14),(659,.11,.13),(784,.18,.08)],.65,1700)
make('wind',.60,[(880,.02,.07)],1.2,4500)
make('light',.85,[(784,0,.16),(1174,.08,.10),(1568,.16,.07)],.18,5000)
make('moon',.90,[(220,0,.17),(330,.10,.12),(440,.20,.07)],.32,900)
make('shield',.65,[(392,0,.22),(587,.02,.13),(783,.045,.07)],.3,2500,.2)
make('heal',.80,[(523,0,.15),(659,.09,.13),(784,.18,.10),(1046,.28,.06)])
make('turn',.65,[(392,0,.15),(523,.11,.13),(784,.22,.08)])
make('victory',1.8,[(392,0,.17),(523,.15,.16),(659,.3,.15),(784,.5,.12),(1046,.7,.08)])
make('defeat',1.5,[(294,0,.16),(261,.25,.13),(196,.5,.12)])
make('error',.22,[(185,0,.13),(174,.06,.1)])
print(f'{len(list(OUT.glob("*.wav")))} eigene Effektdateien erzeugt')
