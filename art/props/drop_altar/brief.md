# Prop brief: Drop Altar (`drop_altar`)

Fill every field before modelling. The reviewer checks the card against this page, so vague answers make a vague review.

- **Use:** lobby (lobby / Old Hold hub: seen up close (2-6 m) and lingered on, so detail and story carry).
- **What it is, in one line:** a giant-relic altar from the age of the giants: a huge upright peg board (plinko) of dark weathered stone, giant bronze and bone pegs, crowned by a glowing glimpur crystal; the drop board's coin falls through it.
- **Where it goes:** the drop board (branch ui/drop-board), standing on a 2-block stone plinth of world blocks; the game drives the coin (`drop_coin`) over its open face.
- **Setting / House:** Age of the giants relic (drop board)
- **Footprint (cells, x east * y up * z south):** 11 x 14 x 2. Front faces north. Anchor cell (5, 0, 0).
- **Scale against a troop:** 14 blocks tall and 11 wide on its plinth: seven troops tall, a giant's machine. Board geometry is fixed by the coin physics (board 9 x 11 blocks, pegs every block).
- **Silhouette at 15 m:** a dark stone frame between two carved pillars, a field of pale bone pegs, a row of nine glowing plaques at the foot and a violet-white crystal at the crown.
- **Close-up story at 3 m:** faint giant script cut between the peg rows; the rune plaques reading x10 x5 x3 x2 x1 x2 x3 x5 x10 in their colours; bronze claws gripping the glimpur crystal and glimpur veins in the pillars.
- **Materials and ramps (proplib):** dark giant stone `make_ramp("514c56")` (ashlar, script and pillar fluting patterns), giant bronze `make_ramp("8a6a3e")` with `pat_iron`, bone `make_ramp("c4b896")`, glimpur `fixed(3a2a66 .. f6f2ff)`; plaques use one 5-tone ramp per multiplier colour (#96969e, #78bee6, #5adcc8, #b078ff, #ffecaa as tone 3) through `PlaquePaint` (lists its ramps).
- **Budget:** at most 64 elements in total (hard limit 96); sheet at most 128 px (256 only for props over 8 cells).
- Budget override: elements 300, sheet 512 (a 11x14x2 flagship board: 76 two-box pegs alone are 152 elements; the lead allowed up to about 300 / 512).
- **Collision:** every footprint cell the model fills is solid (barriers): the back row (stone plate and pillars) and the front row too, so no troop or player walks into the coin channel; the open air cells beside the crown are left open.
- **Light and effects:** glow parts (full-bright): the crown crystal, two pillar veins with studs on the capitals, and the nine rune plaques. No light blocks or particles in the def yet (the lead may add a light at the crown).
- **Gameplay effect (gameplay use only, design only):** none in battles; the drop board's coin physics uses the same board geometry.
- **References:** the giant bones props (giant_rib, stone_hand) for the age-of-giants look; the drop-board design docs on ui/drop-board; `drop_coin` shares its bronze and glimpur ramps.
- **Done means:** the card passes every automatic check and a fresh Sonnet-high reviewer passes the card against this brief.
