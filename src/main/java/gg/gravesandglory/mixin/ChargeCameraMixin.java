package gg.gravesandglory.mixin;

import gg.gravesandglory.ChargeCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Charge's overhead camera (command UI plan Task 10). Only the render camera moves: at the head of each update the frame's mouse
 * turn is taken off the player (ChargeCamera.absorb), and at its tail the camera is placed overhead. The player entity and its rotation
 * are never changed by the overhead view, so nothing reaches the server.
 */
@Mixin(Camera.class)
abstract class ChargeCameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract Vec3d getPos();
    @Shadow public abstract float getPitch();

    @Inject(method = "update", at = @At("HEAD"))
    private void graves$chargeAbsorb(BlockView area, Entity focused, boolean third, boolean inverse, float delta, CallbackInfo ci) {
        ChargeCamera.absorb();
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void graves$chargeOverhead(BlockView area, Entity focused, boolean third, boolean inverse, float delta, CallbackInfo ci) {
        if (!ChargeCamera.active()) return;
        // lifted from the player's eye when the view was first person (vanilla has just placed a third-person camera behind it)
        double[] pose = ChargeCamera.forcedPerspective() ? ChargeCamera.pose(delta, focused.getCameraPosVec(delta), focused.getPitch(delta)) : ChargeCamera.pose(delta, getPos(), getPitch());
        setRotation((float) pose[3], (float) pose[4]);
        setPos(pose[0], pose[1], pose[2]);
        thirdPerson = true; // the commander's own body shows
    }
}
