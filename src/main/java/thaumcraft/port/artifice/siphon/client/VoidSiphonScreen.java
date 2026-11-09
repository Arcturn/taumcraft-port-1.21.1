package thaumcraft.port.artifice.siphon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.artifice.siphon.VoidSiphonMenu;

/** BETA26's original, deliberately unlabelled single-output inventory. */
public final class VoidSiphonScreen extends AbstractContainerScreen<VoidSiphonMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_void_siphon.png");
    public VoidSiphonScreen(VoidSiphonMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){gui.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);}
    @Override protected void renderLabels(GuiGraphics gui,int mouseX,int mouseY){}
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){super.render(gui,mouseX,mouseY,partial);renderTooltip(gui,mouseX,mouseY);}
}
