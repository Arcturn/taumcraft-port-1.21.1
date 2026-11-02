package thaumcraft.port.infusion.bauble;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.infusion.structure.*;
import thaumcraft.port.world.WorldEntities;
import top.theillusivec4.curios.api.SlotContext;

/** Cure, life and sustain share one persistent TC6 type byte and a 200-vis battery. */
public final class VerdantCharmItem extends BaubleItem implements RechargeableItem {
    public VerdantCharmItem(Properties properties){super(properties);}
    public static int kind(ItemStack stack){return InfusionAugments.type(stack);}
    public static ItemStack stack(int kind){var stack=new ItemStack(InfusionBaubles.VERDANT_CHARM.get());InfusionAugments.setType(stack,kind);return stack;}
    @Override public int maxCharge(ItemStack stack,LivingEntity entity){return 200;}
    @Override public ChargeDisplay chargeDisplay(ItemStack stack,LivingEntity entity){return ChargeDisplay.NORMAL;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);
        if(kind(stack)>0)tooltip.add(Component.translatable("item.verdant_charm."+(kind(stack)==1?"life":"sustain")+".text").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tc.charge").append(" "+ItemCharge.get(stack)+" / "+ItemCharge.maximum(stack,null)).withStyle(ChatFormatting.YELLOW));
    }
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(context.cosmetic()||!(context.entity() instanceof Player player)||player.level().isClientSide||player.tickCount%20!=0)return;
        wornSecond(stack,player);
    }
    public static void wornSecond(ItemStack stack,Player player){
        if(player.level().isClientSide)return;
        if(player.hasEffect(MobEffects.WITHER)&&ItemCharge.consume(stack,player,20)){player.removeEffect(MobEffects.WITHER);return;}
        if(player.hasEffect(MobEffects.POISON)&&ItemCharge.consume(stack,player,10)){player.removeEffect(MobEffects.POISON);return;}
        if(player.hasEffect(WorldEntities.TAINT)&&ItemCharge.consume(stack,player,5)){player.removeEffect(WorldEntities.TAINT);return;}
        if(kind(stack)==1&&player.getHealth()<player.getMaxHealth()&&ItemCharge.consume(stack,player,5)){player.heal(1);return;}
        if(kind(stack)==2&&player.getAirSupply()<100&&ItemCharge.consume(stack,player,1)){player.setAirSupply(300);return;}
        if(kind(stack)==2&&player.canEat(false)&&ItemCharge.consume(stack,player,1))player.getFoodData().eat(1,.3f);
    }
}
