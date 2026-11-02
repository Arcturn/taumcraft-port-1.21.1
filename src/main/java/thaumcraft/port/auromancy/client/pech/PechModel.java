package thaumcraft.port.auromancy.client.pech;

import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.port.auromancy.pech.PechEntity;

/** BETA26 ModelPech: original 128x64 UVs, cheeks, hunched body and two backpack sections. */
public final class PechModel extends HumanoidModel<PechEntity> {
    private final ModelPart jowls,lowerPack,upperPack;
    public PechModel(ModelPart root) {super(root);jowls=root.getChild("jowls");lowerPack=root.getChild("lower_pack");upperPack=root.getChild("upper_pack");}
    public static LayerDefinition layer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(2,11).addBox(-3.5f,-5,-5,7,5,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(34,12).addBox(-3,0,0,6,10,6),PartPose.offsetAndRotation(0,9,-3,.3129957f,0,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(35,1).mirror().addBox(-2.9f,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(35,1).addBox(-.1f,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(52,2).mirror().addBox(-2,0,-1,2,6,2),PartPose.offset(-3,10,-1));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(52,2).addBox(0,0,-1,2,6,2),PartPose.offset(3,10,-1));
        root.addOrReplaceChild("jowls",CubeListBuilder.create().texOffs(1,21).addBox(-4,-1,-6,8,3,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("lower_pack",CubeListBuilder.create().texOffs(0,0).addBox(-5,0,0,10,5,5),PartPose.offsetAndRotation(0,10,3.5f,.3013602f,0,0));
        root.addOrReplaceChild("upper_pack",CubeListBuilder.create().texOffs(64,1).addBox(-7.5f,-14,0,15,14,11),PartPose.offsetAndRotation(0,10,3,.4537856f,0,0));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override protected Iterable<ModelPart> bodyParts() {return List.of(body,rightArm,leftArm,rightLeg,leftLeg,jowls,lowerPack,upperPack);}
    @Override public void setupAnim(PechEntity entity,float walk,float amount,float age,float yaw,float pitch) {
        head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        jowls.yRot=head.yRot;jowls.xRot=head.xRot+.2617994f+Mth.cos(walk*.6662f)*amount*.25f+.34906587f*Math.abs(Mth.sin(entity.mumble/8));
        rightArm.xRot=Mth.cos(walk*.6662f+Mth.PI)*amount;leftArm.xRot=Mth.cos(walk*.6662f)*amount;
        rightArm.zRot=leftArm.zRot=0;rightLeg.xRot=Mth.cos(walk*.6662f)*1.4f*amount;leftLeg.xRot=Mth.cos(walk*.6662f+Mth.PI)*1.4f*amount;
        rightLeg.yRot=leftLeg.yRot=0;lowerPack.yRot=lowerPack.zRot=Mth.cos(walk*.6662f)*amount*.25f;
        if(riding){rightArm.xRot-=.62831855f;leftArm.xRot-=.62831855f;rightLeg.xRot=leftLeg.xRot=-1.2566371f;rightLeg.yRot=.31415927f;leftLeg.yRot=-.31415927f;}
        rightArm.yRot=leftArm.yRot=0;
        if(attackTime>-9990){
            rightArm.yRot+=body.yRot;leftArm.yRot+=body.yRot;leftArm.xRot+=body.yRot;
            float swing=1-attackTime;swing*=swing;swing*=swing;swing=1-swing;
            float first=Mth.sin(swing*Mth.PI),second=Mth.sin(attackTime*Mth.PI)*-(head.xRot-.7f)*.75f;
            rightArm.xRot-=first*1.2f+second;rightArm.yRot+=body.yRot*2;rightArm.zRot=Mth.sin(attackTime*Mth.PI)*-.4f;
        }
        if(entity.isCrouching()){rightArm.xRot+=.4f;leftArm.xRot+=.4f;}
        rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;
        rightArm.xRot+=Mth.sin(age*.067f)*.05f;leftArm.xRot-=Mth.sin(age*.067f)*.05f;
    }
}
