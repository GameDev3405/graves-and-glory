# In-game UI editor: design

Date: 2026-11-14. Source: Kort's request and his answers by AskUserQuestion (status/DECISIONS-2026-11-09.md, "2026-11-14: UI editor"). Lines marked **(call)** are the coordinator's choices, not Kort's words; Kort can overrule any of them.

## 1. Why

Kort wants to change the game's wording and arrange its screens himself, in the game, without going into the code or asking for a build. In his words: he types a command, and from then on, on any UI screen, he can click the wording and type what he wants, and click and drag the pieces to move them.

Success looks like: Kort opens the editor, picks a screen, retypes a line, drags a panel, presses Publish, and every player sees it.

## 2. Where the game stands today

Checked on main 5f6f1a12.
- UI wording comes from three places. Most is written directly in the code: several hundred text draw calls across about 30 files (the screens CampScreen, RecruitmentScreen, EndingScreen, FrontMenuScreen, FailureScreen; the battle HUD; BattleMapScreen, PropMenu, PresentationScreen, CommandPanel, BuildMode). Some comes from the design sheet (troop, House, faction, map and formation names). Some is sent by the server (notices, feed rows). Almost nothing uses translation keys.
- Every position and size is computed in code. No screen has named, addressable pieces.
- Text and layout depend on each other in places: chips laid out by a running cursor, buttons sized from their label, text centred on its measured width, long wording clipped with "..." (`UiMath.trim`).
- Screens do not share one scaling rule: most use `UiCanvas` (page or pixel scale), the HUD has its own scale plus a per-player size setting (Compact, Standard, Large), the War Table switches to a separate compact layout below 960x540, the main menu and parts of the recruit screen use older scaling.
- Some text colour is driven by game state (rout, last stand, alarm) or fades.
- A slot grammar already exists for key names: `{key:menu}` (`HintText`).
- One small precedent exists: the battle command panel can be dragged by each player, saved per player on their own machine (`PanelLayout`, `HudLayout`). It is a personal arrangement, not an edit for everyone.

So an editor cannot be bolted on: the screens must first be rebuilt so that every text line, panel, button and picture is a named piece that reads its words, place, size and style from data. That conversion is most of the work.

## 3. Goals, non-goals, anti-goals

Goals
- Kort, and only Kort, edits the UI in game.
- On every screen and the battle HUD: retype text, move pieces, resize, change text colour and style, hide or show a piece.
- Add new text labels and pictures (from art already in the game).
- Edits are a draft until Published; Published edits reach every player on his server live.
- Each release copies the published edits into the mod's built-in defaults.
- One layout that holds at every window size.
- A screen browser that opens any screen and state with sample data.

Non-goals
- No new buttons or anything with behaviour; those still come from code.
- No pictures from Kort's PC; only art already in the mod.
- No translation into other languages.
- No per-player UI customising beyond what exists today.
- No editing of vanilla Minecraft screens.

Anti-goals (these make it a failure even if it works)
- Anyone but an owner changing what players see.
- A published edit that leaves players with an unusable screen and no quick way back.
- A layout that looks right on Kort's screen and breaks at another window size without the editor telling him.
- An edit that silently disappears, or crashes a screen, when the code behind a piece later changes.
- A live number ("Wave 3 of 5") turning into frozen text.

## 4. Kort's rulings

- Who sees edits: everyone, live on the server.
- Draft, then Publish; with undo, history and reset.
- Published edits are baked into the mod's defaults each release.
- Besides words and position: size and scale, text colour and style, hide or show.
- Text with live numbers: he edits the words, the numbers stay live.
- Every screen and the battle HUD.
- He can add text and images; images come from art already in the game.
- One layout that scales to every window size.
- A screen browser with sample data.
- Built after the 1.21.1 port; designed now.
- The main menu is editable; its edits reach players with the next release, since it shows before a player joins a server.
- HUD: Kort's published layout is the default; a player's own HUD and command panel arrangement stays on top of it.
- Positions are arranged separately for the normal and the compact layout; words, colours and hidden pieces are shared.
- Troop, House, faction and map names stay in the design sheet; the editor shows them as locked and says where they come from.

