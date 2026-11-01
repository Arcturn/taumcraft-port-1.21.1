package thaumcraft.port.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.infusion.tools.*;

/** Original tool effects use TC6's linearly filtered particle sheet, not vanilla sprites. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ElementalToolClient {
    private static final KeyMapping ORIENTATION=new KeyMapping("key.thaumcraft.architect_orientation",GLFW.GLFW_KEY_G,"key.categories.thaumcraft");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(ORIENTATION); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ElementalToolVisuals.attraction=ElementalToolClient::attraction;
            ElementalToolVisuals.vortex=ElementalToolClient::vortex;
            ElementalToolVisuals.growth=ElementalToolClient::growth;
            ElementalToolVisuals.till=ElementalToolClient::till;
            ElementalToolPayload.visual=ElementalToolClient::placement;
        });
    }
    private static SalisMundusParticle particle(ClientLevel level,Vec3 pos,Vec3 velocity) {
        var particle=Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("salis_mundus"),
                pos.x,pos.y,pos.z,velocity.x,velocity.y,velocity.z);
        return particle instanceof SalisMundusParticle p ? p:null;
    }
    private static void attraction(ItemEntity item) {
        if (!(item.level() instanceof ClientLevel level)) return;
        var r=level.random;
        var p=particle(level,item.position().add((r.nextFloat()-r.nextFloat())*.2,
                item.getBbHeight()+(r.nextFloat()-r.nextFloat())*.2,(r.nextFloat()-r.nextFloat())*.2),Vec3.ZERO);
        if (p!=null) p.crucible(CrucibleVisualRules.bubble(r,CrucibleVisualRules.Kind.BUBBLE,1),.33f,.33f,1);
    }
    private static void vortex(LivingEntity entity) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        int minY=entity.onGround()?Mth.floor(entity.getBoundingBox().minY):(int)(entity.getBoundingBox().minY-2);
        for (int i=0;i<5;i++) Minecraft.getInstance().particleEngine.add(new ElementalVortexParticle(level,
                entity.getX(),entity.getBoundingBox().minY+entity.getBbHeight()/2,entity.getZ(),1.5f,level.random.nextInt(360),minY));
        if (entity.onGround()) {
            float angle=level.random.nextFloat()*(float)Math.PI*2;
            level.addParticle(ParticleTypes.SMOKE,entity.getX(),entity.getBoundingBox().minY+.1,entity.getZ(),-Mth.sin(angle)/5,0,Mth.cos(angle)/5);
        }
    }
    private static void growth(BlockPos pos) {
        var level=Minecraft.getInstance().level;
        if (level==null) return;
        var shape=level.getBlockState(pos).getShape(level,pos);
        if (shape.isEmpty()) return;
        var box=shape.bounds();var r=level.random;
        for (int i=0;i<8;i++) {
            var p=particle(level,new Vec3(pos.getX()+box.minX+r.nextFloat()*box.getXsize(),pos.getY()+box.minY+r.nextFloat()*box.getYsize(),
                    pos.getZ()+box.minZ+r.nextFloat()*box.getZsize()),new Vec3(r.nextGaussian()*.01,r.nextFloat()*.075,r.nextGaussian()*.01));
            if (p!=null) p.growthMist();
        }
    }
    private static void placement(ElementalToolPayload.Placement placement) {
        var level=Minecraft.getInstance().level;
        if (level==null || !level.hasChunkAt(placement.pos())) return;
        int color=8401408;float red=((color>>16)&255)/255f,green=((color>>8)&255)/255f,blue=(color&255)/255f;
        bamf(level,Vec3.atCenterOf(placement.pos()),placement.face(),red,green,blue);
    }
    private static void till(BlockPos pos,boolean center) {
        var level=Minecraft.getInstance().level;
        if (level==null) return;
        var point=new Vec3(pos.getX()+.5,pos.getY()+1.01,pos.getZ()+.5);
        if (center) level.playLocalSound(point.x,point.y,point.z,thaumcraft.port.content.PortSounds.get("poof"),
                net.minecraft.sounds.SoundSource.BLOCKS,.4f,1+(float)level.random.nextGaussian()*.05f,false);
        bamf(level,point,net.minecraft.core.Direction.UP,.3f,.12f,.1f);
    }
    private static void bamf(ClientLevel level,Vec3 center,net.minecraft.core.Direction face,float red,float green,float blue) {
        var r=level.random;
        for (int i=0;i<8+r.nextInt(3);i++) {
            var velocity=new Vec3((.05+r.nextFloat()*.05)*(r.nextBoolean()?-1:1)+face.getStepX()*.1,
                    (.05+r.nextFloat()*.05)*(r.nextBoolean()?-1:1)+face.getStepY()*.1,
                    (.05+r.nextFloat()*.05)*(r.nextBoolean()?-1:1)+face.getStepZ()*.1);
            var p=particle(level,center.add(velocity.scale(2)),velocity.scale(.5));
            if (p!=null) p.smoke(Mth.clamp(red*(1+(float)r.nextGaussian()*.1f),0,1),
                    Mth.clamp(green*(1+(float)r.nextGaussian()*.1f),0,1),Mth.clamp(blue*(1+(float)r.nextGaussian()*.1f),0,1));
        }
        for (int i=0;i<r.nextInt(3);i++) {
            var velocity=new Vec3((.0025+r.nextFloat()*.005)*(r.nextBoolean()?-1:1)+face.getStepX()*.025,
                    (.0025+r.nextFloat()*.005)*(r.nextBoolean()?-1:1)+face.getStepY()*.025,
                    (.0025+r.nextFloat()*.005)*(r.nextBoolean()?-1:1)+face.getStepZ()*.025);
            var p=particle(level,center.add(velocity.scale(5)),velocity);
            if (p!=null) p.wisp(i,(.9f+r.nextFloat()*.1f+red)/2,(.1f+green)/2,(.5f+r.nextFloat()*.1f+blue)/2);
        }
    }
    @EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
    public static final class Gameplay {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var client=Minecraft.getInstance();
            while (ORIENTATION.consumeClick()) if (client.player!=null && client.screen==null
                    && client.player.getMainHandItem().is(InfusionTools.SHOVEL.get()))
                PacketDistributor.sendToServer(new ElementalToolPayload());
        }
        @SubscribeEvent public static void overlay(RenderHighlightEvent.Block event) {
            var client=Minecraft.getInstance();
            if (client.player==null || client.level==null) return;
            var stack=client.player.getMainHandItem();
            if (!stack.is(InfusionTools.SHOVEL.get())) stack=client.player.getOffhandItem();
            if (!stack.is(InfusionTools.SHOVEL.get())) return;
            var blocks=ElementalShovelItem.preview(stack,client.level,event.getTarget().getBlockPos(),event.getTarget().getDirection(),client.player);
            if (!blocks.isEmpty()) ArchitectOverlay.render(event,blocks);
        }
    }
    private ElementalToolClient() {}
}
