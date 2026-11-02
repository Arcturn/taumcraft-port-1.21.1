package thaumcraft.port.auromancy.firebat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.port.auromancy.firebat.FireBat;

/** ModelFireBat, retaining the original mirrored left ear/wing UVs and hanging pose. */
public final class FireBatModel extends EntityModel<FireBat> {
    private final ModelPart root,head,body,right,left;
    public FireBatModel(ModelPart root){this.root=root;head=root.getChild("head");body=root.getChild("body");right=body.getChild("right");left=body.getChild("left");}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-3,6,6,6),PartPose.ZERO);
        head.addOrReplaceChild("right_ear",CubeListBuilder.create().texOffs(24,0).addBox(-4,-6,-2,3,4,1),PartPose.ZERO);
        head.addOrReplaceChild("left_ear",CubeListBuilder.create().texOffs(24,0).mirror().addBox(1,-6,-2,3,4,1),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,16).addBox(-3,4,-3,6,12,6).texOffs(0,34).addBox(-5,16,0,10,6,1),PartPose.ZERO);
        var right=body.addOrReplaceChild("right",CubeListBuilder.create().texOffs(42,0).addBox(-12,1,1.5f,10,16,1),PartPose.ZERO);
        right.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).addBox(-8,1,0,8,12,1),PartPose.offset(-12,1,1.5f));
        var left=body.addOrReplaceChild("left",CubeListBuilder.create().texOffs(42,0).mirror().addBox(2,1,1.5f,10,16,1),PartPose.ZERO);
        left.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(24,16).mirror().addBox(0,1,0,8,12,1),PartPose.offset(12,1,1.5f));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(FireBat bat,float swing,float swingAmount,float age,float headYaw,float pitch){pose(bat.hanging(),age,headYaw,pitch);}
    void pose(boolean hanging,float age,float headYaw,float pitch){
        head.xRot=pitch/57.295776f;
        if(hanging){
            head.yRot=Mth.PI-headYaw/57.295776f;head.zRot=Mth.PI;head.setPos(0,-2,0);
            right.setPos(-3,0,3);left.setPos(3,0,3);body.xRot=Mth.PI;
            right.xRot=-.15707964f;right.yRot=-1.2566371f;right.getChild("outer").yRot=-1.7278761f;
            left.xRot=right.xRot;left.yRot=-right.yRot;left.getChild("outer").yRot=-right.getChild("outer").yRot;
        }else{
            head.yRot=headYaw/57.295776f;head.zRot=0;head.setPos(0,0,0);right.setPos(0,0,0);left.setPos(0,0,0);
            body.xRot=.7853982f+Mth.cos(age*.1f)*.15f;body.yRot=0;
            // The source leaves the small x rotation from the hanging pose on subsequent flight frames.
            right.yRot=Mth.cos(age*1.3f)*Mth.PI*.25f;left.yRot=-right.yRot;
            right.getChild("outer").yRot=right.yRot*.5f;left.getChild("outer").yRot=-right.yRot*.5f;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){root.render(pose,buffer,light,overlay,color);}
}
