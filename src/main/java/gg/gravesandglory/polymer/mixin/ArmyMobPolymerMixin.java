package gg.gravesandglory.polymer.mixin;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import gg.gravesandglory.PlainLooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Plain clients (P3), Polymer loaded only: every troop class is a PolymerEntity, so Polymer asks PlainLooks what each viewer is told
 *  the troop is (a modpack viewer: the real type; a plain viewer: a vanilla mob by role). */
@Mixin(targets = {
    "gg.gravesandglory.GravesAndGlory$ArmySupplyBanner",
    "gg.gravesandglory.GravesAndGlory$ArmyHusk",
    "gg.gravesandglory.GravesAndGlory$ArmySkeleton",
    "gg.gravesandglory.GravesAndGlory$ArmyWither",
    "gg.gravesandglory.GravesAndGlory$ArmyVindicator",
    "gg.gravesandglory.GravesAndGlory$ArmyPillager",
    "gg.gravesandglory.GravesAndGlory$ArmyGolem",
    "gg.gravesandglory.GravesAndGlory$ArmyDrowned",
    "gg.gravesandglory.GravesAndGlory$ArmyEvoker",
    "gg.gravesandglory.GravesAndGlory$ArmyRavager",
    "gg.gravesandglory.GravesAndGlory$ArmyBoarkin",
    "gg.gravesandglory.GravesAndGlory$ArmyWolf",
    "gg.gravesandglory.GravesAndGlory$ArmySpider",
    "gg.gravesandglory.GravesAndGlory$ArmyBear",
    "gg.gravesandglory.GravesAndGlory$ArmyZombie",
    "gg.gravesandglory.GravesAndGlory$ArmyStray"})
public abstract class ArmyMobPolymerMixin implements PolymerEntity {
    @Override public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return PlainLooks.look((Entity) (Object) this, player);
    }
}
