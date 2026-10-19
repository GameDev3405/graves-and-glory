import json, os, pathlib, re, sys, zipfile
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
# Sides (spec 1.1): factions holds the banner colours. Team 0 and team 1 read their positions from rows 0 and 1;
# colour, name and glyph come from the side each team fields (Battle.side).
GLYPHS=('sun','moon','star','wave')
sides=data['factions']
if len(sides)<2: errors.append('factions: at least two side rows, since teams 0 and 1 read their positions from rows 0 and 1')
for f in sides:
    if f['glyph'] not in GLYPHS: errors.append(f'factions/{f["id"]}: glyph {f["glyph"]} is not one of {", ".join(GLYPHS)}')
if len({f['glyph'] for f in sides})!=len(sides) or len({f['color'] for f in sides})!=len(sides): errors.append('factions: every side needs its own colour and its own glyph')
troops={r['id']:r for r in data['troops']}
if data['rules'][0]['playersPerTeam']*2!=data['rules'][0]['maxPlayers']: errors.append('capacity mismatch: maxPlayers must be playersPerTeam for each of the two teams')
groups={g['id'] for g in data.get('groups',[])}
orders={o['id'] for o in data['orders']}
for t in data['troops']:
    if t.get('group') not in groups: errors.append(t['id']+': group does not resolve')
for row in data.get('commandInterface',[]):
    for group in row['selection'].split(','):
        if group not in groups: errors.append('command selection does not resolve: '+group)
    for order in row['orders'].split(','):
        if order not in orders: errors.append('command order does not resolve: '+order)
# Troop textures. A gravesandglory: path must exist in the mod's assets. A minecraft: path must be one of VANILLA_TEXTURES, the
# vanilla paths the sheet may name, each checked by hand against the Minecraft client jar of build.gradle (1.20.1); when Loom has
# cached that jar (after the first build) the path must also be an entry of it, so a misspelled or renamed vanilla texture fails
# here instead of showing as a missing-texture model in game. The list keeps the result the same on a machine without the jar:
# naming a new vanilla texture in the sheet means adding it here.
VANILLA_TEXTURES=('textures/entity/bear/polarbear.png','textures/entity/illager/evoker.png','textures/entity/illager/pillager.png',
    'textures/entity/illager/ravager.png','textures/entity/illager/vindicator.png','textures/entity/iron_golem/iron_golem.png',
    'textures/entity/piglin/zombified_piglin.png','textures/entity/skeleton/skeleton.png','textures/entity/skeleton/wither_skeleton.png',
    'textures/entity/spider/spider.png','textures/entity/wolf/wolf.png','textures/entity/zombie/drowned.png')
mcVersion=re.search(r"minecraft 'com\.mojang:minecraft:([^']+)'",(root/'build.gradle').read_text()) if (root/'build.gradle').is_file() else None
mcVersion=mcVersion.group(1) if mcVersion else None
clientJar=pathlib.Path(os.environ.get('GRADLE_USER_HOME') or pathlib.Path.home()/'.gradle')/'caches/fabric-loom'/(mcVersion or '-')/'minecraft-client.jar'
vanilla=None
if mcVersion and clientJar.is_file():
    with zipfile.ZipFile(clientJar) as jar: vanilla={n for n in jar.namelist() if n.startswith('assets/minecraft/textures/')}
for r in data['troops']:
    space,_,path=r['texture'].partition(':')
    if space=='gravesandglory':
        if not (root/'src/main/resources/assets/gravesandglory'/path).exists(): errors.append(f'{r["id"]}: missing texture')
    elif space=='minecraft':
        if path not in VANILLA_TEXTURES: errors.append(f'troops/{r["id"]}: texture {r["texture"]} is not in VANILLA_TEXTURES (tools/preflight.py), the vanilla paths checked against the {mcVersion} client jar')
        elif vanilla is not None and 'assets/minecraft/'+path not in vanilla: errors.append(f'troops/{r["id"]}: texture {r["texture"]} is not in the Minecraft {mcVersion} client jar ({clientJar})')
    else: errors.append(f'troops/{r["id"]}: texture {r["texture"]} must be a gravesandglory: or a minecraft: path')
for r in data['ui']:
    if r['id']=='command':
        formationIds={f['id'] for f in data['formations']}
        for id in r['bottom'].split(','):
            if id not in formationIds: errors.append('UI formation reference missing: '+id)
# Phase 2 Houses (spec 6.1): a troop's House, rarity, ability and evolve line are its own columns.
squad=data['rules'][0]['squadSize']
houses={h['id']:h for h in data['houses']}
rarities={r['id']:r for r in data['rarities']}
RARITY_ORDER=['common','rare','epic','legendary']
if [r['id'] for r in data['rarities']]!=RARITY_ORDER: errors.append('rarities: the rows must be common, rare, epic, legendary, in that order (validBanner checks bannerMax from the last row up, rarest first)')
abilityIds={a['id'] for a in data['abilities']}
evolveTroops={e['troop'] for e in data['evolutions']}
HOUSE_SHAPE={'common':2,'rare':2,'epic':1,'legendary':1}
houseOf,rarityOf={},{}
for t in data['troops']:
    name=f'troops/{t["id"]}'
    if t['house'] not in houses or t['rarity'] not in rarities: errors.append(f'{name}: house {t["house"]} or rarity {t["rarity"]} does not resolve'); continue
    houseOf[t['id']]=t['house'];rarityOf[t['id']]=t['rarity']
    if t['ability'] not in abilityIds: errors.append(f'{name}: ability {t["ability"]} does not resolve')
    if t['rarity'] in ('common','rare'):
        if t['evolveLine'] not in evolveTroops: errors.append(f'{name}: a Common or Rare needs an evolveLine that names an evolutions troop, not {t["evolveLine"]}')
    elif t['evolveLine']!='none': errors.append(f'{name}: an Epic or Legendary has evolveLine none')
    if t['slotWeight']!=rarities[t['rarity']]['slotWeight']: errors.append(f'{name}: slotWeight {t["slotWeight"]} differs from rarities/{t["rarity"]} slotWeight {rarities[t["rarity"]]["slotWeight"]}')
    if t['ranged']!=(t['range']>3): errors.append(f'{name}: ranged must be true exactly when range is above 3')
    for slot in ('weapon','helmet','chest'):
        if not t[slot].startswith('minecraft:'): errors.append(f'{name}: {slot} {t[slot]} must be a minecraft: item id (minecraft:air for none)')
    tags=t['tags'].split(',')
    if 'none' in tags and len(tags)>1: errors.append(f'{name}: tags is none or a comma list of tags, never both')
