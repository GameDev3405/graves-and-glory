package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The War Table's Banner page as a plain player's chest menu (plain clients P4, plan decisions 5 and 6). Two views in one window:
 * <ul>
 * <li><b>Tools</b>: the banner tabs (a click shows that banner), Take to battle (banner:use), New, Copy, Delete (two presses),
 *     Auto-arrange and Clear (two presses), the team colour (colour:), the pinned notice's Understood (notice:understood), the House
 *     chips and the House's owned roll as the palette (a click arms the brush), the eraser and the move tool.</li>
 * <li><b>Grid</b>: the shown banner's grid, five rows at a time (a toggle shows the rest); a click paints the armed troop (cell:),
 *     takes off the same troop or erases, or with the move tool lifts a troop and drops it (banner:move:).</li>
 * </ul>
 * Every command is the modpack page's own string; the server decides it again and says any refusal in chat. Rename stays modpack-only
 * (decision 6): the name is shown, never typed.
 */
final class PlainBanner {
    private PlainBanner() {}

    /** The move tool's brush id (never sent: the move tool sends banner:move). */
    static final String MOVE = "#move";
    /** Grid rows shown at once in the grid view (the sixth menu row holds its controls). */
    static final int GRID_ROWS = 5;

    /** What this window shows, kept across redraws: the banner shown, the brush, the House of the palette, the grid's first row shown,
     *  the cell the move tool lifted, the two-press arms (tick until which a second press confirms) and the view. */
    static final class View {
        int shown = -1, top, lifted = -1, deleteFor = -1, clearFor = -1;
        long deleteUntil, clearUntil;
        String brush, house;
        boolean grid;
    }

    static Text title() { return Text.literal("Banner").styled(st -> st.withFont(PlainPack.TITLE_FONT)); }

    static void open(ServerPlayerEntity p) {
        View v = new View();
        PlainMenu.open(p, title(), 6, (m, pl) -> draw(m, pl, v));
    }

