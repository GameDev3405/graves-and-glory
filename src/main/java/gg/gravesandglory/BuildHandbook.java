package gg.gravesandglory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * The builder handbook (plan task B2, Kort 2026-11-08): a vanilla written book of {@link HandbookText} pages, given once on a player's first join to the
 * build-mode server (marker list gg-build-handbook.txt next to gg-build-opped.txt) and on /handbook. Build mode only; no registry entry.
 */
final class BuildHandbook {
    private BuildHandbook(){}
    static final String GIVEN="gg-build-handbook.txt";

    static ItemStack book(){
        var stack=new ItemStack(Items.WRITTEN_BOOK);
        var tag=stack.getOrCreateNbt();
        tag.putString("title",HandbookText.TITLE);
        tag.putString("author",HandbookText.AUTHOR);
        var pages=new NbtList();
        for(String p:HandbookText.pages(BuildMode.worlds()))pages.add(NbtString.of(Text.Serializer.toJson(page(p))));
        tag.put("pages",pages);
        tag.putBoolean("resolved",true);
        return stack;
    }

    /** One page: literal text, each {key:...} token a keybind component (the reader's own binding). */
    static MutableText page(String p){
        var out=Text.empty();var m=HandbookText.KEY.matcher(p);int at=0;
        while(m.find()){
            if(m.start()>at)out.append(Text.literal(p.substring(at,m.start())));
            out.append(Text.keybind(m.group(1)));at=m.end();
        }
        if(at<p.length())out.append(Text.literal(p.substring(at)));
        return out;
    }

    static void give(ServerPlayerEntity player){
        var stack=book();
        if(!player.getInventory().insertStack(stack))player.dropItem(stack,false);
    }

    /** First join only: a player already in the marker list gets nothing (a lost book comes back with /handbook). */
    static void giveOnce(MinecraftServer s,ServerPlayerEntity player){
        Path file=s.getRunDirectory().toPath().resolve(GIVEN);String uuid=player.getUuidAsString();
        try{
            String seen=Files.exists(file)?Files.readString(file):"";
            if(seen.lines().anyMatch(uuid::equals))return;
            give(player);
            SafeFiles.write(file,seen+uuid+"\n");
            BuildMode.log("gave "+player.getGameProfile().getName()+" the builder handbook");
        }catch(IOException e){BuildMode.log("handbook for "+player.getGameProfile().getName()+" not recorded: "+e);}
    }
}
