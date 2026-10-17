"""Validate pinned files against official Modrinth records and export usable metadata."""
import hashlib
import json
import pathlib
import urllib.parse
import urllib.request
from release_config import ROOT

HEADERS={'User-Agent':'GravesAndGlory/0.3.2 (local modpack dependency verification)'}
def get(path):
    with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/'+path,headers=HEADERS),timeout=30) as response:
        return json.load(response)

def review():
    dependencies=json.loads((ROOT/'dependencies/manifest.json').read_text(encoding='utf8'))
    ids=[d['versionId'] for d in dependencies]
    versions=get('versions?'+urllib.parse.urlencode({'ids':json.dumps(ids)}))
    api=get('project/fabric-api/version/'+urllib.parse.quote('0.92.2+1.20.1',safe=''))
    versions.append(api)
    projects=get('projects?'+urllib.parse.urlencode({'ids':json.dumps(sorted({v['project_id'] for v in versions}))}))
    by_id={v['id']:v for v in versions};project_by_id={p['id']:p for p in projects}
    api_path=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.2+1.20.1').rglob('fabric-api-0.92.2+1.20.1.jar'))
    dependencies.append({'slug':'fabric-api','versionId':api['id'],'path':str(api_path),'fileName':api_path.name})
    rows=[]
    client_only={'iris','sodium','immediatelyfast','entity-model-features','entitytexturefeatures','presence-footsteps'}
    for dependency in dependencies:
        record=by_id[dependency['versionId']];project=project_by_id[record['project_id']]
        file=next(f for f in record['files'] if f['filename']==dependency['fileName'])
        path=ROOT/dependency['path'];data=path.read_bytes()
        for algorithm in ('sha1','sha512'):
            if hashlib.new(algorithm,data).hexdigest()!=file['hashes'][algorithm]:
                raise RuntimeError('Upstream hash mismatch: '+dependency['slug'])
        if dependency.get('sha256') and hashlib.sha256(data).hexdigest()!=dependency['sha256']:
            raise RuntimeError('Local pin mismatch: '+dependency['slug'])
        kind='mod' if path.suffix=='.jar' else 'shader' if '/shaders/' in dependency['path'] else 'resourcepack'
        server='unsupported' if kind!='mod' or dependency['slug'] in client_only else 'required'
        if kind=='mod' and ('1.20.1' not in record['game_versions'] or 'fabric' not in record['loaders']):
            raise RuntimeError('No upstream Fabric 1.20.1 support: '+dependency['slug'])
        rows.append({'slug':dependency['slug'],'projectId':record['project_id'],'versionId':record['id'],
                     'version':record['version_number'],'fileName':file['filename'],'localPath':dependency['path'],
                     'fileSize':len(data),'hashes':file['hashes'],'sha256':hashlib.sha256(data).hexdigest(),
                     'downloads':[file['url']],'kind':kind,'env':{'client':'required','server':server},
                     'license':project['license'],'projectUrl':'https://modrinth.com/'+project['project_type']+'/'+project['slug'],
                     'sourceUrl':project.get('source_url'),'gameVersions':record['game_versions'],
                     'redistributionReview':'Use upstream Modrinth download; preserve notices and author conditions',
                     'curseforgeProjectId':None,'curseforgeFileId':None})
    return rows

if __name__=='__main__':
    rows=review()
    (ROOT/'dependencies/platform-lock.json').write_text(json.dumps(rows,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
    print('Verified',len(rows),'files against official SHA-1/SHA-512 and Fabric 1.20.1 mod metadata')
