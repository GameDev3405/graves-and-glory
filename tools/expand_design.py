import json,pathlib
p=pathlib.Path(__file__).resolve().parents[1]/'sheets/design.json'
d=json.loads(p.read_text())
for i,f in enumerate(d['factions']):
    f.update(barracksX=-108 if i==0 else 108,barracksZ=-24 if i==0 else 24)
for t in d['troops']:
    t.update(cost=50 if t['magic'] else 25 if t['range']>3 else 20,trainingSeconds=6 if t['magic'] else 4,texture='gravesandglory:textures/entity/dawn_guard.png' if t['faction']=='dawn' else 'minecraft:textures/entity/skeleton/'+('skeleton.png' if t['entity']=='skeleton' else 'wither_skeleton.png'))
if not any(r['id']=='rush' for r in d['orders']): d['orders'].append(dict(id='rush',label='Rush at aim',targeted=True,engage=True))
if not any(r['id']=='loose' for r in d['formations']):d['formations'].append(dict(id='loose',label='Loose',columns=5,spacing=3.2,wedge=False))
for r in d['orders']:
    r.update(engageRadius=6 if r['id']=='move' else 12 if r['id']=='hold' else 18 if r['id']=='follow' else 32 if r['id']=='rush' else 26,moveMultiplier=1.25 if r['id']=='rush' else 1.0)
    r['engage']=True
for point in d['points']:point['elevation']=68 if point['id']=='B' else 66 if point['id']=='C' else 64
for s in d['structures']:
    s['elevation']=68 if s['kind']=='ruin' else 66 if s['kind']=='shrine' else 64
    if s['kind']=='bridge':s['width']=32;s['depth']=12
def structure(id,kind,x,z,w,dep,h,pal,elev=64):return dict(id=id,kind=kind,x=x,z=z,width=w,depth=dep,height=h,palette=pal,elevation=elev)
new=[structure('dawn_barracks','barracks',-108,-24,20,18,9,'dawn'),structure('dusk_barracks','barracks',108,24,20,18,9,'dusk'),structure('village_tavern','house',-64,-29,15,12,9,'wood'),structure('village_mill','house',-25,-59,11,10,8,'wood'),structure('market','market',-39,-23,18,8,5,'dawn'),structure('east_bridge','bridge',30,66,34,12,2,'stone'),structure('west_watchtower','watchtower',-77,36,9,9,12,'dawn',70),structure('east_watchtower','watchtower',79,-10,9,9,12,'dusk',68),structure('east_ruins','ruin',67,62,18,15,8,'dusk',68)]
ids={s['id'] for s in d['structures']}
d['structures'] += [r for r in new if r['id'] not in ids]
d['terrain']=[
 dict(id='western_ridge',x=-80,z=52,radiusX=32,radiusZ=30,height=13.0),
 dict(id='eastern_ridge',x=79,z=46,radiusX=34,radiusZ=32,height=14.0),
 dict(id='north_bluff',x=-13,z=-79,radiusX=30,radiusZ=22,height=11.0),
 dict(id='southern_bluff',x=18,z=82,radiusX=29,radiusZ=24,height=12.0),
 dict(id='west_woodland',x=-94,z=-72,radiusX=24,radiusZ=25,height=8.0),
 dict(id='east_woodland',x=93,z=-70,radiusX=26,radiusZ=26,height=9.0),
 dict(id='western_valley',x=-53,z=24,radiusX=23,radiusZ=25,height=-3.0),
 dict(id='eastern_valley',x=48,z=22,radiusX=23,radiusZ=24,height=-3.0)]
trees=[(-93,-70),(-80,-28),(-82,48),(-58,68),(-25,-82),(24,-81),(76,-68),(91,-26),(83,46),(52,69),(-25,76),(24,78),(-118,69),(118,69),(-99,54),(-91,63),(-74,63),(-65,82),(-47,79),(-110,-63),(-100,-80),(-81,-84),(-119,44),(-125,58),(89,-82),(105,-76),(111,-58),(73,-83),(99,55),(112,62),(121,43),(62,83),(78,79),(-118,-45),(118,-43),(-14,92),(38,94)]
d['scenery']=[dict(id='tree_'+str(i),kind='tree',x=x,z=z,height=6+i%3,width=5,palette='spruce' if i%3==0 else 'oak') for i,(x,z) in enumerate(trees)]
d['scenery'] += [dict(id='rock_'+str(i),kind='rock',x=x,z=z,height=2+i%2,width=3+i%2,palette='stone') for i,(x,z) in enumerate([(-72,16),(-55,53),(-23,60),(20,-64),(58,28),(80,19),(-97,78),(100,-30)])]
d['economy']=[dict(id='barracks',startingGold=160,passiveGold=2,incomeSeconds=5,pointGold=1,killGold=5,maxQueue=20,accessRadius=18,disbandRefund=0)]
d['navigation']=[dict(id='tactical_ai',waterPenalty=16.0,searchRange=40,waypointDistance=16.0,stuckSeconds=3,repathTicks=20,rangedRetreatDistance=4.0,rangedStandoff=0.65,bridgeEntryInset=2,unreachableNoticeSeconds=10)]
d['ui']=[dict(id='command',trigger='R',screen='CommandScreen',content='attack,rush,move,follow,hold',bottom='block,line,wedge,loose'),dict(id='barracks',trigger='right_click_lectern',screen='BarracksScreen',content='buy,train,disband,cancel_queue,roster',bottom='gold,capacity,queue'),dict(id='map',trigger='M_or_click_map_card',screen='MapScreen',content='terrain,points,friendly_troops,fighting',bottom='legend')]
r=d['rules'][0];r.update(worldHalfX=164,worldHalfZ=124,automaticReinforcements=False)
newchecks=[('barracks-management','paid training, disband, capacity and composition swap exercised'),('friendly-fire','own troop damage rejected and enemy damage allowed'),('river-navigation','troops cross the river by an authored bridge'),('hill-navigation','squad climbs a ridge without teleporting or getting stuck')]
ids={c['id'] for c in d['checks']}
d['checks'] += [dict(id=id,evidence=e) for id,e in newchecks if id not in ids]
for c in d['checks']:
    if c['id']=='formations-commanded':c['evidence']='client packets applied block, line, wedge and loose on the host'
p.write_text(json.dumps(d,indent=2)+'\n')
