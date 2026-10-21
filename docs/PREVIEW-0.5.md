# Graves and Glory 0.5.0 preview 1

A throwaway build of everything committed so far, for you to play and take notes on. It is not the real 0.5.0 and nothing in it is published.

## Install

Same as 0.4.0.

1. Take the .mrpack (`dist/graves-and-glory-0.5.0-preview1.mrpack`).
2. In Prism: Add Instance, Import, pick the .mrpack. Prism sets up Minecraft 1.20.1 and Fabric and downloads the pinned mods. (Or put the .zip into a Fabric 1.20.1 instance.)
3. Make it a new instance, so your 0.4.0 one stays as it was.
4. Start it. A fresh camp is fine. Keep your own key bindings.
5. Use a window at least 960 by 540 (maximise, or F11). The new pages crop in the small first-launch window.

Java 17 and a Minecraft account, as before. It was tested only in the development client and the preview has not been through Prism yet.

## What is new

- **Gilded Order troops with our own models.** Order Squire, Order Archer, Templar, Order Longbow, Sunweaver and Lord Marshal Aldric no longer look like vanilla mobs. Each has its own stage 1, 2 and 3 art. Their gifts work: the Longbow's shot pierces, the Sunweaver mends, shields turn arrows, the Order's Bulwark steadies a group near other Gilded troops, and the Lord Marshal plants an Oath Banner when his group holds.
- **Six commander classes.** Champion, Reaver, Warden, Marshal, Warlock, Quartermaster. Pick yours on the Commander class page in camp. Each has a passive bonus from the start. **H** fires your class ability. A small slot by the hotbar shows the key, the ability and its timer.
- **Stand Fast (Champion) and Rally Horn (Marshal)** are the two abilities that work in this build.
- **Five built maps:** Rivergate Valley, Ironhold Siege, Frostmarch Pass, Hollowwood Reach, Kingsfield Crossroads. The maps were rebuilt with roads, bridges, lanterns and buildings.
- **The War Table.** The camp home is a table with your party seats, your orders, the next battle on a field card and a Ride button. The recruit page has a banner for each House, a muster roll, a letter for the troop you point at, and an Enlist button (a random troop of that House, 100 gold).

## Rough edges you will meet

Checked in the development client: the 11 battle checks (marching, capture, combat, 80 troops, bridges, a hill, friendly fire, the barracks) pass. So do 32 of the 35 Phase 2 checks I ran for the new classes and Gilded troops. The three that fail are mismatches between two of the merged branches, not crashes:

- **Class page text is too big.** On the Commander class page the Quartermaster card runs its name into "ALL TROOPS", and its last line is cut off.
- **Two checks are out of date** (the recruit tabs, the HUD heart gap). The pictures look fine.

Unfinished work, seen or known:

- **Four class abilities do nothing yet:** Bloodrush (Reaver), Mark Prey (Warden), Grave Bloom (Warlock), Forward Supply (Quartermaster). H shows NOT IN THIS BUILD for them.
- **The Choose button (300 gold) on the recruit page is refused** with a notice. Only Enlist works. I read this in the code and did not click it in game.
- **Only Gilded has its own art.** Graveborn, Wildmoot, Ironclad and Tidecallers still use vanilla mob bodies, and only Gilded and Graveborn can be fielded. The other three show on the recruit page but cannot fight yet.
- **Balance is not done.** Gilded beat Graveborn every time in bot matches. Bots never use class abilities.
- **Small windows crop** the War Table home and recruit page.
- **The field card on the home page is painted for Rivergate only.** Other maps show bare paper with no point seals.
- **Maps still have patchy worn-earth bits** (dirt patches at points and along roads). A fix round was queued, not done.
- **Old wording:** a few labels still say Dawn and Dusk, though sides are now Gold, Violet, Crimson and Teal. Gold and teal are close in low light.
- **The class page and the banner editor** are still in the old look. They move onto the table later.
- **A server stall of a few seconds** ("Can't keep up") can happen right after a fresh world starts. It was there before this branch.

Your 0.4.0 profile is converted to the new collection format on first load. A new instance avoids that.
