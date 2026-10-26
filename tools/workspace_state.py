"""Inventory/checkpoint registered worktrees. Never deletes or resets a checkout."""
import argparse, datetime, hashlib, json, pathlib, subprocess, tarfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args])
def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''): h.update(block)
    return h.hexdigest()
def inventory():
    entries = []
    base = git(ROOT, 'rev-parse', 'HEAD').decode().strip()
    for record in git(ROOT, 'worktree', 'list', '--porcelain').decode().strip().split('\n\n'):
        fields = dict(line.split(' ', 1) if ' ' in line else (line, True) for line in record.splitlines())
        p = pathlib.Path(fields['worktree'])
        changes = git(p, 'status', '--porcelain=v1', '-uall').decode().splitlines()
        ignored = git(p, 'ls-files', '--others', '--ignored', '--exclude-standard', '-z').decode().split('\0')
        entries.append(dict(path=str(p), head=fields['HEAD'], branch=fields.get('branch'),
            unique_commits=int(git(p, 'rev-list', '--count', base+'..HEAD')), changes=changes,
            ignored_files=sum(bool(n) for n in ignored), owner='unknown—preserve', status='unknown',
            task='Preserve pending owner/disposition review', graph=str(p/'graphify-out/graph.json'),
            external=not p.is_relative_to(ROOT.parent)))
    other=[dict(path=str(ROOT.parent/name),kind=kind,owner='unknown—preserve',status='retained in place; not a registered Git checkout')
           for name,kind in [('minecraft','legacy'),('bannerbound-pre-rename','legacy'),('bannerbound-diag','diagnostic'),('flop-formation','separate project')]
           if (ROOT.parent/name).exists()]
    return dict(generated_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(), integration_base=str(ROOT), base_head=base, worktrees=entries, other_directories=other)
def checkpoint(dest):
    dest.mkdir(parents=True, exist_ok=False)
    data = inventory()
    (dest/'inventory.json').write_text(json.dumps(data, indent=2), encoding='utf8')
    # Explicit heads include detached worktrees without moving refs.
    git(ROOT, 'bundle', 'create', str(dest/'history.bundle'), '--all', *sorted({w['head'] for w in data['worktrees']}))
    git(ROOT, 'bundle', 'verify', str(dest/'history.bundle'))
    manifest = []
    retained = []
    with tarfile.open(dest/'local-files.tar.gz', 'w:gz', compresslevel=1) as tar:
        for w in data['worktrees']:
            p=pathlib.Path(w['path']); name=p.name
            for label, args in [('staged', ['diff','--cached','--binary']),('unstaged',['diff','--binary'])]:
                (dest/(name+'-'+label+'.patch')).write_bytes(git(p,*args))
            tracked=git(p,'diff','--name-only','HEAD','-z').decode().split('\0')
            untracked=git(p,'ls-files','--others','--exclude-standard','-z').decode().split('\0')
            ignored=git(p,'ls-files','--others','--ignored','--exclude-standard','-z').decode().split('\0')
            selected=set(tracked+untracked)
            for n in filter(None,ignored):
                f=p/n
                if not f.is_file(): continue
                # Preserve project decisions, references, run scripts and captures; retain large runtime trees in place.
                keep=(n.startswith(('.superpowers/','.claude/')) or
                      (n.startswith(('work/','evidence/','clips/','dist/')) and f.suffix.lower() in
                       {'.md','.txt','.json','.py','.ps1','.sh','.png','.jpg','.jpeg','.bbmodel','.jar','.mrpack','.zip'}))
                if keep and f.stat().st_size <= 64*1024*1024: selected.add(n)
                else: retained.append(dict(path=str(f),size=f.stat().st_size,disposition='retain-in-place; blocks archival'))
            for n in sorted(filter(None,selected)):
                f=p/n
                if not f.is_file(): continue
                if f.name.startswith('.env') or f.suffix in {'.pem','.key'}: continue
                arc=name+'/'+n
                digest=sha(f)
                tar.add(f,arcname=arc,recursive=False)
                if sha(f)!=digest: raise RuntimeError('Concurrent write: '+str(f))
                manifest.append(dict(original=str(f),member=arc,sha256=digest,size=f.stat().st_size))
    (dest/'files.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
    (dest/'retained-in-place.json').write_text(json.dumps(retained,indent=2),encoding='utf8')
    # Read every archived member and compare; extract representative files using explicit safe paths.
    with tarfile.open(dest/'local-files.tar.gz') as tar:
        for row in manifest:
            if hashlib.sha256(tar.extractfile(row['member']).read()).hexdigest()!=row['sha256']:
                raise RuntimeError('Archive readback failed: '+row['member'])
        samples=[next((r for r in manifest if '/work/' in r['member'] and r['member'].endswith('.png') and r['size']>1024),None),
                 next((r for r in manifest if r['member'].endswith('CLAUDE.md') and r['size']>1024),None)]
        for i,row in enumerate(filter(None,samples)):
            target=dest/('restore-sample-'+str(i)); target.write_bytes(tar.extractfile(row['member']).read())
            assert sha(target)==row['sha256']
    (dest/'RESTORE.md').write_text('Private local checkpoint; do not publish settings or ledgers.\n\n'
        'Clone history.bundle into a NEW directory and checkout the head in inventory.json. '
        'Apply the staged patch with git apply --index, then the unstaged patch with git apply. '
        'Restore selected files from local-files.tar.gz by files.json original/member mapping. '
        'All members were hash-verified; two representative files were extracted and verified. '
        'retained-in-place.json lists bulk assets NOT backed up here. Originals must remain until separately archived and verified. '
        'No worktree is eligible for deletion from this checkpoint alone.\n',encoding='utf8')
    print(json.dumps(dict(checkpoint=str(dest),archived_files=len(manifest),retained_files=len(retained),verified=True)))
if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--checkpoint',type=pathlib.Path); parser.add_argument('--output',type=pathlib.Path)
    args=parser.parse_args()
    if args.checkpoint: checkpoint(args.checkpoint.resolve())
    else:
        data=inventory()
        target=args.output or ROOT.parent/'workspace-state.json'
        if target.exists():
            old={w['path']:w for w in json.loads(target.read_text(encoding='utf8'))['worktrees']}
            for w in data['worktrees']:
                for k in ('owner','status','task'):
                    if w['path'] in old: w[k]=old[w['path']].get(k,w[k])
        target.write_text(json.dumps(data,indent=2),encoding='utf8'); print(target)
