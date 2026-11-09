package thaumcraft.port.champion.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.champion.ChampionSystem;
import thaumcraft.port.client.SalisMundusParticle;

/** Original champion particle cells, colours and taint-fibre overlay; no renderer replacement. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class ChampionVisuals {
    private static final Set<LivingEntityRenderer<?,?>> TAINT_LAYERS=Collections.newSetFromMap(new WeakHashMap<>());
    private static final ResourceLocation TAINT_TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/taint_fibres.png");
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var game=Minecraft.getInstance();if(game.level==null||game.isPaused())return;
        for(var entity:game.level.entitiesForRendering())if(entity instanceof PathfinderMob mob&&mob.isAlive()){
            int type=ChampionSystem.getModifier(mob);if(type<0)continue;
            var r=game.level.random;
            if(type==11?r.nextFloat()>.2f:type==3?r.nextFloat()>.3f:type==2?r.nextInt(4)!=0:type!=8&&type!=13&&r.nextBoolean())continue;
            double x=mob.getBoundingBox().minX+r.nextFloat()*mob.getBbWidth(),y=mob.getY()+r.nextFloat()*mob.getBbHeight()/(type==0?3:1),z=mob.getBoundingBox().minZ+r.nextFloat()*mob.getBbWidth();
            if(type==12){game.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL)),x,y,z,0,-.02,0);continue;}
            var particle=new ChampionParticle(game.level,x,y,z,type);game.particleEngine.add(particle);
        }
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"}) public static void render(RenderLivingEvent.Pre<?,?> event){
        if(ChampionSystem.getModifier(event.getEntity())!=13)return;
        var renderer=event.getRenderer();if(TAINT_LAYERS.add(renderer))((LivingEntityRenderer)renderer).addLayer(new TaintedLayer((RenderLayerParent)renderer));
    }
    private static final class TaintedLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M>{
        TaintedLayer(RenderLayerParent<T,M> renderer){super(renderer);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch){
            if(entity.isInvisible()||ChampionSystem.getModifier(entity)!=13)return;
            pose.pushPose();pose.scale(1.002f,1.002f,1.002f);
            var target=buffers.getBuffer(RenderType.entityTranslucent(TAINT_TEXTURE));float dx=(float)Math.cos(entity.getId()*.00025f),dy=entity.getId()*.001f;
            VertexConsumer wrapped=new VertexConsumer(){
                public VertexConsumer addVertex(float x,float y,float z){target.addVertex(x,y,z);return this;}
                public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,a);return this;}
                public VertexConsumer setUv(float u,float v){target.setUv(u*8+dx,v*4+dy);return this;}
                public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
                public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
                public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
            };
            getParentModel().renderToBuffer(pose,wrapped,light,OverlayTexture.NO_OVERLAY,0xa8ffffff);pose.popPose();
        }
    }
    private static final class ChampionParticle extends TextureSheetParticle{
        private final int first,frames,frameSpeed;
        private final boolean additive;
        private final float opacity,size,rotation;
        ChampionParticle(ClientLevel level,double x,double y,double z,int type){
            super(level,x,y,z);hasPhysics=false;int start=69,count=4,speed=1;boolean glow=false;float red=1,green=1,blue=1,a=.7f,scale=.7f,rot=0;int life=6;
            switch(type){
                case 0->{start=320;count=16;glow=true;red=.3f-random.nextFloat()*.1f;green=0;blue=.8f+random.nextFloat()*.2f;scale=.2f;life=8;}
                case 1->{start=704+random.nextInt(4)*3;count=3;red=.5f+random.nextFloat()*.2f;green=.1f+random.nextFloat()*.2f;blue=.1f+random.nextFloat()*.2f;scale=1.2f+random.nextFloat()*.3f;life=3;}
                case 2->{start=448;count=9;red=green=.9f;blue=.9f+random.nextFloat()*.1f;life=5+random.nextInt(4);scale=.6f+random.nextFloat()*.2f;}
                case 3->{start=704+random.nextInt(4)*3;count=3;red=.8f+random.nextFloat()*.2f;green=.8f+random.nextFloat()*.2f;blue=.8f+random.nextFloat()*.2f;life=4+random.nextInt(3);scale=1+random.nextFloat()*.3f;}
                case 4->{start=640;count=10;red=random.nextFloat()*.2f;green=random.nextFloat()*.2f;blue=random.nextFloat()*.2f;yd=-.02;a=.8f;life=8+random.nextInt(4);scale=.6f+random.nextFloat()*.4f;}
                case 5->{glow=true;red=.5f+random.nextFloat()*.1f;green=.5f+random.nextFloat()*.1f;blue=.5f+random.nextFloat()*.1f;a=.6f;life=4+random.nextInt(4);scale=.8f+random.nextFloat()*.3f;}
                case 6->{start=264;count=8;glow=true;red=.8f+random.nextFloat()*.2f;green=0;blue=.9f+random.nextFloat()*.1f;life=10+random.nextInt(4);scale=.6f+random.nextFloat()*.4f;}
                case 7->{glow=true;red=.1f+random.nextFloat()*.1f;green=.8f+random.nextFloat()*.2f;blue=.1f+random.nextFloat()*.1f;yd=.03;a=.9f;life=4+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;}
                case 8->{start=640;count=10;red=.9f+random.nextFloat()*.1f;yd=.03;life=8+random.nextInt(4);scale=.7f+random.nextFloat()*.2f;}
                case 9->{start=1;count=4;speed=2;red=.2f;green=.6f+random.nextFloat()*.1f;blue=.2f+random.nextFloat()*.1f;yd=-.02;a=.5f;life=5+random.nextInt(4);scale=.9f+random.nextFloat()*.3f;}
                case 10->{start=579;count=4;red=.2f;green=.6f+random.nextFloat()*.1f;blue=.2f+random.nextFloat()*.1f;yd=.02;life=8+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;rot=.5f;}
                case 11->{start=579;count=4;red=.9f+random.nextFloat()*.1f;green=blue=0;a=.9f;life=4+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;}
                case 13->{start=1;count=5;red=.1f+random.nextFloat()*.2f;green=0;blue=.1f+random.nextFloat()*.1f;yd=-.01;a=.25f;life=6+random.nextInt(6);scale=2+random.nextFloat();rot=.5f;gravity=.1f;}
                default->{}
            }
            first=start;frames=count;frameSpeed=speed;additive=glow;setColor(red,green,blue);alpha=opacity=a;quadSize=size=scale*.1f;lifetime=life;rotation=rot;
        }
        private int cell(){return first+(age/frameSpeed)%frames;}
        @Override protected float getU0(){return cell()%64/64f;}
        @Override protected float getU1(){return getU0()+1/64f;}
        @Override protected float getV0(){return cell()/64/64f;}
        @Override protected float getV1(){return getV0()+1/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
        @Override public void tick(){super.tick();oRoll=roll;roll+=rotation;alpha=opacity*(1-age/(float)lifetime);quadSize=size;}
    }
    private ChampionVisuals(){}
}
