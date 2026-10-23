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
    'textures/entity/skeleton/stray.png','textures/entity/spider/spider.png','textures/entity/wolf/wolf.png','textures/entity/zombie/drowned.png',
    'textures/entity/zombie/zombie.png')
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
# Troop looks (Phase 2 Task 5, unit identity rules 2 and 7): the painted shield is the one vanilla item a troop draws,
# its patterns from troops.shield on the side's factions.dye; each art/troops/<id>.json is a troop's own model source, and
# tools/build_skins.py lists every model it built in models/troop/index.json.
DYES=('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')
BANNER_PATTERNS=('b','bl','br','tl','tr','bs','ts','ls','rs','cs','ms','drs','dls','ss','cr','sc','bt','tt','bts','tts','ld','rd','lud','rud',
    'mc','mr','vh','hh','vhr','hhb','bo','cbo','gra','gru','bri','glb','cre','sku','flo','moj','pig')
for f in sides:
    if f['dye'] not in DYES: errors.append(f'factions/{f["id"]}: dye {f["dye"]} is not a vanilla dye ({", ".join(DYES)})')
# A shield's field is its side's factions.dye in battle and GravesAndGlory.Battle.PORTRAIT_FIELD on a portrait (read from the
# source here, so the two cannot drift). A layer in the colour of a field vanishes on that field, and two sides with one
# dye fly the same shields, so every field dye is a side's own and no layer takes one.
battleSource=root/'src/main/java/gg/gravesandglory/GravesAndGlory.java'
portraitField=re.search(r'PORTRAIT_FIELD\s*=\s*"([a-z_]+)"',battleSource.read_text(encoding='utf-8')) if battleSource.is_file() else None
portraitField=portraitField.group(1) if portraitField else None
fields={}
for f in sides:
    if f['dye'] in fields: errors.append(f'factions/{f["id"]}: dye {f["dye"]} is also {fields[f["dye"]]}; every side needs its own shield field')
    fields.setdefault(f['dye'],f'factions/{f["id"]} dye')
if portraitField is None: errors.append('GravesAndGlory.java: no PORTRAIT_FIELD="<dye>", the shield field of a portrait')
elif portraitField not in DYES: errors.append(f'GravesAndGlory.java: PORTRAIT_FIELD {portraitField} is not a vanilla dye')
elif portraitField in fields: errors.append(f'GravesAndGlory.java: PORTRAIT_FIELD {portraitField} is {fields[portraitField]}, so a portrait would fly that side')
else: fields[portraitField]='the portrait field (GravesAndGlory.Battle.PORTRAIT_FIELD)'
for r in data['troops']:
    shielded='shield' in r['tags'].split(',')
    if (r['shield']=='none')==shielded: errors.append(f'troops/{r["id"]}: shield must be none exactly when tags lacks shield, not {r["shield"]}')
    elif r['shield']!='none':
        for layer in r['shield'].split(','):
            dye,_,code=layer.partition(':')
            if dye not in DYES or code not in BANNER_PATTERNS: errors.append(f'troops/{r["id"]}: shield layer {layer} must be <vanilla dye>:<banner pattern code>')
            elif dye in fields: errors.append(f'troops/{r["id"]}: shield layer {layer} is the colour of {fields[dye]}, and vanishes on that field')
artTroops=sorted(p.stem for p in (root/'art/troops').glob('*.json')) if (root/'art/troops').is_dir() else []
for t in artTroops:
    if t not in troops: errors.append(f'art/troops/{t}.json: {t} is not a troops id')
troopIndex=root/'src/main/resources/assets/gravesandglory/models/troop/index.json'
if artTroops or troopIndex.is_file():
    try: listedTroops=json.loads(troopIndex.read_text(encoding='utf-8')).get('troops')
    except (OSError,ValueError): listedTroops=None
    if listedTroops!=artTroops: errors.append(f'models/troop/index.json: troops {listedTroops} must be exactly the art/troops ids {artTroops}; run python tools/build_skins.py')
