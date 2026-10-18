# Media shoot: shot list (2026-10-18, ON HOLD)

**Status: parked by Kort on 2026-10-18.** "Write all this down and put a hold on it until all the units are designed and polished, maps are designed and polished, etc." Resume after the unit identity pass (Phase 2 Task 5 and the skin batches) and the built world (Phase 3) are done and polished, so the footage shows finished troops and finished maps. The staging harness (`test-tools/.../TitleDemos.java` and its camera and pose mixins) is committed and ready; a `MediaShoot` suite was planned on top of it but not written.

Kort's brief: "take all of these pictures again but with no sword, Steve just stands there, viewing over the war and battle below; close-ups of his face, far-away shots of his face; some with the battle in the background and he's walking away; brainstorm cool poses and scenarios; film for a trailer; gameplay footage; intros and outros. Later we put it together."

Ground rules for every capture
- Real in-game captures, 1920x1080, Complementary Reimagined shaders on, HUD/hotbar/crosshair/chat hidden (except the gameplay set, which keeps the HUD).
- Classic Steve, vanilla skin, EMF off for the player. Empty hands in every shot. No sword anywhere in this shoot.
- One built hill at the edge of Rivergate (the demo hill), a live 3v3 bot battle below (120 troops), as in the title demos.
- Stills: `still-<set><n>-<slug>.png`. Film: `clip-<set><n>-<slug>.mp4` (H.264, 1920x1080, 30 fps, plus the frame PNGs deleted after assembly). Every file gets one line in `CONTACT.md`: what it is, time of day, camera, pose, anything odd.
- Times of day: sunset 12500, golden 12000, night 18000 (lanterns lit), dawn 23500 (mist), noon 6000.

## A. The watcher (stills): Steve standing, hands at his sides, looking at the battle
- A1 behind his right shoulder, low camera (the demo-1 camera), sunset.
- A2 high and wide: whole hill, path, summit, bridge battle (demo-3 camera), sunset.
- A3 three-quarter from his left (demo-4 camera), golden.
- A4 A1's camera at night.
- A5 A2's camera at dawn, mist on the river.
- A6 A1's camera at noon, hard shadows.

## B. His face
- B1 close-up, front, camera slightly below eye level, the sunset behind him so the rim of his head and shoulders glows; battle smoke rising behind.
- B2 close-up profile from his right, looking out over the field, golden.
- B3 extreme close-up, eyes level, dead centre, night with lantern light from below and to one side.
- B4 far away, front: camera at the foot of the hill looking up the slope, Steve small on the summit against the sky, sunset.
- B5 far away, front, from across the battle: camera low on the bridge with the fighting in the foreground, Steve on the hill behind, dusk.
- B6 looking back over his shoulder at the camera (head turned, body still facing the battle), golden.

## C. Walking away (stills, Steve mid-stride)
- C1 walking down the near side of the hill toward the camera, battle and smoke behind him, sunset.
- C2 from behind: walking away from the camera, toward the battle, down the path.
- C3 side-on, walking along the ridge with the battle below.
- C4 C1 at night, lanterns on the path.
- C5 walking away into the dawn mist, the field behind, head down.

## D. Scenarios (stills; brainstorm, keep the ones that read well)
- D1 crouched (sneak) beside a fallen Dawn banner on the slope, battle behind.
- D2 standing in the front rank of a Dawn shieldwall, shoulder to shoulder with his infantry, enemy line ahead.
- D3 on the bridge in the middle of the clash, both armies around him.
- D4 alone at the camp war table at dawn (if the camp is reachable in the same session; else skip).
- D5 arm raised, pointing forward (the pose hook: arm straight out, no item), the Dawn line charging past him.
- D6 aftermath: the field after the battle, arrows in the ground, smoke, Steve alone on the hill.
- D7 silhouette: Steve on the summit black against the setting sun, low camera.
- D8 archers' volley overhead: camera behind the archers, arrows in the air, Steve watching from the hill in the distance.
- D9 looking up: camera at ground level in the battle, a knight charging past the lens, Steve on the hill beyond.
- D10 night watch: Steve beside the summit lantern post, the field dark except the lanterns and braziers.

## E. Film (clips, 8 to 15 s each unless stated; smooth camera moves through the camera hook, eased in and out)
- E1 title drift: slow push from behind Steve toward the battle, 10 s, sunset. Also at night.
- E2 orbit: a full slow circle around Steve on the summit, 12 s, golden.
- E3 crane reveal: start on B1's face close-up, rise and pull back until the whole battle is in frame, 10 s. Intro candidate.
- E4 walk-away tracking: camera leads Steve as he walks away down the hill, battle behind, then he walks out of frame, 10 s. Outro candidate. Also at night.
- E5 battle b-roll, each 6 to 8 s: low tracking along a shieldwall; archers' volley from behind; knight charge toward camera; a capture seal changing hands; a rout (horn, line breaking and running); the bridge melee from above.
- E6 gameplay: first-person, HUD on, 60 to 90 s of a real skirmish from the player's seat: deploy, give a move order, select a group, a charge, a volley, a capture. Shaders may go off for this clip if the frame rate drops below 24; say so in CONTACT.md.
- E7 intro/outro plates: 3 s each of the empty hill at dawn, the empty summit at night, and the battlefield smoke after the fight, as backgrounds for titles.

## Output
- `work/ui-concepts/media/stills/` and `work/ui-concepts/media/clips/`, plus `CONTACT.md` (index) and `contact-sheet.png` (thumbnails of every still, labelled).
- Harness code only under `test-tools/`; nothing from the shoot ships in the mod jar.
