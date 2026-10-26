"""Install only the project-owned graph advisory; preserve unrelated Claude settings."""
import argparse, datetime, json, pathlib, shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
def install(checkout):
    path=checkout/'.claude/settings.json'; path.parent.mkdir(exist_ok=True)
    config=json.loads(path.read_text(encoding='utf8')) if path.exists() else {}
    if path.exists():
        backup=checkout/'work/optimization/settings-backups'/datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f')
        backup.mkdir(parents=True); shutil.copy2(path,backup/'settings.json')
    hooks=config.setdefault('hooks',{}); entries=[]
    for entry in hooks.get('PreToolUse',[]):
        kept=[h for h in entry.get('hooks',[]) if 'graphify hook-guard' not in h.get('command','') and 'graph_hook.py' not in h.get('command','')]
        if kept: entries.append(dict(entry,hooks=kept))
    entries.append({'matcher':'Bash','hooks':[{'type':'command','command':'python "'+(ROOT/'tools/graph_hook.py').as_posix()+'"','timeout':10}]})
    hooks['PreToolUse']=entries
    path.write_text(json.dumps(config,indent=2)+'\n',encoding='utf8')
    print(path)
if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('checkout',type=pathlib.Path,nargs='?',default=ROOT); a=p.parse_args(); install(a.checkout.resolve())
