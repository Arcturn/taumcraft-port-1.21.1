package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.essentia.CondenserVisualBridge;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CondenserFx {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->CondenserVisualBridge.spark=(condenser,pos)->{
        if(condenser.getLevel() instanceof ClientLevel level)Minecraft.getInstance().particleEngine.add(new Spark(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5));
    });}
    /** Original FXDispatcher.spark preset (16x16 sprites, eight animation frames). */
    private static final class Spark extends TextureSheetParticle {
        final int first;final boolean flipped;
        Spark(ClientLevel level,double x,double y,double z){super(level,x,y,z,0,0,0);xd=yd=zd=0;lifetime=5+random.nextInt(5);first=8+random.nextInt(3)*16;flipped=random.nextBoolean();quadSize=(4.5f+random.nextFloat())*.1f;alpha=.8f;setColor(.33f+random.nextFloat()*.66f,.33f+random.nextFloat()*.66f,.33f+random.nextFloat()*.66f);}
        private int frame(){return first+Math.min(7,age*8/lifetime);}
        @Override protected float getU0(){return (frame()%16+(flipped?1:0))/16f;}
        @Override protected float getU1(){return (frame()%16+(flipped?0:1))/16f;}
        @Override protected float getV0(){return frame()/16/16f;}
        @Override protected float getV1(){return (frame()/16+1)/16f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    }
    private CondenserFx(){}
}
