package thaumcraft.port.auromancy.client.pech;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.auromancy.pech.PechMenu;
import thaumcraft.port.content.PortSounds;

/** Original GuiPech atlas and button coordinates, backed by the server's validated menu action. */
public final class PechScreen extends AbstractContainerScreen<PechMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_pech.png");
    public PechScreen(PechMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;}
    private boolean canTrade(){
        var pech=menu.pech();if(pech==null||menu.getSlot(0).getItem().isEmpty()||!pech.isValued(menu.getSlot(0).getItem()))return false;
        for(int slot=1;slot<=4;slot++)if(!menu.getSlot(slot).getItem().isEmpty())return false;
        return true;
    }
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){gui.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);if(canTrade())gui.blit(TEXTURE,leftPos+67,topPos+24,176,0,25,25);}
    @Override protected void renderLabels(GuiGraphics gui,int mouseX,int mouseY){ }
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){super.render(gui,mouseX,mouseY,partial);renderTooltip(gui,mouseX,mouseY);}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(mouseX>=leftPos+67&&mouseX<leftPos+92&&mouseY>=topPos+24&&mouseY<topPos+49&&canTrade()&&minecraft!=null&&minecraft.gameMode!=null){
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
            if(minecraft.player!=null)minecraft.player.playSound(PortSounds.get("pech_dice"),.5f,.95f+minecraft.player.getRandom().nextFloat()*.1f);
            return true;
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
}
