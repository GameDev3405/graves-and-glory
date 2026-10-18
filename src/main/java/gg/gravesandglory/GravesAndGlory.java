package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import java.util.*;

public final class GravesAndGlory implements ModInitializer {
    public static final Identifier CAMP = new Identifier("gravesandglory", "camp_station");
    public static final Identifier ORDER = new Identifier("gravesandglory", "order_v4");
    public static final Identifier PROFILE = new Identifier("gravesandglory", "profile_v1");
    public static final Identifier IMPACT = new Identifier("gravesandglory", "impact_v1");
    public static final Identifier PING = new Identifier("gravesandglory", "ping_v1");
    public static final Identifier TACTICAL = new Identifier("gravesandglory", "tactical_v1");
    public static final Identifier STATE = new Identifier("gravesandglory", "state_v5");
    public static final Identifier OPEN_BARRACKS = new Identifier("gravesandglory", "barracks_v1");
    public static final Identifier PARTY = new Identifier("gravesandglory", "party_v1");
    public static Battle battle;
    public static final Spec.RulesRow R = Spec.RULES[0];
    @Override public void onInitialize() {
        Diagnostics.start();
        VersionGate.registerServer();
        for(String cue:new String[]{"battle_intro","unit_reveal","victory","horn_charge","horn_rout","horn_rally"}){var id=new Identifier("gravesandglory",cue);net.minecraft.registry.Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));}
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Only the dedicated Graves and Glory save belongs to this mod.
            if (java.util.Set.of("Graves and Glory", "Bannerbound").contains(server.getSaveProperties().getLevelName())) battle = new Battle(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> battle=null);
        // Old saves decode our vanilla entity types as ordinary hostile mobs.
        // Catch chunks loaded after SERVER_STARTED, not only spawn-area entities.
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(battle!=null&&!CampWorld.generation.isEmpty()&&entity.getCommandTags().contains(CampWorld.TAG)&&!entity.getCommandTags().contains("camp-generation:"+CampWorld.generation))entity.discard();
            if(entity.getCommandTags().contains("gravesandglory-lobby")||entity.getCommandTags().contains("bannerbound-lobby"))entity.discard();
            if(battle!=null&&world==battle.world&&(entity.getCommandTags().contains("gravesandglory")||entity.getCommandTags().contains("bannerbound"))&&!(entity instanceof ArmyHusk)&&!(entity instanceof ArmySkeleton)&&!(entity instanceof ArmyWither))entity.discard();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { if(battle!=null) battle.tick(); });
        ServerPlayNetworking.registerGlobalReceiver(ORDER, (server, player, handler, buf, responder) -> {
            String command=buf.readString(128);
            server.execute(() -> { if(battle!=null) battle.command(player,command); });
        });
        ServerPlayNetworking.registerGlobalReceiver(PROFILE,(server,p,handler,buf,responder)->{String json=buf.readString(16000);server.execute(()->{if(battle!=null)battle.campaign.importProfile(p,json);});});
        ServerPlayNetworking.registerGlobalReceiver(PING,(server,p,handler,buf,responder)->{String kind=buf.readString(16);double x=buf.readDouble(),z=buf.readDouble();server.execute(()->{if(battle!=null)battle.ping(p,kind,x,z);});});
        ServerPlayNetworking.registerGlobalReceiver(TACTICAL, (server, player, handler, buf, responder) -> {
            String group=buf.readString(16), action=buf.readString(16), formation=buf.readString(16);
            double x=buf.readDouble(),z=buf.readDouble();float yaw=buf.readFloat();int target=buf.readInt();
            server.execute(()->{if(battle!=null)battle.tactical(player,group,action,formation,x,z,yaw,target);});
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if(battle!=null) battle.leave(handler.player.getUuid());
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount) -> {
            if(battle==null) return true;
            return battle.allowDamage(entity,source,amount);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            if(battle==null)return;
            Entity attacker=source.getAttacker();
            for(Unit victim:battle.units)if(victim.mob==entity){
                Squad winner=attacker==null?null:battle.squads.get(attacker.getUuid());
                for(Unit u:battle.units)if(u.mob==attacker)winner=u.squad;
                // A shot that lands after its archer fell still credits the archer's army.
                if(winner==null)winner=victim.lastHitBy;
                if(winner!=null&&winner.team!=victim.squad.team)winner.earn(Spec.ECONOMY[0].killGold());
            }
        });
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            String station=entity.getCommandTags().stream().filter(t->t.startsWith("camp:")).map(t->t.substring(5)).findFirst().orElse("");
            if(station.isEmpty())return ActionResult.PASS;
            if(!world.isClient&&player instanceof ServerPlayerEntity p&&battle!=null&&p.squaredDistanceTo(entity)<36)openCamp(p,station);
            return ActionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            String station=CampWorld.station(hit.getBlockPos());if(station.isEmpty()||battle==null)return ActionResult.PASS;
            if(!world.isClient&&player instanceof ServerPlayerEntity p&&p.squaredDistanceTo(Vec3d.ofCenter(hit.getBlockPos()))<36)openCamp(p,station);
            return ActionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!world.getBlockState(hit.getBlockPos()).isOf(net.minecraft.block.Blocks.LECTERN))return ActionResult.PASS;
            if(world.isClient)return ActionResult.SUCCESS;
            if(battle!=null&&player instanceof ServerPlayerEntity p&&world==battle.world){
                if(!battle.running){
                    // The custom lobby lectern opens the party tab. Every other lectern opens the war council.
                    var channel=hit.getBlockPos().equals(new BlockPos(3,66,CampaignMaps.LOBBY_Z+18))?PARTY:OPEN_BARRACKS;
                    if(ServerPlayNetworking.canSend(p,channel))ServerPlayNetworking.send(p,channel,PacketByteBufs.create());
                }else battle.openBarracks(p);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
    static void openCamp(ServerPlayerEntity p,String station){
        if(battle==null)return;var s=battle.squads.get(p.getUuid());if(battle.running&&s!=null&&s.seated)return;
        if(ServerPlayNetworking.canSend(p,CAMP)){var packet=PacketByteBufs.create();packet.writeString(station);ServerPlayNetworking.send(p,CAMP,packet);}
    }
    public static final class Squad {
        UUID owner;
        int team;
        String order="follow", formation="block", commander="vanguard";
        boolean bot, seated=true; int lane;
        /** A bot's difficulty: the id of its Spec.BOTCOMMAND row. Set from Campaign.difficulty when the round is prepared. */
        String tier="normal";
        Vec3d target;
        float yaw;
        long lastOrder=-100, reinforceAt;
        int gold=Spec.ECONOMY[0].startingGold();
        /** This round's tally (gold earned so far); Battle.startRound gives the squad a fresh one. */
        MatchStats.SquadStats stats=new MatchStats.SquadStats();
        /** Gold the squad earns (income, kills): into the bank and this round's tally. Starting gold and refunds go straight to gold. */
        void earn(int amount){gold+=amount;stats.goldEarned+=amount;}
        final Map<String,Assignment> assignments=new HashMap<>();
        final ArrayDeque<Training> training=new ArrayDeque<>();
        /** Morale per group: when a rout began (present only while routing), rout immunity after a regroup, and the group's full lineup health this round. */
        final Map<String,Long> routedAt=new HashMap<>(),steadyUntil=new HashMap<>();
        final Map<String,Double> fullHealth=new HashMap<>();
        Squad(UUID owner,int team,Vec3d pos) {this.owner=owner;this.team=team;target=pos;}
    }
    public static final class Assignment {
        String order,formation;Vec3d target;float yaw;int enemy=-1;long issued;
        Assignment(String o,String f,Vec3d t,float y){order=o;formation=f;target=t;yaw=y;}
    }
    public record Training(Spec.TroopsRow spec,long finished) {}
    public static final class Unit {
        MobEntity mob;
        Squad squad;
        Spec.TroopsRow spec;
        int slot;
        long nextAttack;long abilityAt,windupUntil;Vec3d spellCenter;
        long nextPath;
        Vec3d lastPosition,lastGoal,deployed;
        int stuckTicks;
        LivingEntity lockedTarget;long lockUntil;
        /** The army whose blow last hurt this unit; kill credit when the attacker itself is gone. */
        Squad lastHitBy;
        Unit(MobEntity m,Squad s,Spec.TroopsRow t,int slot){mob=m;squad=s;spec=t;this.slot=slot;lastPosition=m.getPos();deployed=m.getPos();}
    }
    public static final class Capture {
        Spec.PointsRow spec;
        int owner=-1, challenger=-1, progress=0;boolean contested;
        /** Battle time (ticks) when the current owner took the point. */
        long ownedSince;
        Capture(Spec.PointsRow spec){this.spec=spec;}
    }
    public static final class ArmyHusk extends HuskEntity {
        public ArmyHusk(World world){super(EntityType.HUSK,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    public static final class ArmySkeleton extends SkeletonEntity {
        public ArmySkeleton(World world){super(EntityType.SKELETON,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override public void updateAttackType() {}
    }
    public static final class ArmyWither extends WitherSkeletonEntity {
        public ArmyWither(World world){super(EntityType.WITHER_SKELETON,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override public void updateAttackType() {}
    }
    public static final class Battle {
        public final Campaign campaign;
        public final MinecraftServer server;
        public final ServerWorld world;
        public final LinkedHashMap<UUID,Squad> squads=new LinkedHashMap<>();
        public final List<Unit> units=new ArrayList<>();
        public final List<Capture> points=new ArrayList<>();
        public final List<Projectiles.Shot> shots=new ArrayList<>();
        final Map<UUID,Long> respawns=new HashMap<>();
        final int[] scores={0,0};
        final Set<UUID> welcomed=new HashSet<>();
        long time;
        public boolean ready=false;
        public boolean running=false;
        /** New troops deploy from a safely held point nearest the fight instead of the barracks; a check can switch it off. */
        public boolean forwardReinforcement=Spec.ECONOMY[0].forwardReinforcement();
        /** Round length in seconds, from the rules sheet; the bot simulation shortens it. */
        public int roundSeconds=R.roundSeconds();
        String message="Preparing the battlefield...";
        int buildColumn=0;
        int rounds=0;long roundStarted;
        record Ping(UUID owner,int team,String kind,double x,double z,long until){}
        final List<Ping> pings=new ArrayList<>();final Map<UUID,Long> lastPing=new HashMap<>();
        void ping(ServerPlayerEntity p,String kind,double x,double z){
            Squad s=squads.get(p.getUuid());var spec=Arrays.stream(Spec.PINGS).filter(v->v.id().equals(kind)).findFirst().orElse(null);
            if(s==null||!s.seated||!running||spec==null||!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>R.arenaHalfX()-10||Math.abs(z)>R.arenaHalfZ()-10||time-lastPing.getOrDefault(s.owner,-100L)<spec.cooldownSeconds()*20)return;
            lastPing.put(s.owner,time);pings.removeIf(v->v.owner.equals(s.owner)||v.until<time);pings.add(new Ping(s.owner,s.team,kind,x,z,time+spec.seconds()*20));
        }
        public volatile int testStage=0;
        long testAt=0;
        Vec3d testStart;
        Squad testSquad;
        Unit testVictim;
        float testVictimHealth;
        final Set<String> testedFormations=new HashSet<>();
        final Set<String> testedOrders=new HashSet<>();
        final Set<UUID> testOwners=new HashSet<>();
        int trainingGold,trainingMageBefore;
        int testBridgeIndex,hillClimbY;
        boolean bridgesPassed=true;
        public Battle(MinecraftServer server){
            this.server=server;world=server.getOverworld();campaign=new Campaign(this);
            for(var p:Spec.POINTS)points.add(new Capture(p));
            world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,server);
            world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false,server);
            world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);
            world.getGameRules().get(GameRules.DO_FIRE_TICK).set(false,server);
            world.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false,server);
            world.getGameRules().get(GameRules.DO_MOB_LOOT).set(false,server);
            world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true,server);
            world.setTimeOfDay(3000);
            world.setWeather(0,0,false,false);
            world.setSpawnPos(new BlockPos(-108,66,0),-90);
            server.setDifficulty(Difficulty.NORMAL,true);
            clearOldUnits();
            // Only inhabited chunks tick. Unit navigation tickets are maintained below.
            CampaignMaps.lobby(world);ready=campaign.cached();
        }
        public void tick(){
            time++;
            if(ready&&testStage<8&&Diagnostics.selfTest())selfTest();
            if(!ready){
                // The flat world starts far below this arena: keep arriving players safe
                // while their floor is constructed, before admitting them to the round.
                for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
                    p.changeGameMode(GameMode.ADVENTURE);
                    if(p.getZ()>-275||p.getY()<63)campaign.toLobby(p);
                }
                if(time%20==0)clearOldUnits();
                int total=(2*R.worldHalfX()+1)*(2*R.worldHalfZ()+1);
                for(int n=0;n<R.buildColumnsPerTick()&&buildColumn<total;n++,buildColumn++){
                    int x=buildColumn%(2*R.worldHalfX()+1)-R.worldHalfX();
                    int z=buildColumn/(2*R.worldHalfX()+1)-R.worldHalfZ();
                    Arena.column(world,x,z);
                }
                if(buildColumn>=total){
                    Arena.finish(world);campaign.built();ready=true;message="Your war council is ready. Press R.";
                    Diagnostics.report("arena-ready",true,null);
                }
                if(time%20==0){message="Preparing "+Spec.MAPS[campaign.map].name()+" / "+buildColumn*100/total+"%";sendState();}
                return;
            }
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
                if(!welcomed.contains(p.getUuid()))join(p);
                if(!squads.containsKey(p.getUuid())||!squads.get(p.getUuid()).seated)continue;
                Long until=respawns.get(p.getUuid());
                if(until!=null){
                    if(time>=until){respawns.remove(p.getUuid());p.changeGameMode(GameMode.ADVENTURE);equipPlayer(p);}
                } else if(running&&(Math.abs(p.getX())>R.arenaHalfX()-2||Math.abs(p.getZ())>R.arenaHalfZ()-2||p.getY()<56))toBase(p,squads.get(p.getUuid()).team);
            }
            units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
            if(running)trainingTick();
            campaign.tick();
            updateUnitChunks();
            if(running&&time>=campaign.introUntil){
                Projectiles.tick(this);
                if(time%20==0)Morale.tick(this);
                for(Unit u:new ArrayList<>(units))unitTick(u);
                if(time%20==0)captureTick();
                if(time%R.scoreInterval()==0){
                    for(Capture c:points)if(c.owner>=0)scores[c.owner]++;
                    for(int t=0;t<2;t++)if(scores[t]>=R.winScore()){
                        campaign.finish(t);
                        world.playSound(null,BlockPos.ORIGIN,SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundCategory.MASTER,2,1);
                    }
                }
                if(R.automaticReinforcements()&&time%20==0)for(Squad s:new ArrayList<>(squads.values())){if(!s.seated)continue;
                    if(time>=s.reinforceAt && units.stream().filter(u->u.squad==s).count()<R.squadSize()){
                        s.reinforceAt=time+R.reinforceSeconds()*20L;reinforce(s);
                    }
                }
                if(time%(Spec.ECONOMY[0].incomeSeconds()*20)==0)for(Squad s:squads.values()){if(!s.seated)continue;
                    s.earn((int)(Spec.ECONOMY[0].passiveGold()+Math.min(Spec.ECONOMY[0].objectiveIncomeCap(),points.stream().filter(p->p.owner==s.team).count()*Spec.ECONOMY[0].pointGold())));
                }
            }
            if(running&&time-roundStarted>=roundSeconds*20L)campaign.finish(scores[0]==scores[1]?-1:scores[0]>scores[1]?0:1);
            // Explicit revision checks are driven by the client test timeline.
            if(time%10==0)sendState();
        }
        void join(ServerPlayerEntity p){
            var decision=AdmissionPolicy.decide(running,campaign.practice,campaign.size,p.getUuid(),
                squads.values().stream().map(s->new AdmissionPolicy.Seat(s.owner,s.team,s.bot,s.seated)).toList(),campaign.vacated);
            if(running&&!campaign.activeRoom.isEmpty()){
                var room=campaign.social.rooms.get(campaign.activeRoom);
                if(room==null||!room.members.contains(p.getUuid()))decision=new AdmissionPolicy.Decision(decision.team(),null,false);
            }
            int team=decision.team();
            Squad replacement=decision.replacement()==null?null:squads.get(decision.replacement());
            Squad s;
            if(replacement!=null){
                s=replacement;squads.remove(s.owner);campaign.botRespawns.remove(s.owner);campaign.botRetreating.remove(s.owner);
                campaign.vacated.values().remove(s.owner);s.owner=p.getUuid();s.bot=false;s.seated=true;
            }else{
                s=new Squad(p.getUuid(),team,base(team));s.seated=decision.seated();
                if(s.seated)reinforce(s);
            }
            squads.put(s.owner,s);welcomed.add(s.owner);
            p.changeGameMode(GameMode.ADVENTURE);equipPlayer(p);
            campaign.joined(p);
            if(running&&s.seated)campaign.participants.add(s.owner);
            else if(running)campaign.participants.remove(s.owner);

        }
        void clearOldUnits(){List<Entity> stale=new ArrayList<>();for(Entity e:world.iterateEntities())if(e.getCommandTags().contains("gravesandglory")||e.getCommandTags().contains("bannerbound")||e.getCommandTags().contains(Projectiles.TAG))stale.add(e);for(Entity e:stale)e.discard();}
        void leave(UUID owner){
            campaign.left(owner);
            Squad s=squads.remove(owner);welcomed.remove(owner);respawns.remove(owner);
            // Mid-battle, a bot commands the army until its owner rejoins, so the team is not left a commander short.
            if(s!=null&&running&&s.seated&&!s.bot){UUID stand=UUID.randomUUID();s.owner=stand;s.bot=true;s.tier=campaign.difficulty;s.order="attack";squads.put(stand,s);campaign.vacated.put(owner,stand);return;}
            units.removeIf(u->{if(u.squad==s){u.mob.discard();return true;}return false;});
        }
        static Vec3d base(int t){var f=Spec.FACTIONS[t];return new Vec3d(f.spawnX(),Arena.height(f.spawnX(),f.spawnZ())+1,f.spawnZ());}
        void relocate(ServerPlayerEntity p,double x,double y,double z,float yaw,float pitch){p.fallDistance=0;p.setVelocity(Vec3d.ZERO);p.teleport(world,x,y,z,yaw,pitch);p.fallDistance=0;p.velocityModified=true;}
        void toBase(ServerPlayerEntity p,int team){Vec3d b=base(team);relocate(p,b.x,b.y+1,b.z,team==0?-90:90,8);}
        void equipPlayer(ServerPlayerEntity p){
            p.getInventory().clear();p.getInventory().insertStack(new ItemStack(Items.IRON_SWORD));
            p.getInventory().insertStack(new ItemStack(Items.BOW));p.getInventory().insertStack(new ItemStack(Items.ARROW,64));
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                Item item=switch(slot){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;default->Items.IRON_BOOTS;};
                p.equipStack(slot,unbreakable(item));
            }
            p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);
        }
        static ItemStack unbreakable(Item item){ItemStack s=new ItemStack(item);s.getOrCreateNbt().putBoolean("Unbreakable",true);return s;}
        public void command(ServerPlayerEntity p,String cmd){
            if(cmd.startsWith("social:")){campaign.socialCommand(p,cmd);return;}
            if(cmd.startsWith("progress:")){campaign.progressCommand(p,cmd);return;}
            Squad s=squads.get(p.getUuid());if(s==null||!ready||respawns.containsKey(s.owner))return;
            if(campaign.handle(p,cmd))return;
            if(running&&!s.seated)return;
            if(cmd.equals("barracks")){openBarracks(p);return;}
            if(cmd.startsWith("train:")||cmd.startsWith("disband:")||cmd.equals("cancel-training")){manage(p,s,cmd);return;}
            if(time-s.lastOrder<R.commandCooldownTicks())return;s.lastOrder=time;
            if(!running)return;
            for(var f:Spec.FORMATIONS)if(cmd.equals("formation:"+f.id())){
                s.formation=f.id();s.yaw=p.getYaw();for(Assignment a:s.assignments.values())a.formation=f.id();
                if(Diagnostics.active()&&testedFormations.add(f.id())&&testedFormations.size()==Spec.FORMATIONS.length)Diagnostics.report("formations-commanded",true,null);
                campaign.tutorialDid(p,1);return;
            }
            for(var order:Spec.ORDERS)if(cmd.equals(order.id())){
                boolean charging=order.id().equals("charge")&&startsCharge(s,"all");
                s.assignments.clear();s.order=order.id();s.yaw=p.getYaw();if(charging)chargeHorn(s,"all");
                campaign.tutorialDid(p,order.id().equals("attack")||order.id().equals("charge")?2:0);
                if(Diagnostics.active()&&testedOrders.add(order.id())&&testedOrders.size()==Spec.ORDERS.length)Diagnostics.report("orders-commanded",true,null);
                if(order.targeted()){
                    // Target is derived on the server, never trusted from the client.
                    HitResult hit=p.raycast(R.orderRange(),0,false);
                    Vec3d target=hit.getType()==HitResult.Type.MISS?p.getPos().add(p.getRotationVec(1).multiply(32)):hit.getPos();
                    int tx=MathHelper.clamp((int)target.x,-R.arenaHalfX()+10,R.arenaHalfX()-10),tz=MathHelper.clamp((int)target.z,-R.arenaHalfZ()+10,R.arenaHalfZ()-10);
                    if(Arena.river(tx,tz)&&Arena.ground(tx,tz)<64)tx=(int)Arena.riverX(tz)+(tx>Arena.riverX(tz)?12:-12);
                    s.target=new Vec3d(tx,Arena.ground(tx,tz)+1,tz);
                }else s.target=p.getPos();
                return;
            }
        }
        /** The order a group is under. A routing group runs for base; its stored order waits untouched for the regroup. */
        Assignment assignment(Unit u){if(Morale.routing(u.squad,u.spec.group()))return new Assignment("retreat","loose",base(u.squad.team),u.squad.team==0?-90:90);return storedAssignment(u.squad,u.spec.group());}
        Assignment storedAssignment(Squad s,String group){return s.assignments.getOrDefault(group,new Assignment(s.order,s.formation,s.target,s.yaw));}
        /** Sounds the charge horn at the centre of the named group(s) that are not routing. */
        void chargeHorn(Squad s,String group){
            Vec3d sum=Vec3d.ZERO;int n=0;for(Unit u:units)if(u.squad==s&&u.mob.isAlive()&&(group.equals("all")||u.spec.group().equals(group))&&!Morale.routing(s,u.spec.group())){sum=sum.add(u.mob.getPos());n++;}
            BattleEffects.horn(this,s,n==0?s.target:sum.multiply(1.0/n),"charge");
        }
        boolean startsCharge(Squad s,String group){return Arrays.stream(Morale.GROUPS).anyMatch(g->(group.equals("all")||group.equals(g))&&!Morale.routing(s,g)&&!storedAssignment(s,g).order.equals("charge"));}
        public void tactical(ServerPlayerEntity p,String group,String action,String formation,double x,double z,float yaw,int enemyId){
            Squad s=squads.get(p.getUuid());
            if(s==null||!ready||!running||!s.seated||time<campaign.introUntil||respawns.containsKey(s.owner)||time-s.lastOrder<R.commandCooldownTicks())return;
            if(Arrays.stream(Spec.GROUPS).noneMatch(g->g.id().equals(group))||Arrays.stream(Spec.FORMATIONS).noneMatch(f->f.id().equals(formation)))return;
            if(action.equals("formation")){
                for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id()))){Assignment a=s.assignments.computeIfAbsent(g.id(),k->new Assignment(s.order,s.formation,s.target,s.yaw));a.formation=formation;}
                s.lastOrder=time;campaign.tutorialDid(p,1);return;
            }
            var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(action)).findFirst().orElse(null);
            if(order==null||!Double.isFinite(x)||!Double.isFinite(z)||!Float.isFinite(yaw)||Math.abs(x)>R.arenaHalfX()-10||Math.abs(z)>R.arenaHalfZ()-10)return;
            if(!action.equals("attack")&&!action.equals("charge"))enemyId=-1;
            Entity enemy=world.getEntityById(enemyId);
            if(enemyId>=0&&(enemy==null||!enemy.isAlive()||teamOf(enemy)<0||teamOf(enemy)==s.team||!p.canSee(enemy))){p.sendMessage(Text.literal("Target lost. Aim again or issue an attack-move."),true);return;}
            if(action.equals("charge")&&enemy==null){p.sendMessage(Text.literal("Charge needs a visible enemy. Use Attack to advance to terrain."),true);return;}
            if(Arena.river((int)x,(int)z)&&Arena.ground((int)x,(int)z)<64){p.sendMessage(Text.literal("Choose dry ground or a crossing."),true);return;}
            s.lastOrder=time;Vec3d dest=new Vec3d(x,Arena.ground((int)x,(int)z)+1,z);boolean charging=action.equals("charge")&&startsCharge(s,group);
            if(action.equals("follow"))dest=p.getPos();
            for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id()))){
                Assignment a=new Assignment(action,formation,dest,yaw);a.enemy=enemyId;a.issued=time;s.assignments.put(g.id(),a);
            }
            if(charging)chargeHorn(s,group);
            for(Unit u:units)if(u.squad==s&&(group.equals("all")||u.spec.group().equals(group))){u.lockedTarget=null;u.spellCenter=null;u.mob.setTarget(null);u.mob.getNavigation().stop();u.nextPath=0;u.stuckTicks=0;}
            campaign.tutorialDid(p,action.equals("attack")||action.equals("charge")?2:0);campaign.progressEvent(p.getUuid(),false);
            p.playSound(action.equals("retreat")?SoundEvents.BLOCK_NOTE_BLOCK_BELL.value():SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),.6f,action.equals("charge")?.7f:1.1f);
            p.sendMessage(Text.literal(group+" / "+action+" / "+(int)x+", "+(int)z),true);
        }
        Vec3d formation(Unit u){
            Assignment a=assignment(u);if(!u.squad.bot&&u.squad.order.equals("hold")&&!u.squad.assignments.containsKey(u.spec.group())&&!Morale.routing(u.squad,u.spec.group()))return u.deployed;var f=Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(a.formation)).findFirst().orElse(Spec.FORMATIONS[0]);
            int slot=0;for(Unit other:units)if(other.squad==u.squad&&other.spec.group().equals(u.spec.group())&&other.mob.isAlive()&&other.slot<u.slot)slot++;
            double role=u.spec.group().equals("support")?9:u.spec.group().equals("ranged")?5:0;
            int columns=Math.min(f.columns(),u.spec.group().equals("support")?2:u.spec.group().equals("ranged")?6:12);
            double side=(slot%columns-(columns-1)/2.0)*f.spacing(),back=role+(slot/columns+1)*f.spacing();
            if(f.wedge()){int rank=(int)Math.floor(Math.sqrt(slot));side=(slot-rank*rank-rank)*f.spacing();back=role+(rank+1)*f.spacing();}
            double angle=Math.toRadians(a.yaw);return a.target.add(Math.cos(angle)*side+Math.sin(angle)*back,0,Math.sin(angle)*side-Math.cos(angle)*back);
        }
        public boolean atBarracks(ServerPlayerEntity p,Squad s){
            var f=Spec.FACTIONS[s.team];return horizontalDistance(p.getPos(),new Vec3d(f.barracksX(),65,f.barracksZ()))<=Spec.ECONOMY[0].accessRadius()*Spec.ECONOMY[0].accessRadius();
        }
        void openBarracks(ServerPlayerEntity p){if(running)campaign.tutorialDid(p,3);if(ServerPlayNetworking.canSend(p,OPEN_BARRACKS))ServerPlayNetworking.send(p,OPEN_BARRACKS,PacketByteBufs.create());}
        void manage(ServerPlayerEntity p,Squad s,String cmd){
            if(!atBarracks(p,s)){p.sendMessage(Text.literal("Return to your faction's barracks to train or disband troops."),true);return;}
            if(cmd.equals("cancel-training")){for(Training t:s.training)s.gold+=t.spec().cost();s.training.clear();return;}
            String id=cmd.substring(cmd.indexOf(':')+1);
            var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)&&t.faction().equals(Spec.FACTIONS[s.team].id())).findFirst().orElse(null);
            if(spec==null||!campaign.unlocked(s,spec.id()))return;
            if(cmd.startsWith("disband:")){
                for(int i=units.size()-1;i>=0;i--){Unit u=units.get(i);if(u.squad==s&&u.spec==spec&&u.mob.isAlive()){u.mob.discard();units.remove(i);s.gold+=Spec.ECONOMY[0].disbandRefund();return;}}
                return;
            }
            long alive=units.stream().filter(u->u.squad==s&&u.mob.isAlive()).count();
            if(alive+s.training.size()>=R.squadSize()){p.sendMessage(Text.literal("Army full: disband a troop to make room for a different unit."),true);return;}
            if(s.gold<spec.cost()){p.sendMessage(Text.literal("Not enough gold. Battle supplies arrive over time; holding points increases them."),true);return;}
            if(s.training.size()>=Spec.ECONOMY[0].maxQueue())return;
            s.gold-=spec.cost();long previous=s.training.isEmpty()?time:s.training.getLast().finished();
            s.training.addLast(new Training(spec,Math.max(time,previous)+spec.trainingSeconds()*20L));
        }
        void trainingTick(){
            for(Squad s:squads.values())while(!s.training.isEmpty()&&s.training.getFirst().finished()<=time){
                Training t=s.training.removeFirst();Set<Integer> used=new HashSet<>();for(Unit u:units)if(u.squad==s&&u.mob.isAlive())used.add(u.slot);
                int slot=0;while(used.contains(slot))slot++;
                if(slot>=R.squadSize()){s.gold+=t.spec().cost();continue;}
                var at=deploy(s,slot);spawn(s,t.spec(),slot,at.at());Morale.fielded(this,s,t.spec().group());
                var player=server.getPlayerManager().getPlayer(s.owner);if(player!=null)player.sendMessage(Text.literal(t.spec().name()+" trained: "+deployNote(s,at.anchor())),true);
            }
        }
        /** Points this team has held long enough, uncontested and with no enemy near: where new troops may deploy instead of the barracks. Phase 2 banners add their anchors here. */
        List<Capture> reinforcementAnchors(int team){
            var E=Spec.ECONOMY[0];List<Capture> anchors=new ArrayList<>();
            for(Capture p:points)if(p.owner==team&&!p.contested&&time-p.ownedSince>=E.forwardHoldSeconds()*20L&&!Morale.enemyWithin(this,team,new Vec3d(p.spec.x(),p.spec.elevation()+1,p.spec.z()),E.forwardSafeRadius()))anchors.add(p);
            return anchors;
        }
        /** Where a troop trained now appears, and the held point it deploys at (null: the barracks). Chosen at spawn time, so a point that was lost, contested or approached since the purchase sends the troop home. */
        record Deploy(Vec3d at,Capture anchor){}
        Deploy deploy(Squad s,int slot){
            var E=Spec.ECONOMY[0];Vec3d home=base(s.team),ring=new Vec3d((slot%5-2)*2,0,(slot/5-2)*2),barracks=home.add(ring);
            if(!forwardReinforcement)return new Deploy(barracks,null);
            List<Capture> anchors=reinforcementAnchors(s.team);if(anchors.isEmpty())return new Deploy(barracks,null);
            Vec3d target=storedAssignment(s,"infantry").target;double[] ax=new double[anchors.size()],az=new double[anchors.size()];
            for(int i=0;i<ax.length;i++){ax[i]=anchors.get(i).spec.x();az[i]=anchors.get(i).spec.z();}
            int chosen=BattleMath.nearestAnchor(target.x,target.z,home.x,home.z,ax,az);if(chosen<0)return new Deploy(barracks,null);
            Capture anchor=anchors.get(chosen);Vec3d back=new Vec3d(home.x-ax[chosen],0,home.z-az[chosen]);back=back.lengthSquared()<1e-6?Vec3d.ZERO:back.normalize().multiply(E.forwardSpawnOffset());
            double x=ax[chosen]+back.x+ring.x,z=az[chosen]+back.z+ring.z;int gx=(int)Math.floor(x),gz=(int)Math.floor(z);
            if(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)return new Deploy(barracks,null);
            return new Deploy(new Vec3d(x,Arena.ground(gx,gz)+1,z),anchor);
        }
        Vec3d deployPoint(Squad s,int slot){return deploy(s,slot).at();}
        /** The deploy notice after "<troop> trained: " (a null anchor means the barracks). */
        String deployNote(Squad s,Capture anchor){return anchor==null?"deploying from barracks":"deploying at "+anchor.spec.name();}
        void startRound(){
            Projectiles.clear(this);
            campaign.prepareRound();
            Arrays.fill(scores,0);
            for(Capture p:points){p.owner=-1;p.progress=0;p.challenger=-1;Arena.flag(world,p.spec,-1);}
            respawns.clear();
            for(Squad s:squads.values()){
                if(!s.seated)continue;s.assignments.clear();s.training.clear();s.routedAt.clear();s.steadyUntil.clear();s.fullHealth.clear();s.gold=Spec.ECONOMY[0].startingGold();s.stats=new MatchStats.SquadStats();s.order=s.bot?"attack":"hold";s.target=base(s.team);s.reinforceAt=time+R.reinforceSeconds()*20L;
                var player=server.getPlayerManager().getPlayer(s.owner);
                if(player!=null){toBase(player,s.team);player.changeGameMode(GameMode.ADVENTURE);equipPlayer(player);}
                reinforce(s);
            }
            rounds++;roundStarted=time;running=true;message="Capture the objectives. Your army awaits your orders.";
        }
        void reinforce(Squad s){
            Set<Integer> occupied=new HashSet<>();for(Unit u:units)if(u.squad==s&&u.mob.isAlive())occupied.add(u.slot);
            int slot=0,index=0,count=1;for(Squad other:squads.values()){if(other==s)break;if(other.seated&&other.team==s.team)index++;}
            for(Squad other:squads.values())if(other!=s&&other.seated&&other.team==s.team)count++; // this team's armies, this one included (a joining squad is not in squads yet)
            List<String> line;if(s.bot){line=new ArrayList<>();for(var t:Spec.TROOPS)if(t.faction().equals(Spec.FACTIONS[s.team].id()))for(int n=0;n<t.count();n++)line.add(t.id());}else line=campaign.profile(s.owner).lineup(s.team);
            Morale.lineup(this,s,line);
            for(String id:line){final String type=id;int position=slot++;if(position>=R.squadSize()||occupied.contains(position)||id.equals("empty")||!campaign.unlocked(s,id))continue;
                var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(type)).findFirst().orElse(null);if(spec==null)continue;
                double side=s.team==0?1:-1,x=base(s.team).x+side*(20-position/5*3),z=base(s.team).z+side*((index-(count-1)/2.0)*22+(position%5-2)*2.5); // rows centred on the base for any army count; Dusk's mirror Dawn's, so army i forms on the side of its lane point (BattleMath.laneTarget)
                spawn(s,spec,position,new Vec3d(x,Arena.ground((int)x,(int)z)+1,z));
            }
        }

        Unit spawn(Squad s,Spec.TroopsRow spec,int slot,Vec3d pos){
            MobEntity mob=switch(spec.entity()){case "skeleton"->new ArmySkeleton(world);case "wither_skeleton"->new ArmyWither(world);default->new ArmyHusk(world);};
            mob.refreshPositionAndAngles(pos.x,pos.y,pos.z,s.team==0?-90:90,0);
            mob.setPersistent();mob.addCommandTag("gravesandglory");
            mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(spec.health()*campaign.health(s,spec));mob.setHealth((float)(spec.health()*campaign.health(s,spec)));
            mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(spec.speed());
            mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(Spec.NAVIGATION[0].searchRange());
            mob.setPathfindingPenalty(PathNodeType.WATER,(float)Spec.NAVIGATION[0].waterPenalty());
            if(spec.ability().equals("shield"))mob.equipStack(EquipmentSlot.OFFHAND,unbreakable(Items.SHIELD));
            mob.equipStack(EquipmentSlot.MAINHAND,unbreakable(Registries.ITEM.get(new Identifier(spec.weapon()))));
            mob.equipStack(EquipmentSlot.HEAD,unbreakable(Registries.ITEM.get(new Identifier(spec.helmet()))));
            ItemStack chest=unbreakable(Registries.ITEM.get(new Identifier(spec.chest())));
            if(chest.getItem() instanceof DyeableItem dye)dye.setColor(chest,Spec.FACTIONS[s.team].color());
            mob.equipStack(EquipmentSlot.CHEST,chest);
            for(EquipmentSlot eq:EquipmentSlot.values())mob.setEquipmentDropChance(eq,0);
            mob.setCustomName(Text.literal(spec.name()).formatted(s.team==0?Formatting.AQUA:Formatting.LIGHT_PURPLE));
            mob.setCustomNameVisible(false);world.spawnEntity(mob);
            Unit u=new Unit(mob,s,spec,slot);units.add(u);return u;
        }
        final Set<Long> unitChunks=new HashSet<>();
        void updateUnitChunks(){
            if(time%40!=0)return;Set<Long> wanted=new HashSet<>();
            if(running)for(Unit u:units)if(u.mob.isAlive()){int x=u.mob.getBlockX()>>4,z=u.mob.getBlockZ()>>4;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)wanted.add(net.minecraft.util.math.ChunkPos.toLong(x+dx,z+dz));}
            for(long key:wanted)if(!unitChunks.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,true);}
            for(long key:unitChunks)if(!wanted.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,false);}
            unitChunks.clear();unitChunks.addAll(wanted);
        }
        void unitTick(Unit u){ Tactics.tick(this,u,time); }
        void captureTick(){
            for(Capture p:points){
                int[] presence={0,0};Vec3d center=new Vec3d(p.spec.x(),65,p.spec.z());
                for(Unit u:units)if(u.mob.isAlive()&&horizontalDistance(u.mob.getPos(),center)<p.spec.radius()*p.spec.radius())presence[u.squad.team]++;
                for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList()){
                    Squad s=squads.get(player.getUuid());
                    if(s!=null&&s.seated&&!player.isSpectator()&&horizontalDistance(player.getPos(),center)<p.spec.radius()*p.spec.radius())presence[s.team]+=R.capturePlayersWeight();
                }
                p.contested=presence[0]>0&&presence[1]>0;
                int team=presence[0]>0&&presence[1]==0?0:presence[1]>0&&presence[0]==0?1:-1;
                if(team<0||team==p.owner){if(team==p.owner){p.progress=0;p.challenger=-1;}continue;}
                if(p.challenger!=team){p.challenger=team;p.progress=0;}
                p.progress++;
                if(p.progress>=p.spec.captureSeconds()){
                    p.owner=team;p.ownedSince=time;p.challenger=-1;p.progress=0;Arena.flag(world,p.spec,team);
                    for(ServerPlayerEntity pl:server.getPlayerManager().getPlayerList()){var squad=squads.get(pl.getUuid());if(squad!=null&&squad.seated&&squad.team==team&&horizontalDistance(pl.getPos(),center)<p.spec.radius()*p.spec.radius())campaign.progressEvent(pl.getUuid(),true);}
                    for(ServerPlayerEntity pl:server.getPlayerManager().getPlayerList())if(squads.containsKey(pl.getUuid())&&squads.get(pl.getUuid()).seated)pl.sendMessage(Text.literal(Spec.FACTIONS[team].name()+" captured "+p.spec.name()).formatted(team==0?Formatting.AQUA:Formatting.LIGHT_PURPLE),true);
                }
            }
        }
        static double horizontalDistance(Vec3d a,Vec3d b){double x=a.x-b.x,z=a.z-b.z;return x*x+z*z;}
        int teamOf(Entity e){
            if(e==null)return -1;
            Squad s=squads.get(e.getUuid());if(s!=null)return s.seated?s.team:-1;
            for(Unit u:units)if(u.mob==e)return u.squad.team;return -1;
        }
        boolean scalingDamage;
        boolean allowDamage(LivingEntity entity,net.minecraft.entity.damage.DamageSource source,float amount){
            Entity attacker=source.getAttacker();
            Squad victimSquad=squads.get(entity.getUuid());
            Squad attackerSquad=attacker==null?null:squads.get(attacker.getUuid());
            if(victimSquad!=null&&!victimSquad.seated||attackerSquad!=null&&!attackerSquad.seated)return false;
            int team=teamOf(entity),attackTeam=teamOf(attacker);
            if(team<0)return true;
            if(!running)return false;
            if(!R.friendlyFire()&&attackTeam==team)return false;
            if(!scalingDamage&&attacker instanceof ServerPlayerEntity&&!(entity instanceof ServerPlayerEntity)){
                scalingDamage=true;try{entity.damage(source,(float)(amount*Spec.COMBATDISCIPLINE[0].damageScale()));}finally{scalingDamage=false;}return false;
            }
            if(entity instanceof ServerPlayerEntity p&&p.getHealth()<=amount){
                if(Diagnostics.active())System.out.println("[Graves and Glory damage] respawn cause="+source.getType().msgId()+" amount="+amount+" hp="+p.getHealth()+" pos="+p.getPos()+" fall="+p.fallDistance);
                p.setHealth(p.getMaxHealth());toBase(p,team);p.changeGameMode(GameMode.SPECTATOR);
                respawns.put(p.getUuid(),time+R.respawnSeconds()*20L);return false;
            }
            Squad hitter=attackerSquad;for(Unit u:units)if(u.mob==attacker)hitter=u.squad;
            if(hitter!=null)for(Unit u:units)if(u.mob==entity){u.lastHitBy=hitter;break;}
            return true;
        }
        record Sight(float x,float z,long seen){}
        final Map<Integer,Map<UUID,Sight>> sightings=new HashMap<>();
        void updateSight(){for(int team=0;team<2;team++){var memory=sightings.computeIfAbsent(team,k->new HashMap<>());memory.values().removeIf(v->time-v.seen>Spec.SCOUTING[0].memorySeconds()*20);
            for(Unit enemy:units)if(enemy.squad.team!=team&&enemy.mob.isAlive()){
                boolean visible=false;for(Unit ally:units)if(ally.squad.team==team&&ally.mob.isAlive()&&ally.mob.squaredDistanceTo(enemy.mob)<Spec.SCOUTING[0].range()*Spec.SCOUTING[0].range()&&ally.mob.canSee(enemy.mob)){visible=true;break;}
                if(!visible)for(var player:server.getPlayerManager().getPlayerList()){var s=squads.get(player.getUuid());if(s!=null&&s.seated&&s.team==team&&!player.isSpectator()&&player.squaredDistanceTo(enemy.mob)<Spec.SCOUTING[0].range()*Spec.SCOUTING[0].range()&&player.canSee(enemy.mob)){visible=true;break;}}
                if(visible)memory.put(enemy.mob.getUuid(),new Sight((float)enemy.mob.getX(),(float)enemy.mob.getZ(),time));
            }
        }}
        void sendState(){
            updateSight();
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())if(ServerPlayNetworking.canSend(p,STATE)){
                Squad s=squads.get(p.getUuid());PacketByteBuf b=PacketByteBufs.create();
                b.writeBoolean(ready);b.writeBoolean(running);b.writeInt(scores[0]);b.writeInt(scores[1]);
                b.writeString(message);b.writeInt(s==null?-1:s.team);
                b.writeInt(s==null?0:(int)units.stream().filter(u->u.squad==s&&u.mob.isAlive()).count());
                b.writeString(s==null?"follow":s.order);b.writeString(s==null?"block":s.formation);
                b.writeInt(respawns.containsKey(p.getUuid())?(int)((respawns.get(p.getUuid())-time+19)/20):0);
                b.writeInt(points.size());for(Capture c:points){b.writeString(c.spec.id());b.writeInt(c.owner);b.writeInt(c.progress);b.writeInt(c.challenger);b.writeBoolean(c.contested);}
                b.writeInt(s==null?0:s.gold);b.writeBoolean(s!=null&&atBarracks(p,s));
                b.writeInt(s==null?0:s.training.size());b.writeInt(s==null||s.training.isEmpty()?0:(int)Math.max(0,(s.training.getFirst().finished()-time+19)/20));
                b.writeInt(Spec.TROOPS.length);
                for(var spec:Spec.TROOPS){b.writeInt(s==null?0:(int)units.stream().filter(u->u.squad==s&&u.spec==spec&&u.mob.isAlive()).count());b.writeInt(s==null?0:(int)s.training.stream().filter(t->t.spec()==spec).count());}
                List<Unit> friendly=units.stream().filter(u->s!=null&&u.squad.team==s.team&&u.mob.isAlive()).toList();
                b.writeInt(friendly.size());
                for(Unit u:friendly){b.writeFloat((float)u.mob.getX());b.writeFloat((float)u.mob.getZ());b.writeBoolean(u.mob.getTarget()!=null);b.writeBoolean(u.squad==s);b.writeString(u.spec.name());}
                b.writeInt(3);
                for(var g:Spec.GROUPS)if(!g.id().equals("all")){
                    var members=units.stream().filter(u->u.squad==s&&u.spec.group().equals(g.id())&&u.mob.isAlive()).toList();
                    Assignment a=s==null?new Assignment("follow","block",Vec3d.ZERO,0):storedAssignment(s,g.id());
                    b.writeString(g.id());b.writeInt(members.size());b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getHealth()/u.mob.getMaxHealth()).average().orElse(0));
                    b.writeString(a.order);b.writeString(a.formation);b.writeFloat((float)a.target.x);b.writeFloat((float)a.target.z);b.writeFloat(a.yaw);
                    b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getX()).average().orElse(a.target.x));b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getZ()).average().orElse(a.target.z));
                    b.writeBoolean(members.stream().anyMatch(u->u.mob.getTarget()!=null));b.writeBoolean(s!=null&&Morale.routing(s,g.id()));
                }
                var visiblePings=pings.stream().filter(v->s!=null&&v.team==s.team&&v.until>time).toList();b.writeInt(visiblePings.size());for(var ping:visiblePings){b.writeString(ping.kind);b.writeFloat((float)ping.x);b.writeFloat((float)ping.z);}
                b.writeInt(running?(int)Math.max(0,roundSeconds-(time-roundStarted)/20):roundSeconds);
                var memory=s==null?Map.<UUID,Sight>of():sightings.getOrDefault(s.team,Map.of());b.writeInt(memory.size());for(var sight:memory.values()){b.writeFloat(sight.x);b.writeFloat(sight.z);b.writeInt((int)(time-sight.seen));}
                b.writeString(campaign.snapshot(p));
                ServerPlayNetworking.send(p,STATE,b);
            }
        }
        // Runs only under Melty's explicit test file; uses actual pathfinding, capture and combat.
        void selfTest(){
            if(squads.isEmpty())return;
            if(testStage==0){
                for(var s:squads.values())s.seated=true;startRound();testSquad=squads.values().iterator().next();
                var point=points.get(0).spec;
                for(Unit u:units)if(u.squad==testSquad)u.mob.refreshPositionAndAngles(point.x()-22+(u.slot%5)*1.4,65,point.z()+(u.slot/5)*1.4,0,0);
                testSquad.order="move";testSquad.target=new Vec3d(point.x(),65,point.z());testSquad.yaw=180;
                testStart=units.get(0).mob.getPos();testAt=campaign.introUntil;testStage=1; // units do not tick until the round intro ends, so the wait starts there
            } else if(testStage==1&&time-testAt>100){
                long living=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()).count();
                boolean moved=units.get(0).mob.squaredDistanceTo(testStart)>9;
                Diagnostics.report("troops-commanded",living==20&&moved,"expected twenty troops moving toward ordered point");
                testStage=2;testAt=time;
            } else if(testStage==2){
                if(points.get(0).owner==testSquad.team){
                    Diagnostics.report("point-captured",true,null);
                    Squad enemy=new Squad(UUID.randomUUID(),1-testSquad.team,testSquad.target.add(6,0,0));
                    enemy.order="hold";
                    var spec=Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(Spec.FACTIONS[enemy.team].id())).findFirst().orElseThrow();
                    testVictim=spawn(enemy,spec,0,testSquad.target.add(4,0,0));testVictimHealth=testVictim.mob.getHealth();
                    testSquad.order="attack";testStage=3;testAt=time;
                }else if(time-testAt>1000){Diagnostics.report("point-captured",false,"troops did not take point");testStage=4;}
            }else if(testStage==3&&time-testAt>100){
                Diagnostics.report("combat-resolved",!testVictim.mob.isAlive()||testVictim.mob.getHealth()<testVictimHealth,"enemy took no damage");
                for(Unit u:units)if(u.squad!=testSquad)u.mob.discard();
                units.removeIf(u->u.squad!=testSquad);
                reinforce(testSquad);testSquad.order="hold";
                for(int i=0;i<3;i++){
                    int team=i==0?testSquad.team:1-testSquad.team;
                    Squad synthetic=new Squad(UUID.randomUUID(),team,base(team).add(0,0,i==0?28:(i-1)*35-20));
                    synthetic.order="hold";testOwners.add(synthetic.owner);
                    int slot=0;
                    for(var spec:Spec.TROOPS)if(spec.faction().equals(Spec.FACTIONS[team].id()))for(int n=0;n<spec.count();n++,slot++)spawn(synthetic,spec,slot,synthetic.target.add((slot%5-2)*2,0,slot/5*2));
                }
                testStage=4;testAt=time;
            }else if(testStage==4&&time-testAt>100){
                long living=units.stream().filter(u->u.mob.isAlive()).count();
                double ms=server.getTickTime();
                Diagnostics.report("army-capacity",living==80,"expected eighty living troops, got "+living);
                System.out.println("[Graves and Glory test] Eighty-troop server average tick: "+ms+"ms; players="+server.getPlayerManager().getCurrentPlayerCount());
                testBridgeIndex=0;prepareBridgeTest();testStage=5;testAt=time;
            }else if(testStage==5){
                var bridge=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList().get(testBridgeIndex);
                long crossed=units.stream().filter(u->u.squad==testSquad&&u.mob.getX()>bridge.x()+bridge.width()/2.0-4&&u.mob.getY()>63).count();
                if(crossed==20||time-testAt>900){
                    bridgesPassed &= crossed==20;
                    System.out.println("[Graves and Glory test] "+bridge.id()+": "+crossed+"/20 crossed without teleporting.");
                    testBridgeIndex++;
                    if(testBridgeIndex<Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).count()){prepareBridgeTest();testAt=time;}
                    else{Diagnostics.report("river-navigation",bridgesPassed,"one or more bridges failed to carry all twenty troops");prepareHillTest();testStage=6;testAt=time;}
                }
            }else if(testStage==6){
                long climbed=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()&&u.mob.getY()>=hillClimbY).count();
                if(climbed==20||time-testAt>700){
                    Diagnostics.report("hill-navigation",climbed==20,"only "+climbed+" of twenty troops climbed to y>="+hillClimbY+" on the ridge");
                    var p=server.getPlayerManager().getPlayer(testSquad.owner);var f=Spec.FACTIONS[testSquad.team];
                    p.teleport(world,f.barracksX(),65,f.barracksZ(),0,0);
                    String melee=Spec.TROOPS[testSquad.team==0?0:3].id(),mage=Spec.TROOPS[testSquad.team==0?2:5].id();
                    trainingMageBefore=(int)units.stream().filter(u->u.squad==testSquad&&u.spec.id().equals(mage)&&u.mob.isAlive()).count();
                    trainingGold=testSquad.gold;manage(p,testSquad,"disband:"+melee);manage(p,testSquad,"train:"+mage);
                    int after=testSquad.gold;manage(p,testSquad,"train:"+mage);
                    boolean paid=after==trainingGold-Spec.TROOPS[testSquad.team==0?2:5].cost()&&testSquad.gold==after&&testSquad.training.size()==1;
                    if(!paid)Diagnostics.report("barracks-management",false,"gold or capacity was not enforced");
                    Unit own=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()).findFirst().orElseThrow();
                    float hp=own.mob.getHealth();boolean damage=own.mob.damage(world.getDamageSources().mobAttack(own.mob),8);
                    Diagnostics.report("friendly-fire",!damage&&own.mob.getHealth()==hp,"same-faction damage was allowed");
                    testStage=7;testAt=time;
                }
            }else if(testStage==7&&testSquad.training.isEmpty()&&time-testAt>120){
                String mage=Spec.TROOPS[testSquad.team==0?2:5].id();
                long alive=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()).count(),mages=units.stream().filter(u->u.squad==testSquad&&u.spec.id().equals(mage)&&u.mob.isAlive()).count();
                Diagnostics.report("barracks-management",alive==20&&mages==trainingMageBefore+1,"trained replacement or roster swap did not complete");
                testStage=8;
            }
        }
        void prepareNavigation(double x,double z,Vec3d target,float yaw){
            testSquad.order="move";testSquad.target=target;testSquad.yaw=yaw;
            for(Unit u:units)if(u.squad==testSquad){double px=x+(u.slot%5)*1.5,pz=z+(u.slot/5-1.5)*1.4;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(px,Arena.ground((int)px,(int)pz)+1,pz,yaw,0);u.nextPath=0;u.stuckTicks=0;}
        }
        /** Rivergate's dawn ridge is the climb: troops start on its flat eastern foot, march west to the crest, and must end at least eight blocks above where they began. */
        void prepareHillTest(){
            var ridge=Arrays.stream(Spec.TERRAIN).filter(t->t.id().equals("dawn_ridge")).findFirst().orElseThrow();
            int startX=ridge.x()+ridge.radiusX()*4/5,crestX=ridge.x()+6,z=ridge.z();
            hillClimbY=Arena.ground(startX,z)+8;
            prepareNavigation(startX,z,new Vec3d(crestX,Arena.height(crestX,z)+1,z),90);
        }
        void prepareBridgeTest(){var b=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList().get(testBridgeIndex);prepareNavigation(b.x()-b.width()/2.0-14,b.z(),new Vec3d(b.x()+b.width()/2.0+16,65,b.z()),-90);}
        void prepareVisualSquads(boolean fight){
            for(Unit u:units)u.mob.discard();units.clear();
            testSquad.training.clear();testSquad.order=fight?"attack":"hold";testSquad.target=fight?new Vec3d(15,65,3):new Vec3d(-74,65,0);testSquad.yaw=-90;
            spawnVisualSquad(testSquad,fight?-22:-74,3);
            if(fight){Squad enemy=new Squad(UUID.randomUUID(),1-testSquad.team,new Vec3d(-20,65,3));enemy.order="attack";enemy.yaw=90;spawnVisualSquad(enemy,22,3);}
        }
        void spawnVisualSquad(Squad s,int x,int z){
            int slot=0;for(var spec:Spec.TROOPS)if(spec.faction().equals(Spec.FACTIONS[s.team].id()))for(int n=0;n<spec.count();n++,slot++){
                int px=x+(slot%5-2)*2,pz=z+(slot/5-2)*2;spawn(s,spec,slot,new Vec3d(px,Arena.ground(px,pz)+1,pz));
            }
        }
    }
}



