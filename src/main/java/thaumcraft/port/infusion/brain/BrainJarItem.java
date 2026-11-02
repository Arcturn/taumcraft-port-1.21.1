package thaumcraft.port.infusion.brain;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/** Portable XP uses vanilla block-entity components, so placement and loot preserve it. */
public final class BrainJarItem extends BlockItem {
    public BrainJarItem(Block block,Properties properties){super(block,properties);}
    public static int experience(ItemStack stack){return Math.clamp(stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag().getInt("XP"),0,BrainJarEntity.CAPACITY);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,context,lines,flag);if(experience(stack)>0)lines.add(Component.literal(experience(stack)+" xp").withStyle(ChatFormatting.GREEN));}
}
