package gg.gravesandglory.mixin;

import gg.gravesandglory.ArmyMob;
import gg.gravesandglory.BattleMath;
import gg.gravesandglory.Spec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A fallen troop lies where it fell before it goes (Task 7). Vanilla removes a dead mob on its 20th death tick (LivingEntity.updatePostDeath);
 * a troop mob (an ArmyMob that is a MobEntity: not a player, not a supply banner, not any other mob) instead keeps counting its death ticks and is
 * removed, with vanilla's poof (status 60), on BattleMath.corpseGoneTick of the corpse rows (20 + corpseLieTicks + corpseFadeTicks), read each tick
 * so a check can change them. Its loot, equipment and experience fell once in onDeath before this runs, so lying longer drops nothing more.
 * <p>While it lies it is out of the fight: it is dead (isAlive false) for every tally and target scan, vanilla does not push it (isPushable reads
 * isAlive), it pushes nobody (its cramming is skipped), once settled on the ground it no longer moves on the server, and nothing hits it (canHit is false: a crosshair, a raycast or a stray arrow passes through).
 * Battle.clearCorpses sweeps any still lying on a round start, the lobby, a map switch and server stop.
 */
@Mixin(LivingEntity.class)
abstract class CorpseMixin {
    private static boolean gravesandgloryCorpse(Object self) {
        return self instanceof ArmyMob && self instanceof MobEntity mob && mob.isDead();
    }

    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    void gravesandgloryLie(CallbackInfo ci) {
        if (!((Object) this instanceof ArmyMob && (Object) this instanceof MobEntity)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        self.deathTime++;
        var row = Spec.COMBATDISCIPLINE[0];
        if (self.deathTime >= BattleMath.corpseGoneTick(row.corpseLieTicks(), row.corpseFadeTicks()) && !self.getWorld().isClient() && !self.isRemoved()) {
            self.getWorld().sendEntityStatus(self, (byte) 60);
            self.remove(Entity.RemovalReason.KILLED);
        }
        ci.cancel();
    }

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    void gravesandgloryUnhittable(CallbackInfoReturnable<Boolean> ci) {
        if (gravesandgloryCorpse(this)) ci.setReturnValue(false);
    }

    @Inject(method = "tickCramming", at = @At("HEAD"), cancellable = true)
    void gravesandgloryNoPush(CallbackInfo ci) {
        if (gravesandgloryCorpse(this)) ci.cancel();
    }

    /** A body settled on the ground (its killing blow's slide spent) stops moving on the server: no travel, no collision tests, while it lies. */
    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    void gravesandglorySettled(CallbackInfo ci) {
        if (!gravesandgloryCorpse(this)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.getWorld().isClient() && self.isOnGround() && self.getVelocity().horizontalLengthSquared() < 1.0E-6) ci.cancel();
    }
}
