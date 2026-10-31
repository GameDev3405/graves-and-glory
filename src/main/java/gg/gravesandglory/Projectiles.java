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

/** Delayed, visible ranged hits. Tactics still decides the hit; this queue only lands it after the shot's flight time. */
public final class Projectiles {
    public static final String TAG="gravesandglory-shot";
    static final Spec.CombatDisciplineRow C=Spec.COMBATDISCIPLINE[0];
    /** Ticks after an enemy's last blow on a unit during which a death with no attacker still credits the blow's army. lastHitAt is stamped when a blow lands (melee, or an arrow on impact), so flight time never counts: this is a blow-to-death allowance, sized as projectileMaxTicks plus 20 so a late shot plus a short fall still pays. The +20 is a code-held tuning number; see docs/PARKED.md. */
    public static final int CREDIT_WINDOW_TICKS=C.projectileMaxTicks()+20;
    /** kind: "arrow" (damage, vanilla arrow visual), "trident" (the same damage as an arrow, a flying trident for the visual: the Harpooner's), "bolt" (the blight cast, visual only; with a target and an amount, a magic shot that damages on landing), "mend" (heal).
     *  The blight cast's bolt has no target: it flies to the fixed point {@code at}; other shots follow their target and leave {@code at} null.
     *  peak is how far the shot's arc rises over its straight line mid-flight (0: a straight shot), and ticks its whole flight (lands - fired).
     *  aimed holds, in its one slot, the target's aim point when the visual was last steered (at first, when it was fired), so the steering can lead
     *  a moving target by its last tick's motion (N6). */
    public record Shot(GravesAndGlory.Unit shooter,GravesAndGlory.Squad squad,LivingEntity target,String kind,float amount,Vec3d from,long fired,long lands,Entity visual,Vec3d at,double peak,int ticks,Vec3d[] aimed){
        /** A straight shot (no arc) whose flight is lands - fired. */
        public Shot(GravesAndGlory.Unit shooter,GravesAndGlory.Squad squad,LivingEntity target,String kind,float amount,Vec3d from,long fired,long lands,Entity visual,Vec3d at){
            this(shooter,squad,target,kind,amount,from,fired,lands,visual,at,0,(int)(lands-fired),new Vec3d[1]);
        }
    }
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
        int variant=0;String want=u.spec.family()+(peak>0?"_volley":"_direct");
        for(int i=0;i<rows.size();i++)if(rows.get(i).id().equals(want))variant=i;
        u.catchVariant=variant;u.swingTick=Math.max(0,u.mob.getWorld().getTime()-rows.get(variant).impactTicks());
        MotionEvents.sendAttack(u,variant,u.swingTick);
    }
    public static void fire(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity target,String kind,float amount){
        Vec3d from=u.mob.getEyePos(),to=aim(target);
        Tactics.stamp(b,u,target); // archer fire: the target's group is under fire (Squad.shotAt)
        boolean arrow=kind.equals("arrow")||kind.equals("trident");
        Arc arc=arc(b,u,from,to,arrow); // the volley's arc, or a straight line for a bolt, a mend or the Mortar's shell
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
        b.shots.add(new Shot(u,u.squad,target,kind,amount,from,b.time,lands,visual,null,peak,travel,new Vec3d[]{to}));
        // Queued first, then spawned: the load handler also runs for a new spawn, and discards a tagged arrow that no queued shot owns.
        if(visual!=null){
            // Spawned barely moving, then given the first tick's step along the curve (R26) in a velocity update. The spawn goes out at once with the
            // velocity it has, while the tracker's first update, next tick, sends the place, still the launch point: an arrow spawned with the whole step
            // was drawn a step on, pulled back to the launch point and drawn along that step again, a tick behind the server to the end (N7).
            b.world.spawnEntity(visual);
            visual.setVelocity(step);visual.velocityModified=true;
        }
    }
    /** The blight bolt flies to the spell's fixed centre and arrives on the tick the spell bursts. */
    public static void bolt(GravesAndGlory.Battle b,GravesAndGlory.Unit u,Vec3d center){
        long lands=Math.max(b.time+C.projectileMinTicks(),u.windupUntil);
        b.shots.add(new Shot(u,u.squad,null,"bolt",0,u.mob.getEyePos(),b.time,lands,null,center.add(0,.5,0)));
    }

    public static void tick(GravesAndGlory.Battle b){
        b.landed.removeIf(l->{if(b.time<l.until())return false;l.visual().discard();return true;});
        if(b.shots.isEmpty())return;
        for(Shot s:new ArrayList<>(b.shots)){
            // A fallen commander becomes a spectator at base: the shot is lost, it does not chase them home.
            boolean live=s.target==null||s.target.isAlive()&&!s.target.isRemoved()&&s.target.getWorld()==b.world&&!(s.target instanceof PlayerEntity p&&p.isSpectator());
            if(!live){drop(b,s);continue;}
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
                int rgb=b.side(s.squad.team).color();
                b.world.spawnParticles(new DustParticleEffect(new Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f),1.4f),p.x,p.y,p.z,2,.04,.04,.04,0);
                b.world.spawnParticles(s.squad.team==1?ParticleTypes.WITCH:ParticleTypes.ENCHANT,p.x,p.y,p.z,1,.05,.05,.05,0);
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
                BattleEffects.flash(b,at,s.squad.team);
            }
            case "bolt"->{
                // A bolt with a target and an amount is a damaging magic shot; the blight cast's bolt has neither and stays a visual that arrives with the burst.
                if(s.target!=null&&s.amount>0)Combat.resolve(b,s.shooter,s.target,s.amount,"bolt",false,at.y);
                BattleEffects.flash(b,at,s.squad.team);
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
