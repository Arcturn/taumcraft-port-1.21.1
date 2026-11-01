package thaumcraft.port.alchemy.spa.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.port.alchemy.spa.SpaContent;

@EventBusSubscriber(modid = "thaumcraft", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpaClientEvents {
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event) { event.register(SpaContent.MENU.get(), SpaScreen::new); }
    private SpaClientEvents() {}
}
