package thaumcraft.port.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.boss.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import thaumcraft.port.content.PortSounds;
import net.minecraft.sounds.SoundSource;

/** Original boss smoke, summoning spirals, charging arcs and sonic ripple. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class BossParticles {
    private record Sonic(EldritchWarden entity,int start,int lifetime,float yaw,float pitch) {}
    private static final List<Sonic> SONICS=new ArrayList<>();
    private static List<float[]> hemisphere;
    static void wisp(EldritchWarden entity){
        if(!(entity.level() instanceof ClientLevel level))return;
        var random=entity.getRandom();
        double x=entity.getX()+(random.nextFloat()-random.nextFloat())*.2;
        double z=entity.getZ()+(random.nextFloat()-random.nextFloat())*.2;
        for(int i=0;i<2;i++)Minecraft.getInstance().particleEngine.add(new Smoke(level,entity,x,entity.getY()+.25*entity.getBbHeight(),z));
        if(entity.getSpawnTimer()>0){float height=Math.max(1,entity.getBbHeight()*(150-entity.getSpawnTimer())/150f);for(int i=0;i<33;i++)Minecraft.getInstance().particleEngine.add(new Spiral(level,entity.getX(),entity.getY()+height/2,entity.getZ(),height,(int)entity.getY()-1));}
    }
    static void sonic(EldritchWarden entity){SONICS.add(new Sonic(entity,entity.tickCount,10+entity.getRandom().nextInt(5),entity.getYHeadRot(),entity.getXRot()));}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();
        SONICS.removeIf(s->s.entity.level()!=mc.level||s.entity.isRemoved()||s.entity.tickCount-s.start>=s.lifetime);
        renderArcs(event);
        if(SONICS.isEmpty())return;
        if(hemisphere==null)hemisphere=loadHemisphere();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers=mc.renderBuffers().bufferSource();var camera=event.getCamera().getPosition();
        for(var sonic:SONICS){
            var e=sonic.entity;float fade=(e.tickCount-sonic.start+partial)/sonic.lifetime;
            int frame=Math.min(15,(int)(14*fade)+1);
            var layer=RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/ripple"+frame+".png"));
            var out=buffers.getBuffer(layer);var pose=new PoseStack();
            pose.mulPose(event.getPoseStack().last().pose());
            pose.translate(Mth.lerp(partial,e.xo,e.getX())-camera.x,Mth.lerp(partial,e.yo,e.getY())+e.getEyeHeight()-camera.y,Mth.lerp(partial,e.zo,e.getZ())-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-sonic.yaw));pose.mulPose(Axis.XP.rotationDegrees(sonic.pitch));
            pose.translate(0,0,2*e.getBbHeight()+e.getBbWidth()/2);
            pose.scale(.25f*e.getBbHeight(),.25f*e.getBbHeight(),-e.getBbHeight());
            for(var v:hemisphere)out.addVertex(pose.last().pose(),v[0],v[1],v[2]).setColor(127,127,127,255).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(pose.last(),v[5],v[6],v[7]);
            buffers.endBatch(layer);
        }
    }
    private static List<float[]> loadHemisphere(){
        var vertices=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var normals=new ArrayList<float[]>();var out=new ArrayList<float[]>();
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/obj/hemis.obj")).openAsReader()){
            for(String line:reader.lines().toList()){
                var p=line.trim().split("\\s+");
                if(p[0].equals("v")||p[0].equals("vt")||p[0].equals("vn")){
                    float[] data=new float[p.length-1];for(int i=1;i<p.length;i++)data[i-1]=Float.parseFloat(p[i]);
                    (p[0].equals("v")?vertices:p[0].equals("vt")?uv:normals).add(data);
                }else if(p[0].equals("f")){
                    // Triangulate polygons, repeating the final vertex for the entity QUADS format.
                    for(int i=2;i<p.length-1;i++)for(int index:new int[]{1,i,i+1,i+1}){
                        var ids=p[index].split("/");var v=vertices.get(Integer.parseInt(ids[0])-1);var t=uv.get(Integer.parseInt(ids[1])-1);var n=normals.get(Integer.parseInt(ids[2])-1);
                        out.add(new float[]{v[0],v[1],v[2],t[0],1-t[1],n[0],n[1],n[2]});
                    }
                }
            }
        }catch(IOException|RuntimeException error){com.mojang.logging.LogUtils.getLogger().warn("Cannot load guardian sonic hemisphere",error);}
        return List.copyOf(out);
    }
    private static final class Smoke extends TextureSheetParticle {
        private final EldritchWarden target;
        Smoke(ClientLevel level,EldritchWarden target,double x,double y,double z){
            super(level,x,y,z);this.target=target;xd=random.nextGaussian()*.03;yd=-.05;zd=random.nextGaussian()*.03;
            alpha=.2f;quadSize=.4f*(random.nextFloat()*.5f+.5f);lifetime=(int)(40/(random.nextDouble()*.3+.7));
            friction=.98f;hasPhysics=true;setSize(.01f,.01f);setColor(random.nextFloat()*.05f,random.nextFloat()*.05f,random.nextFloat()*.05f);
        }
        @Override public void tick(){super.tick();if(!removed){x+=target.getDeltaMovement().x;z+=target.getDeltaMovement().z;}var camera=Minecraft.getInstance().getCameraEntity();double distance=camera==null?0:camera.distanceToSqr(x,y,z);alpha=.2f*(1-age/(float)lifetime)*(float)(1-Math.min(1024,distance)/1024);}
        @Override protected float getU0(){return (age%13)/64f;}@Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return 3/64f;}@Override protected float getV1(){return 4/64f;}
        @Override protected int getLightColor(float partial){return 240;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
    }
    private record Arc(EldritchGolem entity,Vec3 end,int start,int lifetime){}
    private static final java.util.Map<EldritchGolem,Arc> ARCS=new java.util.WeakHashMap<>();
    static void charge(EldritchGolem e){
        var previous=ARCS.get(e);if(previous!=null&&e.tickCount-previous.start<previous.lifetime)return;
        var random=e.getRandom();double radius=2+random.nextFloat()*2,angle=random.nextFloat()*Math.PI*2;
        BlockPos pos=BlockPos.containing(e.getX()+radius*Math.cos(angle),e.getY(),e.getZ()+radius*Math.sin(angle));
        for(int i=0;i<5&&e.level().isEmptyBlock(pos);i++)pos=pos.below();
        if(!e.level().isEmptyBlock(pos)&&e.level().isEmptyBlock(pos.above())){
            ARCS.put(e,new Arc(e,Vec3.atBottomCenterOf(pos.above()),e.tickCount,8+random.nextInt(5)));
            e.level().playLocalSound(e.getX(),e.getY(),e.getZ(),PortSounds.get("jacobs"),SoundSource.HOSTILE,.8f,1+(random.nextFloat()-random.nextFloat())*.05f,false);
        }
    }
    private static void renderArcs(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();ARCS.values().removeIf(a->a.entity.isRemoved()||a.entity.level()!=mc.level||a.entity.tickCount-a.start>=a.lifetime);
        if(ARCS.isEmpty())return;var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(RenderType.lightning());
        for(var arc:ARCS.values()){
            var e=arc.entity;var random=new java.util.Random(e.getId()*31L+e.tickCount);Vec3 start=e.position().add(0,e.getBbHeight()/2,0),delta=arc.end.subtract(start),last=start.subtract(camera);
            var pose=event.getPoseStack().last().pose();float alpha=Mth.clamp(1-(arc.lifetime-e.tickCount+arc.start)/10f,.05f,1);
            for(int n=1;n<=12;n++){
                float progress=n/12f;Vec3 end=start.add(delta.scale(progress));if(n<12)end=end.add((random.nextDouble()-.5)*.4,(random.nextDouble()-.5)*.4,(random.nextDouble()-.5)*.4);end=end.subtract(camera);
                for(int ribbon=0;ribbon<2;ribbon++){Vec3 offset=ribbon==0?new Vec3(.025,0,0):new Vec3(0,.025,.025);for(Vec3 point:new Vec3[]{last.subtract(offset),last.add(offset),end.add(offset),end.subtract(offset)})out.addVertex(pose,(float)point.x,(float)point.y,(float)point.z).setColor(.7f,1,1,alpha);}
                last=end;
            }
        }
        buffers.endBatch(RenderType.lightning());
    }
    static void golemTick(EldritchGolem e){
        if(!e.isHeadless()||!(e.level() instanceof ClientLevel level))return;var random=e.getRandom();var direction=Vec3.directionFromRotation(0,e.yBodyRot);
        Minecraft.getInstance().particleEngine.add(new Vent(level,e.getX()+direction.x*.4,e.getEyeY()-1.25,e.getZ()+direction.z*.4));
        if(random.nextInt(20)==0)Minecraft.getInstance().particleEngine.add(new Sprite(level,e.getX()+direction.x+(random.nextFloat()-random.nextFloat())/3,e.getEyeY()-.75,e.getZ()+direction.z+(random.nextFloat()-random.nextFloat())/3,8+random.nextInt(3)*16,8,5+random.nextInt(5),.3f,.7f,1,1,.8f));
    }
    static void orbTrail(GolemOrb e){} // BETA26 renders a pulsing orb without a trail.
    static void impact(GolemOrb e){if(e.level() instanceof ClientLevel level)Minecraft.getInstance().particleEngine.add(new Sprite(level,e.getX(),e.getY(),e.getZ(),208,31,31,.1f,1,1,1,1));}
    private static final class Sprite extends TextureSheetParticle {
        private final int first,frames;
        Sprite(ClientLevel level,double x,double y,double z,int first,int frames,int lifetime,float size,float r,float g,float b,float alpha){super(level,x,y,z);this.first=first;this.frames=frames;this.lifetime=lifetime;quadSize=size;this.alpha=alpha;setColor(r,g,b);hasPhysics=false;xd=yd=zd=0;}
        private int frame(){return first+Math.min(frames-1,age*frames/lifetime);}
        @Override protected float getU0(){return frame()%16/16f;}@Override protected float getU1(){return getU0()+1/16f;}
        @Override protected float getV0(){return frame()/16/16f;}@Override protected float getV1(){return getV0()+1/16f;}
        @Override protected int getLightColor(float partial){return 240;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    }
    private static final class Vent extends TextureSheetParticle {
        private float growth;
        Vent(ClientLevel level,double x,double y,double z){super(level,x,y,z);growth=(random.nextFloat()*.1f+.05f)*4;quadSize=growth*.3f;alpha=.4f;setColor(1/3f,1/3f,1/3f);xd=random.nextGaussian()*.0046875;yd=.125+random.nextGaussian()*.0046875;zd=random.nextGaussian()*.0046875;hasPhysics=true;setSize(.02f,.02f);}
        @Override public void tick(){xo=x;yo=y;zo=z;if(growth>=4){remove();return;}yd+=.0025;move(xd,yd,zd);xd*=.85;yd*=.85;zd*=.85;growth=Math.min(4,growth*1.15f);quadSize=growth*.3f;alpha=.4f*(4-growth)/4;}
        @Override protected float getU0(){return (int)(1+growth)/64f;}@Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return 0;}@Override protected float getV1(){return 1/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
    }
    private static final class Spiral extends TextureSheetParticle {
        private final float radius,start;private final int minY;
        Spiral(ClientLevel level,double x,double y,double z,float radius,int minY){super(level,x,y,z);this.radius=radius;this.minY=minY;start=random.nextInt(360);lifetime=20+random.nextInt(10);quadSize=.15f*(random.nextFloat()*.5f+.5f)*2;hasPhysics=false;setColor(34/255f,17/255f,47/255f);}
        @Override public void tick(){if(age++>=lifetime)remove();alpha=.66f*(lifetime-age)/lifetime;}
        @Override public void render(VertexConsumer out,Camera camera,float partial){
            float progress=(age+partial)/lifetime,a=(start+720*progress)*Mth.DEG_TO_RAD,b=(90-180*progress)*Mth.DEG_TO_RAD;var eye=camera.getPosition();
            double px=x-Mth.sin(a)*Mth.cos(b)*radius,py=Math.max(y-Mth.sin(b)*radius,minY+.1),pz=z+Mth.cos(a)*Mth.cos(b)*radius;
            SalisMundusParticle.renderQuad(out,camera.rotation(),(float)(px-eye.x),(float)(py-eye.y),(float)(pz-eye.z),quadSize,getU0(),getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,getLightColor(partial));
        }
        @Override protected float getU0(){return (int)(1+age/(float)lifetime*4)/64f;}@Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return 0;}@Override protected float getV1(){return 1/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
    }
    private BossParticles(){}
}
