# Admin tools and OG rewards: design

Date: 2026-11-14. Source: Kort's answers by AskUserQuestion (status/DECISIONS-2026-11-09.md, "2026-11-14 (night)"). Lines marked **(call)** are the coordinator's choices, not Kort's words; Kort can overrule any of them.

## 1. Why

Kort runs the Graves and Glory server and wants to do admin work on it without editing files: thank early supporters, hand out rewards, run events, and moderate. The first use is the OG reward: everyone who supported the modpack in its first month gets an item that lets them choose one of four exclusive armor sets that nobody else can ever obtain.

Success looks like: Kort opens a screen in game, sees who the OGs are, sends them a reward, and can see and undo everything he or a moderator did.

## 2. Goals, non-goals, anti-goals

Goals
- Know, from server data alone, who played in the OG window.
- Four exclusive OG armor sets inside the existing commander gear system.
- Reward tokens as a reusable system; the OG token is its first use.
- An in-game admin screen for grants, tags and titles, events, moderation and the action log.
- Two roles: owner (Kort) and moderator.

Non-goals
- No web panel, Discord bot or out-of-game tool.
- No shop, payments or supporter tiers.
- No change to how normal gear drops, forges or balances.
- No new weapons or relics for OG sets.

Anti-goals (these make it a failure even if it works)
- OG armor that is stronger than the best normal gear.
- Any way for a non-OG to get an OG set: trade, Forge roll, breakdown refund, duplication, or a client-side request the server trusts.
- An admin action that leaves no log line, or that Kort cannot undo.
- A player missing the OG tag because join tracking shipped late.

## 3. Phases

| Phase | When | Contents |
|---|---|---|
| A | Before 1.0.0, now | Join tracking, the OG window, the OG tag, stopgap commands (section 4) |
| B | After 1.0.0 | Roles, action log, admin screen, grants, tags and titles, moderation (sections 5, 6) |
| C | After 1.0.0 | Reward tokens and the four OG sets (sections 7, 8) |
| D | After 1.0.0 | Events (section 9) |

B, C and D each get their own implementation plan. B comes first because C and D are operated from its screen. **(call)**

## 4. Phase A: join tracking and the OG tag

Kort's rulings
- A player counts as having joined when they **finish one match**.
- OG is automatic by date **plus** manual add and remove.
- Kort sets the window's start and end dates himself, and may change them.

Design **(call, all of this section below; revised after the builder check)**
- The stamps live in a server-only ledger, `config/gravesandglory/joins.json`, keyed by account uuid: `firstSeenAt`, `firstMatchAt` (epoch milliseconds, 0 = not yet) and `lastName`. They are **not** in `Profile` and the profile schema does not change: a damaged profile falls back to a fresh one, a single-player import loads client data, and the whole profile is sent to its client. The ledger survives all three and reaches no client.
- `firstSeenAt` is set on the first login after this ships; `firstMatchAt` when the player first finishes a match.
- "Finishes a match": a battle of any mode (versus, Long March, survival, convoy, siege) reached its ending, the player was present at the end (dropping and coming back counts; dropping for good does not), and the player's own seated time meets the game's existing minimum for match rewards (`minimumRewardSeconds`, about three minutes). A battle against bots counts. A battle ended by a server crash counts on the same rule.
- Both stamps are written once and never change, including under clock changes or profile repair.
- Server file `config/gravesandglory/admin.json` (server-owned, never sent to clients) holds: `owners [uuid]`, `ogWindow {start, end}` as ISO-8601 instants or plain dates (a window needs both bounds), `ogAdded [uuid]`, `ogRemoved [uuid]`.
- OG status is **derived**, not stored: `ogAdded` contains the player, or (`firstMatchAt` is inside the window, bounds inclusive, and `ogRemoved` does not contain them). Changing the window therefore re-evaluates everyone, including offline players, with no migration.
- Existing testers have no stamp until they next finish a match. Kort sets the window to cover the test period, or adds them by hand.
- Stopgap commands until the admin screen exists, allowed only from the server console or for an account in `owners`. Vanilla operator level grants nothing: the builder server makes every player an operator. An empty `owners` list means console only. Commands: `/gg og window <start> <end>`, `/gg og window clear`, `/gg og add <player>`, `/gg og remove <player>`, `/gg og list`, `/gg og check <player>` (shows both stamps and why they are or are not OG). `add` and `remove` accept offline players by name when the server has seen them.
- The client is told nothing in phase A; the tag has no visible effect yet.
- Checks: standalone checks for the derivation rule (window edges, added, removed, no window, stamp 0), the owner gate, malformed files, and stamp-once; one in-game check that finishing a match sets `firstMatchAt` and leaving early does not.

