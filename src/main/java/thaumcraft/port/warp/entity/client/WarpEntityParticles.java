package thaumcraft.port.warp.entity.client;

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
import thaumcraft.port.warp.entity.EldritchGuardian;

/** Original guardian smoke and hemis.obj sonic ripple. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class WarpEntityParticles {
    private record Sonic(EldritchGuardian entity,int start,int lifetime,float yaw,float pitch) {}
    private static final List<Sonic> SONICS=new ArrayList<>();
    private static List<float[]> hemisphere;
    static void wisp(EldritchGuardian entity){
        if(!(entity.level() instanceof ClientLevel level))return;
        var random=entity.getRandom();
        double x=entity.getX()+(random.nextFloat()-random.nextFloat())*.2;
        double z=entity.getZ()+(random.nextFloat()-random.nextFloat())*.2;
        for(int i=0;i<2;i++)Minecraft.getInstance().particleEngine.add(new Smoke(level,entity,x,entity.getY()+.22*entity.getBbHeight(),z));
    }
    static void sonic(EldritchGuardian entity){SONICS.add(new Sonic(entity,entity.tickCount,10+entity.getRandom().nextInt(5),entity.getYHeadRot(),entity.getXRot()));}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();
        SONICS.removeIf(s->s.entity.level()!=mc.level||s.entity.isRemoved()||s.entity.tickCount-s.start>=s.lifetime);
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
        private final EldritchGuardian target;
        Smoke(ClientLevel level,EldritchGuardian target,double x,double y,double z){
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
    private WarpEntityParticles(){}
}
