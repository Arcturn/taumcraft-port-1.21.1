package thaumcraft.port.client;

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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** BETA26 FXBolt: three ticks, seeded jagged path, five-sided outer and inner tubes. */
public final class WispBoltParticle extends Particle {
    private static final ParticleRenderType LAYER=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator t,TextureManager textures){
            RenderSystem.depthMask(false);RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,ResourceLocation.parse("thaumcraft:textures/misc/essentia.png"));
            return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
        public String toString(){return "THAUMCRAFT_WISP_BOLT";}
    };
    private final Vec3 delta;
    private final int seed;
    private final float phase;
    public WispBoltParticle(ClientLevel level,Vec3 from,Vec3 to,int colour){
        super(level,from.x,from.y,from.z);delta=to.subtract(from);
        lifetime=3;hasPhysics=false;xd=yd=zd=0;
        phase=(float)(random.nextInt(50)*Math.PI);seed=random.nextInt(1000);
        setColor(((colour>>16)&255)/255f,((colour>>8)&255)/255f,(colour&255)/255f);
        setBoundingBox(new AABB(from,to).inflate(1));
    }
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
    record Node(Vec3 position,double radius){}
    static List<Node> path(Vec3 delta,int seed,float phase,int age,float partial){
        var points=new ArrayList<Node>();var r=new Random(seed);
        float length=(float)(delta.length()*Math.PI);int steps=(int)length;
        points.add(new Node(Vec3.ZERO,.06));float amplitude=(age+partial)/10;
        for(int i=1;i<steps-1;i++){
            float distance=i*(length/steps)+phase;
            var point=delta.scale(i/(double)steps).add(Mth.sin(distance/4)*amplitude+(r.nextFloat()-r.nextFloat())*.1f,
                    Mth.sin(distance/3)*amplitude+(r.nextFloat()-r.nextFloat())*.1f,
                    Mth.sin(distance/2)*amplitude+(r.nextFloat()-r.nextFloat())*.1f);
            points.add(new Node(point,(r.nextInt(4)==0?1-age*.25f:1)*.06));
        }
        points.add(new Node(delta,.06));return points;
    }
    @Override public void render(VertexConsumer out,Camera camera,float partial){
        var nodes=path(delta,seed,phase,age,partial);if(nodes.size()<=2)return;
        Vec3 origin=new Vec3(x,y,z).subtract(camera.getPosition());
        float fade=Mth.clamp(1-age/(float)lifetime,.1f,1);
        // Modern vertex buffers replace the old immediate-mode GLE polycone.
        for(double scale:new double[]{1,1.0/3})for(int i=0;i<nodes.size()-1;i++){
            var a=nodes.get(i);var b=nodes.get(i+1);
            var direction=b.position.subtract(a.position).normalize();
            var u=direction.cross(Math.abs(direction.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
            var v=direction.cross(u).normalize();
            for(int side=0;side<5;side++){
                double angle=side*Math.PI*2/5,next=(side+1)*Math.PI*2/5;
                Vec3 p=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                Vec3 q=u.scale(Math.cos(next)).add(v.scale(Math.sin(next)));
                vertex(out,origin.add(a.position).add(p.scale(a.radius*scale)),side/5f,i,fade);
                vertex(out,origin.add(a.position).add(q.scale(a.radius*scale)),(side+1)/5f,i,fade);
                vertex(out,origin.add(b.position).add(q.scale(b.radius*scale)),(side+1)/5f,i+1,fade);
                vertex(out,origin.add(b.position).add(p.scale(b.radius*scale)),side/5f,i+1,fade);
            }
        }
    }
    private void vertex(VertexConsumer out,Vec3 p,float u,float v,float alpha){
        out.addVertex((float)p.x,(float)p.y,(float)p.z).setUv(u,v).setColor(rCol,gCol,bCol,alpha).setLight(15728880);
    }
    @Override public ParticleRenderType getRenderType(){return LAYER;}
}
