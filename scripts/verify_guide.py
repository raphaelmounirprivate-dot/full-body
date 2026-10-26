"""Contrôle du contenu effectivement embarqué, confronté au PDF corrigé."""
import hashlib
import json
import re
import unittest
from pathlib import Path

from PIL import Image
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'app/src/main/assets/guide'


class GuideTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mapping=json.loads((ROOT/'docs/guide-source-mapping.json').read_text('utf-8'))
        cls.exercises=json.loads((ASSETS/'exercises.json').read_text('utf-8'))
        cls.pdf=PdfReader(ASSETS/cls.mapping['source'])

    def test_original_guide_kept_with_its_hash(self):
        self.assertEqual(31,len(self.pdf.pages))
        self.assertEqual(self.mapping['sha256'],hashlib.sha256((ASSETS/self.mapping['source']).read_bytes()).hexdigest())

    def test_nineteen_fiches_keep_source_order(self):
        self.assertEqual([f'F{i}' for i in range(1,20)],[e['id'] for e in self.exercises])
        self.assertEqual(list(range(9,28)),[e['sourcePage'] for e in self.exercises])

    def test_every_instruction_is_present_in_its_source_page(self):
        for e in self.exercises:
            text=' '.join(self.pdf.pages[e['sourcePage']-1].extract_text().split())
            for field in ['name','muscles','caption','setup','instructions','check','warning']:
                with self.subTest(id=e['id'],field=field):
                    self.assertTrue(e[field]);self.assertIn(e[field],text)

    def test_nineteen_distinct_uniform_images(self):
        sizes=set();hashes=set()
        for e in self.exercises:
            path=ROOT/'app/src/main/assets'/e['image']
            with Image.open(path) as im:
                sizes.add(im.size);self.assertGreater(im.width,1000)
            digest=hashlib.sha256(path.read_bytes()).hexdigest()
            self.assertEqual(e['imageSha256'],digest);hashes.add(digest)
        self.assertEqual(1,len(sizes));self.assertEqual(19,len(hashes))

    def test_leg_curl_and_plank_limitations_are_preserved(self):
        by_id={e['id']:e for e in self.exercises}
        self.assertIn('ce siège ne représente pas la machine',by_id['F4']['caption'])
        self.assertIn('deux options, pas deux phases',by_id['F6']['caption'])
        self.assertIn('sans sprint maximal',by_id['F17']['check'])

    def test_app_names_match_the_guide(self):
        names=dict(re.findall(r'"(F\d+)" to "([^"\n]+)"',(ROOT/'app/src/main/java/be/moncoachsport/Programs.kt').read_text('utf-8')))
        self.assertEqual({e['id']:e['name'] for e in self.exercises},names)

    def test_general_safety_and_variants_are_available(self):
        sections=json.loads((ASSETS/'sections.json').read_text('utf-8'))
        self.assertEqual([1,2,3,7,8,31],[s['page'] for s in sections])
        self.assertIn('douleur thoracique',sections[0]['text'])
        self.assertIn('au maximum une fois par semaine',sections[3]['text'])
        self.assertIn('Ne monte pas aussi sa charge',sections[2]['text'])


if __name__=='__main__': unittest.main()
