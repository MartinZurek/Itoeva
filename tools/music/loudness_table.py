#!/usr/bin/env python3
"""Misst die Lautheit jeder ausgelieferten Musikdatei und schreibt sie als Kotlin-Tabelle.

Warum: Die erzeugten Stuecke liegen zwischen etwa -19,5 und -10,8 LUFS. Der Player spielte jede
Datei mit derselben Lautstaerke ab, ein Wechsel von "Bitter Pixel Symphony" zu "99 Pixels" war
deshalb ein Sprung um fast 9 dB. `MusicLoudness.kt` gleicht mit dieser Tabelle auf ein gemeinsames
Ziel an.

Aufruf (nach jedem neuen oder neu erzeugten Stueck):

    python3 tools/music/loudness_table.py

Misst nach ITU-R BS.1770 (K-Gewichtung, Gating), wenn `pyloudnorm` installiert ist; sonst eine
ungewichtete Naeherung mit derselben Blockgating-Logik - fuer das Angleichen innerhalb weniger dB
genuegt beides. Die Tabelle ist erzeugt; nicht von Hand bearbeiten.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "app-sim/src/main/res/raw"
OUT = ROOT / "app-sim/src/main/java/com/notime/glyphsim/matrix/MusicLoudnessTable.kt"


def measure(path: Path) -> float:
    data, rate = sf.read(str(path), always_2d=True)
    try:
        import pyloudnorm

        return float(pyloudnorm.Meter(rate).integrated_loudness(data))
    except ImportError:
        mono_power = (data ** 2).mean(axis=1)
        block = int(0.4 * rate)
        hop = block // 4
        powers = np.array(
            [mono_power[i:i + block].mean() for i in range(0, len(mono_power) - block, hop)]
        )
        powers = powers[powers > 10 ** (-70 / 10)]
        relative = powers.mean() * 10 ** (-10 / 10)
        gated = powers[powers > relative]
        return float(-0.691 + 10 * np.log10(gated.mean()))


def main() -> int:
    files = sorted(RAW.glob("itoeva_*.ogg"))
    if not files:
        print("Keine Musikdateien gefunden.", file=sys.stderr)
        return 1
    rows = []
    for path in files:
        lufs = measure(path)
        rows.append((path.stem, lufs))
        print(f"{path.stem:28s} {lufs:6.1f} LUFS")
    lines = [
        "package com.notime.glyphsim.matrix",
        "",
        "/**",
        " * Gemessene Lautheit (LUFS) jeder ausgelieferten Musikdatei - ERZEUGT von",
        " * `tools/music/loudness_table.py`, nicht von Hand bearbeiten. Siehe [MusicLoudness].",
        " */",
        "internal object MusicLoudnessTable {",
        "    val MEASURED_LUFS: Map<String, Double> = mapOf(",
    ]
    lines += [f'        "{name}" to {lufs:.1f},' for name, lufs in rows]
    lines += ["    )", "}", ""]
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"{len(rows)} Eintraege nach {OUT.relative_to(ROOT)} geschrieben.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
