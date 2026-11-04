package gg.gravesandglory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Short, spatial effects use the player's Minecraft assets and original cues. */
public final class BattleEffects {
    /** Harness counters: how many impact flashes have played, the RGB of the latest, and how many souls have risen (soul). */
    public static int flashes,lastFlashColor,souls;
    /** Harness counter: how many golem and ravager swings have sent entity status 4 (Abilities.animate). */
    public static int attackStatuses;
    /** An impact puff in the hitting team's colour, so a crowded melee shows who is landing blows. */
    public static void flash(GravesAndGlory.Battle b,Vec3d at,int team){
        int rgb=b.side(team).color();
        b.world.spawnParticles(new DustParticleEffect(new Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f),1.4f),at.x,at.y,at.z,Spec.COMBATDISCIPLINE[0].impactParticles(),.25,.25,.25,0);
        b.world.spawnParticles(ParticleTypes.CRIT,at.x,at.y,at.z,3,.2,.2,.2,.02);
        flashes++;lastFlashColor=rgb;
    }
    /** How many flames one rise lights (a presentation number). */
    static final int PLUME=18;
    /** A troop rises (Rise Again, Grave Bloom): a column of soul fire that winds up round it, clear of its body so it shows from any side, and the sound of a soul escaping. */
    public static void soul(GravesAndGlory.Battle b,Vec3d at){
        for(int i=0;i<PLUME;i++){double turn=i*2.399963,height=at.y+.1+1.9*i/PLUME; // the golden angle spreads the flames round the ring
            b.world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x+Math.cos(turn)*.6,height,at.z+Math.sin(turn)*.6,0,0,1,0,.08);}
        b.world.playSound(null,at.x,at.y,at.z,SoundEvents.PARTICLE_SOUL_ESCAPE,SoundCategory.HOSTILE,1f,.9f);
        souls++;
    }
    /** One command feedback sound a player heard: who, which kind (charge, retreat or rally) and the battle time. */
    public record Heard(java.util.UUID player,String kind,long time){}
    /** Every feedback sound played since the round started, in order (Battle.startRound clears it; the harness reads it). */
    public static final java.util.concurrent.CopyOnWriteArrayList<Heard> heard=new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Per player, when each kind last played (the throttle); Battle.startRound clears it. */
    static final java.util.Map<java.util.UUID,java.util.Map<String,Long>> lastFeedback=new java.util.concurrent.ConcurrentHashMap<>();
    /** A new round: the sounds of the last one and the throttle go. */
    static void startRound(){heard.clear();lastFeedback.clear();}
    /**
     * A command's feedback sound, to the commanding player alone (nobody else hears it, and a bot's command plays nothing): the kind's cue (charge for
     * Charge: horn_charge; retreat for Retreat: horn_rout; rally for Muster: horn_rally) at combatDiscipline.hornVolume, once per kind per player per
     * hornCooldownSeconds. A second command of the kind within the cooldown plays nothing (the design's 5.2). Returns whether it played. The call is the
     * ServerPlayerEntity overload (sound, category, volume, pitch): one PlaySoundS2CPacket to his connection; the three-argument PlayerEntity one goes
     * through the world (sendToAround), which sends to every player near him and skips him.
     */
    public static boolean feedback(GravesAndGlory.Battle b,net.minecraft.server.network.ServerPlayerEntity p,String kind){
        if(p==null)return false;
        var mine=lastFeedback.computeIfAbsent(p.getUuid(),k->new java.util.concurrent.ConcurrentHashMap<>());Long last=mine.get(kind);
        if(last!=null&&b.time>=last&&b.time-last<Spec.COMBATDISCIPLINE[0].hornCooldownSeconds()*20L)return false;
        var sound=net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory",kind.equals("retreat")?"horn_rout":"horn_"+kind));if(sound==null)return false;
        mine.put(kind,b.time);
        p.playSound(sound,SoundCategory.PLAYERS,(float)Spec.COMBATDISCIPLINE[0].hornVolume(),1f);
        heard.add(new Heard(p.getUuid(),kind,b.time));
        return true;
    }
    public static void contact(LivingEntity victim,net.minecraft.entity.damage.DamageSource source,float amount){
        if(!(source.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity player))return;
        var b=GravesAndGlory.battle;if(b==null||!b.running||player.getWorld()!=b.world)return;
        var own=b.squads.get(player.getUuid());if(own==null)return;
        for(var unit:b.units)if(unit.mob==victim&&unit.squad.team!=own.team){
            if(net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,GravesAndGlory.IMPACT)){
                var packet=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();packet.writeFloat(Math.min(1,amount/8f));net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,GravesAndGlory.IMPACT,packet);
            }break;
        }
    }
    static void strike(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity enemy){
        if(!u.spec.ranged()){
            Vec3d at=u.mob.getPos().add(0,1,0),dir=enemy.getPos().subtract(u.mob.getPos()).normalize();
            b.world.spawnParticles(ParticleTypes.SWEEP_ATTACK,at.x+dir.x,at.y,at.z+dir.z,1,0,0,0,0);
            flash(b,new Vec3d(enemy.getX(),enemy.getBodyY(.6),enemy.getZ()),u.squad.team);
            Audio.swing(b,u); // a blade's swish (AudioCues.SWORD_SWING); the landed blow has its own sound in Combat.resolve
        }else if(u.spec.magic()){
            b.world.spawnParticles(u.squad.team==0?ParticleTypes.ENCHANT:ParticleTypes.SOUL,u.mob.getX(),u.mob.getY()+1.3,u.mob.getZ(),8,.4,.4,.4,.03);
        }
    }
}
