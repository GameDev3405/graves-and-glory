package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A mesh of quads in one frame (a bone's, or a weapon's), as tools/build_skins.py writes them: the relief of a bone for one
 * skin, or one frame of a weapon (troop depth design, section 3; art pipeline v2, section 2.1). Built once on the resource
 * reload and shared by every entity; nothing is built per frame. A quad is either
 * 13 numbers, axis-aligned: [direction, lo x, y, z, hi x, y, z, u0, v0, u1, v1, u3, v3] with the direction in Minecraft's
 * Direction order, the corners in model px (lo and hi equal on the direction's axis) and the texels of corners 0, 1 and 3
 * (corner 2 is corner 1 + corner 3 - corner 0); or 20 numbers, free: the four corners in model px, then the texel of each
 * corner (a cube of a Blockbench weapon, turned any way). The two are told apart by length. The corners run
 * counter-clockwise seen from the side the normal points to, since the cutout layer culls back faces; the normal is the
 * direction's, or for a free quad the normalised cross product of (corner 1 - corner 0) and (corner 3 - corner 0), worked out
 * once at parse. The texels are divided by the sheet and the px by 16 as ModelPart.Cuboid does, and a quad's normal goes
 * through the entry's normal matrix, as ModelPart.Cuboid does too (the true geometric normal, so Iris lights a side as it is).
 */
final class TroopMesh {
    /** No quads. */
    static final TroopMesh EMPTY=new TroopMesh(new byte[0],new float[0],new float[0],new float[0],new float[0],new float[6]);
    /** The unit normal of each direction id (Direction order: down, up, north, south, west, east). */
    static final int[][] NORMALS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    /** The axis each direction id stands across (0 x, 1 y, 2 z). */
    static final int[] AXIS={1,1,2,2,0,0};
    /** The render thread's scratch: one position and one normal at a time. */
    private static final Vector3f POS=new Vector3f(),NORMAL=new Vector3f();

    /** The direction id of each quad, or -1 for a free (20-number) quad. */
    private final byte[] dir;
    /** 3 floats a quad: its unit normal in model space. */
    private final float[] normal;
    /** 12 floats a quad: the four corners in model px. */
    private final float[] pos;
    /** 8 floats a quad: the texel (u, v) of each of the four corners. */
    private final float[] texel;
    /** 8 floats a quad: the same divided by the sheet. */
    private final float[] uv;
    /** lo x, y, z, hi x, y, z over every corner, in model px. */
    private final float[] bounds;

    private TroopMesh(byte[] dir,float[] normal,float[] pos,float[] texel,float[] uv,float[] bounds){
        this.dir=dir;this.normal=normal;this.pos=pos;this.texel=texel;this.uv=uv;this.bounds=bounds;
    }

    /**
     * Parses the quads of a model file for a sheet of the given size.
     * @throws IllegalArgumentException for a quad that is not 13 or 20 numbers, names no direction, has its min above its max
     *         or different on its direction's axis (13), is not a parallelogram or has no area (20), has no UV area or a texel
     *         off the sheet (the file is left out and logged)
     */
    static TroopMesh parse(JsonArray quads,int sheetW,int sheetH){
        TroopModelLoader.need(sheetW>0&&sheetH>0,"a mesh on a sheet of "+sheetW+" by "+sheetH);
        int n=quads.size();
        if(n==0)return EMPTY;
        byte[] dir=new byte[n];float[] normal=new float[3*n],pos=new float[12*n],texel=new float[8*n],uv=new float[8*n];
        float[] bounds={Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(int i=0;i<n;i++){
            JsonElement el=quads.get(i);TroopModelLoader.need(el.isJsonArray(),"quad "+i+" is not a list of numbers");
            JsonArray q=el.getAsJsonArray();TroopModelLoader.need(q.size()==13||q.size()==20,"quad "+i+" holds "+q.size()+" numbers, not 13 or 20");
            float[] v=new float[q.size()];
            for(int k=0;k<v.length;k++){v[k]=q.get(k).getAsFloat();TroopModelLoader.need(Float.isFinite(v[k]),"quad "+i+" holds a number that is not finite");}
            float[][] c;float[] tex;
            if(v.length==13){
                int d=(int)v[0];TroopModelLoader.need(d==v[0]&&d>=0&&d<=5,"quad "+i+" names direction "+v[0]);
                float[] lo={v[1],v[2],v[3]},hi={v[4],v[5],v[6]};
                for(int a=0;a<3;a++)TroopModelLoader.need(lo[a]<=hi[a],"quad "+i+" has its min above its max on axis "+a);
                TroopModelLoader.need(lo[AXIS[d]]==hi[AXIS[d]],"quad "+i+" is not flat across its direction's axis");
                c=corners(d,lo,hi);
                // the texels of corners 0, 1, 2 and 3: corner 2 is corner 1 + corner 3 - corner 0
                tex=new float[]{v[7],v[8],v[9],v[10],v[9]+v[11]-v[7],v[10]+v[12]-v[8],v[11],v[12]};
                dir[i]=(byte)d;for(int a=0;a<3;a++)normal[3*i+a]=NORMALS[d][a];
            }else{
                c=new float[4][3];for(int k=0;k<4;k++)for(int a=0;a<3;a++)c[k][a]=v[3*k+a];
                for(int a=0;a<3;a++)TroopModelLoader.need(Math.abs(c[2][a]-(c[1][a]+c[3][a]-c[0][a]))<=1e-3f,"quad "+i+" is not a parallelogram (corner 2 is not corner 1 + corner 3 - corner 0)");
                double ux=c[1][0]-c[0][0],uy=c[1][1]-c[0][1],uz=c[1][2]-c[0][2],wx=c[3][0]-c[0][0],wy=c[3][1]-c[0][1],wz=c[3][2]-c[0][2];
                double nx=uy*wz-uz*wy,ny=uz*wx-ux*wz,nz=ux*wy-uy*wx,len=Math.sqrt(nx*nx+ny*ny+nz*nz);
                TroopModelLoader.need(len>1e-6,"quad "+i+" has no area");
                normal[3*i]=(float)(nx/len);normal[3*i+1]=(float)(ny/len);normal[3*i+2]=(float)(nz/len);dir[i]=-1;
                tex=new float[8];for(int k=0;k<8;k++)tex[k]=v[12+k];
            }
            double area=Math.abs((double)(tex[2]-tex[0])*(tex[7]-tex[1])-(double)(tex[3]-tex[1])*(tex[6]-tex[0]));
            TroopModelLoader.need(area>0,"quad "+i+" has no UV area");
            for(int k=0;k<4;k++){
                TroopModelLoader.need(tex[2*k]>=0&&tex[2*k]<=sheetW&&tex[2*k+1]>=0&&tex[2*k+1]<=sheetH,"quad "+i+" has a texel off the "+sheetW+" by "+sheetH+" sheet");
                texel[8*i+2*k]=tex[2*k];texel[8*i+2*k+1]=tex[2*k+1];uv[8*i+2*k]=tex[2*k]/sheetW;uv[8*i+2*k+1]=tex[2*k+1]/sheetH;
                for(int a=0;a<3;a++){pos[12*i+3*k+a]=c[k][a];bounds[a]=Math.min(bounds[a],c[k][a]);bounds[a+3]=Math.max(bounds[a+3],c[k][a]);}
            }
        }
        return new TroopMesh(dir,normal,pos,texel,uv,bounds);
    }

    /**
     * The four corners of an axis-aligned quad, counter-clockwise seen from the side its normal points to: corner 2 is corner
     * 1 + corner 3 - corner 0, and the cross product of (corner 1 - corner 0) and (corner 3 - corner 0) points along the
     * normal. lo and hi are equal on the direction's axis. tools/build_skins.py quad_corners is the same rule.
     */
    static float[][] corners(int dir,float[] lo,float[] hi){
        int a=AXIS[dir],p=(a+1)%3,q=(a+2)%3;
        float[][] pq=NORMALS[dir][a]>0
            ?new float[][]{{lo[p],lo[q]},{hi[p],lo[q]},{hi[p],hi[q]},{lo[p],hi[q]}}
            :new float[][]{{lo[p],lo[q]},{lo[p],hi[q]},{hi[p],hi[q]},{hi[p],lo[q]}};
        float[][] out=new float[4][3];
        for(int k=0;k<4;k++){out[k][a]=lo[a];out[k][p]=pq[k][0];out[k][q]=pq[k][1];}
        return out;
    }

    /** How many quads the mesh has. */
    int quads(){return dir.length;}
    /** The mesh's extent in its frame, model px: lo x, y, z, hi x, y, z. */
    float[] bounds(){return bounds.clone();}
    /** The direction id of a quad, or -1 for a free (20-number) quad. */
    int direction(int i){return dir[i];}
    /** The unit normal of a quad in model space (a direction's, or a free quad's cross product). */
    float[] normal(int i){return new float[]{normal[3*i],normal[3*i+1],normal[3*i+2]};}
    /** The four corners of a quad in model px, corner by corner (12 floats). */
    float[] positions(int i){float[] out=new float[12];System.arraycopy(pos,12*i,out,0,12);return out;}
    /** The texels of a quad's four corners (8 floats). */
    float[] texels(int i){float[] out=new float[8];System.arraycopy(texel,8*i,out,0,8);return out;}

    /** Draws every quad into the consumer through the entry's matrices, as ModelPart.Cuboid does. */
    void render(MatrixStack.Entry entry,VertexConsumer vc,int light,int overlay,float r,float g,float b,float a){render(entry,vc,light,overlay,r,g,b,a,dir.length);}
    /** Draws the first limit quads (a weapon's two big faces come first). Returns how many it drew. */
    int render(MatrixStack.Entry entry,VertexConsumer vc,int light,int overlay,float r,float g,float b,float a,int limit){
        int n=Math.min(limit,dir.length);if(n<=0)return 0;
        Matrix4f matrix=entry.getPositionMatrix();Matrix3f normals=entry.getNormalMatrix();
        for(int i=0;i<n;i++){
            normals.transform(normal[3*i],normal[3*i+1],normal[3*i+2],NORMAL);
            float nx=NORMAL.x,ny=NORMAL.y,nz=NORMAL.z;
            for(int k=0;k<4;k++){
                int o=12*i+3*k;matrix.transformPosition(pos[o]/16f,pos[o+1]/16f,pos[o+2]/16f,POS);
                vc.vertex(POS.x,POS.y,POS.z,r,g,b,a,uv[8*i+2*k],uv[8*i+2*k+1],overlay,light,nx,ny,nz);
            }
        }
        return n;
    }
}
