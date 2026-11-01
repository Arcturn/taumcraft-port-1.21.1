package thaumcraft.port.infusion.content;

import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.port.infusion.armor.VisDiscountGear;
import thaumcraft.port.infusion.bauble.BaubleItem;
import thaumcraft.port.research.ResearchStorage;

public final class VoidseerCharmItem extends BaubleItem implements VisDiscountGear,WarpingGear {
    public static IntSupplier clientPermanentWarp=()->0;
    public VoidseerCharmItem(Properties properties){super(properties);}
    @Override public int visDiscount(ItemStack stack,Player player){
        int warp=player instanceof ServerPlayer server?ResearchStorage.get(server.getServer()).player(server.getUUID()).permanentWarp():clientPermanentWarp.getAsInt();
        return (int)(Math.clamp(warp,0,100)/100f*25);
    }
    @Override public int gearWarp(ItemStack stack,Player player){return visDiscount(stack,player)/5;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        tooltip.add(Component.translatable("item.voidseer_charm.text").withStyle(ChatFormatting.DARK_BLUE,ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tc.visdiscount").append(": "+visDiscount(stack,null)+"%").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
