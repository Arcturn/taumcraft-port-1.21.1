package thaumcraft.port.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** TC6 ItemGoggles: the same item works as a helmet or a head bauble. */
public final class GogglesItem extends ArmorItem implements ICurioItem {
    public static final int VIS_DISCOUNT = 5;
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/entity/armor/goggles.png");

    public GogglesItem(Properties properties) {
        super(PortContent.SPECIAL_ARMOR, Type.HELMET, properties);
    }

    @Override public String getDescriptionId() { return "item.goggles.name"; }

    @Override public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                                      ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }

    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + VIS_DISCOUNT + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
