package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-match progression's server facade (in-match progression design 3 and 4): kill credit, level ups, gear, the Armoury state and the bots' buys.
 * - helped and supported write the kill-credit logs (Battle.credit, a CreditLog) for every troop blow (Battle.allowDamage), slow and pull
 *   (Abilities.slow, Abilities.pull), enemy held in a cloud (Abilities.mistTick), mend, Howl and Oath Banner.
 * - onDeath (AFTER_DEATH, every troop that really dies, a temporary one too) and onCommanderDeath (a player commander struck down, Battle.allowDamage)
 *   credit the killing blow's troop, his team's helpers in the window and their supporters with the victim's bounty (LevelMath.bounty); credit
 *   raises a soldier's credits and levelUp applies each level he reaches: the health step, the gear his army bought, the level name, the flash, his
 *   rank (rank_v1), his ability rows and the pending feed line.
 * - damageFactor and outputFactor read the soldier's level and worn weapon tier (LevelMath.damageFactor). Combat.resolve multiplies damageFactor onto
 *   the blow's base, outside the clamp, as stars do.
 * - Gear (design 4 and 5): each army's Armoury (Battle.armouries, one Kit per troop type) holds the highest level a soldier of the type reached this
 *   round and the tier bought on each track. buy takes a commander's gear: command; purchase takes the gold and wear puts the tier on every soldier
 *   whose level allows it (LevelMath.worn), armour as a health step like a level up's. botBuy is a bot's economy-tick buying (LevelMath.botPicks).
 *   armouryState and armouryOpen give state_v9 the Armoury block and the Iron Ledger glint bit; levelRows give the report's levels block.
 * - clear empties the logs, every Armoury, the round's tallies and the pending feed lines at a round boundary (Battle.clearProgression: startRound,
 *   Campaign.returnLobby and switchMap).
 * Everything runs on the server thread. Lane A owns this file.
 */
final class Veterans {
    private Veterans() {}

    /** The credit window, progression.assistSeconds in battle ticks. */
    static long window() { return Spec.PROGRESSION[0].assistSeconds() * 20L; }

    /** How often (battle ticks) helped and supported trim the logs to the window, so they hold no more than the last few seconds. */
    static final int TRIM_TICKS = 20;

    /**
     * The switch over progression.enabled (in-match progression plan, spec gap 12): null reads the sheet; true or false overrides it. The bot
     * simulation's -PsimProgression and the harness set it; play never does. Off, nobody is logged, credited, levelled or sold gear.
     */
    static volatile Boolean override;

    /** Whether in-match progression runs: the override when it is set, else progression.enabled. */
    static boolean enabled() { Boolean o = override; return o != null ? o : Spec.PROGRESSION[0].enabled(); }

    /** Whether this unit may earn credit: a living troop of the battle that is not temporary (a Grave Bloom Risen never earns; a player commander is no Unit). */
    static boolean earns(GravesAndGlory.Unit u) { return u != null && !u.temporary && u.mob.isAlive(); }

    /**
     * A troop damaged, slowed, pulled or weakened the victim, or held it in one of his zones (design 3.2): the victim's log gains him at this battle
     * time. Temporary troops, players and dead helpers are not logged, and a structure is no victim (design 3.2: structures are not kills).
     */
    static void helped(GravesAndGlory.Battle b, GravesAndGlory.Unit helper, LivingEntity victim) {
        if (b == null || victim == null || !enabled() || !earns(helper) || helper.mob == victim || Combat.structure(victim)) return;
        b.credit.hit(victim.getId(), helper.mob.getId(), helper.squad.team, b.time);
        trim(b);
    }

    /**
     * A troop healed or buffed an own troop (mend, Howl, the Oath Banner): the target's support log gains him at this battle time. Temporary troops
     * and players are not logged on either side, a troop does not support himself, and only a troop of his own team is supported.
     */
    static void supported(GravesAndGlory.Battle b, GravesAndGlory.Unit supporter, LivingEntity target) {
        if (b == null || target == null || !enabled() || !earns(supporter) || supporter.mob == target || target instanceof PlayerEntity) return;
        var t = Combat.unitOf(b, target);
        if (t == null || t.temporary || t.squad.team != supporter.squad.team) return;
        b.credit.support(target.getId(), supporter.mob.getId(), supporter.squad.team, b.time);
        trim(b);
    }

