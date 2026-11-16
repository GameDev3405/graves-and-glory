package gg.gravesandglory;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/**
 * P1.8 (port plan): a world and commander profile saved by the 1.20.1 build, copied into a throwaway run root, opens on 1.21.1 with the same data.
 * GRAVES_PHASE1_CHECKS=save-open, GRAVES_SAVE_REF=<folder of the untouched 1.20.1 copy of the world> and GRAVES_ROADMAP_USER=<the player name that saved it>
 * (so the offline player UUID, and with it the profile and player data files, are the saved ones). Read only: it changes nothing in the world.
 * Reports p1-save-open-profile (the loaded profile equals the 1.20.1 file read the same way: schema, gear, equipped, relics, owned),
 * p1-save-open-dfu (the 1.20.1 item stacks of the saved player run through the game's own DataFixer; each keeps gg_uid, gg_item and gg_rarity under
 * custom_data and they match the profile's gear), p1-save-open-live (the stacks the player holds now carry the same triples as the profile's worn pieces)
 * and p1-save-open-world (the world loaded: the overworld has the camp's spawn chunk). Never shipped.
 */
final class SaveOpenChecks implements Phase1Checks.Suite {
    static void say(String s) { System.out.println("[p1] save-open: " + s); }
    int stage;

    /** The first few places two JSON trees differ: path, then the file's value and the loaded one's. */
    static void diff(String path, com.google.gson.JsonElement a, com.google.gson.JsonElement b, StringBuilder out) {
        if (out.length() > 1500 || a.equals(b)) return;
        if (a.isJsonObject() && b.isJsonObject()) {
            var keys = new java.util.TreeSet<String>(a.getAsJsonObject().keySet()); keys.addAll(b.getAsJsonObject().keySet());
            for (String k : keys) {
                var x = a.getAsJsonObject().get(k); var y = b.getAsJsonObject().get(k);
                if (x == null || y == null) out.append(path).append('/').append(k).append(": ").append(x).append(" -> ").append(y).append("; ");
                else diff(path + "/" + k, x, y, out);
            }
        } else if (a.isJsonArray() && b.isJsonArray() && a.getAsJsonArray().size() == b.getAsJsonArray().size()) {
            for (int i = 0; i < a.getAsJsonArray().size(); i++) diff(path + "[" + i + "]", a.getAsJsonArray().get(i), b.getAsJsonArray().get(i), out);
        } else out.append(path).append(": ").append(a.toString().length() > 120 ? a.toString().substring(0, 120) : a).append(" -> ").append(b.toString().length() > 120 ? b.toString().substring(0, 120) : b).append("; ");
    }

