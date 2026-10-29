// Game facts shown on the interaction cards. Troop numbers come from docs/superpowers/specs/2026-10-17-houses-design.md.
export const RARITY = { C: ['Common', '#c8cdd0'], R: ['Rare', '#4aa0ff'], E: ['Epic', '#b070ff'], L: ['Legendary', '#ffb030'] };
// id: [house, name, rarity, role, mob, hp, dmg, range, cd, speed, cost, train, ability]
const T = (house, name, r, role, mob, hp, dmg, range, cd, speed, cost, train, ability) => ({ house, name, r, role, mob, hp, dmg, range, cd, speed, cost, train, ability });
export const TROOPS = {
  knight: T('gilded', 'Order Squire', 'C', 'Infantry', 'husk', 32, 5, 2.8, 25, 0.28, 22, 80, 'Shield: a raised shield blocks most frontal melee.'),
  archer: T('gilded', 'Order Archer', 'C', 'Ranged', 'husk', 20, 4, 22, 40, 0.30, 24, 80, 'Steady: +20% damage while the group holds.'),
  sentinel: T('gilded', 'Templar', 'R', 'Infantry', 'husk', 46, 6, 2.8, 28, 0.25, 48, 140, 'Shield: the wall other Houses are built to break.'),
  longbow: T('gilded', 'Order Longbow', 'R', 'Ranged', 'husk', 18, 6, 30, 45, 0.28, 46, 140, 'Pierce: each shot also hits the enemy behind for 50%.'),
  mage: T('gilded', 'Sunweaver', 'E', 'Support', 'husk', 24, 0, 18, 55, 0.29, 70, 180, 'Mend: heals 5 HP in a 10-block radius.'),
  gl_marshal: T('gilded', 'Lord Marshal Aldric', 'L', 'Infantry', 'husk', 80, 9, 2.8, 22, 0.27, 140, 300, 'Oath Banner: on Hold, own groups within 12 cannot rout and deal +10%.'),
  revenant: T('graveborn', 'Risen', 'C', 'Infantry', 'zombie', 26, 4, 2.8, 25, 0.25, 14, 60, 'Fearless: groups of Risen rout far less easily.'),
  bonebow: T('graveborn', 'Bone Archer', 'C', 'Ranged', 'skeleton', 18, 4, 22, 40, 0.27, 16, 60, 'Fearless.'),
  reaper: T('graveborn', 'Grave Reaper', 'R', 'Infantry', 'wither skeleton', 34, 8, 2.8, 30, 0.30, 40, 120, 'Cleave: each hit also strikes a second enemy for 50%.'),
  stalker: T('graveborn', 'Barrow Stalker', 'R', 'Ranged', 'stray', 20, 5, 26, 40, 0.30, 40, 120, 'Frostshot: hits apply Slowness I.'),
  hexer: T('graveborn', 'Gravecaller', 'E', 'Support', 'wither skeleton', 22, 7, 18, 55, 0.27, 60, 160, 'Blight: marks an area, then a damaging, slowing spell lands.'),
  gb_hollowking: T('graveborn', 'The Hollow King', 'L', 'Infantry', 'wither skeleton', 70, 10, 3.0, 30, 0.26, 120, 300, 'Dread: enemies within 10 deal -15% and rout more easily.'),
  wm_hound: T('wildmoot', 'Moot Hound', 'C', 'Cavalry-like', 'wolf', 18, 4, 2.2, 20, 0.36, 16, 60, 'Pounce: x1.8 on the first hit after closing 6+ blocks.'),
  wm_hunter: T('wildmoot', 'Hide Hunter', 'C', 'Ranged', 'zombified piglin', 18, 4, 20, 36, 0.31, 20, 70, 'Ambush: +25% damage when firing from under cover.'),
  wm_stalker: T('wildmoot', 'Web Stalker', 'R', 'Cavalry-like', 'spider', 28, 6, 2.6, 25, 0.34, 40, 120, 'Ensnare: hits apply Slowness II. Climbs walls.'),
  wm_tusk: T('wildmoot', 'Tusk Spear', 'R', 'Infantry (spear)', 'zombified piglin', 30, 6, 3.0, 28, 0.31, 38, 120, 'Brace: x1.3 against a charging group while holding.'),
  wm_seer: T('wildmoot', 'Bloodhowl Seer', 'E', 'Support', 'zombified piglin', 24, 3, 14, 40, 0.31, 60, 160, 'Howl: nearby troops get +15% damage and speed.'),
  wm_oldhorn: T('wildmoot', 'Old Horn, the Antlered Alpha', 'L', 'Cavalry-like', 'polar bear', 90, 11, 3.0, 30, 0.30, 130, 300, 'Trample: a charge hits everything within 3 blocks.'),
  ic_hearthguard: T('ironclad', 'Hearthguard', 'C', 'Infantry', 'vindicator', 34, 5, 2.8, 28, 0.24, 22, 90, 'Shield.'),
  ic_arbalist: T('ironclad', 'Arbalist', 'C', 'Ranged', 'pillager', 20, 6, 24, 60, 0.25, 24, 90, 'Armour Bolt: shields block it far less.'),
  ic_breaker: T('ironclad', 'Shieldbreaker', 'R', 'Infantry', 'vindicator', 38, 8, 2.8, 32, 0.25, 44, 140, 'Sunder: ignores shields, x1.4 against them.'),
  ic_ballista: T('ironclad', 'Ballista Crew', 'R', 'Siege', 'pillager', 24, 10, 34, 80, 0.20, 48, 160, 'Pierce: bolts punch through to a second target.'),
  ic_mortar: T('ironclad', 'Ironhold Mortar', 'E', 'Siege', 'pillager', 22, 9, 36, 100, 0.18, 70, 200, 'Shell: area blast, cannot hit anything closer than 10.'),
  ic_colossus: T('ironclad', 'Brassjaw, the Forge Colossus', 'L', 'Infantry', 'iron golem', 120, 12, 3.0, 36, 0.22, 150, 340, 'Forge Slam: every hit also strikes all enemies within 2.5. Fearless.'),
  tc_raider: T('tidecallers', 'Drowned Raider', 'C', 'Infantry', 'drowned', 28, 5, 2.8, 24, 0.29, 18, 70, 'Brine: hits apply Slowness I.'),
  tc_harpooner: T('tidecallers', 'Harpooner', 'C', 'Ranged', 'drowned', 20, 5, 16, 40, 0.29, 20, 70, 'Harpoon: pulls the target a block toward the shooter.'),
  tc_tideguard: T('tidecallers', 'Trident Guard', 'R', 'Infantry (spear)', 'drowned', 34, 7, 3.0, 28, 0.28, 42, 130, 'Brace.'),
  tc_mist: T('tidecallers', 'Mist Caller', 'R', 'Support', 'evoker', 22, 0, 18, 60, 0.28, 46, 140, 'Mist: a 4-block cloud that slows enemies and halves ranged hits through it.'),
  tc_brute: T('tidecallers', 'Kraken-touched Brute', 'E', 'Cavalry-like', 'ravager', 70, 9, 3.0, 34, 0.28, 75, 200, 'Tentacle Sweep: hits also strike enemies within 3 blocks.'),
  tc_admiral: T('tidecallers', 'Admiral Vael, the Drowned Tide', 'L', 'Infantry', 'drowned', 64, 9, 3.0, 26, 0.30, 130, 300, 'Undertow: every 200 ticks pulls enemies within 7 blocks toward her.'),
};
export const HOUSE_NAMES = { gilded: 'Gilded Order', graveborn: 'Graveborn', wildmoot: 'Wildmoot', ironclad: 'Ironclad', tidecallers: 'Tidecallers' };

