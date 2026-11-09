package thaumcraft.port.champion.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
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
            if(type==12){game.particleEngine.add(new InfestedParticle(game.level,mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ()));continue;}
            var particle=new ChampionParticle(game.level,x,y,z,type);game.particleEngine.add(particle);
        }
    }
    @SubscribeEvent @SuppressWarnings({"rawtypes","unchecked"}) public static void render(RenderLivingEvent.Pre<?,?> event){
        if(ChampionSystem.getModifier(event.getEntity())<0)return;
        var renderer=event.getRenderer();if(TAINT_LAYERS.add(renderer)){
            ((LivingEntityRenderer)renderer).addLayer(new TaintedLayer((RenderLayerParent)renderer));
            ((LivingEntityRenderer)renderer).addLayer(new TintLayer((RenderLayerParent)renderer));
        }
    }
    private static final class TintLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M>{
        TintLayer(RenderLayerParent<T,M> renderer){super(renderer);}
        @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,T entity,float walk,float amount,float partial,float age,float yaw,float pitch){
            if(entity.isInvisible())return;
            // Fixed-function GL colour in BETA26 multiplied the existing model colour.
            // A depth-tested multiplicative layer avoids tint leaking into a later batched entity.
            int colour=switch(ChampionSystem.getModifier(entity)){case 4->0xff999999;case 8->0xffffbfff;case 10->0xff99ffb3;case 11->0xffffb3b3;default->-1;};
            if(colour!=-1)getParentModel().renderToBuffer(pose,buffers.getBuffer(TintType.LAYER),light,OverlayTexture.NO_OVERLAY,colour);
        }
    }
    private static final class TintType extends RenderType{
        private TintType(){super("champion_tint",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
        private static final RenderType LAYER=create("thaumcraft_champion_tint",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(new TransparencyStateShard("champion_multiply",()->{
                RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR,GlStateManager.DestFactor.ZERO,GlStateManager.SourceFactor.ZERO,GlStateManager.DestFactor.ONE);
            },()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();})).setWriteMaskState(COLOR_WRITE).setDepthTestState(EQUAL_DEPTH_TEST).setCullState(NO_CULL).createCompositeState(false));
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
        private final int first,frames,frameSpeed,grid;
        private final boolean additive,loop,flipped;
        private final float opacity,size,rotation;
        ChampionParticle(ClientLevel level,double x,double y,double z,int type){
            super(level,x,y,z);hasPhysics=true;friction=.98f;int start=69,count=4,speed=1,gridSize=64;boolean looping=false,flip=false;float red=1,green=1,blue=1,a=.7f,scale=.7f,rot=0;int life=6;
            switch(type){
                case 0->{gridSize=16;start=8+random.nextInt(3)*16;count=8;flip=random.nextBoolean();red=.3f-random.nextFloat()*.1f;green=0;blue=.8f+random.nextFloat()*.2f;scale=.2f;life=5+random.nextInt(5);a=1;}
                case 1->{start=704+random.nextInt(4)*3;count=3;red=.5f+random.nextFloat()*.2f;green=.1f+random.nextFloat()*.2f;blue=.1f+random.nextFloat()*.2f;scale=1.2f+random.nextFloat()*.3f;life=3;}
                case 2->{start=448;count=9;red=green=.9f;blue=.9f+random.nextFloat()*.1f;life=5+random.nextInt(4);scale=.6f+random.nextFloat()*.2f;}
                case 3->{start=704+random.nextInt(4)*3;count=3;red=.8f+random.nextFloat()*.2f;green=.8f+random.nextFloat()*.2f;blue=.8f+random.nextFloat()*.2f;life=4+random.nextInt(3);scale=1+random.nextFloat()*.3f;}
                case 4->{start=640;count=10;red=random.nextFloat()*.2f;green=random.nextFloat()*.2f;blue=random.nextFloat()*.2f;yd=-.02;a=.8f;life=8+random.nextInt(4);scale=.6f+random.nextFloat()*.4f;}
                case 5->{looping=true;red=.5f+random.nextFloat()*.1f;green=.5f+random.nextFloat()*.1f;blue=.5f+random.nextFloat()*.1f;a=.6f;life=4+random.nextInt(4);scale=.8f+random.nextFloat()*.3f;}
                case 6->{start=264;count=8;looping=true;red=.8f+random.nextFloat()*.2f;green=0;blue=.9f+random.nextFloat()*.1f;life=10+random.nextInt(4);scale=.6f+random.nextFloat()*.4f;}
                case 7->{looping=true;red=.1f+random.nextFloat()*.1f;green=.8f+random.nextFloat()*.2f;blue=.1f+random.nextFloat()*.1f;yd=.03;a=.9f;life=4+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;}
                case 8->{start=640;count=10;red=.9f+random.nextFloat()*.1f;yd=.03;life=8+random.nextInt(4);scale=.7f+random.nextFloat()*.2f;}
                case 9->{start=1;count=4;speed=2;red=.2f;green=.6f+random.nextFloat()*.1f;blue=.2f+random.nextFloat()*.1f;yd=-.02;a=.5f;life=5+random.nextInt(4);scale=.9f+random.nextFloat()*.3f;}
                case 10->{start=579;count=4;red=.2f;green=.6f+random.nextFloat()*.1f;blue=.2f+random.nextFloat()*.1f;yd=.02;life=8+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;rot=.5f;}
                case 11->{start=579;count=4;red=.9f+random.nextFloat()*.1f;green=blue=0;a=.9f;life=4+random.nextInt(4);scale=.5f+random.nextFloat()*.2f;}
                case 13->{start=1;count=5;red=.1f+random.nextFloat()*.2f;green=0;blue=.1f+random.nextFloat()*.1f;yd=-.01;a=.25f;life=6+random.nextInt(6);scale=2+random.nextFloat();rot=.5f;}
                default->{}
            }
            first=start;frames=count;frameSpeed=speed;grid=gridSize;loop=looping;flipped=flip;additive=type!=13;setColor(red,green,blue);alpha=opacity=a;quadSize=size=scale*.1f;lifetime=life;rotation=(float)(rot*Math.PI/180*Math.PI*2);
        }
        private int cell(){return first+(loop?(age/frameSpeed)%frames:Math.min(frames-1,frames*age/lifetime));}
        @Override protected float getU0(){return (cell()%grid+(flipped?1:0))/(float)grid;}
        @Override protected float getU1(){return (cell()%grid+(flipped?0:1))/(float)grid;}
        @Override protected float getV0(){return cell()/grid/(float)grid;}
        @Override protected float getV1(){return getV0()+1f/grid;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(additive);}
        @Override public void tick(){super.tick();oRoll=roll;roll+=rotation;alpha=opacity;quadSize=size;}
    }
    private static final ParticleRenderType SLIME_LAYER=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(SalisMundusParticle::legacyShader);RenderSystem.setShaderTexture(0,TextureAtlas.LOCATION_BLOCKS);return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}
        @Override public String toString(){return "THAUMCRAFT_CHAMPION_INFESTED";}
    };
    private static final class InfestedParticle extends TextureSheetParticle{
        private final float u,v;
        InfestedParticle(ClientLevel level,double x,double y,double z){
            super(level,x,y,z,0,0,0);setSprite(Minecraft.getInstance().getItemRenderer().getModel(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL),level,null,0).getParticleIcon());
            u=random.nextFloat()*3;v=random.nextFloat()*3;setColor(.7f,0,1);alpha=.4f;gravity=1;quadSize*=.5f;lifetime=(int)(66/(random.nextFloat()*.9f+.1f));
        }
        @Override protected float getU0(){return sprite.getU(u/4);}
        @Override protected float getU1(){return sprite.getU((u+1)/4);}
        @Override protected float getV0(){return sprite.getV(v/4);}
        @Override protected float getV1(){return sprite.getV((v+1)/4);}
        @Override public ParticleRenderType getRenderType(){return SLIME_LAYER;}
        @Override public void tick(){super.tick();alpha=.4f*(1-age/(float)lifetime);}
    }
    private ChampionVisuals(){}
}
