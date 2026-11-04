package gg.gravesandglory;

/**
 * Whether a living entity is a troop, cached on the entity (Task 7 fix 2, N6). CorpseMixin implements it on every LivingEntity, so the
 * corpse guards (canHit, cramming, knockback) and the corpse overlay ask it without a name lookup per call. On the server a troop is an
 * ArmyMob; the client holds troops as vanilla mobs, so there a troop is told by its custom name (ArmyMobs.named).
 * <p>Contract: the first answer is latched for the entity's life and never recomputed. Ask it only of a dead entity (isDead), whose custom
 * name no longer changes; asked of a living one, a later rename (a troop spawned before its name is set, a nametag) would be missed and the
 * stale answer kept. Every caller today guards on death first: CorpseMixin's corpse test checks isDead, and CorpseOverlayMixin checks
 * deathTime past the fall.
 */
public interface TroopBody {
    boolean gravesandglory$troop();
}
