# Prop brief: Giant's Stone Hand (`stone_hand`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a weathered stone hand, far bigger than a person's, rising out of the ground palm-forward with its fingers half curled, one finger broken off.
- **Where it goes:** chapter 1 maps, first Rivergate Valley ("a stone hand on Rivergate", chapter 1 outline, relic table). Off the main lanes: a river bank, a field edge, half buried by a crossing. Kort places it; it is a hint, never a landmark the briefing names.
- **Setting / House:** no House. It is older than the Houses and nobody in chapter 1 says what it is (lore draft 2026-11-09: the land is a body; nobody names the giants).
- **Footprint (cells, x east * y up * z south):** 3 x 4 x 2. Front faces north. Anchor cell (1, 0, 0).
- **Scale against a troop:** about 61 px, so 1.8 troops tall; the hand alone (wrist to fingertip) is twice a troop. Big enough to say "giant", small enough to sit in a field as an odd rock until you look.
- **Silhouette at 15 m:** an open hand coming up out of the ground: four finger shapes over a broad block, thumb out to the side.
- **Close-up story at 3 m:** the broken index finger; moss on every upward surface (it has stood a long time); the stone's own rubble around the wrist.
- **Materials and ramps (proplib):** new ramps `make_ramp("8A8478")` weathered stone (paler and warmer than the forge's bricks, so it reads as carved bedrock, not masonry), `make_ramp("5F7A3A")` moss on top faces, `make_ramp("6E5038")` churned earth.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** the wrist and palm column (x=1, y 0-2, both z): troops walk around the hand, arrows hit it. The low earth mound and rubble stay walkable (2-5 px). The fingers (y=3) are above anyone's reach and stay open.
- **Light and effects:** none. It is stone; stillness is the point.
- **Gameplay effect (gameplay use only, design only):** none (dressing). As solid cells it already blocks movement and arrows like any wall.
- **References:** the approved forge set (`docs/reports/prop-kit-art-sheet.png`) for density and light; the Knight on the card for scale. No outside game's assets.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
