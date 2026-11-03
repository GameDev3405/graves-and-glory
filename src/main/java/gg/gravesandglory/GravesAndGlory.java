package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
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
    /** The battle state, server to client every 10 ticks: its layout is Battle.sendState's (state_v9 adds the in-match progression fields there), and GravesAndGloryClient reads it. */
    public static final Identifier STATE = new Identifier("gravesandglory", "state_v9");
    public static final Identifier OPEN_BARRACKS = new Identifier("gravesandglory", "barracks_v1");
    public static final Identifier PARTY = new Identifier("gravesandglory", "party_v1");
    /** A troop's stance (Tactics.STANCES), server to client: a varInt entity id and a byte index. */
    public static final Identifier STANCE = new Identifier("gravesandglory", "stance_v1");
    /** S2C battle event feed lines (FeedEvents): kind, the event's team, text. */
    public static final Identifier FEED = new Identifier("gravesandglory", "feed_v1");
    public static Battle battle;
    public static final Spec.RulesRow R = Spec.RULES[0];
    @Override public void onInitialize() {
        Diagnostics.start();
        VersionGate.registerServer();
        Audio.register(); // every sound event of the AudioCues table (plan 4F.4)
        // The commander actives, once. (The horn cues above are command feedback for the commanding player alone: no horn sounds in the field.)
        Commander.register();
        // The Convoy supply wagon's entity type (Kort approved 2026-11-01; never saved).
        WagonEntity.register();
        // A planted banner block must not outlive the battle in the save.
        // A match the server stops is void: no rewards, no losses, armies to camp (MatchRules). Then the banners come down.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {if(battle!=null){battle.campaign.voidMatch(MatchRules.Ending.STOPPED);Abilities.serverStopping(battle);}});
        if (BuildMode.active()) BuildMode.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Only the dedicated Graves and Glory save belongs to this mod.
            if (!BuildMode.active() && java.util.Set.of("Graves and Glory", "Bannerbound").contains(server.getSaveProperties().getLevelName())) battle = new Battle(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> battle=null);
        // Old saves decode our vanilla entity types as ordinary hostile mobs.
        // Catch chunks loaded after SERVER_STARTED, not only spawn-area entities.
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(battle!=null&&!CampWorld.generation.isEmpty()&&entity.getCommandTags().contains(CampWorld.TAG)&&!entity.getCommandTags().contains("camp-generation:"+CampWorld.generation))entity.discard();
            if(entity.getCommandTags().contains("gravesandglory-lobby")||entity.getCommandTags().contains("bannerbound-lobby")||entity.getCommandTags().contains("gravesandglory-camp-v3"))entity.discard();
            if(battle!=null&&world==battle.world&&(entity.getCommandTags().contains("gravesandglory")||entity.getCommandTags().contains("bannerbound"))&&!ArmyMobs.isTroop(entity))entity.discard();
            // A shot's arrow saved mid-flight comes back from disk as a real, clipping arrow. This event also fires for a new spawn, so only a tagged arrow that no queued shot owns is discarded (Projectiles.fire queues the shot before it spawns the arrow). Any world.
            if(entity.getCommandTags().contains(Projectiles.TAG)&&!Projectiles.owns(battle,entity))entity.discard();
        });
        // The backstop for a troop's side-team membership (gg_<side>): it fires when any troop entity leaves the world (a death, a discard, a chunk unload), so a path that removes a troop without Battle.leaveSide (a chunk unload, the checks' direct discards, BotSimulation.field) still leaves no stale member; returnLobby discards its troops and then empties all four teams with clearSides. The call sites that discard a troop (Houses.discard, which Grave Bloom's expiry goes through, among them) still call leaveSide first, as before, and AFTER_DEATH keeps its own call; the two are harmless repeats, since leaveSide does nothing for an entity that is on no gg_ team.
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,world)->{if(ArmyMobs.isTroop(entity))Battle.leaveSide(entity);});
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(battle==null) return;
            battle.tick();FeedEvents.tick(battle);
            // No hunger in Graves and Glory: food stays full (sprinting and slow natural healing still work) and saturation empty (so healing stays slow).
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){var hunger=p.getHungerManager();if(hunger.getFoodLevel()!=20)hunger.setFoodLevel(20);hunger.setSaturationLevel(0);hunger.setExhaustion(0);}
        });
        // A player who starts tracking a troop learns its stance (stance_v1), ahead of the entity's spawn packet (Fabric fires the event first;
        // the client keeps the stance for an id it does not have yet). START_TRACKING fires inside spawnEntity, so a troop Battle.spawn is
        // placing is found through Battle.spawning (it joins units after).
        EntityTrackingEvents.START_TRACKING.register((entity,player)->{
            if(battle==null||!ArmyMobs.isTroop(entity))return;
            Unit u=battle.spawning!=null&&battle.spawning.mob==entity?battle.spawning:null;
            if(u==null)for(Unit x:battle.units)if(x.mob==entity){u=x;break;}
            if(u!=null&&!u.stance.isEmpty())Tactics.trackingSends+=Tactics.sendStance(u,List.of(player));
        });
        // A player whose client registers stance_v1 only after the troops around it began to be tracked (a guest joining a battle in progress: its trackers
        // start when it enters the world, before its client says which channels it takes, so canSend was false for those sends) gets the stance of every troop it tracks.
        S2CPlayChannelEvents.REGISTER.register((handler,sender,server,channels)->{
            if(!channels.contains(STANCE))return;
            ServerPlayerEntity joined=handler.player;
            server.execute(()->{if(battle!=null)Tactics.registerSends+=Tactics.sendTracked(battle,joined);});
        });
        // A soldier's rank (rank_v1: level, weapon tier, armour tier) goes the same two ways as his stance: on START_TRACKING and on a late channel registration.
        RankSync.registerServer();
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
        // Rise Again: a Graveborn troop may stand up instead of dying (Houses.allowDeath); returning false skips vanilla's death and AFTER_DEATH below.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,amount)->Houses.allowDeath(entity,source));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{
            Battle.leaveSide(entity);
            if(battle==null)return;
            Entity attacker=source.getAttacker();
            for(Unit victim:new ArrayList<>(battle.units))if(victim.mob==entity){ // a copy: a level up's ability seams (Abilities.rowsChanged, onKill) may change the list
                battle.fell(victim); // a shot of his that lands after this is still his army's (Battle.fallenSquad: a commander it strikes down is credited to that army)
                Squad winner=attacker==null?null:battle.squads.get(attacker.getUuid());String troop=null; // troop: the troop type the kill goes to in the mastery ledger, none for a commander's or a temporary troop's
                for(Unit u:battle.units)if(u.mob==attacker){winner=u.squad;troop=u.temporary?null:u.spec.id();}
                // A shot that lands after its archer fell still credits the archer's army, and so does a death soon after an enemy's blow; a fall, lava, cramming or /kill long after one is nobody's kill.
                if(winner==null&&BattleMath.creditWindowOpen(battle.time,victim.lastHitAt,Projectiles.CREDIT_WINDOW_TICKS)){winner=victim.lastHitBy;troop=victim.lastHitTroop;}
                Veterans.onDeath(battle,victim,attacker,winner); // kill credit and levels (in-match progression design 3.2): the killing blow's troop, his helpers and their supporters; above the temporary skip, so a Risen killed counts 1. Gold, kills and mastery below are unchanged.
                if(!victim.temporary&&battle.campaign.bonusTracker!=null&&winner!=null&&victim.squad.team!=winner.team)battle.campaign.bonusTracker.killed(victim.spec.id(),winner.team==0&&!winner.bot); // March bonus kill_unit: a troop of the target id fell to the commander's own banner
                if(victim.temporary)continue; // a Grave Bloom Risen is no loss, no kill and no fallen troop to raise
                victim.squad.stats.lost++;victim.squad.fallen.merge(victim.spec.id(),1,Integer::sum);Houses.logDeath(battle,victim);Houses.recompute(victim.squad);
                if(winner!=null&&winner.team!=victim.squad.team){winner.earn(Spec.ECONOMY[0].killGold());winner.stats.kills++;if(troop!=null)winner.stats.troopKills.merge(troop,1,Integer::sum);if(attacker instanceof ServerPlayerEntity&&source.getSource()==attacker)Commander.playerKill(battle,winner);} // a kill by the commander's own hand (not his arrow, not a troop of his, not a fall in his credit window): a Reaver's Bloodrush gives a troop back
                FeedEvents.died(battle,victim,winner);
            }
        });
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            String station=entity.getCommandTags().stream().filter(t->t.startsWith("camp:")).map(t->t.substring(5)).findFirst().orElse("");
            if(station.isEmpty())return ActionResult.PASS;
            if(!world.isClient&&player instanceof ServerPlayerEntity p&&battle!=null&&p.squaredDistanceTo(entity)<36)openCamp(p,station);
            return ActionResult.SUCCESS;
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            String station=CampWorld.station(hit.getBlockPos());if(station==null||battle==null)return ActionResult.PASS;
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
        String order="follow", formation="block", commander="champion";
        boolean bot, seated=true; int lane;
        /** The tick a dropped owner's bot takes command (MatchRules.takeoverAt); 0 when no one is waited for. */
        long takeoverAt;
        /** The banner this squad fields this round (rules.squadSize entries: troop ids, ^ for a slot of the unit before it, empty), set by Campaign.prepareRound. */
        List<String> banner=new ArrayList<>();
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
        /** Per group: which groups stand their last stand now, when each was last told of it (Morale.tick), and the group's full lineup health this round. */
        final Set<String> lastStand=new HashSet<>();final Map<String,Long> lastStandAt=new HashMap<>();
        /** The groups whose Retreat or Move has arrived (Tactics.arrived), latched there until the group's next order (Battle.issue, the command path, Campaign.botDecide) or the round's start. */
        final Set<String> arrived=new HashSet<>();
        final Map<String,Double> fullHealth=new HashMap<>();
        /** Battle time (ticks) each group's order last became Charge (Battle.markCharge): the counter table's charge impact reads it. */
        final Map<String,Long> chargedAt=new HashMap<>();
        /** House id to trait tier (1 to 3) for the Houses this army's living troops reach, from their slot weights; Houses.recompute keeps it. */
        final Map<String,Integer> traits=new LinkedHashMap<>();
        /** Troop id to how many of them this army lost this round and has not had back as a Reaver's Bloodrush refund (Commander.Bloodrush); the round's start clears it. */
        final Map<String,Integer> fallen=new LinkedHashMap<>();
        /** The commander ability (Commander): the battle time its cooldown ends, the time its run ends (0: not running), and what the running active keeps. */
        long abilityReadyAt,abilityUntil;Object abilityData;
        /** The most troops of each group alive at once this round (the HUD card's "of" count), raised with fullHealth and cleared with it. */
        final Map<String,Integer> fullCount=new HashMap<>();
        /** The commander's last enemy blow (a player commander is no Unit): the army whose troop or commander landed it and when (battle ticks). Battle.allowDamage sets it; a death with no attacker within Projectiles.CREDIT_WINDOW_TICKS of it (a pull into lava, a knock off an edge) is that army's (Veterans.onCommanderDeath, which clears it). */
        Squad hitBy;long hitAt;
        Squad(UUID owner,int team,Vec3d pos) {this.owner=owner;this.team=team;target=pos;}
    }
    public static final class Assignment {
        String order,formation;Vec3d target;float yaw;int enemy=-1;long issued;
        /** A raised post (Siege's gate-tower tops): the target's own height counts, the slots pack tight around it (Battle.formation) and a ranged troop there shoots from it rather than walk off after an enemy (Tactics). */
        boolean perch;
        Assignment(String o,String f,Vec3d t,float y){order=o;formation=f;target=t;yaw=y;}
    }
    /** A troop in a squad's training queue: when it is done, and the gold paid for it (the class cost, Commander.cost), which a cancel or a full army refunds. The short form is a free troop: nothing paid, nothing refunded. */
    public record Training(Spec.TroopsRow spec,long finished,int paid) {public Training(Spec.TroopsRow spec,long finished){this(spec,finished,0);}}
    public static final class Unit {
        MobEntity mob;
        Squad squad;
        Spec.TroopsRow spec;
        int slot;
        long nextAttack;long abilityAt,windupUntil;Vec3d spellCenter;
        long nextPath;
        Vec3d lastPosition,lastGoal,deployed;
        int stuckTicks;
        /** The last sidestep of Tactics.walk (battle tick, where, and how many times in a row at that wedge before it), for Wedge; and the detour a unit stuck again is on (its point, the goal it was made for, and the tick it is dropped at); detourRetryAt: no new search before this tick (the last found nothing). */
        long wedgeTime,detourUntil,detourRetryAt;Vec3d wedgeAt,detour,detourFor;int wedgeRepeats;
        LivingEntity lockedTarget;long lockUntil;
        /** The army whose blow last hurt this unit; kill credit when the attacker itself is gone. */
        Squad lastHitBy;
        /** Battle time (ticks) of that blow: a death with no attacker credits lastHitBy only within Projectiles.CREDIT_WINDOW_TICKS of it. */
        long lastHitAt;
        /** The troop id whose blow that was, for the mastery ledger (SquadStats.troopKills); null for a commander's blow or a temporary troop's. */
        String lastHitTroop;
        /** The tags this troop brings to the counter table (ArmyMobs.tags), a copy a House task may add to. */
        Set<String> tags;
        /** The soldier's level for the match (1 to 3), his kill credits, and the weapon and armour tier he wears (1 to 3). Level 1 and tier I when trained or rebought; nothing carries to the next match (in-match progression design 3 and 4). */
        int level=1, credits=0, weaponTier=1, armourTier=1;
        /** A troop raised for the round only (Grave Bloom Risen): it never counts toward the slot check or earns mastery, and expiresAt (battle ticks) ends it. */
        boolean temporary;long expiresAt;
        /** Battle time until which the troop is stunned (a slam, a bear's roar). */
        long stunnedUntil;
        /** True once a Graveborn troop has risen again; it rises once. */
        boolean rose;
        /** Blocks walked since this troop's last attack, for Pounce (Abilities.unitTick adds the ground covered each tick; onHit sets it back to 0). */
        double travelSinceAttack;
        /** Where the troop stood on its last tick, for travelSinceAttack. Null before its first tick. */
        Vec3d travelAt;
        /** The Howl on this troop: the battle time it ends (0: none) and its damage and speed shares (Abilities.howl sets them, auraFactor and unitTick read them). */
        long howlUntil;double howlAmount,howlSpeed;
        /** Battle time of this troop's last charge-impact hit: the impact is the first hit of a fresh Charge (the combatDiscipline chargeImpactTicks window). */
        long chargeHitAt=-1;
        /** The Charge stamp (Squad.chargedAt of its group) Old Horn last trampled under: a trample comes once per fresh Charge (Abilities.trample). */
        long trampledCharge=-1;
        /** The group order an aura troop found on its last beat (Abilities.aura): the Oath Banner plants when it turns to Hold. Null before its first beat. */
        String seenOrder;
        /** Where the troop stood when it last moved more than the Entrench radius, and the battle time (ticks) it has stood there since (Houses.sample, on each Tactics beat). Null before its first beat. */
        Vec3d stillAt;long stillSince;
        /** The stance its clients show (Tactics.STANCES, from its order on its beat; stance_v1 carries it); "" for none. */
        String stance="";
        Unit(MobEntity m,Squad s,Spec.TroopsRow t,int slot){mob=m;squad=s;spec=t;this.slot=slot;lastPosition=m.getPos();deployed=m.getPos();tags=new HashSet<>(ArmyMobs.tags(t));}
    }
    public static final class Capture {
        Spec.PointsRow spec;
        int owner=-1, challenger=-1, progress=0;boolean contested;
        /** Battle time (ticks) when the current owner took the point. */
        long ownedSince;
        Capture(Spec.PointsRow spec){this.spec=spec;}
    }
    /**
     * A place new troops may deploy instead of the barracks (Battle.reinforcementAnchors): a held point (Anchor.of) or a standing Forward Supply
     * banner (ArmySupplyBanner.anchor). team is the army side that may deploy there: the point's owner, the banner's planter.
     */
    public record Anchor(double x,double z,String name,int team){
        public static Anchor of(Capture c){return new Anchor(c.spec.x(),c.spec.z(),c.spec.name(),c.owner);}
    }
    /**
     * The Quartermaster's Forward Supply banner, the first structure: an invisible armour stand at a standing banner block in its side's dye. It is
     * the target the counters rows C10 and C11 name (it carries the command tag "structure", which Combat reads) and it holds its health in hp. A
     * blow's resolved amount comes off it (damage); at 0 it is discarded, and its block goes back with it however it is removed. Only the other side
     * hurts it. While it stands it is one of Battle.structures and, with no enemy within the class's radius of it, an Anchor that reinforcements
     * deploy at. It is never saved.
     */
    public static class ArmySupplyBanner extends ArmorStandEntity implements ArmyMob { // not final: Siege's gate target (SiegeMode.GateTarget) is one
        /** What its name tag says and the notice of a deploy at it names. */
        public static final String NAME="Forward Supply";
        /** What its commander reads when the banner is shot down or killed while his Forward Supply runs. */
        public static final String FALLEN="Your supply banner has fallen.";
        Squad squad;float hp;net.minecraft.util.math.BlockPos block;net.minecraft.block.BlockState was;boolean felled;
        public ArmySupplyBanner(World world){super(EntityType.ARMOR_STAND,world);}
        /** The army that planted it, the side it serves, the health it has left. */
        public Squad squad(){return squad;}
        public int team(){return squad==null?-1:squad.team;}
        public float hp(){return hp;}
        public Anchor anchor(){return new Anchor(getX(),getZ(),NAME,team());}
        Text label(){return Text.literal(NAME+" "+(int)Math.ceil(hp)).formatted(team()==0?Formatting.AQUA:Formatting.LIGHT_PURPLE);}
        @Override public boolean shouldSave(){return false;}
        /** A player's use on it dresses nothing. */
        @Override public ActionResult interactAt(PlayerEntity player,Vec3d hitPos,Hand hand){return ActionResult.FAIL;}
        /**
         * Takes the blow's amount off hp. Only a troop or a seated commander of the other side hurts it (fire, falling and the void do not); a
         * commander's own blow counts as a non-siege blow on a structure (Combat.structureBlow, the sheet's C11). Returns whether it took the blow.
         */
        /** The sound of a blow on the structure (felled: its last): the wool of a banner; the Siege gate's target overrides it (SiegeMode.GateTarget). */
        void structureCue(ServerWorld w,boolean felled){
            if(felled)w.playSound(null,getBlockPos(),SoundEvents.BLOCK_WOOL_BREAK,SoundCategory.HOSTILE,1f,.8f);
            else w.playSound(null,getBlockPos(),SoundEvents.BLOCK_WOOL_HIT,SoundCategory.HOSTILE,.8f,1f);
        }
        @Override public boolean damage(DamageSource source,float amount){
            var b=battle;
            if(b==null||getWorld().isClient||isRemoved()||squad==null||amount<=0||!b.running)return false;
            Entity hitter=source.getAttacker();
            int side=hitter==null?-1:b.teamOf(hitter);
            if(side<0||side==squad.team)return false;
            if(hitter instanceof ServerPlayerEntity)amount*=Combat.structureBlow();
            hp-=amount;
            var w=(ServerWorld)getWorld();
            if(hp<=0){
                felled=true;
                w.spawnParticles(ParticleTypes.POOF,getX(),getY()+1,getZ(),12,.3,.6,.3,.04);
                structureCue(w,true);
                discard();
            }else{
                w.spawnParticles(ParticleTypes.CRIT,getX(),getY()+1,getZ(),5,.25,.5,.25,.08);
                structureCue(w,false);
                setCustomName(label());
            }
            return true;
        }
        /**
         * Off the battle's list and, when it is discarded or killed (not merely unloaded), the banner block back to what stood there. When it was
         * shot down or killed while it is the banner of its commander's running Forward Supply, he is told and the active ends (so the HUD stops
         * reading it as up; the cooldown stands). Expiry, the lobby, a round start and a map switch discard it quietly.
         */
        @Override public void remove(RemovalReason reason){
            if(!isRemoved()){
                if((reason==RemovalReason.DISCARDED||reason==RemovalReason.KILLED)&&block!=null&&getWorld().getBlockState(block).getBlock() instanceof net.minecraft.block.BannerBlock)
                    getWorld().setBlockState(block,was==null?net.minecraft.block.Blocks.AIR.getDefaultState():was);
                if(battle!=null){
                    battle.structures.remove(this);
                    if((felled||reason==RemovalReason.KILLED)&&squad!=null&&squad.abilityData==this){battle.campaign.notice(squad.owner,FALLEN);Commander.stop(battle,squad);}
                }
            }
            super.remove(reason);
        }
        /** Plants a banner of hp health for the army at the spot Abilities.spot finds by the feet (null when none is free): a banner block in its side's dye, with the stand on it. */
        static ArmySupplyBanner place(Battle b,Squad s,Vec3d feet,float yaw,float hp){
            net.minecraft.util.math.BlockPos spot=Abilities.spot(b,feet,yaw);
            return spot==null?null:place(b,s,spot,yaw,hp);
        }
        /** The same on a spot already found (Abilities.spot): the banner block and the stand on it. */
        static ArmySupplyBanner place(Battle b,Squad s,net.minecraft.util.math.BlockPos spot,float yaw,float hp){
            var e=new ArmySupplyBanner(b.world);
            e.squad=s;e.hp=hp;e.block=spot;e.was=b.world.getBlockState(spot);
            b.world.setBlockState(spot,net.minecraft.block.BannerBlock.getForColor(Abilities.dye(b.side(s.team).color())).getDefaultState().with(net.minecraft.block.BannerBlock.ROTATION,RotationPropertyHelper.fromYaw(yaw)));
            e.refreshPositionAndAngles(spot.getX()+.5,spot.getY(),spot.getZ()+.5,yaw,0);
            e.setNoGravity(true);e.setInvisible(true);
            e.addCommandTag("gravesandglory");e.addCommandTag("structure");
            e.setCustomName(e.label());e.setCustomNameVisible(true);
            b.world.spawnEntity(e);b.structures.add(e);
            return e;
        }
    }
    public static final class ArmyHusk extends HuskEntity implements ArmyMob {
        public ArmyHusk(World world){super(EntityType.HUSK,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        /** Never burns in daylight and never turns into a Zombie in water: a troop stays the troop its sheet row says. */
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override protected boolean canConvertInWater(){return false;}
    }
    public static final class ArmySkeleton extends SkeletonEntity implements ArmyMob {
        public ArmySkeleton(World world){super(EntityType.SKELETON,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override public void updateAttackType() {}
    }
    public static final class ArmyWither extends WitherSkeletonEntity implements ArmyMob {
        public ArmyWither(World world){super(EntityType.WITHER_SKELETON,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override public void updateAttackType() {}
    }
    /** The Ironclad Hearthguard and Shieldbreaker: an illager with only a SwimGoal, so it neither raids nor celebrates and Tactics alone moves and swings it. */
    public static final class ArmyVindicator extends VindicatorEntity implements ArmyMob {
        public ArmyVindicator(World world){super(EntityType.VINDICATOR,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    /** The Ironclad Arbalist, Ballista Crew and Mortar: a pillager with only a SwimGoal, so no crossbow goal ever puts it in the charging pose. */
    public static final class ArmyPillager extends PillagerEntity implements ArmyMob {
        public ArmyPillager(World world){super(EntityType.PILLAGER,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    /** Brassjaw, the Forge Colossus: an iron golem with only a SwimGoal (no village defence, no anger at players). Its swing is the status 4 the attack sends (Abilities.attack). */
    public static final class ArmyGolem extends IronGolemEntity implements ArmyMob {
        public ArmyGolem(World world){super(EntityType.IRON_GOLEM,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    /** The Tidecallers Drowned Raider, Harpooner, Trident Guard and Admiral: a drowned with only a SwimGoal, so it never throws a trident on its own, never walks to the water or the shore, and Tactics alone moves and swings it. */
    public static final class ArmyDrowned extends DrownedEntity implements ArmyMob {
        public ArmyDrowned(World world){super(EntityType.DROWNED,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        /** Never burns in daylight and never turns into anything in water: a troop stays the troop its sheet row says. */
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override protected boolean canConvertInWater(){return false;}
    }
    /** The Tidecallers Mist Caller: an evoker with only a SwimGoal, so it never summons fangs or vexes and never calls a sheep blue; its cloud is the mist ability (Abilities.cloud, begun in Abilities.attack). */
    public static final class ArmyEvoker extends EvokerEntity implements ArmyMob {
        public ArmyEvoker(World world){super(EntityType.EVOKER,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    /** The Tidecallers Kraken-touched Brute: a ravager with only a SwimGoal, so it never roars, stuns itself or charges on its own; its tentacle sweep is the slam ability. Its own tick pulls its speed to 0.3 or 0.35, so the sheet's speed is put back after it. */
    public static final class ArmyRavager extends RavagerEntity implements ArmyMob {
        /**
         * A vanilla Ravager paths through leaves (its constructor sets their penalty to 0) because it smashes them as it walks, which needs mobGriefing; a
         * match has that rule off, so a Brute that walked into a canopy stood against it for the rest of the round. Leaves keep the default penalty (blocked).
         */
        public ArmyRavager(World world){super(EntityType.RAVAGER,world);setPathfindingPenalty(PathNodeType.LEAVES,PathNodeType.LEAVES.getDefaultPenalty());}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override public void tickMovement(){
            var speed=getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);double sheet=speed.getBaseValue();
            super.tickMovement();
            speed.setBaseValue(sheet);
        }
    }
    /** The Wildmoot Hide Hunter, Tusk Spear and Seer: a zombified piglin with only a SwimGoal that never holds anger (vanilla's mobTick, which is its anger pass, its grunts and its speed boost, is skipped whole: Tactics alone moves and swings it), never burns in daylight and never turns into a zombie in water. */
    public static final class ArmyBoarkin extends ZombifiedPiglinEntity implements ArmyMob {
        public ArmyBoarkin(World world){super(EntityType.ZOMBIFIED_PIGLIN,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override protected boolean canConvertInWater(){return false;}
        @Override protected void mobTick(){}
        @Override public void setAngerTime(int ticks){super.setAngerTime(0);}
        @Override public void setAngryAt(UUID uuid){super.setAngryAt(null);}
        @Override public void chooseRandomAngerTime(){}
    }
    /** The Wildmoot Moot Hound: a wolf with only a SwimGoal that never sits (a seat is cleared every tick), never holds anger at anyone (Tactics alone picks its target, and the anger vanilla's tick would set from it is dropped), takes a troop's blow in full (vanilla's wolf halves a blow from anything but a player or an arrow) and cannot be tamed or fed (a click is passed). */
    public static final class ArmyWolf extends WolfEntity implements ArmyMob {
        public ArmyWolf(World world){super(EntityType.WOLF,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override public void tickMovement(){super.tickMovement();if(isSitting())setSitting(false);if(isInSittingPose())setInSittingPose(false);}
        /** Vanilla's wolf takes (amount + 1) / 2 of a blow whose attacker is neither a player nor an arrow; a troop's blow goes in as 2a - 1, so the half is a again. */
        @Override public boolean damage(DamageSource source,float amount){Entity e=source.getAttacker();return super.damage(source,e!=null&&!(e instanceof PlayerEntity)&&!(e instanceof PersistentProjectileEntity)?amount*2-1:amount);}
        /** No taming, no seat on a click, no bone taken: a troop is not a pet. */
        @Override public ActionResult interactMob(PlayerEntity player,Hand hand){return ActionResult.PASS;}
        @Override public void setAngerTime(int ticks){super.setAngerTime(0);}
        @Override public void setAngryAt(UUID uuid){super.setAngryAt(null);}
        @Override public void chooseRandomAngerTime(){}
    }
    /** The Wildmoot Web Stalker: a spider with only a SwimGoal. Its climbing navigation (SpiderNavigation, and the wall climb its tick sets from a horizontal collision) is vanilla's, so it goes over a wall Tactics sends it against. */
    public static final class ArmySpider extends SpiderEntity implements ArmyMob {
        public ArmySpider(World world){super(EntityType.SPIDER,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
    }
    /** The Wildmoot Old Horn: a polar bear with only a SwimGoal that never holds anger. Its warning pose (vanilla's rear) is set by nothing of its own: the trample (Abilities.trample) pulses it through rear for WARNING_TICKS as the bear's attack animation. */
    public static final class ArmyBear extends PolarBearEntity implements ArmyMob {
        /** How long the rear shows (presentation only): the client takes 6 ticks to raise the bear and 6 to drop it. */
        static final int WARNING_TICKS=20;
        int warningUntil;
        public ArmyBear(World world){super(EntityType.POLAR_BEAR,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override public void setAngerTime(int ticks){super.setAngerTime(0);}
        @Override public void setAngryAt(UUID uuid){super.setAngryAt(null);}
        @Override public void chooseRandomAngerTime(){}
        /** The trample's rear: the warning pose for WARNING_TICKS from now. */
        public void rear(){setWarning(true);warningUntil=age+WARNING_TICKS;}
        @Override public void tick(){super.tick();if(isWarning()&&age>=warningUntil)setWarning(false);}
    }
    /** A troop that fell: where, when (battle ticks), which troop (its sheet id) and for which team. Grave Bloom raises from Battle.deaths. */
    public record Death(Vec3d at,long time,String troop,int team) {}
    public static final class ArmyZombie extends ZombieEntity implements ArmyMob {
        public ArmyZombie(World world){super(EntityType.ZOMBIE,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        /** Never burns in daylight and never turns into a Drowned in water: a troop stays the troop its sheet row says. */
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override protected boolean canConvertInWater(){return false;}
    }
    public static final class ArmyStray extends StrayEntity implements ArmyMob {
        public ArmyStray(World world){super(EntityType.STRAY,world);}
        @Override public boolean shouldSave(){return false;}
        @Override protected void initGoals() {goalSelector.add(0,new SwimGoal(this));}
        @Override protected boolean isAffectedByDaylight(){return false;}
        @Override public void updateAttackType() {}
    }
    public static final class Battle {
        public final Campaign campaign;
        public final MinecraftServer server;
        public final ServerWorld world;
        public final LinkedHashMap<UUID,Squad> squads=new LinkedHashMap<>();
        public final List<Unit> units=new ArrayList<>();
        /** Per army, how many of its living troops of a Pack Hunters House (Houses.packHouse) have each enemy (entity id) as their target: Houses.countTargeters rebuilds it every 10 ticks before the units tick, and Pack Hunters (Houses.packHunters) reads it. */
        final Map<Squad,Map<Integer,Integer>> targeters=new HashMap<>();
        /** How many of an army's living Pack Hunters troops had this target at the last count. */
        int targeters(Squad s,LivingEntity target){var m=targeters.get(s);return m==null?0:m.getOrDefault(target.getId(),0);}
        /** The unit spawn is placing, while spawnEntity runs (the stance_v1 and rank_v1 tracking sends find it here); null otherwise. */
        Unit spawning;
        /** The kill-credit logs of in-match progression (design 3.2): who hit, slowed, pulled or zoned each victim and who healed or buffed each troop, within progression.assistSeconds (Veterans.helped and supported write it); and when Veterans last trimmed it. clearProgression empties it. */
        final CreditLog credit=new CreditLog();long creditTrimmedAt;
        /** Each army's Armoury this round (in-match progression design 4.2): per banner troop type, the highest level a soldier of it reached and the tier bought on each track (Veterans.Kit). Veterans owns it; clearProgression empties it. */
        final Map<Squad,Map<String,Veterans.Kit>> armouries=new IdentityHashMap<>();
        /** A troop that died, by entity id: his army and when (battle ticks). A shot of his that lands within Projectiles.CREDIT_WINDOW_TICKS is still his army's blow (fallenSquad); AFTER_DEATH records it (fell). */
        record Fallen(Squad squad,long time){}
        final Map<Integer,Fallen> fallen=new HashMap<>();
        /** Records a troop's death for fallenSquad and drops the records older than the window. */
        void fell(Unit u){fallen.values().removeIf(f->!BattleMath.creditWindowOpen(time,f.time(),Projectiles.CREDIT_WINDOW_TICKS));fallen.put(u.mob.getId(),new Fallen(u.squad,time));}
        /** The army of a troop that fell within Projectiles.CREDIT_WINDOW_TICKS, for a blow his shot lands after it; null for anyone else. */
        Squad fallenSquad(Entity e){var f=e==null?null:fallen.get(e.getId());return f!=null&&BattleMath.creditWindowOpen(time,f.time(),Projectiles.CREDIT_WINDOW_TICKS)?f.squad():null;}
        /** A round boundary for in-match progression (startRound, Campaign.returnLobby and switchMap): the credit logs, every Armoury, every status, every client's ranks and the fallen troops' records go. */
        void clearProgression(){Veterans.clear(this);Statuses.clear(this);RankSync.clear(this);fallen.clear();}
        public final List<Capture> points=new ArrayList<>();
        /** The pieces this round's mode saved before changing them (Phase 4 Task 5); teardown and the crash marker put them back. */
        public final List<Structures.Saved> saves=new ArrayList<>();
        public final List<Projectiles.Shot> shots=new ArrayList<>();
        /** The standing banners (the Oath Banner); Abilities.bannerTick takes them down on time, and startRound, the lobby and a map switch clear them. */
        public final List<Abilities.Banner> banners=new ArrayList<>();
        /** The standing structures (the Quartermaster's Forward Supply banners): troops of the other side target them, and Abilities.clearBanners takes them down with the Oath Banners. ArmySupplyBanner.remove drops a banner from it however it goes. */
        public final List<ArmySupplyBanner> structures=new ArrayList<>();
        /** The Mist Callers' standing clouds (Abilities.Mist): counters row C17 reads them through Abilities.inMist, Abilities.mistTick slows the enemies in them and drops the expired ones, and Abilities.clearBanners empties the list (startRound, the lobby, a map switch). */
        public final List<Abilities.Mist> mists=new ArrayList<>();
        /** The troops that really died in the last Houses.deathWindow ticks (a Rise Again and a temporary troop leave no entry), in the order they fell. Grave Bloom raises from it; startRound, the lobby and a map switch empty it. */
        public final List<Death> deaths=new ArrayList<>();
        /** The living troops of an army that fill its slots: a temporary troop (Grave Bloom's) is not one of them. */
        public java.util.stream.Stream<Unit> fielded(Squad s){return units.stream().filter(u->u.squad==s&&!u.temporary&&u.mob.isAlive());}
        final Map<UUID,Long> respawns=new HashMap<>();
        final int[] scores={0,0};
        /** The side row each team flies this round, as indexes into Spec.FACTIONS (spec 1.1); Campaign.chooseSides sets it as the round is prepared. Positions still come from rows 0 and 1 by team. */
        public int[] sides={0,1};
        /** The side row a team flies: its colour, name and glyph. */
        public Spec.FactionsRow side(int team){int row=team==0||team==1?sides[team]:-1;return Spec.FACTIONS[row>=0&&row<Spec.FACTIONS.length?row:Math.max(0,Math.min(1,team))];}
        /** The scoreboard team of a side: gg_<factions id>. Its members are troops' UUIDs; the client reads it to pick the troop's team cloth (TroopModels.resolve). */
        static String sideTeam(Spec.FactionsRow side){return "gg_"+side.id();}
        /** Puts a troop on the scoreboard team of the side its team flies (gg_<side id>), creating the team on first use. */
        public void joinSide(MobEntity mob,int team){
            var board=server.getScoreboard();String name=sideTeam(side(team));
            net.minecraft.scoreboard.Team t=board.getTeam(name);if(t==null)t=board.addTeam(name);
            board.addPlayerToTeam(mob.getEntityName(),t);
        }
        /** Takes an entity off its gg_ side team: a no-op for one that is on none. The ENTITY_UNLOAD hook calls it for every troop that leaves the world and is the backstop; AFTER_DEATH and the call sites that discard a troop call it too, harmlessly repeating it, so the membership is gone however a troop was removed. */
        public static void leaveSide(Entity e){
            var board=e.getWorld().getScoreboard();var t=board.getPlayerTeam(e.getEntityName());
            if(t!=null&&t.getName().startsWith("gg_"))board.removePlayerFromTeam(e.getEntityName(),t);
        }
        /** Empties the four side teams (startRound, Campaign.returnLobby and switchMap). */
        public void clearSides(){
            var board=server.getScoreboard();
            for(var f:Spec.FACTIONS){var t=board.getTeam(sideTeam(f));if(t!=null)for(String member:new ArrayList<>(t.getPlayerList()))board.removePlayerFromTeam(member,t);}
        }
        /**
         * The field of a portrait's shield (ArmyCards.portrait): grey, a dye no side flies and no troops.shield layer uses
         * (tools/preflight.py reads it here and refuses a clash), so every layer shows on a card.
         */
        public static final String PORTRAIT_FIELD="gray";
        /**
         * The troop's painted shield (unit identity rule 7): unbreakable, its BlockEntityTag Base the given dye (the side's
         * factions.dye in battle, PORTRAIT_FIELD on portraits) and its Patterns the troops.shield layers, <dye>:<pattern> each.
         */
        public static ItemStack shield(Spec.TroopsRow t,String dye){
            ItemStack s=unbreakable(Items.SHIELD);NbtCompound tag=new NbtCompound();tag.putInt("Base",DyeColor.byName(dye,DyeColor.WHITE).getId());
            net.minecraft.nbt.NbtList patterns=new net.minecraft.nbt.NbtList();
            if(!t.shield().equals("none"))for(String layer:t.shield().split(",")){String[] p=layer.trim().split(":");NbtCompound one=new NbtCompound();one.putString("Pattern",p[1]);one.putInt("Color",DyeColor.byName(p[0],DyeColor.WHITE).getId());patterns.add(one);}
            tag.put("Patterns",patterns);BlockItem.setBlockEntityNbt(s,net.minecraft.block.entity.BlockEntityType.BANNER,tag);return s;
        }
        /** Seconds each team held each point this round, [team][point]; startRound sizes it from points (five on Kingsfield). */
        int[][] pointSeconds=new int[2][0];
        final Set<UUID> welcomed=new HashSet<>();
        long time;
        public boolean ready=false;
        public boolean running=false;
        /** New troops deploy from a safely held point nearest the fight instead of the barracks; a check can switch it off. */
        public boolean forwardReinforcement=Spec.ECONOMY[0].forwardReinforcement();
        /** Round length in seconds, from the round's mode row; the bot simulation shortens it. */
        public int roundSeconds=Modes.row(Modes.DOMINATION).roundSeconds();
        /**
         * The mode this round plays (Phase 4 Task 2): its clock, scoring, income, bases and timeout. A fresh object each
         * round, so nothing a mode places outlives the round that placed it; Domination outside a round, for the lobby.
         */
        public BattleMode mode=Modes.create(Modes.DOMINATION);
        String message="Preparing the battlefield...";
        /** The notice a round opens with, short enough to show whole on one line of the feed (140 px). */
        public static final String ROUND_START="Capture the objectives.";
        int buildColumn=0;
        /** True once the columns are built and the map's landmarks are being placed (Structures.step, one a tick); the map is ready when the queue is empty. */
        boolean placing=false;
        int rounds=0;long roundStarted;
        record Ping(UUID owner,int team,String kind,double x,double z,long until){}
        final List<Ping> pings=new ArrayList<>();final Map<UUID,Long> lastPing=new HashMap<>();
        void ping(ServerPlayerEntity p,String kind,double x,double z){
            Squad s=squads.get(p.getUuid());var spec=Arrays.stream(Spec.PINGS).filter(v->v.id().equals(kind)).findFirst().orElse(null);
            if(s==null||!s.seated||!running||spec==null||!inArena(x,z)||time-lastPing.getOrDefault(s.owner,-100L)<spec.cooldownSeconds()*20)return;
            lastPing.put(s.owner,time);pings.removeIf(v->v.owner.equals(s.owner)||v.until<time);pings.add(new Ping(s.owner,s.team,kind,x,z,time+spec.seconds()*20));
        }
        /** The commander ability the HUD slot shows: its id and the ticks left and in all of its cooldown. */
        public record AbilityState(String id,int remaining,int total){}
        /** Null in play; only the harness sets it (Phase 2 sends the real ability). */
        public volatile AbilityState testAbility;
        public volatile int testStage=0;
        long testAt=0;
        Vec3d testStart;
        Squad testSquad;
        Unit testVictim;
        float testVictimHealth;
        final Set<String> testedFormations=new HashSet<>();
        final Set<String> testedOrders=new HashSet<>();
        final Set<UUID> testOwners=new HashSet<>();
        int trainingGold,trainingSwapBefore;
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
            world.getGameRules().get(GameRules.DO_VINES_SPREAD).set(false,server); // the camp's stonework holds 53 vines; a random tick would grow them into the cells the pieces leave void
            world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true,server);
            world.getGameRules().get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false,server); // vanilla advancement lines do not belong in battle chat
            world.setTimeOfDay(3000);
            world.setWeather(0,0,false,false);
            world.setSpawnPos(new BlockPos(-108,66,0),-90);
            server.setDifficulty(Difficulty.NORMAL,true);
            clearOldUnits();
            // Only inhabited chunks tick. Unit navigation tickets are maintained below.
            CampaignMaps.lobby(world);if(campaign.cached()){System.out.println("[Graves and Glory] Cached map ready.");mapReady();}
        }
        /**
         * The arena is ready, built or cached: read back what an interrupted round changed, then open the camp. Called after
         * the constructor's time of day and CampaignMaps.lobby, and after the build path's Arena.finish, so a recovered time
         * is the one that stands. The camp's war table is laid down by the build path before this.
         */
        void mapReady(){
            Modes.recover(this);campaign.payInterrupted();
            ready=true;placing=false;message="Your war council is ready. Press {key:menu}.";
        }
        public void tick(){
            time++;
            if(ready&&testStage<8&&Diagnostics.selfTest())selfTest();
            if(!ready){
                // The flat world starts far below this arena: keep arriving players safe
                // while their floor is constructed, before admitting them to the round.
                for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
                    p.changeGameMode(GameMode.ADVENTURE);
                    if(campaign.leash&&!CampWorld.inCamp(p.getX(),p.getY(),p.getZ()))campaign.toLobby(p);
                }
                if(time%20==0)clearOldUnits();
                if(EngineMaps.active()){ // map engine Plan 2: the slot is placed from the engine build, then checked against its design
                    if(EngineMaps.step(world)){campaign.built();CampWorld.warTable(world);mapReady();Diagnostics.report("arena-ready",true,null);}
                    else if(time%20==0){message="Preparing "+Spec.MAPS[campaign.map].name()+" / "+EngineMaps.percent()+"%";sendState();}
                    return;
                }
                int total=(2*R.worldHalfX()+1)*(2*R.worldHalfZ()+1);
                for(int n=0;n<R.buildColumnsPerTick()&&buildColumn<total;n++,buildColumn++){
                    int x=buildColumn%(2*R.worldHalfX()+1)-R.worldHalfX();
                    int z=buildColumn/(2*R.worldHalfX()+1)-R.worldHalfZ();
                    Arena.column(world,x,z);
                }
                if(buildColumn>=total){
                    // The columns are built: the landmarks of the map go down one a tick on the finished ground, and the map is ready once the last is placed.
                    // Then a baked overlay, when the map has one, goes over the generated blocks (MapAdoption).
                    if(!placing){Arena.finish(world);Structures.queueLandmarks(world);MapAdoption.reset(campaign.map);placing=true;}
                    else if(Structures.step(world)&&MapAdoption.step(world)){
                        campaign.built();CampWorld.warTable(world);mapReady();
                        Diagnostics.report("arena-ready",true,null);
                    }
                }
                if(time%20==0){message="Preparing "+Spec.MAPS[campaign.map].name()+" / "+buildColumn*100/total+"%";sendState();}
                return;
            }
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()){
                if(!welcomed.contains(p.getUuid()))join(p);
                if(!squads.containsKey(p.getUuid())||!squads.get(p.getUuid()).seated)continue;
                Long until=respawns.get(p.getUuid());
                if(until!=null){
                    if(time>=until){respawns.remove(p.getUuid());p.changeGameMode(GameMode.ADVENTURE);equipPlayer(p);FeedEvents.commander(this,p,true);}
                } else if(running&&(Math.abs(p.getX())>R.arenaHalfX()-2||Math.abs(p.getZ())>R.arenaHalfZ()-2||p.getY()<56))toBase(p,squads.get(p.getUuid()).team);
            }
            if(running&&campaign.bonusTracker!=null){int fell=0;for(Unit u:units)if(!u.mob.isAlive()&&!u.temporary&&u.squad.team==0&&!u.squad.bot)fell++;if(fell>0)campaign.bonusTracker.lost(fell);} // March bonus: troops of the commander's banner that fell this tick
            units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
            if(running)trainingTick();
            campaign.tick();
            updateUnitChunks();
            if(running&&time>=campaign.introUntil){
                Projectiles.tick(this);Commander.tick(this);Abilities.bannerTick(this);Houses.expire(this);Statuses.tick(this);
                if(time%20==0){Morale.tick(this);Houses.tick(this);}
                if(time%10==0)Houses.countTargeters(this);
                for(Unit u:new ArrayList<>(units))unitTick(u);
                if(time%20==0)captureTick();
                if(time%20==0&&campaign.bonusTracker!=null&&mode.capturesPoints(this)){int own=0;for(Capture c:points)if(c.owner==0)own++;campaign.bonusTracker.held(own,points.size());} // March bonus hold_all: the commander's side (Dawn) holds every point at once
                mode.tick(this);
                if(R.automaticReinforcements()&&time%20==0)for(Squad s:new ArrayList<>(squads.values())){if(!s.seated)continue;
                    if(time>=s.reinforceAt && fielded(s).mapToInt(u->u.spec.slotWeight()).sum()<R.squadSize()){
                        s.reinforceAt=time+R.reinforceSeconds()*20L;reinforce(s);
                    }
                }
                if(time%(Spec.ECONOMY[0].incomeSeconds()*20)==0)for(Squad s:squads.values()){if(!s.seated)continue;
                    s.earn(income(s));
                }
            }
            Audio.tick(this); // cues that wait for their moment (a blow's contact frame, the gate's crash, the wave's rise)
            if(running)Audio.clock(this,roundSeconds*20L-(time-roundStarted)); // the last ten seconds tick
            if(running&&time-roundStarted>=roundSeconds*20L&&!mode.overtime(this))campaign.finish(mode.timeoutWinner(this));
            // Explicit revision checks are driven by the client test timeline.
            if(time%10==0)sendState();
        }
        void join(ServerPlayerEntity p){
            campaign.releaseExpiredHolds();
            // The team the lobby roster lists him on (a round started from a War Table room): a rejoiner past his hold is seated there, not on the emptier side.
            int rosterTeam=-1;
            if(running&&!campaign.activeRoom.isEmpty()){var r0=campaign.social.rooms.get(campaign.activeRoom);if(r0!=null&&r0.members.contains(p.getUuid()))rosterTeam=r0.teams.getOrDefault(p.getUuid(),-1);}
            var decision=AdmissionPolicy.decide(running,campaign.practice,campaign.size,p.getUuid(),
                squads.values().stream().map(s->new AdmissionPolicy.Seat(s.owner,s.team,s.bot,s.seated)).toList(),campaign.vacated,rosterTeam);
            if(running&&!campaign.activeRoom.isEmpty()){
                var room=campaign.social.rooms.get(campaign.activeRoom);
                if(room==null||!room.members.contains(p.getUuid()))decision=new AdmissionPolicy.Decision(decision.team(),null,false);
            }
            int team=decision.team();
            Squad replacement=decision.replacement()==null?null:squads.get(decision.replacement());
            Squad s;
            if(replacement!=null){
                s=replacement;campaign.botRespawns.remove(s.owner);campaign.botRetreating.remove(s.owner);
                campaign.vacated.values().remove(s.owner);campaign.vacatedUntil.remove(p.getUuid());rekey(s.owner,p.getUuid());s.owner=p.getUuid();s.bot=false;s.seated=true;s.takeoverAt=0;
                // The seat keeps the bot's living army and its queue, but from now on it trains and reinforces from the joiner's own banner, and fights under his class.
                // The active's cooldown belongs to the squad, not to whoever commands it: it stays with the seat, so taking a seat over (its owner coming back, or
                // anyone else) never refreshes an active. Only a running active stops here, before the class changes; leave already stopped the owner's.
                Commander.stop(this,s);s.banner=campaign.banner(s);s.commander=campaign.profile(s.owner).commander;
            }else{
                s=new Squad(p.getUuid(),team,fieldBase(team));s.seated=decision.seated();
                // A fresh seat mid-round fights under its owner's class, as round setup (Campaign.prepareRound) and a takeover give it.
                if(s.seated){s.commander=campaign.profile(s.owner).commander;s.banner=campaign.banner(s);reinforce(s);}
            }
            squads.put(s.owner,s);welcomed.add(s.owner);
            p.changeGameMode(GameMode.ADVENTURE);equipPlayer(p);
            campaign.joined(p);
            if(running&&s.seated)campaign.participants.add(s.owner);
            else if(running&&!campaign.away(s.owner))campaign.participants.remove(s.owner); // a commander who dropped and comes back to no seat is still owed the time he was seated

        }
        void clearOldUnits(){List<Entity> stale=new ArrayList<>();for(Entity e:world.iterateEntities())if(e.getCommandTags().contains("gravesandglory")||e.getCommandTags().contains("bannerbound")||e.getCommandTags().contains(Projectiles.TAG))stale.add(e);for(Entity e:stale){leaveSide(e);e.discard();}}
        /** Gives the seat held under one key another, in the same place in the seat list (squads keeps join order, and a seat's index is its place in it). */
        void rekey(UUID from,UUID to){
            var entries=new ArrayList<>(squads.entrySet());squads.clear();
            for(var e:entries)squads.put(e.getKey().equals(from)?to:e.getKey(),e.getValue());
        }
        void leave(UUID owner){
            campaign.left(owner);
            Squad s=squads.get(owner);welcomed.remove(owner);respawns.remove(owner);if(s!=null)Commander.stop(this,s);
            // Mid-battle, the army waits on its last orders for disconnectGraceSeconds, then a bot commands it until its owner rejoins (Kort 2026-11-01).
            if(s!=null&&running&&s.seated&&!s.bot){campaign.dropped(s);UUID stand=UUID.randomUUID();s.owner=stand;s.bot=true;s.tier=campaign.difficulty;s.takeoverAt=MatchRules.takeoverAt(time,Spec.CAMPAIGN[0].disconnectGraceSeconds());rekey(owner,stand);campaign.vacated.put(owner,stand);campaign.vacatedUntil.put(owner,MatchRules.releaseAt(time));return;}
            squads.remove(owner);
            units.removeIf(u->{if(u.squad==s){leaveSide(u.mob);u.mob.discard();return true;}return false;});
        }
        static Vec3d base(int t){var f=Spec.FACTIONS[t];return new Vec3d(f.spawnX(),Arena.height(f.spawnX(),f.spawnZ())+1,f.spawnZ());}
        /** Where a team respawns, deploys from and falls back to: the round's mode decides (Domination's faction base). */
        Vec3d fieldBase(int team){return mode.fieldBase(this,team);}
        /** A team's barracks access point: the round's mode decides (Domination's faction barracks). */
        Vec3d fieldBarracks(int team){return mode.fieldBarracks(this,team);}
        /** Every teleport of a player goes through here. This runs at END_SERVER_TICK, after the player's network handler has ticked, so the handler's
         *  last-tick position is still the old one when the client's teleport confirm and first move packet arrive: a long jump (camp to arena) read as
         *  "moved too quickly". syncWithPlayerPosition() moves that reference to the new spot, which is what the vanilla /tp path gets from the next tick. */
        void relocate(ServerPlayerEntity p,double x,double y,double z,float yaw,float pitch){p.fallDistance=0;p.setVelocity(Vec3d.ZERO);p.teleport(world,x,y,z,yaw,pitch);TeleportSync.afterTeleport(p);p.fallDistance=0;p.velocityModified=true;}
        void toBase(ServerPlayerEntity p,int team){Vec3d b=fieldBase(team);relocate(p,b.x,b.y+1,b.z,team==0?-90:90,8);}
        void equipPlayer(ServerPlayerEntity p){
            p.getInventory().clear();p.getInventory().insertStack(new ItemStack(Items.IRON_SWORD));
            p.getInventory().insertStack(new ItemStack(Items.BOW));p.getInventory().insertStack(new ItemStack(Items.ARROW,64));
            for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                Item item=switch(slot){case HEAD->Items.IRON_HELMET;case CHEST->Items.IRON_CHESTPLATE;case LEGS->Items.IRON_LEGGINGS;default->Items.IRON_BOOTS;};
                p.equipStack(slot,unbreakable(item));
            }
            Commander.applyPlayer(p,squads.get(p.getUuid()));p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);
        }
        static ItemStack unbreakable(Item item){if(item==Items.AIR)return ItemStack.EMPTY;ItemStack s=new ItemStack(item);s.getOrCreateNbt().putBoolean("Unbreakable",true);return s;}
        public void command(ServerPlayerEntity p,String cmd){
            if(cmd.startsWith("social:")){campaign.socialCommand(p,cmd);return;}
            if(cmd.startsWith("progress:")){campaign.progressCommand(p,cmd);return;}
            Squad s=squads.get(p.getUuid());if(s==null||!ready||respawns.containsKey(s.owner))return;
            if(campaign.handle(p,cmd))return;
            if(running&&!s.seated)return;
            if(cmd.equals("ability")){Commander.activate(this,p);return;}
            if(cmd.equals("barracks")){openBarracks(p);return;}
            if(cmd.startsWith("train:")||cmd.startsWith("disband:")||cmd.equals("cancel-training")){manage(p,s,cmd);return;}
            if(cmd.startsWith("gear:")){Veterans.buy(this,p,s,cmd);return;} // gear:<troop>:<weapon|armour>:<tier>, checked as manage checks training (in-match progression design 5)
            if(time-s.lastOrder<R.commandCooldownTicks())return;s.lastOrder=time;
            if(!running)return;
            for(var f:Spec.FORMATIONS)if(cmd.equals("formation:"+f.id())){
                s.formation=f.id();s.yaw=p.getYaw();for(Assignment a:s.assignments.values())a.formation=f.id();
                if(Diagnostics.active()&&testedFormations.add(f.id())&&testedFormations.size()==Spec.FORMATIONS.length)Diagnostics.report("formations-commanded",true,null);
                campaign.tutorialDid(p,1);return;
            }
            for(var order:Spec.ORDERS)if(cmd.equals(order.id())){
                boolean charging=order.id().equals("charge")&&startsCharge(s,"all");
                s.assignments.clear();s.arrived.clear();s.order=order.id();s.yaw=p.getYaw();if(charging)markCharge(s,"all");
                if(order.id().equals("charge")||order.id().equals("retreat"))BattleEffects.feedback(this,p,order.id()); // the command's feedback, to this player alone
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
        /** The order a group is under: its stored one. No group ever leaves it on its own (the no-routing design); only its commander's next order changes it. */
        Assignment assignment(Unit u){return storedAssignment(u.squad,u.spec.group());}
        Assignment storedAssignment(Squad s,String group){return s.assignments.getOrDefault(group,new Assignment(s.order,s.formation,s.target,s.yaw));}
        /** Stamps the charge of the named group(s) (Squad.chargedAt), which the counters table's charge impact (row C5) reads. Nothing sounds in the field: the commanding player alone hears his feedback (BattleEffects.feedback), on the order paths. */
        void markCharge(Squad s,String group){for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id())))s.chargedAt.put(g.id(),time);}
        /** Whether a Charge order on the named group(s) starts a charge: some named group is not charging already. */
        boolean startsCharge(Squad s,String group){return Arrays.stream(Morale.GROUPS).anyMatch(g->(group.equals("all")||group.equals(g))&&!storedAssignment(s,g).order.equals("charge"));}
        /** An order's spot lies inside the arena: finite, and at least 10 blocks off its edge. */
        static boolean inArena(double x,double z){return Double.isFinite(x)&&Double.isFinite(z)&&Math.abs(x)<=R.arenaHalfX()-10&&Math.abs(z)<=R.arenaHalfZ()-10;}
        /** River water below the crossings: no order sends a group there (a bridge or a ford is ground at 64 or above). */
        static boolean wet(double x,double z){return Arena.river((int)x,(int)z)&&Arena.ground((int)x,(int)z)<64;}
        /** The point an order to (x, z) sends a group to: the ground there, one block up, whatever height whoever gave it stands at. */
        static Vec3d groundAt(double x,double z){return new Vec3d(x,Arena.ground((int)x,(int)z)+1,z);}
        /**
         * The one place a group order takes effect, for a player's order (tactical) and the Marshal's Muster alike: each named group (a group id, or "all")
         * stores a fresh Assignment of this order (never shared, since a later formation change edits one group's), and those groups' troops drop their target,
         * spell centre and path so they turn at once, and the group's arrival latch (Squad.arrived) is cleared. The caller has vetted the spot (inArena, wet, groundAt) and, for a player, the command cooldown.
         */
        void issue(Squad s,String group,String action,String formation,Vec3d dest,float yaw,int enemyId){
            for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id()))){
                Assignment a=new Assignment(action,formation,dest,yaw);a.enemy=enemyId;a.issued=time;s.assignments.put(g.id(),a);s.arrived.remove(g.id());
            }
            for(Unit u:units)if(u.squad==s&&(group.equals("all")||u.spec.group().equals(group))){u.lockedTarget=null;u.spellCenter=null;u.mob.setTarget(null);u.mob.getNavigation().stop();u.nextPath=0;u.stuckTicks=0;}
        }
        public void tactical(ServerPlayerEntity p,String group,String action,String formation,double x,double z,float yaw,int enemyId){
            Squad s=squads.get(p.getUuid());
            if(s==null||!ready||!running||!s.seated||time<campaign.introUntil||respawns.containsKey(s.owner)||time-s.lastOrder<R.commandCooldownTicks())return;
            if(Arrays.stream(Spec.GROUPS).noneMatch(g->g.id().equals(group))||Arrays.stream(Spec.FORMATIONS).noneMatch(f->f.id().equals(formation)))return;
            if(action.equals("formation")){
                for(var g:Spec.GROUPS)if(!g.id().equals("all")&&(group.equals("all")||group.equals(g.id()))){Assignment a=s.assignments.computeIfAbsent(g.id(),k->new Assignment(s.order,s.formation,s.target,s.yaw));a.formation=formation;}
                s.lastOrder=time;campaign.tutorialDid(p,1);return;
            }
            var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(action)).findFirst().orElse(null);
            if(order==null||!Float.isFinite(yaw)||!inArena(x,z))return;
            if(!action.equals("attack")&&!action.equals("charge"))enemyId=-1;
            Entity enemy=world.getEntityById(enemyId);
            if(enemyId>=0&&(enemy==null||!enemy.isAlive()||teamOf(enemy)<0||teamOf(enemy)==s.team||!p.canSee(enemy))){p.sendMessage(Text.literal("Target lost. Aim again or issue an attack-move."),true);return;}
            if(action.equals("charge")&&enemy==null){p.sendMessage(Text.literal("Charge needs a visible enemy. Use Attack to advance to terrain."),true);return;}
            if(wet(x,z)){p.sendMessage(Text.literal("Choose dry ground or a crossing."),true);return;}
            s.lastOrder=time;Vec3d dest=groundAt(x,z);boolean charging=action.equals("charge")&&startsCharge(s,group);
            if(action.equals("follow"))dest=p.getPos();
            issue(s,group,action,formation,dest,yaw,enemyId);
            if(charging)markCharge(s,group);
            campaign.tutorialDid(p,action.equals("attack")||action.equals("charge")?2:0);campaign.progressEvent(p.getUuid(),false);
            // The command's feedback, to this player alone: Retreat's and Charge's own cues (once per kind per hornCooldownSeconds, else nothing), every other order the chime (the ServerPlayerEntity overload: to his connection only).
            if(action.equals("retreat")||action.equals("charge"))BattleEffects.feedback(this,p,action);else p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),SoundCategory.PLAYERS,.6f,1.1f);
            p.sendMessage(Text.literal(group+" / "+action+" / "+(int)x+", "+(int)z),true);
        }
        /** The room a troop's body takes in a formation and the lane it takes on a bridge: its width and 0.3, at least 1.3 (BattleMath.spacing). */
        public static double spacing(Unit u){return BattleMath.spacing(u.mob.getWidth());}
        Vec3d formation(Unit u){
            Assignment a=assignment(u);if(!u.squad.bot&&u.squad.order.equals("hold")&&!u.squad.assignments.containsKey(u.spec.group()))return u.deployed;var f=Arrays.stream(Spec.FORMATIONS).filter(v->v.id().equals(a.formation)).findFirst().orElse(Spec.FORMATIONS[0]);
            int slot=0;double cell=f.spacing();
            for(Unit other:units)if(other.squad==u.squad&&other.spec.group().equals(u.spec.group())&&other.mob.isAlive()){if(other.slot<u.slot)slot++;cell=Math.max(cell,spacing(other));} // the cell is the widest body of the group, so a golem's neighbours do not overlap it
            boolean siege=u.spec.group().equals("siege"); // behind the archers: a mortar cannot fire at what stands within its minimum range
            double role=u.spec.group().equals("support")?9:siege?7:u.spec.group().equals("ranged")?5:0;
            int columns=Math.min(f.columns(),u.spec.group().equals("support")?2:siege||u.spec.group().equals("ranged")?6:12);
            double side=(slot%columns-(columns-1)/2.0)*cell,back=role+(slot/columns+1)*cell;
            if(f.wedge()){int rank=(int)Math.floor(Math.sqrt(slot));side=(slot-rank*rank-rank)*cell;back=role+(rank+1)*cell;}
            if(a.perch){side=(slot%3-1)*1.0;back=slot/3*1.0;} // a post's floor is a few blocks across: rows of three a block apart, from the post back, at its height
            double angle=Math.toRadians(a.yaw);return a.target.add(Math.cos(angle)*side+Math.sin(angle)*back,0,Math.sin(angle)*side-Math.cos(angle)*back);
        }
        public boolean atBarracks(ServerPlayerEntity p,Squad s){
            return horizontalDistance(p.getPos(),fieldBarracks(s.team))<=Spec.ECONOMY[0].accessRadius()*Spec.ECONOMY[0].accessRadius();
        }
        void openBarracks(ServerPlayerEntity p){if(running)campaign.tutorialDid(p,3);if(ServerPlayNetworking.canSend(p,OPEN_BARRACKS))ServerPlayNetworking.send(p,OPEN_BARRACKS,PacketByteBufs.create());}
        /** Why a troop cannot join the squad's training queue now, or null when it can: a troop trains only while its living and queued units are below its count in the squad's banner, and slot weights stay within rules.squadSize. */
        String roomFor(Squad s,Spec.TroopsRow spec){
            int listed=Collections.frequency(s.banner,spec.id());
            if(listed==0)return spec.name()+" is not in your banner. Only your banner's troops train here.";
            long fielded=fielded(s).filter(u->u.spec==spec).count()+s.training.stream().filter(t->t.spec()==spec).count();
            if(fielded>=listed)return "All "+listed+" "+spec.name()+" of your banner are fielded or in training.";
            int weight=fielded(s).mapToInt(u->u.spec.slotWeight()).sum()+s.training.stream().mapToInt(t->t.spec().slotWeight()).sum();
            if(weight+spec.slotWeight()>R.squadSize())return "Army full: disband a troop to make room for a different unit.";
            return null;
        }
        /** A free troop at the back of the squad's training queue (a Bloodrush refund), done after the one before it; false, and nothing queued, when the queue is full or roomFor says no. */
        boolean queueFree(Squad s,Spec.TroopsRow spec){
            if(s.training.size()>=Spec.ECONOMY[0].maxQueue()||roomFor(s,spec)!=null)return false;
            long previous=s.training.isEmpty()?time:s.training.getLast().finished();
            s.training.addLast(new Training(spec,Math.max(time,previous)+Commander.trainingTicks(s,spec)));return true;
        }
        void manage(ServerPlayerEntity p,Squad s,String cmd){
            if(!atBarracks(p,s)){p.sendMessage(Text.literal("Return to your faction's barracks to train or disband troops."),true);return;}
            if(cmd.equals("cancel-training")){for(Training t:s.training)s.gold+=t.paid();s.training.clear();return;}
            String id=cmd.substring(cmd.indexOf(':')+1);
            var spec=Campaign.troop(id);if(spec==null)return;
            if(cmd.startsWith("disband:")){
                for(int i=units.size()-1;i>=0;i--){Unit u=units.get(i);if(u.squad==s&&!u.temporary&&u.spec==spec&&u.mob.isAlive()){leaveSide(u.mob);u.mob.discard();units.remove(i);s.gold+=Spec.ECONOMY[0].disbandRefund();Houses.recompute(s);return;}}
                return;
            }
            String full=roomFor(s,spec);if(full!=null){p.sendMessage(Text.literal(full),true);return;}
            int price=Commander.cost(s,spec);
            if(s.gold<price){p.sendMessage(Text.literal("Not enough gold. Battle supplies arrive over time; holding points increases them."),true);return;}
            if(s.training.size()>=Spec.ECONOMY[0].maxQueue())return;
            s.gold-=price;long previous=s.training.isEmpty()?time:s.training.getLast().finished();
            s.training.addLast(new Training(spec,Math.max(time,previous)+Commander.trainingTicks(s,spec),price));
        }
        /** Round gold a squad earns per income tick: the mode's income, Domination's passive gold plus its team's held points. */
        int income(Squad s){return mode.income(this,s);}
        void trainingTick(){
            for(Squad s:squads.values())while(!s.training.isEmpty()&&s.training.getFirst().finished()<=time){
                Training t=s.training.removeFirst();Set<Integer> used=new HashSet<>();fielded(s).forEach(u->used.add(u.slot));
                int slot=-1;for(int i=0;i<s.banner.size()&&i<R.squadSize();i++)if(s.banner.get(i).equals(t.spec().id())&&!used.contains(i)){slot=i;break;} // its own banner slot, so it forms where the banner puts it
                if(slot<0){slot=0;while(used.contains(slot))slot++;}
                if(slot>=R.squadSize()){s.gold+=t.paid();continue;}
                var at=deploy(s,slot);spawn(s,t.spec(),slot,at.at());Morale.fielded(this,s,t.spec().group());
                FeedEvents.deployed(this,s,t.spec());
            }
        }
        /**
         * Where this team's new troops may deploy instead of the barracks: its held points (held forwardHoldSeconds, uncontested, no enemy near where the troops
         * would appear) and its standing Forward Supply banners (no enemy within the Quartermaster's radius of the banner).
         */
        List<Anchor> reinforcementAnchors(int team){
            List<Anchor> anchors=new ArrayList<>(mode.anchors(this,team)); // the mode's places: Domination's held points, none while the flags are down
            var supply=Commander.byActive("forward_supply");
            if(supply!=null)for(var st:structures)if(st.team()==team&&!(st instanceof SiegeMode.GateTarget)&&st.isAlive()&&!Morale.enemyWithin(this,team,st.getPos(),supply.radius()))anchors.add(st.anchor());
            return anchors;
        }
        /** Domination's forward places (BattleMode.anchors): this team's held points, held forwardHoldSeconds, uncontested and with no enemy near where the troops would appear. */
        List<Anchor> pointAnchors(int team){
            var E=Spec.ECONOMY[0];List<Anchor> anchors=new ArrayList<>();
            for(Capture p:points)if(p.owner==team&&!p.contested&&time-p.ownedSince>=E.forwardHoldSeconds()*20L&&!Morale.enemyWithin(this,team,deployCentre(team,p),E.forwardSafeRadius()))anchors.add(Anchor.of(p));
            return anchors;
        }
        /** Where troops deployed at a held point appear: its centre moved forwardSpawnOffset toward the team's base; each slot then stands in its ring place around this spot. The safe radius is measured here, not at the point centre. */
        Vec3d deployCentre(int team,Capture p){return deployCentre(team,Anchor.of(p));}
        /** The same for any anchor: a point's is moved toward the base, a supply banner's is the banner itself. */
        Vec3d deployCentre(int team,Anchor a){
            double x=a.x(),z=a.z();
            if(points.stream().anyMatch(p->p.spec.x()==a.x()&&p.spec.z()==a.z())){
                Vec3d home=fieldBase(team),back=new Vec3d(home.x-a.x(),0,home.z-a.z());back=back.lengthSquared()<1e-6?Vec3d.ZERO:back.normalize().multiply(Spec.ECONOMY[0].forwardSpawnOffset());
                x+=back.x;z+=back.z;
            }
            return new Vec3d(x,Arena.ground((int)Math.floor(x),(int)Math.floor(z))+1,z);
        }
        /** Where a troop trained now appears, and the anchor it deploys at (null: the barracks). Chosen at spawn time, so a point or banner that was lost, contested or approached since the purchase sends the troop home. */
        record Deploy(Vec3d at,Anchor anchor){}
        Deploy deploy(Squad s,int slot){
            Vec3d home=fieldBase(s.team),ring=new Vec3d((slot%5-2)*2,0,(slot/5-2)*2),barracks=home.add(ring);
            if(!forwardReinforcement)return new Deploy(barracks,null);
            List<Anchor> anchors=reinforcementAnchors(s.team);if(anchors.isEmpty())return new Deploy(barracks,null);
            Vec3d target=storedAssignment(s,"infantry").target;double[] ax=new double[anchors.size()],az=new double[anchors.size()];
            for(int i=0;i<ax.length;i++){ax[i]=anchors.get(i).x();az[i]=anchors.get(i).z();}
            int chosen=BattleMath.nearestAnchor(target.x,target.z,home.x,home.z,ax,az);if(chosen<0)return new Deploy(barracks,null);
            Anchor anchor=anchors.get(chosen);Vec3d centre=deployCentre(s.team,anchor);
            double x=centre.x+ring.x,z=centre.z+ring.z;int gx=(int)Math.floor(x),gz=(int)Math.floor(z);
            if(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)return new Deploy(barracks,null);
            return new Deploy(new Vec3d(x,Arena.ground(gx,gz)+1,z),anchor);
        }
        Vec3d deployPoint(Squad s,int slot){return deploy(s,slot).at();}
        /** The deploy notice after "<troop> trained: " (a null anchor means the barracks). */
        String deployNote(Squad s,Anchor anchor){return anchor==null?"deploying from barracks":"deploying at "+anchor.name();}
        void startRound(){
            // The previous mode's pieces go back before a new mode is built: a Siege round re-entered here must not re-save its shut gate over the open one.
            // A piece that cannot be restored stays in saves, and the new round's marker lists it, so a crash still recovers it.
            if(!mode.teardown(this))System.err.println("[Graves and Glory] startRound: "+saves.size()+" saved piece(s) of the last "+mode.id()+" round could not be restored; the marker keeps them.");
            if(!Modes.registered(campaign.battleMode)||!Modes.hosts(campaign.battleMode,campaign.map))campaign.battleMode=Modes.DOMINATION;
            mode=Modes.create(campaign.battleMode);
            roundSeconds=mode.row.roundSeconds();
            Projectiles.clear(this);Abilities.clearBanners(this);Houses.reset(this);FeedEvents.clear();clearProgression();
            campaign.prepareRound();
            clearSides();
            Arrays.fill(scores,0);pointSeconds=new int[2][points.size()];
            for(Capture p:points){p.owner=-1;p.progress=0;p.challenger=-1;Arena.flag(world,p.spec,-1);}
            mode.setup(this); // after the flags, so a mode that takes them down keeps them down
            respawns.clear();BattleEffects.startRound();Morale.startRound();
            for(Squad s:squads.values()){
                Commander.reset(this,s);if(!s.seated)continue;s.assignments.clear();s.arrived.clear();s.training.clear();s.lastStand.clear();s.lastStandAt.clear();s.fullHealth.clear();s.fullCount.clear();s.chargedAt.clear();s.traits.clear();s.fallen.clear();s.hitBy=null;s.gold=Spec.ECONOMY[0].startingGold();s.stats=new MatchStats.SquadStats();s.order=s.bot?"attack":"hold";s.target=fieldBase(s.team);s.reinforceAt=time+R.reinforceSeconds()*20L;
                var player=server.getPlayerManager().getPlayer(s.owner);
                if(player!=null){toBase(player,s.team);player.changeGameMode(GameMode.ADVENTURE);equipPlayer(player);}
                reinforce(s);
            }
            Audio.reset(); // before the mode's first tick: a new round's cooldowns start empty and nothing queued by the last one plays
            rounds++;roundStarted=time;running=true;message=mode.startLine(this);
            mode.mark(this); // the mode says what it changed: flags down, saved pieces
        }
        void reinforce(Squad s){
            Set<Integer> occupied=new HashSet<>();fielded(s).forEach(u->occupied.add(u.slot));
            int slot=0,index=0,count=1;for(Squad other:squads.values()){if(other==s)break;if(other.seated&&other.team==s.team)index++;}
            for(Squad other:squads.values())if(other!=s&&other.seated&&other.team==s.team)count++; // this team's armies, this one included (a joining squad is not in squads yet)
            List<String> line=s.banner; // from Campaign.prepareRound: the slot index is the position, and empty and ^ slots stay empty
            Morale.lineup(this,s,line);
            Vec3d from=mode.lineup(this,s.team);
            for(String id:line){int position=slot++;if(position>=R.squadSize()||occupied.contains(position))continue;
                var spec=Campaign.troop(id);if(spec==null)continue;
                double side=s.team==0?1:-1,x=from.x+side*(20-position/5*3),z=from.z+side*((index-(count-1)/2.0)*22+(position%5-2)*2.5); // rows centred on the base for any army count; Dusk's mirror Dawn's, so army i forms on the side of its lane point (BattleMath.laneTarget)
                spawn(s,spec,position,new Vec3d(x,Arena.ground((int)x,(int)z)+1,z));
            }
            Houses.recompute(s);
        }

        Unit spawn(Squad s,Spec.TroopsRow spec,int slot,Vec3d pos){
            MobEntity mob=ArmyMobs.create(spec.entity(),world);
            mob.refreshPositionAndAngles(pos.x,pos.y,pos.z,s.team==0?-90:90,0);
            mob.setPersistent();mob.addCommandTag("gravesandglory");
            mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(spec.health()*campaign.health(s,spec));mob.setHealth((float)(spec.health()*campaign.health(s,spec)));
            mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(spec.speed());
            mob.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(Spec.NAVIGATION[0].searchRange());
            mob.setPathfindingPenalty(PathNodeType.WATER,(float)Spec.NAVIGATION[0].waterPenalty());
            if(ArmyMobs.tags(spec).contains("shield"))mob.equipStack(EquipmentSlot.OFFHAND,shield(spec,side(s.team).dye()));
            mob.equipStack(EquipmentSlot.MAINHAND,unbreakable(Registries.ITEM.get(new Identifier(spec.weapon()))));
            mob.equipStack(EquipmentSlot.HEAD,unbreakable(Registries.ITEM.get(new Identifier(spec.helmet()))));
            ItemStack chest=unbreakable(Registries.ITEM.get(new Identifier(spec.chest())));
            if(chest.getItem() instanceof DyeableItem dye)dye.setColor(chest,side(s.team).color());
            mob.equipStack(EquipmentSlot.CHEST,chest);
            for(EquipmentSlot eq:EquipmentSlot.values())mob.setEquipmentDropChance(eq,0);
            mob.setCustomName(troopName(s,spec,1));
            mob.setCustomNameVisible(false);
            Unit u=new Unit(mob,s,spec,slot);u.stance=Tactics.stance(this,u);
            spawning=u;try{world.spawnEntity(mob);}finally{spawning=null;} // the players in range start tracking it here (its stance goes with that)
            joinSide(mob,s.team);
            units.add(u);Houses.recompute(s);return u;
        }
        /**
         * A troop's hidden custom name at a level: its level name (ArmyMobs.levelName) and, for a commander's Mastery II troop, the veteran suffix. Battle.spawn names a new troop at
         * level 1 and Veterans.levelUp renames him as he rises. The name colour is the team tag, not the side colour: CommandMode.nameTeam reads it on the client to tell friend from
         * foe (both teams can field the same House). Recolour names by side only together with that reader.
         */
        Text troopName(Squad s,Spec.TroopsRow spec,int level){return Text.literal(ArmyMobs.levelName(spec,level)+(campaign.veteran(s,spec)?ArmyMobs.VETERAN:"")).formatted(s.team==0?Formatting.AQUA:Formatting.LIGHT_PURPLE);}
        final Set<Long> unitChunks=new HashSet<>();
        void updateUnitChunks(){
            if(time%40!=0)return;Set<Long> wanted=new HashSet<>();
            if(running)for(Unit u:units)if(u.mob.isAlive()){int x=u.mob.getBlockX()>>4,z=u.mob.getBlockZ()>>4;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)wanted.add(net.minecraft.util.math.ChunkPos.toLong(x+dx,z+dz));}
            if(running)for(var st:structures)if(st.isAlive()){int x=st.getBlockX()>>4,z=st.getBlockZ()>>4;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)wanted.add(net.minecraft.util.math.ChunkPos.toLong(x+dx,z+dz));}
            for(long key:wanted)if(!unitChunks.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,true);}
            for(long key:unitChunks)if(!wanted.contains(key)){var p=new net.minecraft.util.math.ChunkPos(key);world.setChunkForced(p.x,p.z,false);}
            unitChunks.clear();unitChunks.addAll(wanted);
        }
        void unitTick(Unit u){ Abilities.unitTick(this,u,time);Tactics.tick(this,u,time); }
        void captureTick(){
            if(!mode.capturesPoints(this)){for(Capture p:points){p.contested=false;p.progress=0;p.challenger=-1;}return;} // flags down: no point is taken, held or scored
            Set<Squad> onPoint=new HashSet<>(); // armies with a troop or their commander inside any point radius this second
            for(Capture p:points){
                int[] presence={0,0};Vec3d center=new Vec3d(p.spec.x(),65,p.spec.z());
                for(Unit u:units)if(u.mob.isAlive()&&horizontalDistance(u.mob.getPos(),center)<p.spec.radius()*p.spec.radius()){presence[u.squad.team]++;onPoint.add(u.squad);}
                for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList()){
                    Squad s=squads.get(player.getUuid());
                    if(s!=null&&s.seated&&!player.isSpectator()&&horizontalDistance(player.getPos(),center)<p.spec.radius()*p.spec.radius()){presence[s.team]+=R.capturePlayersWeight();onPoint.add(s);}
                }
                p.contested=presence[0]>0&&presence[1]>0;
                int team=presence[0]>0&&presence[1]==0?0:presence[1]>0&&presence[0]==0?1:-1;
                if(team<0||team==p.owner){if(team==p.owner){p.progress=0;p.challenger=-1;}continue;}
                if(p.challenger!=team){p.challenger=team;p.progress=0;Audio.all(this,AudioCues.Cue.CAPTURE_START,p.spec.name());} // a key per point: a second point contested inside the cooldown still sounds, and two in one tick sound once (the cue's per-tick cap)
                p.progress++;
                if(p.progress>=p.spec.captureSeconds()){
                    p.owner=team;p.ownedSince=time;p.challenger=-1;p.progress=0;Arena.flag(world,p.spec,team);FeedEvents.point(this,p,team);Audio.captured(this,p.spec.name(),team);
                    for(ServerPlayerEntity pl:server.getPlayerManager().getPlayerList()){var squad=squads.get(pl.getUuid());if(squad!=null&&squad.seated&&squad.team==team&&horizontalDistance(pl.getPos(),center)<p.spec.radius()*p.spec.radius())campaign.progressEvent(pl.getUuid(),true);}
                    for(ServerPlayerEntity pl:server.getPlayerManager().getPlayerList())if(squads.containsKey(pl.getUuid())&&squads.get(pl.getUuid()).seated)pl.sendMessage(Text.literal(side(team).name()+" captured "+p.spec.name()).setStyle(net.minecraft.text.Style.EMPTY.withColor(side(team).color())),true);
                }
            }
            for(Squad s:onPoint)s.stats.objectiveSeconds++;
            MatchStats.teamPointSeconds(pointSeconds,points.stream().mapToInt(c->c.owner).toArray());
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
            // A commander's own blow: a troop takes it at combatDiscipline.damageScale, and the commander effects (his Mark Prey, the target side's Stand Fast) multiply in. Under his Bloodrush a blow by hand feeds him from what the target really loses (this hook fires before vanilla's shield block and hurt cooldown, which may take nothing), so the scaled blow is dealt here and measured.
            if(!scalingDamage&&attacker instanceof ServerPlayerEntity p){
                double scale=(entity instanceof ServerPlayerEntity?1:Spec.COMBATDISCIPLINE[0].damageScale())*Commander.playerFactor(this,attackerSquad,entity);
                boolean feeds=Commander.feeds(this,attackerSquad,source);
                if(scale!=1||feeds){
                    float before=entity.getHealth()+entity.getAbsorptionAmount();
                    scalingDamage=true;try{entity.damage(source,(float)(amount*scale));}finally{scalingDamage=false;}
                    if(feeds){float after=entity.getHealth()+entity.getAbsorptionAmount();Commander.lifesteal(this,attackerSquad,p,after>before?before:before-after);} // more than before: a commander struck down and respawned, or a troop that rose again, lost all it had
                    return false;
                }
            }
            Squad hitter=attackerSquad;Unit striker=null;for(Unit u:units)if(u.mob==attacker){hitter=u.squad;striker=u;}
            Squad side=hitter!=null?hitter:fallenSquad(attacker); // a commander's view of the blow: a shot that lands after its troop fell is still his army's (a troop victim has it from Combat.resolve's lastHitBy already)
            if(entity instanceof ServerPlayerEntity p&&p.getHealth()<=amount){
                if(Diagnostics.active())System.out.println("[Graves and Glory damage] respawn cause="+source.getType().msgId()+" amount="+amount+" hp="+p.getHealth()+" pos="+p.getPos()+" fall="+p.fallDistance);
                p.setHealth(p.getMaxHealth());toBase(p,team);p.changeGameMode(GameMode.SPECTATOR);
                respawns.put(p.getUuid(),time+Commander.respawnSeconds(victimSquad)*20L);FeedEvents.commander(this,p,false);
                Veterans.onCommanderDeath(this,p,victimSquad,team,attacker,striker,side); // a commander struck down never reaches AFTER_DEATH: his killers get the commander bounty here, and his credit log is forgotten (he stands again within its window)
                return false;
            }
            if(hitter!=null)for(Unit u:units)if(u.mob==entity){u.lastHitBy=hitter;u.lastHitAt=time;u.lastHitTroop=striker==null||striker.temporary?null:striker.spec.id();break;}
            if(side!=null&&victimSquad!=null&&side.team!=team){victimSquad.hitBy=side;victimSquad.hitAt=time;} // a commander's last enemy blow, as lastHitBy is a troop's: a death with no attacker soon after is that army's (Veterans.onCommanderDeath)
            if(striker!=null)Veterans.helped(this,striker,entity); // every troop blow the battle lets through (Combat.resolve's and any other) logs its striker as a helper on the victim (kill credit, design 3.2)
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
        /** A packet buffer as LevelMath's pure writer: writeBoolean, writeByte and writeString. */
        static LevelMath.Out wire(PacketByteBuf b){return new LevelMath.Out(){public void bool(boolean v){b.writeBoolean(v);}public void b(int v){b.writeByte(v);}public void str(String v){b.writeString(v);}};}
        /**
         * state_v9 to every player that takes it. The fields, in order: ready, running, scores, message, team, fielded count, order, formation, respawn
         * seconds, the points, gold, at barracks, training, the troop counts, the friendly markers, the three groups (each ends with state_v9's level2,
         * level3 and armouryOpen), the pings, the seconds left, the sightings, the camp snapshot, then state_v8's sides, House tiers and commander
         * ability, then state_v9's Armoury block (LevelMath.encodeArmoury). GravesAndGloryClient reads the same order.
         */
        void sendState(){
            updateSight();
            for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList())if(ServerPlayNetworking.canSend(p,STATE)){
                Squad s=squads.get(p.getUuid());PacketByteBuf b=PacketByteBufs.create();
                int[] ledger=mode.ledger(this);b.writeBoolean(ready);b.writeBoolean(running);b.writeInt(ledger[0]);b.writeInt(ledger[1]);
                b.writeString(message);b.writeInt(s==null?-1:s.team);
                b.writeInt(s==null?0:(int)fielded(s).count());
                b.writeString(s==null?"follow":s.order);b.writeString(s==null?"block":s.formation);
                b.writeInt(respawns.containsKey(p.getUuid())?(int)((respawns.get(p.getUuid())-time+19)/20):0);
                b.writeInt(points.size());for(Capture c:points){b.writeString(c.spec.id());b.writeInt(c.owner);b.writeInt(c.progress);b.writeInt(c.challenger);b.writeBoolean(c.contested);}
                b.writeInt(s==null?0:s.gold);b.writeBoolean(s!=null&&atBarracks(p,s));
                b.writeInt(s==null?0:s.training.size());b.writeInt(s==null||s.training.isEmpty()?0:(int)Math.max(0,(s.training.getFirst().finished()-time+19)/20));
                b.writeInt(Spec.TROOPS.length);
                for(var spec:Spec.TROOPS){b.writeInt(s==null?0:(int)fielded(s).filter(u->u.spec==spec).count());b.writeInt(s==null?0:(int)s.training.stream().filter(t->t.spec()==spec).count());}
                List<Unit> friendly=units.stream().filter(u->s!=null&&u.squad.team==s.team&&u.mob.isAlive()).toList();
                b.writeInt(friendly.size());
                for(Unit u:friendly){b.writeFloat((float)u.mob.getX());b.writeFloat((float)u.mob.getZ());b.writeBoolean(u.mob.getTarget()!=null);b.writeBoolean(u.squad==s);b.writeString(u.spec.name());}
                b.writeInt(3);
                for(String group:Morale.GROUPS){ // the three morale groups: the client reads exactly 3 (cavalry and siege wait for their troops)
                    var members=units.stream().filter(u->u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive()).toList();
                    Assignment a=s==null?new Assignment("follow","block",Vec3d.ZERO,0):storedAssignment(s,group);
                    b.writeString(group);b.writeInt(members.size());b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getHealth()/u.mob.getMaxHealth()).average().orElse(0));
                    b.writeString(a.order);b.writeString(a.formation);b.writeFloat((float)a.target.x);b.writeFloat((float)a.target.z);b.writeFloat(a.yaw);
                    b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getX()).average().orElse(a.target.x));b.writeFloat((float)members.stream().mapToDouble(u->u.mob.getZ()).average().orElse(a.target.z));
                    b.writeBoolean(members.stream().anyMatch(u->u.mob.getTarget()!=null));b.writeBoolean(s!=null&&Morale.lastStand(s,group)); // the last boolean was Phase 1's routing
                    b.writeInt(s==null?0:s.fullCount.getOrDefault(group,0));
                    b.writeFloat((float)BattleMath.strength(members.stream().mapToDouble(u->u.mob.getHealth()).sum(),s==null?0:s.fullHealth.getOrDefault(group,0.0)));
                    // state_v9, at the end of each group record: the living soldiers at level 2 and at level 3, and whether an Armoury buy for a troop of the group is open and not yet bought (Veterans.armouryOpen). On the merged base these follow the UI branch's fullCount and strength, never sit between lastStand and them.
                    b.writeInt((int)members.stream().filter(u->u.level==2).count());b.writeInt((int)members.stream().filter(u->u.level==3).count());b.writeBoolean(s!=null&&Veterans.armouryOpen(this,s,group));
                }
                var ability=testAbility;b.writeString(ability==null?"none":ability.id());b.writeInt(ability==null?0:ability.remaining());b.writeInt(ability==null?0:ability.total());
                var visiblePings=pings.stream().filter(v->s!=null&&v.team==s.team&&v.until>time).toList();b.writeInt(visiblePings.size());for(var ping:visiblePings){b.writeString(ping.kind);b.writeFloat((float)ping.x);b.writeFloat((float)ping.z);}
                b.writeInt(mode.clock(this));
                var memory=s==null?Map.<UUID,Sight>of():sightings.getOrDefault(s.team,Map.of());b.writeInt(memory.size());for(var sight:memory.values()){b.writeFloat(sight.x);b.writeFloat(sight.z);b.writeInt((int)(time-sight.seen));}
                b.writeString(campaign.snapshot(p));
                // Added in state_v8, after the Phase 1 fields: the side id each team flies, this army's House tiers (count, then House and tier), and the commander ability of a seated squad (its class's active id, ticks until ready, ticks left active; ready is -1 while this build has no behaviour for that active, and the fields are "", 0, 0 for anyone unseated).
                b.writeString(side(0).id());b.writeString(side(1).id());
                Map<String,Integer> tiers=s==null?Map.of():s.traits;b.writeByte(tiers.size());for(var tier:tiers.entrySet()){b.writeString(tier.getKey());b.writeByte(tier.getValue());}
                boolean commanding=s!=null&&s.seated;b.writeString(commanding?Commander.row(s).active():"");b.writeInt(commanding?Commander.readyIn(this,s):0);b.writeInt(commanding?Commander.activeFor(this,s):0);
                // state_v9, after the commander ability: this army's Armoury (LevelMath.encodeArmoury sets the layout: boolean armouryHere, byte n, then per banner troop type String troop and the bytes weapon2, weapon3, armour2, armour3, each a LevelMath.Buy ordinal). armouryHere is whether this commander stands where he may buy (Veterans.buyHere).
                LevelMath.encodeArmoury(s==null?LevelMath.Armoury.EMPTY:Veterans.armouryState(this,s,p),wire(b));
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
                    var spec=Campaign.troop(enemy.team==0?"knight":"revenant");
                    testVictim=spawn(enemy,spec,0,testSquad.target.add(4,0,0));testVictimHealth=testVictim.mob.getHealth();
                    testSquad.order="attack";testStage=3;testAt=time;
                }else if(time-testAt>1000){Diagnostics.report("point-captured",false,"troops did not take point");testStage=4;}
            }else if(testStage==3&&time-testAt>100){
                Diagnostics.report("combat-resolved",!testVictim.mob.isAlive()||testVictim.mob.getHealth()<testVictimHealth,"enemy took no damage");
                for(Unit u:units)if(u.squad!=testSquad){leaveSide(u.mob);u.mob.discard();}
                units.removeIf(u->u.squad!=testSquad);
                reinforce(testSquad);testSquad.order="hold";
                for(int i=0;i<3;i++){
                    int team=i==0?testSquad.team:1-testSquad.team;
                    Squad synthetic=new Squad(UUID.randomUUID(),team,fieldBase(team).add(0,0,i==0?28:(i-1)*35-20));
                    synthetic.order="hold";testOwners.add(synthetic.owner);
                    int slot=0;
                    synthetic.banner=Campaign.bannerRow(team==0?"gilded_easy":"graveborn_easy");
                    for(String id:synthetic.banner){var spec=Campaign.troop(id);if(spec!=null)spawn(synthetic,spec,slot,synthetic.target.add((slot%5-2)*2,0,slot/5*2));slot++;}
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
                    // Disband one troop of the banner's first type and train it back; the second train is refused, as its banner count is filled again.
                    String swap=testSquad.banner.stream().filter(id->Campaign.troop(id)!=null).findFirst().orElse("knight");
                    trainingSwapBefore=(int)units.stream().filter(u->u.squad==testSquad&&u.spec.id().equals(swap)&&u.mob.isAlive()).count();
                    trainingGold=testSquad.gold;manage(p,testSquad,"disband:"+swap);manage(p,testSquad,"train:"+swap);
                    int after=testSquad.gold;manage(p,testSquad,"train:"+swap);
                    boolean paid=after==trainingGold-Commander.cost(testSquad,Campaign.troop(swap))&&testSquad.gold==after&&testSquad.training.size()==1;
                    if(!paid)Diagnostics.report("barracks-management",false,"gold or capacity was not enforced");
                    Unit own=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()).findFirst().orElseThrow();
                    float hp=own.mob.getHealth();boolean damage=own.mob.damage(world.getDamageSources().mobAttack(own.mob),8);
                    Diagnostics.report("friendly-fire",!damage&&own.mob.getHealth()==hp,"same-faction damage was allowed");
                    testStage=7;testAt=time;
                }
            }else if(testStage==7&&testSquad.training.isEmpty()&&time-testAt>120){
                String swap=testSquad.banner.stream().filter(id->Campaign.troop(id)!=null).findFirst().orElse("knight");
                long alive=units.stream().filter(u->u.squad==testSquad&&u.mob.isAlive()).count(),swapped=units.stream().filter(u->u.squad==testSquad&&u.spec.id().equals(swap)&&u.mob.isAlive()).count();
                Diagnostics.report("barracks-management",alive==20&&swapped==trainingSwapBefore,"the disbanded troop was not trained back into the army");
                testStage=8;
            }
        }
        void prepareNavigation(double x,double z,Vec3d target,float yaw){
            testSquad.order="move";testSquad.target=target;testSquad.yaw=yaw;
            for(Unit u:units)if(u.squad==testSquad){double px=x+(u.slot%5)*1.5,pz=z+(u.slot/5-1.5)*1.4;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(px,Arena.ground((int)px,(int)pz)+1,pz,yaw,0);u.nextPath=0;u.stuckTicks=0;}
        }
        /** Rivergate's dawn ridge is the climb: troops start on its flat eastern foot, march west to the crest, and must end at least eight blocks above where they began, or on the crest when that is lower. */
        void prepareHillTest(){
            var ridge=Arrays.stream(Spec.TERRAIN).filter(t->t.id().equals("dawn_ridge")).findFirst().orElseThrow();
            int startX=ridge.x()+ridge.radiusX()*4/5,crestX=ridge.x()+6,z=ridge.z();
            // Eight blocks above the foot, or the crest when it is lower: the Dawn ridge was 11 blocks from its foot to its crest (67 to 78) until Task 8b put the supply house yard rg-supply-nw on the crest (74, with the foot at 71), so the threshold follows the ground (the standing height of the crest is the lower of the two there, 3 blocks up) and every unit must get there.
            hillClimbY=Math.min(Arena.ground(startX,z)+8,Arena.height(crestX,z)+1);
            prepareNavigation(startX,z,new Vec3d(crestX,Arena.height(crestX,z)+1,z),90);
        }
        void prepareBridgeTest(){var b=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList().get(testBridgeIndex);prepareNavigation(b.x()-b.width()/2.0-14,b.z(),new Vec3d(b.x()+b.width()/2.0+16,65,b.z()),-90);}
        void prepareVisualSquads(boolean fight){
            for(Unit u:units){leaveSide(u.mob);u.mob.discard();}units.clear();
            testSquad.training.clear();testSquad.order=fight?"attack":"hold";testSquad.target=fight?new Vec3d(15,65,3):new Vec3d(-74,65,0);testSquad.yaw=-90;
            spawnVisualSquad(testSquad,fight?-22:-74,3);
            if(fight){Squad enemy=new Squad(UUID.randomUUID(),1-testSquad.team,new Vec3d(-20,65,3));enemy.order="attack";enemy.yaw=90;spawnVisualSquad(enemy,22,3);}
        }
        void spawnVisualSquad(Squad s,int x,int z){
            if(s.banner.isEmpty())s.banner=Campaign.bannerRow(s.team==0?"gilded_easy":"graveborn_easy");
            int slot=0;for(String id:s.banner){var spec=Campaign.troop(id);int px=x+(slot%5-2)*2,pz=z+(slot/5-2)*2;if(spec!=null)spawn(s,spec,slot,new Vec3d(px,Arena.ground(px,pz)+1,pz));slot++;}
        }
    }
}



