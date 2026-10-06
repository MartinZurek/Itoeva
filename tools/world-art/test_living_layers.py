"""Prueft ausgelieferte Ebenen, Ufer und Materialien ohne Android."""
import json
import unittest
from pathlib import Path
import numpy as np
from PIL import Image
import living_layers as live

class LivingLayersTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rooms = json.loads(Path(__file__).with_name('living_parts.json').read_text())

    def test_all_painted_rooms_present(self):
        self.assertEqual(set(live.ROOMS),set(self.rooms))

    def test_neutral_composite_preserves_original_pixel_for_pixel(self):
        for name,room in self.rooms.items():
            with self.subTest(name=name):
                base=Image.open(live.ASSETS/f'{name}_live.png').copy().convert('RGBA')
                atlas=Image.open(live.ASSETS/f'{name}_parts.png').copy().convert('RGBA')
                for p in room['parts']:
                    crop=atlas.crop((p['sx'],p['sy'],p['sx']+p['w'],p['sy']+p['h']))
                    base.alpha_composite(crop,(p['x'],p['y']))
                original=Image.open(live.ASSETS/f'{name}.png').convert('RGBA')
                self.assertTrue(np.array_equal(np.asarray(base),np.asarray(original)))

    def test_texture_dimensions_and_memory_are_bounded(self):
        for name,room in self.rooms.items():
            with self.subTest(name=name):
                base=Image.open(live.ASSETS/f'{name}_live.png').copy()
                atlas=Image.open(live.ASSETS/f'{name}_parts.png').copy()
                self.assertEqual((480,270),base.size)
                self.assertLessEqual(max(atlas.size),2048)
                # Base, Atlas und Raster zusammen unter 4 MiB pro aktivem Ort.
                self.assertLess(480*270*5+atlas.width*atlas.height*4,4*1024*1024)
                grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8)
                self.assertEqual(480*270,grid.size)
                self.assertLess(grid.max(),len(live.MATERIALS))

    def test_every_water_strip_stays_entirely_in_water(self):
        for name,room in self.rooms.items():
            grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8).reshape(270,480)
            for p in room['parts']:
                if p['kind']=='WATER':
                    self.assertTrue((grid[p['y']:p['y']+p['h'],p['x']:p['x']+p['w']]==5).all(),name)

    def test_docks_chair_and_dry_ground_do_not_turn_to_water(self):
        for name in ('pond','swamp','beach'):
            grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8).reshape(270,480)
            for x,y,r,b in live.ROOMS[name]['protect']:
                self.assertFalse((grid[y:b,x:r]==5).any(),name)
        for name,x,y in [('beach',400,235),('swamp',420,232),('pond',120,164)]:
            grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8).reshape(270,480)
            self.assertNotEqual(5,grid[y,x])

    def test_shallow_beach_and_swamp_water_are_reachable(self):
        metas=json.loads(Path(__file__).with_name('places').joinpath('meta.json').read_text())
        for name in ('beach','swamp'):
            grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8).reshape(270,480)
            walk=metas[name]['walk']
            accessible=grid[int(walk['farY']):int(walk['nearY']),20:460]
            self.assertGreater((accessible==5).sum(),200,name)

    def test_mud_sand_and_indoor_floor_are_distinct(self):
        for name,expected in [('beach',3),('swamp',4),('living',1),('bedroom',1)]:
            grid=np.frombuffer((live.ASSETS/f'{name}_materials.bin').read_bytes(),np.uint8).reshape(270,480)
            self.assertEqual(expected,grid[240,220])

    def test_real_alpha_edges_and_indoor_motion_are_present(self):
        for name in ('forest','living','bedroom','beach'):
            room=self.rooms[name]
            atlas=Image.open(live.ASSETS/f'{name}_parts.png').copy().convert('RGBA')
            moving=[p for p in room['parts'] if p['kind'] not in ('WATER','CLOUD')]
            self.assertTrue(moving,name)
            self.assertTrue(any((np.asarray(atlas.crop((p['sx'],p['sy'],p['sx']+p['w'],p['sy']+p['h'])))[:,:,3]==0).any() for p in moving),name)

if __name__=='__main__':
    unittest.main()
