package gg.gravesandglory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.util.math.Vec3d;

/** Short, spatial effects use the player's Minecraft assets and original cues. */
public final class BattleEffects {
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
        if(u.spec.range()<3){
            Vec3d at=u.mob.getPos().add(0,1,0),dir=enemy.getPos().subtract(u.mob.getPos()).normalize();
            b.world.spawnParticles(ParticleTypes.SWEEP_ATTACK,at.x+dir.x,at.y,at.z+dir.z,1,0,0,0,0);
            b.world.spawnParticles(ParticleTypes.CRIT,enemy.getX(),enemy.getBodyY(.6),enemy.getZ(),4,.25,.25,.25,.02);
            if(u.slot%3==0){
                // Vanilla cues, rotated and randomly pitched so a line of blows does not sound identical.
                SoundEvent[] cues={SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,SoundEvents.ENTITY_PLAYER_ATTACK_CRIT};
                b.world.playSound(null,u.mob.getBlockPos(),cues[b.world.random.nextInt(cues.length)],SoundCategory.HOSTILE,.3f,.85f+b.world.random.nextFloat()*.3f);
            }
        }else if(u.spec.magic()){
            b.world.spawnParticles(u.squad.team==0?ParticleTypes.ENCHANT:ParticleTypes.SOUL,u.mob.getX(),u.mob.getY()+1.3,u.mob.getZ(),8,.4,.4,.4,.03);
        }
    }
}
