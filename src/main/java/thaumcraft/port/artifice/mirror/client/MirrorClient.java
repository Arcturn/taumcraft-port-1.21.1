package thaumcraft.port.artifice.mirror.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.artifice.mirror.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MirrorClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(MirrorContent.MENU.get(),HandMirrorScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(MirrorContent.ENTITY.get(),MirrorRenderer::new);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        for(var block:java.util.List.of(thaumcraft.port.infusion.content.InfusionCraftingContent.MIRROR,thaumcraft.port.infusion.content.InfusionCraftingContent.MIRROR_ESSENTIA))
            net.minecraft.client.renderer.item.ItemProperties.register(block.get().asItem(),net.minecraft.resources.ResourceLocation.parse("thaumcraft:linked"),(stack,level,entity,seed)->MirrorLink.get(stack)==null?0:1);
        MirrorEntity.effect=mirror->{
        if(!(mirror.getLevel() instanceof ClientLevel level))return;
        var pos=mirror.getBlockPos();var face=mirror.facing();var r=level.random;
        double x=pos.getX()+.33+r.nextFloat()*.33f-face.getStepX()/2.0,y=pos.getY()+.33+r.nextFloat()*.33f-face.getStepY()/2.0,z=pos.getZ()+.33+r.nextFloat()*.33f-face.getStepZ()/2.0;
        double vx=face.getStepX()/50.0+r.nextGaussian()*.01,vy=face.getStepY()/50.0+r.nextGaussian()*.01,vz=face.getStepZ()/50.0+r.nextGaussian()*.01;
        int age=10+r.nextInt(21);float red=r.nextFloat()/3,blue=r.nextFloat()/2,gravity=(float)(r.nextGaussian()*.01);
        Minecraft.getInstance().particleEngine.add(new MirrorMote(level,x,y,z,vx,vy,vz,age,red,blue,gravity));
    };});}
    private MirrorClient(){}
}
