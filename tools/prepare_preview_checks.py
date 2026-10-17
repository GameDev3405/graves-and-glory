from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]
s=(r/'test-tools/java/gg/gravesandglory/CommandPlaytest.java').read_text().replace('public final class CommandPlaytest','public final class RevisionChecks').replace('if(!Boolean.getBoolean("gravesandglory.commandPlaytest"))return;','if(!MeltyTest.active())return;')
a=s.index('    static void result(');b=s.index('    void server(',a);s=s[:a]+'    static void result(String name,boolean pass,String detail){MeltyTest.report(name,pass,detail);}\n'+s[b:]
s=s.replace('boolean fourSeen;', 'boolean fourSeen;int bridgeIndex=-1,bridgeAt;boolean crossings=true;')
start=s.index('            if(tick>1400&&tick%100==0)');end=s.index('\n        });',start)
s=s[:start]+'''            if(tick==1400)server(c,b->{b.testSquad=b.squads.get(c.player.getUuid());b.testSquad.assignments.clear();b.testBridgeIndex=0;b.prepareBridgeTest();bridgeIndex=0;bridgeAt=tick;});
            if(bridgeIndex>=0&&bridgeIndex<3&&tick%10==0&&tick>bridgeAt+20)server(c,b->{
                var bridge=Arrays.stream(Spec.STRUCTURES).filter(v->v.kind().equals("bridge")).toList().get(bridgeIndex);
                long crossed=b.units.stream().filter(u->u.squad==b.testSquad&&u.mob.isAlive()&&u.mob.getX()>bridge.x()+bridge.width()/2.0-4&&u.mob.getY()>63).count();
                if(crossed==20||tick-bridgeAt>900){crossings&=crossed==20;System.out.println("[Graves and Glory revision] "+bridge.id()+" "+crossed+"/20 crossed in "+(tick-bridgeAt)+" ticks");bridgeIndex++;bridgeAt=tick;
                    if(bridgeIndex<3){b.testBridgeIndex=bridgeIndex;b.prepareBridgeTest();}else result("revised-crossings",crossings,"All three changed crossings must carry all twenty troops");
                }
            });
''' +s[end:]
s=s.replace('/** Unshipped interaction tests; reports mechanics, never asserts fun or balance. */','/** Explicit Melty test mode only. Measures mechanics; never asserts fun or balance. */')
(r/'src/main/java/gg/gravesandglory/RevisionChecks.java').write_text(s)
p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text().replace('            if(MeltyTest.active())selfTest();','            // Explicit revision checks are driven by the client test timeline.');p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text();s='\n'.join(line for line in s.splitlines() if 'if(MeltyTest.active()&&running&&!VisualChecks.posing()' not in line and 'if(MeltyTest.active())VisualChecks.tick(client);' not in line)+'\n';p.write_text(s)
p=r/'src/main/resources/fabric.mod.json';d=json.loads(p.read_text());d['entrypoints']['client'].append('gg.gravesandglory.RevisionChecks');d['version']='0.2.0-preview.1';p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'build.gradle';s=p.read_text().replace("version = '0.2.0-dev'","version = '0.2.0-preview.1'");p.write_text(s)
checks=['arena-ready','independent-packet-orders','ranged-group-moves','retreat-interrupt','retreat-disengages','whole-army-recall','charge-requires-enemy','barracks-queue','barracks-cancel','teammate-ping','dawn-support-heals','dusk-blight-resolves','revised-crossings']
p=r/'melty.json';d=json.loads(p.read_text());d['components'][0]['fileName']='gravesandglory-0.2.0-preview.1.zip';d['test']={'checks':checks,'minutes':5};p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'sheets/design.json';d=json.loads(p.read_text());d['revisionChecks']=[{'id':c,'mode':'explicit_melty_test','evidence':'real_game','experienceClaim':'none'} for c in checks];p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'tools/package.py';s=p.read_text().replace('0.1.0','0.2.0-preview.1');p.write_text(s)