for r in data['ui']:
    if r['id']=='command':
        formationIds={f['id'] for f in data['formations']}
        for id in r['bottom'].split(','):
            if id not in formationIds: errors.append('UI formation reference missing: '+id)
# Phase 2 Houses (spec 6.1): a troop's House, rarity, ability and evolve line (the levelNames key since the in-match progression design) are its own columns.
squad=data['rules'][0]['squadSize']
houses={h['id']:h for h in data['houses']}
rarities={r['id']:r for r in data['rarities']}
RARITY_ORDER=['common','rare','epic','legendary']
if [r['id'] for r in data['rarities']]!=RARITY_ORDER: errors.append('rarities: the rows must be common, rare, epic, legendary, in that order (validBanner checks bannerMax from the last row up, rarest first)')
abilityIds={a['id'] for a in data['abilities']}
evolveTroops={e['troop'] for e in data['levelNames']}
HOUSE_SHAPE={'common':2,'rare':2,'epic':1,'legendary':1}
houseOf,rarityOf={},{}
for t in data['troops']:
    name=f'troops/{t["id"]}'
    if t['house'] not in houses or t['rarity'] not in rarities: errors.append(f'{name}: house {t["house"]} or rarity {t["rarity"]} does not resolve'); continue
    houseOf[t['id']]=t['house'];rarityOf[t['id']]=t['rarity']
    if t['ability'] not in abilityIds: errors.append(f'{name}: ability {t["ability"]} does not resolve')
    if t['rarity'] in ('common','rare'):
        if t['evolveLine'] not in evolveTroops: errors.append(f'{name}: a Common or Rare needs an evolveLine that names a levelNames troop, not {t["evolveLine"]}')
    elif t['evolveLine']!='none': errors.append(f'{name}: an Epic or Legendary has evolveLine none')
    if t['slotWeight']!=rarities[t['rarity']]['slotWeight']: errors.append(f'{name}: slotWeight {t["slotWeight"]} differs from rarities/{t["rarity"]} slotWeight {rarities[t["rarity"]]["slotWeight"]}')
    if t['ranged']!=(t['range']>3): errors.append(f'{name}: ranged must be true exactly when range is above 3')
    for slot in ('weapon','helmet','chest'):
        if not t[slot].startswith('minecraft:'): errors.append(f'{name}: {slot} {t[slot]} must be a minecraft: item id (minecraft:air for none)')
    tags=t['tags'].split(',')
    if 'none' in tags and len(tags)>1: errors.append(f'{name}: tags is none or a comma list of tags, never both')
# A troop is drawn from its custom name (TroopModels.resolve through ArmyMobs.named, where a later troop row wins and a troop
# name beats a level name), and " (Veteran)" on it marks the Veteran skin: so no name may stand for two troops, two levels of
# one line, or a level and its own troop, and none may end in " (Veteran)".
VETERAN=' (Veteran)'
nameOwners={}
for t in data['troops']: nameOwners.setdefault(t['name'],{}).setdefault(t['id'],[]).append(f'troops/{t["id"]}')
for e in data['levelNames']:
    for t in data['troops']:
        if t['evolveLine']==e['troop']: nameOwners.setdefault(e['name'],{}).setdefault(t['id'],[]).append(f'levelNames/{e["id"]}')
for nm,owners in nameOwners.items():
    where=', '.join(w for ws in owners.values() for w in ws)
    if nm.endswith(VETERAN): errors.append(f'{where}: the name "{nm}" ends in "{VETERAN.strip()}", which marks a Veteran skin')
    if len(owners)>1: errors.append(f'{where}: the name "{nm}" stands for {len(owners)} troops ({", ".join(owners)}); a troop is drawn from its name, so one would draw as the other')
    else:
        for troop,ws in owners.items():
            if len(ws)>1: errors.append(f'{where}: the name "{nm}" is two levels of {troop}; its level could not be told from it')
