import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class ContentTests(unittest.TestCase):
    def setUp(self):
        self.exercises = json.loads((ROOT/'app/src/main/assets/exercises.json').read_text('utf-8'))
        self.by_id = {e['id']:e for e in self.exercises}
    def test_all_nineteen_stable_ids(self):
        self.assertEqual(set(self.by_id), {f'F{i}' for i in range(1,20)})
    def test_every_illustration_exists(self):
        for e in self.exercises:
            self.assertGreater((ROOT/'app/src/main/assets'/e['image']).stat().st_size,1000)
            self.assertTrue(e['instructions'] and e['muscles'] and e['setup'])
    def test_original_program_parameters(self):
        self.assertEqual((self.by_id['F8']['sets'],self.by_id['F8']['min'],self.by_id['F8']['max']),(4,8,10))
        self.assertEqual((self.by_id['F6']['sets'],self.by_id['F6']['min'],self.by_id['F6']['max']),(3,30,45))
        self.assertEqual(self.by_id['F17']['rest'],30)
        self.assertEqual(self.by_id['F12']['rest'],0)
    def test_no_network_permission(self):
        self.assertNotIn('android.permission.INTERNET',(ROOT/'app/src/main/AndroidManifest.xml').read_text('utf-8'))

if __name__=='__main__': unittest.main()
