package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.port.machine.CrucibleEntity;

public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleEntity> {
    public CrucibleRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(CrucibleEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(be.state.water()<=0)return;
        var sprite=Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(ResourceLocation.withDefaultNamespace("block/water_still"));
        float base=.3f+.5f*be.state.water()/1000f,y=Math.min(.999f,base+be.state.total()/500f*(1-base));
        int color=be.getLevel()==null?0x3f76e4:BiomeColors.getAverageWaterColor(be.getLevel(),be.getBlockPos());
        var out=buffers.getBuffer(RenderType.translucent());
        float[][] vertices={{.126f,y,.126f,.126f,.126f},{.126f,y,.874f,.126f,.874f},{.874f,y,.874f,.874f,.874f},{.874f,y,.126f,.874f,.126f}};
        for(var v:vertices)out.addVertex(pose.last().pose(),v[0],v[1],v[2]).setColor((color>>16)&255,(color>>8)&255,color&255,210)
            .setUv(sprite.getU(v[3]),sprite.getV(v[4])).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,1,0);
    }
}
