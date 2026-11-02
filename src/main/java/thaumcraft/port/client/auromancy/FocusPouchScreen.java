package thaumcraft.port.client.auromancy;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.auromancy.item.FocusPouchMenu;

public final class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/gui/gui_focuspouch.png");
    public FocusPouchScreen(FocusPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 175; imageHeight = 232; inventoryLabelY = 139;
    }
    @Override protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // BETA26 draws only the locked-hotbar overlay in the foreground, without generic container labels.
        if (menu.lockedSlot() < 9) { gui.pose().pushPose(); gui.pose().translate(0,0,300);
            gui.blit(TEXTURE,8 + menu.lockedSlot() * 18,209,240,0,16,16); gui.pose().popPose(); }
    }
    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        super.render(gui, mouseX, mouseY, partial); renderTooltip(gui, mouseX, mouseY);
    }
}
