package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.essentia.SmelterEntity;
import thaumcraft.port.essentia.SmelterVisualBridge;

/** TileSmelter event 1: the successful vent hisses and emits four outward puffs. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SmelterFx {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->SmelterVisualBridge.vent=SmelterFx::vent);
    }
    private static boolean vent(SmelterEntity smelter,Direction towardSmelter){
        if(!(smelter.getLevel() instanceof ClientLevel level))return false;
        var outward=towardSmelter.getOpposite();
        var pos=smelter.getBlockPos().relative(outward);
        var random=level.random;
        level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS,.25f,2.6f+(random.nextFloat()-random.nextFloat())*.8f,false);
        for(int i=0;i<4;i++)Minecraft.getInstance().particleEngine.add(new SmelterVentParticle(level,
                pos.getX()+.5+(random.nextFloat()-random.nextFloat())*.1,
                pos.getY()+.5+(random.nextFloat()-random.nextFloat())*.1,
                pos.getZ()+.5+(random.nextFloat()-random.nextFloat())*.1,
                outward.getStepX()*.25+(random.nextFloat()-random.nextFloat())*.1,
                outward.getStepY()*.25+(random.nextFloat()-random.nextFloat())*.1,
                outward.getStepZ()*.25+(random.nextFloat()-random.nextFloat())*.1));
        return true;
    }
    private SmelterFx(){}
}
