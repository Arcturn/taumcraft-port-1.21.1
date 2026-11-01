package thaumcraft.port.infusion.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import thaumcraft.port.infusion.armor.VisDiscountGear;

/** Source void materials, self repair, warping, and dyeable thaumaturge equipment. */
public final class VoidArmorItem extends ArmorItem implements VisDiscountGear, WarpingGear {
    public static final int DEFAULT_COLOR=6961280;
    private final boolean robe;
    public VoidArmorItem(Type type,boolean robe,Properties properties){
        super(robe?InfusionCraftingContent.VOID_ROBE:InfusionCraftingContent.VOID,type,properties);this.robe=robe;
    }
    public boolean isRobe(){return robe;}
    public boolean reveals(){return robe&&type==Type.HELMET;}
    @Override public int visDiscount(ItemStack stack,Player player){return robe?5:0;}
    @Override public int gearWarp(ItemStack stack,Player player){return robe?3:1;}
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){
        String name=robe?"void_robe_armor"+(layer.dyeable()?"_overlay":""):"void_"+(inner?2:1);
        return ResourceLocation.parse("thaumcraft:textures/entity/armor/"+name+".png");
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        super.inventoryTick(stack,level,entity,slot,selected);VoidTools.repair(stack,level,entity);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(robe&&context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.WATER_CAULDRON)){
            if(!context.getLevel().isClientSide){context.getItemInHand().remove(DataComponents.DYED_COLOR);
                LayeredCauldronBlock.lowerFillLevel(context.getLevel().getBlockState(context.getClickedPos()),context.getLevel(),context.getClickedPos());}
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        if(robe)tooltip.add(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
