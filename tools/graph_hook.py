"""Advisory Claude hook. Check this checkout only when a graph query is attempted."""
import hashlib,json,pathlib,re,sys
QUERY=re.compile(r'\bgraphify\s+(query|path|explain|affected)\b')
ARG=r'("[^"]+"|\'[^\']+\'|[^\s;&|]+)'
def _path(raw,base):
    raw=raw.strip('"\'')
    m=re.match(r'^/([A-Za-z])/(.*)$',raw)  # Git Bash path (/c/Users/...) -> C:/Users/...
    if m: raw=m.group(1).upper()+':/'+m.group(2)
    p=pathlib.Path(raw)
    return (p if p.is_absolute() else base/p).resolve()
def checkout_root(command,cwd):
    """The checkout whose graph the command will read: its --graph argument, else a leading `cd`, else the session cwd. Walks up to the
    worktree root, so a session in a subfolder is not misread as having no graph (a worktree has a .git file, a clone a .git folder)."""
    base=pathlib.Path(cwd).resolve() if cwd else pathlib.Path.cwd().resolve()
    g=re.search(r'--graph(?:=|\s+)'+ARG,command)
    cd=re.match(r'\s*cd\s+'+ARG+r'\s*(?:&&|;)',command)
    if g:
        graph=_path(g.group(1),_path(cd.group(1),base) if cd else base)
        if graph.parent.name=='graphify-out': return graph.parent.parent
    start=_path(cd.group(1),base) if cd else base
    for d in (start,*start.parents):
        if (d/'.git').exists(): return d
    return start
def advisory(payload):
    command=payload.get('tool_input',{}).get('command','')
    if not QUERY.search(command): return None
    root=checkout_root(command,payload.get('cwd'))
    out=root/'graphify-out'; state=out/'source-state.json'; graph=out/'graph.json'
    try:
        saved=json.loads(state.read_text())
        # Import the canonical fingerprint code, but resolve all inputs in the caller's checkout.
        import graph_state
        graph_state.ROOT=root
        fresh=saved['inputs']==graph_state.inputs() and saved['graph_sha256']==hashlib.sha256(graph.read_bytes()).hexdigest()
    except (OSError,ValueError,KeyError): fresh=False
    if fresh: return None
    return {'hookSpecificOutput':{'hookEventName':'PreToolUse','additionalContext':
        'The graph in '+str(root)+' is missing or stale. Use rg/direct source now or refresh the checkout-local AST graph (python tools/graph_state.py refresh). Do not loop graph queries or substitute another branch graph.'}}
if __name__=='__main__':
    try:
        result=advisory(json.load(sys.stdin))
        if result: print(json.dumps(result))
    except Exception as exc:
        # Never strand normal Claude tools; failures are visible, not silent query loops.
        print('Graph freshness hook unavailable: '+str(exc)+'; use direct source.',file=sys.stderr)
