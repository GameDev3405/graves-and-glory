# Armoury restyle and War Table 1440p scale (2026-10-31)

Branch `work/armoury-restyle`, cut from `work/lobby-six` at d625a4b. Local commits only. Nothing was pushed or merged.

## Kort's rulings and what changed

1. **The Armoury matches the troop sheet.** `ArmouryTab` is rewritten on the War Table's own textures.
   - It draws on the oak with the side rail.
   - The left paper (`table/note-room`) lists the banner's troop types. Each row has a portrait in a frame edged in its House's accent colour, the House seal, the troop's name and its kind line.
   - The right paper (`table/sheet-field`) is a ledger with the four buys on each troop's row: Weapons II and III, Armour II and III.
   - Each buy state shows a word plus a glyph or shape, never colour alone:
     - Open buys are vellum tags in ink, underlined when hovered.
     - Unaffordable buys and buys away from a buy place are flat greyed boxes in sepia.
     - Bought buys are paper with a check.
     - Locked buys are paper with the brass padlock.
   - When the banner has more troops than fit, triangle arrows scroll the rows and the subtitle reads "Troops a-b of n".
   - The rail shows Back, Barracks and Armoury plaques. The Armoury plaque is marked as the current page.
   - The Barracks tab (tab 0) is unchanged.
2. **Every War Table page draws at the bigger scale.** `UiCanvas` and `CampScreen.fillsWindow` now cover all War Table pages, including the Armoury.
   - The scale is 3x at 2560x1440, 2x at 1920x1080 and 1x in small windows, and never above the player's GUI Scale.
   - The rail is the same size on every page.
   - Drawing is crisp: one texel per pixel at whole-number scales, with no filtered scaling.
3. **Legible Ready button.** In `WarTableLobby.plate`, the dull (not-ready) label ink is now `MUTED`.

`UiButton` gained a `paint(...)` hook, so the Armoury's buttons draw themselves. There are no gameplay, save or protocol changes: the page still only reads the server's buy states and sends the same `gear:` command.

## Checks

- **LobbyRulesCheck:** 107 checks passed.
- **UiMathCheck:** 171 checks passed.
- **Build and `compileLocalTestTools`:** clean.
- **Armoury game suite (`armoury`):** all checks pass.
  - The fit checks run at six sizes: 854x480, 1280x720, 1920x974, 1920x1080 at GUI 1 and GUI 2, and 2560x1440 (k=3).
  - They also cover contrast, scroll, back and tabs.
- **ui-lobby:** all checks pass, including the new `p1-lobby-1440`, `p1-lobby-troop-1440` and `p1-lobby-troop-1080`.
- **ui-pages:** every check passes except **`p1-ui-page-chapter` at the new 2560x1440 step** (see Open issues).
- **`python tools/verify.py fast --offline`:** preflight, python-tests, ui-standard, ui-assets and skin-assets pass. The compile and game stage deferred because another Minecraft client was running machine-wide (game_guard), and the client was not stopped. That stage was covered by the Gradle build and the harness suites above.

The contact sheet is at `work/armoury/sheet.png` (gitignored). It shows the Armoury, the lobby, the troop sheet and the Banner page, each at 1080p and 1440p. Each shot is reduced by a whole factor using nearest-neighbour.

## Open issues (stopped at the two-fix-round limit)

- **Chapter page at 2560x1440.** The new check requires every box to sit 12 px inside the canvas. At 1440p the canvas is 853x480, and two of the Chapter page's boxes break the margin:
  - `xp-rail` at [472,146,370,23];
  - the "later" widget at [827,273,15,15].

  The same page passes at 854x480 and in `p1-ui-page-scales`, so it is 1 px of margin at the 853-wide canvas. It is not visibly clipped. A likely fix is to inset the Chapter page's right column by a few pixels, or to measure the margin against the compact page rather than the canvas.
- **Paper heights on the Armoury.** The note paper (373 px tall) runs about 48 px lower than the sheet (325 px). The shorter `note-room-short` (305 px) would match only if the key line moved onto the sheet. This is cosmetic.

## Questions for Kort

1. Should the Armoury's left note be shortened to match the sheet's height, moving the "U opens the Armoury" key line onto the sheet?
2. Is a 1 px margin miss on the Chapter page at 1440p worth a layout tweak, or should the check's margin be relaxed there?
