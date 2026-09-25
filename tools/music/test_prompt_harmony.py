"""The prompt rules that keep the high register in tune.

This exists because of a listening report on 2026-09-25: over longer play the music sounded
"schraeg", the high piano notes "uebersteuert", like caterwauling. A measurement of all 29 shipped
takes pointed at two prompt habits rather than at the model:

- **A second bright voice doubling the melody an octave higher.** Thirteen hommage prompts asked
  for "a soft 8-bit square-wave voice doubles the melody one octave higher". The model renders the
  two voices without locking their tuning, and in the octave above the melody even a few cents of
  difference beat audibly. Those takes carried on average 5.8 dB more energy in 2-5 kHz than the
  others.
- **Saturation and overdrive as texture.** "Warm analog saturation", "crunchy overdriven",
  "gritty", "fuzzy". Distortion adds intermodulation products that sit off the note grid. Those
  takes showed about 75 % more roughness above 900 Hz.

Neither shows up in the loop/peak gate of ``audio_polish.py``, which measures level, not pitch -
so, as with the "wow and flutter" lesson of 2026-09-20, the prompt text is the defence, and
this test keeps it from quietly coming back.

Gloop's theme is exempt from the harmony paragraph: it is protected from replacement, and its
slightly off-kilter melody is intended.
"""

from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
HARMONY_EXEMPT = {"theme-gloop"}

# A voice that doubles another one above it - the mistuned-octave source.
DOUBLING_ABOVE = re.compile(r"doubl\w*[^.]*\b(octave (higher|up)|above)\b", re.IGNORECASE)
# Asking for distortion as a colour.
DISTORTION = re.compile(r"\b(saturation|saturated|overdriven|overdrive|fuzzy|fuzz|gritty|crunchy)\b",
                        re.IGNORECASE)
# Bell-like leads with inharmonic overtones in the top register.
BELL_LEADS = re.compile(r"\b(glockenspiel|celesta|kalimba|music[- ]box)\b", re.IGNORECASE)


def prompts():
    manifest = json.loads((REPO / "music" / "manifest.json").read_text(encoding="utf-8"))
    for track in manifest["tracks"]:
        text = (REPO / track["prompt_file"]).read_text(encoding="utf-8")
        yield track["id"], " ".join(text.split())


class PromptHarmonyTest(unittest.TestCase):
    def test_no_voice_doubles_the_melody_above_it(self):
        for track_id, text in prompts():
            with self.subTest(track=track_id):
                self.assertIsNone(DOUBLING_ABOVE.search(text))

    def test_no_prompt_asks_for_distortion_as_texture(self):
        for track_id, text in prompts():
            with self.subTest(track=track_id):
                self.assertIsNone(DISTORTION.search(text))

    def test_no_bell_like_lead_in_the_top_register(self):
        for track_id, text in prompts():
            with self.subTest(track=track_id):
                self.assertIsNone(BELL_LEADS.search(text))

    def test_every_prompt_states_the_harmony_rules(self):
        for track_id, text in prompts():
            if track_id in HARMONY_EXEMPT:
                continue
            with self.subTest(track=track_id):
                self.assertIn("Harmony and tone: one key from the first bar to the last", text)
                self.assertIn("perfectly in tune with every other", text)

    def test_the_rules_would_catch_the_old_wording(self):
        self.assertIsNotNone(DOUBLING_ABOVE.search(
            "On every second pass a soft 8-bit square-wave voice doubles the melody one octave higher."))
        self.assertIsNotNone(DISTORTION.search("A little tape hiss and warm analog saturation."))
        self.assertIsNotNone(BELL_LEADS.search("Gentle glockenspiel or celesta states a melody."))


if __name__ == "__main__":
    unittest.main()
