package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.mob.MobContent;
import thaumcraft.port.mob.TaintSwarm;

/** Taint geysers replenish a little flux and defend their area with one ordinary swarm. */
public final class TaintGeyserBlock extends TaintBlock {
    public TaintGeyserBlock(Properties properties){super("taint_geyser",properties);}

    @Override public void die(Level level,BlockPos pos,BlockState state){level.setBlockAndUpdate(pos,WorldContent.block("flux_goo").defaultBlockState());}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!level.hasChunkAt(pos))return;
        if(!TaintSystem.nearby(level,pos)&&random.nextInt(10)==0){die(level,pos,state);return;}
        Vec3 center=Vec3.atCenterOf(pos);
        // The original helper uses a 32-block box for swarms and a sphere for players.
        if(random.nextFloat()<.2f&&level.hasNearbyAlivePlayer(center.x,center.y,center.z,32)
                &&level.getEntitiesOfClass(TaintSwarm.class,new AABB(center,center).inflate(32)).isEmpty()){
            var swarm=MobContent.SWARM.get().create(level);
            if(swarm!=null){swarm.moveTo(pos.getX()+.5,pos.getY()+1.25,pos.getZ()+.5,random.nextInt(360),0);level.addFreshEntity(swarm);}
        }else{
            var aura=AuraSystem.get(level,pos);
            if(aura!=null&&aura.flux()<2)AuraSystem.pollute(level,pos,.25f,true);
        }
    }
}