for h in houses.values():
    if h['glyph']!=f'gravesandglory:textures/gui/house/{h["id"]}.png': errors.append(f'houses/{h["id"]}: glyph must be gravesandglory:textures/gui/house/{h["id"]}.png')
    elif not (root/'src/main/resources/assets/gravesandglory'/h['glyph'].partition(':')[2]).is_file(): errors.append(f'houses/{h["id"]}: missing glyph texture {h["glyph"]}')
    mine=[t['id'] for t in data['troops'] if t['house']==h['id']]
    if h['troops'].split(',')!=mine: errors.append(f'houses/{h["id"]}: troops must be {",".join(mine)}, the House troops in sheet order')
    if not h.get('fieldable'): errors.append(f'houses/{h["id"]}: fieldable must be true (all five Houses are fieldable since Phase 2 Task 13; a House that is not vanishes from banners)')
    for r,n in HOUSE_SHAPE.items():
        have=sum(1 for t in mine if rarityOf.get(t)==r)
        if have!=n: errors.append(f'houses/{h["id"]}: {have} {r} troops; every House has exactly {n} (all five are fieldable since Phase 2 Task 13)')
CLASS_ACTIVES=('stand_fast','bloodrush','mark_prey','rally_horn','grave_bloom','forward_supply')
CLASS_PASSIVES=('champion','reaver','warden','marshal','warlock','quartermaster')
for c in data['classes']:
    if c['group'] not in groups: errors.append(f'classes/{c["id"]}: group {c["group"]} does not resolve')
    if c['track'] not in ('hero','general'): errors.append(f'classes/{c["id"]}: track must be hero or general')
    if c['active'] not in CLASS_ACTIVES: errors.append(f'classes/{c["id"]}: active {c["active"]} is not one of {", ".join(CLASS_ACTIVES)} (Commander.ACTIVES keys)')
    if c['passive'] not in CLASS_PASSIVES: errors.append(f'classes/{c["id"]}: passive {c["passive"]} is not one of {", ".join(CLASS_PASSIVES)} (the effect kinds Commander reads)')
    if c['cooldownTicks']<c['durationTicks']: errors.append(f'classes/{c["id"]}: cooldownTicks must be at least durationTicks')
    for col in ('activeText','passiveText'):
        if len(c[col])>90: errors.append(f'classes/{c["id"]}: {col} is {len(c[col])} characters, over 90')
    if c['bonus']!=c['passiveText']: errors.append(f'classes/{c["id"]}: bonus must equal passiveText')
for col in ('active','passive'):
    seen=[c[col] for c in data['classes']]
    if len(set(seen))!=len(seen): errors.append(f'classes: every class needs its own {col}')
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
MECHANICS={'none','shield','mend','blight','stance','pierce','cleave','onhit_status','pounce','aura','slam','fearless','mist','onkill_heal','raise','rise','mark'}
TRIGGERS={'passive','onhit','cast','hold','timer','charge_contact','rider','onkill'}
for a in data['abilities']:
    if a['mechanic'] not in MECHANICS: errors.append(f'abilities/{a["id"]}: mechanic {a["mechanic"]} is not in the closed list')
    if a['trigger'] not in TRIGGERS: errors.append(f'abilities/{a["id"]}: trigger {a["trigger"]} is not in the closed list')
    if a['target'] not in ('enemies','allies'): errors.append(f'abilities/{a["id"]}: target must be enemies or allies')
    if a['mechanic']=='stance' and a['amount']!=0: errors.append(f'abilities/{a["id"]}: stance multipliers live only in counters, so amount must be 0.0')
    if len(a['text'])>90: errors.append(f'abilities/{a["id"]}: text is {len(a["text"])} characters, over 90')
    if not isinstance(a['anchors'],bool): errors.append(f'abilities/{a["id"]}: anchors must be true or false')
    elif a['anchors'] and (a['mechanic']!='aura' or a['target']!='allies'): errors.append(f'abilities/{a["id"]}: only an aura on allies can anchor (Abilities.anchored reads the Oath Banner)')
    if a['mechanic']=='fearless' and not 0<a['amount2']<1: errors.append(f'abilities/{a["id"]}: a fearless row health share (amount2) must be between 0 and 1')