## 5. Phase B: roles and the action log

Kort's rulings
- Kort can do everything. Moderators can mute, kick, temporarily ban, permanently ban, and read the action log. They cannot grant anything or run events.
- "mods can do perm bans but everything is undoable by me."
- Every admin action is logged.

Design **(call)**
- Roles live in `admin.json` by account uuid: the `owners` list from phase A and a `moderators` list. Only an owner can change either list. Vanilla operator status grants nothing in this system; the first owner is set in the file by hand.
- The public server must not make players operators (the builder server does, through `BuildMode.opOnce`): an operator could use vanilla `/give` and similar commands around this system. Phase B's plan confirms which servers auto-op and closes it for the public one.
- Every admin request is checked on the server against the role; the screen hiding a button is never the protection.
- Action log: append-only file, one JSON line per action: time, actor, action, target, parameters, reason, and the data needed to reverse it. Kept for the life of the server.
- Undo: each action type defines its reverse (unban, unmute, remove the granted thing, restore the removed thing, remove a tag, end an event). An undo is itself a logged action. Where an exact reverse is impossible (a player already spent granted shards), undo removes what remains and says so. Only the owner can undo.

## 6. Phase B: the admin screen

Kort's ruling: an in-game screen from the start, not commands first.

Design **(call)**
- Opened by a key or command; refused by the server for anyone without a role. It follows the existing dark UI kit.
- Tabs: Players, Rewards, Events, Log, Settings. Moderators see Players (moderation part only) and Log.
- Players: search by name, online and offline. A player page shows stamps, tags, titles, currencies, gear and troop counts, and moderation state. From there: grant or remove gear, troops, gear shards, troop writs, House shards and Renown; add or remove tags and titles; mute, kick, ban.
- Every action asks for a reason (required for moderation, optional for grants) and a confirmation.
- Settings: the OG window dates, the moderator list.
- Moderation: a mute blocks chat for a duration; a ban is temporary (duration) or permanent; both show the reason to the player. Bans are by account.
- Tags and titles. A tag is a server-side label on an account (og, tester). A title is the text players see; a tag may grant a title. Kort's ruling: titles show in chat, above the head, and on the end-of-match screen, and a player with several chooses which one shows. The choice is made in the camp profile screen **(call)**.
- Each admin command stays available as a typed command for scripting **(call)**.

## 7. Phase C: reward tokens

Kort's rulings
- Reusable: Kort defines a token and hands it out; OG is the first.
- The token is an item the player holds and right-clicks.
- Bound to its player. Pick one option, forever.

Design **(call)**
- A token type is data: id, name, look, and a list of options; each option is a bundle of grants (gear pieces, a title, currency). Token types live in a server sheet so a new reward needs no code.
- The item in the player's hand is only a handle: it carries the token type and the owner's uuid. The server holds the truth: each profile has a list of unclaimed tokens and a record of claimed ones. Dropping, copying or handing over the item gives nobody anything; a lost item is given again on next login while the claim is open.
- Right-click opens the picker: every option with a live preview on the player's own model, the set bonus text, and a confirm step that says the choice is permanent.
- Claiming is one server transaction: check owner and unclaimed, grant the bundle, mark claimed, remove the item, log it.
- Giving: to one player, to everyone with a tag, or to everyone. Offline players receive it on next login. A token type may have a claim window (limited-time rewards).
- A player holds at most one unclaimed token of a type.

## 8. Phase C: the four OG sets

