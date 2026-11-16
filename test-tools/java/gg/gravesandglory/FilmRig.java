package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/** Unshipped film rig for the media shoot (MediaShoot). Drives a free camera along a path sampled once per rendered frame
 *  (testing/mixin/Film*Mixin read it), reads every rendered frame back and pipes it to ffmpeg, so one frame in the game is
 *  exactly one frame in the file, and logs every frame time so a clip with hitches can be thrown away. Never in releases. */
public final class FilmRig {
    private FilmRig(){}
    /** Seconds since the shot started to {x, y, z, yaw, pitch, fov}. */
    public interface Path{double[] at(double t);}
    /** Seconds to the local player's right-arm pitch, yaw, roll in radians, or null for the vanilla arm. */
    public interface Pose{float[] at(double t);}
    /** One shot. warm frames are rendered (camera held at t=0) before the first recorded frame so shadows, temporal
     *  filtering and chunk meshes settle; stills maps a recorded frame to a screenshot file name. */
    public record Take(String name,int fps,int warm,int frames,Path path,Pose pose,File video,Map<Integer,String> stills,Map<Integer,Runnable> events,double clearance){}

    public static volatile Take take;
    /** Slow-time factor for title-screen slow motion (FilmSlowServerMixin, FilmSlowClockMixin): the integrated server's 50 ms
     *  tick slot and the client's render clock both stretch by this factor, so ticks interpolate smoothly and a 60 fps
     *  capture shows true slow motion. 1 is vanilla speed; a take's event switches it on, the end of the take resets it. */
    public static volatile int slow=1;
    public static volatile long frame;
    public static volatile boolean done;
    /** True while the media shoot runs: InGameHud.render is cancelled (FilmHudMixin). */
    public static volatile boolean noHud;
    public static volatile String lastStats="";
    static double[] cam;static long camFrame=-1;static double lift;static Recorder rec;
    static final ArrayList<Long> times=new ArrayList<>();static long lastNanos;
    /** World ticks and real time at the take's start: its tick rate is logged at the end, so a slowed take shows its slowdown (20 a second is vanilla). */
    static long startTick,startNanos;

