# Prop brief: Drop Coin (`drop_coin`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** the giant coin the drop altar drops: a thick bronze disc with a glowing glimpur crystal core and an eye-rune on each face.
- **Where it goes:** nowhere on its own: a model the game renders and moves itself on the drop altar (`drop_altar`) board.
- **Setting / House:** Age of the giants relic (drop board)
- **Footprint (cells, x east * y up * z south):** 1 x 1 x 1. Front faces north. Anchor cell (0, 0, 0).
- **Scale against a troop:** 10 px across (0.625 block, physics radius 0.3125), 3 px thick (about 0.2 block): about a third of a troop's height, a giant's coin.
- **Silhouette at 15 m:** a bright bronze disc with a white-violet spark in the middle, read against the dark altar board.
- **Close-up story at 3 m:** the glimpur core with the giants' eye-rune cut through it; four engraved notches on the bronze rim.
- **Materials and ramps (proplib):** giant bronze `make_ramp("8a6a3e")`, glimpur crystal `fixed(3a2a66 .. f6f2ff)` (both shared with drop_altar), painted by `CoinPaint` (lists its ramps).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- **Collision:** none (solid []): it is only a rendered model.
- **Light and effects:** none in the def; the core elements are shade:false so they read bright, and the renderer may draw the whole coin at full brightness.
- **Gameplay effect (gameplay use only, design only):** none.
- **References:** drop_altar (same bronze and glimpur ramps); the drop-board design docs on ui/drop-board.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
