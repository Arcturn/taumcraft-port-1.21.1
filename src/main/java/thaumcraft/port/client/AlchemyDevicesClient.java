package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.port.alchemy.devices.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AlchemyDevicesClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(AlchemyDevices.SPRAYER_MENU.get(),PotionSprayerScreen::new);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{DeviceVisuals.urnTick=AlchemyDevicesClient::urn;DeviceVisuals.urnSplash=AlchemyDevicesClient::splash;DeviceVisuals.sprayerTick=AlchemyDevicesClient::sprayer;});}
    private static void urn(EverfullUrnEntity entity){
        if(!(entity.getLevel() instanceof ClientLevel level))return;
        var pos=entity.getBlockPos();
        if(entity.trailTicks>0){if(entity.trailTicks%5==0)EssentiaStreams.water(pos,pos.offset(EverfullUrnEntity.offset(entity.trailZone)));entity.trailTicks--;}
    }
    private static void splash(EverfullUrnEntity entity){if(entity.getLevel() instanceof ClientLevel level){var pos=entity.getBlockPos();Minecraft.getInstance().particleEngine.add(DeviceParticle.splash(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5));}}
    private static void sprayer(PotionSprayerEntity entity){
        if(!(entity.getLevel() instanceof ClientLevel level)||entity.venting<=0)return;
        --entity.venting;var random=level.random;var pos=entity.getBlockPos();var facing=entity.facing();
        for(int i=0;i<entity.venting/2;i++){
            double x=pos.getX()+.5+.1-random.nextFloat()*.2+facing.getStepX()*.5;
            double y=pos.getY()+.5+.1-random.nextFloat()*.2+facing.getStepY()*.5;
            double z=pos.getZ()+.5+.1-random.nextFloat()*.2+facing.getStepZ()*.5;
            double vx=random.nextGaussian()*.06+facing.getStepX()*.25,vy=random.nextGaussian()*.06+facing.getStepY()*.25,vz=random.nextGaussian()*.06+facing.getStepZ()*.25;
            var engine=Minecraft.getInstance().particleEngine;engine.add(DeviceParticle.vent(level,x,y,z,vx,vy,vz,entity.color));
            if(random.nextInt(6)<2)engine.add(DeviceParticle.spark(level,x,y,z,vx/2,vy/2,vz/2));
        }
    }
    private AlchemyDevicesClient(){}
}