    /** Trims the logs to the window once every TRIM_TICKS. */
    static void trim(GravesAndGlory.Battle b) {
        if (b.time - b.creditTrimmedAt < TRIM_TICKS) return;
        b.creditTrimmedAt = b.time;
        b.credit.trim(b.time, window());
    }

    // ---- kill credit (design 3.2) ----

    /**
     * A troop of the battle really died (AFTER_DEATH; a Rise Again stands him up instead and never reaches here, so the rising blow credits nobody).
     * winner is the army the kill goes to, as the gold line finds it: the attacker's army, or within the credit window the army whose blow last hurt
     * the victim (a shot that lands after its archer fell, a fall or lava soon after a blow). When it is the other team's, the killing blow's troop
     * (found among b.units, alive and not temporary; a commander's own blow, a temporary troop's, a fallen archer's shot and a death with no attacker
     * have none), every helper of that team in the window and their supporters each get the victim's bounty: his level, 1 for a temporary troop. The
     * killer's onKill runs after. Whatever happens, the victim is forgotten by the credit logs. Called above AFTER_DEATH's temporary skip, so a Risen
     * killed counts 1.
     */
    static void onDeath(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, Entity attacker, GravesAndGlory.Squad winner) {
        if (b == null || victim == null) return;
        int id = victim.mob.getId();
        try {
            if (!enabled() || winner == null || winner.team == victim.squad.team) return;
            var striker = attacker == null ? null : Combat.unitOf(b, attacker);
            var killer = earns(striker) && striker.squad.team == winner.team ? striker : null;
            award(b, id, killer, winner.team, LevelMath.bounty(victim.level, false, victim.temporary), victim.spec.id());
        } finally {
            b.credit.forget(id);
        }
    }

    /**
     * A commander was struck down (Battle.allowDamage's respawn branch: a player commander never reaches AFTER_DEATH). The other team's striker, his
     * helpers in the window and their supporters each get progression.commanderBounty. side is the blow's army as Battle.allowDamage finds it: the
     * striking troop's (found among b.units), an enemy commander's own, or the army of a troop that fell before his shot landed (Battle.fallenSquad;
     * the fallen troop himself gets nothing). A death with no attacker (a pull into lava, a knock off an edge, a fall) goes, as a troop's does, to the
     * army of the commander's last enemy blow within Projectiles.CREDIT_WINDOW_TICKS (Squad.hitBy). An attacker the battle does not know (an operator,
     * a stray vanilla mob) credits nobody. Whatever happens, the commander's log and last blow are forgotten: he stands again within the window (the
     * Reaver in 9 s), and a second death must not credit the first one's helpers again. A bot commander has no body today; a body would call this too.
     */
    static void onCommanderDeath(GravesAndGlory.Battle b, LivingEntity commander, GravesAndGlory.Squad victim, int team, Entity attacker, GravesAndGlory.Unit striker, GravesAndGlory.Squad side) {
        if (b == null || commander == null) return;
        int id = commander.getId();
        try {
            if (!enabled()) return;
            var army = side;
            if (army == null && attacker == null && victim != null && victim.hitBy != null && BattleMath.creditWindowOpen(b.time, victim.hitAt, Projectiles.CREDIT_WINDOW_TICKS)) army = victim.hitBy;
            if (army == null || army.team == team) return;
            award(b, id, earns(striker) && striker.squad.team == army.team ? striker : null, army.team, LevelMath.bounty(1, true, false), "commander");
        } finally {
            b.credit.forget(id);
            if (victim != null) victim.hitBy = null;
        }
    }

    /** Credits the killer (null: no troop's blow), every helper of the team on the victim in the window and their supporters with the bounty; each is found among b.units (a dead or departed id gets nothing). Then the killer's onKill. */
    static void award(GravesAndGlory.Battle b, int victim, GravesAndGlory.Unit killer, int team, int bounty, String what) {
        Set<Integer> ids = b.credit.credited(victim, killer == null ? -1 : killer.mob.getId(), team, b.time, window());
        log(b, "kill", killer, bounty, what + " #" + victim + " for team " + team + ", bounty " + bounty + ", credited " + ids);
        for (var u : new ArrayList<>(b.units)) if (ids.contains(u.mob.getId()) && earns(u) && u.squad.team == team) credit(b, u, bounty);
        if (killer != null) Abilities.onKill(b, killer);
    }

