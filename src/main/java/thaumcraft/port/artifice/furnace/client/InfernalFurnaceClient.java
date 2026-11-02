package thaumcraft.port.artifice.furnace.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.artifice.furnace.InfernalFurnaceVisuals;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfernalFurnaceClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->InfernalFurnaceVisuals.completion=furnace->{
        var world=furnace.getLevel();if(world==null)return;
        var random=world.random;var pos=furnace.getBlockPos();var output=furnace.output();int fx=output.getStepX(),fz=output.getStepZ();
        // FXDispatcher.furnaceLavaFx and TileInfernalFurnace block event 3, including five sounds.
        for(int n=0;n<5;n++){
            float qx=fx==0?(random.nextFloat()-random.nextFloat())*.5f:fx*random.nextFloat();
            float qz=fz==0?(random.nextFloat()-random.nextFloat())*.5f:fz*random.nextFloat();
            double x=pos.getX()+.5f+(random.nextFloat()-random.nextFloat())*.3f+fx;
            double z=pos.getZ()+.5f+(random.nextFloat()-random.nextFloat())*.3f+fz;
            world.addParticle(ParticleTypes.LAVA,x,pos.getY()+.3f,z,.15f*qx,.2f*random.nextFloat(),.15f*qz);
            world.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
        }
    });}
    private InfernalFurnaceClient(){}
}
