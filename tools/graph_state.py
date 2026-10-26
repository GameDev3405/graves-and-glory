"""Local AST-only graph refresh and content-based freshness, including dirty files."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'graphify-out'
def inputs():
    names=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
    return {n:hashlib.sha256((ROOT/n).read_bytes()).hexdigest() for n in sorted(set(names))
        if n and (ROOT/n).is_file() and not n.startswith(('graphify-out/','design-archive/'))
        and (pathlib.Path(n).suffix in {'.java','.py','.gradle','.json','.sh','.ps1','.toml','.properties'} or n=='.graphifyignore')}
def check():
    stamp=OUT/'source-state.json'
    if not stamp.exists() or not (OUT/'graph.json').exists(): return False
    saved=json.loads(stamp.read_text()); return saved['inputs']==inputs() and saved['graph_sha256']==hashlib.sha256((OUT/'graph.json').read_bytes()).hexdigest()
def refresh():
    env=dict(os.environ,GRAPHIFY_BACKEND='none',PYTHONIOENCODING='utf-8')
    before=inputs()
    # A fresh output avoids incremental preservation of deleted paths/old semantic nodes.
    stage=ROOT/'work'/('graph-refresh-'+str(os.getpid()))
    stage.mkdir(parents=True,exist_ok=False)
    log=stage/'refresh.log'
    with log.open('w',encoding='utf8') as out:
        for args in [['extract',str(ROOT),'--code-only','--no-cluster','--max-workers','2','--out',str(stage)],
                     ['cluster-only',str(ROOT),'--graph',str(stage/'graphify-out/graph.json'),'--no-label','--no-viz']]:
            result=subprocess.run(['graphify',*args],cwd=ROOT,env=env,stdout=out,stderr=subprocess.STDOUT)
            if result.returncode: raise RuntimeError('Graph refresh failed; see '+str(log))
    if before!=inputs(): raise RuntimeError('Source changed during extraction; old graph retained')
    graph=json.loads((stage/'graphify-out/graph.json').read_text(encoding='utf8'))
    if not graph.get('nodes'): raise RuntimeError('Empty graph; old graph retained')
    OUT.mkdir(exist_ok=True)
    for p in (stage/'graphify-out').iterdir():
        if p.is_file(): shutil.copy2(p,OUT/p.name)
    (OUT/'source-state.json').write_text(json.dumps(dict(inputs=before,graph_sha256=hashlib.sha256((OUT/'graph.json').read_bytes()).hexdigest(),
        head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip()),indent=2))
    print('Graph refreshed locally; '+str(len(graph['nodes']))+' nodes; log '+str(log))
if __name__=='__main__':
    p=argparse.ArgumentParser(); p.add_argument('action',choices=['check','refresh']); a=p.parse_args()
    if a.action=='refresh': refresh()
    else:
        fresh=check(); print('fresh' if fresh else 'stale/missing: read known files directly or refresh; do not loop queries'); sys.exit(0 if fresh else 1)
