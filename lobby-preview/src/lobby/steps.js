import { buildCourt } from './court.js';
import { buildIronclad } from './ironclad.js';
import { buildGilded } from './gilded.js';
import { buildWildmoot } from './wildmoot.js';
import { buildTide } from './tide.js';
import { buildGrave } from './grave.js';
import { buildCommand } from './command.js';
import { buildArmory, buildMuster, buildArchive } from './services.js';
import { buildYard, buildGates } from './gates.js';
import { buildValley } from './dress.js';
export const STEPS = [
  ['Building the court and the Founders\' Stair', buildCourt],
  ['Forging the Ironclad Hold', buildIronclad],
  ['Raising the Gilded Oathhall', buildGilded],
  ['Pitching the Wildmoot lodge', buildWildmoot],
  ['Flooding the Tidepost', buildTide],
  ['Opening the Barrow Shrine', buildGrave],
  ['Pitching the Command Pavilion', buildCommand],
  ['Stocking the Armory', buildArmory],
  ['Raising the Muster', buildMuster],
  ['Shelving the Archive', buildArchive],
  ['Marking out the Training Yard', buildYard],
  ['Raising the War Gates', buildGates],
  ['Planting the woods and laying the road', buildValley],
];
