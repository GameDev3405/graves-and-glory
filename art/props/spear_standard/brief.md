# Prop brief: Spear Standard (`spear_standard`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** dressing (map dressing (battle maps): read at 15-40 m, silhouette first, never blocks a lane by accident).
- **What it is, in one line:** a war spear planted upright in a small cairn of stones, a swallow-tailed deep red and gold pennant streaming from just below the head: a camp rally marker.
- **Where it goes:** battle maps and lobby camps: beside a tent, at a camp edge, flanking a Domination point or a muster ground. Builders and map designers place it.
- **Setting / House:** any House; neutral deep red and gold, kept on its own part (`spear_standard_pennant`) so a team colour can replace it later.
- **Footprint (cells, x east * y up * z south):** 1 x 3 x 1. Front faces north. Anchor cell (0, 0, 0). The shaft stands at the west side of the cell so the pennant (it streams east) stays inside the cell plus 2 px.
- **Scale against a troop:** 48 px tall, 1.4 troops (a Knight is 35 px): a spear is longer than its bearer; the pennant hangs at head height of a troop (y 28..38).
- **Silhouette at 15 m:** a thin vertical shaft with a bright leaf-shaped head and a red-and-gold swallow-tailed flag streaming to one side.
- **Close-up story at 3 m:** the gold trim on the red cloth, the iron socket collar and leather grip wrap on the shaft, a heap of packed stones holding the shaft.
- **Materials and ramps (proplib):** `wood_y` (shaft), `steel` (head), `iron` (socket), `leather` (grip), `stone` (cairn); new ramp `make_ramp("8E1F20")` for the pennant red; `gilt` for the trim.
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px.
- **Collision:** the one cell solid: a planted spear is a post troops path around; arrows hit it. The model fills only part of the cell (shaft, cairn, pennant), so the collision map may warn about low fill; this is intended (it stops lanes being threaded past a thin pole at the cell edge).
- **Light and effects:** none.
- **Gameplay effect (gameplay use only, design only):** none (dressing).
- **References:** `stake_barricade` for field wood; `tent_soldier` for camp canvas; the Knight on the card for scale.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
