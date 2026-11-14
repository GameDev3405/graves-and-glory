#!/usr/bin/env python3
"""Builds Kort's design page (drop-board-page.html) with the mock-ups as JPEG data URIs."""
import base64
from pathlib import Path

D = Path(__file__).resolve().parent


def img(name, alt):
    b = base64.b64encode((D / name).read_bytes()).decode()
    return f'<img src="data:image/jpeg;base64,{b}" alt="{alt}">'


def table(head, rows, bold_last=False):
    out = ['<div class="scroll"><table><thead><tr>' + "".join(f"<th>{h}</th>" for h in head) + "</tr></thead><tbody>"]
    for i, r in enumerate(rows):
        cls = ' class="sum"' if bold_last and i >= len(rows) - 2 else ""
        out.append(f"<tr{cls}>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>")
    out.append("</tbody></table></div>")
    return "".join(out)


GEAR = [["x1", "30%", "57.97%", "32.46%", "6.52%", "2.61%", "0.43%"], ["x2", "40%", "51.84%", "29.03%", "13.04%", "5.22%", "0.87%"],
        ["x3", "20%", "45.71%", "25.60%", "19.57%", "7.83%", "1.30%"], ["x5", "8%", "33.44%", "18.73%", "32.61%", "13.04%", "2.17%"],
        ["x10", "2%", "2.79%", "1.56%", "65.22%", "26.09%", "4.35%"], ["Average with the board", "", "50.00%", "28.00%", "15.00%", "6.00%", "1.00%"],
        ["Today", "", "50.00%", "28.00%", "15.00%", "6.00%", "1.00%"]]
UNITS = [["x1", "30%", "93.91%", "5.22%", "0.87%"], ["x2", "40%", "87.83%", "10.43%", "1.74%"], ["x3", "20%", "81.74%", "15.65%", "2.61%"],
         ["x5", "8%", "69.57%", "26.09%", "4.35%"], ["x10", "2%", "39.13%", "52.17%", "8.70%"], ["Average with the board", "", "86.00%", "12.00%", "2.00%"],
         ["Today", "", "86.00%", "12.00%", "2.00%"]]
SLOTS = [["A bell (my pick)", "1%", "4%", "10%", "20%", "30%", "20%", "10%", "4%", "1%", "0.43x"],
         ["B wild", "2%", "6%", "12%", "20%", "20%", "20%", "12%", "6%", "2%", "0.37x"]]
SEQ = [["Fly in", "1.6 s", "The War Table closes and the camera glides to the altar. Braziers flare, stone rumbles."],
       ["Aim", "up to 8 s", "The coin follows your mouse along the rail. Click drops it. A ring drains; at zero it drops by itself."],
       ["Drop", "about 5 s", "Real bounces through the pegs. Every hit rings and sparks. The camera drifts down with the coin."],
       ["Flare", "1 s", "The slot's rune flares in its colour and the others dim. x5 and x10 hit harder; x10 sounds a horn for the whole lobby."],
       ["Reveal", "2.5 s", "The troop or piece rises from the slot in its rarity colour with its name and a sting."],
       ["Return", "0.8 s", "Back to the War Table, on the result."]]

