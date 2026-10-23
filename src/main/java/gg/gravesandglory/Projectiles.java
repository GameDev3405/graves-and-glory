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
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import java.util.ArrayList;

/** Delayed, visible ranged hits. Tactics still decides the hit; this queue only lands it after the shot's flight time. */
public final class Projectiles {
    public static final String TAG="gravesandglory-shot";
    static final Spec.CombatDisciplineRow C=Spec.COMBATDISCIPLINE[0];
    /** Ticks after an enemy's last blow on a unit during which a death with no attacker still credits the blow's army. lastHitAt is stamped when a blow lands (melee, or an arrow on impact), so flight time never counts: this is a blow-to-death allowance, sized as projectileMaxTicks plus 20 so a late shot plus a short fall still pays. The +20 is a code-held tuning number; see docs/PARKED.md. */
    public static final int CREDIT_WINDOW_TICKS=C.projectileMaxTicks()+20;
    /** kind: "arrow" (damage, vanilla arrow visual), "trident" (the same damage as an arrow, a flying trident for the visual: the Harpooner's), "bolt" (the blight cast, visual only; with a target and an amount, a magic shot that damages on landing), "mend" (heal).
     *  The blight cast's bolt has no target: it flies to the fixed point {@code at}; other shots follow their target and leave {@code at} null. */
    public record Shot(GravesAndGlory.Unit shooter,GravesAndGlory.Squad squad,LivingEntity target,String kind,float amount,Vec3d from,long fired,long lands,Entity visual,Vec3d at){}
    private Projectiles(){}

    static Vec3d aim(LivingEntity target){return new Vec3d(target.getX(),target.getBodyY(.5),target.getZ());}

    public static void fire(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity target,String kind,float amount){
        Vec3d from=u.mob.getEyePos(),to=aim(target);
        boolean arrow=kind.equals("arrow")||kind.equals("trident");
        int travel=BattleMath.travelTicks(from.distanceTo(to),arrow?C.arrowSpeed():C.boltSpeed(),C.projectileMinTicks(),C.projectileMaxTicks());
        long lands=b.time+travel;
        Entity visual=null;
        if(arrow){
            // No-clip arrows never collide, so they carry no damage of their own: the queue applies it on landing.
            PersistentProjectileEntity a=kind.equals("trident")?new TridentEntity(b.world,u.mob,new ItemStack(Items.TRIDENT)):new ArrowEntity(b.world,from.x,from.y,from.z);
            a.setPosition(from);
            a.setNoGravity(true);a.setNoClip(true);a.pickupType=PersistentProjectileEntity.PickupPermission.DISALLOWED;a.addCommandTag(TAG);
            Vec3d v=to.subtract(from).multiply(1.0/travel);a.setVelocity(v.x,v.y,v.z,(float)v.length(),0);
            visual=a;
        }
        b.shots.add(new Shot(u,u.squad,target,kind,amount,from,b.time,lands,visual,null));
        // Queued first, then spawned: the load handler also runs for a new spawn, and discards a tagged arrow that no queued shot owns.
        if(visual!=null)b.world.spawnEntity(visual);
    }
    /** The blight bolt flies to the spell's fixed centre and arrives on the tick the spell bursts. */
    public static void bolt(GravesAndGlory.Battle b,GravesAndGlory.Unit u,Vec3d center){
        long lands=Math.max(b.time+C.projectileMinTicks(),u.windupUntil);
        b.shots.add(new Shot(u,u.squad,null,"bolt",0,u.mob.getEyePos(),b.time,lands,null,center.add(0,.5,0)));
    }

    public static void tick(GravesAndGlory.Battle b){
        if(b.shots.isEmpty())return;
        for(Shot s:new ArrayList<>(b.shots)){
            // A fallen commander becomes a spectator at base: the shot is lost, it does not chase them home.
            boolean live=s.target==null||s.target.isAlive()&&!s.target.isRemoved()&&s.target.getWorld()==b.world&&!(s.target instanceof PlayerEntity p&&p.isSpectator());
            if(!live){drop(b,s);continue;}
            Vec3d to=s.target==null?s.at:aim(s.target);
            if(b.time>=s.lands){drop(b,s);land(b,s,to);continue;}
            if(s.visual!=null){
                // Steer so the arrow meets a moving target exactly on its landing tick.
                Vec3d v=to.subtract(s.visual.getPos()).multiply(1.0/Math.max(1,s.lands-b.time));
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
    public static boolean owns(GravesAndGlory.Battle b,Entity entity){if(b!=null)for(Shot s:b.shots)if(s.visual==entity)return true;return false;}
    static void drop(GravesAndGlory.Battle b,Shot s){b.shots.remove(s);if(s.visual!=null)s.visual.discard();}
    static void land(GravesAndGlory.Battle b,Shot s,Vec3d at){
        switch(s.kind){
            case "arrow","trident"->{
                // Combat.resolve credits the shooter's army (lastHitBy), applies the counters and hurts the target with the troop damage type.
                Combat.resolve(b,s.shooter,s.target,s.amount,"arrow",false);
                BattleEffects.flash(b,at,s.squad.team);
            }
            case "bolt"->{
                // A bolt with a target and an amount is a damaging magic shot; the blight cast's bolt has neither and stays a visual that arrives with the burst.
                if(s.target!=null&&s.amount>0)Combat.resolve(b,s.shooter,s.target,s.amount,"bolt",false);
                BattleEffects.flash(b,at,s.squad.team);
            }
            case "mend"->{s.target.heal(s.amount);b.world.spawnParticles(ParticleTypes.HEART,s.target.getX(),s.target.getY()+2,s.target.getZ(),3,.3,.2,.3,0);}
            default->BattleEffects.flash(b,at,s.squad.team);
        }
    }
    public static void clear(GravesAndGlory.Battle b){for(Shot s:b.shots)if(s.visual!=null)s.visual.discard();b.shots.clear();}
    public static int inFlight(GravesAndGlory.Battle b){return b.shots.size();}
}
