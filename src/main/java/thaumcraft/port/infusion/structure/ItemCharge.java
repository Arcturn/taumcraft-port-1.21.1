package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;

/** Shared TC6 RechargeHelper semantics, persisted and synchronized through a data component. */
public final class ItemCharge {
    public static int get(ItemStack stack) {
        return supported(stack) ? stack.getOrDefault(StructureContent.CHARGE.get(), 0) : -1;
    }
    public static boolean consume(ItemStack stack, LivingEntity wearer, int amount) {
        if (amount < 0 || !supported(stack) || !stack.has(StructureContent.CHARGE.get()) || get(stack) < amount) return false;
        stack.set(StructureContent.CHARGE.get(), get(stack) - amount);
        return true;
    }
    public static int add(ItemStack stack, LivingEntity wearer, int amount) {
        if (!supported(stack) || amount <= 0) return 0;
        int accepted = Math.min(amount, Math.max(0, ((RechargeableItem)stack.getItem()).maxCharge(stack, wearer) - get(stack)));
        if (accepted > 0) stack.set(StructureContent.CHARGE.get(), get(stack) + accepted);
        return accepted;
    }
    public static int recharge(ServerLevel level, ItemStack stack, BlockPos pos, Player player, int amount) {
        if (!supported(stack) || amount <= 0) return 0;
        var aura = AuraSystem.get(level, pos);
        if (aura == null) return 0;
        if (player != null && ResearchStorage.get(level.getServer()).player(player.getUUID()).knows("AURAPRESERVE")
                && aura.vis() / aura.base() < .1f) return 0;
        int requested = Math.min(amount, Math.max(0, ((RechargeableItem)stack.getItem()).maxCharge(stack, player) - get(stack)));
        if (requested == 0) return 0;
        // The original also consumes a fractional final vis point without producing a whole charge.
        return add(stack, player, (int)AuraSystem.drain(level, pos, requested, false, false));
    }
    public static float fraction(ItemStack stack, LivingEntity wearer) {
        if (!supported(stack)) return -1;
        int maximum = maximum(stack, wearer);
        return maximum <= 0 ? 0 : Math.clamp((float)get(stack) / maximum, 0, 1);
    }
    public static int maximum(ItemStack stack, LivingEntity wearer) {
        return supported(stack) ? Math.max(0, ((RechargeableItem)stack.getItem()).maxCharge(stack, wearer)) : 0;
    }
    private static boolean supported(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getItem() instanceof RechargeableItem; }
    private ItemCharge() {}
}
