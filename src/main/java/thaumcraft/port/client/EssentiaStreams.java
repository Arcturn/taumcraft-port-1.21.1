package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import thaumcraft.port.essentia.EssentiaStreamPayload;
import thaumcraft.port.essentia.EssentiaTransferBlock;

/** BETA26 FXEssentiaStream motion, taper, texture and eight-sided tube, rendered with modern buffers. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class EssentiaStreams {
    private static final Map<EssentiaStreamPayload,Stream> ACTIVE=new HashMap<>();
    private static ClientLevel lastLevel;
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/essentia.png");
    private static final ParticleRenderType LAYER=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){
            RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,TEXTURE);
            textures.getTexture(TEXTURE).setFilter(false,false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
        @Override public String toString(){return "THAUMCRAFT_ESSENTIA_STREAM";}
    };
    @SubscribeEvent public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event){ACTIVE.clear();lastLevel=null;}
    public static void receive(EssentiaStreamPayload packet){
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null||mc.player==null||!packet.valid()||mc.player.distanceToSqr(Vec3.atCenterOf(packet.to()))>64*64)return;
        if(lastLevel!=level){ACTIVE.clear();lastLevel=level;}
        ACTIVE.values().removeIf(stream->!stream.isAlive());
        Stream previous=ACTIVE.get(packet);
        if(previous!=null&&previous.extend())return;
        var stream=new Stream(level,packet,false);ACTIVE.put(packet,stream);mc.particleEngine.add(stream);
    }
    /** Original FXDispatcher.waterTrailFx, shared by the everfull urn's client block events. */
    public static void water(BlockPos from,BlockPos to){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(lastLevel!=mc.level){ACTIVE.clear();lastLevel=mc.level;}
        ACTIVE.values().removeIf(stream->!stream.isAlive());
        var packet=new EssentiaStreamPayload(from,to,2650102);var previous=ACTIVE.get(packet);
        if(previous!=null&&previous.extend())return;
        var stream=new Stream(mc.level,packet,true);ACTIVE.put(packet,stream);mc.particleEngine.add(stream);
    }
    public static void vent(BlockPos pos,int color){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var random=new java.util.Random(pos.asLong()*4);double pitch=random.nextFloat()*Math.PI*2,yaw=random.nextFloat()*Math.PI*2;
        var particle=new SmelterVentParticle(mc.level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,-Math.sin(yaw)*Math.cos(pitch)/5,-Math.sin(pitch)/5,Math.cos(yaw)*Math.cos(pitch)/5);
        particle.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);mc.particleEngine.add(particle);
    }
    private record Node(Vec3 point,double radius){}
    private static final class Stream extends Particle {
        private final Vec3 target;
        private final int count;
        private final ArrayList<Node> nodes=new ArrayList<>();
        private int length=20,growing=-1;
        private double scale;
        Stream(ClientLevel level,EssentiaStreamPayload packet,boolean water){
            super(level,packet.from().getX()+.5,packet.from().getY()+(water?.66:.5),packet.from().getZ()+.5);
            var state=level.getBlockState(packet.from());
            if(state.getBlock() instanceof EssentiaTransferBlock){var facing=state.getValue(EssentiaTransferBlock.FACING);x+=facing.getStepX()*.05;y+=facing.getStepY()*.05;z+=facing.getStepZ()*.05;}
            target=Vec3.atCenterOf(packet.to());count=(int)level.getGameTime();scale=.1*(1+random.nextGaussian()*.15);
            lifetime=Math.max(1,(int)(new Vec3(x,y,z).distanceTo(target)*21));
            xd=Math.sin(count/4f)*.015;yd=(water?.2:0)+Math.sin(count/3f)*.015;zd=Math.sin(count/2f)*.015;
            rCol=((packet.color()>>16)&255)/255f;gCol=((packet.color()>>8)&255)/255f;bCol=(packet.color()&255)/255f;
            hasPhysics=false;nodes.add(new Node(new Vec3(x,y,z),.001));nodes.add(new Node(new Vec3(x,y,z),.001));
            setBoundingBox(new AABB(new Vec3(x,y,z),target).inflate(2));
        }
        boolean extend(){if(!isAlive()||nodes.size()>=length)return false;length+=5;lifetime+=5;return true;}
        @Override public void tick(){
            xo=x;yo=y;zo=z;
            if(age++>=lifetime||length<1){remove();return;}
            yd+=.002;x+=xd;y+=yd;z+=zd;
            xd=Mth.clamp(xd*.985,-.05,.05);yd=Mth.clamp(yd*.985,-.05,.05);zd=Mth.clamp(zd*.985,-.05,.05);
            Vec3 delta=target.subtract(x,y,z);double distance=delta.length();
            if(distance>1.0E-8){var force=delta.scale(.01/(distance*Math.min(1,distance)));xd+=force.x;yd+=force.y;zd+=force.z;}
            double radius=scale*(.75+Math.sin((count+age)/2f)*.25);
            if(distance<1){double f=Math.sin(distance*Math.PI/2);radius*=f;scale*=f;}
            if(scale>.001)nodes.add(new Node(new Vec3(x,y,z),radius));
            else{if(growing<0)growing=age;length--;drop(target.add(random.nextGaussian()*.075,random.nextGaussian()*.075,random.nextGaussian()*.075));}
            if(nodes.size()>length&&!nodes.isEmpty())nodes.remove(0);
            if(nodes.size()>2&&random.nextBoolean()){int q=random.nextBoolean()?nodes.size()-2:random.nextInt(3);drop(nodes.get(q).point());}
        }
        private void drop(Vec3 point){Minecraft.getInstance().particleEngine.add(new Drop(level,point,rCol,gCol,bCol));}
        @Override public void render(VertexConsumer out,Camera camera,float partial){
            int size=nodes.size();if(size<3)return;
            Vec3[] points=new Vec3[size];double[] radii=new double[size];
            for(int i=0;i<size;i++){
                Node node=nodes.get(size-1-i);
                points[i]=node.point().add(Math.sin((i+age)/6f)*.03,Math.sin((i+age)/7f)*.03,Math.sin((i+age)/8f)*.03).subtract(camera.getPosition());
                double radius=node.radius()*(1+Math.sin((i+age)/3f)*.2);
                if(i>size-10)radius*=Math.cos((i-(size-12))/10f*Math.PI/2);
                radius=switch(i){case 0,1->0;case 2->(scale*.5+radius)/2;case 3->(scale+radius)/2;case 4->(scale+radius*2)/3;default->radius;};
                radii[i]=Math.max(0,radius);
            }
            Vec3[][] rings=new Vec3[size][8];
            for(int i=0;i<size;i++){
                Vec3 tangent=points[Math.min(size-1,i+1)].subtract(points[Math.max(0,i-1)]).normalize();
                Vec3 side=tangent.cross(Math.abs(tangent.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();Vec3 up=tangent.cross(side).normalize();
                for(int j=0;j<8;j++){double a=j*Math.PI/4;rings[i][j]=points[i].add(side.scale(Math.cos(a)*radii[i])).add(up.scale(Math.sin(a)*radii[i]));}
            }
            float offset=growing<0?0:.075f*(age-growing+partial);
            for(int i=0;i<size-1;i++)for(int j=0;j<8;j++){
                vertex(out,rings[i][j],j/8f,i*.075f+offset,i);
                vertex(out,rings[i+1][j],j/8f,(i+1)*.075f+offset,i+1);
                vertex(out,rings[i+1][(j+1)%8],(j+1)/8f,(i+1)*.075f+offset,i+1);
                vertex(out,rings[i][(j+1)%8],(j+1)/8f,i*.075f+offset,i);
            }
        }
        private void vertex(VertexConsumer out,Vec3 point,float u,float v,int index){float shade=1-(float)Math.sin((index+age)/2f)*.1f;out.addVertex((float)point.x,(float)point.y,(float)point.z).setUv(u,v).setColor(rCol*shade,gCol*shade,bCol*shade,1).setLight(0xf000f0);}
        @Override public ParticleRenderType getRenderType(){return LAYER;}
    }
    private static final class Drop extends TextureSheetParticle {
        private final float start;
        Drop(ClientLevel level,Vec3 point,float r,float g,float b){super(level,point.x,point.y,point.z);xd=random.nextGaussian()*.005;yd=random.nextGaussian()*.005;zd=random.nextGaussian()*.005;lifetime=20+random.nextInt(10);setColor(r,g,b);alpha=.5f;quadSize=start=.04f+random.nextFloat()*.02f;gravity=.01f;}
        @Override public void tick(){super.tick();quadSize=start+(.02f-start)*age/lifetime;}
        @Override protected float getU0(){return 25/64f;}@Override protected float getU1(){return 26/64f;}@Override protected float getV0(){return 0;}@Override protected float getV1(){return 1/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
    }
    private EssentiaStreams(){}
}
