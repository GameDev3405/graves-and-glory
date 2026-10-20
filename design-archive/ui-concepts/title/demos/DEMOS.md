# Title screen demos

Real in-game captures, 1920x1080, no AI images. Every demo shows classic Steve on the summit of one built hill (about 18 blocks across, 6 high: grass, dirt, stone, a dirt path up, tufts, a lantern post) at the edge of Rivergate, facing a live bot battle (120 troops, 3 Dawn armies against 3 Dusk armies, fighting under the game's own tactics). Diamond sword in his right hand; HUD, hotbar, crosshair and chat hidden; no lockup or menu.

Shaders are on in all six: Complementary Reimagined r5.5.1 on Iris 1.7.6 and Sodium 0.5.13, with the shipping resource packs (Excalibur, Clarent, Fresh Animations, EFA patch, armory).

- demo-1: low camera just behind his right shoulder (3.6 back, 2.7 up, FOV 60); time 12500, looking west into the setting sun along the road to the bridge; sword straight up; shaders on.
- demo-2: closer over-the-shoulder camera (2.3 back, 2.05 up, FOV 60); time 12000, sun higher; sword straight up against the sun; shaders on.
- demo-3: higher camera (9 back, 6.5 up, FOV 60) showing the whole hill, path and summit; time 12500, battle on the Rivergate bridge; sword straight up; shaders on.
- demo-4: three-quarter view from his left (2.6 back, 3 left, 2.1 up, FOV 70); time 12500, bridge battle; sword straight up; shaders on.
- demo-5: shoulder camera as demo-1; night, time 18000, objective lanterns lit; bridge battle; sword straight up; shaders on.
- demo-6: shoulder camera; time 12500, bridge battle; vanilla mid-swing frame, no pose hook (blade slightly motion-blurred); shaders on.

## The raised-sword pose

Minecraft has no arm pose that holds a sword overhead. The vanilla swing only gets about 45 degrees up and forward for a frame or two (demo-6 is the best such frame). Demos 1 to 5 use a harness-only render hook (a 10-line mixin at the end of `BipedEntityModel.setAngles`) that sets the right arm to pitch -2.36 rad and roll -0.25. The armory's 3D sword sits about 135 degrees from the arm, so a fully vertical arm tilts the blade backwards; leaning the arm slightly forward makes the blade stand straight up.

For the real title a true "sword straight up" pose needs only that much code. If the title draws its own `PlayerEntityModel`, it sets those two angles after `setAngles` and needs no hook. If it renders the player through the vanilla entity renderer, `setAngles` resets the arm every frame, so it needs the same small hook, limited to the title's own player model.

## Notes

- Steve is the vanilla classic skin. Excalibur replaces `steve.png` and EMF swaps in the armory player model, so the harness forced the vanilla texture and turned EMF off for the player.
- The Rivergate landmark obelisks (point markers) stand at the right of the bridge shots; the warehouse wall was kept out of the chosen sunset frames.
- Sources: `work/title-demos/130012/run/screenshots` (one launch, all checks passed). demo-1 = title-demo-15-sunset-12500-shoulder-up, demo-2 = title-demo-11-sunset-12000-over-up, demo-3 = title-demo-3-bridge-golden-high-up, demo-4 = title-demo-4-bridge-golden-threeq-up, demo-5 = title-demo-6-bridge-night-shoulder-up, demo-6 = title-demo-5-bridge-golden-shoulder-swing-a.
