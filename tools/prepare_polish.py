"""Pin official, unmodified candidate files. Nothing here installs into a user's game."""
import concurrent.futures, hashlib, io, json, pathlib, urllib.request, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
RESEARCH = json.loads((ROOT/'evidence/polish-research.json').read_text(encoding='utf8'))
# No third-party resource packs are pinned: the modpack ships its own look (Kort, 2026-10-18).
PINS = {
    'iris': '1.8.14-beta.1+1.21.1-fabric', 'sodium': 'mc1.21.1-0.8.13-fabric',
    'lithium': 'mc1.21.1-0.15.4-fabric', 'ferrite-core': '7.0.3-fabric',
    'immediatelyfast': '1.6.15+1.21.1-fabric', 'modernfix': '5.25.1+mc1.21.1',
    'entity-model-features': '3.3.12-fabric-1.21',
    'entitytexturefeatures': '7.2.6-fabric-1.21',
    'presence-footsteps': '1.11.2+1.21',
    'entityculling': '1.12.1', 'modmenu': '11.0.5',  # port/tools: evidence/polish-research.json must be regenerated for 1.21.1
    # 5.5.1 retains the author's explicit modpack redistribution grant.
    # 5.6+ disallows uploading the original pack outside CF/Modrinth systems.
    'complementary-reimagined': 'r5.5.1', 'complementary-unbound': 'r5.5.1',
    'makeup-ultra-fast-shaders': '9.5g',
}

def fetch(slug):
    project=next(p for p in RESEARCH if p['slug']==slug)
    v=next(v for v in project['versions'] if v['version_number']==PINS[slug])
    f=next(f for f in v['files'] if f['primary'])
    folder='shaders' if 'shaders' in v['loaders'] or slug.startswith('complementary') or slug.startswith('makeup') else 'resourcepacks' if v['loaders']==['minecraft'] else 'mods'
    dest=ROOT/'dependencies'/folder/f['filename'];dest.parent.mkdir(parents=True,exist_ok=True)
    if not dest.exists():
        req=urllib.request.Request(f['url'],headers={'User-Agent':'Graves and Glory/0.2.0 (Minecraft compatibility evaluation)'})
        dest.write_bytes(urllib.request.urlopen(req,timeout=60).read())
    data=dest.read_bytes()
    assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512'], slug+' checksum mismatch'
    z=zipfile.ZipFile(io.BytesIO(data));notices={}
    for name in z.namelist():
        if ('license' in name.lower() or name.lower().endswith('notice')) and not name.endswith('.jar'):
            notices[name]=z.read(name).decode('utf8',errors='replace')
    if slug.startswith('complementary'):
        assert any('redistributed inside a Modpack' in t and 'direct file upload' not in t for t in notices.values())
    manifest=json.loads(z.read('fabric.mod.json')) if 'fabric.mod.json' in z.namelist() else None
    return {'slug':slug,'title':project['title'],'version':v['version_number'],'versionId':v['id'],
            'path':dest.relative_to(ROOT).as_posix(),'fileName':f['filename'],'size':len(data),
            'sha256':hashlib.sha256(data).hexdigest(),'sha512':f['hashes']['sha512'],
            'download':f['url'],'project':'https://modrinth.com/'+('resourcepack/' if folder=='resourcepacks' else 'shader/' if folder=='shaders' else 'mod/')+slug,
            'source':project.get('source'),'licenseMetadata':project['license'],
            'notices':notices,'manifest':manifest,'dependencies':v['dependencies'],
            'status':'candidate - requires integration testing and source/notice packaging'}

if __name__=='__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool: items=list(pool.map(fetch,PINS))
    out=ROOT/'dependencies/manifest.json';out.write_text(json.dumps(items,indent=2),encoding='utf8')
    for p in items:
        print(p['slug'],p['version'],p['fileName'],p['size'], (p['manifest'] or {}).get('depends',{}))


