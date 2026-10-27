"""Copies des artefacts réellement générés et archive du code, sans outils/cache."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import shutil
import re

root=Path(__file__).resolve().parents[1]
out=root/'output';out.mkdir(exist_ok=True)
artifacts=[(root/'app/build/outputs/apk/debug/app-debug.apk',out/'Mon-Coach-Sport-debug.apk'),(root/'app/build/outputs/bundle/release/app-release.aab',out/'Mon-Coach-Sport-release-non-signe.aab')]
optimized=root/'.tools/Mon-Coach-Sport-optimise-test.apk'
if optimized.exists():
    unsigned=root/'app/build/outputs/apk/release/app-release-unsigned.apk'
    with ZipFile(unsigned) as a, ZipFile(optimized) as b:
        payload=lambda z:{i.filename:(i.CRC,i.file_size) for i in z.infolist() if i.filename!='META-INF/MANIFEST.MF' and not re.fullmatch(r'META-INF/[^/]+\.(SF|RSA|DSA|EC)',i.filename)}
        if payload(a)!=payload(b):raise ValueError('APK optimisé périmé : signer le dernier build avant empaquetage.')
    artifacts.append((optimized,out/'Mon-Coach-Sport-optimise-test.apk'))
for source,target in artifacts:
    if not source.exists(): raise FileNotFoundError(source)
    shutil.copy2(source,target)
with ZipFile(out/'Mon-Coach-Sport-sources.zip','w',ZIP_DEFLATED) as archive:
    for name in ['app/src','docs','scripts','gradle','.github']:
        for path in (root/name).rglob('*'):
            if path.is_file() and '__pycache__' not in path.parts: archive.write(path,path.relative_to(root))
    for name in ['AGENTS.md','README.md','CHANGELOG.md','.gitignore','.gitattributes','settings.gradle.kts','build.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts','app/proguard-rules.pro']:
        archive.write(root/name,name)
checksums='\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name for p in sorted(out.iterdir()) if p.suffix in ['.apk','.aab','.zip'])+'\n'
(out/'SHA256SUMS.txt').write_text(checksums,encoding='ascii')
print(checksums)
