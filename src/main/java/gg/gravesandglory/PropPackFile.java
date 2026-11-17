package gg.gravesandglory;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The prop server resource pack on disk and over HTTP (prop kit plan task S7). Pure JDK, no Minecraft, so PropPackFileCheck runs it standalone.
 * The sha1 is always computed from the file's bytes, never typed by hand. A folder (dist/props) means "the zip gg-props-latest.sha1 names", so the
 * update steps are: rebuild the pack, then /prop pack reload.
 * The same server can also serve a packwiz pack folder (gg.packwiz.dir) read-only at /pack/..., so the builder pack updates itself: publishing an
 * update is copying the new folder in, since every request reads the file fresh.
 */
public final class PropPackFile {
    private PropPackFile(){}

    static final String LATEST="gg-props-latest.sha1";

    /** One read of the pack: its bytes and their sha1 (40 lowercase hex). */
    record Pack(Path file,byte[] bytes,String sha1){
        String name(){return PropPackFile.name(sha1);}
    }

    /** gg-props-&lt;sha8&gt;.zip, the name tools/build_prop_pack.py writes and the embedded server serves. */
    static String name(String sha1){
        if(sha1==null||!sha1.matches("[0-9a-f]{40}"))throw new IllegalArgumentException("sha1 must be 40 lowercase hex, got "+sha1);
        return "gg-props-"+sha1.substring(0,8)+".zip";
    }

    static String sha1(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }

    /** A zip path is read as is; a folder holding gg-props-latest.sha1 means the zip that file names. */
    static Path resolve(Path path)throws IOException{
        if(!Files.isDirectory(path))return path;
        Path latest=path.resolve(LATEST);
        if(!Files.isRegularFile(latest))throw new IOException(path+" is a folder without "+LATEST+"; run tools/build_prop_pack.py or point gg.propPack.file at the zip");
        String sha=Files.readString(latest).trim().toLowerCase();
        try{return path.resolve(name(sha));}catch(IllegalArgumentException e){throw new IOException(latest+": "+e.getMessage());}
    }

    static Pack read(Path path)throws IOException{
        Path file=resolve(path);
        if(!Files.isRegularFile(file))throw new IOException("prop pack "+file+" not found");
        byte[] bytes=Files.readAllBytes(file);
        if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K')throw new IOException(file+" is not a zip");
        String sha=sha1(bytes);
        if(Files.isDirectory(path)&&!file.getFileName().toString().equals(name(sha)))throw new IOException(file+" has sha1 "+sha+", not the one "+LATEST+" names; rebuild the pack");
        return new Pack(file,bytes,sha);
    }

    static final String PAPER="assets/minecraft/models/item/paper.json";
    private static final java.util.regex.Pattern CMD=java.util.regex.Pattern.compile("\"custom_model_data\"\\s*:\\s*(\\d+)");

