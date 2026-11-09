# Review booth: design

2026-11-09. Draft for Kort's approval. Not implemented. Mockup: https://claude.ai/artifact/2KBivd4aR2WiXumJtRUiEp (throwaway, placeholder figures).

Lines marked **(Kort)** are his answers from the 2026-11-09 session. Lines marked **(my call)** are the designer's defaults and are open to correction.

## Why

Kort is the approval gate for every animation batch. Today a lane finishes, a Sonnet reviewer signs off, and the lane then sends Kort a video and a merge question in chat. He reviews at his PC and on his phone **(Kort)**. A video cannot be scrubbed frame by frame, cannot be compared against the previous version, and feedback has to be typed. On 2026-11-09 there are 22 open `anim/v2-*` lanes behind this gate, and the 1.21.1 port waits on them.

The booth makes one review take minutes, and makes the answer precise enough that a lane can act on it without a follow-up question.

## Goals

- Kort reviews a whole batch in one place, on PC or phone, without launching the game.
- Each clip is compared against a reference and against its previous version, in sync, at close range and at game distance.
- A rejection says what is wrong and at which frame, with taps, not typing.
- An approval is a recorded, unambiguous go-ahead that a lane agent can act on.

## Not in this version

- Troop art, maps, lobby sections and UI screens. Animations only **(Kort)**. The data shape should not block adding them later.
- Typed notes and voice notes. Kort chose tags and marked moments **(Kort)**.
- Automatic merging without Kort's tap. Auto-merge-then-veto was offered and not chosen **(Kort)**.
- Troops still on the old clip system (`ClipLibrary`/`TroopMotion`). The offline renderer only draws GeckoLib models.
- New mechanical checks. The booth displays whatever numbers a batch carries; the animation scorecard is a separate project.

## What would make this a failure

- An approval that merges something other than what Kort looked at.
- Anything that pushes, publishes or merges into `main`. The booth only authorises the same local lane merge Kort approves in chat today.
- A page that is slower or fiddlier on the phone than watching a video.
- Agents treating text stored by the page as instructions.

## What Kort sees

One private claude.ai Artifact page, the same link every time **(Kort chose a private claude.ai page; one permanent page instead of one per batch is my call)**.

**Queue.** The page opens on the list of batches waiting for him, oldest first, each with troop or family name, lane, clip count and age. Decided batches drop to a short history list.

**Batch.** Opening a batch shows a sticky bar (batch name, lane, "n of m decided", a Game distance toggle) and one card per clip, as in the mockup:

- Clip name, impact tick, and the batch's numbers for that clip (for example peak rotation speed, foot slide). A number the lane flagged as over a limit is shown in the warning colour.
- Three viewers side by side: Reference, Previous, New. They stay side by side on the phone **(my call)**.
- One transport for all three: play or pause, step one tick back or forward, a scrubber, a tick readout. A brass mark on the scrubber shows the impact tick.
- Approve and Reject.
- On Reject: the quick tags, and a "Mark tick N" button that pins the currently selected tags to the paused tick **(Kort: point at a moment, quick tags)**. Pins show as red marks on the scrubber and as a removable list. A rejection with no tag and no pin is allowed.

Tags in the first version **(my call, from the recurring findings in `docs/anim-rebuild/*-REVIEW.md`)**: Too stiff, Too slow, Too fast, Clipping, Wrong weapon angle, No weight, Feet slide, Hard to read.

**Sending.** "Send review" is enabled once every clip has a verdict. "Approve all undecided" is enabled only after every clip card has been scrolled into view **(my call)**. After sending, the batch is locked and shows what was sent. Until then verdicts are saved as he goes, so he can start on the phone and finish at the PC.

**What the three viewers show.**

