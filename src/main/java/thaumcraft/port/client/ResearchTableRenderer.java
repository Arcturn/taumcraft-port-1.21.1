package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.port.theory.*;

public final class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(BookAssets.id("research_table"),"main");
    public static final ModelResourceLocation QUILL=ModelResourceLocation.standalone(BookAssets.id("research/quill"));
    private final ModelPart model;
    public ResearchTableRenderer(BlockEntityRendererProvider.Context context){model=context.bakeLayer(LAYER);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("inkwell",CubeListBuilder.create().texOffs(0,16).addBox(0,0,0,3,2,3),PartPose.offset(-6,-2,3));
        root.addOrReplaceChild("scroll",CubeListBuilder.create().texOffs(0,0).addBox(-8,-.5f,0,8,2,2),PartPose.offsetAndRotation(-2,-2,2,0,10,0));
        root.addOrReplaceChild("ribbon",CubeListBuilder.create().texOffs(0,4).addBox(-4.25f,-.275f,0,1,2,2),PartPose.offsetAndRotation(-2,-2,2,0,10,0));
        // In BETA26 mirror was assigned AFTER addBox, so it did not mirror the constructed UVs.
        return LayerDefinition.create(mesh,64,32);
    }
    public void render(ResearchTableEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();ResearchTableGeometry.tablePose(pose,table.getBlockState().getValue(ResearchTableBlock.FACING));
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(BookAssets.id("textures/blocks/research_table_model.png")));
        if(table.active()){model.getChild("scroll").render(pose,out,light,overlay);pose.pushPose();pose.scale(1.2f,1.2f,1.2f);model.getChild("ribbon").render(pose,out,light,overlay,0xff000000|thaumcraft.port.aspect.Aspect.ALCHEMY.color());pose.popPose();}
        if(table.getItem(0).is(thaumcraft.port.content.PortContent.SCRIBING_TOOLS.get())){
            model.getChild("inkwell").render(pose,buffers.getBuffer(RenderType.entityTranslucent(BookAssets.id("textures/blocks/research_table_model.png"))),light,overlay);
            pose.pushPose();ResearchTableGeometry.quillPose(pose);
            // The original quill extrudes backwards from z=0 and reverses U; a generated item does neither.
            var sprite=Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(BookAssets.id("research/quill"));
            ResearchTableGeometry.quill(pose,buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),
                    sprite.getU0(),sprite.getU1(),sprite.getV0(),sprite.getV1(),light,overlay);
            pose.popPose();
        }pose.popPose();
    }
}
