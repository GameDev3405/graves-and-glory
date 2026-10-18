import json,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
p=root/'sheets/design.json'; d=json.loads(p.read_text())
r=d['rules'][0];r.update(maxPlayers=8,playersPerTeam=4,arenaHalfX=350,arenaHalfZ=250,worldHalfX=366,worldHalfZ=266,buildColumnsPerTick=750,orderRange=240,roundSeconds=900)
# The six starting types remain in their original order for existing barracks code.
extra=[('sentinel',0,'Dawn Sentinel',0,44,6,2.8,.26,'diamond_sword','diamond_helmet','iron_chestplate','shield'),('longbow',0,'Dawn Longbow',1,18,6,30.0,.29,'bow','leather_helmet','leather_chestplate','none'),('oracle',0,'Dawn Oracle',2,26,4,18.0,.30,'amethyst_shard','golden_helmet','leather_chestplate','mend'),('reaper',1,'Dusk Reaper',3,34,8,2.8,.33,'iron_axe','netherite_helmet','iron_chestplate','none'),('stalker',1,'Dusk Stalker',4,22,5,27.0,.34,'bow','chainmail_helmet','leather_chestplate','none'),('wraith',1,'Dusk Wraith',5,24,9,19.0,.28,'echo_shard','golden_helmet','leather_chestplate','blight')]
d['troops']=d['troops'][:6]
for id,team,name,template,hp,damage,range,speed,weapon,helmet,chest,ability in extra:
    t=d['troops'][template].copy();t.update(id=id,name=name,count=0,health=hp,damage=damage,range=range,speed=speed,weapon=weapon,helmet=helmet,chest=chest,ability=ability,cost=60 if range<3 else 65,trainingSeconds=7);d['troops'].append(t)
d['maps']=[
 {'id':'rivergate','name':'Rivergate Valley','theme':'river','description':'A working river town: mill, farms, trade crossing and hilltop shrine.'},
 {'id':'ironhold','name':'Ironhold Siege','theme':'fortress','description':'A breached border fortress: supply yards, gate roads and exposed flanks.'},
 {'id':'frostmarch','name':'Frostmarch Pass','theme':'snow','description':'A mountain trade pass: sheltered camps, ridge routes and a frozen gorge.'},
 {'id':'hollowwood','name':'Hollowwood Reach','theme':'forest','description':'An abandoned woodland road: old logging camps, shrines and marsh crossings.'}]
d['classes']=[{'id':'vanguard','name':'Vanguard','group':'infantry','bonus':'Infantry gain 20% health and 10% damage.'},{'id':'marksman','name':'Marksman','group':'ranged','bonus':'Ranged troops gain 15% damage and 10% attack speed.'},{'id':'arcanist','name':'Arcanist','group':'support','bonus':'Support gain 20% health and 20% spell or healing power.'}]
d['campaign']=[{'id':'collection','rollCost':100,'completionGold':80,'victoryGold':40,'practiceGold':40,'minimumRewardSeconds':180,'duplicateRefund':50,'starterGold':0}]
p.write_text(json.dumps(d,indent=2))
q=root/'tools/preflight.py';s=q.read_text().replace("public static final {name}[]", "public static {name}[]");q.write_text(s)
for path in ['build.gradle','src/main/resources/fabric.mod.json','tools/package.py','melty.json']:
 q=root/path;s=q.read_text().replace('0.2.0-preview.1','0.2.0');q.write_text(s)
q=root/'src/main/resources/fabric.mod.json';j=json.loads(q.read_text());j['description']='Permanent armies, five battlefields and fantasy army battles for one to eight commanders.';j['entrypoints']['client']=['gg.gravesandglory.GravesAndGloryClient','gg.gravesandglory.CampaignChecks'];q.write_text(json.dumps(j,indent=2))
print('Campaign design updated')