    /** The custom_model_data values the pack's paper.json overrides name; empty when the zip has no paper.json. */
    static java.util.SortedSet<Integer> overrides(byte[] zip)throws IOException{
        java.util.SortedSet<Integer> out=new java.util.TreeSet<>();
        try(var in=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))){
            for(var e=in.getNextEntry();e!=null;e=in.getNextEntry()){
                if(!e.getName().equals(PAPER))continue;
                var m=CMD.matcher(new String(in.readAllBytes(),StandardCharsets.UTF_8));
                while(m.find())try{out.add(Integer.parseInt(m.group(1)));}catch(NumberFormatException x){}
                break;
            }
        }
        return out;
    }

    /**
     * The part custom_model_data values the pack has no exact override for, ascending. A stale pack is the usual cause: paper overrides match
     * any value at or above their own and the last match wins, so every missing part draws as the highest lower part (one part repeated).
     */
    static java.util.List<Integer> missing(byte[] zip,java.util.Collection<Integer> cmds)throws IOException{
        var have=overrides(zip);
        return new java.util.TreeSet<>(cmds).stream().filter(c->!have.contains(c)).toList();
    }

    /** gg.propPack.port: blank or unset is 0 (no embedded server); else 1..65535. */
    static int port(String text){
        if(text==null||text.isBlank())return 0;
        int p;
        try{p=Integer.parseInt(text.trim());}catch(NumberFormatException e){throw new IllegalArgumentException("gg.propPack.port must be a number, got "+text);}
        if(p<1||p>65535)throw new IllegalArgumentException("gg.propPack.port must be 1..65535, got "+p);
        return p;
    }

    /**
     * The URL players download from. gg.propPack.url wins: ending in "/" it is a folder and the pack name is added (so each update gets a new URL),
     * else it is used as is. Without it, an embedded port serves http://&lt;server-ip or localhost&gt;:&lt;port&gt;/&lt;name&gt;. Neither: null, nothing is sent.
     */
    static String url(String url,String host,int port,String name){
        if(url!=null&&!url.isBlank()){url=url.trim();return url.endsWith("/")?url+name:url;}
        if(port<=0)return null;
        String h=host==null||host.isBlank()?"localhost":host.trim();
        if(h.indexOf(':')>=0&&!h.startsWith("["))h="["+h+"]";
        return "http://"+h+":"+port+"/"+name;
    }

    /** gg.propPack.required: unset means true in build mode, false elsewhere. */
    static boolean required(String text,boolean buildMode){
        return text==null||text.isBlank()?buildMode:Boolean.parseBoolean(text.trim());
    }

    static final String PACKWIZ="/pack/";

    /**
     * The file under root that a request path names, or null when it is not a plain path inside root (404/400 for the caller). The raw path must
     * start with /pack/; it is percent-decoded once, then refused if it holds a backslash, NUL, colon, an empty, "." or ".." segment, or a
     * hidden (dot) name. The result's real path (symlinks followed) must still lie inside root's real path.
     */
    static Path packwizFile(Path root,String rawPath){
        if(root==null||rawPath==null||!rawPath.startsWith(PACKWIZ))return null;
        String rel;
        try{rel=URLDecoder.decode(rawPath.substring(PACKWIZ.length()).replace("+","%2B"),StandardCharsets.UTF_8);}catch(IllegalArgumentException e){return null;}
        if(rel.isEmpty()||rel.indexOf('\\')>=0||rel.indexOf('\u0000')>=0||rel.indexOf(':')>=0)return null;
        for(String part:rel.split("/",-1))if(part.isEmpty()||part.startsWith("."))return null;
        try{
            Path base=root.toRealPath();
            Path file=base.resolve(rel).normalize();
            if(!file.startsWith(base)||!Files.isRegularFile(file))return null;
            Path real=file.toRealPath();
            return real.startsWith(base)&&Files.isRegularFile(real)?real:null;
        }catch(IOException|InvalidPathException e){return null;}
    }

    /** Content type by extension: toml/txt/md as UTF-8 text, jar, zip, json; anything else octet-stream. */
    static String contentType(String name){
        String n=name.toLowerCase(java.util.Locale.ROOT);
        if(n.endsWith(".toml")||n.endsWith(".txt")||n.endsWith(".md"))return "text/plain; charset=utf-8";
        if(n.endsWith(".jar"))return "application/java-archive";
        if(n.endsWith(".zip"))return "application/zip";
        if(n.endsWith(".json"))return "application/json";
        return "application/octet-stream";
    }

    /** Serves the current pack at /&lt;name&gt; on all interfaces (port 0 picks a free one); every other path is 404. */
    static HttpServer serve(int port,Supplier<Pack> current)throws IOException{return serve(port,current,null);}

    /**
     * Serves on all interfaces (port 0 picks a free one): the current prop pack at /&lt;name&gt; when there is one, and, when packwiz is not null,
     * every regular file under it at /pack/&lt;relative path&gt;, read fresh per request. Only GET and HEAD (else 405); no directory listing; anything
     * outside the folder is 404, a malformed path 400. Four daemon threads.
     */
    static HttpServer serve(int port,Supplier<Pack> current,Path packwiz)throws IOException{
        HttpServer http=HttpServer.create(new InetSocketAddress(port),0);
        http.createContext("/",ex->{
            try(ex){
                String method=ex.getRequestMethod();
                boolean read=method.equals("GET")||method.equals("HEAD");
                String raw=ex.getRequestURI().getRawPath();
                if(packwiz!=null&&raw!=null&&raw.startsWith(PACKWIZ)){
                    if(!read){ex.sendResponseHeaders(405,-1);return;}
                    Path file=packwizFile(packwiz,raw);
                    if(file==null){ex.sendResponseHeaders(raw.contains("..")||raw.toLowerCase(java.util.Locale.ROOT).contains("%2e")||raw.indexOf('\\')>=0||raw.toLowerCase(java.util.Locale.ROOT).contains("%5c")?400:404,-1);return;}
                    byte[] bytes;
                    try{bytes=Files.readAllBytes(file);}catch(IOException e){ex.sendResponseHeaders(404,-1);return;}
                    send(ex,method,contentType(file.getFileName().toString()),bytes);
                    return;
                }
                Pack p=current.get();
                if(p==null||raw==null||!raw.equals("/"+p.name())){ex.sendResponseHeaders(404,-1);return;}
                if(!read){ex.sendResponseHeaders(405,-1);return;}
                send(ex,method,"application/zip",p.bytes());
            }
        });
        http.setExecutor(Executors.newFixedThreadPool(4,r->{Thread t=new Thread(r,"gg-pack-http");t.setDaemon(true);return t;}));
        http.start();
        return http;
    }

    private static void send(com.sun.net.httpserver.HttpExchange ex,String method,String type,byte[] bytes)throws IOException{
        ex.getResponseHeaders().set("Content-Type",type);
        ex.getResponseHeaders().set("Cache-Control","no-cache");
        if(method.equals("HEAD")){ex.getResponseHeaders().set("Content-Length",Integer.toString(bytes.length));ex.sendResponseHeaders(200,-1);return;}
        ex.sendResponseHeaders(200,bytes.length);
        try(OutputStream out=ex.getResponseBody()){out.write(bytes);}
    }
}
