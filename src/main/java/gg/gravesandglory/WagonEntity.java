package gg.gravesandglory;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * The Convoy supply wagon (Kort approved the entity type 2026-11-01; convoy spec 4.2). Kinematic: ConvoyMode places it on the
 * road each server tick, the client eases between those places over the tracker's steps and turns the wheels by the distance
 * rolled, backwards on a roll back. It has no health and no physics, and it is never written to a save: shouldSave is false,
 * the type is built with saving off, and it reads and writes no NBT of its own, so the save format is unchanged (a crash
 * leaves at most an unsaved wagon in a loaded chunk, which ConvoyMode.recover removes by type and tag).
 */
public final class WagonEntity extends Entity {
    public static final EntityType<WagonEntity> TYPE = Registry.register(Registries.ENTITY_TYPE, Identifier.of("gravesandglory", "supply_wagon"),
        FabricEntityTypeBuilder.<WagonEntity>create(SpawnGroup.MISC, WagonEntity::new)
            .dimensions(EntityDimensions.fixed(2.0f, 2.5f)).trackRangeBlocks(160).trackedUpdateRate(1)
            .disableSummon().disableSaving().fireImmune().build());
    /** Loads the class, which registers the type (GravesAndGlory.onInitialize). */
    static void register() {}

    /** A wheel's radius in blocks (model: 7 px from hub to tyre). */
    static final float WHEEL_RADIUS = 7 / 16f;

    // Client only: the eased move toward the tracker's last place, and the wheels' turn in radians.
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw;
    float wheel, prevWheel;

    public WagonEntity(EntityType<? extends WagonEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
        setInvulnerable(true);
    }

    public WagonEntity(World world) {this(TYPE, world);}

    @Override protected void initDataTracker(DataTracker.Builder builder) {}
    /** No fields of its own: the round's state lives on ConvoyMode, never in the wagon. */
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
    @Override public boolean shouldSave() {return false;}
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {return new EntitySpawnS2CPacket(this, entry);}

    /** No health (spec 3.3): arrows, blows, bolts and blasts change nothing. */
    @Override public boolean damage(DamageSource source, float amount) {return false;}
    @Override public boolean isImmuneToExplosion(Explosion explosion) {return true;}
    @Override public boolean canHit() {return false;}
    @Override public boolean isPushable() {return false;}
    @Override public void pushAwayFrom(Entity entity) {}
    @Override public ActionResult interact(PlayerEntity player, Hand hand) {return ActionResult.PASS;}
    /** The pole and tilt reach past the 2-block box; keep it drawn while any of it is on screen. */
    @Override public Box getVisibilityBoundingBox() {return getBoundingBox().expand(2.5, 1.0, 2.5);}

    @Override public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYaw = yaw;
        lerpSteps = Math.max(1, steps);
    }

    @Override public void tick() {
        super.tick();
        if (!getWorld().isClient) return;
        prevWheel = wheel;
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps, y = getY() + (lerpY - getY()) / lerpSteps, z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYaw(getYaw() + MathHelper.wrapDegrees(lerpYaw - getYaw()) / lerpSteps);
            lerpSteps--;
            setPosition(x, y, z);
        }
        // Forward is the way the wagon faces (MC yaw: -sin, cos); rolling back runs the wheels the other way.
        double rad = Math.toRadians(getYaw());
        double along = (getX() - prevX) * -Math.sin(rad) + (getZ() - prevZ) * Math.cos(rad);
        wheel += (float) (along / WHEEL_RADIUS);
    }
}
