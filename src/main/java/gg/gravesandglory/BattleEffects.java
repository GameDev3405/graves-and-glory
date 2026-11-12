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
    /** Harness counter: how many vanilla sweeps melee blows on the old model have shown (strike). */
    public static int sweeps;
    /** An impact puff in the hitting team's colour, so a crowded melee shows who is landing blows. */
    public static void flash(GravesAndGlory.Battle b,Vec3d at,int team){flash(b,at,team,0f);}
    /** The impact, faded by the shooter's flicker (0: full): a guttering caster's bolt lands as a small grey puff with no sparks. */
    static void flash(GravesAndGlory.Battle b,Vec3d at,int team,float fade){
        int rgb=MarchMath.Ebb.ash(b.side(team).color(),fade);int n=Math.max(1,Math.round(Spec.COMBATDISCIPLINE[0].impactParticles()*(1-.7f*fade)));
        b.world.spawnParticles(new DustParticleEffect(new Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f),1.4f*(1-.5f*fade)),at.x,at.y,at.z,n,.25,.25,.25,0);
        if(fade<.45f)b.world.spawnParticles(ParticleTypes.CRIT,at.x,at.y,at.z,3,.2,.2,.2,.02);
        flashes++;lastFlashColor=rgb;
    }
    /** How many flames one rise lights (a presentation number). */
    static final int PLUME=18;
    /** A troop rises (Rise Again, Grave Bloom): a column of soul fire that winds up round it, clear of its body so it shows from any side, and the sound of a soul escaping. */
    public static void soul(GravesAndGlory.Battle b,Vec3d at){
        for(int i=0;i<PLUME;i++){double turn=i*2.399963,height=at.y+.1+1.9*i/PLUME; // the golden angle spreads the flames round the ring
            b.world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x+Math.cos(turn)*.6,height,at.z+Math.sin(turn)*.6,0,0,1,0,.08);}
        Audio.at(b,AudioCues.Cue.SOUL_LEAVING,at,"so"+(long)at.x+","+(long)at.z);
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
     * A command's sound, played in the world at the commander's position in the PLAYERS category, so every player in range hears it, the other side
     * included (Kort, 2026-11-04; the old rule was that the commander alone heard it). The kind's horn (charge: horn_charge; retreat: horn_rout; rally for
     * Muster: horn_rally) at combatDiscipline.hornVolume, and the troops' shout beside it: Charge also plays ORDER_CHARGE_SHOUT; the kind "hold", sent for a
     * Hold or Move order or a formation change, plays ORDER_HOLD_SHOUT alone and is not logged in {@link #heard}. Once per kind per commander per
     * hornCooldownSeconds: a second command of the kind within the cooldown plays nothing (the design's 5.2). Only a player's command comes here, so a
     * bot's orders (Campaign.botDecide, markCharge) play nothing. Returns whether it played. The call is the world overload with no excluded player
     * (sendToAround), which sends to every player near him, the commander too.
     */
    public static boolean feedback(GravesAndGlory.Battle b,net.minecraft.server.network.ServerPlayerEntity p,String kind){
        if(p==null)return false;
        var mine=lastFeedback.computeIfAbsent(p.getUuid(),k->new java.util.concurrent.ConcurrentHashMap<>());Long last=mine.get(kind);
        if(last!=null&&b.time>=last&&b.time-last<Spec.COMBATDISCIPLINE[0].hornCooldownSeconds()*20L)return false;
        var shout=kind.equals("charge")?AudioCues.Cue.ORDER_CHARGE_SHOUT:kind.equals("hold")?AudioCues.Cue.ORDER_HOLD_SHOUT:null;
        // "hold" has no horn, only its shout; the other kinds keep their horn.
        var sound=kind.equals("hold")?null:net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory",kind.equals("retreat")?"horn_rout":"horn_"+kind));
        if(sound==null&&shout==null)return false;
        mine.put(kind,b.time);
        if(sound!=null){
            b.world.playSound(null,p.getX(),p.getY(),p.getZ(),sound,SoundCategory.PLAYERS,(float)Spec.COMBATDISCIPLINE[0].hornVolume(),1f);
            heard.add(new Heard(p.getUuid(),kind,b.time));
        }
        if(shout!=null){
            var shoutSound=net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory",shout.id));
            if(shoutSound!=null)b.world.playSound(null,p.getX(),p.getY(),p.getZ(),shoutSound,SoundCategory.PLAYERS,shout.volume,shout.pitch);
        }
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
    /** The ebb row of the running campaign battle, if it sets ebb (brief N3). */
    static boolean ebb(GravesAndGlory.Battle b){var row=b.running?b.campaign.activeBattle:null;return row!=null&&row.ebb();}
    static boolean ebbing(GravesAndGlory.Battle b,GravesAndGlory.Unit u){return MarchMath.Ebb.glow(ebb(b),u.mob.getId(),b.world.getTime())<1f;}
    /** How deep a magic troop is in its flicker now (0 for any other troop, or outside an ebb battle): its bolts, casts and impacts scale by it. */
    static float ebbFade(GravesAndGlory.Battle b,GravesAndGlory.Unit u){return u!=null&&u.spec.magic()?MarchMath.Ebb.fade(ebb(b),u.mob.getId(),b.world.getTime()):0f;}
    /** Whether a magic troop's glimpur particles skip this tick (the dark beats of its stutter). */
    static boolean ebbSkips(GravesAndGlory.Battle b,GravesAndGlory.Unit u){return u!=null&&u.spec.magic()&&MarchMath.Ebb.sputter(ebb(b),u.mob.getId(),b.world.getTime());}
    /** Each tick of an ebb battle: a caster whose flicker starts gives a faint gutter, as a candle snuffed (presentation only). */
    static void flickers(GravesAndGlory.Battle b){
        if(!ebb(b))return;long t=b.world.getTime();
        for(var u:b.units)if(u.spec.magic()&&u.mob.isAlive()&&MarchMath.Ebb.starts(true,u.mob.getId(),t))
            Audio.at(b,AudioCues.Cue.GLIMPUR_EBB,u.mob.getPos(),"ebb"+u.mob.getId()); // a faint gutter of failing magic (see tools/audio/make_glimpur_ebb.py)
    }
    static void strike(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity enemy){
        if(!u.spec.ranged()){
            // The vanilla sweep only on the old model: a GeckoLib swing carries the blow itself (Kort, 2026-11-08); the old clips do not (OLD-MODEL-GAPS.md gap 4).
            if(GeoTroops.sweeps(u.spec.id())){Vec3d at=u.mob.getPos().add(0,1,0),dir=enemy.getPos().subtract(u.mob.getPos()).normalize();b.world.spawnParticles(ParticleTypes.SWEEP_ATTACK,at.x+dir.x,at.y,at.z+dir.z,1,0,0,0,0);sweeps++;}
            flash(b,new Vec3d(enemy.getX(),enemy.getBodyY(.6),enemy.getZ()),u.squad.team);
            Audio.swing(b,u); // a blade's swish (AudioCues.SWORD_SWING); the landed blow has its own sound in Combat.resolve
        }else if(u.spec.magic()&&!ebbing(b,u)){ // mid-flicker in an ebb battle the caster's particles fade with its glow
            b.world.spawnParticles(u.squad.team==0?ParticleTypes.ENCHANT:ParticleTypes.SOUL,u.mob.getX(),u.mob.getY()+1.3,u.mob.getZ(),8,.4,.4,.4,.03);
        }else if(u.spec.magic()&&!ebbSkips(b,u)){ // guttering: the cast coughs out a little spent smoke instead of its glimpur
            b.world.spawnParticles(ParticleTypes.SMOKE,u.mob.getX(),u.mob.getY()+1.3,u.mob.getZ(),3,.25,.25,.25,.01);
        }
    }
}
