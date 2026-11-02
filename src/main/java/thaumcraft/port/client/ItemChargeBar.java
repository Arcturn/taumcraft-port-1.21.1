package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import thaumcraft.port.infusion.structure.ItemCharge;
import thaumcraft.port.infusion.structure.RechargeableItem;

/** Inventory/hotbar charge meter; keeps the actual durability meter on damaged equipment. */
@EventBusSubscriber(modid="thaumcraft", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ItemChargeBar {
    @SubscribeEvent public static void register(RegisterItemDecorationsEvent event) {
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof RechargeableItem)) continue;
            event.register(item, (graphics, font, stack, x, y) -> {
                float charge = ItemCharge.fraction(stack, Minecraft.getInstance().player);
                int width = Math.round(13 * Math.max(0, charge));
                int top = y + (stack.isBarVisible() ? 10 : 13);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x + 2, top, x + 15, top + 2, 0xff000000);
                // The empty track remains visible, distinguishing an empty battery from no battery.
                graphics.fill(x + 2, top, x + 15, top + 1, 0xff173744);
                if (width > 0) graphics.fill(x + 2, top, x + 2 + width, top + 1,
                        charge <= .1f ? 0xffff6959 : 0xff45d9ff);
                graphics.pose().popPose();
                return false;
            });
        }
    }
    private ItemChargeBar() {}
}
