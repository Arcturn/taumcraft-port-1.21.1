package thaumcraft.port.infusion.armor;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import thaumcraft.port.infusion.recipe.InfusionAugments;
public final class FortressArmorItem extends ArmorItem {
    public FortressArmorItem(Type type,Properties properties){super(InfusionArmor.FORTRESS,type,properties);}
    @Override public String getDescriptionId(){return "item.fortress_"+switch(type){case HELMET->"helm";case CHESTPLATE->"chest";default->"legs";}+".name";}
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return ResourceLocation.parse("thaumcraft:textures/entity/armor/fortress_armor.png");}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        if(InfusionAugments.goggles(stack))tooltip.add(Component.translatable("item.goggles.name").withStyle(ChatFormatting.DARK_PURPLE));
        if(InfusionAugments.mask(stack)>=0)tooltip.add(Component.translatable("item.fortress_helm.mask."+InfusionAugments.mask(stack)).withStyle(ChatFormatting.GOLD));
    }
}
