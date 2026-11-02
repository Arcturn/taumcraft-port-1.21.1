package thaumcraft.port.artifice.tool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.infusion.structure.RechargeableItem;

/** One vis launches a hook; the tip and spool are crafting components, not consumable ammunition. */
public final class GrappleGunItem extends Item implements RechargeableItem {
    public GrappleGunItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public int maxCharge(ItemStack stack,LivingEntity wearer){return 100;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);tooltip.add(Component.translatable("tc.charge").append(" "+ItemCharge.get(stack)+" / "+maxCharge(stack,null)).withStyle(ChatFormatting.YELLOW));}
    public static boolean loaded(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean("grapple_loaded");}
    private static void loaded(ItemStack stack,boolean loaded){var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(loaded)tag.putBoolean("grapple_loaded",true);else tag.remove("grapple_loaded");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity owner,int slot,boolean selected){if(!level.isClientSide&&loaded(stack)&&GrappleEntity.active(owner)==null)loaded(stack,false);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        level.playSound(player,player.blockPosition(),PortSounds.get("ice"),SoundSource.PLAYERS,3,.8f+level.random.nextFloat()*.1f);
        if(!level.isClientSide&&ItemCharge.get(stack)>0){
            var hook=new GrappleEntity(level,player,hand);hook.shootFromRotation(player,player.getXRot(),player.getYRot(),-5,1.5f,0);
            int side=(hand==InteractionHand.MAIN_HAND?1:-1)*(player.getMainArm()==HumanoidArm.RIGHT?1:-1);
            double yaw=Math.toRadians(player.getYRot()-.5);var look=player.getLookAngle();
            hook.setPos(hook.position().add(-Math.cos(yaw)*.2*side+look.x,0,-Math.sin(yaw)*.3*side+look.z));
            if(level.addFreshEntity(hook)){hook.activate();ItemCharge.consume(stack,player,1);loaded(stack,true);}
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged){return oldStack.getItem()!=newStack.getItem()||loaded(oldStack)!=loaded(newStack);}
}
