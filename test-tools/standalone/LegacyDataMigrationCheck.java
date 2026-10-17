package gg.gravesandglory;

import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Real temporary-filesystem checks. Run without Minecraft; never packaged. */
public final class LegacyDataMigrationCheck {
    static int passed;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);passed++;System.out.println("PASS "+message);}
    static void write(Path root,String name,String data)throws Exception{Path p=root.resolve(name);Files.createDirectories(p.getParent());Files.writeString(p,data);}
    static String read(Path root,String name)throws Exception{return Files.readString(root.resolve(name));}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("gravesandglory-migration-check-");
        try{
            write(root,"bannerbound/collections/alice.json","{\"gold\":350,\"unlocked\":[\"sentinel\"],\"deployment\":{\"dawn\":[\"sentinel\"]}}");
            write(root,"bannerbound/collections/bob.json","old-bob");
            write(root,"gravesandglory/collections/bob.json","new-bob");
            write(root,"bannerbound/test-collections/test.json","test-only");
            write(root,"bannerbound/presentation.json","{\"impact\":2}");
            LegacyDataMigration.instance(root,false);
            check(read(root,"bannerbound/collections/alice.json").equals(read(root,"gravesandglory/collections/alice.json")),"collection, unlocks and deployment copied byte-for-byte; original kept");
            check(read(root,"gravesandglory/collections/bob.json").equals("new-bob"),"current collection wins when both directories exist");
            check(!Files.exists(root.resolve("gravesandglory/test-collections/test.json")),"production migration excludes test collections");
            check(read(root,"gravesandglory/presentation.json").equals("{\"impact\":2}"),"visual preference preserved");
            write(root,"gravesandglory/collections/alice.json","new-progress");
            LegacyDataMigration.instance(root,false);
            check(read(root,"gravesandglory/collections/alice.json").equals("new-progress"),"repeat migration does not roll back progress");
            LegacyDataMigration.instance(root,true);
            check(read(root,"gravesandglory/test-collections/test.json").equals("test-only"),"development collection isolation retained");
            write(root,"saves/Bannerbound/level.dat","legacy-world");
            write(root,"saves/Bannerbound/region/r.0.0.mca","region-data");
            write(root,"saves/Bannerbound/session.lock","lock");
            try(var channel=FileChannel.open(root.resolve("saves/Bannerbound/session.lock"),StandardOpenOption.WRITE);var lock=channel.lock()){
                boolean refused=false;try{LegacyDataMigration.world(root.resolve("saves"));}catch(java.io.IOException expected){refused=true;}
                check(refused&&!Files.exists(root.resolve("saves/Graves and Glory")),"active legacy world is never copied/opened");
            }
            check(LegacyDataMigration.world(root.resolve("saves")).equals("Graves and Glory"),"legacy world copied into current save name");
            check(read(root,"saves/Graves and Glory/region/r.0.0.mca").equals("region-data")&&read(root,"saves/Bannerbound/level.dat").equals("legacy-world"),"world regions copied and original retained");
            check(!Files.exists(root.resolve("saves/Graves and Glory/session.lock")),"transient session lock not copied");
            write(root,"saves/Graves and Glory/level.dat","new-world");LegacyDataMigration.world(root.resolve("saves"));
            check(read(root,"saves/Graves and Glory/level.dat").equals("new-world"),"repeat world migration preserves new world");
            write(root,"incomplete/Bannerbound/level.dat","old");Files.createDirectories(root.resolve("incomplete/Graves and Glory"));
            boolean refused=false;try{LegacyDataMigration.world(root.resolve("incomplete"));}catch(java.io.IOException expected){refused=true;}
            check(refused,"incomplete current world requires recovery instead of overwrite");
            check(LegacyDataMigration.world(root.resolve("fresh")).equals("Graves and Glory"),"fresh install has no legacy migration side effects");
            System.out.println("Migration checks passed: "+passed);
        }finally{
            // Remove only this test's unique temporary directory.
            try(var tree=Files.walk(root)){for(Path p:tree.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