CONDITIONS={'always','raised_front','lowered_front','front','charge_impact','charge','hold','flank','no_sky','in_mist','target_slowed_or_wounded','target_wounded'}
# A counter tag is a dynamic tag, an ability id, a tag some troop carries in its tags column or a tag an abilities row grants (abilities.tags, | joined).
troopTags={tag for t in data['troops'] for tag in t['tags'].split(',')}-{'none'}
grantedTags={tag for a in data['abilities'] for tag in a['tags'].split('|')}-{'none'}
TAGS={'melee','arrow','shot','magic','nonsiege','ranged','charging','holding','structure','any'}|troopTags|abilityIds|grantedTags
counterIds={c['id'] for c in data['counters']}
for c in data['counters']:
    if c['condition'] not in CONDITIONS: errors.append(f'counters/{c["id"]}: condition {c["condition"]} is not in the closed list')
    for tag in c['attackerTag'].split('+')+c['targetTag'].split('+'):
        if tag not in TAGS: errors.append(f'counters/{c["id"]}: tag {tag} does not resolve')
    if c['replaces']!='none' and any(r not in counterIds or r==c['id'] for r in c['replaces'].split('|')): errors.append(f'counters/{c["id"]}: replaces must be none or a | list of other counter ids')
    if c['multiplier']<=0: errors.append(f'counters/{c["id"]}: multiplier must be above 0')
cd=data['combatDiscipline'][0]
if not 0<cd['multiplierMin']<=1<=cd['multiplierMax']: errors.append('combatDiscipline: the counter clamp must hold 1 (0 < multiplierMin <= 1 <= multiplierMax)')
if cd['chargeImpactTicks']<1: errors.append('combatDiscipline: chargeImpactTicks (the counters row C5 window) must be at least 1')
# The columns that came over from the Phase 1 morale sheet when routing went (Task 8b): the flank arc, the last stand, the command feedback sounds, the wounded share and the arrival radius; and the bleed (damage per beat and ticks between beats, design N3, Statuses: Task 10 moved them from code).
for col,lo,hi in (('flankArcDegrees',0,180),('lastStandShare',0,1),('lastStandRadius',0,None),('hornCooldownSeconds',0,None),('hornVolume',0,None),('woundedShare',0,1),('lastStandFeedSeconds',1,None),('arriveRadius',0,None),('bleedDamage',0,None),('bleedEveryTicks',0,None)):
    if col not in cd: errors.append(f'combatDiscipline: missing {col}')
    elif cd[col]<lo or hi is not None and cd[col]>hi or cd[col]==lo and col not in ('hornCooldownSeconds','lastStandFeedSeconds'): errors.append(f'combatDiscipline: {col} must be above {lo}'+(f' and at most {hi}' if hi is not None else ''))
if 'morale' in data: errors.append('morale: the sheet went with routing (Task 8b); its surviving numbers live in combatDiscipline')
if len(data['recruitment'])!=1: errors.append('recruitment: exactly one row')
if any(data['campaign'][0].get(k,-1)<0 for k in ('completionShards','victoryShards','completionRenown','victoryRenown')): errors.append('campaign: completionShards, victoryShards, completionRenown and victoryRenown must be 0 or more')
cap=data['recruitment'][0]['capPct']
for r in rarities.values():
    if not 0<=r['starPct2']<=r['starPct3']<=cap+1e-9: errors.append(f'rarities/{r["id"]}: star bonuses must rise and stay within recruitment.capPct')
    if r['slotWeight']<1 or r['bannerMax']<1: errors.append(f'rarities/{r["id"]}: slotWeight and bannerMax must be at least 1')
