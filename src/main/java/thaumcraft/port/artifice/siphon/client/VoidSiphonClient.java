package thaumcraft.port.artifice.siphon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.port.artifice.siphon.VoidSiphonContent;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class VoidSiphonClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(VoidSiphonContent.MENU.get(),VoidSiphonScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(VoidSiphonContent.ENTITY.get(),VoidSiphonRenderer::new);}
    @SubscribeEvent public static void shaders(RegisterShadersEvent event){VoidSiphonRenderer.registerShader(event);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->VoidSiphonContent.stream=(siphon,seed)->{
        if(!(siphon.getLevel() instanceof ClientLevel level))return;
        var pos=siphon.getBlockPos();var target=new Vec3(pos.getX()+.5,pos.getY()+.5625,pos.getZ()+.5);
        for(var rift:siphon.validRifts())VoidStreamParticle.spawn(Minecraft.getInstance().particleEngine,level,rift.position(),target,seed);
    });}
    private VoidSiphonClient(){}
}
