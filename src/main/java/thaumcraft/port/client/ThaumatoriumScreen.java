package thaumcraft.port.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.essentia.ThaumatoriumMenu;

/** Modern menu layout; icons and recipe decisions are synchronized by the server. */
public final class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    public ThaumatoriumScreen(ThaumatoriumMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=186;inventoryLabelY=92;}
    @Override protected void init(){super.init();for(int i=0;i<3;i++){final int button=i;addRenderableWidget(Button.builder(Component.literal(i==0?"<":i==1?">":"×"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button)).bounds(leftPos+84+i*18,topPos+72,16,16).build());}}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffc6c6c6);for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff373737);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xff8b8b8b);}for(int i=0;i<9;i++)if(menu.data.get(i)==1){var slot=menu.slots.get(i+1);g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0x8064c080);}}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,8,5,0x404040,false);g.drawString(font,playerInventoryTitle,8,92,0x404040,false);g.drawString(font,menu.data.get(9)+"/"+menu.data.get(10),140,34,0x404040,false);g.drawString(font,menu.data.get(14)+"/"+menu.data.get(11),8,58,0x404040,false);g.drawString(font,Component.translatable(menu.data.get(12)==1?"gui.thaumcraft.heated":"gui.thaumcraft.cold"),8,71,menu.data.get(12)==1?0x8b3200:0x404040,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
