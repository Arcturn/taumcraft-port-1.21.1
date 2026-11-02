package thaumcraft.port.auromancy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.port.auromancy.AuromancyContent;
import thaumcraft.port.auromancy.focal.FocalManipulatorEntity;
import thaumcraft.port.auromancy.focal.FocalVisuals;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FocalManipulatorClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(AuromancyContent.FOCAL_MENU.get(),FocalManipulatorScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(AuromancyContent.FOCAL_ENTITY.get(),FocalManipulatorRenderer::new);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{FocalVisuals.tick=FocalManipulatorClient::tick;FocalVisuals.gather=FocalManipulatorClient::gather;});}
    private static void tick(FocalManipulatorEntity table) {
        if(table.vis<=0||!(table.getLevel() instanceof ClientLevel level))return;
        var p=table.getBlockPos();var r=level.random;
        Vec3 point=new Vec3(p.getX()+.5+(r.nextFloat()-r.nextFloat())*.3,p.getY()+1.4+(r.nextFloat()-r.nextFloat())*.3,p.getZ()+.5+(r.nextFloat()-r.nextFloat())*.3);
        Minecraft.getInstance().particleEngine.add(new FocalParticle.Craft(level,point,.5f+r.nextFloat()*.4f,1-r.nextFloat()*.4f,1-r.nextFloat()*.4f,6+r.nextInt(5),.3f+r.nextFloat()*.3f));
    }
    private static void gather(FocalManipulatorEntity table,int color) {
        if(!(table.getLevel() instanceof ClientLevel level))return;
        var p=table.getBlockPos();var r=level.random;
        int x=p.getX()+r.nextInt(3)-r.nextInt(3),y=p.getY()+r.nextInt(3),z=p.getZ()+r.nextInt(3)-r.nextInt(3);
        Vec3 point=new Vec3(x+r.nextFloat(),y+r.nextFloat(),z+r.nextFloat());
        Vec3 target=new Vec3(p.getX()+.4+r.nextFloat()*.2,p.getY()+1.4+r.nextFloat()*.2,p.getZ()+.4+r.nextFloat()*.2);
        Minecraft.getInstance().particleEngine.add(new FocalParticle.VisSparkle(level,point,target,color));
    }
    private FocalManipulatorClient() { }
}
