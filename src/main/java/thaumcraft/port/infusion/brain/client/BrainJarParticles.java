package thaumcraft.port.infusion.brain.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.infusion.brain.BrainJarContent;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BrainJarParticles {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->BrainJarContent.fullSpark=(world,pos)->{if(world instanceof ClientLevel level)Minecraft.getInstance().particleEngine.add(new Spark(level,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5));});}
    /** TileJarBrain's full-jar FXDispatcher.spark: 3 scale, half alpha, green with source jitter. */
    private static final class Spark extends TextureSheetParticle {
        private final int first;private final boolean flipped;
        Spark(ClientLevel level,double x,double y,double z){super(level,x,y,z);xd=yd=zd=0;setSize(.1f,.1f);lifetime=5+random.nextInt(5);first=8+random.nextInt(3)*16;flipped=random.nextBoolean();quadSize=.3f;alpha=.5f;setColor(.2f+random.nextFloat()*.2f,1,.3f+random.nextFloat()*.2f);}
        private int frame(){return first+Math.min(7,age*8/lifetime);}
        @Override protected float getU0(){return (frame()%16+(flipped?1:0))/16f;}
        @Override protected float getU1(){return (frame()%16+(flipped?0:1))/16f;}
        @Override protected float getV0(){return frame()/16/16f;}
        @Override protected float getV1(){return (frame()/16+1)/16f;}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){if(age>0)super.render(buffer,camera,partial);}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.legacyLayer(true);}
    }
    private BrainJarParticles(){}
}