for h in houses.values():
    if h['glyph']!=f'gravesandglory:textures/gui/house/{h["id"]}.png': errors.append(f'houses/{h["id"]}: glyph must be gravesandglory:textures/gui/house/{h["id"]}.png')
    elif not (root/'src/main/resources/assets/gravesandglory'/h['glyph'].partition(':')[2]).is_file(): errors.append(f'houses/{h["id"]}: missing glyph texture {h["glyph"]}')
    mine=[t['id'] for t in data['troops'] if t['house']==h['id']]
    if h['troops'].split(',')!=mine: errors.append(f'houses/{h["id"]}: troops must be {",".join(mine)}, the House troops in sheet order')
    for r,n in HOUSE_SHAPE.items():
        have=sum(1 for t in mine if rarityOf.get(t)==r)
        if have>n or (h['fieldable'] and have!=n): errors.append(f'houses/{h["id"]}: {have} {r} troops; a House has at most {n}, and a fieldable House exactly {n}')
for c in data['classes']:
    if c['group'] not in groups: errors.append(f'classes/{c["id"]}: group {c["group"]} does not resolve')
thresholds={}
for h in houses:
    tiers=sorted((t for t in data['houseTraits'] if t['house']==h),key=lambda t:t['tier'])
    if [t['tier'] for t in tiers]!=[1,2,3]: errors.append(f'houseTraits: {h} needs exactly tiers 1, 2 and 3')
    if len({t['mechanic'] for t in tiers})>1: errors.append(f'houseTraits: {h} uses one mechanic on every tier')
    if any(a['slots']>=b['slots'] for a,b in zip(tiers,tiers[1:])): errors.append(f'houseTraits: {h} slots must rise with the tier')
    thresholds[h]=[t['slots'] for t in tiers]
base=next(iter(thresholds),None)
for h,s in thresholds.items():
    if s!=thresholds[base]: errors.append(f'houseTraits: {h} tier slots {s} differ from {base} {thresholds[base]}; matchRewards takes one threshold array, so every House needs the same slots per tier')
TRAIT_MECHANICS=('rise_again','bulwark','pack_hunters','entrench','riptide')
for t in data['houseTraits']:
    if t['house'] not in houses: errors.append(f'houseTraits/{t["id"]}: house does not resolve')
    if t['id']!=f'{t["house"]}_{t["tier"]}': errors.append(f'houseTraits/{t["id"]}: id must be <house>_<tier>')
    if t['mechanic'] not in TRAIT_MECHANICS: errors.append(f'houseTraits/{t["id"]}: mechanic {t["mechanic"]} is not one of {", ".join(TRAIT_MECHANICS)} (CounterMath.is reads bulwark, pack_hunters, entrench and riptide; rise_again is the Graveborn death rule)')
MECHANICS={'none','shield','mend','blight','stance','pierce','cleave','onhit_slow','pounce','aura','slam','fearless','mist'}
TRIGGERS={'passive','onhit','cast','hold','timer','charge_contact'}
for a in data['abilities']:
    if a['mechanic'] not in MECHANICS: errors.append(f'abilities/{a["id"]}: mechanic {a["mechanic"]} is not in the closed list')
    if a['trigger'] not in TRIGGERS: errors.append(f'abilities/{a["id"]}: trigger {a["trigger"]} is not in the closed list')
    if a['target'] not in ('enemies','allies'): errors.append(f'abilities/{a["id"]}: target must be enemies or allies')
    if a['mechanic']=='stance' and a['amount']!=0: errors.append(f'abilities/{a["id"]}: stance multipliers live only in counters, so amount must be 0.0')
    if len(a['text'])>90: errors.append(f'abilities/{a["id"]}: text is {len(a["text"])} characters, over 90')
CONDITIONS={'always','raised_front','lowered_front','front','charge_impact','charge','hold','flank','no_sky','in_mist'}
# A counter tag is a dynamic tag, an ability id or a tag some troop carries in its tags column.
troopTags={tag for t in data['troops'] for tag in t['tags'].split(',')}-{'none'}
TAGS={'melee','arrow','shot','magic','nonsiege','ranged','charging','holding','structure','any'}|troopTags|abilityIds
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
for d in sorted(difficulties):
    if not any(b['difficulty']==d and all(houses.get(houseOf.get(s),{}).get('fieldable') for s in b['slots'].split(',') if s!='^') for b in data['botBanners']): errors.append(f'botBanners: no {d} banner fields only fieldable Houses (Campaign.botBanner picks from those)')
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
