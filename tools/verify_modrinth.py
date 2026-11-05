"""Read back the local export and compare every download/override to reviewed input."""
import hashlib
import io
import json
import zipfile
from export_modrinth import validate_index
from release_files import resolve_local_path
from release_config import ROOT, version, source_identity, validate_player_jar

def verify():
    archive=ROOT/f'dist/graves-and-glory-{version()}.mrpack'
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text())
    lock={({'mod':'mods','shader':'shaderpacks','resourcepack':'resourcepacks'}[r['kind']]+'/'+r['fileName']):r for r in rows}
    with zipfile.ZipFile(archive) as pack:
        assert pack.testzip() is None
        names=pack.namelist()
        assert len(names)==len(set(names))
        index=json.loads(pack.read('modrinth.index.json'));validate_index(index)
        assert index['versionId']==version() and index['dependencies']=={'minecraft':'1.20.1','fabric-loader':'0.16.14'}
        assert len(index['files'])==len(lock)
        for file in index['files']:
            row=lock[file['path']];data=resolve_local_path(ROOT,row['localPath']).read_bytes()
            assert file['hashes']=={a:hashlib.new(a,data).hexdigest() for a in ('sha1','sha512')}
            assert file['downloads']==row['downloads'] and file['env']==row['env'] and file['fileSize']==len(data)
        jar_name=f'overrides/mods/graves-and-glory-{version()}.jar'
        validate_player_jar(io.BytesIO(pack.read(jar_name)),source_identity())
        player=json.loads((ROOT/'dist/package-manifest.json').read_text())
        with zipfile.ZipFile(ROOT/'dist'/player['fileName']) as source:
            expected={'overrides/'+n for n in source.namelist() if n.startswith('docs/')}
            for n in expected:assert pack.read(n)==source.read(n[len('overrides/'):])
        assert set(names)==expected|{jar_name,'modrinth.index.json','server-overrides/server.properties','server-overrides/SERVER-README.txt'}
        server=pack.read('server-overrides/server.properties').decode()
        assert 'online-mode=true' in server and 'server-ip=127.0.0.1' in server
    result={'fileName':archive.name,'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'bytes':archive.stat().st_size,
            'verifiedDownloads':len(lock),'status':'Archive integrity passed; real launcher install still required'}
    (ROOT/'dist/modrinth-verification.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__':verify()
