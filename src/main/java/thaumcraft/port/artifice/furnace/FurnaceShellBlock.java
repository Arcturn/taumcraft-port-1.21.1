package thaumcraft.port.artifice.furnace;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

public final class FurnaceShellBlock extends Block {
    public final Block original;
    public FurnaceShellBlock(Block original,Properties properties){super(properties);this.original=original;}
    @Override public String getDescriptionId(){return "tile.infernal_furnace.name";}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of(new ItemStack(original));}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))InfernalFurnaceStructure.shellRemoved(level,pos);
        super.onRemove(state,level,pos,next,moving);
    }
}
