package thaumcraft.port.artifice.furnace;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;

public final class InfernalFurnaceBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public InfernalFurnaceBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override public String getDescriptionId(){return "tile.infernal_furnace.name";}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(0,0,0,16,8,16);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfernalFurnaceEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==InfernalFurnaceContent.ENTITY.get()?(l,p,s,e)->((InfernalFurnaceEntity)e).tick():null;}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of();}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))InfernalFurnaceStructure.dismantle(level,pos,state,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value){return level.getBlockEntity(pos) instanceof InfernalFurnaceEntity furnace&&furnace.triggerEvent(id,value);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        var movement=entity.getDeltaMovement();
        double x=entity.getX()<pos.getX()+.3?1.0E-4:entity.getX()>pos.getX()+.7?-1.0E-4:0;
        double z=entity.getZ()<pos.getZ()+.3?1.0E-4:entity.getZ()>pos.getZ()+.7?-1.0E-4:0;
        entity.setDeltaMovement(movement.add(x,0,z));
        if(!level.isClientSide&&entity.tickCount%10==0){
            if(entity instanceof ItemEntity item){
                entity.setDeltaMovement(entity.getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,.025));
                if(item.onGround()&&level.getBlockEntity(pos) instanceof InfernalFurnaceEntity furnace){
                    var remainder=furnace.accept(item.getItem());if(remainder.isEmpty())item.discard();else item.setItem(remainder);
                }
            }else if(entity instanceof LivingEntity&&!entity.fireImmune()){
                entity.hurt(level.damageSources().inFire(),3);entity.igniteForSeconds(10);
            }
        }
        super.entityInside(state,level,pos,entity);
    }
}
