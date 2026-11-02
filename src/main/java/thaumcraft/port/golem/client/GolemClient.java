package thaumcraft.port.golem.client;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.item.GolemPlacerItem;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GolemClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(GolemContent.GOLEM.get(),GolemRenderer::new);
        event.registerEntityRenderer(GolemContent.DART.get(),GolemProjectileRenderers.Dart::new);
        event.registerEntityRenderer(GolemContent.ORB.get(),GolemProjectileRenderers.Orb::new);
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->0xff000000|(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains("props")?GolemPlacerItem.getProperties(stack).getMaterial().itemColor:0xffffff),GolemContent.GOLEM_ITEM.get());
    }
    @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws IOException {GolemRenderLayers.shaders(event);}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        GolemVisuals.tick=GolemClient::tick;GolemVisuals.event=GolemClient::emote;
        GolemVisuals.orbBurst=orb->Minecraft.getInstance().particleEngine.add(GolemParticle.burst((ClientLevel)orb.level(),orb.position()));
    });}
    private static void tick(GolemEntity entity){
        if(!(entity.level() instanceof ClientLevel level))return;var random=entity.getRandom();var props=entity.getProperties();
        if(props.hasTrait(EnumGolemTrait.FLYER)&&(!entity.onGround()||entity.tickCount%5==0))
            Minecraft.getInstance().particleEngine.add(GolemParticle.flyer(level,entity.position().add(0,.1,0),new Vec3(random.nextGaussian()/100,-.1,random.nextGaussian()/100)));
        double dx=entity.getX()-entity.xo,dy=entity.getY()-entity.yo,dz=entity.getZ()-entity.zo;
        if(props.hasTrait(EnumGolemTrait.WHEELED)&&entity.onGround()&&!entity.isInWater()&&Math.sqrt(dx*dx+dy*dy+dz*dz)>.25){
            var position=BlockPos.containing(entity.getX(),entity.getY()-.2,entity.getZ());var state=level.getBlockState(position);
            if(state.getRenderShape()!=RenderShape.INVISIBLE){var velocity=entity.getDeltaMovement();
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,state),entity.getX()+(random.nextFloat()-.5)*entity.getBbWidth(),entity.getBoundingBox().minY+.1,entity.getZ()+(random.nextFloat()-.5)*entity.getBbWidth(),-velocity.x*4,1.5,-velocity.z*4);}
        }
    }
    private static void emote(GolemEntity entity,byte event){
        if(!(entity.level() instanceof ClientLevel level))return;var random=entity.getRandom();var point=entity.position().add(0,entity.getBbHeight()+.1,0);
        if(event==9){for(int i=0;i<5;i++)Minecraft.getInstance().particleEngine.add(GolemParticle.emote(level,point.add(0,-.1,0),new Vec3(random.nextGaussian()*.01,random.nextFloat()*.02,random.nextGaussian()*.01),13,1,20+random.nextInt(20),.3f+random.nextFloat()*.4f,1,1,1));return;}
        GolemParticle particle=switch(event){
            case 5->GolemParticle.emote(level,point,Vec3.ZERO,704+(random.nextBoolean()?0:3),3,6,2,1,1,1);
            case 6->GolemParticle.emote(level,point,new Vec3(0,.025,0),15,1,10,2,.1f,1,1);
            case 7->GolemParticle.emote(level,point,new Vec3(0,.05,0),640,10,10,2,1,1,1);
            case 8->GolemParticle.emote(level,point,new Vec3(0,.01,0),14,1,20,2,1,1,.1f);
            default->null;
        };if(particle!=null)Minecraft.getInstance().particleEngine.add(particle);
    }
    private GolemClient(){}
}
