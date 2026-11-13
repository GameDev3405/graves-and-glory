package gg.gravesandglory;

import java.util.*;

/** The server's half of the War Table's banner commands, apart from Minecraft so a check can drive it: "cell:(banner):(col):(row):(troop id or -)",
 *  "banner:new:(name)", "banner:rename:(banner):(name)", "banner:copy:(banner)", "banner:delete:(banner)" and "banner:use:(banner)". A command is decided with the same
 *  BannerDrop.paint and RosterMath.validTable the client previews with. */
final class BannerCommands {
    private BannerCommands() {}

    /** True when cmd is one of the banner commands this class decides. */
    static boolean handles(String cmd) { return cmd.startsWith("cell:") || cmd.startsWith("banner:"); }

    /** Applies cmd to the profile. Null when it was accepted (the profile changed); otherwise the refusal in plain words and the profile is untouched. */
    static String apply(Profile p, String cmd, RosterMath.Catalog c, Set<String> houses) {
        if (p.banners == null) return "You have no banners.";
        String[] a = cmd.split(":", 3);
        if (cmd.startsWith("cell:")) return cell(p, cmd.split(":", -1), c, houses);
        if (a.length < 2) return "That banner command is not understood.";
        switch (a[1]) {
            case "new": {
                if (p.banners.size() >= RosterMath.maxBanners()) return "You can keep " + RosterMath.maxBanners() + " banners. Delete one first.";
                p.banners.add(new Profile.Banner(Profile.bannerName(a.length > 2 ? a[2] : "", p.banners.size() + 1), ""));
                return null;
            }
            case "rename": {
                String[] r = cmd.split(":", 4);
                int i = index(p, r.length > 2 ? r[2] : "");
                if (i < 0) return "That banner does not exist.";
                p.banners.get(i).name = Profile.bannerName(r.length > 3 ? r[3] : "", i + 1);
                return null;
            }
            case "copy": {
                int i = index(p, a.length > 2 ? a[2] : "");
                if (i < 0) return "That banner does not exist.";
                if (p.banners.size() >= RosterMath.maxBanners()) return "You can keep " + RosterMath.maxBanners() + " banners. Delete one first.";
                var src = p.banners.get(i);
                p.banners.add(new Profile.Banner(Profile.bannerName(src.name + " copy", p.banners.size() + 1), src.cells));
                return null;
            }
            case "delete": {
                int i = index(p, a.length > 2 ? a[2] : "");
                if (i < 0) return "That banner does not exist.";
                if (p.banners.size() <= 1) return "You cannot delete your last banner.";
                p.banners.remove(i);
                if (p.activeBanner > i || p.activeBanner >= p.banners.size()) p.activeBanner = Math.max(0, p.activeBanner - 1);
                return null;
            }
            case "use": {
                int i = index(p, a.length > 2 ? a[2] : "");
                if (i < 0) return "That banner does not exist.";
                p.activeBanner = i;
                return null;
            }
            default: return "That banner command is not understood.";
        }
    }

    /** A banner number as an index into the profile's banners, or -1 when it is not a number or no banner has it. */
    private static int index(Profile p, String s) {
        try { int i = Integer.parseInt(s.trim()); return i >= 0 && i < p.banners.size() ? i : -1; } catch (NumberFormatException e) { return -1; }
    }

    /** cell:(banner):(col):(row):(id or -): one cell painted on a banner (not only the active one). */
    private static String cell(Profile p, String[] a, RosterMath.Catalog c, Set<String> houses) {
        if (a.length != 5) return "That cell command is not understood.";
        int banner, col, row;
        try { banner = Integer.parseInt(a[1]); col = Integer.parseInt(a[2]); row = Integer.parseInt(a[3]); } catch (NumberFormatException e) { return "That cell command is not understood."; }
        if (banner < 0 || banner >= p.banners.size()) return "That banner does not exist.";
        if (col < 0 || col >= WarGrid.cols() || row < 0 || row >= WarGrid.rows()) return BannerDrop.why("range");
        String brush = a[4];
        if (!brush.equals(BannerDrop.ERASE)) {
            if (!c.known(brush)) return BannerDrop.why("unknown");
            if (!p.owned.containsKey(brush)) return "You do not own " + brush + " yet. Recruit it first.";
        }
        var plan = BannerDrop.paint(WarGrid.parse(p.banners.get(banner).cells), WarGrid.index(col, row), brush, new BannerDrop.Rules(c, p.owned.keySet(), houses));
        if (!plan.ok()) return plan.why();
        p.setTable(banner, plan.result());
        return null;
    }
}
