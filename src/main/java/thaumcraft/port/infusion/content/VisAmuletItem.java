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

/** Original found/crafted variants recharge one item every forty/five ticks respectively. */
public final class VisAmuletItem extends BaubleItem {
    private final boolean found;
    public VisAmuletItem(Properties properties){this(properties,false);}
    public VisAmuletItem(Properties properties,boolean found){super(properties);this.found=found;}
    public int rechargeInterval(){return found?40:5;}
    @Override public String getDescriptionId(){return found?"item.amulet_vis.found.name":"item.amulet_vis.crafted.name";}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);tooltip.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
    }
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(context.cosmetic()||!(context.entity() instanceof ServerPlayer player)||player.tickCount%rechargeInterval()!=0)return;
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
