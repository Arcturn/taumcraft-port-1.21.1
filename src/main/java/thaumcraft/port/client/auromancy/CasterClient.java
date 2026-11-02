package thaumcraft.port.client.auromancy;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import thaumcraft.port.auromancy.item.*;
import thaumcraft.port.content.CasterItem;

@EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CasterClient {
    public static final KeyMapping FOCUS = new KeyMapping("key.thaumcraft.focus", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, "key.categories.thaumcraft");
    public static final KeyMapping MISC = new KeyMapping("key.thaumcraft.caster_misc", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.thaumcraft");
    public static int auraBase;
    public static float auraVis;
    private static final ResourceLocation HUD = ResourceLocation.parse("thaumcraft:textures/gui/hud.png");
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(FOCUS); event.register(MISC); }
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event) { event.register(CasterContent.POUCH_MENU.get(), FocusPouchScreen::new); }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) if (item instanceof FocusItem focus)
            event.register((stack, tint) -> 0xff000000 | focus.getFocusColor(stack), item);
    }
    @SubscribeEvent public static void hud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.parse("thaumcraft:caster"), (gui, timer) -> renderHud(gui));
    }
    private static void renderHud(GuiGraphics gui) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.options.hideGui) return;
        var hand = CasterManager.heldCasterHand(mc.player); if (hand == null) return;
        var caster = mc.player.getItemInHand(hand); var focus = CasterItem.getFocusStack(caster); var picked = CasterItem.getPickedBlock(caster);
        gui.pose().pushPose(); gui.pose().scale(.5f, .5f, 1);
        gui.blit(HUD, 0, 0, 0, 0, 64, 64);
        int height = Math.max(0, (int)(30 * (int)auraVis / Math.max(1, auraBase)));
        int energy = thaumcraft.port.aspect.Aspect.ENERGY.color();
        gui.setColor((energy>>16&255)/255f,(energy>>8&255)/255f,(energy&255)/255f,.8f);
        gui.blit(HUD, 60, 47 - height, 104, 0, 8, height);
        gui.setColor(1,1,1,1); gui.blit(HUD, 56, 9, 72, 0, 16, 42); gui.pose().popPose();
        gui.renderItem(picked.isEmpty() ? focus : picked, 8, 8);
        if (!picked.isEmpty()) {
            int count = mc.player.getInventory().items.stream().filter(stack -> net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack, picked)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
            gui.renderItemDecorations(mc.font, picked, 8, 8, Integer.toString(count));
        }
        if (mc.player.isShiftKeyDown()) {
            gui.drawString(mc.font, String.format(java.util.Locale.ROOT, "%.1f", auraVis), 2, 35, 0xffffff);
            var pack = FocusItem.getPackage(focus);
            if (pack != null) gui.drawString(mc.font, String.format(java.util.Locale.ROOT, "%.2f", pack.getVisCost() * ((CasterItem)caster.getItem()).getConsumptionModifier(caster, mc.player, false)), 2, 45, 0x80ffff);
        }
        float cooldown = CasterManager.getCooldown(mc.player);
        if (cooldown > 0) gui.drawString(mc.font, String.format(java.util.Locale.ROOT, "%.1f", cooldown), 8, 25, 0xffffff);
    }
    @EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
    public static final class RuntimeEvents {
        @SubscribeEvent public static void key(InputEvent.Key event) {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || event.getAction() == GLFW.GLFW_RELEASE
                    || CasterManager.heldCasterHand(mc.player) == null || !FOCUS.matches(event.getKey(), event.getScanCode())
                    || !mc.options.keySwapOffhand.matches(event.getKey(), event.getScanCode())) return;
            // Modern NeoForge dispatches a shared binding to both keys. Preserve TC6's F focus selector
            // without sending a vanilla hand-swap packet before the selected-hand focus request.
            while (mc.options.keySwapOffhand.consumeClick()) { }
        }
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance(); if (mc.player == null || mc.screen != null) return;
            InteractionHand hand = CasterManager.heldCasterHand(mc.player); if (hand == null) return;
            while (FOCUS.consumeClick()) {
                if (Screen.hasShiftDown()) PacketDistributor.sendToServer(new CasterPayloads.Action(hand.ordinal(), 0, "REMOVE"));
                else mc.setScreen(new FocusSelectionScreen(hand));
            }
            while (MISC.consumeClick()) PacketDistributor.sendToServer(new CasterPayloads.Action(hand.ordinal(), Screen.hasControlDown() ? 2 : Screen.hasShiftDown() ? 3 : 1, ""));
        }
    }
    private CasterClient() { }
}
