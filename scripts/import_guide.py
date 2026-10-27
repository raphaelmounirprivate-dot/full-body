"""Import fidèle des fiches F1-F19 et de leurs images depuis le guide de 31 pages."""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path

import pypdfium2 as pdfium
from PIL import Image, ImageDraw, ImageFont
from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT/'docs/sources/Programme_Jims_Namur_images_uniformisees.pdf'


def normalize(text):
    return ' '.join(text.split())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', type=Path, default=DEFAULT_SOURCE)
    args = parser.parse_args()
    source = args.source.resolve()
    reader = PdfReader(source)
    assert len(reader.pages) == 31, 'Le guide doit contenir 31 pages.'
    document = pdfium.PdfDocument(str(source))
    assets = ROOT / 'app/src/main/assets/guide'
    assets.mkdir(parents=True, exist_ok=True)
    exercises = []
    for index in range(1, 20):
        page_number = index + 8
        text = reader.pages[page_number - 1].extract_text()
        match = re.search(rf'\bF{index} • ([^\n]+)\n([^\n]+)\n(.*?)Ton mouvement, en trois étapes\n(.*?)Illustration de repérage :(.*)', text, re.S)
        assert match, f'Fiche F{index} introuvable page {page_number}'
        name, muscles, caption, steps, warning = match.groups()
        steps = normalize(steps)
        install, move, check = re.fullmatch(r'1\. Installe-toi\. (.*?) 2\. Bouge\. (.*?) 3\. Vérifie\. (.*)', steps).groups()
        page = document[page_number - 1]
        images = [obj for obj in page.get_objects() if obj.type == 3]
        assert len(images) == 1, f'Image ambiguë page {page_number}'
        box = images[0].get_bounds()
        width, height = page.get_size()
        image = page.render(scale=3, crop=(box[0], box[1], width-box[2], height-box[3])).to_pil().convert('RGB')
        image.thumbnail((1600, 1200), Image.Resampling.LANCZOS)
        filename = f'F{index}.webp'
        image.save(assets / filename, 'WEBP', quality=88, method=6)
        exercises.append(dict(
            id=f'F{index}', name=normalize(name), muscles=normalize(muscles),
            setup=install, instructions=move, check=check,
            caption=normalize(caption), warning='Illustration de repérage : '+normalize(warning),
            accessory='', sets=0, min=0, max=0,
            rest=0, unit='s' if index==6 else 'rép',
            image=f'guide/{filename}', sourcePage=page_number,
            imageBounds=list(box), imageSha256=hashlib.sha256((assets/filename).read_bytes()).hexdigest(),
        ))
    # Les valeurs affichées en séance viennent de Programs.kt, pas de ces valeurs
    # de catalogue : une même chest press a des plages différentes en A/B/C.
    sections = []
    for number in [1, 2, 3, 7, 8, 31]:
        lines = reader.pages[number-1].extract_text().splitlines()
        start = next(i for i,line in enumerate(lines) if '•' in line and line[:2].isdigit())
        sections.append(dict(page=number, title=lines[start+1], text='\n'.join(lines[start+2:])))
    metadata = dict(source=source.name, pages=31, sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                    sourceNote='Guide personnel fourni ; non officiel du club. Illustrations générées, non assimilables à des notices constructeur.',
                    exercises=exercises, sections=sections)
    (assets/'exercises.json').write_text(json.dumps(exercises, ensure_ascii=False, indent=2), encoding='utf-8')
    (assets/'sections.json').write_text(json.dumps(sections, ensure_ascii=False, indent=2), encoding='utf-8')
    (ROOT/'docs/guide-source-mapping.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding='utf-8')
    shutil.copyfile(source, assets/source.name)
    contact = Image.new('RGB', (1200, 1100), 'white')
    draw = ImageDraw.Draw(contact)
    font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 14)
    for position, exercise in enumerate(exercises):
        picture = Image.open(assets/Path(exercise['image']).name)
        picture.thumbnail((230, 175))
        x, y = (position % 5)*240, (position // 5)*270
        draw.text((x+8,y+4),f"{exercise['id']} / p.{exercise['sourcePage']}", fill='black',font=font)
        contact.paste(picture,(x,y+30))
        draw.text((x+8,y+210),exercise['name'][:30],fill='black',font=font)
    contact.save(ROOT/'docs/guide-illustrations-contact.png')
    print(f'Import terminé : {len(exercises)} fiches, {len(sections)} sections, PDF original conservé.')


if __name__ == '__main__':
    main()
