package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Original 525-unit dial, animated vis/flux and crouching readout. */
final class ScannerAuraHud {
    private static final ResourceLocation HUD=BookAssets.id("textures/gui/hud.png");
    private static final java.text.DecimalFormat NUMBER=new java.text.DecimalFormat("#######.#");
    static void render(GuiGraphics g){
        var mc=Minecraft.getInstance();float base=Mth.clamp(ClientJournal.auraBase/525,0,1),vis=Mth.clamp(ClientJournal.auraVis/525,0,1),flux=Mth.clamp(ClientJournal.auraFlux/525,0,1);
        if(vis+flux>1){float m=1/(vis+flux);base*=m;vis*=m;flux*=m;}
        float partial=mc.getTimer().getGameTimeDeltaPartialTick(false),ticks=mc.player.tickCount+partial;
        RenderSystem.enableBlend();g.pose().pushPose();g.pose().translate(2,0,0);
        if(vis>0)bar(g,vis,10+(1-vis)*64,false,ticks,ClientJournal.auraVis);
        if(flux>0)bar(g,flux,10+(1-flux-vis)*64,true,mc.player.tickCount/3f+partial,ClientJournal.auraFlux);
        g.setColor(1,1,1,1);BookAssets.sprite(g,HUD,1,1,72,48,16,80);
        g.pose().pushPose();g.pose().translate(2,8+(1-base)*64,0);BookAssets.sprite(g,HUD,0,0,117,61,14,5);g.pose().popPose();g.pose().popPose();
    }
    private static void bar(GuiGraphics g,float value,float start,boolean flux,float ticks,float amount){
        g.pose().pushPose();g.pose().translate(5,start,0);g.pose().scale(1,value,1);g.setColor(flux?.25f:.7f,flux?.1f:.4f,flux?.3f:.9f,1);BookAssets.sprite(g,HUD,0,0,88,56,8,64);g.pose().popPose();
        g.flush();RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
        g.setColor(flux?.7f:1,flux?.4f:1,1,.5f);g.pose().pushPose();g.pose().translate(5,start,0);
        g.blit(HUD,0,0,flux?104:96,flux?120-ticks%64:56+ticks%64,8,Math.max(1,(int)(value*64)),256,256);g.pose().popPose();g.flush();RenderSystem.defaultBlendFunc();g.setColor(1,1,1,1);
        var mc=Minecraft.getInstance();if(mc.player.isCrouching()){g.pose().pushPose();g.pose().translate(16,start-(flux?4:0),0);g.pose().scale(.5f,.5f,1);g.drawString(mc.font,NUMBER.format(amount),0,0,flux?0xaa11bb:0xeeaaff,true);g.pose().popPose();}
    }
    private ScannerAuraHud(){}
}
