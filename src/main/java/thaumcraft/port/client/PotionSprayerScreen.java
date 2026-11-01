package thaumcraft.port.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.alchemy.devices.PotionSprayerMenu;

/** BETA26's 192×233 GUI: original potion gauge, ten ingredient icons and vertical progress bars. */
public final class PotionSprayerScreen extends AbstractContainerScreen<PotionSprayerMenu> {
    private static final ResourceLocation TEXTURE=BookAssets.id("textures/gui/gui_potion_sprayer.png");
    public PotionSprayerScreen(PotionSprayerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=192;imageHeight=233;}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        graphics.setColor(1,1,1,1);BookAssets.sprite(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.charges()>0){
            int color=menu.color();graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            int scroll=minecraft.player==null?0:minecraft.player.tickCount%256;
            int height=menu.charges()*9,first=Math.min(height,256-scroll),y=topPos+36+(8-menu.charges())*9;
            BookAssets.sprite(graphics,TEXTURE,leftPos+128,y,232,scroll,8,first);
            if(first<height)BookAssets.sprite(graphics,TEXTURE,leftPos+128,y+first,232,0,8,height-first);
            graphics.setColor(1,1,1,1);
        }
        BookAssets.sprite(graphics,TEXTURE,leftPos+125,topPos+28,205,28,14,88);
        var aspects=menu.recipe();
        for(int index=0;index<aspects.size();index++){
            var aspect=aspects.get(index);int col=index%2,row=index/2;
            graphics.setColor(.2f,.2f,.2f,1);BookAssets.sprite(graphics,TEXTURE,leftPos+96+22*col,topPos+32+16*row,192,56,2,14);
            int height=menu.progress(aspect)*14/Math.max(1,menu.required(aspect)),color=aspect.color();
            graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            if(height>0)BookAssets.sprite(graphics,TEXTURE,leftPos+96+22*col,topPos+46+16*row-height,192,56,2,height);
            graphics.setColor(1,1,1,1);
            int x=leftPos+79+22*col,y=topPos+31+16*row;ResearchPageScreen.aspect(graphics,aspect,x,y,16,true);
            graphics.pose().pushPose();graphics.pose().translate(x,y,100);graphics.pose().scale(.5f,.5f,1);
            String amount=Integer.toString(menu.required(aspect));int tx=32-font.width(amount),ty=32-font.lineHeight;
            graphics.drawString(font,amount,tx-1,ty,0,false);graphics.drawString(font,amount,tx+1,ty,0,false);
            graphics.drawString(font,amount,tx,ty-1,0,false);graphics.drawString(font,amount,tx,ty+1,0,false);graphics.drawString(font,amount,tx,ty,0xffffff,false);
            graphics.pose().popPose();
        }
        graphics.setColor(1,1,1,1);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
