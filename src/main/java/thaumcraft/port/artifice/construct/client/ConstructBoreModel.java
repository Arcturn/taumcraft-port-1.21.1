package thaumcraft.port.artifice.construct.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import thaumcraft.port.artifice.construct.*;

/** BETA26 ModelArcaneBore: original boxes, pivots, hierarchy and 64x32 UVs.
 * Source mirror flags were set after box construction and do not mirror those existing boxes. */
public final class ConstructBoreModel extends EntityModel<ArcaneBoreEntity> {
    private final ModelPart root;
    public ConstructBoreModel(ModelPart root){this.root=root;}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var leg2=root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 1.570796f, 0.0f));
        var tripod=root.addOrReplaceChild("tripod",CubeListBuilder.create().texOffs(13,0).addBox(-1.5f, 0.0f, -1.5f, 3, 2, 3),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var leg3=root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 3.141593f, 0.0f));
        var leg4=root.addOrReplaceChild("leg4",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 4.712389f, 0.0f));
        var leg1=root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 0.0f, 0.0f));
        var base=root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(32,0).addBox(-3.0f, -6.0f, -3.0f, 6, 6, 6),PartPose.offsetAndRotation(0.0f, 13.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var crystal=base.addOrReplaceChild("crystal",CubeListBuilder.create().texOffs(32,25).addBox(-1.0f, -4.0f, 5.0f, 2, 2, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var domebase=base.addOrReplaceChild("domebase",CubeListBuilder.create().texOffs(32,19).addBox(-2.0f, -5.0f, 3.0f, 4, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var dome=base.addOrReplaceChild("dome",CubeListBuilder.create().texOffs(44,16).addBox(-2.0f, -5.0f, 4.0f, 4, 4, 4),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var magbase=base.addOrReplaceChild("magbase",CubeListBuilder.create().texOffs(0,18).addBox(-1.0f, -4.0f, -6.0f, 2, 2, 3),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var tip=base.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(0,9).addBox(-1.5f, 0.0f, -1.5f, 3, 3, 3),PartPose.offsetAndRotation(0.0f, -3.0f, -6.0f, -1.570796f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(ArcaneBoreEntity entity,float walk,float amount,float age,float yaw,float pitch){
        var body=root.getChild("base");body.yRot=yaw/57.295776f;body.xRot=pitch/57.295776f;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color){root.render(pose,consumer,light,overlay,color);}
}
