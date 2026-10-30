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
        // BETA26 used already-blue water pixels, without the biome water tint.
        var sprite=Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(ResourceLocation.fromNamespaceAndPath("thaumcraft","block/crucible_water"));
        float y=be.state.fluidHeight();
        var color=CrucibleVisualRules.fluidColour(be.state.total());
        var out=buffers.getBuffer(RenderType.translucent());
        // The original full-block plane rises above the rim when overfilled.
        // Below the rim the crucible walls naturally occlude its outer edge.
        float[][] vertices={{0,y,1,1,1},{1,y,1,0,1},{1,y,0,0,0},{0,y,0,1,0}};
        for(var v:vertices)out.addVertex(pose.last().pose(),v[0],v[1],v[2]).setColor(color.red(),color.green(),color.blue(),1)
            .setUv(sprite.getU(v[3]),sprite.getV(v[4])).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,1,0);
    }
}
