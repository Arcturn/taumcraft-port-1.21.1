package thaumcraft.port.client.artifice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import thaumcraft.port.artifice.tool.*;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.content.CasterItem;
import thaumcraft.port.content.PortContent;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ArtificeToolsClient {
    private static final ResourceLocation HUD=ResourceLocation.parse("thaumcraft:textures/gui/hud.png");
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->ItemProperties.register(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:grapple_gun")),ResourceLocation.parse("thaumcraft:loaded"),(stack,level,entity,seed)->GrappleGunItem.loaded(stack)?1:0));}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ArtificeTools.GRAPPLE.get(),GrappleRenderer::new);}
    @SubscribeEvent public static void hud(RegisterGuiLayersEvent event){event.registerAboveAll(ResourceLocation.parse("thaumcraft:sanity_checker"),(gui,timer)->sanity(gui));}
    private static void sanity(GuiGraphics gui){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
        boolean main=mc.player.getMainHandItem().getItem() instanceof SanityCheckerItem;
        if(!main&&!(mc.player.getOffhandItem().getItem() instanceof SanityCheckerItem))return;
        // Keep the source stacking order when the checker occupies the second hand.
        int shift=!main&&mc.player.getMainHandItem().getItem() instanceof CasterItem?33:!main&&mc.player.getMainHandItem().is(PortContent.THAUMOMETER.get())?80:0;
        var state=ClientJournal.state;int permanent=state.permanentWarp(),normal=state.normalWarp(),temporary=state.temporaryWarp();
        var segments=segments(permanent,normal,temporary);int gap=segments[0],temp=segments[1],norm=segments[2],perm=segments[3];
        gui.blit(HUD,1,1+shift,152,0,20,76);
        segment(gui,shift,gap,temp,1,.5f,1);segment(gui,shift,gap+temp,norm,.75f,0,.75f);segment(gui,shift,gap+temp+norm,perm,.5f,0,.5f);
        gui.setColor(1,1,1,1);gui.blit(HUD,1,1+shift,176,0,20,76);
        if((long)permanent+normal+temporary>=100){gui.pose().pushPose();gui.pose().translate(0,shift,0);gui.pose().scale(.75f,.75f,1);int jitter=(mc.player.tickCount/2)%2;gui.blit(HUD,3+jitter,3+(mc.player.tickCount%2),216,0,20,16);gui.pose().popPose();}
    }
    static int[] segments(int permanent,int normal,int temporary){float total=(float)permanent+normal+temporary,mod=total>100?100/total:1;int gap=(int)((100-Math.min(100,total))/100*48),t=(int)(temporary/100f*48*mod),n=(int)(normal/100f*48*mod),p=permanent>0?48-gap-t-n:0;return new int[]{gap,t,n,p};}
    private static void segment(GuiGraphics gui,int shift,int offset,int size,float red,float green,float blue){if(size<=0)return;gui.setColor(red,green,blue,1);gui.blit(HUD,7,21+shift+offset,200,offset,8,size);}
    private ArtificeToolsClient(){}
}
