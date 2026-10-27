"""Nettoyage borné : archives d'outils extraits et anciens binaires, sources conservées."""
from pathlib import Path
import argparse
import json

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--apply',action='store_true',help='Supprimer les seuls fichiers listés ; sinon simulation')
args=parser.parse_args()
checks={
 'android.zip':'.tools/android/cmdline-tools/bin/sdkmanager.bat',
 'emulator.zip':'.tools/sdk/emulator/emulator.exe',
 'gradle.zip':'.tools/gradle-8.13/bin/gradle.bat',
 'jdk.zip':'.tools/java/jdk-17.0.16+8/bin/java.exe',
}
targets=[]
for name,extracted in checks.items():
    p=ROOT/'.tools'/name
    if p.is_file() and (ROOT/extracted).is_file(): targets.append(p)
for version in ['v0.1.0','v0.2.0','v0.2.1','v0.3.0','v0.3.1','v0.4.0']:
    folder=ROOT/'output'/version
    # L'archive source est conservée pour permettre de reconstruire ces versions.
    if list(folder.glob('*sources.zip')):
        targets += [p for p in folder.iterdir() if p.is_file() and p.suffix in {'.apk','.aab'}]
report=[]
for p in targets:
    absolute=p.resolve()
    assert absolute.is_relative_to(ROOT.resolve()), absolute
    assert absolute.is_relative_to((ROOT/'.tools').resolve()) or absolute.is_relative_to((ROOT/'output').resolve())
    report.append({'path':str(p.relative_to(ROOT)).replace('\\','/'),'bytes':p.stat().st_size})
    if args.apply: p.unlink()
result={'applied':args.apply,'removedBytes':sum(r['bytes'] for r in report),'files':report,
        'kept':'Outils extraits, archives source et version 0.5.0 de retour arrière conservés.'}
if args.apply: (ROOT/'docs/local-cleanup.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,ensure_ascii=True))
