package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Delayed, visible ranged hits. Tactics decides the shot; this queue lands it after its flight time. An archer's arrow is judged where it comes down (scatter, strike) and may miss; other shots land. */
public final class Projectiles {
    public static final String TAG="gravesandglory-shot";
    static final Spec.CombatDisciplineRow C=Spec.COMBATDISCIPLINE[0];
    /** Ticks after an enemy's last blow on a unit during which a death with no attacker still credits the blow's army. lastHitAt is stamped when a blow lands (melee, or an arrow on impact), so flight time never counts: this is a blow-to-death allowance, sized as projectileMaxTicks plus 20 so a late shot plus a short fall still pays. The +20 is a code-held tuning number; see docs/PARKED.md. */
    public static final int CREDIT_WINDOW_TICKS=C.projectileMaxTicks()+20;
    /** kind: "arrow" (damage, vanilla arrow visual), "trident" (the same damage as an arrow, a flying trident for the visual: the Harpooner's), "bolt" (the blight cast, visual only; with a target and an amount, a magic shot that damages on landing), "mend" (heal).
     *  The blight cast's bolt has no target: it flies to the fixed point {@code at}; other shots follow their target and leave {@code at} null.
     *  peak is how far the shot's arc rises over its straight line mid-flight (0: a straight shot), and ticks its whole flight (lands - fired).
     *  aimed holds, in its one slot, the target's aim point when the visual was last steered (at first, when it was fired), so the steering can lead
     *  a moving target by its last tick's motion (N6). range is the distance at the loose. aimPoint, an archer's shot only (scatter), is the fixed
     *  point it was loosed at: it flies there unsteered and lands only if it strikes its target there (strike); null for the steered shots. */
    public record Shot(GravesAndGlory.Unit shooter,GravesAndGlory.Squad squad,LivingEntity target,String kind,float amount,Vec3d from,long fired,long lands,Entity visual,Vec3d at,double peak,int ticks,Vec3d[] aimed,double range,Vec3d aimPoint){
        /** A straight shot (no arc) whose flight is lands - fired. */
        public Shot(GravesAndGlory.Unit shooter,GravesAndGlory.Squad squad,LivingEntity target,String kind,float amount,Vec3d from,long fired,long lands,Entity visual,Vec3d at){
            this(shooter,squad,target,kind,amount,from,fired,lands,visual,at,0,(int)(lands-fired),new Vec3d[1],0,null);
        }
    }
    /** Archer shots (Tactics.archer: bow, crossbow, thrower) since the server started, by the shooter's team and the range at the loose (rangeBin:
     *  under 8 blocks, 8 to 16, 16 to 24, 24 and over): fired, struck their target, and lost (the target fell or left before the shot came down, so
     *  it was never judged). Counters only; the bot simulation reads them (AiMetrics). */
    // Cheap (one array increment a shot), always on, never reset: readers take differences between two reads.
    public static final int RANGE_BINS=4;
    public static final long[][] ARROWS_FIRED=new long[2][RANGE_BINS],ARROWS_HIT=new long[2][RANGE_BINS],ARROWS_LOST=new long[2][RANGE_BINS],ARROWS_MISSED=new long[2][RANGE_BINS];
    /**
     * An archer's aim (Kort 2026-11-11: no homing, believable misses). The shot is loosed once, at the target's body led by its motion over the
     * last tick for the flight's ticks while it walks a path (a troop walking on into the shot is met; one that stops, turns or is shoved is
     * not); a mob standing to fight is shot where it stands, since its shuffle foretells nothing (leading it lost hits in the sims). Scattered by a
     * normal error of sigma = arrowSpreadBase + arrowSpreadPerBlock x range blocks across and along the line and half that up and down. The
     * arrow then flies that fixed curve; strike judges it against the target where it comes down. The Mortar's crew, casters and mends keep
     * the old steered shot that always lands.
     */
    static Vec3d scatter(GravesAndGlory.Unit u,LivingEntity target,Vec3d from,Vec3d to,int travel){
        boolean marching=!(target instanceof net.minecraft.entity.mob.MobEntity m)||!m.getNavigation().isIdle(); // a mob is led only while it walks a path: a fighting troop shuffles, and its last tick foretells nothing
        double vx=target.getX()-target.prevX,vz=target.getZ()-target.prevZ;
        double sigma=BattleMath.arrowSigma(C.arrowSpreadBase(),C.arrowSpreadPerBlock(),from.distanceTo(marching?to.add(vx*travel,0,vz*travel):to));
        var r=u.mob.getRandom();double across=r.nextGaussian()*sigma,along=r.nextGaussian()*sigma,up=r.nextGaussian()*sigma*.5;
        double[] aim=BattleMath.arrowAim(from.x,from.z,xyz(to),vx,vz,travel,marching,across,along,up); // the geometry is BattleMathCheck's
        return new Vec3d(aim[0],aim[1],aim[2]);
    }
    /** How far round its hitbox a body is struck by a passing arrow (blocks): the arrow's own size. */
    static final double HIT_MARGIN=.15;
    /** Where an archer's shot strikes its target, or null for a miss: the last three ticks of its curve and two ticks' travel on past its aim
     *  point, against the target's hitbox (HIT_MARGIN round it) where it stands on the landing tick. */
    static Vec3d strike(Shot s){
        var box=s.target.getBoundingBox().expand(HIT_MARGIN);
        double[] hit=BattleMath.arrowStrike(xyz(s.from),xyz(s.aimPoint),s.peak,s.ticks,new double[]{box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ}); // BattleMathCheck's geometry
        return hit==null?null:new Vec3d(hit[0],hit[1],hit[2]);
    }
    static double[] xyz(Vec3d v){return new double[]{v.x,v.y,v.z};
    }
    /** At most this many ticks a missed arrow flies on past its aim point before it is gone. */
    static final int FLY_ON_MAX=10;
    /** A missed shot: out of the queue; its arrow keeps its last tick's step until it reaches the ground under its aim point (FLY_ON_MAX ticks at most), then goes. */
    static void flyOn(GravesAndGlory.Battle b,Shot s){
        b.shots.remove(s);if(s.visual==null)return;
        Vec3d step=s.aimPoint.subtract(curve(s.from,s.aimPoint,s.peak,s.ticks,Math.max(0,s.ticks-1)));
        double ground=b.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,MathHelper.floor(s.aimPoint.x),MathHelper.floor(s.aimPoint.z));
        s.visual.setVelocity(step);s.visual.velocityModified=true;
        b.landed.add(new Landed(s.visual,b.time+BattleMath.flyOnTicks(s.aimPoint.y,ground,step.y,FLY_ON_MAX)));
    }
    static int rangeBin(double range){return Math.max(0,Math.min(RANGE_BINS-1,(int)(range/8)));}
    static void count(long[][] counter,Shot s){if(s.shooter!=null&&Tactics.archer(s.shooter)&&s.squad.team>=0&&s.squad.team<2)counter[s.squad.team][rangeBin(s.range)]++;}
    /** A landed shot's arrow, held still where it landed until battle time until, then discarded (N7), so the client draws it arriving. */
    public record Landed(Entity visual,long until){}
    /**
     * How many ticks a landed shot's arrow stays where it landed (N7). The tracker sends a tick's place at the start of the next tick, so an arrow
     * discarded on its landing tick vanished on the client one move short of its target. The first extra tick carries the landing place (and a
     * still velocity) to the client; the second makes sure the client ticks once at that place before the arrow's removal reaches it, even when
     * the client's tick falls late against the server's. Two ticks, a tenth of a second, of an arrow standing in its target.
     */
    static final int LINGER_TICKS=2;
    /** How fast, in blocks a tick, a shot's arrow is spawned moving along its launch tangent before its first step: enough for the velocity packet's
     *  1/8000 steps to give its heading within a quarter of a degree, little enough that the client's one tick of it is a twentieth of a block. */
    static final double SPAWN_NUDGE=.05;
    /**
     * Whether e is a battle shot's arrow or trident (N8). On the server, Projectiles' command tag. The client never sees command tags, so there it is
     * the three synced flags fire sets together: no-clip, no gravity and silent. Vanilla never makes an arrow no-clip (the flag is not saved); only a
     * Loyalty trident returning to its owner is, and it keeps its gravity unless a command took it away.
     */
    public static boolean isShot(Entity e){return e.getWorld().isClient?e instanceof PersistentProjectileEntity a&&a.isNoClip()&&a.hasNoGravity()&&a.isSilent():e.getCommandTags().contains(TAG);}
    private Projectiles(){}

    static Vec3d aim(LivingEntity target){return new Vec3d(target.getX(),target.getBodyY(.5),target.getZ());}
    /** How the shooter's weapon scales a volley's arc (BattleMath.arcScale): a bow 1, a crossbow crossbowArcScale (the Ballista crew's too, R24), a thrown trident thrownArcScale; the Mortar's crew none. */
    static double arcScale(GravesAndGlory.Unit u){return BattleMath.arcScale(u.spec.family(),C.crossbowArcScale(),C.thrownArcScale());}
    /**
     * Overhead cover (M2): where along the straight line the columns are read (0.2, 0.35, 0.5, 0.65 and 0.8 of the way, N5), how far under the first
     * solid block over the line the arc keeps, and the least headroom (a column's clearance less that margin) worth arcing under: a shot that cover
     * lowers flies flat if any column leaves it less (R-M2). Code-held tuning numbers; see docs/PARKED.md.
     */
    static final double[] COVER_AT={.2,.35,.5,.65,.8};
    static final double COVER_MARGIN=.5,COVER_MIN_ROOM=1;
    /** Where a shot from from to to, peaking at peak over ticks of flight, stands k ticks after it was fired (R26): the straight line's point lifted by the arc's height there. */
    static Vec3d curve(Vec3d from,Vec3d to,double peak,int ticks,long k){double t=k/(double)Math.max(1,ticks);return from.lerp(to,Math.min(1,t)).add(0,BattleMath.arcHeight(t,peak),0);}
    /** The peak a shot from from to to may take under what stands over its line (BattleMath.arcCoverPeak): peak in open sky. */
    static double coverPeak(World w,Vec3d from,Vec3d to,double peak){
        if(peak<=0)return peak;
        double[] clears=new double[COVER_AT.length];
        for(int i=0;i<COVER_AT.length;i++)clears[i]=headroom(w,from.lerp(to,COVER_AT[i]),BattleMath.arcHeight(COVER_AT[i],peak)+COVER_MARGIN);
        return BattleMath.arcCoverPeak(peak,COVER_AT,clears,COVER_MARGIN,COVER_MIN_ROOM);
    }
    /**
     * How far the underside of the first solid block over p stands above it, read up to reach over p (0 if p is inside a block); infinite if none is
     * that low, and infinite for a column whose chunk is not loaded (N5): the read never loads a chunk, and an unread column caps nothing.
     */
    static double headroom(World w,Vec3d p,double reach){
        int x=MathHelper.floor(p.x),z=MathHelper.floor(p.z);
        if(!w.isChunkLoaded(ChunkSectionPos.getSectionCoord(x),ChunkSectionPos.getSectionCoord(z)))return Double.POSITIVE_INFINITY;
        for(int y=MathHelper.floor(p.y);y<=MathHelper.floor(p.y+reach);y++){
            BlockPos at=new BlockPos(x,y,z);VoxelShape shape=w.getBlockState(at).getCollisionShape(w,at);
            if(shape.isEmpty()||y+shape.getMax(Direction.Axis.Y)<=p.y)continue;
            return Math.max(0,y+shape.getMin(Direction.Axis.Y)-p.y);
        }
        return Double.POSITIVE_INFINITY;
    }

    /** A shot's flight: its peak over the straight line (0: straight) and its ticks in the air. */
    record Arc(double peak,int travel){}
    /**
     * A volley's arc (animation rework section 5), the one fire flies and aimPitch aims along: an arrow or a trident rises volleyPeakPerBlock a block of
     * range over its straight line, up to volleyPeakMax, scaled by the weapon, and kept under any roof over the line (M2). Bolts, mends and the Mortar's
     * shell fly straight by design (no arc, as arcScale 0 would give): the Mortar's straight line is its real flight, not a stand-in for a lob. Flight
     * time is the straight shot's times arcTravelScale (1: unchanged).
     */
    static Arc arc(GravesAndGlory.Battle b,GravesAndGlory.Unit u,Vec3d from,Vec3d to,boolean arrow){
        double dist=from.distanceTo(to);
        double peak=arrow?coverPeak(b.world,from,to,BattleMath.arcPeak(dist,C.volleyPeakPerBlock(),C.volleyPeakMax(),arcScale(u))):0;
        int travel=BattleMath.arcTravelTicks(BattleMath.travelTicks(dist,arrow?C.arrowSpeed():C.boltSpeed(),C.projectileMinTicks(),C.projectileMaxTicks()),peak,C.arcTravelScale());
        return new Arc(peak,travel);
    }
    /**
     * The pitch (BattleMath.aimPitch: degrees, positive down as MotionInput's and Minecraft's, so a lofted volley is negative) a ranged troop would loose
     * its shot at target along now, as fire would: from its eyes to the target's body, on arc's flight for its weapon. For a straight shot (a bolt, a mend,
     * the Mortar's shell) that is the line itself, which is its real flight. stance_v2's aimPitch while it aims (Tactics.show).
     */
    static float aimPitch(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity target){
        Vec3d from=u.mob.getEyePos(),to=aim(target);String kind=Abilities.visual(u);
        Arc a=arc(b,u,from,to,kind.equals("arrow")||kind.equals("trident"));
        return (float)BattleMath.aimPitch(Math.hypot(to.x-from.x,to.z-from.z),to.y-from.y,a.peak(),a.travel());
    }
    /**
     * attack_v1 for a shot (Task 13 carry B9): the shooter's family's variant (for a bow, bow_volley for a lofted arc and bow_direct for a flat one; any other
     * family its first row), begun its impactTicks before now, so the clip's impact, the loose, lands on the tick the shot leaves. Kept as the troop's
     * newest swing (catchVariant, swingTick) for a late joiner's catch-up. Its rolls, lastVariant and swungAt are left alone. Nothing for a family with no rows.
     */
    static void loose(GravesAndGlory.Unit u,double peak){
        var rows=Tactics.variants(u.spec.family());if(rows.isEmpty())return;
        int variant=pinned(u,rows,shotVariant(u,rows,peak),u.mob.getWorld().getTime(),true);u.drawVariant=-1;u.drawStart=-1;
        u.catchVariant=variant;u.swingTick=Math.max(0,u.mob.getWorld().getTime()-rows.get(variant).impactTicks()-hold(u));
        MotionEvents.sendAttack(u,variant,u.swingTick);
    }
    /**
     * Kort Q70 (2026-11-02): a bow troop's shot leaves MotionMath.Seed.loose ticks (0 to 3, from its UUID) after its beat, so a volley line is
     * not lockstep; of those ticks its draw holds at full Seed.hold longer (MotionStack.Track.hold) and starts the rest later. Damage, speed,
     * arrow count and the cooldown's cadence are unchanged. Other troops: 0.
     */
    static int looseTicks(GravesAndGlory.Unit u){return Tactics.drawsAhead(u)?seed(u.mob.getUuid()).loose():0;}
    /** The extra ticks a bow troop's shot holds its full draw (Seed.hold), so its clip starts that much before impactTicks ahead of the loose. Others: 0. */
    static int hold(GravesAndGlory.Unit u){return Tactics.drawsAhead(u)?seed(u.mob.getUuid()).hold():0;}
    /**
     * The variant a shot plays: the one its announce chose (Unit.drawVariant) when the shot is that announced one (the same start tick), else fresh. An
     * announce repeats each beat for a bow and the loose then resends it; a target moving, or the arc fire works out again at the loose, can turn the arc between
     * volley and direct, and a new variant at the same start would be a second clip. Only the clip is pinned: the arrow still flies its own arc.
     * at is the loose's world tick (loosing true) or the announced beat's (loosing false: shotStart adds the troop's loose seed).
     */
    static int pinned(GravesAndGlory.Unit u,List<Spec.AttackVariantsRow> rows,int fresh,long at,boolean loosing){
        int v=u.drawVariant;if(v<0||v>=rows.size()||u.drawStart<0)return fresh;
        int impact=rows.get(v).impactTicks();
        long start=loosing?at-impact-hold(u):Tactics.drawsAhead(u)?shotStart(u.mob.getUuid(),at,impact):at-impact;
        return start==u.drawStart?v:fresh;
    }
    static MotionMath.Seed seed(java.util.UUID id){return MotionMath.seed(id.getMostSignificantBits(),id.getLeastSignificantBits());}
    /**
     * The world tick a bow troop's shot on the beat at beat tick starts its clip (attack_v1's startTick): it leaves loose ticks after the beat and
     * its clip reaches its impact impactTicks plus hold ticks after it starts. drawAhead sends it and loose stamps the same tick (the test harness's
     * volley line uses it as the server would).
     */
    public static long shotStart(java.util.UUID id,long beat,int impactTicks){var s=seed(id);return beat+s.loose()-impactTicks-s.hold();}
    /** The row of a shot's family loose sends for an arc of this peak: <family>_volley for a lofted one, <family>_direct for a flat one, else the first. */
    static int shotVariant(GravesAndGlory.Unit u,List<Spec.AttackVariantsRow> rows,double peak){
        int variant=0;String want=u.spec.family()+(peak>0?"_volley":"_direct");
        for(int i=0;i<rows.size();i++)if(rows.get(i).id().equals(want))variant=i;
        return variant;
    }
    /**
     * Bow-draw fix Plan B (R110), cosmetic only, also for a crossbow (Tactics.announcesAhead): before a troop's shot (Tactics.tick calls it on each beat from
     * Tactics.drawLead ticks ahead, 30 for a bow and 10 for the rest, until the shot's beat; ahead is the ticks from now to that beat, Tactics.shotAhead), the attack_v1 loose will send, sent now. loose stamps its swing impactTicks before the tick the shot
     * leaves; ahead ticks from now that is now + ahead - impactTicks, the same startTick and the same variant (pinned: Unit.drawVariant), so the
     * client starts the clip now (the draw plays before the loose) and drops loose's copy as the event it already has (MotionEngine.events).
     * Nothing else changes: no projectile, no cooldown, no catchVariant or swingTick (loose still sets those when the shot leaves). If the
     * shot never leaves (its target died, the troop was stunned), the draw plays with no arrow. Returns how many players it was sent to.
     */
    static int drawAhead(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity target,int ahead){
        var rows=Tactics.variants(u.spec.family());if(rows.isEmpty())return 0;
        Vec3d from=u.mob.getEyePos(),to=aim(target);String kind=Abilities.visual(u);
        long beat=u.mob.getWorld().getTime()+ahead;
        int variant=pinned(u,rows,shotVariant(u,rows,arc(b,u,from,to,kind.equals("arrow")||kind.equals("trident")).peak()),beat,false); // a repeat keeps the first announce's variant
        int impact=rows.get(variant).impactTicks();
        long start=Tactics.drawsAhead(u)?shotStart(u.mob.getUuid(),beat,impact):beat-impact;if(start<0)return 0; // a crossbow has no loose or hold seed: loose stamps beat - impact
        u.drawVariant=variant;u.drawStart=start;
        return MotionEvents.sendAttack(u,variant,start);
    }
    public static void fire(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity target,String kind,float amount){
        Vec3d from=u.mob.getEyePos(),to=aim(target);double range=from.distanceTo(to);
        Tactics.stamp(b,u,target); // archer fire: the target's group is under fire (Squad.shotAt)
        boolean arrow=kind.equals("arrow")||kind.equals("trident");
        Arc arc=arc(b,u,from,to,arrow); // the volley's arc, or a straight line for a bolt, a mend or the Mortar's shell
        Vec3d aimPoint=null;
        if(arrow&&Tactics.archer(u)){aimPoint=scatter(u,target,from,to,arc.travel());to=aimPoint;arc=arc(b,u,from,to,true);} // an archer's shot flies to where it was loosed at, and may miss (Kort 2026-11-11)
        double peak=arc.peak();int travel=arc.travel();
        loose(u,peak);
        long lands=b.time+travel;
        Entity visual=null;Vec3d step=null;
        if(arrow){
            // No-clip arrows never collide, so they carry no damage of their own: the queue applies it on landing.
            PersistentProjectileEntity a=kind.equals("trident")?new TridentEntity(b.world,u.mob,new ItemStack(Items.TRIDENT)):new ArrowEntity(b.world,from.x,from.y,from.z);
            a.setPosition(from);
            // Silent as well: with no-clip and no gravity, the mark of a battle shot on the client (isShot).
            a.setNoGravity(true);a.setNoClip(true);a.setSilent(true);a.pickupType=PersistentProjectileEntity.PickupPermission.DISALLOWED;a.addCommandTag(TAG);
            // Nose first from the start (R25): along the curve's launch tangent, which the first two ticks' steps give (1.5 v1 - 0.5 v2 on a parabola).
            Vec3d c1=curve(from,to,peak,travel,1);step=c1.subtract(from);
            Vec3d nose=step.multiply(1.5).subtract(curve(from,to,peak,travel,2).subtract(c1).multiply(.5));
            a.setYaw((float)BattleMath.arrowYaw(nose.x,nose.z));a.setPitch((float)BattleMath.arrowPitch(nose.x,nose.y,nose.z));a.prevYaw=a.getYaw();a.prevPitch=a.getPitch();
            // Spawned barely moving, along that tangent: the client turns a new arrow to its first velocity, so a still one would face south, level.
            a.setVelocity(nose.normalize().multiply(SPAWN_NUDGE));
            visual=a;
        }
        Shot shot=new Shot(u,u.squad,target,kind,amount,arrow?from:castFrom(u),b.time,lands,visual,null,peak,travel,new Vec3d[]{to},range,aimPoint);
        b.shots.add(shot);count(ARROWS_FIRED,shot);
        // Queued first, then spawned: the load handler also runs for a new spawn, and discards a tagged arrow that no queued shot owns.
        if(visual!=null){
            // Spawned barely moving, then given the first tick's step along the curve (R26) in a velocity update. The spawn goes out at once with the
            // velocity it has, while the tracker's first update, next tick, sends the place, still the launch point: an arrow spawned with the whole step
            // was drawn a step on, pulled back to the launch point and drawn along that step again, a tick behind the server to the end (N7).
            b.world.spawnEntity(visual);
            visual.setVelocity(step);visual.velocityModified=true;
        }
    }
    /**
     * Caster v2 tip origin (Kort, 2026-11), cosmetic only: where a caster's drawn shot (a bolt, a mend) starts, the tip of its prop on its release frame.
     * The troop's cast_tip resource (art/anim/caster/build_caster_anims.py tip_json, copied by export.py) holds that tip in its body frame (blocks: x to
     * its left, y up from its feet, z forward); here it is turned by the body yaw and set on the feet. A troop with no cast_tip, or drawn on the old model
     * (GeoTroops.castTip: no prop there), keeps its eyes. Only the
     * visual's start moves: the flight's ticks, the arc, the damage, the targeting and the cooldowns are still worked out from the eyes, as before.
     */
    static Vec3d castFrom(GravesAndGlory.Unit u){
        double[] o=GeoTroops.castTip(u.spec.id())?TIPS.computeIfAbsent(u.spec.id(),Projectiles::castTip):NO_TIP; // the old model holds no prop there: the eyes
        if(o.length<3)return u.mob.getEyePos();
        double yaw=Math.toRadians(u.mob.getBodyYaw()),s=Math.sin(yaw),c=Math.cos(yaw);
        Vec3d from=u.mob.getPos().add(o[0]*c-o[2]*s,o[1],o[0]*s+o[2]*c); // left (cos, 0, sin), forward (-sin, 0, cos)
        if(TIP_LOGS.getAndIncrement()<4)TIP_LOG.info("cast tip {}: from {} eye {} body yaw {}",u.spec.id(),from,u.mob.getEyePos(),u.mob.getBodyYaw());
        return from;
    }
    private static final double[] NO_TIP={};
    private static final java.util.Map<String,double[]> TIPS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger TIP_LOGS=new java.util.concurrent.atomic.AtomicInteger();
    private static final org.slf4j.Logger TIP_LOG=org.slf4j.LoggerFactory.getLogger("gravesandglory/cast-tip");
    /** A troop's cast tip offset (blocks, body frame) from data/gravesandglory/cast_tip/<troop>.json, or none. */
    static double[] castTip(String troop){
        try(var in=Projectiles.class.getResourceAsStream("/data/gravesandglory/cast_tip/"+troop+".json")){
            if(in==null)return new double[0];
            var a=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("offset");
            return new double[]{a.get(0).getAsDouble(),a.get(1).getAsDouble(),a.get(2).getAsDouble()};
        }catch(Exception e){return new double[0];}
    }
    /** The blight bolt flies to the spell's fixed centre and arrives on the tick the spell bursts. */
    public static void bolt(GravesAndGlory.Battle b,GravesAndGlory.Unit u,Vec3d center){
        long lands=Math.max(b.time+C.projectileMinTicks(),u.windupUntil);
        b.shots.add(new Shot(u,u.squad,null,"bolt",0,castFrom(u),b.time,lands,null,center.add(0,.5,0)));
    }

    public static void tick(GravesAndGlory.Battle b){
        b.landed.removeIf(l->{if(b.time<l.until())return false;l.visual().discard();return true;});
        if(b.shots.isEmpty())return;
        for(Shot s:new ArrayList<>(b.shots)){
            // A fallen commander becomes a spectator at base: the shot is lost, it does not chase them home.
            boolean live=s.target==null||s.target.isAlive()&&!s.target.isRemoved()&&s.target.getWorld()==b.world&&!(s.target instanceof PlayerEntity p&&p.isSpectator());
            if(s.aimPoint!=null){ // an archer's shot: on its fixed curve to its aim point, judged where it comes down (strike); a miss flies on into the ground
                if(b.time<s.lands){if(s.visual!=null){s.visual.setVelocity(curve(s.from,s.aimPoint,s.peak,s.ticks,b.time-s.fired+1).subtract(s.visual.getPos()));s.visual.velocityModified=true;}continue;}
                Vec3d struck=live?strike(s):null;
                if(struck==null){count(live?ARROWS_MISSED:ARROWS_LOST,s);flyOn(b,s);continue;}
                settle(b,s);land(b,s,struck);continue;
            }
            if(!live){count(ARROWS_LOST,s);drop(b,s);continue;}
            Vec3d to=s.target==null?s.at:aim(s.target);
            if(b.time>=s.lands){settle(b,s);land(b,s,to);continue;}
            if(s.visual!=null){
                // Steer onto the curve (R26): next tick the arrow stands exactly where the curve to the target puts it, so its peak holds at every range.
                // The target is led by its last tick's motion (N6), so one walking on meets the arrow on its landing tick. The hit is Tactics', decided
                // at fire time; it lands on the target wherever the arrow is.  Peak 0 is a straight shot.
                Vec3d last=s.aimed[0];s.aimed[0]=to;
                Vec3d v=curve(s.from,to.add(to.subtract(last)),s.peak,s.ticks,b.time-s.fired+1).subtract(s.visual.getPos());
                s.visual.setVelocity(v);s.visual.velocityModified=true;
            }else{
                double t=Math.min(1,(b.time-s.fired)/(double)Math.max(1,s.lands-s.fired));Vec3d p=s.from.lerp(to,t);
                // the glimpur ebb: a bolt sputters (skips the dark beats) and greys and shrinks with its caster's flicker
                if(BattleEffects.ebbSkips(b,s.shooter))continue;
                float fade=BattleEffects.ebbFade(b,s.shooter);int rgb=MarchMath.Ebb.ash(b.side(s.squad.team).color(),fade);
                b.world.spawnParticles(new DustParticleEffect(new Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f),1.4f*(1-.55f*fade)),p.x,p.y,p.z,fade>0?1:2,.04,.04,.04,0);
                if(fade<.45f)b.world.spawnParticles(s.squad.team==1?ParticleTypes.WITCH:ParticleTypes.ENCHANT,p.x,p.y,p.z,1,.05,.05,.05,0);
                else b.world.spawnParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,1,.03,.03,.03,0); // a guttering bolt trails spent smoke
            }
        }
    }
    /** Whether entity is the arrow of a shot still in the queue. The load handler discards every other tagged arrow: one saved in flight and loaded again belongs to no shot of this battle. */
    public static boolean owns(GravesAndGlory.Battle b,Entity entity){
        if(b!=null){for(Shot s:b.shots)if(s.visual==entity)return true;for(Landed l:b.landed)if(l.visual()==entity)return true;}
        return false;
    }
    /** A shot lost in flight (its target gone): out of the queue, its arrow gone at once. */
    static void drop(GravesAndGlory.Battle b,Shot s){b.shots.remove(s);if(s.visual!=null)s.visual.discard();}
    /** A shot on its landing tick: out of the queue; its arrow, which stands where it landed, is stilled there for LINGER_TICKS (N7). */
    static void settle(GravesAndGlory.Battle b,Shot s){
        b.shots.remove(s);
        if(s.visual==null)return;
        // The next tick's update sends its place (velocityDirty) and its still velocity (velocityModified).
        s.visual.setVelocity(Vec3d.ZERO);s.visual.velocityDirty=true;s.visual.velocityModified=true;
        b.landed.add(new Landed(s.visual,b.time+LINGER_TICKS));
    }
    static void land(GravesAndGlory.Battle b,Shot s,Vec3d at){
        switch(s.kind){
            case "arrow","trident"->{
                // Combat.resolve credits the shooter's army (lastHitBy), applies the counters and hurts the target with the troop damage type.
                Combat.resolve(b,s.shooter,s.target,s.amount,"arrow",false,at.y); // the height it struck at gives hit_v1's zone
                count(ARROWS_HIT,s);
                BattleEffects.flash(b,at,s.squad.team);
            }
            case "bolt"->{
                // A bolt with a target and an amount is a damaging magic shot; the blight cast's bolt has neither and stays a visual that arrives with the burst.
                if(s.target!=null&&s.amount>0)Combat.resolve(b,s.shooter,s.target,s.amount,"bolt",false,at.y);
                BattleEffects.flash(b,at,s.squad.team,BattleEffects.ebbFade(b,s.shooter));
            }
            case "mend"->{s.target.heal(s.amount);b.world.spawnParticles(ParticleTypes.HEART,s.target.getX(),s.target.getY()+2,s.target.getZ(),3,.3,.2,.3,0);}
            default->BattleEffects.flash(b,at,s.squad.team);
        }
    }
    public static void clear(GravesAndGlory.Battle b){
        for(Shot s:b.shots)if(s.visual!=null)s.visual.discard();b.shots.clear();
        for(Landed l:b.landed)l.visual().discard();b.landed.clear();
    }
    public static int inFlight(GravesAndGlory.Battle b){return b.shots.size();}
}
