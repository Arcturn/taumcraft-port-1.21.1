package thaumcraft.port.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.infusion.structure.*;

@EventBusSubscriber(modid="thaumcraft", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class StructureClient {
    record Shield(long start, int duration) {}
    static final Map<BlockEntity, Shield> SHIELDS = new WeakHashMap<>();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> { StructureEffects.events=StructureClient::blockEvent; StructureEffects.ambient=StructureClient::ambient; }); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StructureContent.PEDESTAL_ENTITY.get(), PedestalRenderer::new);
        event.registerBlockEntityRenderer(StructureContent.RECHARGE_ENTITY.get(), PedestalRenderer::new);
    }
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,tint) -> gray(state.getValue(InlayBlock.CHARGE)), StructureContent.INLAY.get());
        event.register((state,level,pos,tint) -> gray(level != null && pos != null && level.getBlockEntity(pos) instanceof StabilizerEntity stabilizer ? stabilizer.getEnergy() : 0), StructureContent.STABILIZER.get());
    }
    @SubscribeEvent public static void items(RegisterColorHandlersEvent.Item event) { event.register((stack,tint) -> gray(0), StructureContent.STABILIZER.get().asItem()); }
    public static int gray(int charge) { int value=(int)((charge==0?.3f:charge/15f*.5f+.5f)*255); return 0xff000000 | value<<16 | value<<8 | value; }
    private static void blockEvent(BlockEntity entity, int event, int parameter) {
        if (!(entity.getLevel() instanceof ClientLevel level)) return;
        BlockPos pos=entity.getBlockPos();
        if (entity instanceof RechargePedestalEntity && event==5) {
            var random=level.random;
            // TileRechargePedestal chooses all block coordinates before
            // FXDispatcher adds its six source/target fractional offsets.
            int x=pos.getX()+random.nextInt(3)-random.nextInt(3),y=pos.getY()+1+random.nextInt(3),z=pos.getZ()+random.nextInt(3)-random.nextInt(3);
            Vec3 source=new Vec3(x+random.nextFloat(),y+random.nextFloat(),z+random.nextFloat());
            Vec3 target=new Vec3(pos.getX()+.4+random.nextFloat()*.2,pos.getY()+1.4+random.nextFloat()*.2,pos.getZ()+.4+random.nextFloat()*.2);
            Minecraft.getInstance().particleEngine.add(new StructureParticle.VisSparkle(level,source,target,parameter));
        } else if (event==5) SHIELDS.put(entity,new Shield(level.getGameTime(),8+level.random.nextInt(4)));
        else if (event==11 || event==12) {
            Vec3 center=Vec3.atCenterOf(pos.above());
            level.playLocalSound(center.x,center.y,center.z,PortSounds.get("poof"),SoundSource.BLOCKS,.4f,1+(float)level.random.nextGaussian()*.05f,false);
            if(event==12) SalisMundusFx.bamf(level,center,true,null);
            else purpleBamf(level,center);
        }
    }
    private static void purpleBamf(ClientLevel level, Vec3 center) {
        var random=level.random;
        for(int i=0;i<8+random.nextInt(3);i++) {
            Vec3 velocity=new Vec3(signed(level,.05,.05),signed(level,.05,.05),signed(level,.05,.05));
            var particle=create(center.add(velocity.scale(2)),velocity.scale(.5));
            if(particle!=null)particle.smoke(Math.clamp(.75f*(1+(float)random.nextGaussian()*.1f),0,1),0,Math.clamp(.5f*(1+(float)random.nextGaussian()*.1f),0,1));
        }
        for(int i=0;i<2+random.nextInt(3);i++) {Vec3 velocity=new Vec3(signed(level,.025,.025),signed(level,.025,.025),signed(level,.025,.025));var particle=create(center.add(velocity.scale(2)),velocity);if(particle!=null)particle.mote(15+random.nextInt(10));}
        var flash=create(center,Vec3.ZERO);if(flash!=null)flash.flash();
        for(int i=0;i<2+random.nextInt(3);i++) {Vec3 velocity=new Vec3(signed(level,.0025,.005),signed(level,.0025,.005),signed(level,.0025,.005));var particle=create(center.add(velocity.scale(5)),velocity);if(particle!=null)particle.wisp(i,(.9f+random.nextFloat()*.1f+.75f)/2,.05f,(.5f+random.nextFloat()*.1f+.5f)/2);}
    }
    private static double signed(ClientLevel level,double base,double range) { return (base+level.random.nextFloat()*range)*(level.random.nextBoolean()?-1:1); }
    private static SalisMundusParticle create(Vec3 point,Vec3 velocity) {
        var particle=Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("salis_mundus"),point.x,point.y,point.z,velocity.x,velocity.y,velocity.z);
        return particle instanceof SalisMundusParticle dust?dust:null;
    }
    private static void ambient(Level world,BlockPos pos,int charge,boolean pedestal) {
        if(!(world instanceof ClientLevel level))return;
        if(pedestal) Minecraft.getInstance().particleEngine.add(new StructureParticle.BlockRune(level,pos));
        else {
            var faces=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
            Direction face=faces[level.random.nextInt(4)];
            if(level.getBlockState(pos).getValue(InlayBlock.CONNECTIONS.get(face))!=InlayBlock.Connection.NONE)
                Minecraft.getInstance().particleEngine.add(new StructureParticle.InlaySpark(level,pos,face));
        }
    }
    private StructureClient() {}
}
