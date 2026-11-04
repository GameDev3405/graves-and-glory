# Current questions

Answered history is preserved verbatim in archive/QUESTIONS-FOR-KORT-before-optimization-2026-10-25.md. Its stale next-actions are not work orders. Owners below refer to decisions, not file ownership.

2026-10-28: Kort approved PLAN-TO-1.0.md and the seven Phase 0 decisions (DECISIONS.md). The rows below are the remaining open items; answered questions are not re-asked.

| ID | Affected work / owner | Recommended direction | Blocks |
|---|---|---|---|
| SLICE-01 | Builder-blocked remainder / Kort | 3v3 scope is answered (DECISIONS.md). Slice map is Ironhold; map adoption is generated base plus edit overlay with a snapshot fallback; AI build authority is in PLAN-TO-1.0.md 7.3. Still open: lobby/map art direction, bot-quality targets and when each builder world is done. | Map/lobby parts of the slice |
| SLICE-02 | Map/lobby direction / Kort + builder | Open: lobby and map art direction. The builder server is the editable surface; AI build authority and adoption are decided in PLAN-TO-1.0.md. Selected UI/unit images do not decide map design. | New map/lobby art |
| SLICE-03 | Performance/bots / coordinator + Kort | Working targets approved 2026-10-28: 60 fps per six-player machine, server MSPT p95 under 50 ms, map build under 60 s; confirm after the Phase 3 baseline. Test clients are Kort's PC plus friends' machines against tjepkema-server; bots never count. Bot-quality targets still open. | Claiming six-player acceptance |
| OWN-01 | Worktree archival / coordinator + Kort | Preserve unknown owners and ignored assets until disposition/recovery are verified. | Deletion only |
| VIS-01 | Existing UI/map review / Kort | Inspect in-party Friends at 1080p, real small/1440p/4K windows and Frostmarch supply flag in the next candidate. The 1280/1366 HUD 8.5 exception is already accepted. | Relevant visual acceptance |
| ARMOUR-01 | Player armour sets / Kort | Review `art/armor/review/*.png`. Then decide: (1) where the first crimson-knight set lives, so all six share one pipeline (its source is not in this repository); (2) whether the troop glow cap of 16 applies to player armour (Legendaries carry 29 to 61 lit texels); (3) whether Epic violet and Legendary orange stay exempt from the team-tint rule on trim. | Wiring armour into the game |
| WEAPONS-01 | Player weapons / Kort | Review `art/player_weapons/review/*.png`. Then decide: (1) the Legendary glow counts (10 to 66 lit texels) and glow on the bow and crossbow; (2) whether each new family (scythe, flail, spear, hammer, crossbow, trident, cutlass, shield) becomes an approved construction under style guide 7.3; (3) a length ruling for pole weapons (34 to 38.5 px now). | Wiring weapons into the game |
| STORY-01 | Voice casting / Kort | Continue approved dialogue/captions; decide speakers/voice production at that milestone. | Voice production only |

Optimization settles the old graph-docs question: code-only now; a small semantic experiment needs a future approved plan. Old growth-design six calls were withdrawn. Existing art, faces, tower and HUD answers are preserved in DECISIONS.md and the historical record; do not re-ask them.

Historical issue entries are retained without declaring them fixed. Before a subsystem task, consult its relevant archived section/spec and confirm whether later evidence supersedes it.
