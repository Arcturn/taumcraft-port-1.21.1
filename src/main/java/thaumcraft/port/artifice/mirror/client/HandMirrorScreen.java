package thaumcraft.port.artifice.mirror.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.artifice.mirror.HandMirrorMenu;

public final class HandMirrorScreen extends AbstractContainerScreen<HandMirrorMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_handmirror.png");
    public HandMirrorScreen(HandMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y){
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.hand==InteractionHand.MAIN_HAND){graphics.pose().pushPose();graphics.pose().translate(0,0,300);graphics.blit(TEXTURE,leftPos+8+menu.selected*18,topPos+142,240,0,16,16);graphics.pose().popPose();}
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
    @Override protected boolean checkHotbarKeyPressed(int key,int scan){return false;}
}
