import json
from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'sheets/design.json';d=json.loads(p.read_text());d['scouting']=[{'id':'shared_sight','range':36,'memorySeconds':10,'refreshTicks':10,'confirmed':'solid_red','lastKnown':'hollow_gray','authority':'server_team_line_of_sight'}];p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text().replace('int owner=-1, challenger=-1, progress=0;','int owner=-1, challenger=-1, progress=0;boolean contested;')
s=s.replace('                int team=presence[0]>0', '                p.contested=presence[0]>0&&presence[1]>0;\n                int team=presence[0]>0')
s=s.replace('b.writeInt(c.challenger);}', 'b.writeInt(c.challenger);b.writeBoolean(c.contested);}')
s=s.replace('        void sendState(){','''        record Sight(float x,float z,long seen){}
        final Map<Integer,Map<UUID,Sight>> sightings=new HashMap<>();
        void updateSight(){for(int team=0;team<2;team++){var memory=sightings.computeIfAbsent(team,k->new HashMap<>());memory.values().removeIf(v->time-v.seen>Spec.SCOUTING[0].memorySeconds()*20);
            for(Unit enemy:units)if(enemy.squad.team!=team&&enemy.mob.isAlive()){
                boolean visible=false;for(Unit ally:units)if(ally.squad.team==team&&ally.mob.isAlive()&&ally.mob.squaredDistanceTo(enemy.mob)<Spec.SCOUTING[0].range()*Spec.SCOUTING[0].range()&&ally.mob.canSee(enemy.mob)){visible=true;break;}
                if(!visible)for(var player:server.getPlayerManager().getPlayerList()){var s=squads.get(player.getUuid());if(s!=null&&s.team==team&&!player.isSpectator()&&player.squaredDistanceTo(enemy.mob)<Spec.SCOUTING[0].range()*Spec.SCOUTING[0].range()&&player.canSee(enemy.mob)){visible=true;break;}}
                if(visible)memory.put(enemy.mob.getUuid(),new Sight((float)enemy.mob.getX(),(float)enemy.mob.getZ(),time));
            }
        }}
        void sendState(){
            updateSight();''')
s=s.replace('                ServerPlayNetworking.send(p,STATE,b);','''                var memory=s==null?Map.<UUID,Sight>of():sightings.getOrDefault(s.team,Map.of());b.writeInt(memory.size());for(var sight:memory.values()){b.writeFloat(sight.x);b.writeFloat(sight.z);b.writeInt((int)(time-sight.seen));}
                ServerPlayNetworking.send(p,STATE,b);''')
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/GravesAndGloryClient.java';s=p.read_text();s=s.replace('static int[] owners=', 'static boolean[] contested={false,false,false};\n    record Sight(float x,float z,int age){}\n    static List<Sight> sightings=List.of();\n    static int[] owners=')
s=s.replace('int[] own=new int[3],pr=new int[3],ch=new int[3];','boolean[] contest=new boolean[3];int[] own=new int[3],pr=new int[3],ch=new int[3];').replace('ch[i]=buf.readInt();}', 'ch[i]=buf.readInt();contest[i]=buf.readBoolean();}')
s=s.replace('            client.execute(()->{CommandMode.pings=', '''            int seen=buf.readInt();if(seen<0||seen>160)return;var sight=new ArrayList<Sight>();for(int i=0;i<seen;i++)sight.add(new Sight(buf.readFloat(),buf.readFloat(),buf.readInt()));
            client.execute(()->{sightings=List.copyOf(sight);contested=contest;CommandMode.pings=''')
s=s.replace('(progress[i]>0?"Capturing":', '(contested[i]?"Contested":progress[i]>0?"Capturing":')
s=s.replace('        for(var ping:CommandMode.pings)', '''        for(var sight:sightings){int px=x+(int)((sight.x()+140)*w/280),pz=y+(int)((sight.z()+100)*h/200);if(sight.age()<20)g.fill(px-2,pz-2,px+3,pz+3,0xffff6860);else g.drawBorder(px-2,pz-2,5,5,0xffa2a5aa);}
        for(var ping:CommandMode.pings)''')
s=s.replace('if(labels)g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,p.name(),px-22,pz+8,WHITE);','if(contested[i])g.drawBorder(px-6,pz-6,13,13,0xffffaa55);if(progress[i]>0)g.fill(px-5,pz+5,px-5+10*progress[i]/p.captureSeconds(),pz+7,factionColor(challengers[i]));if(labels)g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,p.name(),px-22,pz+8,WHITE);')
p.write_text(s)
