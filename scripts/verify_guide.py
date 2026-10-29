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
        cls.original=ROOT/'docs/sources'/cls.mapping['source']
        cls.pdf=PdfReader(cls.original)

    def test_original_guide_kept_with_its_hash(self):
        self.assertEqual(31,len(self.pdf.pages))
        self.assertEqual(self.mapping['sha256'],hashlib.sha256(self.original.read_bytes()).hexdigest())

    def test_mobile_pdf_preserves_all_text_and_page_sizes(self):
        records=json.loads((ROOT/'docs/asset-optimization.json').read_text('utf-8'))
        for record in records:
            original=ROOT/record['original'];mobile=ROOT/record['asset']
            self.assertEqual(record['originalSha256'],hashlib.sha256(original.read_bytes()).hexdigest())
            self.assertEqual(record['sha256'],hashlib.sha256(mobile.read_bytes()).hexdigest())
            before=PdfReader(original);after=PdfReader(mobile)
            self.assertEqual(len(before.pages),len(after.pages))
            self.assertLess(mobile.stat().st_size,original.stat().st_size/3)
            for a,b in zip(before.pages,after.pages):
                self.assertEqual(a.extract_text(),b.extract_text())
                self.assertEqual(list(a.mediabox),list(b.mediabox))

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

    def test_machine_correction_is_separate_from_preserved_source(self):
        corrections=json.loads((ASSETS/'illustration-overrides.json').read_text('utf-8'))
        self.assertEqual({'F4'},set(corrections))
        correction=corrections['F4']
        original=next(e for e in self.exercises if e['id']=='F4')
        self.assertEqual(original['image'],correction['originalImage'])
        self.assertEqual(original['imageSha256'],correction['originalImageSha256'])
        self.assertIn('sur la machine',correction['caption'])
        self.assertIn('ancien schéma sur chaise',correction['note'])
        self.assertIn('Modèle générique',correction['note'])
        for image_key,hash_key in [('image','imageSha256'),('sourceImage','sourceImageSha256')]:
            path=ROOT/'app/src/main/assets'/correction[image_key] if image_key=='image' else ROOT/correction[image_key]
            self.assertEqual(correction[hash_key],hashlib.sha256(path.read_bytes()).hexdigest())
            with Image.open(path) as picture:
                self.assertGreater(picture.width,1000)
                self.assertAlmostEqual(picture.width/picture.height,1.5,places=2)


if __name__=='__main__': unittest.main()