    /** The soldier gains kill credits; each level they carry him to applies at once (9 at level 1 is two level ups). Nothing for a dead or temporary troop, or with progression off. */
    static void credit(GravesAndGlory.Battle b, GravesAndGlory.Unit u, int credits) {
        if (b == null || credits <= 0 || !enabled() || !earns(u)) return;
        int from = u.level;
        u.credits += credits;
        log(b, "credit", u, credits, "now " + u.credits);
        int to = Math.max(from, LevelMath.levelFor(u.credits));
        if (to > from) levelUp(b, u, from, to);
    }

    // ---- level ups (design 3.1, 3.3 and 7) ----

    /** A line for the event feed (design 7): a soldier of this team reached progression.feedLevel under this level name. */
    record Feed(int team, String levelName) {}

    /** This round's feed lines, oldest first, one per soldier reaching progression.feedLevel; the HUD task (Task 12) reads them, clear empties them at a round boundary. */
    static final List<Feed> pendingFeed = new ArrayList<>();

    /**
     * The soldier goes from one level to a higher one. Per level: his max health rises by that level's step (the base value set at spawn, stars in it,
     * scaled by the new factor over the old) and his current health by the same amount only; his army's Armoury sees the level (Kit.highest, which opens
     * that tier's buys for the round); he puts on what his army bought that the new level allows (wearOne: armour is one more health step); his rank
     * goes to every player tracking him (rank_v1). Then, once: his custom name becomes the level name (plus the veteran suffix), the flash plays at him,
     * his ability rows are read again (Abilities.rowsChanged) and, when he crossed progression.feedLevel, one feed line waits.
     */
    static void levelUp(GravesAndGlory.Battle b, GravesAndGlory.Unit u, int from, int to) {
        for (int level = from + 1; level <= to; level++) {
            healthStep(u, LevelMath.healthFactor(level - 1, u.armourTier), LevelMath.healthFactor(level, u.armourTier));
            u.level = level;
            levelUps++;
            seen(b, u, level);
            boolean geared = wearOne(b, u);
            log(b, "level", u, level, "max health " + u.mob.getMaxHealth() + ", health " + u.mob.getHealth() + (geared ? ", now wears weapon " + u.weaponTier + " armour " + u.armourTier : ""));
            RankSync.changed(b, u);
            log(b, "rank", u, level, "rank_v1 level " + u.level + " weapon " + u.weaponTier + " armour " + u.armourTier);
        }
        u.mob.setCustomName(b.troopName(u.squad, u.spec, u.level));
        flash(b, u);
        Abilities.rowsChanged(u);
        int feed = Spec.PROGRESSION[0].feedLevel();
        if (from < feed && to >= feed) {
            String name = ArmyMobs.levelName(u.spec, to);
            pendingFeed.add(new Feed(u.squad.team, name));
            log(b, "feed", u, to, name + " rises");
        }
    }

    /** A soldier of the army reached this level: the type's highest level seen (the buys it opens stay open for the round, design 4.2), its level-3 count for the report, and the team's tally. */
    static void seen(GravesAndGlory.Battle b, GravesAndGlory.Unit u, int level) {
        var k = kit(b, u.squad, u.spec.id());
        k.highest = Math.max(k.highest, level);
        if (level == 3) k.level3s++;
        var t = tally(u.squad.team);
        if (level < t.firstAt.length && t.firstAt[level] < 0) t.firstAt[level] = b.time;
        if (level == 3) t.level3s++;
    }

    /** Level ups applied since the server started (the harness reads it). */
    static int levelUps;

    /**
     * Scales the soldier's max health by after / before, on the attribute's base value (the value Battle.spawn set: the sheet's health times his star
     * bonus, so stars stay on top), and raises his current health by exactly what the maximum gained, never above the new maximum. Armour wear takes
     * the same step.
     */
    static void healthStep(GravesAndGlory.Unit u, double before, double after) {
        if (before <= 0 || after == before) return;
        var max = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (max == null) return;
        float was = u.mob.getMaxHealth();
        max.setBaseValue(max.getBaseValue() * after / before);
        float added = u.mob.getMaxHealth() - was;
        if (u.mob.isAlive() && added > 0) u.mob.setHealth(Math.min(u.mob.getMaxHealth(), u.mob.getHealth() + added));
    }

