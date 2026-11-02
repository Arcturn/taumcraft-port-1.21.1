package thaumcraft.port.artifice.construct.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.port.artifice.construct.ArcaneBoreEntity;

/** PacketFXBoreDig's delay scheduled burst: ceil(50 / delay) particles each tick, one quarter sparkles. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class BoreEffects {
    private static final Map<ArcaneBoreEntity,Job> JOBS=new WeakHashMap<>();
    private static ClientLevel world;
    private static final class Job{int sequence,remaining,duration;BlockPos pos;BlockState state;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var client=Minecraft.getInstance();if(client.level!=world){JOBS.clear();world=client.level;}if(world==null||client.isPaused())return;
        for(var entity:world.entitiesForRendering())if(entity instanceof ArcaneBoreEntity bore){
            var job=JOBS.computeIfAbsent(bore,k->new Job());if(job.sequence!=bore.workSequence()){
                job.sequence=bore.workSequence();job.pos=bore.digTarget();job.duration=job.remaining=bore.digDuration();job.state=job.pos==null?null:world.getBlockState(job.pos);
            }
            if(job.remaining--<=0||job.state==null||job.state.isAir()||job.duration<=0)continue;
            for(int i=0;i<50f/job.duration;i++){
                boolean sparkle=world.random.nextInt(4)==0;double x=job.pos.getX()+world.random.nextFloat(),y=job.pos.getY()+world.random.nextFloat(),z=job.pos.getZ()+world.random.nextFloat();
                client.particleEngine.add(new BoreParticle(world,x,y,z,bore,job.state,sparkle));
            }
        }
        JOBS.keySet().removeIf(ArcaneBoreEntity::isRemoved);
    }
    private BoreEffects(){}
}
