package thaumcraft.port.auromancy.client.media;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.auromancy.client.media.FocusBoltGeometry.Node;

/** BETA26 FXBolt: three ticks, seeded jagged path, five-sided outer and inner tubes. */
public final class FocusBoltParticle extends Particle {
    private static final ParticleRenderType LAYER=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator t,TextureManager textures){
            RenderSystem.depthMask(false);RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
            // Duplicate reverse faces below instead of leaking glDisable(CULL)
            // into later particle layers, which have no end-state callback.
            RenderSystem.enableCull();
            RenderSystem.setShader(SalisMundusParticle::legacyShader);
            RenderSystem.setShaderTexture(0,ResourceLocation.parse("thaumcraft:textures/misc/essentia.png"));
            return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
        public String toString(){return "THAUMCRAFT_FOCUS_BOLT";}
    };
    private final Vec3 delta;
    private final int seed;
    private final float phase;
    private final float width;
    public FocusBoltParticle(ClientLevel level,Vec3 from,Vec3 to,int colour,float width){
        super(level,from.x,from.y,from.z);this.width=width;delta=to.subtract(from);
        lifetime=3;hasPhysics=false;xd=yd=zd=0;
        phase=(float)(random.nextInt(50)*Math.PI);
        for(int i=1;i<(int)(delta.length()*Math.PI)-1;i++)for(int j=0;j<6;j++)random.nextFloat();
        seed=random.nextInt(1000);
        setColor(((colour>>16)&255)/255f,((colour>>8)&255)/255f,(colour&255)/255f);
        setBoundingBox(new AABB(from,to).inflate(1));
    }
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
    static List<Node> path(Vec3 delta,int seed,float phase,int age,float partial){
        var points=new ArrayList<Node>();var r=new Random(seed);
        float length=(float)(delta.length()*Math.PI);int steps=(int)length;
        points.add(new Node(Vec3.ZERO,.1));float amplitude=(age+partial)/10;
        for(int i=1;i<steps-1;i++){
            float distance=i*(length/steps)+phase;
            var point=delta.scale(i/(double)steps).add(Mth.sin(distance/4)*amplitude+(r.nextFloat()-r.nextFloat())*.1f,
                    Mth.sin(distance/3)*amplitude+(r.nextFloat()-r.nextFloat())*.1f,
                    Mth.sin(distance/2)*amplitude+(r.nextFloat()-r.nextFloat())*.1f);
            points.add(new Node(point,(r.nextInt(4)==0?1-age*.25f:1)*.1));
        }
        points.add(new Node(delta,.1));return points;
    }
    @Override public void render(VertexConsumer out,Camera camera,float partial){
        var nodes=path(delta,seed,phase,age,partial);if(nodes.size()<=2)return;
        Vec3 origin=new Vec3(x,y,z).subtract(camera.getPosition());
        float fade=Mth.clamp(1-age/(float)lifetime,.1f,1);
        for(double scale:new double[]{1,1.0/3}){
            var segments=FocusBoltGeometry.tube(nodes,scale*width);
            for(int i=0;i<segments.size();i++){
                var segment=segments.get(i);
                if(i==0)cap(out,origin,segment.front(),0,fade);
                if(i==segments.size()-1)cap(out,origin,segment.back(),i,fade);
                for(int side=0;side<5;side++){
                    int next=(side+1)%5;
                    Vec3 a=origin.add(segment.front()[side]),b=origin.add(segment.back()[side]);
                    Vec3 c=origin.add(segment.back()[next]),d=origin.add(segment.front()[next]);
                    float u=side/5f,v=(side+1)/5f;
                    vertex(out,a,u,i,fade);vertex(out,b,u,i+1,fade);vertex(out,c,v,i+1,fade);vertex(out,d,v,i,fade);
                    vertex(out,d,v,i,fade);vertex(out,c,v,i+1,fade);vertex(out,b,u,i+1,fade);vertex(out,a,u,i,fade);
                }
            }
        }
    }
    private void cap(VertexConsumer out,Vec3 origin,Vec3[] ring,float v,float fade){
        // GLE tessellates each convex end cap with the current texture coordinate.
        Vec3 a=origin.add(ring[0]);
        for(int i=1;i<ring.length-1;i++){
            Vec3 b=origin.add(ring[i]),c=origin.add(ring[i+1]);
            vertex(out,a,1,v,fade);vertex(out,b,1,v,fade);vertex(out,c,1,v,fade);vertex(out,c,1,v,fade);
            vertex(out,c,1,v,fade);vertex(out,c,1,v,fade);vertex(out,b,1,v,fade);vertex(out,a,1,v,fade);
        }
    }
    private void vertex(VertexConsumer out,Vec3 p,float u,float v,float alpha){
        out.addVertex((float)p.x,(float)p.y,(float)p.z).setUv(u,v).setColor(rCol,gCol,bCol,alpha).setLight(15728880);
    }
    @Override public ParticleRenderType getRenderType(){return LAYER;}
}
