package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.infusion.bauble.*;

/** Original BETA26 five-part mesh, dye, wall mounting, cloth motion and aspect decal. */
public final class ThaumicBannerRenderer implements BlockEntityRenderer<ThaumicBannerEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.parse("thaumcraft:banner"),"main");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/banner_blank.png");
    private static final ResourceLocation CRIMSON=ResourceLocation.parse("thaumcraft:textures/models/banner_cultist.png");
    private final ModelPart model;
    public ThaumicBannerRenderer(BlockEntityRendererProvider.Context context){model=context.bakeLayer(LAYER);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        // Original ModelRenderer.mirror is set after addBox; the cube UVs are not mirrored.
        root.addOrReplaceChild("tab1",CubeListBuilder.create().texOffs(0,29).addBox(-5,-7.5f,-1.5f,2,3,3),PartPose.ZERO);
        root.addOrReplaceChild("tab2",CubeListBuilder.create().texOffs(0,29).addBox(3,-7.5f,-1.5f,2,3,3),PartPose.ZERO);
        root.addOrReplaceChild("beam",CubeListBuilder.create().texOffs(30,0).addBox(-7,-7,-1,14,2,2),PartPose.ZERO);
        root.addOrReplaceChild("cloth",CubeListBuilder.create().texOffs(0,0).addBox(-7,0,-.5f,14,28,1),PartPose.offset(0,-5,0));
        root.addOrReplaceChild("pole",CubeListBuilder.create().texOffs(62,0).addBox(0,0,-1,2,31,2),PartPose.offset(-1,-7,-2));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void render(ThaumicBannerEntity banner,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var state=banner.getBlockState();if(!(state.getBlock() instanceof ThaumicBannerBlock block))return;
        pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.mulPose(Axis.YP.rotationDegrees(180+state.getValue(ThaumicBannerBlock.ROTATION)*360/16f));
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(block.crimson?CRIMSON:TEXTURE));
        if(state.getValue(ThaumicBannerBlock.WALL))pose.translate(0,1,-.4125);else model.getChild("pole").render(pose,out,light,overlay);
        model.getChild("beam").render(pose,out,light,overlay);
        int color=block.crimson?-1:0xff000000|block.dye.getTextureDiffuseColor();
        model.getChild("tab1").render(pose,out,light,overlay,color);model.getChild("tab2").render(pose,out,light,overlay,color);
        var pos=banner.getBlockPos();var player=Minecraft.getInstance().player;
        float phase=pos.getX()*7+pos.getY()*9+pos.getZ()*13+(player==null?0:player.tickCount)+partial;
        float angle=.02f-(float)Math.sin(phase/11)*.02f;var cloth=model.getChild("cloth");cloth.xRot=angle;cloth.render(pose,out,light,overlay,color);
        if(!block.crimson&&banner.aspect()!=null){
            pose.translate(0,0,.05001);pose.scale(.0375f,.0375f,.0375f);pose.mulPose(Axis.YP.rotationDegrees(180));pose.mulPose(Axis.XP.rotation(-angle*2));
            var aspect=banner.aspect();var decal=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/aspects/"+aspect.id()+".png")));
            int tint=0xbf000000|aspect.color();float[][] vertices={{-8,0,0,0},{-8,16,0,1},{8,16,1,1},{8,0,1,0}};
            for(var vertex:vertices)decal.addVertex(pose.last().pose(),vertex[0],vertex[1],0).setColor(tint).setUv(vertex[2],vertex[3]).setOverlay(overlay).setLight(light).setNormal(pose.last(),0,0,1);
        }
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(ThaumicBannerEntity banner){return true;}
}
