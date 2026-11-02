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
import thaumcraft.port.client.BookAssets;
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
    private boolean[] hovering = new boolean[0];
    private float closingScale;
    private long previousFrame = System.nanoTime(), closing;
    public FocusSelectionScreen(InteractionHand hand) { super(Component.translatable("key.thaumcraft.focus")); this.hand = hand; }
    @Override protected void init() { choices = new ArrayList<>(CasterManager.getAvailableFoci(minecraft.player).entrySet()); hoverScale = new float[choices.size()];Arrays.fill(hoverScale,1);hovering=new boolean[choices.size()]; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if(choices.isEmpty()){onClose();return;}
        if (closing != 0) { if (System.nanoTime() - closing >= closingScale*150_000_000L) onClose(); return; }
        if (!(minecraft.player.getItemInHand(hand).getItem() instanceof CasterItem)) { onClose(); return; }
        var key = CasterClient.FOCUS.getKey();
        boolean down = key.getType() == InputConstants.Type.MOUSE ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), key.getValue()) == 1
                : InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
        if (!down) select();
    }
    private void select() {
        if (closing != 0) return;
        if (hovered >= 0 && hovered < choices.size()) PacketDistributor.sendToServer(new CasterPayloads.Action(hand.ordinal(), 0, choices.get(hovered).getKey()));
        closing = System.nanoTime();closingScale=Math.clamp((closing-opened)/150_000_000f,0,1);
    }
    @Override public boolean mouseClicked(double x, double y, int button) { if (button == 0) { select(); return true; } return super.mouseClicked(x, y, button); }
    @Override public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partial) { }
    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime(); float elapsed = Math.min(.1f, (now - previousFrame) / 1_000_000_000f); previousFrame = now;
        if(choices.isEmpty())return;
        float scale = closing == 0 ? Math.clamp((now - opened) / 150_000_000f, 0, 1) : Math.max(0,closingScale-(now-closing)/150_000_000f);
        int cx = width / 2, cy = height / 2; float radius = 16 + choices.size() * 2.5f;
        float ringRotation=minecraft.player.tickCount%720/2f+partial;
        ring(gui, RING, cx, cy, radius * 2.75f * scale, ringRotation);
        ring(gui, RING_2, cx, cy, radius * 2.55f * scale, -ringRotation);
        var current= CasterItem.getFocusStack(minecraft.player.getItemInHand(hand));
        gui.renderItem(current,cx-8,cy-8);
        ItemStack tooltip=Math.abs(mouseX-cx)<=10&&Math.abs(mouseY-cy)<=10?current:ItemStack.EMPTY;
        if(closing==0)hovered=-1;
        for (int i = 0; i < choices.size(); i++) {
            double angle=Math.toRadians(-90*scale+i*360.0/choices.size());
            double dx=Math.cos(angle)*radius,dy=Math.sin(angle)*radius;
            if(closing==0){int mx=(int)(mouseX-cx-dx),my=(int)(mouseY-cy-dy);hovering[i]=mx>=-10&&mx<=10&&my>=-10&&my<=10;if(hovering[i]){hovered=i;tooltip=choices.get(i).getValue();}}
            gui.pose().pushPose(); gui.pose().translate(cx+(int)dx*scale,cy+(int)dy*scale,0);
            gui.pose().scale(hoverScale[i]*scale,hoverScale[i]*scale,1);
            gui.renderItem(choices.get(i).getValue(), -8, -8); gui.pose().popPose();
            // The source changes the actual 1.0..1.3 scale by dt/150 or dt/250,
            // so reaching the endpoint takes 45ms / 75ms, not 150ms / 250ms.
            hoverScale[i]=Math.clamp(hoverScale[i]+elapsed*(hovering[i]?1/.15f:-1/.25f),1,1.3f);
        }
        if(!tooltip.isEmpty())gui.renderTooltip(font,tooltip,-4,20);
    }
    private static void ring(GuiGraphics gui, ResourceLocation texture, float x, float y, float size, float rotation) {
        gui.pose().pushPose(); gui.pose().translate(x,y,0);
        // UtilsFX.renderQuadCentered rotates the texture's UV axes by +90 degrees.
        gui.pose().mulPose(Axis.ZP.rotationDegrees(rotation+90));gui.pose().scale(size/256,size/256,1);
        BookAssets.prepareImage(gui);gui.setColor(.5f,.5f,.5f,.5f);gui.blit(texture,-128,-128,256,256,0,0,256,256,256,256);
        gui.setColor(1, 1, 1, 1); gui.pose().popPose();
    }
}
