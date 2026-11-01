package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.essentia.EssentiaItem;
import thaumcraft.port.infusion.bauble.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionBaublesClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        ItemProperties.register(InfusionBaubles.VERDANT_CHARM.get(),ResourceLocation.parse("thaumcraft:type"),(stack,level,entity,seed)->VerdantCharmItem.kind(stack));
        CuriosRendererRegistry.register(InfusionBaubles.CURIOSITY_BAND.get(),CuriosityRenderer::new);
    });}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(ThaumicBannerRenderer.LAYER,ThaumicBannerRenderer::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(InfusionBaubles.BANNER_ENTITY.get(),ThaumicBannerRenderer::new);}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        for(var entry:InfusionBaubles.BANNERS.entrySet())event.register((stack,tint)->{
            if(tint==2&&EssentiaItem.aspect(stack)!=null)return 0xff000000|EssentiaItem.aspect(stack).color();
            return tint==1||tint==2?0xff000000|entry.getKey().getMapColor().col:0xffffffff;
        },entry.getValue().get().asItem());
    }
    public static final class CuriosityRenderer implements ICurioRenderer {
        @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float limbSwing,float limbSwingAmount,float partial,float age,float yaw,float pitch){
            if(context.entity().isInvisible()||!(parent.getModel() instanceof HumanoidModel<?> model))return;
            pose.pushPose();model.head.translateAndRotate(pose);
            if(context.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty())pose.translate(0,0,.066f);
            GogglesGeometry.emit(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/items/curiosity_band_worn.png"))),light);pose.popPose();
        }
    }
    @EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
    public static final class Input {
        private static boolean wasDown,used;private static int lastGroundJump=-100;private static Player lastPlayer;
        @SubscribeEvent public static void jump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event){if(event.getEntity()==Minecraft.getInstance().player&&event.getEntity().onGround())lastGroundJump=event.getEntity().tickCount;}
        @SubscribeEvent public static void tick(ClientTickEvent.Post event){
            var mc=Minecraft.getInstance();var player=mc.player;
            if(player!=lastPlayer){lastPlayer=player;wasDown=false;used=false;lastGroundJump=-100;}
            if(player==null)return;
            boolean down=mc.options.keyJump.isDown(),pressed=down&&!wasDown;wasDown=down;
            if(player.onGround())used=false;
            // TC6 waits for the vanilla ten-tick ground jump cooldown, but a step off a ledge has none.
            if(mc.screen==null&&pressed&&!used&&!player.onGround()&&player.tickCount-lastGroundJump>=10&&!player.isInWater()&&!player.isPassenger()&&!player.getAbilities().flying&&BaubleEvents.worn(player,InfusionBaubles.CLOUD_RING.get())){
                used=true;BaubleEvents.applyJump(player);PacketDistributor.sendToServer(new CloudJumpPayload());
            }
        }
    }
    public static void cloudEffect(int entityId){
        var mc=Minecraft.getInstance();if(mc.level==null)return;var entity=mc.level.getEntity(entityId);if(entity==null)return;
        var random=mc.level.random;var center=entity.position().add(0,.5,0);
        for(int i=0;i<8+random.nextInt(3);i++){
            Vec3 velocity=new Vec3(signed(random,.05f,.05f),signed(random,.05f,.05f)+.1,signed(random,.05f,.05f));
            var p=particle(center.add(velocity.scale(2)),velocity.scale(.5));if(p!=null)p.smoke(tint(random),tint(random),tint(random));
        }
        for(int i=0;i<random.nextInt(3);i++){
            float red=(1.9f+random.nextFloat()*.1f)/2,blue=(1.5f+random.nextFloat()*.1f)/2;
            Vec3 velocity=new Vec3(signed(random,.0025f,.005f),signed(random,.0025f,.005f)+.025,signed(random,.0025f,.005f));
            var p=particle(center.add(velocity.scale(5)),velocity);if(p!=null)p.wisp(i,red,.55f,blue);
        }
        mc.level.playLocalSound(entity.getX(),entity.getY(),entity.getZ(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.1f,1+(float)random.nextGaussian()*.05f,false);
    }
    private static SalisMundusParticle particle(Vec3 pos,Vec3 velocity){var p=Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("salis_mundus"),pos.x,pos.y,pos.z,velocity.x,velocity.y,velocity.z);return p instanceof SalisMundusParticle dust?dust:null;}
    private static float signed(RandomSource random,float base,float range){return(base+random.nextFloat()*range)*(random.nextBoolean()?-1:1);}
    private static float tint(RandomSource random){return Math.clamp(1+(float)random.nextGaussian()*.1f,0,1);}
    public static void barrierRunes(Level level,BlockPos pos,RandomSource random){
        if(!(level instanceof net.minecraft.client.multiplayer.ClientLevel client))return;
        if(level.hasNeighborSignal(pos)){for(int i=0;i<4;i++)rune(client,pos,pos.getY()+.7,.2f+random.nextFloat()*.4f,random.nextFloat()*.3f,.8f+random.nextFloat()*.2f,20,-.02f);return;}
        for(int y=1;y<=2;y++)if(level.getBlockState(pos.above(y)).is(InfusionBaubles.BARRIER.get())&&!BarrierBlock.active(level,pos.above(y))){for(int i=0;i<6;i++)rune(client,pos,pos.getY()+.7,.9f+random.nextFloat()*.1f,random.nextFloat()*.3f,random.nextFloat()*.3f,24,-.02f);return;}
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(1),e->!(e instanceof Player))){rune(client,pos,pos.getY()+.6+random.nextFloat()*Math.max(.8,entity.getEyeHeight()),.6f+random.nextFloat()*.4f,0,.3f+random.nextFloat()*.7f,20,0);break;}
    }
    private static void rune(net.minecraft.client.multiplayer.ClientLevel level,BlockPos pos,double y,float r,float g,float b,int duration,float gravity){var p=new PavingRuneParticle(level,pos.getX()+.5,y+.5,pos.getZ()+.5,duration,gravity);p.setColor(r,g,b);Minecraft.getInstance().particleEngine.add(p);}
    private InfusionBaublesClient(){}
}
