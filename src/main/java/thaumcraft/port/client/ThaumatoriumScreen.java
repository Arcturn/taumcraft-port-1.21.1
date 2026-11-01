package thaumcraft.port.client;

import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.ThaumatoriumMenu;
import thaumcraft.port.essentia.ThaumatoriumSelectPayload;

/** TC6 artwork, recipe window, rotating aspect preview and individual essentia bars. */
public final class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    private static final ResourceLocation TEXTURE = BookAssets.id("textures/gui/gui_thaumatorium.png");
    private int row;
    private List<ThaumatoriumMenu.RecipeView> displayed = List.of();

    public ThaumatoriumScreen(ThaumatoriumMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 175;
        imageHeight = 216;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        // TC6 sorted output names in the client language; the server cannot choose the Russian order.
        displayed = menu.catalystPresent ? menu.recipes.stream().sorted(Comparator
                .comparing((ThaumatoriumMenu.RecipeView recipe) -> recipe.output().getHoverName().getString())
                .thenComparing(recipe -> recipe.id().toString())).toList() : menu.recipes;
        row = ThaumatoriumLayout.clampRow(row, displayed.size());
        long time = System.currentTimeMillis();
        BookAssets.sprite(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (row > 0) BookAssets.sprite(graphics, TEXTURE, leftPos + 82, topPos + 56, 176, 56, 8, 11);
        if (row < ThaumatoriumLayout.maxRow(displayed.size()))
            BookAssets.sprite(graphics, TEXTURE, leftPos + 82, topPos + 93, 176, 93, 8, 11);
        ResourceLocation preview = menu.selected.isEmpty() ? null
                : menu.selected.get(ThaumatoriumLayout.preview(time, menu.selected.size()));
        for (var recipe : displayed) if (recipe.id().equals(preview)) {
            drawAspects(graphics, recipe);
            break;
        }
        if (menu.capacity > 1) {
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + 64, topPos + 48, 0);
            graphics.pose().scale(.5f, .5f, 1);
            String count = menu.selected.size() + "/" + menu.capacity;
            graphics.drawString(font, count, -font.width(count) / 2, 0, 0xffffff, false);
            graphics.pose().popPose();
        }
        for (int i = 0; i < 6 && row * 2 + i < displayed.size(); i++) {
            var recipe = displayed.get(row * 2 + i);
            int x = leftPos + 48 + i % 2 * 16, y = topPos + 56 + i / 2 * 16;
            if (menu.selected.contains(recipe.id())) {
                graphics.setColor(1, 1, 1, .7f);
                BookAssets.sprite(graphics, TEXTURE, x, y, 176, 8, 16, 16);
                if (menu.selected.size() > 1 && recipe.id().equals(preview))
                    BookAssets.sprite(graphics, TEXTURE, x, y, 176, 8, 16, 16);
                graphics.setColor(1, 1, 1, 1);
            }
            graphics.renderItem(recipe.output(), x, y);
            graphics.renderItemDecorations(font, recipe.output(), x, y);
        }
    }

    private void drawAspects(GuiGraphics graphics, ThaumatoriumMenu.RecipeView recipe) {
        var aspects = recipe.aspects().keySet().stream().sorted(Comparator.comparing(Aspect::displayName)).limit(8).toList();
        for (int i = 0; i < aspects.size(); i++) {
            Aspect aspect = aspects.get(i);
            int x = leftPos + 98 + i % 2 * 16, y = topPos + 40 + i / 2 * 20;
            BookAssets.sprite(graphics, TEXTURE, x, y, 176, 4, 12, 3);
            int color = aspect.color();
            graphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
            int progress = ThaumatoriumLayout.progress(menu.contents.getOrDefault(aspect, 0), recipe.aspects().get(aspect));
            if (progress > 0) BookAssets.sprite(graphics, TEXTURE, x, y, 176, 0, progress, 3);
            graphics.setColor(1, 1, 1, 1);
        }
        for (int i = 0; i < aspects.size(); i++) {
            Aspect aspect = aspects.get(i);
            int x = leftPos + 96 + i % 2 * 16, y = topPos + 24 + i / 2 * 20;
            ResearchPageScreen.aspect(graphics, aspect, x, y, 16, true);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 100);
            graphics.pose().scale(.5f, .5f, 1);
            String amount = Integer.toString(recipe.aspects().get(aspect));
            int tx = 32 - font.width(amount), ty = 32 - font.lineHeight;
            graphics.drawString(font, amount, tx - 1, ty, 0, false);
            graphics.drawString(font, amount, tx + 1, ty, 0, false);
            graphics.drawString(font, amount, tx, ty - 1, 0, false);
            graphics.drawString(font, amount, tx, ty + 1, 0, false);
            graphics.drawString(font, amount, tx, ty, 0xffffff, false);
            graphics.pose().popPose();
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos, y = mouseY - topPos;
        int index = ThaumatoriumLayout.recipeAt(x, y, row, displayed.size());
        if (index >= 0 && menu.revision >= 0) {
            PacketDistributor.sendToServer(new ThaumatoriumSelectPayload(menu.containerId, menu.token,
                    menu.revision, displayed.get(index).id()));
            ResearchScreen.sound("hhon", .3f, 1);
            return true;
        }
        if (row > 0 && ThaumatoriumLayout.inside(x, y, 82, 56, 8, 11)) {
            row--;
            ResearchScreen.sound("clack", .4f, 1);
            return true;
        }
        if (row < ThaumatoriumLayout.maxRow(displayed.size()) && ThaumatoriumLayout.inside(x, y, 82, 93, 8, 11)) {
            row++;
            ResearchScreen.sound("clack", .4f, 1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
        int index = ThaumatoriumLayout.recipeAt(mouseX - leftPos, mouseY - topPos, row, displayed.size());
        if (index >= 0) graphics.renderTooltip(font, displayed.get(index).output(), mouseX, mouseY);
    }
}
