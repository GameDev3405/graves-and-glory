"""Export pinned file identities without treating license metadata as approval."""
import hashlib
import json
import pathlib
import sys
from release_config import ROOT, version

rows=[]
for dep in json.loads((ROOT/'dependencies/manifest.json').read_text(encoding='utf8')):
    file=ROOT/dep['path']
    actual=hashlib.sha256(file.read_bytes()).hexdigest()
    if actual!=dep['sha256']:raise RuntimeError('Dependency pin mismatch: '+dep['slug'])
    environment=(dep.get('manifest') or {}).get('environment')
    rows.append({'name':dep['title'],'slug':dep['slug'],'version':dep['version'],
                 'file':dep['fileName'],'sha256':actual,'sha512':dep.get('sha512'),
                 'project':dep['project'],'modrinthVersionId':dep.get('versionId'),
                 'download':dep.get('download'),'licenseMetadata':dep['licenseMetadata']['id'],
                 'fabricEnvironment':environment,'curseforgeProjectId':None,
                 'curseforgeFileId':None,'platformReview':'Exact platform file/permission review still required'})
api=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.2+1.20.1').rglob('fabric-api-0.92.2+1.20.1.jar'))
rows.append({'name':'Fabric API','version':'0.92.2+1.20.1','file':api.name,
             'sha256':hashlib.sha256(api.read_bytes()).hexdigest(),
             'sha512':hashlib.sha512(api.read_bytes()).hexdigest(),
             'project':'https://modrinth.com/mod/fabric-api','licenseMetadata':'Apache-2.0',
             'modrinthVersionId':None,'curseforgeProjectId':None,'curseforgeFileId':None,
             'platformReview':'Resolve exact platform version IDs'})
out=pathlib.Path(sys.argv[1])
out.write_text(json.dumps({'gameVersion':version(),'status':'Pinned local inventory; not platform approval',
                          'thirdPartyFiles':rows},indent=2,ensure_ascii=False)+'\n',encoding='utf8')
print('Recorded',len(rows),'pinned third-party files; CurseForge mapping remains open')