    // ---- shared helpers ----
    /** Sends a War Table command (as PlainMenu.command does) and shows the notice in chat. */
    static void send(ServerPlayerEntity p, String cmd) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        b.command(p, cmd);
        PlainHud.notices(p);
    }

    static ItemStack glint(ItemStack s, boolean on) { if (on) s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true); return s; }

    /** Words wrapped to lines of about width characters (lore). */
    static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        StringBuilder line = new StringBuilder();
        for (String w : s.split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + w.length() > width) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    static ItemStack item(Item item, String name, Formatting colour, List<String> lore) {
        return PlainMenu.named(item, name, colour, lore.toArray(String[]::new));
    }

    static Item vanilla(String path, Item fallback) {
        Item i = Registries.ITEM.get(Identifier.of("minecraft", path));
        return i == Items.AIR ? fallback : i;
    }

    static Spec.TroopsRow troop(String id) {
        for (var t : Spec.TROOPS) if (t.id().equals(id)) return t;
        return null;
    }

    /** A troop's card item: the vanilla spawn egg of its mob (paper when there is none). */
    static Item egg(String id) {
        var t = troop(id);
        return t == null ? Items.PAPER : vanilla(t.entity() + "_spawn_egg", Items.PAPER);
    }

    static String troopName(String id) { var t = troop(id); return t == null ? id : t.name(); }

    static String rarityWord(String r) { return r == null || r.isEmpty() ? "" : Character.toUpperCase(r.charAt(0)) + r.substring(1); }

    static String roman(int n) { return new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"}[Math.max(0, Math.min(9, n - 1))]; }

    static String cellName(int i) {
        return (WarGrid.row(i) == 0 ? "Front row" : "Row " + (WarGrid.row(i) + 1)) + ", column " + (WarGrid.col(i) + 1);
    }

    static BannerDrop.Rules rules(Profile pr) { return new BannerDrop.Rules(RosterMath.Catalog.fromSpec(), pr.owned.keySet(), RosterMath.fieldableHouses()); }

    static SortedMap<Integer, String> table(Profile pr, int banner) {
        var bs = pr.banners;
        String cells = bs == null || banner < 0 || banner >= bs.size() || bs.get(banner) == null || bs.get(banner).cells == null ? "" : bs.get(banner).cells;
        return WarGrid.parse(cells);
    }

    /** The owned troops of a House in roll order (the palette; a House holds six troops). */
    static List<Spec.TroopsRow> owned(Profile pr, String house) {
        List<Spec.TroopsRow> out = new ArrayList<>();
        for (var t : Spec.TROOPS) if (t.house().equals(house) && pr.owned.containsKey(t.id())) out.add(t);
        return out;
    }

    /** Why the troop cannot go on the table now (the paint rules on its first empty cell), or "" when it can (the palette's tooltip). */
    static String blockedWhy(SortedMap<Integer, String> t, String troop, BannerDrop.Rules r) {
        for (int i = 0; i < WarGrid.cells(); i++) if (!t.containsKey(i)) return BannerDrop.paint(t, i, troop, r).why();
        return BannerDrop.why("count");
    }

    // ---- the page ----
    static void draw(PlainMenu m, ServerPlayerEntity p, View v) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        Profile pr = b.campaign.profile(p.getUuid());
        int n = pr.banners == null ? 0 : pr.banners.size();
        if (n == 0) { PlainWarTable.back(m, 0); PlainWarTable.note(m, 4, "You have no banners.", List.of()); return; }
        if (v.shown < 0) v.shown = pr.activeBanner;
        v.shown = Math.max(0, Math.min(n - 1, v.shown));
        if (v.house == null) v.house = RosterMath.rollHouse(pr.line());
        if (Spec.HOUSES.length > 0 && java.util.Arrays.stream(Spec.HOUSES).noneMatch(h -> h.id().equals(v.house))) v.house = Spec.HOUSES[0].id();
        var roll = owned(pr, v.house);
        if (v.brush == null || (!v.brush.equals(BannerDrop.ERASE) && !v.brush.equals(MOVE) && roll.stream().noneMatch(t -> t.id().equals(v.brush))))
            v.brush = roll.isEmpty() ? BannerDrop.ERASE : roll.get(0).id();
        if (!MOVE.equals(v.brush)) v.lifted = -1;
        var table = table(pr, v.shown);
        var rules = rules(pr);
        if (v.grid) grid(m, p, v, pr, table, rules, roll); else tools(m, p, v, pr, table, rules, roll, n);
    }

    static String bannerName(Profile pr, int i) { var bn = pr.banners.get(i); return bn == null || bn.name == null ? "Banner " + roman(i + 1) : bn.name; }

    /** The gold and troop lines of the shown table. */
    static List<String> goldLines(SortedMap<Integer, String> table) {
        var cat = RosterMath.Catalog.fromSpec();
        int allow = RosterMath.allowance(), spent = WarGrid.cost(table, cat), left = allow - spent, troops = WarGrid.troops(table, cat);
        return List.of(left >= 0 ? left + " gold left" : -left + " gold over", spent + " of " + allow + " gold spent", troops + (troops == 1 ? " troop" : " troops"),
            "Every commander gets the same " + allow + " gold.");
    }

    static String brushWord(String brush) {
        if (BannerDrop.ERASE.equals(brush)) return "Eraser";
        if (MOVE.equals(brush)) return "Move";
        return troopName(brush);
    }

    static void tools(PlainMenu m, ServerPlayerEntity p, View v, Profile pr, SortedMap<Integer, String> table, BannerDrop.Rules rules, List<Spec.TroopsRow> roll, int n) {
        long now = p.getServer().getTicks();
        int shown = v.shown, max = RosterMath.maxBanners();
        PlainWarTable.back(m, 0);
        // the tabs: a click shows that banner; the ticked one goes into battle
        for (int i = 0; i < max && i < 5; i++) {
            int bi = i;
            if (i >= n) { m.icon(1 + i, PlainMenu.named(Items.LIGHT_GRAY_STAINED_GLASS_PANE, "Empty banner slot", Formatting.DARK_GRAY, "New or Copy fills it.")); continue; }
            boolean on = i == shown, going = i == pr.activeBanner;
            var cat = RosterMath.Catalog.fromSpec();
            var t = table(pr, i);
            List<String> lore = new ArrayList<>();
            lore.add(WarGrid.troops(t, cat) + " troops, " + WarGrid.cost(t, cat) + " gold");
            if (going) lore.add("Goes into battle");
            lore.add(on ? "Shown" : "Click to show");
            ItemStack tab = glint(item(going ? Items.YELLOW_BANNER : Items.WHITE_BANNER, "Banner " + (i + 1) + ": " + bannerName(pr, i), on ? Formatting.GOLD : Formatting.WHITE, lore), on);
            m.button(1 + i, tab, on ? null : pl -> { v.shown = bi; v.lifted = -1; v.deleteFor = -1; v.clearFor = -1; });
        }
        m.button(7, item(Items.FILLED_MAP, "Paint the grid", Formatting.GOLD, List.of("Brush: " + brushWord(v.brush), "Open the shown banner's grid.")), pl -> v.grid = true);
        m.icon(8, item(Items.GOLD_INGOT, bannerName(pr, shown), Formatting.YELLOW, goldLines(table)));
        // the banner actions
        boolean going = shown == pr.activeBanner;
        if (going) m.icon(9, glint(item(Items.LIME_DYE, "Going to battle", Formatting.GREEN, List.of("This banner goes into battle")), true));
        else m.command(9, item(Items.GOLDEN_SWORD, "Take to battle", Formatting.GOLD, List.of("Take this banner into battle")), "banner:use:" + shown);
        String full = "You can keep " + max + " banners. Delete one first.";
        if (n < max) {
            m.button(10, item(Items.WHITE_BANNER, "New", Formatting.WHITE, List.of("Start a new empty banner")), pl -> { send(pl, "banner:new:Banner " + roman(n + 1)); showNewest(pl, v, n); });
            m.button(11, item(Items.PAPER, "Copy", Formatting.WHITE, List.of("Copy this banner into a new one")), pl -> { send(pl, "banner:copy:" + shown); showNewest(pl, v, n); });
        } else {
            m.icon(10, item(Items.GRAY_DYE, "New", Formatting.DARK_GRAY, List.of(full)));
            m.icon(11, item(Items.GRAY_DYE, "Copy", Formatting.DARK_GRAY, List.of(full)));
        }
        if (n <= 1) m.icon(12, item(Items.GRAY_DYE, "Delete", Formatting.DARK_GRAY, List.of("You cannot delete your last banner.")));
        else {
            boolean armed = v.deleteFor == shown && now < v.deleteUntil;
            m.button(12, glint(item(Items.BARRIER, armed ? "Sure? Click again to delete" : "Delete", Formatting.RED, List.of("Delete this banner (press twice)")), armed), pl -> {
                if (v.deleteFor == shown && pl.getServer().getTicks() < v.deleteUntil) { v.deleteFor = -1; send(pl, "banner:delete:" + shown); v.shown = Math.min(v.shown, Math.max(0, pr.banners.size() - 1)); }
                else { v.deleteFor = shown; v.deleteUntil = pl.getServer().getTicks() + 60; }
            });
        }
        m.icon(13, item(Items.NAME_TAG, bannerName(pr, shown), Formatting.WHITE, List.of("Rename on the modpack's War Table.")));
        var arrange = BannerDrop.arrange(table, rules);
        if (arrange.ok()) m.command(14, item(Items.COMPARATOR, "Auto-arrange", Formatting.WHITE, List.of("Set the troops out: shields in front,", "then melee, then ranged and casters behind")), "banner:arrange:" + shown);
        else m.icon(14, item(Items.GRAY_DYE, "Auto-arrange", Formatting.DARK_GRAY, wrap(arrange.why() == null || arrange.why().isEmpty() ? "Nothing to arrange." : arrange.why(), 40)));
        if (table.isEmpty()) m.icon(15, item(Items.GRAY_DYE, "Clear", Formatting.DARK_GRAY, List.of("No troops on this banner.")));
        else {
            boolean armed = v.clearFor == shown && now < v.clearUntil;
            m.button(15, glint(item(Items.BUCKET, armed ? "Sure? Click again to clear" : "Clear", Formatting.RED, List.of("Take every troop off this banner (press twice)")), armed), pl -> {
                if (v.clearFor == shown && pl.getServer().getTicks() < v.clearUntil) { v.clearFor = -1; send(pl, "banner:clear:" + shown); }
                else { v.clearFor = shown; v.clearUntil = pl.getServer().getTicks() + 60; }
            });
        }
        if (pr.notice != null && !pr.notice.isEmpty()) {
            List<String> lore = new ArrayList<>(wrap(pr.notice, 40));
            lore.add("Click to confirm");
            m.command(17, item(Items.WRITABLE_BOOK, "Notice", Formatting.GOLD, lore), "notice:understood");
        }
        // the team colour
        m.icon(18, item(Items.PAINTING, "Team colour", Formatting.GOLD, List.of("First team to pick it keeps it.")));
        for (int i = 0; i < Spec.FACTIONS.length && i < 8; i++) {
            var side = Spec.FACTIONS[i];
            boolean on = side.id().equals(pr.bannerColour);
            ItemStack wool = glint(item(vanilla(side.dye() + "_wool", Items.WHITE_WOOL), side.name(), on ? Formatting.GOLD : Formatting.WHITE,
                List.of(on ? "Your team colour" : "Click to ask for it", "Team colour. First team to pick it keeps it.")), on);
            m.command(19 + i, wool, "colour:" + side.id());
        }
        // the House chips: the palette shows that House's owned troops
        int col = 0;
        for (var h : Spec.HOUSES) {
            if (col > 8) break;
            boolean on = h.id().equals(v.house);
            String id = h.id();
            Item banner = switch (id) { case "gilded" -> Items.YELLOW_BANNER; case "graveborn" -> Items.BLACK_BANNER; case "ironclad" -> Items.GRAY_BANNER; case "tidecallers" -> Items.CYAN_BANNER; default -> Items.GREEN_BANNER; };
            ItemStack chip = glint(PlainRecruit.named(banner, Text.empty().append(PlainRecruit.crest(id)).append(Text.literal(h.name()).formatted(on ? Formatting.GOLD : Formatting.WHITE)),
                List.of(Text.literal("Troops of " + h.name()).formatted(Formatting.GRAY), Text.literal(on ? "Shown" : "Click to show").formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY))), on);
            m.button(27 + col, chip, on ? null : pl -> { v.house = id; v.brush = null; });
            col++;
        }
        // the palette: a click arms the brush
        if (roll.isEmpty()) {
            String house = v.house;
            m.button(36, item(Items.WRITABLE_BOOK, "Recruit", Formatting.GOLD, List.of("No " + RosterMath.houseName(house) + " troops recruited yet.", "Open the Recruit page")), pl -> PlainRecruit.open(pl, house));
        }
        for (int i = 0; i < roll.size() && i < 6; i++) {
            var t = roll.get(i);
            m.button(36 + i, paletteCard(t, v, table, rules, pr), pl -> { v.brush = t.id(); v.lifted = -1; });
        }
        boolean erase = BannerDrop.ERASE.equals(v.brush), move = MOVE.equals(v.brush);
        m.button(43, glint(item(Items.SPONGE, "Eraser", erase ? Formatting.GOLD : Formatting.WHITE, List.of("A click on a cell takes its troop off.", erase ? "Armed" : "Click to arm")), erase), pl -> { v.brush = BannerDrop.ERASE; v.lifted = -1; });
        m.button(44, glint(item(Items.LEAD, "Move", move ? Formatting.GOLD : Formatting.WHITE, List.of("Click a troop on the grid, then the cell", "to move it to (a troop there swaps).", move ? "Armed" : "Click to arm")), move), pl -> { v.brush = MOVE; v.lifted = -1; });
        m.button(49, item(Items.FILLED_MAP, "Paint the grid", Formatting.GOLD, List.of("Brush: " + brushWord(v.brush))), pl -> v.grid = true);
    }

    /** After New or Copy: the new banner is shown once it is there (the modpack's showLastFrom). */
    static void showNewest(ServerPlayerEntity p, View v, int before) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        var pr = b.campaign.profile(p.getUuid());
        if (pr.banners != null && pr.banners.size() > before) { v.shown = pr.banners.size() - 1; v.lifted = -1; }
    }

    static ItemStack paletteCard(Spec.TroopsRow t, View v, SortedMap<Integer, String> table, BannerDrop.Rules rules, Profile pr) {
        boolean armed = t.id().equals(v.brush);
        int placed = 0;
        for (var id : table.values()) if (id.equals(t.id())) placed++;
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal(rarityWord(t.rarity()) + ", " + RosterMath.Catalog.fromSpec().cost(t.id()) + " gold").formatted(PlainRecruit.rarityColour(t.rarity())));
        var o = pr.owned.get(t.id());
        if (o != null) lore.add(Text.literal("★".repeat(Math.max(0, o.stars)) + "  on this banner: " + placed).formatted(Formatting.GRAY));
        String why = blockedWhy(table, t.id(), rules);
        if (why != null && !why.isEmpty()) for (String l : wrap(why, 40)) lore.add(Text.literal(l).formatted(Formatting.RED));
        lore.add(Text.literal(armed ? "Armed: click a grid cell to place it" : "Click to arm").formatted(armed ? Formatting.GREEN : Formatting.DARK_GRAY));
        return glint(PlainRecruit.named(egg(t.id()), Text.literal(t.name()).formatted(PlainRecruit.rarityColour(t.rarity())), lore), armed);
    }

    static void grid(PlainMenu m, ServerPlayerEntity p, View v, Profile pr, SortedMap<Integer, String> table, BannerDrop.Rules rules, List<Spec.TroopsRow> roll) {
        int rows = WarGrid.rows(), cols = Math.min(9, WarGrid.cols()), shownRows = Math.min(GRID_ROWS, rows);
        v.top = Math.max(0, Math.min(rows - shownRows, v.top));
        int shown = v.shown;
        for (int r = 0; r < shownRows; r++) {
            int row = v.top + r;
            for (int c = 0; c < cols; c++) {
                int cell = WarGrid.index(c, row), slot = r * 9 + c;
                m.button(slot, cellStack(cell, table, v), pl -> clickCell(pl, v, shown, cell));
            }
        }
        // the control row
        List<String> lore = new ArrayList<>(goldLines(table));
        lore.add("Brush: " + brushWord(v.brush));
        lore.add("Click to back to the banner tools");
        m.button(45, item(Items.ARROW, "Tools: " + bannerName(pr, shown), Formatting.WHITE, lore), pl -> { v.grid = false; v.lifted = -1; });
        if (rows > shownRows) {
            boolean atTop = v.top == 0;
            m.button(46, item(atTop ? Items.SPECTRAL_ARROW : Items.TIPPED_ARROW, atTop ? "Show the back rows" : "Show the front row", Formatting.WHITE,
                List.of("Rows " + (v.top + 1) + " to " + (v.top + shownRows) + " of " + rows + " shown", "Row 1 is the front row")), pl -> v.top = atTop ? rows - shownRows : 0);
        }
        boolean erase = BannerDrop.ERASE.equals(v.brush), move = MOVE.equals(v.brush);
        m.button(47, glint(item(Items.SPONGE, "Eraser", erase ? Formatting.GOLD : Formatting.WHITE, List.of("A click on a cell takes its troop off.")), erase), pl -> { v.brush = BannerDrop.ERASE; v.lifted = -1; });
        m.button(48, glint(item(Items.LEAD, "Move", move ? Formatting.GOLD : Formatting.WHITE, List.of("Click a troop, then the cell to move it to.")), move), pl -> { v.brush = MOVE; v.lifted = -1; });
        for (int i = 0; i < roll.size() && i < 5; i++) {
            var t = roll.get(i);
            m.button(49 + i, paletteCard(t, v, table, rules, pr), pl -> { v.brush = t.id(); v.lifted = -1; });
        }
    }

    static ItemStack cellStack(int cell, SortedMap<Integer, String> table, View v) {
        String id = table.get(cell);
        List<String> lore = new ArrayList<>();
        lore.add(cellName(cell));
        boolean move = MOVE.equals(v.brush), erase = BannerDrop.ERASE.equals(v.brush);
        if (id != null) {
            var t = troop(id);
            if (t != null) lore.add(rarityWord(t.rarity()) + ", " + RosterMath.Catalog.fromSpec().cost(id) + " gold");
            if (move) lore.add(v.lifted == cell ? "Lifted: click another cell to move it" : v.lifted >= 0 ? "Click to move the lifted troop here" : "Click to lift this troop");
            else if (erase || id.equals(v.brush)) lore.add("Click to take it off");
            else lore.add("Click to put " + brushWord(v.brush) + " here");
            return glint(item(egg(id), troopName(id), Formatting.WHITE, lore), v.lifted == cell);
        }
        if (move) lore.add(v.lifted >= 0 ? "Click to move the lifted troop here" : "Empty");
        else if (!erase) lore.add("Click to put " + brushWord(v.brush) + " here");
        return item(WarGrid.row(cell) == 0 ? Items.WHITE_STAINED_GLASS_PANE : Items.LIGHT_GRAY_STAINED_GLASS_PANE, WarGrid.row(cell) == 0 ? "Front row" : "Empty cell", Formatting.GRAY, lore);
    }

    /** A grid click: the brush paints (BannerDrop.paint gives the stroke's command, the same troop twice taking it off), the eraser takes
     *  off, the move tool lifts then drops (banner:move). The server decides every stroke again and says a refusal. */
    static void clickCell(ServerPlayerEntity p, View v, int banner, int cell) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        Profile pr = b.campaign.profile(p.getUuid());
        var table = table(pr, banner);
        if (MOVE.equals(v.brush)) {
            if (v.lifted < 0) { if (table.containsKey(cell)) v.lifted = cell; return; }
            if (v.lifted == cell) { v.lifted = -1; return; }
            int from = v.lifted;
            v.lifted = -1;
            send(p, BannerDrop.move(table, from, cell, rules(pr)).withBanner(banner).command());
            return;
        }
        if (BannerDrop.ERASE.equals(v.brush) && !table.containsKey(cell)) return;
        send(p, BannerDrop.paint(table, cell, v.brush, rules(pr)).withBanner(banner).command());
    }
}
