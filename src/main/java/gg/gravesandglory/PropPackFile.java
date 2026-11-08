package gg.gravesandglory;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
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

    /** Serves the current pack at /&lt;name&gt; on all interfaces (port 0 picks a free one); every other path is 404. Two daemon threads. */
    static HttpServer serve(int port,Supplier<Pack> current)throws IOException{
        HttpServer http=HttpServer.create(new InetSocketAddress(port),0);
        http.createContext("/",ex->{
            try(ex){
                Pack p=current.get();
                String method=ex.getRequestMethod();
                if(p==null||!ex.getRequestURI().getPath().equals("/"+p.name())){ex.sendResponseHeaders(404,-1);return;}
                if(!method.equals("GET")&&!method.equals("HEAD")){ex.sendResponseHeaders(405,-1);return;}
                ex.getResponseHeaders().set("Content-Type","application/zip");
                ex.getResponseHeaders().set("Cache-Control","no-cache");
                if(method.equals("HEAD")){ex.sendResponseHeaders(200,-1);return;}
                ex.sendResponseHeaders(200,p.bytes().length);
                try(OutputStream out=ex.getResponseBody()){out.write(p.bytes());}
            }
        });
        http.setExecutor(Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"gg-prop-pack-http");t.setDaemon(true);return t;}));
        http.start();
        return http;
    }
}
