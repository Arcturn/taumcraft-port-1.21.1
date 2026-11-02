package thaumcraft.port.golem.builder.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.golem.GolemContent;
import thaumcraft.port.golem.builder.GolemBuilderEntity;
import thaumcraft.port.client.SmelterVentParticle;
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GolemBuilderClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(GolemContent.BUILDER_ENTITY.get(),GolemBuilderRenderer::new);}
 @SubscribeEvent public static void menus(RegisterMenuScreensEvent event){event.register(GolemContent.BUILDER_MENU.get(),GolemBuilderScreen::new);}
 @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->GolemBuilderEntity.clientTick=GolemBuilderClient::tick);}
 private static void tick(GolemBuilderEntity b){var level=b.getLevel();if(level==null)return;var p=b.getBlockPos();int particles=0;float volume=0;
  if(b.press>b.previousPress&&b.press>=60){particles=16;volume=.66f;}else if(b.press>=90&&level.random.nextInt(8)==0){particles=1;volume=.1f;}else if(b.previousPress>=90&&b.press<b.previousPress)particles=10;
  for(int i=0;i<particles;i++)Minecraft.getInstance().particleEngine.add(new SmelterVentParticle((ClientLevel)level,p.getX()+.5,p.getY()+1,p.getZ()+.5,level.random.nextGaussian()*.1,0,level.random.nextGaussian()*.1));
  if(volume>0)level.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,volume,1+level.random.nextFloat()*.1f,false);
 }
 private GolemBuilderClient(){}
}
