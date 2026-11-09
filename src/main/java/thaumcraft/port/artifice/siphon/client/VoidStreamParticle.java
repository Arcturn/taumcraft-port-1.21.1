package thaumcraft.port.artifice.siphon.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** FXVoidStream's moving, tapering 40-node trail, with the original two star-field passes. */
final class VoidStreamParticle extends Particle {
    private static final Layer[] LAYERS=layers();
    private final Vec3 start,target;
    private final int seed;
    private final ArrayList<Node> trail=new ArrayList<>();
    private Vec3[] points={};
    private double[] radii={};
    private int length=40;
    private float scale;

    static void spawn(ParticleEngine engine,ClientLevel level,Vec3 from,Vec3 target,int seed){
        var particle=new VoidStreamParticle(level,from,target,seed);engine.add(particle);engine.add(new InnerPass(particle));
    }
    private VoidStreamParticle(ClientLevel level,Vec3 start,Vec3 target,int seed){
        super(level,start.x,start.y,start.z);this.start=start;this.target=target;this.seed=seed;
        scale=(float)(.04f*(1+random.nextGaussian()*.15f));lifetime=Math.max(1,(int)(start.distanceTo(target)*21))*2;
        xd=Mth.sin(seed/4f)*.025f;yd=Mth.sin(seed/3f)*.025f;zd=Mth.sin(seed/2f)*.025f;gravity=.2f;
        trail.add(new Node(new Vec3(0,0,.001),0));trail.add(new Node(new Vec3(0,0,.001),0));
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime||length<1){remove();return;}
        yd+=.01*gravity;move(xd,yd,zd);
        xd=Mth.clamp((float)(xd*.985),-.04f,.04f);yd=Mth.clamp((float)(yd*.985),-.04f,.04f);zd=Mth.clamp((float)(zd*.985),-.04f,.04f);
        Vec3 delta=target.subtract(x,y,z);double distance=delta.length();
        // Coincident end points must never poison the mesh with the source's 0/0 velocity.
        if(distance>1e-8){Vec3 pull=delta.scale(.01/(distance*Math.min(1,distance)));xd+=pull.x+random.nextGaussian()*.015f;yd+=pull.y+random.nextGaussian()*.015f;zd+=pull.z+random.nextGaussian()*.015f;}
        float radius=scale*(.75f+Mth.sin((seed+age)/2f)*.25f);
        if(distance<.5){float taper=Mth.sin((float)(distance*Math.PI/2));radius*=taper;scale*=taper;}
        if(scale>.001)trail.add(new Node(new Vec3(x-start.x,y-start.y,z-start.z),radius));else length--;
        if(trail.size()>length)trail.removeFirst();
        points=new Vec3[trail.size()];radii=new double[trail.size()];int index=trail.size();
        for(var node:trail){
            int c=--index;float variance=1+Mth.sin((c+age)/3f)*.2f;
            points[c]=node.position.add(Mth.sin((c+age)/6f)*.01f,Mth.sin((c+age)/7f)*.01f,Mth.sin((c+age)/8f)*.01f);
            double radiusHere=node.radius*variance;
            if(c>trail.size()-10)radiusHere*=Mth.cos((float)(((c-(trail.size()-12))/10f)*Math.PI/2));
            radii[c]=switch(c){case 0,1->0;case 2->(scale*.5+radiusHere)/2;case 3->(scale+radiusHere)/2;case 4->(scale+radiusHere*2)/3;default->radiusHere;};
        }
        // Preserve the source's harmless random draws after geometry generation.
        if(trail.size()>2&&random.nextBoolean()){random.nextInt(3);random.nextBoolean();}
    }
    @Override public void render(VertexConsumer out,Camera camera,float partial){emit(out,camera,1.5);}
    private void emit(VertexConsumer out,Camera camera,double multiplier){
        if(points.length<3)return;Vec3 origin=start.subtract(camera.getPosition());
        // TC6 asks GLE for three-sided cross sections. Ring axes follow the bisector
        // of adjoining segments, avoiding gaps and overlaps at the noisy bends.
        var rings=new Vec3[points.length][3];Vec3 previousU=null;
        for(int i=0;i<points.length;i++){
            Vec3 tangent=points[Math.min(i+1,points.length-1)].subtract(points[Math.max(0,i-1)]).normalize();
            if(tangent.lengthSqr()<1e-12)tangent=new Vec3(0,1,0);
            Vec3 u=previousU==null?Vec3.ZERO:previousU.subtract(tangent.scale(previousU.dot(tangent))).normalize();
            if(u.lengthSqr()<1e-12)u=tangent.cross(Math.abs(tangent.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
            Vec3 v=tangent.cross(u).normalize();previousU=u;
            for(int k=0;k<3;k++){double angle=k*Math.PI*2/3;rings[i][k]=origin.add(points[i]).add(u.scale(Math.cos(angle)*radii[i]*multiplier)).add(v.scale(Math.sin(angle)*radii[i]*multiplier));}
        }
        for(int i=1;i<points.length-2;i++)for(int side=0;side<3;side++){
            int next=(side+1)%3;vertex(out,rings[i][side]);vertex(out,rings[i][next]);vertex(out,rings[i+1][next]);vertex(out,rings[i+1][side]);
        }
    }
    private static void vertex(VertexConsumer out,Vec3 point){out.addVertex((float)point.x,(float)point.y,(float)point.z);}
    @Override public AABB getRenderBoundingBox(float partial){return new AABB(start,target).inflate(1);}
    @Override public ParticleRenderType getRenderType(){return LAYERS[0];}
    private record Node(Vec3 position,double radius){}
    private static final class InnerPass extends Particle {
        private final VoidStreamParticle outer;
        InnerPass(VoidStreamParticle outer){super(outer.level,outer.start.x,outer.start.y,outer.start.z);this.outer=outer;hasPhysics=false;}
        @Override public void tick(){if(age++>=outer.lifetime||!outer.isAlive())remove();}
        @Override public void render(VertexConsumer out,Camera camera,float partial){outer.emit(out,camera,.5);}
        @Override public AABB getRenderBoundingBox(float partial){return outer.getRenderBoundingBox(partial);}
        @Override public ParticleRenderType getRenderType(){return LAYERS[1];}
    }
    private static Layer[] layers(){
        var first=new Layer();var second=new Layer();
        var outer=System.identityHashCode(first)<System.identityHashCode(second)?first:second;
        var inner=outer==first?second:first;outer.outer=true;return new Layer[]{outer,inner};
    }
    private static final class Layer implements ParticleRenderType {
        private boolean outer;
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){
            RenderSystem.depthMask(!outer);RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,outer?GlStateManager.DestFactor.ONE:GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(VoidSiphonRenderer::activeShader);RenderSystem.setShaderTexture(0,VoidSiphonRenderer.STARS);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
        }
        @Override public String toString(){return outer?"THAUMCRAFT_VOID_STREAM_GLOW":"THAUMCRAFT_VOID_STREAM_CORE";}
    }
}