# In-match progression (design sections 3, 4, 8 and 9): levels from kills, gear bought in battle, the level names and the per-level ability rows.
# Nothing here is a stage or an evolution: the between-match evolutions are gone, and no player text may say so (checked below).
levelNameRows={}
for e in data['levelNames']:
    if e['id']!=f'{e["troop"]}_{e["level"]}' or e['level'] not in (2,3): errors.append(f'levelNames/{e["id"]}: id must be <troop>_<level> for level 2 or 3'); continue
    if rarityOf.get(e['troop']) not in ('common','rare'): errors.append(f'levelNames/{e["id"]}: only a House Common or Rare has level names (an Epic or Legendary keeps its own name)'); continue
    levelNameRows.setdefault(e['troop'],{})[e['level']]=e
for t,r in rarityOf.items():
    if r in ('common','rare') and sorted(levelNameRows.get(t,{}))!=[2,3]: errors.append(f'levelNames: {t} needs one level 2 and one level 3 row')
if len(data['progression'])!=1: errors.append('progression: exactly one row')
else:
    pg=data['progression'][0]
    if not isinstance(pg['enabled'],bool): errors.append('progression: enabled must be true or false')
    if pg['assistSeconds']<1 or pg['commanderBounty']<1 or pg['chevronRange']<=0: errors.append('progression: assistSeconds, commanderBounty and chevronRange must be above 0')
    if pg['feedLevel'] not in (2,3): errors.append('progression: feedLevel must be 2 or 3')
levels=data['levels']
if [l['level'] for l in levels]!=[1,2,3]: errors.append('levels: the rows must be levels 1, 2 and 3, in order')
elif levels[0]['kills']!=0: errors.append('levels: level 1 needs 0 kills (a soldier is trained at level 1)')
for a,b in zip(levels,levels[1:]):
    if a['kills']>=b['kills']: errors.append(f'levels: kills must rise strictly with the level ({a["id"]} {a["kills"]}, {b["id"]} {b["kills"]})')
    if b['healthPct']<a['healthPct'] or b['damagePct']<a['damagePct']: errors.append(f'levels: healthPct and damagePct must not fall with the level ({a["id"]} to {b["id"]})')
tiers=data['gearTiers']
if [g['tier'] for g in tiers]!=[1,2,3]: errors.append('gearTiers: the rows must be tiers 1, 2 and 3, in order')
for a,b in zip(tiers,tiers[1:]):
    if b['weaponPct']<a['weaponPct'] or b['armourPct']<a['armourPct']: errors.append(f'gearTiers: weaponPct and armourPct must not fall with the tier ({a["id"]} to {b["id"]})')
for r in rarities.values():
    if r['gearPrice2']<=0 or r['gearPrice3']<=r['gearPrice2']: errors.append(f'rarities/{r["id"]}: gear prices must be positive with tier III (gearPrice3) above tier II (gearPrice2)')
STATUSES=('none','slow','weaken','bleed','knockback','stun')
abilityRows={a['id']:a for a in data['abilities']}
for a in data['abilities']:
    if a['status'] not in STATUSES: errors.append(f'abilities/{a["id"]}: status {a["status"]} is not one of {", ".join(STATUSES)}')
    if a['mechanic']=='onhit_status' and a['status']=='none': errors.append(f'abilities/{a["id"]}: an onhit_status row needs a status')
    if a['taken']<0 or a['taken']>=1: errors.append(f'abilities/{a["id"]}: taken must be at least 0 and below 1')
    if not isinstance(a['cleanse'],bool) or not isinstance(a['rangedPenalty'],bool): errors.append(f'abilities/{a["id"]}: cleanse and rangedPenalty must be true or false')
