import json, pathlib, sys
root = pathlib.Path(__file__).resolve().parents[1]
data = json.loads((root/'sheets/design.json').read_text())
errors=[]
cells=0
for sheet, rows in data.items():
    if not rows: errors.append(f'{sheet}: empty sheet'); continue
    columns=set(rows[0])
    ids=set()
    for row in rows:
        if set(row)!=columns: errors.append(f'{sheet}/{row.get("id")}: inconsistent columns')
        if row.get('id') in ids: errors.append(f'{sheet}: duplicate id')
        ids.add(row.get('id'))
        for col in columns:
            cells+=1
            if col not in row or row[col] is None or row[col]=='': errors.append(f'{sheet}/{row.get("id")}/{col}: unfilled')
factions={r['id'] for r in data['factions']}
troops={r['id']:r for r in data['troops']}
for r in troops.values():
    if r['faction'] not in factions: errors.append(f'{r["id"]}: faction does not resolve')
for f in data['factions']:
    refs=f['troops'].split(',')
    for t in refs:
        if t not in troops or troops[t]['faction']!=f['id']: errors.append(f'{f["id"]}: troop {t} does not resolve')
    if sum(troops[t]['count'] for t in refs if t in troops)!=data['rules'][0]['squadSize']: errors.append('squad count mismatch')
if data['rules'][0]['playersPerTeam']*len(factions)!=data['rules'][0]['maxPlayers']: errors.append('capacity mismatch')
groups={g['id'] for g in data.get('groups',[])}
orders={o['id'] for o in data['orders']}
for t in data['troops']:
    if t.get('group') not in groups: errors.append(t['id']+': group does not resolve')
for row in data.get('commandInterface',[]):
    for group in row['selection'].split(','):
        if group not in groups: errors.append('command selection does not resolve: '+group)
    for order in row['orders'].split(','):
        if order not in orders: errors.append('command order does not resolve: '+order)
for r in data['troops']:
    if r['texture'].startswith('gravesandglory:'):
        texture=root/'src/main/resources/assets/gravesandglory'/r['texture'].split(':',1)[1]
        if not texture.exists(): errors.append(f'{r["id"]}: missing texture')
for r in data['ui']:
    if r['id']=='command':
        formationIds={f['id'] for f in data['formations']}
        for id in r['bottom'].split(','):
            if id not in formationIds: errors.append('UI formation reference missing: '+id)
if errors:
    print('\n'.join(errors)); sys.exit(1)
def typ(v): return 'boolean' if isinstance(v,bool) else 'int' if isinstance(v,int) else 'double' if isinstance(v,float) else 'String'
def literal(v): return json.dumps(v,ensure_ascii=True)
# Validated above, but process metadata rather than game data: never shipped as Spec records.
NOT_SHIPPED={'checks','revisionChecks','playtestMilestone'}
out=['package gg.gravesandglory;','public final class Spec {']
for sheet,rows in data.items():
    if sheet in NOT_SHIPPED: continue
    name=sheet[0].upper()+sheet[1:]+'Row'
    cols=list(rows[0])
    out.append(f'public record {name}('+','.join(typ(rows[0][c])+' '+c for c in cols)+') {}')
    out.append(f'public static {name}[] {sheet.upper()} = new {name}[] {{'+','.join('new '+name+'('+','.join(literal(r[c]) for c in cols)+')' for r in rows)+'};')
out.append('}')
dest=root/'src/main/java/gg/gravesandglory/Spec.java'
dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text('\n'.join(out))
report={'filledCells':cells,'unfilledCells':[],'unresolvedReferences':[],'runtimeVerification':'pending; tracked separately in verification.json'}
(root/'sheets/preflight.json').write_text(json.dumps(report,indent=2))
print(f'Preflight clean: {cells} filled cells; every row generates a Java record. Runtime verification pending.')
