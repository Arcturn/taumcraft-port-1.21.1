package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.port.machine.WorkbenchMenu;

/** TC6 artwork, 190x234 canvas, original slot layout and crystal-cost animation. */
public final class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private static final ResourceLocation TEXTURE=BookAssets.id("textures/gui/arcaneworkbench.png");
    public WorkbenchScreen(WorkbenchMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=190;imageHeight=234;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.setColor(1,1,1,1);BookAssets.sprite(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        g.flush();RenderSystem.enableBlend();RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
        for(int i=0;i<6;i++)if(menu.crystalCost(i)>0){
            int color=WorkbenchMenu.PRIMALS.get(i).color();g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,.33f);
            g.pose().pushPose();g.pose().translate(leftPos+WorkbenchMenu.crystalX(i)+7.5f,topPos+WorkbenchMenu.crystalY(i)+8,0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(WorkbenchMenu.shardId(i)*60+minecraft.player.tickCount%360+partial));g.pose().scale(.5f,.5f,1);
            g.blit(TEXTURE,-32,-32,192,0,64,64,256,256);g.pose().popPose();
        }
        g.setColor(1,1,1,1);RenderSystem.defaultBlendFunc();
        label(g,Component.literal(menu.availableVis()+" ").append(Component.translatable("workbench.available")),46,menu.availableVis()<menu.visCost()?0xee6e6e:0x6e6eee);
        if(menu.visCost()>0){
            label(g,Component.literal(menu.visCost()+" ").append(Component.translatable("workbench.cost")),38,0xc0ffff);
            var preview=menu.unavailablePreview();if(!preview.isEmpty()){
                g.setColor(.33f,.33f,.33f,.66f);g.renderFakeItem(preview,leftPos+160,topPos+64);g.setColor(1,1,1,1);
            }
        }
    }
    private void label(GuiGraphics g,Component text,int y,int color){
        g.pose().pushPose();g.pose().translate(leftPos+168,topPos+y,0);g.pose().scale(.5f,.5f,1);
        g.drawString(font,text,-font.width(text)/2,0,color,false);g.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
