import hashlib,json,pathlib,shutil,zipfile
from release_config import version, source_identity, validate_player_jar
root=pathlib.Path(__file__).resolve().parents[1]
dist=root/'dist';dist.mkdir(exist_ok=True)
release_version=version()
jar=root/f'build/libs/graves-and-glory-{release_version}.jar'
package=dist/f'graves-and-glory-{release_version}.zip'
identity=source_identity()
api=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.2+1.20.1').rglob('fabric-api-0.92.2+1.20.1.jar'))
entries=[];files={}
dependencies=json.loads((root/'dependencies/manifest.json').read_text(encoding='utf8'))
research=json.loads((root/'dependencies/author-permissions.json').read_text(encoding='utf8'))
sources=json.loads((root/'dependencies/source-manifest.json').read_text(encoding='utf8'))
credits=['# Graves and Glory: third-party credits and permissions','',
 'Graves and Glory original code, environments, effects and original sounds: Kort Tjepkema, assisted by OpenAI Codex; MIT.',
 'Original Graves and Glory licensing does not replace any third-party terms below. Keep their notices when sharing a remix.',
 'This free Minecraft modpack is not endorsed by Mojang, Microsoft or the authors below.',
 'Shader/modpack compatibility support is the responsibility of Graves and Glory, not the shader authors.',
 'Excalibur and its patches are included unchanged for noncommercial use. Do not modify their assets or monetize a derivative containing them.',
 'Fresh Animations is included unchanged under the author\'s explicit modpack permission. Do not reupload it as your own pack.',
 'Faithful 32x is included unchanged as an optional world texture choice. https://faithfulpack.net/ and https://faithfulpack.net/license',
 'Complementary Reimagined and Unbound r5.5.1 are by EminGTR / Complementary Development: https://www.complementary.dev/shaders/ . Their contained License Agreement 1.5 explicitly permits modpack redistribution. Newer versions have different terms; do not replace these files automatically.',
 'MakeUp Ultra Fast: Javier GC (javiergcim), LGPL-3.0-or-later; GLSL source is inside its unchanged shader archive.',
 'Fresh Animations: FreshLX. Excalibur: Matthew (Maffhew) Dillow. Clarent: GrandPappyJay. Excalibur Fresh Animations Patch: GrandPappyJay and Ruseki, with FreshLX and Maffhew; see included original notices.',
 'Lato body font: Lukasz Dziedzic, SIL Open Font License 1.1; original OFL is inside the Graves and Glory jar. Source: https://github.com/google/fonts/tree/main/ofl/lato',
 'Marcellus heading font: Brian J. Bonislawsky / Astigmatic, SIL Open Font License 1.1; original OFL is inside the Graves and Glory jar.',
 'Fabric API: FabricMC contributors, Apache-2.0; Fabric Loader is provided by the selected launcher or server installer.',
 'Nested libraries retain their original licenses: ANTLR 4.13.1 (BSD-3-Clause), glsl-transformer 2.0.1 (AGPL-3.0), jcpp 1.4.14 (Apache-2.0), Reflect 1.6.2 (MIT), Kirin 1.15.6 (MIT).',
 'Corresponding library sources and build files are supplied in docs/third-party/sources alongside their notices. They may be replaced by compatible builds; no library is merged into Graves and Glory\'s code.', '']
validate_player_jar(jar,identity)
recipe=json.loads((root/'melty.json').read_text())
if recipe['components'][0]['fileName']!=package.name:
    raise RuntimeError('Melty component does not match release version')
