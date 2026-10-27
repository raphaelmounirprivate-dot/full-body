"""Vérification visuelle reproductible des deux PDF mobiles contre les originaux."""
from pathlib import Path
import json
import pypdfium2 as pdfium
from PIL import Image, ImageChops, ImageStat, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
records=json.loads((ROOT/'docs/asset-optimization.json').read_text('utf-8'))
results=[]
for record in records:
    before=pdfium.PdfDocument(str(ROOT/record['original']))
    after=pdfium.PdfDocument(str(ROOT/record['asset']))
    thumbs=[];errors=[]
    for i in range(len(after)):
        a=before[i].render(scale=1).to_pil().convert('RGB')
        b=after[i].render(scale=1).to_pil().convert('RGB')
        assert a.size==b.size
        mean=sum(ImageStat.Stat(ImageChops.difference(a,b)).mean)/3
        assert mean<8, (record['asset'],i+1,mean)
        errors.append(mean)
        b.thumbnail((208,290))
        tile=Image.new('RGB',(224,320),'white');tile.paste(b,((224-b.width)//2,20))
        ImageDraw.Draw(tile).text((8,304),f'Page {i+1}',fill='black')
        thumbs.append(tile)
    sheet=Image.new('RGB',(224*4,320*((len(thumbs)+3)//4)), '#dce3ec')
    for i,tile in enumerate(thumbs):sheet.paste(tile,((i%4)*224,(i//4)*320))
    name='guide-mobile-contact.jpg' if len(after)==31 else 'legacy-mobile-contact.jpg'
    sheet.save(ROOT/'docs'/name,quality=88)
    results.append({'asset':record['asset'],'pagesRendered':len(after),'maxMeanPixelError':max(errors),'contact':name})
    before.close();after.close()
(ROOT/'docs/pdf-render-validation.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
print(json.dumps(results))