Kort's rulings
- Sets and their meaning, in Kort's words:
  - **Glitch**: purple and black, glitching. For those who were here when things were still glitching.
  - **Debug**: black and green. For those who were here when the bugs were many.
  - **Redstone**: redstone themed. For those who were here when the redstone was being laid.
  - **Builder**: builder and mechanic themes. For those who were here before the blocks were placed.
- Each set is four armour pieces: helm, chest, legs, boots. No weapon.
- Real gear with stats, equal to the best normal sets, never above.
- Same base stats across the four; each has its own set bonus, of equal strength.
- Animated looks.
- Permanent and unbreakable: no breakdown, no trade, no wear, never rolled by the Forge.

Design **(call)**
- Sixteen new rows in the gear items sheet and four in the gear sets sheet, through `sheets/design.json`. Pieces are granted at Legendary rarity, quality 100.
- A new `exclusive` flag on the gear item: the Forge never rolls it, breakdown and repair refuse it, durability never drops, and it does not count against inventory size.
- Role. Kort's ruling: the player chooses the role. Today a commander's role comes from the set with the most worn pieces (`GearStats.of`); an OG set carries no fixed role or class limit, and its wearer picks which role it counts as. Any commander can wear any OG set.
- Base stats copy the strongest existing Legendary armour piece of each slot, measured by gear power. Whether OG pieces carry Legendary perks is settled in the phase C plan so that set plus perks equals, and never exceeds, the best normal set.
- Set bonuses. Kort's ruling: new themed bonuses, not re-skinned existing ones. Each needs new combat code (`GearStats` and `GearEffects` know only a fixed list of effects today). They are proposed in the phase C plan and approved by Kort before they are built; each is checked in the balance sim against the best existing set and must land within the sim's normal tolerance.
- The `exclusive` flag must hold everywhere gear is handled, including `Forge.roll`'s item pool and `Forge.breakdownDuplicates`, and reaches the client so the Arsenal can show the piece as locked (a protocol change, gated like any other).
- Proposed bonus directions, for Kort to accept or replace: Glitch, a chance for a hit on the commander to miss; Debug, the commander sees enemy health and takes less damage from the troop type that last hit them; Redstone, a short speed burst after giving an order; Builder, barricades and props near the commander are tougher.
- Art: each set gets its own commander armour model and texture in the existing gear art pipeline. Animation by set: Glitch flickers and tears in bands; Debug scrolls green text across the plates; Redstone pulses and throws sparks; Builder has turning gears. Art follows the project's existing review gates and Kort approves each set's look before it ships.
- Wearing a full OG set grants nothing beyond its set bonus.

## 9. Phase D: events

Kort's ruling: timed bonuses, gifts to everyone, announcements, limited-time rewards.

Design **(call)**
- Timed bonus: a multiplier on gear shards, Renown or troop writs earned, with start and end times. Bonuses do not stack; the highest applies. Active bonuses show to players in the camp.
- Gift: a token or currency sent to everyone, or to everyone with a tag, claimed on login (uses section 7).
- Announcement: a message sent now or at a set time, shown in chat and as a banner.
- Limited-time reward: a token type with a claim window (section 7).
- Events are created, edited and ended from the Events tab, owner only, and each start, change and end is logged.

## 10. Constraints

- Never hand-edit `Spec.java`; all catalogue data goes through `sheets/design.json`.
- The server never trusts the client: every admin and claim request is parsed defensively and re-checked on the server, as `GearCommands` does today.
- Admin data never reaches a non-admin client.
- A profile saved by a newer build must still not be overwritten by an older one (existing schema rule).
- Sonnet-built code gets an Opus high review; art follows the art routing and review gates in docs/WORKFLOW.md.

## 11. Left to the builder

File layout and class names, packet shapes, screen layout within the UI kit, the log file's rotation, the exact command syntax beyond section 4, and how previews are drawn.

## 12. Open for Kort

1. The set bonus for each OG set (section 8 proposes one each).
2. The OG window dates, once the launch date is known.
3. Whether testers from before launch are covered by the window or added by hand.