with zipfile.ZipFile(package.with_suffix('.zip.tmp'),'w',zipfile.ZIP_DEFLATED) as archive:
    def add(src,dest):
        archive.write(src,dest);entries.append({'path':dest,'size':src.stat().st_size,'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
        if src.suffix in ['.md','.txt','.json','.pom'] or src.name=='LICENSE':files[dest]=src.read_text(encoding='utf8',errors='replace')
    def content(dest,data):
        if isinstance(data,str):data=data.encode('utf8')
        archive.writestr(dest,data);entries.append({'path':dest,'size':len(data),'sha256':hashlib.sha256(data).hexdigest()})
        if len(data)<40000:files[dest]=data.decode('utf8',errors='replace')
    for src,dest in [(jar,'mods/'+jar.name),(api,'mods/'+api.name),(root/'README.md','docs/README.md'),(root/'docs/UI_REDESIGN.md','docs/UI_REDESIGN.md'),(root/f'docs/RELEASE-{release_version}.txt',f'docs/RELEASE-{release_version}.txt'),(root/'LICENSE','docs/LICENSE')]:
        add(src,dest)
    # Preserve the exact upstream license without changing its notices.
    with zipfile.ZipFile(api) as z:
        licenses=[n for n in z.namelist() if 'license' in n.lower() and not n.endswith('.jar')]
        if not licenses: raise RuntimeError('Fabric API license missing')
        for n in licenses:
            data=z.read(n);dest='docs/third-party/'+pathlib.Path(n).name
            content(dest,data)
    # Explicit manifest allowlist: no test harness, player save, game asset jar or credentials.
    for dep in dependencies:
        src=root/dep['path']
        if hashlib.sha256(src.read_bytes()).hexdigest()!=dep['sha256']:
            raise RuntimeError('Pinned dependency checksum mismatch: '+dep['path'])
        folder='mods' if '/mods/' in dep['path'] else 'shaderpacks' if '/shaders/' in dep['path'] else 'resourcepacks'
        add(src,folder+'/'+src.name)
        who=(dep.get('manifest') or {}).get('authors',[])
        credits.extend(['## '+dep['title']+' '+dep['version'], 'Authors: '+json.dumps(who,ensure_ascii=False) if who else '',dep['project'],'Source: '+str(dep.get('source') or 'resource/shader sources in the original archive'), 'License metadata: '+dep['licenseMetadata']['id'],''])
        noticeRoot='docs/third-party/'+dep['slug']+'/'
        with zipfile.ZipFile(src) as z:
            for name in z.namelist():
                if not name.endswith('/') and len(name.split('/'))<=3 and any(t in name.lower() for t in ['license','terms','readme','credits']) and not name.endswith('.jar'):
                    data=z.read(name)
                    if len(data)<120000:content(noticeRoot+pathlib.Path(name).name,data)
            if dep.get('manifest'):files[folder+'/'+src.name+'!/fabric.mod.json']=json.dumps(dep['manifest'],indent=2)
        if dep['slug'] in ['fresh-animations','clarent','excalibur-fresh-animations']:
            body=next(p['body'] for p in research if p['slug']==dep['slug']);content(noticeRoot+'author-project-terms.md',body)
        if dep['slug'] in sources:
            info=sources[dep['slug']]
            for name,data in info['licenses'].items():content(noticeRoot+'source-'+pathlib.Path(name).name,data)
    for src in (root/'dependencies/sources').iterdir():
        if src.is_file():add(src,'docs/third-party/sources/'+src.name)
    for src in (root/'dependencies/notices').iterdir():
        if src.is_file():add(src,'docs/third-party/licenses/'+src.name)
    # Persist URLs and exact revisions for every supplied corresponding source.
    for name in ['source-manifest.json','nested-source-manifest.json','nested-full-sources.json']:
        add(root/'dependencies'/name,'docs/third-party/'+name)
    content('docs/THIRD_PARTY.md','\n'.join(credits))
with zipfile.ZipFile(jar) as z:files['mods/'+jar.name+'!/fabric.mod.json']=z.read('fabric.mod.json').decode()
package.with_suffix('.zip.tmp').replace(package)
manifest={'fileName':package.name,'build':identity,'size':package.stat().st_size,'sha256':hashlib.sha256(package.read_bytes()).hexdigest(),'entries':entries,'files':files}
(dist/'package-manifest.json').write_text(json.dumps(manifest,indent=2))
print(json.dumps({k:v for k,v in manifest.items() if k not in ('files','entries')},indent=2))
print('Packaged entries:',len(entries))
