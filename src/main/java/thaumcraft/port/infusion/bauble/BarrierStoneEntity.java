package thaumcraft.port.infusion.bauble;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BarrierStoneEntity extends BlockEntity {
    private int count;
    public BarrierStoneEntity(BlockPos pos,BlockState state){super(InfusionBaubles.BARRIER_STONE_ENTITY.get(),pos,state);}
    public void tick(){
        if(level==null||level.isClientSide)return;
        if(count==0)count=level.random.nextInt(100);
        if(count%5==0&&!level.hasNeighborSignal(worldPosition)){
            var area=new AABB(worldPosition).expandTowards(0,2,0).inflate(.1);
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,area,e->!(e instanceof Player)&&!e.onGround())){
                double yaw=(entity.getYRot()+180)*Math.PI/180;
                entity.push(-Math.sin(yaw)*.2,-.1,Math.cos(yaw)*.2);
            }
        }
        if(++count%100==0)for(int i=1;i<=2;i++)if(level.isEmptyBlock(worldPosition.above(i)))level.setBlockAndUpdate(worldPosition.above(i),InfusionBaubles.BARRIER.get().defaultBlockState());
    }
}
