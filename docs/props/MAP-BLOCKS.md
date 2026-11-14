# Map blocks (section F)

Two real blocks for map builders, both in the Building Blocks creative tab. Code: `MapBlocks.java` (`MapBlocksClient.java` for the water look). Check: `GRAVES_PHASE1_CHECKS=map-blocks`.

| id | what it is | stand-in for plain clients |
| --- | --- | --- |
| `gravesandglory:troop_fence` | invisible full cube | `minecraft:barrier` |
| `gravesandglory:still_water` | still water surface on top of real sealed water; not a fluid | `minecraft:barrier` |

The stand-ins are declared once, in `MapBlocks.STAND_INS`, for the later Polymer layer (not added).

## Collision rules

- **Every non-projectile entity** (players, mobs, troops, and also items, boats and wagons): full cube.
- **Projectiles** (the context entity is a `ProjectileEntity`) and **context-free queries** (no entity): empty. Arrows and spells pass. The second matters because vanilla asks an arrow "am I stuck in this block?" with a context-free query; a full cube there would catch every arrow that crosses the cell. Cost: vanilla mob pathfinding reads the same query and sees the block as open, so troop pathing must classify the ids (the AI lead does).
- **Outline, raycast and camera shapes:** empty, so nobody can target the block, aim rays and the camera pass. A creative builder holding the item can still target it, to break it or build against it.
- **Sight:** the vanilla `canSee` ray uses the looking entity's collision shape, so it is blocked. The AI lead's sight helper skips the fence.
- Not opaque, no light block, no spawning, unbreakable in survival, drops nothing, no suffocation. Both are `solid`, so water, lava and buckets cannot replace them.
- Targeting: the commander relic ray (`RelicEffects.pick`) uses the outline shape, so it passes the fence.
- A 2-high stack is two full cubes with no gap, so it stops a jump (1.25 high).
- Builders holding the item in creative see a barrier-style marker on `troop_fence` and `still_water`.

## still_water

- Drawn as vanilla still water (`block/water_still`), tinted to the biome water colour, translucent, top at 14/16. Place it directly on a layer of real, sealed water.
- **Nobody stays on it.** Each server tick, a living entity on the ground whose feet are only on still_water is put back: at the last place it stood on anything else in the same world (within 8 blocks), otherwise on the nearest dry bank within 12 blocks (searched at most once per 20 ticks per entity; never onto a fence or still_water), and when no bank is found at that last place however far. Players are corrected by the server, so vanilla clients get the same result. Creative and spectator players are exempt (builders).
- **Known limit:** riders and vehicles are not put back (there are no mounts today); an entity that is not living (an item, a boat) is blocked by the full cube but not moved.
- Pathing is the AI lead's: `ai/pathing` classifies the ids, this branch does not touch it.

## Troop side (ai/map-blocks)

- **Pathing:** troop mobs only (`ArmyMob`; `TroopPathMixin`) get BLOCKED for a cell holding either block and for the cell above it, so troops do not walk into them or over a pool; vanilla mobs keep the open reading. The route field and engine-map read-back classify by id (fence: wall, still_water: no-entry water).
- **Sight:** `MapBlockRules.canSee` sees through both blocks and still stops at ordinary walls. Ranged troops, abilities, scouting, order aim and the stall's watched test use it; melee troops keep vanilla `canSee`, so a fence stops their target pick.
- **Landing:** floors, the stall jump, step hops and `Tactics.standable` never put a troop in, on or under either block (`MapBlockRules.landingOk`).
