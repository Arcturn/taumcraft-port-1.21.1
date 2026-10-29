package thaumcraft.port.essentia;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class JarItem extends BlockItem {
    public JarItem(Block block,Properties properties){super(block,properties);}
    public static EssentiaTank contents(ItemStack stack){var tank=new EssentiaTank(250);EssentiaEntity.readTank(stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag(),tank);return tank;}
    /** Prepare exactly one filled jar; caller commits source extraction only after this succeeds. */
    public static ItemStack filledCopy(ItemStack stack,JarEntity source){
        if(stack.isEmpty()||!source.alembic||source.tank.amount()==0)return ItemStack.EMPTY;
        var tank=contents(stack);int count=tank.insert(source.tank.aspect(),source.tank.amount(),false,false);
        if(count==0)return ItemStack.EMPTY;
        var result=stack.copyWithCount(1);var tag=result.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag();
        tag.remove("Aspect");tag.remove("Filter");EssentiaEntity.writeTank(tag,tank);tag.putString("id","thaumcraft:jar");
        result.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));return result;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
        if(player==null||!(level.getBlockEntity(pos) instanceof JarEntity source)||!source.alembic||source.tank.amount()==0)return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        var result=filledCopy(stack,source);if(result.isEmpty())return InteractionResult.FAIL;
        if(!level.isClientSide){
            int transferred=contents(result).amount()-contents(stack).amount();
            if(source.tank.extract(source.tank.aspect(),transferred,false)!=transferred)return InteractionResult.FAIL;
            // Even in Creative, split a single container rather than cloning a filled stack.
            stack.shrink(1);if(stack.isEmpty())player.setItemInHand(context.getHand(),result);else if(!player.getInventory().add(result))player.drop(result,false);
            source.sync();level.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.25f,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);var tank=contents(stack);
        if(tank.aspect()!=null)lines.add(Component.translatable("tooltip.thaumcraft.jar_contents",Component.literal(tank.aspect().displayName()),tank.amount()));
        if(tank.filter()!=null)lines.add(Component.translatable("tooltip.thaumcraft.jar_filter",Component.literal(tank.filter().displayName())));
    }
}
