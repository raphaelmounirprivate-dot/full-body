"""Copies des artefacts réellement générés et archive du code, sans outils/cache."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import shutil

root=Path(__file__).resolve().parents[1]
out=root/'output';out.mkdir(exist_ok=True)
artifacts=[(root/'app/build/outputs/apk/debug/app-debug.apk',out/'Mon-Coach-Sport-debug.apk'),(root/'app/build/outputs/bundle/release/app-release.aab',out/'Mon-Coach-Sport-release-non-signe.aab')]
for source,target in artifacts:
    if not source.exists(): raise FileNotFoundError(source)
    shutil.copy2(source,target)
with ZipFile(out/'Mon-Coach-Sport-sources.zip','w',ZIP_DEFLATED) as archive:
    for name in ['app/src','docs','scripts','gradle']:
        for path in (root/name).rglob('*'):
            if path.is_file() and '__pycache__' not in path.parts: archive.write(path,path.relative_to(root))
    for name in ['AGENTS.md','README.md','CHANGELOG.md','.gitignore','.gitattributes','settings.gradle.kts','build.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts']:
        archive.write(root/name,name)
checksums='\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name for p in sorted(out.iterdir()) if p.suffix in ['.apk','.aab','.zip'])+'\n'
(out/'SHA256SUMS.txt').write_text(checksums,encoding='ascii')
print(checksums)
