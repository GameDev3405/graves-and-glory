from pathlib import Path
r=Path(__file__).resolve().parents[1];base=r/'src/main/java/gg/gravesandglory'
p=base/'GravesAndGlory.java';s=p.read_text()
s=s.replace('public static final Identifier PING', 'public static final Identifier PROFILE = new Identifier("gravesandglory", "profile_v1");\n    public static final Identifier PING',1)
s=s.replace('ServerPlayNetworking.registerGlobalReceiver(PING,', 'ServerPlayNetworking.registerGlobalReceiver(PROFILE,(server,p,handler,buf,responder)->{String json=buf.readString(16000);server.execute(()->{if(battle!=null)battle.campaign.importProfile(p,json);});});\n        ServerPlayNetworking.registerGlobalReceiver(PING,',1)
s=s.replace('String order="follow", formation="block";','String order="follow", formation="block", commander="vanguard";\n        boolean bot; int lane;',1)
s=s.replace('public final MinecraftServer server;', 'public final Campaign campaign;\n        public final MinecraftServer server;',1)
s=s.replace('this.server=server;world=server.getOverworld();', 'this.server=server;world=server.getOverworld();campaign=new Campaign(this);')
s=s.replace('if(battle!=null&&player instanceof ServerPlayerEntity p&&world==battle.world){battle.openBarracks(p);return ActionResult.SUCCESS;}', 'if(battle!=null&&player instanceof ServerPlayerEntity p&&world==battle.world){if(!battle.running){if(ServerPlayNetworking.canSend(p,OPEN_BARRACKS))ServerPlayNetworking.send(p,OPEN_BARRACKS,PacketByteBufs.create());}else battle.openBarracks(p);return ActionResult.SUCCESS;}')
s=s.replace('for(int x=-11;x<=11;x++)for(int z=-9;z<=9;z++)world.setChunkForced(x,z,true);','// Only inhabited chunks tick. Unit navigation tickets are maintained below.\n            CampaignMaps.lobby(world);')
s=s.replace('p.changeGameMode(GameMode.SPECTATOR);\n                    p.teleport(world,-108,70,0,-90,10);','p.changeGameMode(GameMode.ADVENTURE);\n                    if(p.getZ()>-275||p.getY()<63)campaign.toLobby(p);')
s=s.replace('Arena.finish(world);ready=true;message="Choose a faction with R. Lead your army!";', 'Arena.finish(world);ready=true;message="Your war council is ready. Press R.";')
s=s.replace('else if(Math.abs(p.getX())>R.arenaHalfX()-2||Math.abs(p.getZ())>R.arenaHalfZ()-2||p.getY()<56)toBase(p,squads.get(p.getUuid()).team);','else if(running&&(Math.abs(p.getX())>R.arenaHalfX()-2||Math.abs(p.getZ())>R.arenaHalfZ()-2||p.getY()<56))toBase(p,squads.get(p.getUuid()).team);')
s=s.replace('trainingTick();\n            if(running){','if(running)trainingTick();\n            campaign.tick();\n            updateUnitChunks();\n            if(running&&time>=campaign.introUntil){')
s=s.replace('running=false;message=Spec.FACTIONS[t].name()+" wins! Press R to play again.";', 'campaign.finish(t);')
s=s.replace('if(running&&time-roundStarted>=R.roundSeconds()*20L){running=false;message=scores[0]==scores[1]?"Time: draw":(scores[0]>scores[1]?"Dawn":"Dusk")+" wins on score";for(var p:server.getPlayerManager().getPlayerList())p.sendMessage(Text.literal(message),false);}', 'if(running&&time-roundStarted>=R.roundSeconds()*20L)campaign.finish(scores[0]==scores[1]?-1:scores[0]>scores[1]?0:1);')
s=s.replace('if(squads.size()>=R.maxPlayers()){','if(squads.values().stream().filter(s->!s.bot).count()>=R.maxPlayers()||running){')
s=s.replace('This battlefield has four commanders already.', 'This battle is full or already underway. Join the next round from the lobby.')
s=s.replace('reinforce(s);\n        }\n        void clearOldUnits()', 'campaign.joined(p);\n        }\n        void clearOldUnits()',1)
s=s.replace('if(cmd.equals("barracks")){', 'if(campaign.handle(p,cmd))return;\n            if(cmd.equals("barracks")){',1)
s=s.replace('count()>=R.playersPerTeam()', 'count()>=campaign.size')
s=s.replace('That faction already has two commanders.', 'That faction is full for the selected mode.')
s=s.replace('s.team=team;toBase(p,team);reinforce(s);return;', 's.team=team;campaign.toLobby(p);return;')
s=s.replace('Math.abs(x)>130||Math.abs(z)>90', 'Math.abs(x)>R.arenaHalfX()-10||Math.abs(z)>R.arenaHalfZ()-10')
s=s.replace('-130,130','-R.arenaHalfX()+10,R.arenaHalfX()-10').replace('-90,90','-R.arenaHalfZ()+10,R.arenaHalfZ()-10')
s=s.replace('if(spec==null)return;\n            if(cmd.startsWith("disband:"))','if(spec==null||!campaign.unlocked(s,spec.id()))return;\n            if(cmd.startsWith("disband:"))')
s=s.replace('Holding points and defeating enemies earns more.', 'Battle supplies arrive over time; holding points increases them.')
start=s.index('        void startRound(){');end=s.index('        Unit spawn(',start)
s=s[:start]+'''        void startRound(){
            campaign.prepareRound();
            Arrays.fill(scores,0);
            for(Capture p:points){p.owner=-1;p.progress=0;p.challenger=-1;Arena.flag(world,p.spec,-1);}
            respawns.clear();
            for(Squad s:squads.values()){
                s.assignments.clear();s.training.clear();s.gold=Spec.ECONOMY[0].startingGold();s.order=s.bot?"attack":"hold";s.target=base(s.team);s.reinforceAt=time+R.reinforceSeconds()*20L;
                var player=server.getPlayerManager().getPlayer(s.owner);
                if(player!=null){toBase(player,s.team);player.changeGameMode(GameMode.ADVENTURE);equipPlayer(player);}
                reinforce(s);
            }
            rounds++;roundStarted=time;running=true;message="Capture the objectives. Your army awaits your orders.";
        }
        void reinforce(Squad s){
            Set<Integer> occupied=new HashSet<>();for(Unit u:units)if(u.squad==s&&u.mob.isAlive())occupied.add(u.slot);
            int slot=0,index=0;for(Squad other:squads.values()){if(other==s)break;if(other.team==s.team)index++;}
            for(var spec:Spec.TROOPS)if(spec.faction().equals(Spec.FACTIONS[s.team].id()))for(int n=0;n<campaign.amount(s,spec)&&slot<R.squadSize();n++,slot++){
                if(occupied.contains(slot))continue;
                double x=base(s.team).x+(s.team==0?15:-15)+(slot%5-2)*2,z=base(s.team).z+(index-1.5)*22+(slot/5-2)*2;
                spawn(s,spec,slot,new Vec3d(x,Arena.ground((int)x,(int)z)+1,z));
            }
        }
''' +s[end:]
s=s.replace('setBaseValue(spec.health());mob.setHealth(spec.health());','setBaseValue(spec.health()*campaign.health(s,spec));mob.setHealth((float)(spec.health()*campaign.health(s,spec)));')
s=s.replace('b.writeInt(running?(int)Math.max(0,R.roundSeconds()', 'b.writeInt(running?(int)Math.max(0,R.roundSeconds()')
s=s.replace('ServerPlayNetworking.send(p,STATE,b);','b.writeString(campaign.snapshot(p));\n                ServerPlayNetworking.send(p,STATE,b);')
# Player chunk range is insufficient for a giant battlefield. Keep only live
# unit chunks and one-chunk neighbours active; release our own tickets on exit.
needle='        void unitTick(Unit u){'
code='''        final Set<Long> unitChunks=new HashSet<>();
        void updateUnitChunks(){
            if(time%40!=0)return;Set<Long> wanted=new HashSet<>();
            if(running)for(Unit u:units)if(u.mob.isAlive()){int x=u.mob.getBlockX()>>4,z=u.mob.getBlockZ()>>4;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)wanted.add(net.minecraft.util.math.ChunkPos.toLong(x+dx,z+dz));}
            for(long key:wanted)if(!unitChunks.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,true);}
            for(long key:unitChunks)if(!wanted.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,false);}
            unitChunks.clear();unitChunks.addAll(wanted);
        }
'''
s=s.replace(needle,code+needle)
p.write_text(s)
p=base/'Arena.java';s=p.read_text();start=s.index('    public static double riverX');end=s.index('    static double blend',start)
s=s[:start]+'''    public static double riverX(double z){return CampaignMaps.riverX(z);}
    public static boolean river(int x,int z){return CampaignMaps.river(x,z);}
    public static boolean road(int x,int z){return CampaignMaps.road(x,z);}
'''+s[end:]
s=s.replace('h+=Spec.TERRAINSTYLE[0].roughness()', 'h+=.5*Spec.TERRAINSTYLE[0].roughness()')
s=s.replace('double border=Math.max', 'if(CampaignMaps.roadDistance(x,z)<8)h=blend(h,Y,CampaignMaps.roadDistance(x,z),0,8);\n        double border=Math.max')
s=s.replace('for(int y=Math.min(Y-4,h);y<=Math.max(Y+46,top);y++)put(w,x,y,z,Blocks.AIR);','for(int y=h+1;y<=top;y++)if(!w.getBlockState(new BlockPos(x,y,z)).isAir())put(w,x,y,z,Blocks.AIR);')
s=s.replace('h>85?Blocks.STONE:Blocks.GRASS_BLOCK','h>94?Blocks.STONE:CampaignMaps.selected==2?Blocks.SNOW_BLOCK:CampaignMaps.selected==3?Blocks.PODZOL:Blocks.GRASS_BLOCK')
s=s.replace('hash%deco.flowerEvery()==0?Blocks.POPPY:hash%deco.flowerEvery()==1?Blocks.DANDELION:Blocks.GRASS','CampaignMaps.selected==2?Blocks.SNOW:CampaignMaps.selected==3?Blocks.FERN:hash%deco.flowerEvery()==0?Blocks.POPPY:Blocks.GRASS')
s=s.replace('int h=height(x,z),color=river(x,z)?', 'int h=height(x,z),color=river(x,z)?')
s=s.replace('double k=1.0;', 'if(!river(x,z)&&!road(x,z)){if(CampaignMaps.selected==2)color=h>89?0x9bafb5:0xd4dedc;else if(CampaignMaps.selected==3)color=0x354d41;else if(CampaignMaps.selected==1)color=h>72?0x757965:0x7c8658;}\n        double k=.9+Math.min(.18,(height(x-1,z)-height(x+1,z))*.025);')
s=s.replace('for(int x=-100;x<=100;x+=20)', 'for(int x=-260;x<=260;x+=26)')
p.write_text(s)
p=base/'CampaignMaps.java';s=p.read_text().replace('Blocks.ARMOR_STAND==null?Blocks.LANTERN:Blocks.LANTERN','Blocks.LANTERN');p.write_text(s)
p=base/'Tactics.java';s=p.read_text().replace('ally.mob.heal((float)Spec.COMBATDISCIPLINE[0].healAmount())','ally.mob.heal((float)(Spec.COMBATDISCIPLINE[0].healAmount()*battle.campaign.damage(u)))').replace('u.spec.damage())','(float)(u.spec.damage()*battle.campaign.damage(u)))')
s=s.replace('u.nextAttack=time+u.spec.cooldown();mob.swingHand(Hand.MAIN_HAND);','u.nextAttack=time+(s.commander.equals("marksman")&&u.spec.group().equals("ranged")?(int)(u.spec.cooldown()*.9):u.spec.cooldown());mob.swingHand(Hand.MAIN_HAND);\n                    BattleEffects.strike(battle,u,enemy);')
p.write_text(s)
p=base/'CommandMode.java';s=p.read_text().replace('Math.abs(destination.x)>130||Math.abs(destination.z)>90','Math.abs(destination.x)>Graves and Glory.R.arenaHalfX()-10||Math.abs(destination.z)>Graves and Glory.R.arenaHalfZ()-10');p.write_text(s)
print('Campaign integrated')
