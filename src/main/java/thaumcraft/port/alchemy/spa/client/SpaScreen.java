package thaumcraft.port.alchemy.spa.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import thaumcraft.port.alchemy.spa.SpaMenu;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.client.BookAssets;

public final class SpaScreen extends AbstractContainerScreen<SpaMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/gui/gui_spa.png");
    public SpaScreen(SpaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.setColor(1, 1, 1, 1);
        BookAssets.sprite(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        BookAssets.sprite(graphics, TEXTURE, leftPos + 89, topPos + 35, 208, menu.mixing() ? 16 : 32, 8, 8);
        var fluid = menu.fluid();
        if (!fluid.isEmpty()) {
            var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture = extensions.getStillTexture(fluid);
            if (texture != null) {
                var sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
                int color = extensions.getTintColor(fluid);
                // Original GuiSpa draws six complete 16px sprites at half scale, then masks the empty area.
                for (int row = 0; row < 6; row++) graphics.blit(leftPos + 107, topPos + 15 + row * 8, 0, 8, 8, sprite,
                        ((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 1);
                BookAssets.sprite(graphics, TEXTURE, leftPos + 107, topPos + 15, 107, 15, 10, SpaLayout.coveredPixels(fluid.getAmount()));
            }
        }
        BookAssets.sprite(graphics, TEXTURE, leftPos + 106, topPos + 11, 232, 0, 10, 55);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        double x = mouseX - leftPos, y = mouseY - topPos;
        if (SpaLayout.toggleTooltip(x, y)) graphics.renderTooltip(font,
                Component.translatable("text.spa.mix." + menu.mixing()), mouseX, mouseY);
        var fluid = menu.fluid();
        if (SpaLayout.fluidTooltip(x, y) && !fluid.isEmpty()) graphics.renderTooltip(font,
                List.of(fluid.getHoverName(), Component.literal(fluid.getAmount() + " mb")), Optional.empty(), mouseX, mouseY);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SpaLayout.toggle(mouseX - leftPos, mouseY - topPos) && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(PortSounds.get("clack"), 1, .4f));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