    public static void start(Take t){
        var c=MinecraftClient.getInstance();
        frame=0;camFrame=-1;lift=0;times.clear();lastNanos=0;done=false;
        if(c.options.getMaxFps().getValue()!=t.fps())c.options.getMaxFps().setValue(t.fps());
        if(t.video()!=null){try{rec=new Recorder(t.video(),c.getFramebuffer().textureWidth,c.getFramebuffer().textureHeight,t.fps());}catch(Exception e){System.out.println("[film] recorder failed: "+e);rec=null;}}
        startTick=c.world==null?0:c.world.getTime();startNanos=System.nanoTime();
        System.out.println("[film] take "+t.name()+" starts at slow x"+slow);
        take=t;
    }
    /** The camera for this frame, or null while no shot runs. Called from Camera.update, so it is memoised per frame. */
    public static double[] sample(){
        Take t=take;if(t==null)return null;
        if(camFrame==frame&&cam!=null)return cam;
        double sec=Math.max(0,(frame-t.warm())/(double)t.fps());
        double[] v=t.path().at(sec).clone();
        var w=MinecraftClient.getInstance().world;
        if(w!=null&&t.clearance()>0){
            int x=(int)Math.floor(v[0]),z=(int)Math.floor(v[2]);double ground=-1e9;
            for(int dx=-3;dx<=3;dx+=3)for(int dz=-3;dz<=3;dz+=3)ground=Math.max(ground,w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x+dx,z+dz));
            double need=Math.max(0,ground+t.clearance()-v[1]);
            lift+=(need-lift)*(need>lift?.35:.06); // up quickly so it never clips, down slowly so a ridge does not bounce it
            v[1]+=lift;
            if(v.length>=9&&lift>.01){double dx=v[6]-v[0],dy=v[7]-v[1],dz=v[8]-v[2];v[4]=-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));} // keep looking at the target after the lift
        }
        TitleDemos.pose=t.pose()==null?null:t.pose().at(sec);
        cam=v;camFrame=frame;return v;
    }
    public static double fov(){var v=sample();return v==null?-1:v[5];}

    /** Called at the end of every rendered frame. */
    public static void afterFrame(MinecraftClient c){
        Take t=take;if(t==null)return;
        long now=System.nanoTime();
        int rf=(int)(frame-t.warm());
        if(rf>=0){
            if(lastNanos!=0&&rf>0)times.add(now-lastNanos);
            lastNanos=now;
            Framebuffer fb=c.getFramebuffer();
            String still=t.stills().get(rf);
            if(still!=null)ScreenshotRecorder.saveScreenshot(c.runDirectory,still,fb,x->{});
            if(rec!=null)rec.push(fb);
            Runnable ev=t.events().get(rf);if(ev!=null)ev.run();
        }
        frame++;
        if(frame>=t.warm()+t.frames())finish(t);
    }
    static void finish(Take t){
        if(rec!=null){rec.close();rec=null;}
        var ms=new ArrayList<Double>();for(long n:times)ms.add(n/1e6);
        var bad=new StringBuilder();for(int i=0;i<ms.size();i++)if(ms.get(i)>1000.0/t.fps()*1.35)bad.append(i).append('=').append(Math.round(ms.get(i))).append(' ');
        if(bad.length()>0)System.out.println("[film] slow frames (index=ms): "+bad);
        Collections.sort(ms);
        double avg=ms.stream().mapToDouble(d->d).average().orElse(0),p95=ms.isEmpty()?0:ms.get((int)(ms.size()*.95)),p99=ms.isEmpty()?0:ms.get((int)(ms.size()*.99)),max=ms.isEmpty()?0:ms.get(ms.size()-1);
        long over=ms.stream().filter(d->d>1000.0/t.fps()*1.35).count();
        lastStats=String.format("{\"take\":\"%s\",\"frames\":%d,\"avg_ms\":%.2f,\"p95_ms\":%.2f,\"p99_ms\":%.2f,\"max_ms\":%.2f,\"over_135pct\":%d}",t.name(),ms.size(),avg,p95,p99,max,over);
        System.out.println("[film] "+lastStats);
        var w=MinecraftClient.getInstance().world;double secs=(System.nanoTime()-startNanos)/1e9;
        if(w!=null&&secs>0)System.out.printf("[film] %s ran %d ticks in %.1f s (%.1f a second) at slow x%d at its end%n",t.name(),w.getTime()-startTick,secs,(w.getTime()-startTick)/secs,slow);
        if(t.video()!=null)try{java.nio.file.Files.writeString(new File(t.video().getPath()+".json").toPath(),lastStats);}catch(Exception ignored){}
        TitleDemos.pose=null;take=null;cam=null;done=true;FilmRig.slow=1; // the static factor, not a local
    }

    // ---- paths ------------------------------------------------------------------------------------------------
    /** Trapezoid ease: accelerate over the first ramp share of the time, cruise, brake over the last share (0 = constant speed). */
    static double ease(double u,double a){
        u=Math.max(0,Math.min(1,u));if(a<=0)return u;a=Math.min(a,.5);double v=1/(1-a);
        return u<a?v*u*u/(2*a):u>1-a?1-v*(1-u)*(1-u)/(2*a):v*(u-a/2);
    }
    static Vec3d cr(Vec3d[] p,double u){
        int n=p.length;if(n==1)return p[0];
        double s=Math.max(0,Math.min(1,u))*(n-1);int i=Math.min(n-2,(int)Math.floor(s));double f=s-i;
        Vec3d p0=p[Math.max(0,i-1)],p1=p[i],p2=p[i+1],p3=p[Math.min(n-1,i+2)];
        double f2=f*f,f3=f2*f;
        return new Vec3d(cr1(p0.x,p1.x,p2.x,p3.x,f,f2,f3),cr1(p0.y,p1.y,p2.y,p3.y,f,f2,f3),cr1(p0.z,p1.z,p2.z,p3.z,f,f2,f3));
    }
    static double cr1(double a,double b,double c,double d,double f,double f2,double f3){return .5*((2*b)+(-a+c)*f+(2*a-5*b+4*c-d)*f2+(-a+3*b-3*c+d)*f3);}
    static double crs(double[] p,double u){
        int n=p.length;if(n==1)return p[0];
        double s=Math.max(0,Math.min(1,u))*(n-1);int i=Math.min(n-2,(int)Math.floor(s));double f=s-i;
        return cr1(p[Math.max(0,i-1)],p[i],p[i+1],p[Math.min(n-1,i+2)],f,f*f,f*f*f);
    }
    static double[] aim(Vec3d from,Vec3d to,double fov){
        double dx=to.x-from.x,dy=to.y-from.y,dz=to.z-from.z;
        return new double[]{from.x,from.y,from.z,Math.toDegrees(Math.atan2(-dx,dz)),-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))),fov,to.x,to.y,to.z};
    }
    /** Camera positions and look-at points as smooth splines over the shot, eased at both ends. */
    public static Path rail(double seconds,double ramp,Vec3d[] pos,Vec3d[] look,double[] fov){
        return t->{double u=ease(t/seconds,ramp);return aim(cr(pos,u),cr(look,u),crs(fov,u));};
    }
    /** A circle round a centre: radius, height above the centre, start and end bearing in degrees (0 = +z, 90 = -x), looking at a point. */
    public static Path orbit(double seconds,double ramp,Vec3d centre,double radius,double height,double fromDeg,double toDeg,Vec3d look,double fov){
        return t->{double u=ease(t/seconds,ramp),a=Math.toRadians(fromDeg+(toDeg-fromDeg)*u);
            return aim(new Vec3d(centre.x-Math.sin(a)*radius,centre.y+height,centre.z+Math.cos(a)*radius),look,fov);};
    }
    /** A drone: flies a spline and looks a little further along it (lead, a share of the path), dropped by drop blocks. */
    public static Path chase(double seconds,double ramp,Vec3d[] pos,double lead,double drop,double[] fov){
        return t->{double u=ease(t/seconds,ramp);return aim(cr(pos,u),cr(pos,Math.min(1,u+lead)).add(0,-drop,0),crs(fov,u));};
    }
    /** An orbit whose radius, height, look-at point and field of view all drift from their start to their end value. */
    public static Path orbitEx(double seconds,double ramp,Vec3d centre,double r0,double r1,double h0,double h1,double fromDeg,double toDeg,Vec3d look0,Vec3d look1,double fov0,double fov1){
        return t->{double u=ease(t/seconds,ramp),a=Math.toRadians(fromDeg+(toDeg-fromDeg)*u),r=r0+(r1-r0)*u,h=h0+(h1-h0)*u;
            return aim(new Vec3d(centre.x-Math.sin(a)*r,centre.y+h,centre.z+Math.cos(a)*r),look0.add(look1.subtract(look0).multiply(u)),fov0+(fov1-fov0)*u);};
    }
    /** The right arm's pitch as (seconds, radians) keys, eased between them; the roll leans it slightly outward. */
    public static Pose arm(double... k){
        return t->{int n=k.length/2;double pitch=k[1];
            if(t>=k[(n-1)*2])pitch=k[(n-1)*2+1];
            else for(int i=0;i+1<n;i++){double t0=k[i*2],t1=k[i*2+2];if(t>=t0&&t<t1){double u=(t-t0)/(t1-t0);u=u*u*(3-2*u);pitch=k[i*2+1]+(k[i*2+3]-k[i*2+1])*u;break;}}
            return new float[]{(float)pitch,0f,-0.12f};};
    }
    public static Path hold(Vec3d pos,Vec3d look,double fov){double[] v=aim(pos,look,fov);return t->v;}
    public static Path hold(double[] v){return t->v;}

    // ---- frame readback and ffmpeg ------------------------------------------------------------------------------
    static final class Recorder{
        static final int SLOTS=3;
        final Process proc;final OutputStream out;final Thread writer;volatile Throwable error;
        final ArrayBlockingQueue<byte[]> full=new ArrayBlockingQueue<>(12),free=new ArrayBlockingQueue<>(12);
        final int n;final int[] pbo=new int[SLOTS];long issued,harvested;
        Recorder(File file,int w,int h,int fps)throws Exception{
            n=w*h*4;
            file.getParentFile().mkdirs();for(int i=0;i<12;i++)free.add(new byte[n]);
            for(int i=0;i<SLOTS;i++){pbo[i]=GL15.glGenBuffers();GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo[i]);GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER,n,GL15.GL_STREAM_READ);}
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            String ff=System.getenv().getOrDefault("GRAVES_FILM_FFMPEG","C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe");
            var cmd=new ArrayList<>(List.of(ff,"-y","-hide_banner","-loglevel","warning","-f","rawvideo","-pix_fmt","rgba","-s",w+"x"+h,"-framerate",""+fps,"-i","-",
                "-vf","vflip,scale=out_color_matrix=bt709:out_range=tv,format=yuv420p","-c:v","h264_nvenc","-preset","p5","-tune","hq","-rc","vbr","-cq","15","-b:v","0","-maxrate","250M","-bufsize","500M",
                "-profile:v","high","-color_primaries","bt709","-color_trc","bt709","-colorspace","bt709","-color_range","tv","-movflags","+faststart",file.getPath()));
            var pb=new ProcessBuilder(cmd);pb.redirectErrorStream(true);pb.redirectOutput(new File(file.getPath()+".ffmpeg.log"));
            proc=pb.start();out=proc.getOutputStream();
            writer=new Thread(()->{try{while(true){byte[] a=full.take();if(a.length==0)break;out.write(a);free.put(a);}}catch(Throwable e){error=e;}},"film-writer");writer.start();
        }
        /** Starts an asynchronous copy of the finished frame into the next slot; the frame from two renders ago is collected, so the GPU never stalls the render thread. */
        void push(Framebuffer fb){
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo[(int)(issued%SLOTS)]);
            fb.beginRead();GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,0L);fb.endRead();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            issued++;
            if(issued-harvested>=SLOTS-1)harvest();
        }
        void harvest(){
            try{
                byte[] a=free.take();
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo[(int)(harvested%SLOTS)]);
                ByteBuffer m=GL30.glMapBufferRange(GL21.GL_PIXEL_PACK_BUFFER,0,n,GL30.GL_MAP_READ_BIT);
                if(m!=null){m.get(0,a,0,n);GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);}
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
                harvested++;full.put(a);
            }catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
        void close(){
            while(harvested<issued)harvest();
            for(int b:pbo)GL15.glDeleteBuffers(b);
            try{full.put(new byte[0]);writer.join();out.close();proc.waitFor();}catch(Exception e){System.out.println("[film] recorder close: "+e);}
            if(error!=null)System.out.println("[film] writer error: "+error);
        }
    }
}
