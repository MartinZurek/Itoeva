"""Die Best-of-N-Regel aus best_take.py, ohne Audio geprueft."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from best_take import Take, pick  # noqa: E402


class BestTakeTest(unittest.TestCase):
    def test_the_least_rough_take_wins(self):
        takes = [Take("a", -15.0, 0.20), Take("b", -16.0, 0.08), Take("c", -14.0, 0.15)]
        self.assertEqual("b", pick(takes).name)

    def test_a_dull_take_cannot_win_by_having_no_highs(self):
        # "b" ist am wenigsten rau, aber 10 dB dumpfer als seine Geschwister.
        takes = [Take("a", -15.0, 0.20), Take("b", -30.0, 0.01), Take("c", -14.0, 0.15)]
        self.assertEqual("c", pick(takes).name)

    def test_near_tie_goes_to_the_softer_take(self):
        takes = [Take("a", -12.0, 0.100), Take("b", -18.0, 0.102)]
        self.assertEqual("b", pick(takes).name)

    def test_clear_lead_is_not_overturned_by_softness(self):
        takes = [Take("a", -12.0, 0.08), Take("b", -18.0, 0.12)]
        self.assertEqual("a", pick(takes).name)

    def test_a_single_take_wins_by_default(self):
        self.assertEqual("a", pick([Take("a", -40.0, 0.5)]).name)

    def test_empty_field_is_an_error(self):
        with self.assertRaises(ValueError):
            pick([])


if __name__ == "__main__":
    unittest.main()
