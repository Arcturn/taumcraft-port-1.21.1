package thaumcraft.port.theory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.content.PortContent;

public final class WoodTableBlock extends Block {
    public WoodTableBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return ResearchTableBlock.SHAPE;}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(PortContent.SCRIBING_TOOLS.get()))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(player instanceof ServerPlayer sp){
            level.setBlock(pos,TheoryContent.TABLE.get().defaultBlockState().setValue(ResearchTableBlock.FACING,player.getDirection()),3);
            if(level.getBlockEntity(pos) instanceof ResearchTableEntity table){table.setItem(0,stack.copy());player.setItemInHand(hand,ItemStack.EMPTY);player.getInventory().setChanged();thaumcraft.port.research.ResearchProgress.craft(sp,new ItemStack(TheoryContent.TABLE.get()));}
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
