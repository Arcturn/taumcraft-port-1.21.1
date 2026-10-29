package thaumcraft.port.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.essentia.SmelterMenu;

public final class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_smelter.png");
    public SmelterScreen(SmelterMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        g.setColor(1,1,1,1);BookAssets.sprite(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int n=net.minecraft.util.Mth.clamp(menu.data.get(0)*20/(menu.data.get(1)==0?200:menu.data.get(1)),0,20);if(n>0)BookAssets.sprite(g,TEXTURE,leftPos+80,topPos+46-n,176,20-n,16,n);
        n=net.minecraft.util.Mth.clamp(menu.data.get(2)*46/Math.max(1,menu.data.get(3)),0,46);if(n>0)BookAssets.sprite(g,TEXTURE,leftPos+106,topPos+59-n,216,46-n,9,n);
        n=net.minecraft.util.Mth.clamp(menu.data.get(4)*48/256,0,48);if(n>0)BookAssets.sprite(g,TEXTURE,leftPos+61,topPos+60-n,200,48-n,8,n);BookAssets.sprite(g,TEXTURE,leftPos+60,topPos+8,232,0,10,55);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
