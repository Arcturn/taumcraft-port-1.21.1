package thaumcraft.port.artifice.device.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.artifice.device.*;
import thaumcraft.port.client.SalisMundusParticle;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ArtificeDeviceClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{ArtificeDevices.clientTick=ArtificeDeviceClient::tick;ArtificeDevices.clientAnimate=ArtificeDeviceClient::animate;});}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ArtificeDevices.ENTITY.get(),ArtificeDeviceRenderer::new);}
    private static void tick(ArtificeDeviceEntity be){
        if(!(be.getLevel() instanceof ClientLevel level))return;var random=level.random;var pos=be.getBlockPos();var face=be.facing();
        if(be.kind()==ArtificeDeviceBlock.Kind.PATTERN_CRAFTER&&be.rotationTicks>0&&(be.rotationTicks-1)%Math.floor(Math.max(1,be.rotationSpeed))==0){
            level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("thaumcraft","clack")),SoundSource.BLOCKS,.2f,1.7f,false);
        }
        if(be.kind()!=ArtificeDeviceBlock.Kind.LEVITATOR||!be.enabled()||be.vis<=0||be.rangeActual<=0)return;
        var engine=Minecraft.getInstance().particleEngine;
        var box=new AABB(pos).expandTowards(face.getStepX()*be.rangeActual,face.getStepY()*be.rangeActual,face.getStepZ()*be.rangeActual);
        int credit=be.vis;
        for(Entity e:level.getEntities((Entity)null,box,e->e instanceof ItemEntity||e.isPushable()||e instanceof AbstractHorse)){
            if(random.nextFloat()<.1f)engine.add(Dust.lift(level,e.getX()+(random.nextFloat()-random.nextFloat())*e.getBbWidth(),e.getY()+random.nextFloat()*e.getBbHeight(),e.getZ()+(random.nextFloat()-random.nextFloat())*e.getBbWidth(),(random.nextFloat()-random.nextFloat())*.01,(random.nextFloat()-random.nextFloat())*.01,(random.nextFloat()-random.nextFloat())*.01));
            if(random.nextFloat()<.6f)engine.add(Dust.lift(level,pos.getX()+.25+random.nextFloat()*.5,pos.getY()+.25+random.nextFloat()*.5,pos.getZ()+.25+random.nextFloat()*.5,face.getStepX()/50.0,face.getStepY()/50.0,face.getStepZ()/50.0));
            credit-=be.liftCost();if(credit<=0)break;
        }
        if(random.nextFloat()<.1f)engine.add(Dust.lift(level,pos.getX()+.25+random.nextFloat()*.5,pos.getY()+.25+random.nextFloat()*.5,pos.getZ()+.25+random.nextFloat()*.5,face.getStepX()/50.0,face.getStepY()/50.0,face.getStepZ()/50.0));
    }
    private static void animate(ArtificeDeviceEntity be){
        if(be.kind()!=ArtificeDeviceBlock.Kind.VIS_GENERATOR||be.energy<=0||!(be.getLevel() instanceof ClientLevel level))return;
        var random=level.random;var d=be.facing();var p=be.getBlockPos();
        double x=d.getStepX()==0?random.nextGaussian()*.1:d.getStepX()*.1,y=d.getStepY()==0?random.nextGaussian()*.1:d.getStepY()*.1,z=d.getStepZ()==0?random.nextGaussian()*.1:d.getStepZ()*.1;
        Minecraft.getInstance().particleEngine.add(Dust.spark(level,p.getX()+.5+x,p.getY()+.5+y,p.getZ()+.5+z));
    }
    /** Exact FXDispatcher drawLevitatorParticles/spark presets on the original 16×16 atlas. */
    private static final class Dust extends TextureSheetParticle {
        private final boolean spark;private final int first;private float step;private final boolean flipped;
        private Dust(ClientLevel level,double x,double y,double z,boolean spark){super(level,x,y,z);this.spark=spark;first=spark?8+random.nextInt(3)*16:56;flipped=spark&&random.nextBoolean();setSize(.1f,.1f);friction=1;}
        static Dust lift(ClientLevel level,double x,double y,double z,double vx,double vy,double vz){var p=new Dust(level,x,y,z,false);p.xd=vx;p.yd=vy;p.zd=vz;p.lifetime=200+level.random.nextInt(100);p.setColor(.5f,.5f,.2f);p.alpha=.3f;p.quadSize=.2f;p.roll=level.random.nextFloat()*(float)Math.PI*2;p.step=(float)(Math.PI*2*Math.toRadians(level.random.nextBoolean()?-1:1));return p;}
        static Dust spark(ClientLevel level,double x,double y,double z){var p=new Dust(level,x,y,z,true);p.lifetime=5+level.random.nextInt(5);p.alpha=.8f;p.setColor(.65f+level.random.nextFloat()*.1f,1,1);p.quadSize=(.66f+level.random.nextFloat())*.1f;p.xd=p.yd=p.zd=0;return p;}
        @Override public void tick(){
            xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}oRoll=roll;roll+=step;move(xd,yd,zd);
            if(spark){xd*=.98f;yd*=.98f;zd*=.98f;if(onGround){xd*=.7;zd*=.7;}}
            else{float progress=age/(float)lifetime;alpha=.3f*(1-progress);quadSize=.2f+.3f*progress;}
        }
        @Override public void move(double dx,double dy,double dz){
            var motion=new net.minecraft.world.phys.Vec3(dx,dy,dz);
            if(hasPhysics&&(dx!=0||dy!=0||dz!=0)){var obstacles=new java.util.ArrayList<net.minecraft.world.phys.shapes.VoxelShape>();level.getBlockCollisions(null,getBoundingBox().expandTowards(motion)).forEach(obstacles::add);motion=thaumcraft.port.client.FocusParticleMotion.collide(getBoundingBox(),motion,obstacles);}
            setBoundingBox(getBoundingBox().move(motion));setLocationFromBoundingbox();onGround=dy!=motion.y&&dy<0;
            if(dx!=motion.x)xd=0;if(dz!=motion.z)zd=0;
        }
        @Override public void render(com.mojang.blaze3d.vertex.VertexConsumer out,net.minecraft.client.Camera camera,float partial){
            var view=camera.getPosition();float x=(float)(net.minecraft.util.Mth.lerp(partial,xo,this.x)-view.x),y=(float)(net.minecraft.util.Mth.lerp(partial,yo,this.y)-view.y),z=(float)(net.minecraft.util.Mth.lerp(partial,zo,this.z)-view.z);
            var rotation=new org.joml.Quaternionf(camera.rotation()).rotateZ(roll+(roll-oRoll)*partial);
            SalisMundusParticle.renderQuad(out,rotation,x,y,z,quadSize,getU0(),getU1(),getV0(),getV1(),rCol,gCol,bCol,alpha,getLightColor(partial));
        }
        private int frame(){return first+(spark?Math.min(7,(int)(8*age/(float)lifetime)):0);}
        @Override protected float getU0(){return (frame()%16+(flipped?1:0))/16f;}
        @Override protected float getU1(){return (frame()%16+(flipped?0:1))/16f;}
        @Override protected float getV0(){return (frame()/16)/16f;}
        @Override protected float getV1(){return (frame()/16+1)/16f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(false);}
    }
}
