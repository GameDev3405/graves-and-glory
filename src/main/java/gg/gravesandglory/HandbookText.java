package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The builder handbook's pages (plan task B2). Pure text; {@link BuildHandbook} turns them into a vanilla written book.
 * Tools only, approved by Kort 2026-11-08 (source text: docs/builder-handbook.md). Keys are {key:<key binding translation key>}
 * tokens, shown as keybind text components with the player's own binding (the UI standard: no hardcoded default keys).
 * A book page holds about 14 lines of 19 characters; {@link #lines(String)} estimates the wrapped line count and
 * HandbookTextCheck keeps every page inside it.
 */
public final class HandbookText {
    private HandbookText(){}

    public static final String TITLE="Builder Handbook",AUTHOR="Graves and Glory";
    public static final int LINE_CHARS=19,PAGE_LINES=14,PAGE_CHARS=1023,MAX_PAGES=100;
    /** A bound key: {key:key.use} and the like. */
    public static final Pattern KEY=Pattern.compile("\\{key:([a-z0-9_.]+)\\}");
    /** Width a bound key's name counts as in the line estimate ("Right Button" is the long common case). */
    static final int KEY_CHARS=12;
    static final String WORLDS="{worlds}";

    static final String[] PAGES={
        "BUILDER HANDBOOK\n\nHow to use the builder tools.\n\n1. Moving around\n2. Props\n3. Axiom\n4. WorldEdit\n5. Voice chat\n6. Maps",
        "MOVING AROUND\n\n/map lists the worlds and says which one you are in.\n\n/map <name> takes you to that world.",
        "Worlds:\n"+WORLDS,
        "/warp lists the places in the world you are in.\n\n/warp <name> takes you there.\n\nYou log back in where you left.\n\n/handbook gives you a new copy of this book.",
        "PROPS\n\nProps are ready-made pieces such as the forge and the anvil.\n\n/prop opens the prop menu. Pick a prop from it.",
        "A ghost shows where it will go.\n\nScroll to turn it.\n\n{key:key.use} places it.\n\n{key:key.attack} on a placed prop removes it.",
        "Props are solid: troops walk around them and arrows hit them. Leave room to pass.",
        "AXIOM\n\nThe Axiom editor key opens the editor and closes it again.\n\nUse the selection tool to drag a box over a build.",
        "Copy and paste the selection. Before placing a paste, use the gizmo to move, rotate or flip it.\n\nUndo works as in any editor.",
        "The blueprint browser keeps builds you save so you can place them again in any world.",
        "WORLDEDIT\n\nThe wooden axe is the wand.\n\n{key:key.attack} sets position 1.\n\n{key:key.use} sets position 2.",
        "//set <block> fills the box.\n\n//replace <from> <to>\n\n//copy, then //paste where you stand.\n\n//rotate 90 turns the copy.\n\n//undo, //redo",
        "VOICE CHAT\n\nThe first time, {key:key.voice_chat} sets it up: push to talk (and its key) or voice activation.\n\nPlayers near you hear you.",
        "{key:key.voice_chat_group}: groups. Make or join one to talk to its members anywhere.\n\n{key:key.disable_voice_chat}: voice chat off and on.\n\n{key:key.voice_chat}: settings.",
        "MAPS\n\nThe minimap sits in the corner. {key:gui.xaero_enlarge_map} makes it bigger.\n\n{key:gui.xaero_new_waypoint} marks a waypoint. {key:gui.xaero_waypoints_key} lists them.",
        "{key:gui.xaero_open_map}: the world map.\n\nIf voice chat mute ({key:key.mute_microphone}) is the same key, move one in Options, Controls, Key Binds.",
    };

    /** The pages in order, with the build worlds listed (one per line) on the /map page. */
    public static List<String> pages(List<String> worlds){
        var out=new ArrayList<String>(PAGES.length);
        for(String p:PAGES)out.add(p.replace(WORLDS,String.join("\n",worlds)));
        return out;
    }

    /** Estimated wrapped line count of a page: greedy word wrap at {@link #LINE_CHARS}; longer words break; a key counts {@link #KEY_CHARS}. */
    public static int lines(String page){
        int n=0;
        page=KEY.matcher(page).replaceAll("K".repeat(KEY_CHARS));
        for(String para:page.split("\n",-1)){
            int len=0;n++;
            for(String w:para.split(" ")){
                if(w.isEmpty())continue;
                int need=len==0?w.length():len+1+w.length();
                if(need<=LINE_CHARS){len=need;continue;}
                if(len>0){n++;len=0;}
                while(w.length()>LINE_CHARS){n++;w=w.substring(LINE_CHARS);}
                len=w.length();
            }
        }
        return n;
    }
}
