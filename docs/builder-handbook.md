# Builder handbook text (plan task B2)

Source text for `HandbookText.pages(...)`. Each `---` is one book page; a Minecraft book page holds about 14 lines of
19 characters, so pages are short.

Book title "Builder Handbook" (vanilla caps titles at 32 characters). Kort, 2026-11-08: tools only (no lore, no What-is-G&G page, no rules page, no map briefs). Approved by Kort as written, 2026-11-08.
The world list on the `/map` page comes from `BuildWorlds.ids()` at run time.

Keys are written as `{key:<binding>}`: the book shows the reader's own binding (the UI standard forbids hardcoded keys). Defaults: `key.use` Use (right mouse), `key.attack` Attack (left mouse), `key.voice_chat` V, `key.voice_chat_group` G, `key.disable_voice_chat` N, `key.mute_microphone` M, `gui.xaero_enlarge_map` Z, `gui.xaero_new_waypoint` B, `gui.xaero_waypoints_key` U, `gui.xaero_open_map` M.
The shipped text is `HandbookText.java`; this page mirrors it.

## Pages

BUILDER HANDBOOK

How to use the builder tools.

1. Moving around
2. Props
3. Axiom
4. WorldEdit
5. Voice chat
6. Maps

---

MOVING AROUND

/map lists the worlds and says which one you are in.

/map <name> takes you to that world.

---

Worlds:
{worlds}

---

/warp lists the places in the world you are in.

/warp <name> takes you there.

You log back in where you left.

/handbook gives you a new copy of this book.

---

PROPS

Props are ready-made pieces such as the forge and the anvil.

/prop opens the prop menu. Pick a prop from it.

---

A ghost shows where it will go.

Scroll to turn it.

{key:key.use} places it.

{key:key.attack} on a placed prop removes it.

---

Props are solid: troops walk around them and arrows hit them. Leave room to pass.

---

AXIOM

The Axiom editor key opens the editor and closes it again.

Use the selection tool to drag a box over a build.

---

Copy and paste the selection. Before placing a paste, use the gizmo to move, rotate or flip it.

Undo works as in any editor.

---

The blueprint browser keeps builds you save so you can place them again in any world.

---

WORLDEDIT

The wooden axe is the wand.

{key:key.attack} sets position 1.

{key:key.use} sets position 2.

---

//set <block> fills the box.

//replace <from> <to>

//copy, then //paste where you stand.

//rotate 90 turns the copy.

//undo, //redo

---

VOICE CHAT

The first time, {key:key.voice_chat} sets it up: push to talk (and its key) or voice activation.

Players near you hear you.

---

{key:key.voice_chat_group}: groups. Make or join one to talk to its members anywhere.

{key:key.disable_voice_chat}: voice chat off and on.

{key:key.voice_chat}: settings.

---

MAPS

The minimap sits in the corner. {key:gui.xaero_enlarge_map} makes it bigger.

{key:gui.xaero_new_waypoint} marks a waypoint. {key:gui.xaero_waypoints_key} lists them.

---

{key:gui.xaero_open_map}: the world map.

If voice chat mute ({key:key.mute_microphone}) is the same key, move one in Options, Controls, Key Binds.