levelAbility={}
for l in data['levelAbilities']:
    if l['troop'] not in troops or l['level'] not in (2,3) or l['id']!=f'{l["troop"]}_{l["level"]}': errors.append(f'levelAbilities/{l["id"]}: id must be <troop>_<level> for a troops id and level 2 or 3'); continue
    if l['ability'] not in abilityRows: errors.append(f'levelAbilities/{l["id"]}: ability {l["ability"]} does not resolve'); continue
    if (l['troop'],l['level']) in levelAbility: errors.append(f'levelAbilities/{l["id"]}: a second row for the same troop and level')
    levelAbility[(l['troop'],l['level'])]=l
for t in data['troops']:
    for lv in (2,3):
        if (t['id'],lv) not in levelAbility: errors.append(f'levelAbilities: {t["id"]} needs exactly one level-{lv} row')
    two=levelAbility.get((t['id'],2))
    if two and t['ability'] in abilityRows and abilityRows[two['ability']]['mechanic']!=abilityRows[t['ability']]['mechanic']: errors.append(f'levelAbilities/{two["id"]}: a level-2 ability keeps its base ability\'s mechanic ({abilityRows[t["ability"]]["mechanic"]}), not {abilityRows[two["ability"]]["mechanic"]}')
# In-match progression Task 6, ruling I1: a level-3 row that changes what the level-2 row does is no second cast. Trigger rider: it rides on the level-2 row
# (Abilities.second): a mend or the howl that also cleanses, a cloud that also lays a status, or a burst (blight) that also leaves a zone. It keeps the level-2
# row's timing, and a rider of the same mechanic its reach too, so it never casts on its own clock. Trigger onkill: it acts on a kill (onkill_heal, Abilities.onKill;
# raise, on a kill by the troop's blight).
for (troop,lv),l in levelAbility.items():
    a=abilityRows[l['ability']];name=f'abilities/{a["id"]}'
    # Task 6 review M6: Abilities.tick casts only a soldier's first row and onHit adds his level-3 row only on the onhit trigger, so a level-3 row on a clock
    # (cast, timer, hold) or on a charge's contact would never fire.
    if lv==3 and a['trigger'] in ('cast','timer','hold','charge_contact'): errors.append(f'{name}: a level-3 row on trigger {a["trigger"]} never fires (a level-3 row acts as a passive, on a hit, as a rider or on a kill)')
    if a['trigger']=='rider':
        two=levelAbility.get((troop,2));two=abilityRows.get(two['ability']) if two else None
        if lv!=3 or two is None: errors.append(f'{name}: a rider is a level-3 row on a troop with a level-2 row'); continue
        same=a['mechanic']==two['mechanic']
        if not (same and a['mechanic'] in ('mend','mist') or same and a['mechanic']=='aura' and two['trigger']=='timer' or a['mechanic']=='mist' and two['mechanic']=='blight'): errors.append(f'{name}: a rider is a mend on a mend, an aura on a timed aura (the howl), a mist on a mist, or a mist zone on a blight burst; its level-2 row is {two["mechanic"]} on {two["trigger"]}'); continue
        if two['trigger']=='rider': errors.append(f'{name}: the level-2 row a rider rides on has a trigger of its own')
        if any(a[k]!=two[k] for k in ('cooldownTicks','windupTicks','minRange')): errors.append(f'{name}: a rider keeps its level-2 row\'s cooldownTicks, windupTicks and minRange (it never casts on its own clock)')
        if same and any(a[k]!=two[k] for k in ('radius','durationTicks')): errors.append(f'{name}: a rider of its level-2 row\'s mechanic keeps its radius and durationTicks')
        if a['mechanic'] in ('mend','aura') and (not a['cleanse'] or a['amount']!=0 or a['amount2']!=0): errors.append(f'{name}: a mend or aura rider cleanses (cleanse true) and adds no heal or share of its own (amount and amount2 0)')
        if a['mechanic']=='mist' and (a['status'] not in ('weaken','bleed') or a['statusTicks']<=0): errors.append(f'{name}: a mist rider lays weaken or bleed (a Statuses status) for statusTicks above 0')
        if a['mechanic']=='mist' and not same and (a['radius']<=0 or a['durationTicks']<=0): errors.append(f'{name}: a zone a burst leaves needs a radius and a durationTicks above 0')
    elif a['mechanic']=='raise':
        two=levelAbility.get((troop,2));two=abilityRows.get(two['ability']) if two else None
        if lv!=3 or two is None or two['mechanic']!='blight': errors.append(f'{name}: raise is a level-3 row of a troop whose level-2 row is a blight (her blight kills rise)')
        elif a['amount']<1 or a['durationTicks']<=0: errors.append(f'{name}: raise needs amount (the most at a time) 1 or more and durationTicks above 0')
