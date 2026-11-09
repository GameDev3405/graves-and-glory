package gg.gravesandglory;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Prop pack delivery (prop kit plan task S7): pack name, sha1 from the bytes, folder resolution through gg-props-latest.sha1, url building,
 *  required default, port parsing, and the embedded HTTP server serving exactly /gg-props-<sha8>.zip with the current bytes; plus the packwiz
 *  folder at /pack/...: path guard (.., encoded, absolute, backslash, symlink out), read-only, no listing, fresh reads, content types. */
public final class PropPackFileCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static boolean refuses(Runnable r){try{r.run();return false;}catch(IllegalArgumentException e){return true;}}
 static byte[] zip(String body){return ("PK\u0003\u0004"+body).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);}
 static int get(int port,String path,byte[][] body)throws IOException{
  HttpURLConnection c=(HttpURLConnection)URI.create("http://localhost:"+port+path).toURL().openConnection();
  int code=c.getResponseCode();
  if(code==200)try(InputStream in=c.getInputStream()){body[0]=in.readAllBytes();}
  c.disconnect();return code;
 }

 /** A raw request line, so nothing on the client side normalizes the path. Returns the status code. */
 static int raw(int port,String method,String path)throws IOException{
  try(java.net.Socket s=new java.net.Socket("localhost",port)){
   s.getOutputStream().write((method+" "+path+" HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
   String line=new String(s.getInputStream().readNBytes(12),java.nio.charset.StandardCharsets.ISO_8859_1);
   return Integer.parseInt(line.substring(9,12));
  }
 }
 static String type(int port,String path)throws IOException{
  HttpURLConnection c=(HttpURLConnection)URI.create("http://localhost:"+port+path).toURL().openConnection();
  c.getResponseCode();String t=c.getContentType();c.disconnect();return t;
 }

 static void packwiz()throws Exception{
  Path base=Files.createTempDirectory("gg-packwiz");
  Path root=Files.createDirectories(base.resolve("pack"));
  try{
   Files.writeString(root.resolve("pack.toml"),"name = \"x\"\n");
   Files.createDirectories(root.resolve("mods"));
   Files.writeString(root.resolve("mods/a.pw.toml"),"name = \"a\"\n");
   Files.write(root.resolve("mods/graves-and-glory-0.5.1.jar"),zip("jar"));
   Files.writeString(root.resolve("mods/x+y.txt"),"plus");
   Files.writeString(base.resolve("secret.txt"),"outside");
   Files.writeString(root.resolve(".hidden"),"h");
   check(PropPackFile.packwizFile(root,"/pack/pack.toml")!=null&&PropPackFile.packwizFile(root,"/pack/mods/a.pw.toml")!=null,"guard: plain files resolve");
   check(PropPackFile.packwizFile(root,"/pack/mods/x+y.txt")!=null,"guard: + is a literal plus");
   check(PropPackFile.packwizFile(root,"/pack/mods%2Fa.pw.toml")!=null,"guard: encoded slash inside the folder is fine");
   for(String bad:new String[]{"/pack/../secret.txt","/pack/%2e%2e/secret.txt","/pack/%2E%2E/secret.txt","/pack/mods/../../secret.txt","/pack/%2e%2e%2fsecret.txt",
     "/pack/..%5csecret.txt","/pack/mods\\..\\..\\secret.txt","/pack/C:/Windows/win.ini","/pack//etc/passwd","/pack/"+base.resolve("secret.txt").toString().replace('\\','/'),
     "/pack/","/pack/mods","/pack/mods/","/pack/./pack.toml","/pack/.hidden","/pack/%00pack.toml","/pack/%zz","/secret.txt","/pack/missing.toml"})
    check(PropPackFile.packwizFile(root,bad)==null,"guard refuses "+bad);
   check(PropPackFile.packwizFile(null,"/pack/pack.toml")==null,"guard: no folder, nothing");
   boolean linked=false;
   try{Files.createSymbolicLink(root.resolve("out.txt"),base.resolve("secret.txt"));linked=true;}catch(IOException|UnsupportedOperationException|SecurityException e){}
   if(linked)check(PropPackFile.packwizFile(root,"/pack/out.txt")==null,"guard: a symlink out of the folder is refused");
   else System.out.println("PropPackFileCheck: symlinks not permitted here; symlink-out case skipped");
   check(PropPackFile.contentType("pack.toml").startsWith("text/plain")&&PropPackFile.contentType("a.JAR").equals("application/java-archive")
    &&PropPackFile.contentType("p.zip").equals("application/zip")&&PropPackFile.contentType("x.bin").equals("application/octet-stream"),"content types");

   HttpServer http=PropPackFile.serve(0,()->null,root);
   try{
    int port=http.getAddress().getPort();byte[][] body=new byte[1][];
    check(get(port,"/pack/pack.toml",body)==200&&new String(body[0]).equals("name = \"x\"\n"),"serves pack.toml");
    check(type(port,"/pack/pack.toml").startsWith("text/plain")&&type(port,"/pack/mods/graves-and-glory-0.5.1.jar").equals("application/java-archive"),"served content types");
    check(get(port,"/pack/mods/graves-and-glory-0.5.1.jar",body)==200&&Arrays.equals(body[0],zip("jar")),"serves the jar bytes");
    Files.writeString(root.resolve("pack.toml"),"name = \"y\"\n");
    check(get(port,"/pack/pack.toml",body)==200&&new String(body[0]).equals("name = \"y\"\n"),"reads fresh: a copied-in update is served at once");
    check(raw(port,"HEAD","/pack/pack.toml")==200,"HEAD allowed");
    for(String m:new String[]{"POST","PUT","DELETE"})check(raw(port,m,"/pack/pack.toml")==405,m+" is 405");
    check(raw(port,"GET","/pack/")==404&&raw(port,"GET","/pack/mods/")==404&&raw(port,"GET","/pack/mods")==404,"no directory listing");
    check(raw(port,"GET","/pack/missing.toml")==404,"missing file 404");
    for(String bad:new String[]{"/pack/../secret.txt","/pack/%2e%2e/secret.txt","/pack/%2E%2e%2Fsecret.txt","/pack/mods/../../secret.txt","/pack/..%5csecret.txt"}){
     int code=raw(port,"GET",bad);check(code==400||code==404,"over HTTP refused ("+code+"): "+bad);
    }
    check(raw(port,"GET","/secret.txt")==404&&raw(port,"GET","/")==404,"outside /pack/ and no prop pack: 404");
    Files.delete(root.resolve("mods/a.pw.toml"));
    check(raw(port,"GET","/pack/mods/a.pw.toml")==404,"a removed file is gone at once");
   }finally{http.stop(0);}
  }finally{
   try(var w=Files.walk(base)){for(Path p:(Iterable<Path>)w.sorted(java.util.Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}
  }
 }

 public static void main(String[] args)throws Exception{
  check(PropPackFile.sha1(new byte[0]).equals("da39a3ee5e6b4b0d3255bfef95601890afd80709"),"sha1 of nothing");
  String sha="246af3b5"+"0".repeat(32);
  check(PropPackFile.name(sha).equals("gg-props-246af3b5.zip"),"name is gg-props-<sha8>.zip");
  check(refuses(()->PropPackFile.name("246AF3B5")),"name refuses a short or upper-case sha1");

  check(PropPackFile.url(null,"",0,"p.zip")==null,"no url and no port: nothing to send");
  check(PropPackFile.url(null,"",0,"p.zip")==null,"port 0 is no server");
  check(PropPackFile.url(null,"",25621,"p.zip").equals("http://localhost:25621/p.zip"),"port, blank server-ip: localhost");
  check(PropPackFile.url("  ","10.0.0.5",25621,"p.zip").equals("http://10.0.0.5:25621/p.zip"),"port with server-ip");
  check(PropPackFile.url(null,"::1",25621,"p.zip").equals("http://[::1]:25621/p.zip"),"IPv6 host bracketed");
  check(PropPackFile.url("https://h.example/packs/","x",25621,"p.zip").equals("https://h.example/packs/p.zip"),"url folder gets the name, wins over the port");
  check(PropPackFile.url("https://h.example/pack.zip",null,0,"p.zip").equals("https://h.example/pack.zip"),"url file used as is");

  check(PropPackFile.required(null,true)&&!PropPackFile.required(null,false)&&!PropPackFile.required("",false),"required defaults to build mode");
  check(!PropPackFile.required("false",true)&&PropPackFile.required("true",false),"required set explicitly");

  check(PropPackFile.port(null)==0&&PropPackFile.port(" ")==0&&PropPackFile.port("25621")==25621,"port parse");
  check(refuses(()->PropPackFile.port("0"))&&refuses(()->PropPackFile.port("70000"))&&refuses(()->PropPackFile.port("x")),"port refuses 0, >65535, text");

  Path dir=Files.createTempDirectory("gg-prop-pack");
  try{
   byte[] a=zip("one"),b=zip("two");
   String sa=PropPackFile.sha1(a),sb=PropPackFile.sha1(b);
   Path za=dir.resolve(PropPackFile.name(sa));Files.write(za,a);
   PropPackFile.Pack pa=PropPackFile.read(za);
   check(pa.sha1().equals(sa)&&Arrays.equals(pa.bytes(),a)&&pa.name().equals(za.getFileName().toString()),"read a zip: sha1 from its bytes");
   boolean noLatest=false;try{PropPackFile.read(dir);}catch(IOException e){noLatest=e.getMessage().contains("gg-props-latest.sha1");}
   check(noLatest,"folder without gg-props-latest.sha1 is refused, naming the file");
   Files.writeString(dir.resolve("gg-props-latest.sha1"),sa+"\n");
   check(PropPackFile.read(dir).sha1().equals(sa),"folder: the zip gg-props-latest.sha1 names");
   Path notZip=dir.resolve("x.zip");Files.writeString(notZip,"hello");
   boolean refused=false;try{PropPackFile.read(notZip);}catch(IOException e){refused=true;}
   check(refused,"a non-zip is refused");
   Files.write(za,b); // the named zip no longer matches its sha1
   refused=false;try{PropPackFile.read(dir);}catch(IOException e){refused=e.getMessage().contains("rebuild");}
   check(refused,"folder: a zip whose sha1 is not the one named is refused");

   PropPackFile.Pack[] cur={pa};
   HttpServer http=PropPackFile.serve(0,()->cur[0]);
   try{
    int port=http.getAddress().getPort();byte[][] body=new byte[1][];
    check(get(port,"/"+pa.name(),body)==200&&Arrays.equals(body[0],a),"serves the pack bytes at /<name>");
    check(get(port,"/",body)==404&&get(port,"/gg-props-00000000.zip",body)==404,"every other path is 404");
    Path zb=dir.resolve(PropPackFile.name(sb));Files.write(zb,b);cur[0]=PropPackFile.read(zb);
    check(get(port,"/"+cur[0].name(),body)==200&&Arrays.equals(body[0],b),"after reload the new pack is served at its new name");
    check(get(port,"/"+pa.name(),body)==404,"the old name is gone");
    cur[0]=null;
    check(get(port,"/"+PropPackFile.name(sb),body)==404,"no pack: 404");
   }finally{http.stop(0);}
  }finally{
   try(var s=Files.list(dir)){for(Path p:(Iterable<Path>)s::iterator)Files.delete(p);}
   Files.delete(dir);
  }
  packwiz();
  System.out.println("PropPackFileCheck: "+count+" checks passed");
 }
}
