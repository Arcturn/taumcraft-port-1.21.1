package thaumcraft.port.golem.seal.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.port.golem.GolemContent;
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SealClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event){event.register(GolemContent.SEAL_MENU.get(),SealScreen::new);event.register(GolemContent.LOGISTICS_MENU.get(),LogisticsScreen::new);}
    private SealClient(){}
}
