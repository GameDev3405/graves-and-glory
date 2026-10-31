package gg.gravesandglory;

/**
 * Whether a living entity is a troop, cached on the entity (Task 7 fix 2, N6). CorpseMixin implements it on every LivingEntity, so the
 * corpse guards (canHit, cramming, knockback) and the corpse overlay ask it without a name lookup per call. On the server a troop is an
 * ArmyMob; the client holds troops as vanilla mobs, so there a troop is told by its custom name (ArmyMobs.named). Only a dead entity is
 * asked, whose name no longer changes, so the first answer is kept.
 */
public interface TroopBody {
    boolean gravesandglory$troop();
}
