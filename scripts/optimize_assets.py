"""Copies mobiles allégées ; originaux conservés hors APK, textes et pages vérifiés."""
from pathlib import Path
import hashlib
import json
import shutil
from pypdf import PdfReader, PdfWriter
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
ORIGINALS = ROOT / 'docs/sources'

def optimize_pdf(path, limit):
    original = ORIGINALS / path.name
    ORIGINALS.mkdir(parents=True, exist_ok=True)
    if not original.exists():
        shutil.copyfile(path, original)
    reader = PdfReader(original)
    writer = PdfWriter(clone_from=reader)
    seen = set()
    for page in writer.pages:
        for item in page.images:
            reference = item.indirect_reference.idnum if item.indirect_reference else item.name
            if reference in seen:
                continue
            seen.add(reference)
            image = item.image.convert('RGB')
            image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
            item.replace(image, quality=78, optimize=True)
    writer.compress_identical_objects(remove_identicals=True, remove_orphans=True)
    with path.open('wb') as output:
        writer.write(output)
    compact = PdfReader(path)
    assert len(reader.pages) == len(compact.pages)
    for before, after in zip(reader.pages, compact.pages):
        assert before.extract_text() == after.extract_text(), 'Texte modifié'
        assert list(before.mediabox) == list(after.mediabox), 'Format modifié'
    return {'original': str(original.relative_to(ROOT)).replace('\\','/'),
            'originalSha256': hashlib.sha256(original.read_bytes()).hexdigest(),
            'asset': str(path.relative_to(ROOT)).replace('\\','/'),
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'originalBytes': original.stat().st_size, 'bytes': path.stat().st_size,
            'pages': len(reader.pages)}

def main():
    results = [optimize_pdf(ASSETS/'guide/Programme_Jims_Namur_images_uniformisees.pdf', 1400),
               optimize_pdf(ASSETS/'Programme_full_body_machines.pdf', 1800)]
    exercises_path = ASSETS/'exercises.json'
    exercises = json.loads(exercises_path.read_text('utf-8'))
    for e in exercises:
        path = ASSETS/e['image']
        if path.suffix.lower() != '.png':
            continue
        target = ASSETS/'legacy'/f"{e['id']}.webp"
        target.parent.mkdir(exist_ok=True)
        with Image.open(path) as im:
            im.convert('RGB').save(target, 'WEBP', quality=82, method=6)
        e['image'] = str(target.relative_to(ASSETS)).replace('\\','/')
        path.unlink()
    exercises_path.write_text(json.dumps(exercises, ensure_ascii=False, indent=2)+'\n',encoding='utf-8')
    (ROOT/'docs/asset-optimization.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(results,ensure_ascii=True))

if __name__ == '__main__': main()
