# Graves and Glory 0.3.0 interface and depth

Implemented screens use the actual troops, classes, maps, prices, and server actions. Concept-image names, abilities, and costs were illustrative and were not substituted into gameplay.

- Battle setup: mode selection, four battlefield choices, commander list, faction joining, bot fill, host-only start, and actual battlefield preview captures.
- Class selection: large live equipped models, real class bonuses, explicit selection and confirmation.
- Barracks: selectable roster, costs and owned counts, live equipped model, health/damage/role, visible recruitment constraints, sequential training status, queue refunds, and separate disband action.
- Shared charcoal surfaces, brass highlights, Lato body text, Marcellus headings. Vanilla HUD is hidden only while a Graves and Glory menu is displayed. Controls retain keyboard focus and narration.

## Added depth

Original lightweight geometry adds infantry pauldrons, helmet crests, faction tabards and belt buckles. It follows the animated biped parts on named Graves and Glory troops and their menu previews. No simulation stats change.

The built-in `gravesandglory:armory` resource pack supplies original volumetric swords, axes, pickaxes, shovels and hoes for six vanilla material tiers. It is enabled above the world texture packs by the visual presets. Disable it in Minecraft's resource-pack screen to restore other item models; choosing a Graves and Glory texture preset enables it again. Bow and armor textures retain their existing resources.

The supplied shaders, EMF, ETF and Fresh Animations remain in use. The concept art is a visual direction, not a promise of identical lighting or bespoke character meshes.

## Research checked 2026-10-16

- Iris + Sodium: already installed. Iris supplies shader support; shaders improve lighting but do not turn flat tools into volumetric meshes. https://www.irisshaders.dev/download/
- Entity Model Features + Entity Texture Features: already installed. EMF supports custom entity models and Fresh Animations, making authored CEM packs another way to replace whole troop models. https://modrinth.com/mod/entity-model-features
- 3D Skin Layers: Fabric and 1.20.x support, but its geometry applies to player skin outer layers, not this mod's husk/skeleton troop models. Its current project text limits modpack jar redistribution to Modrinth/CurseForge. Not bundled. https://modrinth.com/mod/3dskinlayers
- Actually 3D Equipment: lists 1.20.1 and volumetric tools/weapons. Project license metadata and prose permissions differ, and its prose explicitly mentions Modrinth modpacks. Not bundled into the Melty package; original models avoid relying on that ambiguity. https://modrinth.com/resourcepack/3d-equipment

## Development and packaging

`gradle runClient --offline -PcampaignChecks` builds and loads the separate local test jar. Test runners, file-driven capture controls and MeltyTest are under test-tools and are absent from the player jar. Main code exposes only an optional no-provider diagnostics contract. Existing test names are preserved. The old Melty recipe cannot automatically test this release without a separately prepared compatible test companion; do not publish on the strength of earlier 0.2.0 results.

Screenshots used as battlefield previews are real game captures with the bundled Excalibur look and Complementary Reimagined, not generated artwork. Third-party assets and their original notices remain governed by the existing package credits.
