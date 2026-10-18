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
# Phase 2 Houses (spec 6.1). Until troops gain house and rarity columns, a troop's House and rarity come from houses.troops,
# which lists Common infantry, Common ranged, Rare, Rare, Epic, Legendary in that order.
POSITION_RARITY=['common','common','rare','rare','epic','legendary']
squad=data['rules'][0]['squadSize']
houses={h['id']:h for h in data['houses']}
rarities={r['id']:r for r in data['rarities']}
if set(rarities)!=set(POSITION_RARITY): errors.append('rarities: the rows must be common, rare, epic and legendary')
houseOf,rarityOf={},{}
for h in houses.values():
    if h['glyph']!=f'gravesandglory:textures/gui/house/{h["id"]}.png': errors.append(f'houses/{h["id"]}: glyph must be gravesandglory:textures/gui/house/{h["id"]}.png')
    ids=h['troops'].split(',')
    if len(ids)!=len(POSITION_RARITY): errors.append(f'houses/{h["id"]}: troops must list {len(POSITION_RARITY)} ids')
    for i,t in enumerate(ids[:len(POSITION_RARITY)]):
        if t in houseOf: errors.append(f'houses/{h["id"]}: {t} is already in {houseOf[t]}')
        houseOf[t]=h['id'];rarityOf[t]=POSITION_RARITY[i]
for h in houses:
    tiers=sorted((t for t in data['houseTraits'] if t['house']==h),key=lambda t:t['tier'])
    if [t['tier'] for t in tiers]!=[1,2,3]: errors.append(f'houseTraits: {h} needs exactly tiers 1, 2 and 3')
    if len({t['mechanic'] for t in tiers})>1: errors.append(f'houseTraits: {h} uses one mechanic on every tier')
    if any(a['slots']>=b['slots'] for a,b in zip(tiers,tiers[1:])): errors.append(f'houseTraits: {h} slots must rise with the tier')
for t in data['houseTraits']:
    if t['house'] not in houses: errors.append(f'houseTraits/{t["id"]}: house does not resolve')
    if t['id']!=f'{t["house"]}_{t["tier"]}': errors.append(f'houseTraits/{t["id"]}: id must be <house>_<tier>')
MECHANICS={'none','shield','mend','blight','stance','pierce','cleave','onhit_slow','pounce','aura','slam','fearless','mist'}
TRIGGERS={'passive','onhit','cast','hold','timer','charge_contact'}
for a in data['abilities']:
    if a['mechanic'] not in MECHANICS: errors.append(f'abilities/{a["id"]}: mechanic {a["mechanic"]} is not in the closed list')
    if a['trigger'] not in TRIGGERS: errors.append(f'abilities/{a["id"]}: trigger {a["trigger"]} is not in the closed list')
    if a['target'] not in ('enemies','allies'): errors.append(f'abilities/{a["id"]}: target must be enemies or allies')
    if a['mechanic']=='stance' and a['amount']!=0: errors.append(f'abilities/{a["id"]}: stance multipliers live only in counters, so amount must be 0.0')
    if len(a['text'])>90: errors.append(f'abilities/{a["id"]}: text is {len(a["text"])} characters, over 90')
CONDITIONS={'always','raised_front','lowered_front','front','charge_impact','charge','hold','flank','no_sky','in_mist'}
# A counter tag is a dynamic tag, an ability id or a troop tag (spec section 3 list until the troops sheet carries tags).
TAGS={'melee','arrow','shot','magic','nonsiege','ranged','charging','holding','structure','any'}|{'shield','spear','beast','cavalry','siege','large','small','fearless'}|{a['id'] for a in data['abilities']}
counterIds={c['id'] for c in data['counters']}
for c in data['counters']:
    if c['condition'] not in CONDITIONS: errors.append(f'counters/{c["id"]}: condition {c["condition"]} is not in the closed list')
    for tag in c['attackerTag'].split('+')+c['targetTag'].split('+'):
        if tag not in TAGS: errors.append(f'counters/{c["id"]}: tag {tag} does not resolve')
    if c['replaces']!='none' and any(r not in counterIds or r==c['id'] for r in c['replaces'].split('|')): errors.append(f'counters/{c["id"]}: replaces must be none or a | list of other counter ids')
    if c['multiplier']<=0: errors.append(f'counters/{c["id"]}: multiplier must be above 0')
