package thaumcraft.port.infusion.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.port.infusion.bauble.BaubleItem;
import thaumcraft.port.infusion.structure.ItemCharge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** The crafted (metadata 1) amulet recharges one hotbar, bauble or armor item every five ticks. */
public final class VisAmuletItem extends BaubleItem {
    public VisAmuletItem(Properties properties){super(properties);}
    @Override public String getDescriptionId(){return "item.amulet_vis.crafted.name";}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);tooltip.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
    }
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(context.cosmetic()||!(context.entity() instanceof ServerPlayer player)||player.tickCount%5!=0)return;
        rechargeOne(player);
    }
    public static boolean rechargeOne(ServerPlayer player){
        for(int i=0;i<9;i++)if(recharge(player,player.getInventory().getItem(i)))return true;
        boolean charged=CuriosApi.getCuriosInventory(player).map(inventory->{
            for(var handler:inventory.getCurios().values())for(int slot=0;slot<handler.getStacks().getSlots();slot++)
                if(inventory.isSlotActive(handler.getIdentifier(),slot)&&recharge(player,handler.getStacks().getStackInSlot(slot)))return true;
            return false;
        }).orElse(false);
        if(charged)return true;
        for(var armor:player.getArmorSlots())if(recharge(player,armor))return true;
        return false;
    }
    private static boolean recharge(ServerPlayer player,ItemStack stack){return ItemCharge.recharge(player.serverLevel(),stack,player.blockPosition(),player,1)>0;}
}