    /** The level-up flash's presentation (design 7: readable within about 24 blocks): dust in the side's colour, a ring of it at his feet and a column round him, and the sound at a volume vanilla carries 24 blocks. */
    static final int FLASH_RING = 16, FLASH_COLUMN = 30;
    static final float FLASH_SIZE = 1.7f, FLASH_VOLUME = 1.5f, FLASH_PITCH = 0.9f;
    static final SoundEvent FLASH_SOUND = SoundEvents.ENTITY_PLAYER_LEVELUP;

    /** One flash as it played: the soldier's entity id, where, the dust's RGB, the sound's id and the battle time (the harness reads it). */
    record Flash(int unit, Vec3d at, int rgb, String sound, long time) {}
    static int flashes;
    static Flash lastFlash;

    /** The level-up flash at the soldier, on the server, so every client near him sees and hears it with no packet of our own (spec gap 10). */
    static void flash(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        int rgb = b.side(u.squad.team).color();
        var dust = new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), FLASH_SIZE);
        double x = u.mob.getX(), y = u.mob.getY(), z = u.mob.getZ(), h = u.mob.getHeight();
        for (int i = 0; i < FLASH_RING; i++) {
            double a = i * Math.PI * 2 / FLASH_RING;
            b.world.spawnParticles(dust, x + Math.cos(a) * .75, y + .15, z + Math.sin(a) * .75, 1, 0, 0, 0, 0);
        }
        b.world.spawnParticles(dust, x, y + h * .55, z, FLASH_COLUMN, .35, h * .4, .35, 0);
        b.world.playSound(null, x, y, z, FLASH_SOUND, SoundCategory.HOSTILE, FLASH_VOLUME, FLASH_PITCH);
        flashes++;
        lastFlash = new Flash(u.mob.getId(), new Vec3d(x, y, z), rgb, FLASH_SOUND.getId().toString(), b.time);
    }

    // ---- gear (design 4 and 5) ----

    /** One troop type in an army's Armoury this round: the highest level a soldier of it reached, the tier bought on each track (1: none bought), and how many of its soldiers reached level 3 (the report). */
    static final class Kit {
        int highest = 1, weapon = 1, armour = 1, level3s;
        int bought(String track) { return LevelMath.ARMOUR.equals(track) ? armour : weapon; }
        @Override public String toString() { return "highest " + highest + " weapon " + weapon + " armour " + armour + " level3s " + level3s; }
    }

    /** The army's Kit for a troop type, made at its first use. */
    static Kit kit(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop) {
        return b.armouries.computeIfAbsent(s, k -> new LinkedHashMap<>()).computeIfAbsent(troop, k -> new Kit());
    }

    /** The army's Kit for a troop type, or null when no soldier of it has levelled and nothing was bought for it this round. */
    static Kit peek(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop) {
        var m = b.armouries.get(s);
        return m == null ? null : m.get(troop);
    }

    /** The banner's troop types, once each, in banner order (no empty or ^ slots, no unknown ids): the Armoury's rows. */
    static List<Spec.TroopsRow> types(GravesAndGlory.Squad s) {
        List<Spec.TroopsRow> out = new ArrayList<>();
        for (String id : s.banner) { var t = Campaign.troop(id); if (t != null && !out.contains(t)) out.add(t); }
        return out;
    }

    /**
     * Where a commander may buy gear (design 5.3, until supply houses exist): within economy.accessRadius of his barracks, or of one of his team's
     * reinforcement anchors (Battle.reinforcementAnchors: a held point that passes the forward-deploy rule, a standing Forward Supply banner with no
     * enemy near), measured on the ground plane as the barracks reach is. With Battle.forwardReinforcement off nothing deploys at an anchor, so only the barracks counts.
     */
    static boolean buyHere(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s) {
        if (b == null || p == null || s == null) return false;
        if (b.atBarracks(p, s)) return true;
        if (!b.forwardReinforcement) return false;
        double reach = Spec.ECONOMY[0].accessRadius();
        for (var a : b.reinforcementAnchors(s.team)) if (GravesAndGlory.Battle.horizontalDistance(p.getPos(), new Vec3d(a.x(), 0, a.z())) <= reach * reach) return true;
        return false;
    }

    /**
     * A commander's gear:<troop>:<weapon|armour>:<tier> (Battle.command). Refused with its reason (refusal) or bought (purchase); either way the commander
     * reads the outcome as a notice. Returns the refusal, "" when bought.
     */
    static String buy(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, String cmd) {
        String[] part = cmd.split(":", -1);
        Spec.TroopsRow t = part.length == 4 ? Campaign.troop(part[1]) : null;
        String track = part.length == 4 ? part[2] : "";
        int tier = -1;
        if (part.length == 4) try { tier = Integer.parseInt(part[3]); } catch (NumberFormatException ignored) {}
        String why = refusal(b, s, t, track, tier, buyHere(b, p, s));
        String said = why;
        if (why.isEmpty()) {
            int price = LevelMath.price(t.rarity(), tier);
            purchase(b, s, t, track, tier, price);
            said = t.name() + ": " + (LevelMath.ARMOUR.equals(track) ? "Armour " : "Weapon ") + MatchStats.roman(tier) + " bought for " + price + " gold";
        }
        log(b, "buy", null, why.isEmpty() ? 1 : 0, cmd + ": " + said);
        if (p != null) b.campaign.notice(p.getUuid(), said);
        return why;
    }

    /**
     * Why this buy is refused, or "" when it is allowed. First the checks that come before the Armoury's own: progression on, a running round the army
     * is seated in, a known troop, track and tier (tier 1 to the sheet's highest; tier I is always worn, so it reads "Already bought"), a troop of the
     * army's banner. Then LevelMath.refusal: already bought, the level not yet seen this round ("Needs a level-2 Knight", with the level name), the tier
     * below not bought, the location (here), the gold. A commander class's discount never applies to gear (design 4.2): the price is the rarity's.
     */
    static String refusal(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow t, String track, int tier, boolean here) {
        if (!enabled()) return "Levels are off";
        if (b == null || s == null || !b.running || !s.seated) return "Gear is bought during a battle";
        if (t == null || !(LevelMath.WEAPON.equals(track) || LevelMath.ARMOUR.equals(track)) || tier < 1 || tier > LevelMath.maxTier()) return "No such gear";
        if (!types(s).contains(t)) return t.name() + " is not in your banner";
        var k = peek(b, s, t.id());
        int highest = k == null ? 1 : k.highest, bought = k == null ? 1 : k.bought(track);
        return LevelMath.refusal(tier, highest, bought, s.gold, LevelMath.price(t.rarity(), Math.max(2, tier)), here, track, LevelMath.levelName(t, tier));
    }

    /** One gear buy as it happened, for the round's tally (the bot simulation's levels.csv and the harness): the army, the troop type, the track, the tier, the gold paid and the battle time. */
    record Bought(UUID army, String troop, String track, int tier, int price, long time) {}

    /** The army pays and the tier is bought for the round: no refund, and no gear cost on rebuys (design 4.2). Every soldier of the type whose level allows it wears it at once (wear). */
    static void purchase(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow t, String track, int tier, int price) {
        s.gold -= price;
        var k = kit(b, s, t.id());
        if (LevelMath.ARMOUR.equals(track)) k.armour = Math.max(k.armour, tier); else k.weapon = Math.max(k.weapon, tier);
        tally(s.team).bought.add(new Bought(s.owner, t.id(), track, tier, price, b.time));
        wear(b, s, t.id());
    }

    /** Every living soldier of the type in the army puts on what its Armoury allows him (wearOne); each whose tiers changed has his rank sent (rank_v1). Returns how many changed. */
    static int wear(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop) {
        int changed = 0;
        for (var u : new ArrayList<>(b.units)) if (u.squad == s && u.spec.id().equals(troop) && wearOne(b, u)) {
            changed++;
            RankSync.changed(b, u);
            log(b, "wear", u, u.level, "weapon " + u.weaponTier + " armour " + u.armourTier + ", max health " + u.mob.getMaxHealth() + ", health " + u.mob.getHealth());
        }
        return changed;
    }

    /**
     * One soldier's worn tiers brought to what his level and his army's buys allow (design 4.2: min(his level, the highest tier bought) on each track,
     * LevelMath.worn). A higher armour tier is a health step at his level, as a level up's (max and current health rise by the same amount); a higher
     * weapon tier raises his damage factor. Nothing for a temporary or dead troop. True when a tier changed; the caller sends the rank.
     */
    static boolean wearOne(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (u == null || u.temporary || !u.mob.isAlive()) return false;
        var k = peek(b, u.squad, u.spec.id());
        int weapon = Math.max(u.weaponTier, LevelMath.worn(u.level, k == null ? 1 : k.weapon)), armour = Math.max(u.armourTier, LevelMath.worn(u.level, k == null ? 1 : k.armour));
        if (weapon == u.weaponTier && armour == u.armourTier) return false;
        if (armour != u.armourTier) healthStep(u, LevelMath.healthFactor(u.level, u.armourTier), LevelMath.healthFactor(u.level, armour));
        u.weaponTier = weapon;
        u.armourTier = armour;
        return true;
    }

    /** One banner troop type's Armoury row: the four buttons Weapon II, Weapon III, Armour II and Armour III, each LevelMath.buyState against the army's gold. */
    static LevelMath.ArmouryRow row(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow t) {
        var k = peek(b, s, t.id());
        int highest = k == null ? 1 : k.highest, weapon = k == null ? 1 : k.weapon, armour = k == null ? 1 : k.armour;
        int price2 = LevelMath.price(t.rarity(), 2), price3 = LevelMath.price(t.rarity(), 3);
        return new LevelMath.ArmouryRow(t.id(),
            LevelMath.buyState(2, highest, weapon, s.gold, price2), LevelMath.buyState(3, highest, weapon, s.gold, price3),
            LevelMath.buyState(2, highest, armour, s.gold, price2), LevelMath.buyState(3, highest, armour, s.gold, price3));
    }

    /** The army's Armoury as state_v9 carries it to its commander p: whether he stands where he may buy (buyHere) and one row per banner troop type. Empty with progression off. */
    static LevelMath.Armoury armouryState(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p) {
        if (b == null || s == null || !enabled()) return LevelMath.Armoury.EMPTY;
        List<LevelMath.ArmouryRow> rows = new ArrayList<>();
        for (var t : types(s)) rows.add(row(b, s, t));
        return new LevelMath.Armoury(buyHere(b, p, s), rows);
    }

    /** Whether an Armoury buy for a banner troop of this group is open and not yet bought (state_v9's per-group armouryOpen, the Iron Ledger glint): any of its buttons OPEN or UNAFFORDABLE. */
    static boolean armouryOpen(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        if (b == null || s == null || !enabled()) return false;
        for (var t : types(s)) {
            if (!t.group().equals(group)) continue;
            var r = row(b, s, t);
            for (var state : new LevelMath.Buy[] {r.weapon2(), r.weapon3(), r.armour2(), r.armour3()}) if (state == LevelMath.Buy.OPEN || state == LevelMath.Buy.UNAFFORDABLE) return true;
        }
        return false;
    }

    /**
     * A bot's gear buying, in its economy tick after the banner refill (Campaign.tick; design 9.1). The banner's types go to LevelMath.botPicks with the
     * Armoury's state, the living soldiers at level 2 or higher, the bot's gold and the cheapest banner troop at the price the bot really pays
     * (the troop's sheet cost: the class discounts ended with the classes). Each pick is bought in order through the same rules as a player's buy, bar the location
     * (design 5.3: bots ignore it). Returns what was bought.
     */
    static List<LevelMath.Option> botBuy(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (b == null || s == null || !s.bot || !enabled() || !b.running) return List.of();
        var banner = types(s);
        if (banner.isEmpty()) return List.of();
        int cheapest = Integer.MAX_VALUE;
        List<LevelMath.TypeState> states = new ArrayList<>();
        for (var t : banner) {
            cheapest = Math.min(cheapest, t.cost());
            var k = peek(b, s, t.id());
            int living = (int) b.fielded(s).filter(u -> u.spec == t && u.level >= 2).count();
            states.add(new LevelMath.TypeState(t.id(), t.group(), t.rarity(), k == null ? 1 : k.highest, k == null ? 1 : k.weapon, k == null ? 1 : k.armour, living));
        }
        List<LevelMath.Option> bought = new ArrayList<>();
        for (var o : LevelMath.botPicks(b.campaign.tier(s), states, s.gold, cheapest)) {
            var t = Campaign.troop(o.troop());
            if (t == null || !refusal(b, s, t, o.track(), o.tier(), true).isEmpty()) continue;
            purchase(b, s, t, o.track(), o.tier(), o.price());
            bought.add(o);
        }
        if (!bought.isEmpty()) log(b, "botbuy", null, bought.size(), s.tier + " bot " + s.owner + ": " + bought + ", gold left " + s.gold);
        return bought;
    }

    /** The report's levels rows for an army (design 7; MatchStats.LevelRow): one per banner troop type in banner order, the highest level reached, how many reached level 3, and the tiers bought. */
    static List<MatchStats.LevelRow> levelRows(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        List<MatchStats.LevelRow> rows = new ArrayList<>();
        for (var t : types(s)) {
            var k = peek(b, s, t.id());
            rows.add(k == null ? new MatchStats.LevelRow(t.id(), 1, 0, 1, 1) : new MatchStats.LevelRow(t.id(), k.highest, k.level3s, k.weapon, k.armour));
        }
        return rows;
    }

    // ---- the round's tally (the bot simulation's levels.csv) ----

    /** One team's progression this round: the battle time a soldier first reached each level (index 2 and 3; -1: not yet), how many soldiers reached level 3, and every gear buy in order. */
    static final class Tally {
        final long[] firstAt = {-1, -1, -1, -1};
        int level3s;
        final List<Bought> bought = new ArrayList<>();
    }

    /** This round's tallies, team 0 and team 1. clear puts fresh ones in place, so a holder of the old one (BotSimulation, after the round ends) keeps it whole. */
    static final Tally[] TALLY = {new Tally(), new Tally()};

    /** A team's tally (a team outside 0 and 1 writes into a throwaway one). */
    static Tally tally(int team) { return team == 0 || team == 1 ? TALLY[team] : new Tally(); }

    // ---- the log the harness reads ----

    /** One progression event: kill (value the bounty), credit (credits added), level (the new level), rank (the level sent), feed (the level reached), buy (1 bought, 0 refused), wear (the soldier's level), botbuy (buys made); unit is the soldier's entity id, -1 for none. */
    record Logged(String kind, int unit, int value, long time, String detail) {}
    /** The newest events, oldest first, at most LOG_SIZE. Server thread only. */
    static final ArrayDeque<Logged> LOG = new ArrayDeque<>();
    static final int LOG_SIZE = 256;

    static void log(GravesAndGlory.Battle b, String kind, GravesAndGlory.Unit u, int value, String detail) {
        int id = u == null ? -1 : u.mob.getId();
        LOG.addLast(new Logged(kind, id, value, b.time, detail));
        while (LOG.size() > LOG_SIZE) LOG.removeFirst();
        if (Diagnostics.active()) System.out.println("[levels] " + kind + " " + (u == null ? "-" : u.spec.id() + "#" + id) + " " + value + " " + detail);
    }

    // ---- stat factors (design 4.1) ----

    /** The soldier's damage factor from his level and worn weapon tier (design 4.1): 1 + levels.damagePct + gearTiers.weaponPct. 1 for no unit. */
    static double damageFactor(GravesAndGlory.Unit u) { return u == null ? 1 : LevelMath.damageFactor(u.level, u.weaponTier); }

    /** The same factor for a troop whose main output is no hit (design 4.1): the Sunweaver's heal amount and the Mist Caller's cloud duration. Abilities applies it: mend (the heal amount) and cloud (the duration). */
    static double outputFactor(GravesAndGlory.Unit u) { return damageFactor(u); }

    // ---- the round boundary ----

    /** A round boundary: every credit entry, every Armoury, the round's tallies and every pending feed line go. The tallies are replaced, not emptied (see TALLY): whoever reads a round's numbers after it ends must hold the Tally before this runs, as BotSimulation does on each running tick. */
    static void clear(GravesAndGlory.Battle b) {
        b.credit.clear();
        b.creditTrimmedAt = 0;
        b.armouries.clear();
        TALLY[0] = new Tally();
        TALLY[1] = new Tally();
        pendingFeed.clear();
    }
}
