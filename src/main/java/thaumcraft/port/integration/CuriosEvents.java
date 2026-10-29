package thaumcraft.port.integration;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import thaumcraft.port.content.PortContent;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosEvents {
    private CuriosEvents() {}

    /** Migrated from Thaumcraft PlayerEvents.onDeath; keeps the original effect durations. */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.findFirstCurio(PortContent.CHARM_UNDYING.get()).ifPresent(found -> {
                    var slot = found.slotContext();
                    inventory.getStacksHandler(slot.identifier()).ifPresent(handler -> {
                        var consumed = handler.getStacks().extractItem(slot.index(), 1, false);
                        if (consumed.isEmpty()) return;
                        player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
                        CriteriaTriggers.USED_TOTEM.trigger(player, consumed);
                        player.setHealth(1.0f);
                        player.removeAllEffects();
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                        player.level().broadcastEntityEvent(player, (byte) 35);
                        event.setCanceled(true);
                    });
                }));
    }
}