HTML = f"""<title>Drop Board Design</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600&family=Inter:wght@400;600&display=swap">
<style>
/* layout: one reading column, pictures full width, tables scroll inside themselves */
:root {{ --bg:#f3f1f6; --fg:#1d1a26; --muted:#5b566b; --line:#d3cee0; --panel:#ffffff; --accent:#6a3fc4; --bronze:#8a5a1c;
  --display:"Cinzel",Georgia,serif; --body:"Inter",system-ui,sans-serif; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --bg:#14121b; --fg:#ebe6f5; --muted:#a79fbd; --line:#342e47; --panel:#1d1a28; --accent:#b99bff; --bronze:#d6aa60; color-scheme:dark }} }}
:root[data-theme="dark"] {{ --bg:#14121b; --fg:#ebe6f5; --muted:#a79fbd; --line:#342e47; --panel:#1d1a28; --accent:#b99bff; --bronze:#d6aa60; color-scheme:dark }}
body {{ background:var(--bg); color:var(--fg); font:16px/1.6 var(--body); padding-block:32px;
  padding-left:max(16px, env(safe-area-inset-left, 0px)); padding-right:max(16px, env(safe-area-inset-right, 0px)); }}
main {{ max-width:900px; margin:0 auto; display:flex; flex-direction:column; gap:20px; }}
h1 {{ font:600 2rem/1.2 var(--display); margin:0; text-wrap:balance; }}
h2 {{ font:600 1.25rem/1.3 var(--display); margin:20px 0 0; color:var(--bronze); text-wrap:balance; }}
p, ul {{ margin:0; max-width:68ch; }}
.lead {{ color:var(--muted); }}
img {{ display:block; width:100%; border:1px solid var(--line); border-radius:4px; image-rendering:pixelated; }}
.scroll {{ overflow-x:auto; }}
table {{ border-collapse:collapse; width:100%; font-variant-numeric:tabular-nums; font-size:.92rem; background:var(--panel); }}
th, td {{ padding:7px 10px; border-bottom:1px solid var(--line); text-align:right; white-space:nowrap; }}
th:first-child, td:first-child {{ text-align:left; }}
td.wrap, .seq td:last-child {{ white-space:normal; text-align:left; }}
.seq td {{ text-align:left; }}
th {{ color:var(--muted); font-weight:600; }}
tr.sum td {{ font-weight:600; color:var(--accent); }}
.q {{ border-left:3px solid var(--accent); padding:2px 0 2px 14px; }}
.q b {{ color:var(--accent); }}
</style>
<main>
<h1>The Drop Board</h1>
<p class="lead">Design for the new roll moment, for your approval. Pictures are concept mock-ups, not game art. Six choices are marked; I ask them in the session.</p>

<h2>The altar</h2>
<p>A giant relic of stone and bronze with a glimpur crystal in its lintel, bone pegs and nine rune slots. It is never named. Each slot shows its giant numeral with the plain multiplier under it. Slot colours climb from ash to white gold and stay apart from the rarity colours, which only the reveal uses.</p>
{img("board.jpg", "The altar board at giant size with a troop for scale")}
<p class="q"><b>Choice 1, size.</b> Giant (11 by 13 blocks, about six troops tall) or large (7 by 9).</p>
{img("size.jpg", "Giant and large board sizes side by side")}

<h2>The moment</h2>
{img("storyboard.jpg", "Four steps: aim, drop, flare, reveal")}
<div class="scroll"><table class="seq"><thead><tr><th>Step</th><th>Time</th><th>What you see</th></tr></thead><tbody>
{"".join(f"<tr><td>{a}</td><td>{b}</td><td>{c}</td></tr>" for a, b, c in SEQ)}</tbody></table></div>
<p>About 11 seconds in all. A click, Escape or the menu key skips to the result at any point after the drop starts. Motion is drawn every frame from the server's path, so it stays smooth at any frame rate.</p>
<p class="q"><b>Choice 2, timing.</b> Standard (5 s drop, 8 s to aim) or short (4 s drop, 5 s to aim, about 9 s in all).</p>

<h2>Fair, and decided by the server</h2>
<ul>
<li>When you pay, the server draws the slot and the item at once and saves the result. Nothing after that can change it, so a disconnect never loses or doubles a roll.</li>
<li>The bounces are real physics run on the server from where you placed the coin. It re-runs them with fresh randomness until the coin ends in the slot it drew. Every start point has exactly the same odds. There is no trick spot.</li>
</ul>

<h2>The odds</h2>
<p>The multiplier multiplies the chance of Rare and above for that roll. To keep the average the same, an x1 has to give less than today, and by more than a little: about 0.43 of today's chance. x2 is close to today; x3 and up are better.</p>
{table(["Table", "x10", "x5", "x3", "x2", "x1", "x2", "x3", "x5", "x10", "x1 gives"], SLOTS)}
<p>A gentler table than A cannot work: an x10 on gear would pass 100% Rare or better.</p>
<p class="q"><b>Choice 3, slot table.</b> A bell: x10 is 1 roll in 50. B wild: x10 is 1 in 25, and the middle slots pay less.</p>

<h2>Proof the average is unchanged</h2>
<p>Gear with table A. Each row is what a roll gives after landing in that slot.</p>
{table(["Slot", "Chance", "Common", "Uncommon", "Rare", "Epic", "Legendary"], GEAR, True)}
<p>Units with table A, the same for every House.</p>
{table(["Slot", "Chance", "Common", "Rare", "Epic"], UNITS, True)}
<p>The match is exact (worked in fractions, checked by a test). Pity is untouched: gear still gives an Epic or better every 7.86 rolls on average and units an Epic every 27.7. Over many rolls you get exactly what you get today. The board changes how one roll feels, not what a hundred rolls give.</p>

<h2>Where it stands</h2>
{img("lobby.jpg", "Lobby plan with three candidate spots")}
<p class="q"><b>Choice 4, spot.</b> A east of the hearth court (my pick: seen from the War Table, players can walk up and watch). B beyond the graveyard. C its own stage off the island, like the podium. The maps lead agrees the blocks; the spot is one row in the sheet, so it moves with the Old Hold.</p>

<h2>Other players</h2>
<ul>
<li>Anyone near the altar sees the coin fall, the flare and the item rise, with the roller's name.</li>
<li>An x10 or a Legendary sends one line and one sound to the whole lobby.</li>
</ul>
<p class="q"><b>Choice 5, two rolling at once.</b> No queue (my pick): each roller sees his own drop at once; watchers see every coin, named. Or a queue: one drop at a time, everyone watches each.</p>
<p class="q"><b>Choice 6, relic rolls.</b> Relics have no rarity. Either the board plays and the multiplier scales the refund for a relic you already own (average unchanged), or relic rolls keep today's plain card.</p>

<h2>Sounds</h2>
<p>Free recordings only, as before: stone rumble, coin on bronze, bone peg ticks that rise in pitch with speed, a flare chime per multiplier, a horn for x10, and rarity stings. All credited.</p>
</main>
"""
(D / "drop-board-page.html").write_text(HTML, encoding="utf-8")
print("ok", len(HTML) // 1024, "KB")