export const NPCS = {
  quartermaster: { name: 'The Quartermaster', role: 'Army & shop', station: 'Armory', desc: 'Opens the army page: arrange your twenty deployment slots, equip troops, spend gold.', lines: ['Twenty slots. Fill them how you like. I only keep the iron from rusting.', 'Nobody has ever asked me for a ratio. Take what you can carry and I will find a place for it.'] },
  recruiter: { name: 'The Recruiter', role: 'New troops', station: 'The Muster', desc: 'Runs the Roll: 100 gold unlocks one troop you do not own, equal odds, no duplicates, no real money. Also spends Legendary Pacts.', lines: ['A roll costs a hundred. It never gives you what you already have.', 'The seal on a pact is the crown\'s. I only read what it names.'] },
  pell: { name: 'Pell, the Archivist', role: 'Daily & weekly contracts', station: 'The Archive', desc: 'Keeps the contract boards (claim gold and XP) and the Founders\' Stone. The roll of the dead is kept here, one name at a time.', lines: ['A pact is the crown\'s seal on paper. It names a captain and gives them a standard.', 'I copied the Stone by lantern light, nineteen years ago. I am fairly sure I got it right.', 'I write them as they come. The road has not let me stop.'] },
  steward: { name: 'The Steward', role: 'Parties & rooms', station: 'War Gates · east tower', desc: 'Invite an online friend, accept, then create or join a private room by code. Parties hold up to four and move teams together; they never silently split.', lines: ['Four to a party. Nobody is split without being asked.', 'Give them the code. The gates open when both sides stand ready.'] },
  drillmaster: { name: 'The Drillmaster', role: 'Training', station: 'Training Yard', desc: 'Teaches command with four lessons that name your own keys, then sends you through the START arch to campaign battle 1, The Training Field.', lines: ['R for the council. Pick a group, then give it an order. Hold means hold.', 'The road west is shut. Learn the keys before you open it.'] },
  maud: { name: 'Ser Maud Harrow', role: 'The Long March', station: 'Command Pavilion', desc: 'Sergeant of the Hollow. Her camp card introduces the Long March; she counts your troops out loud after every battle. The storybook lies on the table beside her.', lines: ['Two names above yours, both scraped. The second was my knife. That makes you Third.', 'The Hollow carry the standard until there is a living hand for the crown. Then the dead can rest.', 'Count them before the horn. I always do.'] },
  wren: { name: 'Wren', role: 'Companion · Graveborn archer', station: 'The Companions\' Fire', desc: 'Joins after battle 2. She is trying to remember how she died. Her memory advances every time the company returns to camp.', lines: ['Hollow. Like him.', 'I remember a stair. Stone, cold. I do not remember going up it.'] },
  dunmar: { name: 'Dunmar Coalbright', role: 'Companion · Ironclad shieldbreaker', station: 'The Companions\' Fire', desc: 'Joins after battle 3. Sealed the hold with his wife inside. Wants to open his own gate and let the dead out, not in.', lines: ['Slow. Every word placed, or it does not hold.', 'Six minutes to break the gate. I have taken longer to say no.'] },
  ashka: { name: 'Ashka of the Thornwood', role: 'Companion · Wildmoot hunter', station: 'The Companions\' Fire', desc: 'Joins after battle 5. Her brother Tarn was the toll eleven years ago, and now he walks. Her silence choices are the story\'s only branches.', lines: ['The woods let nothing through that has not bled for it. Neither will I.', 'Eat. You look like the sort who forgets.'] },
};
export const CLASSES = {
  champion: ['Champion', 'Hero track', 'Sword and shield. Active: Stand Fast, nearby troops stop routing and gain armour for 8 s.'],
  reaver: ['Reaver', 'Hero track', 'Two-hander, fast. Active: Bloodrush, speed and lifesteal; kills refund a reinforcement.'],
  warden: ['Warden', 'Hero track', 'Spear and bow. Active: Mark Prey, a target takes extra damage from all your troops.'],
  marshal: ['Marshal', 'General track', 'Active: Rally Horn, all groups regroup on you and gain morale. Passive: faster training.'],
  warlock: ['Warlock', 'General track', 'Active: Grave Bloom, raises fallen troops in an area as temporary Graveborn. Passive: casters cost less.'],
  quartermaster: ['Quartermaster', 'General track', 'Active: Forward Supply, drops a supply banner that acts as a reinforcement point. Passive: more gold from objectives.'],
};
