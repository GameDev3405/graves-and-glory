package gg.gravesandglory.mixin;

import gg.gravesandglory.ArmyMob;
import gg.gravesandglory.ArmyMobs;
import gg.gravesandglory.BattleMath;
import gg.gravesandglory.Spec;
import gg.gravesandglory.TroopBody;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
 * isAlive), it pushes nobody (its cramming is skipped), takes no knockback once it lies (the killing blow still throws it back: design 5), once
 * settled on the ground it no longer moves on the server, and nothing hits it (canHit is false: a crosshair, a raycast or a stray arrow passes
 * through). These guards hold on the client too, where a troop is a vanilla mob told by its name (TroopBody, cached per entity), so the
 * crosshair passes through a body and shows no nameplate.
 * Once its fall is over and it rests on dry ground, still and not burning (settled, a latch) the server skips the rest of its LivingEntity.tick:
 * it only counts its death ticks and goes on time. A burning body settles once its fire has burned out, so the fire ends as it would.
 * Battle.clearCorpses sweeps any still lying on a round start, the lobby, a map switch and server stop.
 */
@Mixin(LivingEntity.class)
abstract class CorpseMixin implements TroopBody {
    /** A troop's body: dead, and a troop (TroopBody, on either side). */
    private static boolean gravesandgloryCorpse(Object self) {
        return self instanceof MobEntity mob && mob.isDead() && ((TroopBody) mob).gravesandglory$troop();
    }

    /** TroopBody's cached answer: 0 not yet asked, 1 a troop, 2 not. */
    @Unique private byte gravesandgloryTroop;

    @Override
    public boolean gravesandglory$troop() {
        if (gravesandgloryTroop == 0) {
            Object self = this;
            boolean troop = self instanceof MobEntity mob && (self instanceof ArmyMob || mob.getCustomName() != null && ArmyMobs.named(mob.getCustomName().getString()) != null);
            gravesandgloryTroop = troop ? (byte) 1 : (byte) 2;
        }
        return gravesandgloryTroop == 1;
    }

    /** A server body whose fall is over and that rests on the ground, still (set once; it then no longer moves). */
    @Unique private boolean gravesandglorySettled;

    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    void gravesandgloryLie(CallbackInfo ci) {
        if (!((Object) this instanceof ArmyMob && (Object) this instanceof MobEntity)) return;
        gravesandgloryAge((LivingEntity) (Object) this);
        ci.cancel();
    }

    /** One death tick of a body: counts it, and on the server removes the body, with vanilla's poof, on the corpse rows' gone tick. */
    @Unique
    private static void gravesandgloryAge(LivingEntity self) {
        self.deathTime++;
        var row = Spec.COMBATDISCIPLINE[0];
        if (self.deathTime >= BattleMath.corpseGoneTick(row.corpseLieTicks(), row.corpseFadeTicks()) && !self.getWorld().isClient() && !self.isRemoved()) {
            self.getWorld().sendEntityStatus(self, (byte) 60);
            self.remove(Entity.RemovalReason.KILLED);
        }
    }

    /**
     * A settled body on the server skips the rest of its LivingEntity.tick (base tick, effects, movement, body turn): it only ages and goes on
     * time (gravesandgloryAge, as updatePostDeath would). It settles once past its fall, on the ground and still; its effects were cleared at
     * death (GravesAndGlory's AFTER_DEATH) and the fall's full ticks have already put out the glow flag.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    void gravesandgloryLieStill(CallbackInfo ci) {
        if (!gravesandgloryCorpse(this)) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient()) return;
        if (!gravesandglorySettled) {
            if (self.deathTime <= BattleMath.DEATH_FALL_TICKS || !self.isOnGround() || self.isTouchingWater() || self.getFireTicks() > 0 || self.getVelocity().horizontalLengthSquared() >= 1.0E-6) return;
            gravesandglorySettled = true;
            self.setVelocity(Vec3d.ZERO);
        }
        gravesandgloryAge(self);
        ci.cancel();
    }

    /**
     * A body that lies takes no knockback. Not the killing blow's: in LivingEntity.damage the health is already 0 when takeKnockback runs,
     * but deathTime is still 0, so the blow throws the body back (design 5) and a troop Rise Again saves keeps its knockback too.
     */
    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    void gravesandgloryNoKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (((LivingEntity) (Object) this).deathTime > 0 && gravesandgloryCorpse(this)) ci.cancel();
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
        if (!self.getWorld().isClient() && self.isOnGround() && !self.isTouchingWater() && self.getVelocity().horizontalLengthSquared() < 1.0E-6) ci.cancel();
    }
}
