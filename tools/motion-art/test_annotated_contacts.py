"""Eine schwebende Pfote darf keine Kontaktstrecke des Gegenbeins erben."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image, ImageDraw

module_spec = importlib.util.spec_from_file_location(
    "annotated_contacts", Path(__file__).with_name("measure_annotated_contacts.py"))
contacts = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(contacts)


class AnnotatedContactsTest(unittest.TestCase):
    def test_airborne_named_foot_is_not_replaced_by_grounded_other_foot(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp)
            (project / "data").mkdir()
            image = Image.new("RGBA", (128, 64))
            draw = ImageDraw.Draw(image)
            frames = []
            for i in range(8):
                ox, oy = i % 4 * 32, i // 4 * 32
                bottom = 20 if i == 4 else 29
                draw.rectangle((ox+4, oy+16, ox+8, oy+bottom), fill="white")
                draw.rectangle((ox+20, oy+16, ox+24, oy+29), fill="white")
                frames.append(dict(feet={"near": [4, 16, 5, 14], "far": [20, 16, 5, 14]}))
            image.save(project / "test.png")
            spec = dict(asset="res://test.png", sha256=hashlib.sha256((project / "test.png").read_bytes()).hexdigest(),
                        source_sha256="fixture", ground=30, cell=32, columns=4,
                        cycle_distance_pixels=285, reference_height=208)
            (project / "data/mosslyn_authored_cycles.json").write_text(json.dumps({"walk": spec}))
            (project / "data/visual_geometry.json").write_text(json.dumps({"character_height": {}}))
            (project / "data/new_characters.json").write_text(json.dumps({"mosslyn": {"relative_height": 1}}))
            annotation = project / "annotation.json"
            annotation.write_text(json.dumps(dict(mode="walk", asset="res://test.png", frames=frames)))
            with patch.object(contacts, "PROJECT", project):
                report = contacts.measure(annotation)
            self.assertTrue(report["contacts"][4]["far"]["contact"])
            self.assertFalse(report["contacts"][4]["near"]["contact"])
            self.assertIsNone(report["intervals"][3]["travel_pixels"])
            self.assertIsNone(report["estimated_cycle_pixels"])
            self.assertFalse(report["complete_contact_coverage"])
            self.assertFalse(report["pressure_points_verified"])

    def test_rejects_annotations_for_another_atlas(self):
        annotation = Path(__file__).with_name("mosslyn-walk-8.json")
        with self.assertRaisesRegex(ValueError, "anderen Atlas"):
            contacts.measure(annotation)


if __name__ == "__main__":
    unittest.main()
