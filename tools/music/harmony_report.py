#!/usr/bin/env python3
"""Misst, wie scharf und wie rau die Hoehen jeder Musikdatei sind - zum Vergleichen, nicht als Gate.

Warum: Gemeldet am 2026-09-25, die Musik klinge auf Dauer "schraeg", hohe Klaviertoene
"uebersteuert". Das Freigabe-Gate in `audio_polish.py` misst Pegel, Randstille und Loop-Naht,
aber keine Tonhoehen - diese Art Mangel ging deshalb ungesehen durch. Dieses Skript liefert zwei
Kennzahlen, mit denen sich eine Neuerzeugung gegen ihre Vorgaengerin vergleichen laesst:

- `scharf_db`: Energie 2-5 kHz gegen 100-1000 Hz, in dB. Dort sitzen Beiss-Anteile hoher
  Klavier- und Synthtoene; hoeher heisst schaerfer.
- `rau_hoch`: Rauigkeit (Plomp-Levelt-Modell nach Sethares) der Teiltoene oberhalb 900 Hz, auf
  ihre Energie normiert. Verstimmte Verdopplungen und Verzerrungsprodukte liegen nah beieinander,
  ohne auf dem Halbtonraster zu liegen, und treiben diesen Wert.

Beide Zahlen sind Hinweise, keine Hoerurteile: Ein absichtlich rauer Gitarrenklang misst rauer
als ein Klavier, ohne falsch zu sein. Aussagekraeftig ist der Vergleich desselben Stuecks vor und
nach einer Prompt-Aenderung. Die Entscheidung trifft das Hoeren.

Aufruf:

    python3 tools/music/harmony_report.py                 # alle ausgelieferten Stuecke
    python3 tools/music/harmony_report.py pfad/zu/take.ogg
    python3 tools/music/harmony_report.py --pick take_a.ogg take_b.ogg take_c.ogg  # Best-of-N
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import find_peaks, stft

RAW = Path(__file__).resolve().parents[2] / "app-sim" / "src" / "main" / "res" / "raw"
NFFT, HOP = 8192, 2048


def _roughness(freqs: np.ndarray, amps: np.ndarray) -> float:
    if freqs.size < 2:
        return 0.0
    keep = np.argsort(amps)[::-1][:24]
    f, a = freqs[keep], amps[keep]
    i, j = np.triu_indices(f.size, 1)
    fmin = np.minimum(f[i], f[j])
    s = 0.24 / (0.0207 * fmin + 18.96)
    d = np.abs(f[j] - f[i])
    r = np.minimum(a[i], a[j]) * (np.exp(-3.5 * s * d) - np.exp(-5.75 * s * d))
    return float(r.sum() / (a.sum() + 1e-12))


def measure(path: Path) -> dict:
    audio, rate = sf.read(str(path), always_2d=True)
    mono = audio.mean(axis=1)
    freqs, _, spec = stft(mono, rate, nperseg=NFFT, noverlap=NFFT - HOP, boundary=None)
    mag = np.abs(spec)
    power = mag ** 2

    def band(lo: float, hi: float) -> float:
        return float(power[(freqs >= lo) & (freqs < hi)].sum())

    scharf = 10 * np.log10(band(2000, 5000) / band(100, 1000))

    total = power.sum(axis=0)
    frames = np.flatnonzero(total > np.percentile(total, 20))
    frames = frames[:: max(1, frames.size // 400)]
    # Schwelle am ganzen tonalen Bereich, nicht am Hochband allein - sonst wird in einem Stueck
    # ohne Hoehen das Grundrauschen dort zu "Teiltoenen" hochgerechnet.
    ton = np.flatnonzero((freqs >= 120) & (freqs <= 4500))
    werte = []
    for j in frames:
        col = mag[ton, j]
        if col.max() <= 0:
            continue
        thr = col.max() * 10 ** (-45 / 20)
        peaks, _ = find_peaks(col, height=thr, prominence=thr)
        f, a = freqs[ton][peaks], col[peaks]
        werte.append(_roughness(f[f >= 900], a[f >= 900]))
    return {"datei": path.stem, "scharf_db": round(scharf, 1),
            "rau_hoch": round(float(np.mean(werte)) if werte else 0.0, 4)}


def main(argv: list[str]) -> int:
    pick_mode = bool(argv) and argv[0] == "--pick"
    if pick_mode:
        argv = argv[1:]
    files = [Path(a) for a in argv] or sorted(RAW.glob("itoeva_*.ogg"))
    rows = [measure(p) for p in files]
    if pick_mode:
        # Takes EINES Stuecks: Gewinner nach der Regel in best_take.py.
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        from best_take import Take, pick

        takes = [Take(str(p), r["scharf_db"], r["rau_hoch"]) for p, r in zip(files, rows)]
        for t in takes:
            print(f"{t.name}  scharf_db={t.scharf_db:.1f}  rau_hoch={t.rau_hoch:.4f}")
        print(f"gewinner: {pick(takes).name}")
        return 0
    print(f"{'datei':32} {'scharf_db':>9} {'rau_hoch':>9}")
    for r in sorted(rows, key=lambda r: -r["rau_hoch"]):
        print(f"{r['datei']:32} {r['scharf_db']:9.1f} {r['rau_hoch']:9.4f}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
