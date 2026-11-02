package thaumcraft.port.infusion.tools;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.infusion.structure.RechargeableItem;

public final class TravellerBootsItem extends ArmorItem implements RechargeableItem {
    private static final ResourceLocation STEP=ResourceLocation.parse("thaumcraft:traveller_step");
    public TravellerBootsItem(Properties properties) { super(InfusionTools.TRAVELLER_MATERIAL,Type.BOOTS,properties); }
    @Override public int maxCharge(ItemStack stack,LivingEntity entity) { return 240; }
    @Override public ChargeDisplay chargeDisplay(ItemStack stack,LivingEntity entity) { return ChargeDisplay.PERIODIC; }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        tooltip.add(Component.translatable("tc.charge").append(" "+ItemCharge.get(stack)+" / "+ItemCharge.maximum(stack,null)).withStyle(ChatFormatting.YELLOW));
    }
    public static boolean worn(Player player) { return player.getItemBySlot(EquipmentSlot.FEET).is(InfusionTools.BOOTS.get()); }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) { tickBoots(event.getEntity()); }
    public static void tickBoots(Player player) {
        ItemStack stack=player.getItemBySlot(EquipmentSlot.FEET);
        boolean worn=worn(player), charged=worn&&ItemCharge.get(stack)>0;
        if (worn && !player.level().isClientSide && player.tickCount%20==0) {
            int energy=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt("energy");
            if (energy>0) energy--;
            else if (ItemCharge.consume(stack,player,1)) energy=60;
            final int nextEnergy=energy;
            CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> tag.putInt("energy",nextEnergy));
        }
        boolean moving=charged&&!player.getAbilities().flying&&player.zza>0;
        var step=player.getAttribute(Attributes.STEP_HEIGHT);
        if (step!=null) {
            step.removeModifier(STEP);
            if (moving&&!player.isShiftKeyDown()&&step.getValue()<1)
                step.addTransientModifier(new AttributeModifier(STEP,1-step.getValue(),AttributeModifier.Operation.ADD_VALUE));
        }
        if (!moving) return;
        if (player.onGround()) player.moveRelative(1,new Vec3(0,0,player.isInWater()?.0125:.05));
        else if (player.isInWater()) player.moveRelative(1,new Vec3(0,0,.025));
        else {
            // 1.12's mutable jumpMovementFactor became Player#getFlyingSpeed in 1.21.
            // Supply only its difference, retaining vanilla's input normalization and sprint value.
            float vanilla=player.isSprinting()?.026f:.02f;
            player.moveRelative(.05f-vanilla,new Vec3(player.xxa,0,player.zza));
        }
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && worn(player)
                && ItemCharge.get(player.getItemBySlot(EquipmentSlot.FEET))>0)
            player.setDeltaMovement(player.getDeltaMovement().add(0,.275,0));
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void fall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && worn(player) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            float damage=Math.max(0,event.getAmount()/2-1);
            if (damage<1) event.setCanceled(true);else event.setAmount(damage);
        }
    }
}
