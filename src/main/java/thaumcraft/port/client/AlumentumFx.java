package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.machine.AlumentumVisualBridge;
import thaumcraft.port.world.AlumentumProjectile;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AlumentumFx {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->AlumentumVisualBridge.tick=AlumentumFx::tick);}
    private static void tick(AlumentumProjectile projectile){
        if(!(projectile.level() instanceof ClientLevel level))return;
        var random=level.random;
        for(int i=0;i<3;i++){
            double fraction=i/3.0;
            Minecraft.getInstance().particleEngine.add(new AlumentumFireParticle(level,
                    projectile.xo+(projectile.getX()-projectile.xo)*fraction,
                    projectile.yo+(projectile.getY()-projectile.yo)*fraction+projectile.getBbHeight()/2,
                    projectile.zo+(projectile.getZ()-projectile.zo)*fraction,
                    .0125*(random.nextFloat()-.5),.0125*(random.nextFloat()-.5),.0125*(random.nextFloat()-.5),
                    random.nextFloat()*.2f,random.nextFloat()*.1f,random.nextFloat()*.1f));
            level.addParticle(PortParticles.get("alumentum"),projectile.getX()+random.nextGaussian()*.2,
                    projectile.getY()+random.nextGaussian()*.2,projectile.getZ()+random.nextGaussian()*.2,0,0,0);
        }
    }
    private AlumentumFx(){}
}