for a in data['abilities']:
    if (a['trigger']=='onkill')!=(a['mechanic'] in ('onkill_heal','raise')): errors.append(f'abilities/{a["id"]}: onkill_heal and raise act on a kill (trigger onkill), and only they do')
    if a['trigger'] in ('rider','onkill') and a['id'] in {t['ability'] for t in data['troops']}: errors.append(f'abilities/{a["id"]}: a rider or onkill row is a level-3 row, never a troop\'s base ability')
    if a['trigger'] in ('rider','onkill') and a['id'] not in {l['ability'] for (_,lv),l in levelAbility.items() if lv==3}: errors.append(f'abilities/{a["id"]}: a rider or onkill row is the ability of a level-3 levelAbilities row')
# Every counter tag an ability grants is read by some counter row, so no granted tag is dead.
counterTags={tag for c in data['counters'] for tag in c['attackerTag'].split('+')+c['targetTag'].split('+')}
for a in data['abilities']:
    if a['tags']!='none':
        for tag in a['tags'].split('|'):
            if tag not in counterTags: errors.append(f'abilities/{a["id"]}: tag {tag} appears in no counter row')
# Player-facing text never says stage, evolve or Writ (it says Pact): every text and name cell, and every troopNotes cell.
BANNED=re.compile(r'stage|evolve|writ',re.I)
for sheet,rows in data.items():
    for row in rows:
        for col,value in row.items():
            if isinstance(value,str) and (col in ('text','name') or sheet=='troopNotes' and col!='id') and BANNED.search(value): errors.append(f'{sheet}/{row.get("id")}/{col}: player text says "{BANNED.search(value).group(0)}" (it says level, tier or Pact): {value}')
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
# Troop card text (Task 12): one troopNotes row per troop, in troops order, and nothing else; the economy model row (houses design 4.9, NOT_SHIPPED) has every input.
if [n['id'] for n in data.get('troopNotes',[])]!=[t['id'] for t in data['troops']]: errors.append('troopNotes: one row per troops row, in the same order, with the same ids')
ECONOMY_INPUTS={'matchesPerHour','winRate','hoursPerPlayDay','playDaysPerWeek','matchGold','winGold','dailyGold','weeklyGold','chapterGold','chapterRareValue','runs'}
if len(data.get('economyModel',[]))!=1 or not ECONOMY_INPUTS<=set(data['economyModel'][0]): errors.append('economyModel: one row with '+', '.join(sorted(ECONOMY_INPUTS))+' (tools/economy_model.py reads it)')
elif not 0<=data['economyModel'][0]['winRate']<=1 or any(data['economyModel'][0][k]<=0 for k in ECONOMY_INPUTS-{'winRate'}): errors.append('economyModel: winRate within 0 to 1 and every other input above 0')
if errors:
    print('\n'.join(errors)); sys.exit(1)
def typ(v): return 'boolean' if isinstance(v,bool) else 'int' if isinstance(v,int) else 'double' if isinstance(v,float) else 'String'
def literal(v): return json.dumps(v,ensure_ascii=True)
# Validated above, but process metadata rather than game data: never shipped as Spec records.
NOT_SHIPPED={'checks','revisionChecks','playtestMilestone','economyModel'}
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
