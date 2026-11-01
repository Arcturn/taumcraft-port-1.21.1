package thaumcraft.port.infusion.bauble;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class PavingStoneBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,15,16);
    public final boolean barrier;
    public PavingStoneBlock(boolean barrier,Properties properties){super(properties);this.barrier=barrier;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){
        if(!barrier&&!level.isClientSide&&entity instanceof LivingEntity living){living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1,false,false));living.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0,false,false));}
        super.stepOn(level,pos,state,entity);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return barrier?new BarrierStoneEntity(pos,state):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&barrier&&type==InfusionBaubles.BARRIER_STONE_ENTITY.get()?(l,p,s,be)->((BarrierStoneEntity)be).tick():null;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(barrier)thaumcraft.port.client.InfusionBaublesClient.barrierRunes(level,pos,random);}
}
