"""Prepare a local CurseForge modpack zip from verified release files; never uploads or publishes anything.

Third-party mods and shaders are listed in manifest.json files[] by CurseForge projectID/fileID (CurseForge downloads them),
never bundled. dependencies/curseforge-ids.json holds the IDs; export refuses while any pinned dependency has no resolved ID."""
import hashlib
import html
import json
import sys
import zipfile
from release_config import ROOT, version, source_identity, validate_player_jar
from export_modrinth import MINECRAFT_VERSION, FABRIC_LOADER
import build_skins

IDS_PATH='dependencies/curseforge-ids.json'

def load_ids(rows,ids):
    """Return {slug: entry} for every locked dependency, or raise ValueError listing each unresolved or stale one."""
    entries=ids.get('entries',{});problems=[]
    for row in rows:
        entry=entries.get(row['slug'])
        if entry is None:problems.append(row['slug']+': no entry');continue
        if entry.get('status')!='resolved':problems.append(row['slug']+': unresolved');continue
        good=all(isinstance(entry.get(k),int) and not isinstance(entry.get(k),bool) and entry[k]>0 for k in ('projectID','fileID'))
        if not good:problems.append(row['slug']+': projectID/fileID not positive integers');continue
        if not entry.get('sourceUrl','').startswith('https://www.curseforge.com/'):problems.append(row['slug']+': no CurseForge source URL');continue
        if entry.get('fileName')!=row['fileName']:problems.append(row['slug']+': ID entry is for '+str(entry.get('fileName'))+', pin is '+row['fileName'])
    if problems:raise ValueError('CurseForge IDs unresolved or stale; refusing to export: '+'; '.join(problems))
    return {row['slug']:entries[row['slug']] for row in rows}

def build_manifest(rows,entries,release):
    return {'minecraft':{'version':MINECRAFT_VERSION,'modLoaders':[{'id':'fabric-'+FABRIC_LOADER,'primary':True}]},
            'manifestType':'minecraftModpack','manifestVersion':1,'name':'Graves and Glory','version':release,'author':'Kort',
            'files':[{'projectID':entries[r['slug']]['projectID'],'fileID':entries[r['slug']]['fileID'],'required':True} for r in rows],
            'overrides':'overrides'}

def build_modlist(rows,entries):
    items=''.join('<li><a href="%s">%s</a></li>\n'%(html.escape(entries[r['slug']]['projectUrl'],quote=True),html.escape(entries[r['slug']]['name']))
                  for r in rows)
    return '<ul>\n'+items+'</ul>\n'

def export(argv=()):
    stopgap=build_skins.release_gate(ROOT,list(argv),'export_curseforge')
    release=version()
    rows=json.loads((ROOT/'dependencies/platform-lock.json').read_text(encoding='utf8'))
    entries=load_ids(rows,json.loads((ROOT/IDS_PATH).read_text(encoding='utf8')))
    jar=ROOT/f'build/libs/graves-and-glory-{release}.jar'
    validate_player_jar(jar,source_identity())
    for row in rows:
        if hashlib.sha512((ROOT/row['localPath']).read_bytes()).hexdigest()!=row['hashes']['sha512']:raise RuntimeError('Platform lock is stale: '+row['slug'])
    manifest=json.loads((ROOT/'dist/package-manifest.json').read_text())
    archive=ROOT/'dist'/build_skins.release_archive(manifest,release,stopgap,'export_curseforge')
    if hashlib.sha256(archive.read_bytes()).hexdigest()!=manifest['sha256']:raise RuntimeError('Player archive hash mismatch')
    output=ROOT/'dist'/build_skins.release_name(release,stopgap,'-curseforge.zip');temp=output.with_suffix('.zip.tmp')
    with zipfile.ZipFile(temp,'w',zipfile.ZIP_DEFLATED) as pack:
        pack.writestr('manifest.json',json.dumps(build_manifest(rows,entries,release),indent=2))
        pack.writestr('modlist.html',build_modlist(rows,entries))
        pack.write(jar,'overrides/mods/'+jar.name)
        # Same overrides as the .mrpack: documentation and notices from the verified player archive. No third-party jars.
        with zipfile.ZipFile(archive) as source:
            for name in source.namelist():
                if name.startswith('docs/'):pack.writestr('overrides/'+name,source.read(name))
    temp.replace(output)
    print(json.dumps({'fileName':output.name,'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'listedFiles':len(rows),
                      'status':'Prepared locally; CurseForge import and moderation remain pending'},indent=2))

if __name__=='__main__':export(sys.argv[1:])
