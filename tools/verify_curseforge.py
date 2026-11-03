"""Read back the local CurseForge zip and compare manifest, modlist and overrides to reviewed input."""
import hashlib
import io
import json
import zipfile
from export_curseforge import IDS_PATH, load_ids, build_manifest, build_modlist
from release_config import ROOT, version, source_identity, validate_player_jar

def verify():
    release=version();archive=ROOT/f'dist/graves-and-glory-{release}-curseforge.zip'
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text(encoding='utf8'))
    entries=load_ids(rows,json.loads((ROOT/IDS_PATH).read_text(encoding='utf8')))
    with zipfile.ZipFile(archive) as pack:
        assert pack.testzip() is None
        names=pack.namelist()
        assert len(names)==len(set(names))
        manifest=json.loads(pack.read('manifest.json'))
        assert manifest==build_manifest(rows,entries,release)
        assert manifest['manifestType']=='minecraftModpack' and manifest['minecraft']['version']=='1.20.1'
        pairs=[(f['projectID'],f['fileID']) for f in manifest['files']]
        assert len(pairs)==len(set(pairs))==len(rows) and all(f['required'] is True for f in manifest['files'])
        assert pack.read('modlist.html').decode('utf8')==build_modlist(rows,entries)
        jar_name=f'overrides/mods/graves-and-glory-{release}.jar'
        validate_player_jar(io.BytesIO(pack.read(jar_name)),source_identity())
        player=json.loads((ROOT/'dist/package-manifest.json').read_text())
        with zipfile.ZipFile(ROOT/'dist'/player['fileName']) as source:
            expected={'overrides/'+n for n in source.namelist() if n.startswith('docs/')}
            for n in expected:assert pack.read(n)==source.read(n[len('overrides/'):])
        assert set(names)==expected|{jar_name,'manifest.json','modlist.html'}
    result={'fileName':archive.name,'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'bytes':archive.stat().st_size,
            'listedFiles':len(rows),'status':'Archive integrity passed; real CurseForge import still required'}
    (ROOT/'dist/curseforge-verification.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__':verify()
