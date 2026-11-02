package thaumcraft.port.artifice.construct.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.port.artifice.construct.*;

/** Exact source 175x232 atlas, slots, health bar and advanced targeting toggles. */
public final class ConstructScreen extends AbstractContainerScreen<ConstructMenu>{
    private final ResourceLocation texture;
    public ConstructScreen(ConstructMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=175;imageHeight=232;texture=ResourceLocation.parse("thaumcraft:textures/gui/gui_"+(menu.construct() instanceof ArcaneBoreEntity?"arcanebore":advanced()?"turret_advanced":"turret_basic")+".png");}
    private boolean advanced(){return menu.construct() instanceof TurretEntity turret&&turret.isAdvanced();}
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY){
        gui.blit(texture,leftPos,topPos,0,0,imageWidth,imageHeight);var construct=menu.construct();if(construct==null)return;
        int health=(int)(39*construct.getHealth()/construct.getMaxHealth());gui.blit(texture,leftPos+(advanced()?30:68),topPos+59,192,48,health,6);
        if(construct instanceof TurretEntity turret&&turret.isAdvanced())for(int i=0;i<4;i++){
            int x=leftPos+90,y=topPos+13+i*14;gui.blit(texture,x,y,192,16,8,8);if(turret.targetFlag(i))gui.blit(texture,x,y,192,24,8,8);gui.drawString(font,Component.translatable("button.turretfocus."+(i+1)),x+12,y,0xffffff,true);
        }
        if(construct instanceof ArcaneBoreEntity bore){
            var stack=bore.getMainHandItem();if(!stack.isEmpty()&&stack.isDamageableItem()&&stack.getDamageValue()+1>=stack.getMaxDamage())gui.blit(texture,leftPos+80,topPos+29,240,0,16,16);
            gui.pose().pushPose();gui.pose().translate(leftPos+124,topPos+18,505);gui.pose().scale(.5f,.5f,1);
            gui.drawString(font,"Width: "+(1+bore.getDigRadius()*2),0,0,0xffffff,true);gui.drawString(font,"Depth: "+bore.getDigDepth(),64,0,0xffffff,true);gui.drawString(font,"Speed: +"+bore.getDigSpeed(Blocks.STONE.defaultBlockState()),0,10,0xffffff,true);
            int y=34;if(bore.getRefining()>0||bore.getFortune()>0||bore.hasSilkTouch())gui.drawString(font,"Other properties:",0,24,0xffffff,true);
            if(bore.getRefining()>0){gui.drawString(font,"Refining "+bore.getRefining(),4,y,0xc0c0c0,true);y+=9;}if(bore.getFortune()>0){gui.drawString(font,"Fortune "+bore.getFortune(),4,y,15648330,true);y+=9;}if(bore.hasSilkTouch())gui.drawString(font,"Silk Touch",4,y,0x8080ff,true);gui.pose().popPose();
        }
    }
    @Override protected void renderLabels(GuiGraphics gui,int x,int y){}
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){super.render(gui,mouseX,mouseY,partial);renderTooltip(gui,mouseX,mouseY);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(advanced()&&button==0&&x>=leftPos+90&&x<leftPos+98&&minecraft!=null&&minecraft.gameMode!=null)for(int i=0;i<4;i++)if(y>=topPos+13+i*14&&y<topPos+21+i*14){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i+1);return true;}
        return super.mouseClicked(x,y,button);
    }
}
