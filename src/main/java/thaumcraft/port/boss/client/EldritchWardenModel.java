package thaumcraft.port.boss.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import thaumcraft.port.boss.EldritchWarden;

/** The original guardian mesh, with the Warden's separate luminous hood eye. */
public final class EldritchWardenModel extends EntityModel<EldritchWarden> {
    private final ModelPart root,eye,hood,leftArm,rightArm,front1,front2,front3,cloak1,cloak2,cloak3,left2,left3,left4,right2,right3,right4;
    public EldritchWardenModel(ModelPart root){
        this.root=root;eye=root.getChild("HoodEye");eye.visible=false;hood=root.getChild("Hood1");leftArm=root.getChild("ArmL1");rightArm=root.getChild("ArmR1");
        front1=root.getChild("LegpanelC1");front2=front1.getChild("LegpanelC2");front3=front2.getChild("LegpanelC3");
        cloak1=root.getChild("Cloak1");cloak2=cloak1.getChild("Cloak2");cloak3=cloak2.getChild("Cloak3");
        left2=root.getChild("SidepanelL2");left3=left2.getChild("SidepanelL3");left4=left3.getChild("SidepanelL4");
        right2=root.getChild("SidepanelR2");right3=right2.getChild("SidepanelR3");right4=right3.getChild("SidepanelR4");
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){root.render(pose,out,light,overlay,color);}
    public void renderEye(PoseStack pose,VertexConsumer out,int light,int overlay){pose.pushPose();pose.scale(1.01f,1.01f,1.01f);eye.visible=true;eye.render(pose,out,light,overlay,-1);eye.visible=false;pose.popPose();}
    @Override public void setupAnim(EldritchWarden e,float walk,float amount,float age,float yaw,float pitch){
        hood.yRot=eye.yRot=yaw*Mth.DEG_TO_RAD;hood.xRot=eye.xRot=pitch*Mth.DEG_TO_RAD;
        leftArm.xRot=-1-e.armLiftL+Mth.sin((age+20)/10)*.08f;rightArm.xRot=-1-e.armLiftR+Mth.sin(age/10)*.08f;
        front1.xRot=-.15f+Mth.sin(age/8)*.12f;front2.xRot=Mth.sin((age-5)/8)*.13f;front3.xRot=Mth.sin((age-10)/8)*.14f;
        cloak1.xRot=.2f+Mth.sin(age/7)*.08f;cloak2.xRot=Mth.sin((age-5)/7)*.1f;cloak3.xRot=Mth.sin((age-10)/7)*.12f;
        left2.zRot=-.2f+Mth.sin((age+10)/8)*.12f;left3.zRot=Mth.sin((age+5)/8)*.13f;left4.zRot=Mth.sin(age/8)*.14f;
        right2.zRot=.2f+Mth.sin((age-5)/8)*.12f;right3.zRot=Mth.sin((age-10)/8)*.13f;right4.zRot=Mth.sin((age-15)/8)*.14f;
    }
}
