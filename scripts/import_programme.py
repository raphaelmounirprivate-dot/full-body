"""Import de l'archive de 8 pages, conservée pour les anciennes séances."""
import json
from pathlib import Path
from pypdf import PdfReader
import pypdfium2 as pdfium

ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path(r'C:\Users\Darkmoon\Desktop\full body\Programme_full_body_machines.pdf')
ASSETS = ROOT / 'app/src/main/assets'
rows = [
 ('F8','Hack squat (ou pendulum squat)',2,4,8,10,120,'rép'),
 ('F2','Développé couché à disques',2,4,8,10,120,'rép'),
 ('F3','Tirage vertical (machine)',3,3,10,12,90,'rép'),
 ('F4','Leg curl assis',3,3,10,12,60,'rép'),
 ('F5','Élévations latérales à la poulie',4,3,12,15,60,'rép'),
 ('F19','Crunch à la poulie haute',4,3,12,15,60,'rép'),
 ('F1','Presse à cuisses',5,4,10,12,120,'rép'),
 ('F9','Rowing à disques (appui poitrine)',5,4,8,10,90,'rép'),
 ('F10','Développé incliné à la poulie double',5,3,10,12,90,'rép'),
 ('F18','Leg extension',5,3,12,15,60,'rép'),
 ('F12','Curl biceps à la poulie',6,3,12,12,0,'rép'),
 ('F13','Extension triceps à la poulie',6,3,12,12,0,'rép'),
 ('F6','Gainage planche',6,3,30,45,45,'s'),
 ('F7','Machine squat à levier',7,4,8,10,120,'rép'),
 ('F11','Hip thrust (machine)',7,3,10,12,90,'rép'),
 ('F14','Rowing assis à la poulie',7,3,10,12,90,'rép'),
 ('F15','Développé épaules (machine)',7,3,10,12,90,'rép'),
 ('F16','Écarté à la poulie vis-à-vis',8,3,12,15,60,'rép'),
 ('F17','Finisher au rameur',8,6,30,30,30,'s'),
]
reader=PdfReader(SOURCE)
doc=pdfium.PdfDocument(str(SOURCE))
result=[]
for page_no in range(2,9):
 page=doc[page_no-1]
 text=reader.pages[page_no-1].extract_text()
 page_rows=[r for r in rows if r[2]==page_no]
 # Text blocks are in original reading order. Image boxes preserve source mapping.
 boxes=sorted([o.get_bounds() for o in page.get_objects() if o.type == 3], key=lambda b: -b[3])
 groups=[]
 for box in boxes:
  if groups and abs(groups[-1][3]-box[3]) < 5:
   prev=groups[-1]; groups[-1]=(min(prev[0],box[0]),min(prev[1],box[1]),max(prev[2],box[2]),max(prev[3],box[3]))
  else: groups.append(box)
 image_boxes=groups
 for idx,(eid,name,p,sets,low,high,rest,unit) in enumerate(page_rows):
  normalized=' '.join(text.split())
  start=normalized.index(name)
  end=normalized.index(page_rows[idx+1][1],start+len(name)) if idx+1<len(page_rows) else len(normalized)
  block=normalized[start:end]
  muscles=block.split('Muscles ciblés : ')[1].split('Secondaires :')[0].strip()
  setup=block.split('Réglage : ')[1].split('Accessoire :')[0].strip()
  after=block.split('Accessoire : ')[1]
  # Source descriptions retained whole, including accessory and execution text.
  instructions=after.split('Voir ')[0].strip()
  image=''
  if idx<len(image_boxes):
   box=image_boxes[idx]
   image=f'{eid}.png'
   width,height=page.get_size()
   page.render(scale=2,crop=(box[0],box[1],width-box[2],height-box[3])).to_pil().save(ASSETS/image)
  result.append(dict(id=eid,name=name,sets=sets,min=low,max=high,rest=rest,unit=unit,muscles=muscles,setup=setup,accessory='Accessoire et exécution',instructions=instructions,image=image,page=p))
(ASSETS/'exercises.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(ASSETS/'Programme_full_body_machines.pdf').write_bytes(SOURCE.read_bytes())
(ROOT/'docs/source-mapping.json').write_text(json.dumps({'source':SOURCE.name,'pages':8,'note':'Archive historique de 8 pages. F1–F19 sont des identifiants de correspondance ajoutés ; les nouvelles séances utilisent le guide corrigé de 31 pages.','exercises':result},ensure_ascii=False,indent=2),encoding='utf-8')
print(f'{len(result)} exercices importés')
