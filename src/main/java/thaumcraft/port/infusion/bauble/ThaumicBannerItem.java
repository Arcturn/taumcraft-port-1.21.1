package thaumcraft.port.infusion.bauble;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.port.essentia.EssentiaItem;
import java.util.List;

public final class ThaumicBannerItem extends BlockItem {
    public ThaumicBannerItem(Block block,Properties properties){super(block,properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);var aspect=EssentiaItem.aspect(stack);if(aspect!=null)tooltip.add(Component.literal(aspect.displayName()));}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        if(context.getClickedFace()==Direction.DOWN)return null;
        return super.getPlacementState(context);
    }
}
