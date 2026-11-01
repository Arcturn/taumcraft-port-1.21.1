package thaumcraft.port.infusion.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

public final class RobeArmorItem extends ArmorItem implements VisDiscountGear {
    public static final int DEFAULT_COLOR=6961280;
    public RobeArmorItem(Type type,Properties properties){super(InfusionArmor.CLOTH,type,properties);}
    @Override public String getDescriptionId(){return "item.cloth_"+switch(type){case CHESTPLATE->"chest";case LEGGINGS->"legs";default->"boots";}+".name";}
    @Override public int visDiscount(ItemStack stack,Player player){return type==Type.BOOTS?2:3;}
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){
        return ResourceLocation.parse("thaumcraft:textures/entity/armor/robes_"+(inner?2:1)+(layer.dyeable()?"":"_overlay")+".png");
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var state=context.getLevel().getBlockState(context.getClickedPos());
        if(state.is(Blocks.WATER_CAULDRON)){
            if(!context.getLevel().isClientSide){context.getItemInHand().remove(DataComponents.DYED_COLOR);LayeredCauldronBlock.lowerFillLevel(state,context.getLevel(),context.getClickedPos());}
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);tooltip.add(Component.translatable("tc.visdiscount").append(": "+visDiscount(stack,null)+"%").withStyle(ChatFormatting.DARK_PURPLE));}
}
