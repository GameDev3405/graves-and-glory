# Questions and problems for Kort

Written while working autonomously on 2026-10-17. Newest at the bottom.
Each item says what was decided in the meantime so work could continue.

## Open questions

1. **Phase gates without you.** Each phase ends with a Kort playtest gate.
   I am treating the gates as "screenshots reviewed by a tester subagent"
   and moving on; please play the builds listed under "Builds to try" below
   when you are back and send corrections.

2. **Houses design (docs/superpowers/specs/2026-10-17-houses-design.md, section 8).**
   The designer made 12 calls that change the vision table and has 7 questions:
   trait tiers count banner slots (6/10/14) not troop count (3/5/7); the iron
   golem moved to Ironclad's Legendary and the ravager to Tidecallers; Wildmoot
   hunters are boar-kin on the zombified-piglin model, alpha is a polar bear;
   Sunweaver and Gravecaller become Epics (Oracle/Wraith fold into them as a
   star); migrated players keep two Epics free; spiders climbing walls in Siege;
   Legendary names; the 29-hour median to a full roster; commander ability on
   key G. I am proceeding with the designer's calls; overturn any when back.

## Problems hit

1. **Kingsfield (map 4) crashes the client when the lobby switches to it**
   (`ArrayIndexOutOfBoundsException` in `GravesAndGloryClient.map`: the minimap
   draws 5 points while the contested/progress arrays still hold 3 until the
   next state packet). Found 2026-10-17 by the harness-fix run. Decision: fix it
   inside Phase 1 (regression task) and add a check for it; until then do not
   pick Kingsfield from the lobby.
2. **Two campaign-suite checks were already failing before Phase 1:**
   `solo-pad-3v3` and `roadmap-suite`. Left as-is for now; Phase 1 Task 9
   either fixes them or records why they are stale.

## Builds to try

- 0.3.7 (master 2437e4f): camp, pads, parties, rooms, contracts verified by
  automated checks in a real client. Run it from the dev client or import
  dist/graves-and-glory-0.3.6.mrpack into Prism (0.3.7 mrpack not yet exported).