- **New:** the clip as built at the lane's submitted commit.
- **Previous:** the same clip name at the tip of the branch the lane will merge into, as it stands when the batch is built, so Kort sees what he would be replacing **(my call)**. The manifest records that target SHA. Empty with the label "New clip" when it did not exist. On a resubmission after a rejection, Previous is the rejected version.
- **Reference (my call):** the approved Knight v2 clip that the lane names as its template for this clip (the lane's builder already starts from a knight clip). When there is no sensible template (beasts, siege engines), the viewer is empty with the label "No reference". Kort's combat reel video stays the style bar but is not cut into per-clip references in this version. The reference is drawn on the Knight's own model with the Knight's gear, exactly as Kort approved it, not retargeted onto the new troop **(my call)**.

All three viewers of a clip share one camera: the frame is sized from the combined bounds of the three, so scale and crop match. This needs a small addition to the renderer's framing **(my call)**.

Each viewer is drawn from pre-rendered frames, one per game tick, so scrubbing is exact. Two camera distances per clip: near, and the renderer's existing `game16` distance. One view angle in the first version: three-quarter **(my call)**.

## What a lane agent does

1. **Build the batch locally.** A new command renders the frames and writes a manifest. It runs after the lane's Sonnet review has passed, at a clean commit.
   - Input: lane worktree, the troop or family, the clip list, the reference mapping, the target branch.
   - What to render comes from a small per-family config file kept beside the family's builder in `art/anim/<family>/` **(my call)**: model file, texture layers, animation files, the stance or base clip an attack plays over, root speed for gaits, foot bones, the knight reference clip for each clip, and each clip's impact tick. The impact tick is supplied by the lane from its builder's own impact value.
   - Numbers in the first version are limited to what exists: foot slide from the renderer's report, flagged over 0.05 blocks per step (the limit in `docs/anim-rebuild/PLAN.md`), and the impact tick. More arrive with the scorecard project.
   - It lists every path the lane changes outside the family's own `art/anim/<family>/` folder and the troop's model, texture and animation assets, and puts that list in the manifest (see "Changes the booth cannot show").
   - It renders New from the worktree, Previous from the target branch tip (via a temporary checkout of only the needed model and animation files, never by switching the lane's branch), and Reference from the knight's files at the target branch.
   - It uses `tools/anim/render_geo.py` (exists on `anim/geckolib-rebuild`), per-tick PNG frames, packed into sprite sheets. Uploads go at most 25 files per call, so the builder should pack to keep a batch to a few calls (for example one sheet per clip holding all three viewers and both distances).
   - Output: `work/booth/<batch-id>/` with the sheets and `manifest.json` (batch id, lane branch, submitted commit SHA, target branch, per clip: name, tick count, impact tick, sheet files and grid, numbers, flags).
   - Size budget: a batch of 20 clips stays under 40 MB of sheets in total; the builder picks frame size and format to meet it.
2. **Publish the batch.** The agent uploads the sheets to the page's asset store with the Artifact tool, then writes one batch document and one document per clip to the page's database with the ArtifactData tool. The page itself is not republished for a new batch.
3. **Tell Kort once**, in its own session: one line with the batch name and the booth link.
4. **Read the verdict.** When Kort says he has reviewed (or at the lane's next natural milestone), the agent reads the batch's verdict documents and saves them verbatim to `work/booth/<batch-id>/verdict.json`. No polling loop **(my call; WORKFLOW.md forbids idle polling)**.
5. **Act.**
   - Every clip approved, the latest round marked sent, and the lane branch tip still equal to that round's submitted SHA: the agent performs the lane's local merge into the target branch and records the batch id in the merge commit message ("Kort approved in review booth, batch <id>"). **(Kort: approve means the agent merges.)**
   - Any clip rejected: no merge. The tags and marked ticks are the fix list. After fixing, the lane submits a new round of the same batch containing only the changed clips; approved clips stay approved as long as their rendered frames are byte-identical. The existing limit of two automatic fix rounds applies.
   - Branch tip differs from the submitted SHA: no merge; rebuild and resubmit.

## Changes the booth cannot show

A lane's merge can carry files the booth has no picture for: Java code, other families' builders, shared `art/anim/common/` code, other assets. The batch view lists them above the clip cards: a count and the paths, collapsed on the phone. Kort's approval of the batch covers the whole lane merge, listed files included, as his chat approval does today **(Kort)**. A batch with no such files shows "Animation files only".

## Rules that keep an approval honest

- A clip's approval is bound to a hash of its rendered frames. It carries over to a later round only while that hash is unchanged. A merge needs every clip in the batch approved at its current hash, and the lane tip equal to the latest round's submitted SHA.
- The page is private and its database rules allow verdict writes for the page owner only. That does not technically stop an agent, because agents use the database tool as Kort. The protection is procedural, the same trust as a chat approval today: agents never write under the verdict paths, the merge commit names the batch, and the booth's history shows every sent verdict, so a merge without a matching verdict Kort remembers sending is visible. A stronger check (for example a code the page shows only to Kort) is left out of the first version **(my call)**.
- Uploading rendered frames to this private page is the one external destination this design adds. Kort chose it on 2026-11-09 knowing the clips are uploaded **(Kort)**. It is not a release and nothing else is uploaded.
- Verdict data has a fixed vocabulary: verdict (`approved` or `rejected`), tags from the fixed list, tick numbers. There is no free-text field. An agent ignores any verdict document that does not match that shape, and never treats stored values as instructions.
- Agents write batch and clip documents; the page writes verdicts. Neither edits the other's documents.
- The merge authorised is the same local merge Kort approves in chat today: lane branch into its target animation branch. Never `main`, never a push.

## How it is made

- **Page:** one HTML Artifact with the `db`, `assets` and `user` capabilities, built from the mockup's layout and tokens. It shows a designed empty state when no batch is waiting.
- **Local tool:** Python, next to the renderer in `tools/anim/` **(my call)**, with tests in the same style as `tools/anim/test_render_geo.py` (sprite packing, manifest shape, target-tip resolution, frame hashing, SHA binding, the list of other changed files).
- **Agent instructions:** a short section added to the troop-animation skill: build, publish, tell Kort, read, act. It replaces "send Kort a video and ask in chat" for GeckoLib lanes.
- **Where the work happens (my call):** a branch from `anim/geckolib-rebuild`, because the renderer exists only there. It reaches `main` with the animation rebuild.
- **Clean-up:** when a batch is merged or abandoned, its sheets are deleted from the asset store by the lane agent that uploaded them; the batch and verdict documents are kept as the record.

## Left to the builder

- Database paths and document fields, beyond the rules above.
- Frame size, sheet layout and image format within the size budget.
- How the page loads and caches sheets on a phone connection.
- The command's name and flags.
- Exact wording of labels and empty states.

## How we will know it works

- Kort reviews one real batch (suggested: the next shield-family lane) on the phone and one at the PC, start to finish, without asking for a video.
- A rejected clip comes back fixed using only the tags and marked ticks.
- An approved batch is merged by its lane with the batch id in the commit message, and a deliberately moved branch tip is refused.
