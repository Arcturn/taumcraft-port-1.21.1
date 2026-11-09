package thaumcraft.port.mob.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import thaumcraft.port.mob.*;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class MobRenderers {
    public static final ModelLayerLocation CRAB=layer("eldritch_crab"),TENTACLE=layer("taintacle"),SMALL=layer("taintacle_small");
    private static ModelLayerLocation layer(String name){return new ModelLayerLocation(id(name),"main");}
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("thaumcraft",name);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(CRAB,EldritchCrabModel::layer);event.registerLayerDefinition(TENTACLE,()->TaintacleModel.layer(10));event.registerLayerDefinition(SMALL,()->TaintacleModel.layer(6));}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private KnightArmorModel outer,inner;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){if(outer==null){outer=new KnightArmorModel(false);inner=new KnightArmorModel(true);}return slot==EquipmentSlot.LEGS?inner:outer;}
            @Override public void setupModelAnimations(LivingEntity entity,ItemStack stack,EquipmentSlot slot,Model model,float swing,float amount,float partial,float age,float yaw,float pitch){if(model instanceof KnightArmorModel armor)armor.animateCloth(swing,amount);}
        },MobArmor.HELM,MobArmor.CHEST,MobArmor.LEGS);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(MobContent.BRAINY_ZOMBIE.get(),c->new BrainyRenderer<BrainyZombie>(c));
        event.registerEntityRenderer(MobContent.GIANT_BRAINY_ZOMBIE.get(),c->new BrainyRenderer<GiantBrainyZombie>(c));
        event.registerEntityRenderer(MobContent.ELDRITCH_CRAB.get(),CrabRenderer::new);
        event.registerEntityRenderer(MobContent.INHABITED_ZOMBIE.get(),InhabitedRenderer::new);
        event.registerEntityRenderer(MobContent.CRAWLER.get(),CrawlerRenderer::new);
        event.registerEntityRenderer(MobContent.TAINTACLE.get(),c->new TentacleRenderer(c,false));
        event.registerEntityRenderer(MobContent.TAINTACLE_SMALL.get(),c->new TentacleRenderer(c,true));
        event.registerEntityRenderer(MobContent.SWARM.get(),c->new EntityRenderer<TaintSwarm>(c){@Override public ResourceLocation getTextureLocation(TaintSwarm e){return id("textures/misc/particles.png");}});
    }
    private static final class BrainyModel<T extends Zombie> extends ZombieModel<T>{
        private int tint=-1;
        BrainyModel(ModelPart root){super(root);}
        @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch){super.setupAnim(entity,walk,amount,age,yaw,pitch);float q=entity instanceof GiantBrainyZombie giant?Math.min(1,giant.getAnger())/2:0;int dim=(int)(255*(1-q));tint=0xff0000|(dim<<8)|dim;}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){super.renderToBuffer(pose,out,light,overlay,(color&0xff000000)|tint);}
    }
    private static final class BrainyRenderer<T extends BrainyZombie> extends HumanoidMobRenderer<T,BrainyModel<T>>{
        BrainyRenderer(EntityRendererProvider.Context c){super(c,new BrainyModel<T>(c.bakeLayer(ModelLayers.ZOMBIE)),.5f);addLayer(new HumanoidArmorLayer<>(this,new ZombieModel<T>(c.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new ZombieModel<T>(c.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),c.getModelManager()));}
        @Override public ResourceLocation getTextureLocation(T e){return id("textures/entity/bzombie.png");}
        @Override protected void scale(T e,PoseStack pose,float partial){if(e instanceof GiantBrainyZombie giant){float scale=1+giant.getAnger();pose.scale(scale,scale,scale);}}
    }
    private static final class InhabitedRenderer extends HumanoidMobRenderer<InhabitedZombie,ZombieModel<InhabitedZombie>>{
        InhabitedRenderer(EntityRendererProvider.Context c){super(c,new ZombieModel<>(c.bakeLayer(ModelLayers.ZOMBIE)),.5f);addLayer(new HumanoidArmorLayer<>(this,new ZombieModel<>(c.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new ZombieModel<>(c.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),c.getModelManager()));}
        @Override public ResourceLocation getTextureLocation(InhabitedZombie e){return id("textures/entity/czombie.png");}
    }
    private static final class CrabRenderer extends MobRenderer<EldritchCrab,EldritchCrabModel>{
        CrabRenderer(EntityRendererProvider.Context c){super(c,new EldritchCrabModel(c.bakeLayer(CRAB)),.5f);}
        @Override public ResourceLocation getTextureLocation(EldritchCrab e){return id("textures/entity/crab.png");}
        @Override protected void scale(EldritchCrab e,PoseStack pose,float partial){pose.scale(.8f,.8f,.8f);}
    }
    private static final class CrawlerRenderer extends MobRenderer<TaintCrawler,SilverfishModel<TaintCrawler>>{
        CrawlerRenderer(EntityRendererProvider.Context c){super(c,new SilverfishModel<>(c.bakeLayer(ModelLayers.SILVERFISH)),.2f);}
        @Override public ResourceLocation getTextureLocation(TaintCrawler e){return id("textures/entity/crawler.png");}
        @Override protected void scale(TaintCrawler e,PoseStack pose,float partial){pose.scale(.7f,.7f,.7f);}
        @Override protected float getFlipDegrees(TaintCrawler e){return 180;}
    }
    private static final class TentacleRenderer extends MobRenderer<Taintacle,TaintacleModel>{
        TentacleRenderer(EntityRendererProvider.Context c,boolean small){super(c,new TaintacleModel(c.bakeLayer(small?SMALL:TENTACLE),small?6:10),small?.2f:.6f);}
        @Override public ResourceLocation getTextureLocation(Taintacle e){return id("textures/entity/taintacle.png");}
        @Override protected RenderType getRenderType(Taintacle e,boolean visible,boolean translucent,boolean outline){return outline?RenderType.outline(getTextureLocation(e)):visible||translucent?RenderType.entityTranslucent(getTextureLocation(e)):null;}
    }
    private MobRenderers(){}
}
