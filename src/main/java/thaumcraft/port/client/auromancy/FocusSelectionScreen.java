package thaumcraft.port.client.auromancy;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.auromancy.item.*;
import thaumcraft.port.content.CasterItem;

/** Hold F, point at a focus, and release. The selected hand is part of the server request. */
public final class FocusSelectionScreen extends Screen {
    private static final ResourceLocation RING = ResourceLocation.parse("thaumcraft:textures/misc/radial.png");
    private static final ResourceLocation RING_2 = ResourceLocation.parse("thaumcraft:textures/misc/radial2.png");
    private final InteractionHand hand;
    private final long opened = System.nanoTime();
    private List<Map.Entry<String, ItemStack>> choices = List.of();
    private int hovered = -1;
    private float[] hoverScale = new float[0];
    private long previousFrame = System.nanoTime(), closing;
    public FocusSelectionScreen(InteractionHand hand) { super(Component.translatable("key.thaumcraft.focus")); this.hand = hand; }
    @Override protected void init() { choices = new ArrayList<>(CasterManager.getAvailableFoci(minecraft.player).entrySet()); hoverScale = new float[choices.size()]; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if (closing != 0) { if (System.nanoTime() - closing >= 150_000_000L) onClose(); return; }
        if (!(minecraft.player.getItemInHand(hand).getItem() instanceof CasterItem)) { onClose(); return; }
        var key = CasterClient.FOCUS.getKey();
        boolean down = key.getType() == InputConstants.Type.MOUSE ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), key.getValue()) == 1
                : InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
        if (!down) select();
    }
    private void select() {
        if (closing != 0) return;
        if (hovered >= 0 && hovered < choices.size()) PacketDistributor.sendToServer(new CasterPayloads.Action(hand.ordinal(), 0, choices.get(hovered).getKey()));
        closing = System.nanoTime();
    }
    @Override public boolean mouseClicked(double x, double y, int button) { if (button == 0) { select(); return true; } return super.mouseClicked(x, y, button); }
    @Override public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partial) { }
    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime(); float elapsed = Math.min(.1f, (now - previousFrame) / 1_000_000_000f); previousFrame = now;
        float scale = closing == 0 ? Math.clamp((now - opened) / 150_000_000f, 0, 1) : 1 - Math.clamp((now - closing) / 150_000_000f, 0, 1);
        int cx = width / 2, cy = height / 2; float radius = 16 + choices.size() * 2.5f;
        ring(gui, RING, cx, cy, radius * 2.75f * scale, (minecraft.player.tickCount + partial) / 2);
        ring(gui, RING_2, cx, cy, radius * 2.55f * scale, -(minecraft.player.tickCount + partial) / 2);
        gui.renderItem(CasterItem.getFocusStack(minecraft.player.getItemInHand(hand)), cx - 8, cy - 8);
        hovered = -1; double closest = 18 * 18;
        for (int i = 0; i < choices.size(); i++) {
            double angle = Math.toRadians(-90 + i * 360.0 / choices.size());
            int x = cx + (int)(Math.cos(angle) * radius * scale), y = cy + (int)(Math.sin(angle) * radius * scale);
            double distance = (mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y);
            if (distance < closest) { hovered = i; closest = distance; }
        }
        for (int i = 0; i < choices.size(); i++) {
            double angle = Math.toRadians(-90 + i * 360.0 / choices.size());
            float x = cx + (float)Math.cos(angle) * radius * scale, y = cy + (float)Math.sin(angle) * radius * scale;
            gui.pose().pushPose(); gui.pose().translate(x, y, 0); hoverScale[i] = Math.clamp(hoverScale[i] + elapsed * (i == hovered ? 1/.15f : -1/.25f), 0, 1);
            float zoom = 1 + .3f * hoverScale[i]; gui.pose().scale(zoom * scale, zoom * scale, 1);
            gui.renderItem(choices.get(i).getValue(), -8, -8); gui.pose().popPose();
        }
        if (hovered >= 0) gui.renderTooltip(font, choices.get(hovered).getValue(), mouseX, mouseY);
        else if (choices.isEmpty()) gui.drawCenteredString(font, Component.translatable("thaumcraft.focus.none"), cx, cy + 30, 0xffffff);
    }
    private static void ring(GuiGraphics gui, ResourceLocation texture, float x, float y, float size, float rotation) {
        gui.pose().pushPose(); gui.pose().translate(x, y, 0); gui.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        gui.setColor(.5f, .5f, .5f, .5f); int width = (int)size; gui.blit(texture, -width / 2, -width / 2, width, width, 0, 0, 256, 256, 256, 256);
        gui.setColor(1, 1, 1, 1); gui.pose().popPose();
    }
}
