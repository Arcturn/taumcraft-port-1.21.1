package thaumcraft.port.artifice.mirror;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import thaumcraft.port.content.PortSounds;

public final class MirrorBlockItem extends BlockItem {
    public MirrorBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();
        if(player==null||!(level.getBlockEntity(context.getClickedPos()) instanceof MirrorEntity target)||target.getBlockState().getBlock()!=getBlock())return InteractionResult.PASS;
        if(!level.isClientSide){
            if(target.peer()!=null){player.displayClientMessage(Component.translatable("thaumcraft.mirror.already_linked"),false);return InteractionResult.FAIL;}
            var bound=stack.copyWithCount(1);MirrorLink.set(bound,target.address());
            if(!player.isCreative())stack.shrink(1);
            if(stack.isEmpty())player.setItemInHand(context.getHand(),bound);else if(!player.getInventory().add(bound))player.drop(bound,false);
            player.getInventory().setChanged();level.playSound(null,context.getClickedPos(),PortSounds.get("jar"),SoundSource.BLOCKS,1,2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){super.appendHoverText(stack,context,lines,flags);MirrorLink.tooltip(stack,lines);}
}