## 5. Design **(call, all of this section)**

### 5.1 Pieces

Every drawn thing on a mod screen becomes a **piece** with a stable id, such as `camp.header.title` or `hud.wave.counter`. A piece has a kind (text, panel, button, picture) and built-in defaults in code: its words, its place, its size, its style.

A screen draws a piece through one gate that applies the edit for that id, if any. No screen reads edits any other way.

A row that repeats (a list of troops, a list of players) is one piece used as a template: editing one row edits them all.

Things whose parts are laid out against each other (a row of key chips, a House mark with its name, a pouch with its sum) are one **group** piece: the group moves and scales as a whole and its inner layout stays in code. Each text inside a group is still editable as text.

Not editable, shown as locked in the editor: names and labels from the design sheet, text sent by the server, live 3D portraits, the map and its pins, code-drawn emblems. They can still be moved, scaled or hidden as a piece where that makes sense.

Essential controls (back, close, confirm, anything a player needs to leave a screen or finish a required step) can be moved and restyled but never hidden or moved off screen; each piece declares this in code.

### 5.2 What an edit can hold

Per piece id:
- `text`: the wording, as a template (5.3). Text pieces and button labels only.
- `dx`, `dy`: a move, measured from the piece's default place in the units of the screen's own scaled canvas, so it holds as that screen scales. Stored per layout family: one set for the normal layout, one for the compact layout (and per HUD size where the HUD's code layout differs). Converting the older-scaled screens (main menu, recruit) to the shared canvas is part of the conversion work.
- `scale`: within fixed limits.
- `color`, `bold`, `italic`, `shadow`: text pieces only. Where code changes a text's colour by game state, each state colour is a separately editable value on that piece; fades keep working on top of the edited colour.
- `hidden`.

Added pieces (new labels and pictures) are stored the same way with a generated id, a parent piece, and for pictures the id of an image from the mod's own art.

### 5.3 Text templates

A line with live values is written with named slots: `Wave {wave} of {total}`. The code supplies the values. In the editor Kort sees and edits the template; slots show as chips he can move within the line or delete.
- A slot name the piece does not offer is refused in the editor.
- Deleting a slot is allowed and the editor says which live value is no longer shown.
- Text is plain: formatting codes typed into it are shown as typed, never interpreted.
- The existing `{key:id}` slots for key names keep working inside the same grammar; `{{` and `}}` write literal braces.
- Text is never silently clipped by an edit: where a piece clips or wraps today, the editor shows the result at each preview size and warns (5.6). A piece has a length cap only as a safety limit.

### 5.4 Where edits live

- **Built-in defaults:** a file inside the mod. Every client and single-player world has it.
- **Server sheet:** `config/gravesandglory/ui-edits.json` on the server: the published edits, a version number, and the history of earlier published versions.
- **Draft:** kept on the server per owner, so it survives a restart and a crash.

A client shows: code defaults, then the mod's built-in edits, then the server's published edits, then (HUD and command panel only) the player's own arrangement on top. Screens shown before joining a server (the main menu) use the built-in edits only; Kort's edits to them are published to the sheet like any other and reach players at the next bake.

In single-player the same rules apply through the built-in server: Publish works when the player's account is in that world's `owners` list. The server sends its sheet when a player joins and again on each Publish, so a Publish shows within a moment for everyone online, without a restart.

### 5.5 Who may edit

The same owner gate as the admin tools (`owners` in `config/gravesandglory/admin.json`, by account uuid; vanilla operator status grants nothing). The server checks it on every editor request; the client-side editor being open is never the protection.

The server validates every edit before saving it: known piece id, known slot names, text length cap, scale and move within limits, image id exists in the mod. Clients treat the sheet they receive as untrusted and apply the same limits, so a damaged or hostile sheet cannot push a piece off screen or blow up a screen.

### 5.6 The editor

- Opened by a command (name left to Kort, for example `/gg ui edit`). The server refuses it for anyone who is not an owner.
- **Screen browser:** a list of every screen and each of its states (empty, full, error, each tab, each battle HUD state), opened with sample data, with a window-size switch (small, 1080p, 1440p, 4K) to preview the same layout at each size.
- **On a screen in edit mode:** hovering outlines a piece; click selects it; double-click a text piece to type; drag to move; handles to scale; a small side panel for colour, style, hide, reset this piece, and the piece's id.
- Arrow keys nudge. Snapping to neighbours and to a grid, switchable.
- Undo and redo within the draft.
- A list of hidden pieces, so a hidden piece can be found and shown again.
- While editing, the screen's own buttons do nothing; a hidden button also loses its click area and keyboard focus for players, and a moved button's click area moves with it.
- **Warnings** shown before Publish: text that overflows its panel at any of the preview sizes, pieces that overlap a button, pieces partly off screen at any size.
- **Publish** sends the draft to everyone and records a history entry (time, who, a one-line summary). **Discard** drops the draft. **Roll back** republishes any earlier version.
- The editor's own controls are not editable.

### 5.7 Way back

- `/gg ui reset` (owner or server console) returns the server to the mod's built-in defaults; the previous sheet stays in history.
- A malformed server sheet is ignored (the game runs on built-in defaults), kept as a copy, and never overwritten by a save. Same rule as `admin.json` today.

### 5.8 Baking into a release

A tool copies the server's published sheet into the mod's built-in defaults. It runs as a release step; the result is a normal change that Kort sees and approves like any other before it ships. After a bake, the server sheet holds only what differs from the new defaults.

### 5.9 When code changes

- Piece ids are permanent once shipped. Renaming one needs an entry in a rename table so existing edits follow it.
- An edit whose piece no longer exists is an **orphan**: ignored at run time, kept in the sheet, listed in the editor, and reported by a check so it is noticed before release.
- A check fails the build if two pieces share an id, or a text piece's default template names a slot the code does not supply.

### 5.10 Building order

After the 1.21.1 port lands. Three phases, each usable on its own:
1. **Words:** convert every screen's text to pieces with templates; editor with click-to-type, the screen browser, draft, Publish, history, live sync, bake.
2. **Layout and style:** move, scale, colour, style, hide; size preview and the warnings.
3. **Added pieces:** new labels and pictures.

Phase 1 carries the conversion of all text and is the largest. Each phase gets its own implementation plan.

## 6. Considered and dropped

- **Swapping text as it is drawn, by matching the old wording.** Quick to build and needs no screen conversion, but it cannot tell two identical words apart, breaks on any line with a live number, and gives no way to move anything. It fails three of Kort's rulings.
- **Moving everything into the standard language file.** Gives editable words through a resource pack, but not in game, not live, and no layout.

## 7. Constraints

- The server never trusts the client; the client never trusts the sheet.
- Edit data holds no code and no file paths; images are named by id from the mod's own art.
- Screens must look exactly as they do today when no edit exists: the conversion to pieces is checked screen by screen against today's screenshots.
- Protocol change (the sheet sync) is gated like any other.
- Sonnet-built code gets an Opus high review; UI work follows the art and UI routing and review gates in docs/WORKFLOW.md.

## 8. Left to the builder

Class and file names, the packet shapes, how pieces are declared in code, the exact scale and move limits, the snapping rules, how sample data for the browser is produced, the sheet's file format beyond 5.2, how history is capped.

## 9. Open for Kort

1. The command's name.
2. Whether a moderator may ever edit, or owners only forever (design assumes owners only).
3. How many published versions the history keeps (design assumes the last 50).
