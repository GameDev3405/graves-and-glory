# Campaign bonus objectives, draft for Kort (2026-10-31)

Status: proposal only. Nothing here is applied; `sheets/design.json` still holds the placeholders `b2_tbd` to `b6_tbd`.

Every option uses a kind `MarchMath.Tracker` already knows: `hold_all`, `lose_at_most`, `stage_by`, `carry_self`, `win_within`, `kill_unit`. Every bonus needs the win. Shards are 5 to the battle's House (Houses spec). Gold is 40 unless an option says otherwise; all values are draft values. Battle rows follow the sheet (PLAN-TO-1.0 4D.1), which differs from the older spec section 10 list, so the spec's ideas are moved onto these rows where they fit.

Pick one letter per battle (or "none of these").

## Battle 2: The Iron Gate (Ironhold, Siege on the plan; runs as Domination until Siege joins the lobby; Ironclad shards)

| Option | Kind | Value / target | Gold | Text |
|---|---|---|---|---|
| 2A | `stage_by` | 360 s, `gate` | 40 | "Break the gate inside six minutes." (spec's Ironhold bonus; works only once Siege runs, so until then it can never be kept) |
| 2B | `lose_at_most` | 10 | 40 | "Lose no more than 10 troops." (works in Domination now and in Siege later) |
| 2C | `win_within` | 720 s | 40 | "Take Ironhold inside twelve minutes." (works in both modes; value needs retuning when Siege's 10-16 minute clock lands) |

Recommendation: 2B while it runs as Domination, then 2A when it goes back to Siege. Alternatively 2A now, accepting that the bonus is unkeepable until Siege ships.

## Battle 3: The Long Night (Hollowwood, Survival; Graveborn shards)

| Option | Kind | Value / target | Gold | Text |
|---|---|---|---|---|
| 3A | `lose_at_most` | 8 | 40 | "Lose no more than 8 troops." (spec's shrine bonus) |
| 3B | `kill_unit` | `hexer` | 40 | "Strike down a Gravecaller with your own banner." (needs a wave set that holds a Gravecaller) |
| 3C | `stage_by` | wave 5 by 480 s | 50 | "Clear the fifth wave inside eight minutes." (needs 2C.4 to name Survival's wave stages) |

## Battle 4: The Frozen Standard (Frostmarch, Banner; Wildmoot shards)

| Option | Kind | Value / target | Gold | Text |
|---|---|---|---|---|
| 4A | `carry_self` | none | 40 | "Carry their banner home yourself." (spec's banner bonus) |
| 4B | `win_within` | 600 s | 40 | "Take three banners inside ten minutes." |
| 4C | `kill_unit` | `wm_seer` | 40 | "Strike down the Bloodhowl Seer with your own banner." |

## Battle 5: The Five Crossings (Kingsfield, Domination, five points; Tidecallers shards)

| Option | Kind | Value / target | Gold | Text |
|---|---|---|---|---|
| 5A | `hold_all` | none | 50 | "Hold all five crossings at the same moment." (harder than battle 1's three, hence 50 gold) |
| 5B | `win_within` | 600 s | 40 | "Win the crossings inside ten minutes." (spec's Frozen Pass bonus) |
| 5C | `kill_unit` | `tc_brute` | 40 | "Strike down a Kraken-touched Brute with your own banner." |

## Battle 6: The Last Watch (Ironhold, Siege, mixed final; house still `all`, a placeholder)

The bonus needs one House for its shards (preflight refuses `all` once the bonus is real), so pick a House with the option.

| Option | Kind | Value / target | House | Gold | Text |
|---|---|---|---|---|---|
| 6A | `kill_unit` | `gb_hollowking` | graveborn | 60 | "Strike down the Hollow King with your own banner." (spec's finale bonus) |
| 6B | `stage_by` | 480 s, `keep` | ironclad | 60 | "Seize the keep inside eight minutes." |
| 6C | `lose_at_most` | 12 | gilded | 60 | "Lose no more than 12 troops." |

Finale gold is 60 in all three options to mark the payoff; at 40 everywhere the chapter pays the spec's 1060 total, and each 10 more gold adds 10 to that total. The economy model must be rerun with whatever is picked.

## Open points with any choice
- `stage_by` targets for Survival (3C) and Siege (2A, 6B) are only real once 2C.4 and Siege define the stage names.
- `kill_unit` needs the target troop in the enemy banner; 2C.4 fixes the battle's banners.
- Battle 2's `mode` returns to `siege` when Siege joins the lobby (its placeholder note says so).