cd=data['combatDiscipline'][0]
if not 0<cd['multiplierMin']<=1<=cd['multiplierMax']: errors.append('combatDiscipline: the counter clamp must hold 1 (0 < multiplierMin <= 1 <= multiplierMax)')
if len(data['recruitment'])!=1: errors.append('recruitment: exactly one row')
if any(data['campaign'][0].get(k,-1)<0 for k in ('completionShards','victoryShards','completionRenown','victoryRenown')): errors.append('campaign: completionShards, victoryShards, completionRenown and victoryRenown must be 0 or more')
cap=data['recruitment'][0]['capPct']
for r in rarities.values():
    if not 0<=r['starPct2']<=r['starPct3']<=cap+1e-9: errors.append(f'rarities/{r["id"]}: star bonuses must rise and stay within recruitment.capPct')
    if r['slotWeight']<1 or r['bannerMax']<1: errors.append(f'rarities/{r["id"]}: slotWeight and bannerMax must be at least 1')
stages={}
for e in data['evolutions']:
    if e['id']!=f'{e["troop"]}_{e["stage"]}' or e['stage'] not in (2,3): errors.append(f'evolutions/{e["id"]}: id must be <troop>_<stage> for stage 2 or 3'); continue
    if rarityOf.get(e['troop']) not in ('common','rare'): errors.append(f'evolutions/{e["id"]}: only a House Common or Rare evolves'); continue
    stages.setdefault(e['troop'],{})[e['stage']]=e
    # The hard cap (spec 4.4): star 3 plus the stage bonus stays within capPct on every stat.
    if rarities.get(rarityOf[e['troop']],{}).get('starPct3',0)+max(e['healthPct'],e['damagePct'])>cap+1e-9: errors.append(f'evolutions/{e["id"]}: starPct3 + max(healthPct, damagePct) passes recruitment.capPct {cap}')
for t,r in rarityOf.items():
    if r in ('common','rare') and sorted(stages.get(t,{}))!=[2,3]: errors.append(f'evolutions: {t} needs one stage 2 and one stage 3 row')
for t,s in stages.items():
    if len(s)==2 and (s[3]['healthPct']<s[2]['healthPct'] or s[3]['damagePct']<s[2]['damagePct']): errors.append(f'evolutions: {t} percentages are cumulative, so stage 3 cannot be below stage 2')
for sheet,keys in (('mastery',('kills','matches')),('renown',('points',))):
    rows=data[sheet]
    if [r['level'] for r in rows]!=list(range(1,len(rows)+1)): errors.append(f'{sheet}: levels must run 1, 2, 3 and on, in order')
    for k in keys:
        if any(a[k]>=b[k] for a,b in zip(rows,rows[1:])): errors.append(f'{sheet}: {k} must rise with the level')
for r in data['renown']:
    if r['reward']=='legendary' and r['points']!=data['recruitment'][0]['renownForLegendary']: errors.append(f'renown/{r["id"]}: the legendary level must sit at recruitment.renownForLegendary')
difficulties={b['id'] for b in data['botCommand']}
for b in data['botBanners']:
    name=f'botBanners/{b["id"]}';parts=b['id'].split('_')
    mixed=parts[0]=='mix' and len(parts)==4
    allowed={parts[1],parts[2]} if mixed else {b['house']}
    if b['house'] not in houses or b['difficulty'] not in difficulties: errors.append(f'{name}: house or difficulty does not resolve')
    if b['id']!=(f'mix_{b["house"]}_{parts[2]}_{b["difficulty"]}' if mixed else f'{b["house"]}_{b["difficulty"]}') or not allowed<=set(houses): errors.append(f'{name}: id must be <house>_<difficulty>, or mix_<house>_<second house>_<difficulty> with house the first House')
    slots=b['slots'].split(',')
    if len(slots)!=squad: errors.append(f'{name}: {len(slots)} entries, not {squad}')
    pending,weights,units,last=0,0,{},None
    for s in slots:
        if s=='^':
            if pending==0: errors.append(f'{name}: a ^ that follows no Epic or Legendary'); break
            pending-=1;continue
        if pending: errors.append(f'{name}: {last} is missing a ^'); break
        if s not in rarityOf or houseOf[s] not in allowed: errors.append(f'{name}: {s} is not a troop of this banner\'s Houses'); break
        last=s;w=rarities.get(rarityOf[s],{}).get('slotWeight',1);weights+=w;pending=w-1;units[rarityOf[s]]=units.get(rarityOf[s],0)+1
    else:
        if pending: errors.append(f'{name}: {last} is missing a ^')
        if weights!=squad: errors.append(f'{name}: slot weights sum to {weights}, not {squad}')
        for r,n in units.items():
            if n>rarities.get(r,{}).get('bannerMax',squad): errors.append(f'{name}: {n} {r} units, over bannerMax {rarities[r]["bannerMax"]}')
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