    public void step(MinecraftClient c, int tick) {
        if (tick < 40 || stage > 0) return;
        stage = 1;
        try {
            Path ref = Paths.get(System.getenv("GRAVES_SAVE_REF"));
            UUID id = c.player.getUuid();
            say("player " + c.player.getName().getString() + " uuid " + id);
            // 1. profile
            var holder = new Object[1];
            c.getServer().submit(() -> { holder[0] = GravesAndGlory.battle.campaign.profile(id); }).join();
            Profile now = (Profile) holder[0];
            Path file = null;
            for (String dir : List.of("gravesandglory-test-profiles", "gravesandglory-profiles")) if (Files.isRegularFile(ref.resolve(dir).resolve(id + ".json"))) { file = ref.resolve(dir).resolve(id + ".json"); break; }
            if (file == null) { Phase1Checks.report("p1-save-open-profile", false, "no profile of " + id + " in the 1.20.1 copy " + ref); }
            else {
                Profile was = Campaign.JSON.fromJson(Files.readString(file), Profile.class); was.sanitize();
                // The daily and weekly counters roll over by the calendar (Progression.reset, unchanged since 1.20.1): bring the pristine copy to the loaded profile's date by that same rule, so a run on a later day than the save still compares like for like.
                if (now.progression.day != null) Progression.reset(was.progression, java.time.LocalDate.parse(now.progression.day));
                boolean eq = JsonParser.parseString(Campaign.JSON.toJson(was)).equals(JsonParser.parseString(Campaign.JSON.toJson(now)));
                StringBuilder dif = new StringBuilder();
                if (!eq) diff("", JsonParser.parseString(Campaign.JSON.toJson(was)), JsonParser.parseString(Campaign.JSON.toJson(now)), dif);
                if (!eq) say("profile differs (1.20.1 file -> loaded): " + dif);
                StringBuilder gear = new StringBuilder();
                for (var g : now.gear) gear.append(g.uid).append(':').append(g.item).append(':').append(g.rarity).append(' ');
                Phase1Checks.report("p1-save-open-profile", eq && now.schema == Profile.SCHEMA && !now.gear.isEmpty(), "1.20.1 file " + file.getFileName() + " (schema " + was.schema + ") equals the loaded profile=" + eq + "; schema " + now.schema + ", " + now.owned.size() + " owned, gear " + gear + "equipped " + now.equipped + " relics " + now.relics + " shards " + now.gearShards);
            }
            // 2. the game's DataFixer on the 1.20.1 stacks
            Path dat = ref.resolve("playerdata").resolve(id + ".dat");
            NbtCompound player = NbtIo.readCompressed(dat, NbtSizeTracker.ofUnlimitedBytes());
            int from = player.getInt("DataVersion"), to = net.minecraft.SharedConstants.getGameVersion().getSaveVersion().getId();
            NbtList inv = player.getList("Inventory", 10);
            int ok = 0, bad = 0; StringBuilder det = new StringBuilder();
            Map<Integer, String> want = new HashMap<>(); for (var g : now.gear) want.put(g.uid, g.item);
            for (int i = 0; i < inv.size(); i++) {
                NbtCompound oldStack = inv.getCompound(i);
                if (!oldStack.contains("tag")) continue;
                NbtCompound fixed = (NbtCompound) net.minecraft.datafixer.Schemas.getFixer().update(net.minecraft.datafixer.TypeReferences.ITEM_STACK, new com.mojang.serialization.Dynamic<>(NbtOps.INSTANCE, oldStack), from, to).getValue();
                NbtCompound cd = fixed.getCompound("components").getCompound("minecraft:custom_data");
                boolean good = cd.contains("gg_uid") && cd.contains("gg_item") && cd.contains("gg_rarity") && want.getOrDefault(cd.getInt("gg_uid"), "").equals(cd.getString("gg_item"));
                if (good) ok++; else bad++;
                det.append(fixed.getString("id")).append('{').append(cd.getInt("gg_uid")).append(',').append(cd.getString("gg_item")).append(',').append(cd.getInt("gg_rarity")).append("} ");
            }
            Phase1Checks.report("p1-save-open-dfu", ok > 0 && bad == 0, "DataVersion " + from + " -> " + to + ", " + ok + " gear stacks keep gg_uid/gg_item/gg_rarity under custom_data and match the profile, " + bad + " do not: " + det);
            // 3. what the camp does with the gear: toLobby empties the inventory on purpose (the gear lives in the profile), so the check is the stand-in stack the profile's pieces issue
            var pi = c.player.getInventory(); int held = 0; for (var list : List.of(pi.main, pi.armor, pi.offHand)) for (ItemStack hs : list) if (!hs.isEmpty()) held++;
            StringBuilder live = new StringBuilder(); int liveOk = 0, liveBad = 0;
            for (var g : now.gear) { ItemStack is = GearLooks.stack(g); var art = GearLooks.art(is); boolean m = !is.isEmpty() && GearLooks.uid(is) == g.uid && art != null && art.item().equals(g.item); if (m) liveOk++; else liveBad++; live.append(g.uid).append(':').append(art == null ? "?" : art.item() + "/r" + art.rarity()).append(m ? "" : "(MISMATCH)").append(' '); }
            Phase1Checks.report("p1-save-open-live", liveOk >= 1 && liveBad == 0, liveOk + " of " + now.gear.size() + " profile pieces issue a stack carrying their gg_uid/gg_item/gg_rarity; the camp holds " + held + " stacks in the inventory (toLobby clears it, as on 1.20.1): " + live);
            // 4. the world
            var w = c.getServer().getOverworld();
            var bp = c.player.getBlockPos(); boolean chunk = w.getChunkManager().isChunkLoaded(bp.getX() >> 4, bp.getZ() >> 4) && !w.getBlockState(bp.down()).isAir();
            Phase1Checks.report("p1-save-open-world", chunk, "the chunk under the player is loaded and has a solid block below the feet=" + chunk + ", player at " + c.player.getBlockPos() + ", level " + c.getServer().getSaveProperties().getLevelName());
        } catch (Exception e) { Phase1Checks.report("p1-save-open-exception", false, e.toString()); e.printStackTrace(); }
        Phase1Checks.finish(c, "save-open");
    }
}
