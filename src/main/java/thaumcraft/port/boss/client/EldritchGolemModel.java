package thaumcraft.port.boss.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import thaumcraft.port.boss.EldritchGolem;

/** BETA26 golem geometry, UVs, cloth, two heads and synchronized slam animation. */
public final class EldritchGolemModel extends HierarchicalModel<EldritchGolem> {
    private final ModelPart root;
    public EldritchGolemModel(ModelPart root){this.root=root;}
    @Override public ModelPart root(){return root;}
    public static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        var Cloak1 = root.addOrReplaceChild("Cloak1",CubeListBuilder.create().texOffs(0,47).addBox(-5.0f, 1.5f, 4.0f, 10, 12, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.1396263f, 0.0f, 0.0f));
        var Cloak3 = root.addOrReplaceChild("Cloak3",CubeListBuilder.create().texOffs(0,37).addBox(-5.0f, 17.5f, -0.8f, 10, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.4465716f, 0.0f, 0.0f));
        var Cloak2 = root.addOrReplaceChild("Cloak2",CubeListBuilder.create().texOffs(0,59).addBox(-5.0f, 13.5f, 1.7f, 10, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.3069452f, 0.0f, 0.0f));
        var CloakCL = root.addOrReplaceChild("CloakCL",CubeListBuilder.create().texOffs(0,43).addBox(3.0f, 0.5f, 2.0f, 2, 1, 3),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.1396263f, 0.0f, 0.0f));
        var CloakCR = root.addOrReplaceChild("CloakCR",CubeListBuilder.create().texOffs(0,43).addBox(-5.0f, 0.5f, 2.0f, 2, 1, 3),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.1396263f, 0.0f, 0.0f));
        var Head = root.addOrReplaceChild("Head",CubeListBuilder.create().texOffs(47,12).addBox(-3.5f, -6.0f, -2.5f, 7, 7, 5),PartPose.offsetAndRotation(0.0f, 4.5f, -3.8f,-0.1047198f, 0.0f, 0.0f));
        var Head2 = root.addOrReplaceChild("Head2",CubeListBuilder.create().texOffs(26,16).addBox(-2.0f, -2.0f, -2.0f, 4, 4, 4),PartPose.offsetAndRotation(0.0f, 0.0f, -5.0f,-0.1047198f, 0.0f, 0.0f));
        var CollarL = root.addOrReplaceChild("CollarL",CubeListBuilder.create().texOffs(75,50).addBox(3.5f, -0.5f, -7.0f, 1, 4, 10),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.837758f, 0.0f, 0.0f));
        var CollarR = root.addOrReplaceChild("CollarR",CubeListBuilder.create().texOffs(67,50).addBox(-4.5f, -0.5f, -7.0f, 1, 4, 10),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.837758f, 0.0f, 0.0f));
        var CollarB = root.addOrReplaceChild("CollarB",CubeListBuilder.create().texOffs(77,59).addBox(-3.5f, -0.5f, 2.0f, 7, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.837758f, 0.0f, 0.0f));
        var CollarF = root.addOrReplaceChild("CollarF",CubeListBuilder.create().texOffs(77,59).addBox(-3.5f, -0.5f, -7.0f, 7, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.837758f, 0.0f, 0.0f));
        var CollarBlack = root.addOrReplaceChild("CollarBlack",CubeListBuilder.create().texOffs(22,0).addBox(-3.5f, 0.0f, -6.0f, 7, 1, 8),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.837758f, 0.0f, 0.0f));
        var Frontcloth0 = root.addOrReplaceChild("Frontcloth0",CubeListBuilder.create().texOffs(114,52).addBox(-3.0f, 3.2f, -3.5f, 6, 10, 1),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.1745329f, 0.0f, 0.0f));
        var Frontcloth1 = root.addOrReplaceChild("Frontcloth1",CubeListBuilder.create().texOffs(114,39).addBox(-1.0f, 1.5f, -3.5f, 6, 6, 1),PartPose.offsetAndRotation(-2.0f, 12.0f, 0.0f,-0.1047198f, 0.0f, 0.0f));
        var Frontcloth2 = root.addOrReplaceChild("Frontcloth2",CubeListBuilder.create().texOffs(114,47).addBox(-1.0f, 8.5f, -1.5f, 6, 3, 1),PartPose.offsetAndRotation(-2.0f, 11.0f, 0.0f,-0.3316126f, 0.0f, 0.0f));
        var Torso = root.addOrReplaceChild("Torso",CubeListBuilder.create().texOffs(34,45).addBox(-5.0f, 2.5f, -3.0f, 10, 10, 6),PartPose.offsetAndRotation(0.0f, 0.0f, -2.5f,0.1745329f, 0.0f, 0.0f));
        var ArmR = root.addOrReplaceChild("ArmR",CubeListBuilder.create().texOffs(78,32).addBox(-3.5f, 1.5f, -2.0f, 4, 13, 5),PartPose.offsetAndRotation(-5.0f, 3.0f, -2.0f,0.0f, 0.0f, 0.1047198f));
        var ShoulderR1 = ArmR.addOrReplaceChild("ShoulderR1",CubeListBuilder.create().texOffs(0,23).addBox(-3.3f, 4.0f, -2.5f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 1.186824f));
        var ShoulderR = ArmR.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(0,0).addBox(-4.3f, -1.0f, -3.0f, 4, 5, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 1.186824f));
        var ShoulderR2 = ArmR.addOrReplaceChild("ShoulderR2",CubeListBuilder.create().texOffs(0,12).addBox(-2.3f, 4.0f, -3.0f, 2, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 1.186824f));
        var ShoulderR0 = ArmR.addOrReplaceChild("ShoulderR0",CubeListBuilder.create().texOffs(56,31).addBox(-4.5f, -1.5f, -2.5f, 5, 6, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        var ArmL = root.addOrReplaceChild("ArmL",CubeListBuilder.create().texOffs(78,32).mirror().addBox(-0.5f, 1.5f, -2.0f, 4, 13, 5),PartPose.offsetAndRotation(5.0f, 3.0f, -2.0f,0.0f, 0.0f, -0.1047198f));
        var ShoulderL1 = ArmL.addOrReplaceChild("ShoulderL1",CubeListBuilder.create().texOffs(0,23).mirror().addBox(2.3f, 4.0f, -2.5f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -1.186824f));
        var ShoulderL0 = ArmL.addOrReplaceChild("ShoulderL0",CubeListBuilder.create().texOffs(56,31).mirror().addBox(-0.5f, -1.5f, -2.5f, 5, 6, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        var ShoulderL = ArmL.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(0,0).mirror().addBox(0.3f, -1.0f, -3.0f, 4, 5, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -1.186824f));
        var ShoulderL2 = ArmL.addOrReplaceChild("ShoulderL2",CubeListBuilder.create().texOffs(0,12).mirror().addBox(0.3f, 4.0f, -3.0f, 2, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -1.186824f));
        var BackpanelR1 = root.addOrReplaceChild("BackpanelR1",CubeListBuilder.create().texOffs(96,7).addBox(0.0f, 2.5f, -2.5f, 2, 2, 5),PartPose.offsetAndRotation(-2.0f, 12.0f, 0.0f,0.0f, 0.0f, 0.1396263f));
        var WaistR1 = root.addOrReplaceChild("WaistR1",CubeListBuilder.create().texOffs(96,14).addBox(-3.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(-2.0f, 12.0f, 0.0f,0.0f, 0.0f, 0.1396263f));
        var WaistR2 = root.addOrReplaceChild("WaistR2",CubeListBuilder.create().texOffs(116,13).addBox(-3.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(-2.0f, 12.0f, 0.0f,0.0f, 0.0f, 0.1396263f));
        var WaistR3 = root.addOrReplaceChild("WaistR3",CubeListBuilder.create().texOffs(114,5).mirror().addBox(-2.0f, 2.5f, -2.5f, 2, 3, 5),PartPose.offsetAndRotation(-2.0f, 12.0f, 0.0f,0.0f, 0.0f, 0.1396263f));
        var LegR = root.addOrReplaceChild("LegR",CubeListBuilder.create().texOffs(79,19).addBox(-2.5f, 2.5f, -2.0f, 4, 9, 4),PartPose.offsetAndRotation(-2.0f, 12.5f, 0.0f,0.0f, 0.0f, 0.0f));
        var WaistL1 = root.addOrReplaceChild("WaistL1",CubeListBuilder.create().texOffs(96,14).addBox(-2.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(2.0f, 12.0f, 0.0f,0.0f, 0.0f, -0.1396263f));
        var WaistL2 = root.addOrReplaceChild("WaistL2",CubeListBuilder.create().texOffs(116,13).addBox(2.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(2.0f, 12.0f, 0.0f,0.0f, 0.0f, -0.1396263f));
        var WaistL3 = root.addOrReplaceChild("WaistL3",CubeListBuilder.create().texOffs(114,5).addBox(0.0f, 2.5f, -2.5f, 2, 3, 5),PartPose.offsetAndRotation(2.0f, 12.0f, 0.0f,0.0f, 0.0f, -0.1396263f));
        var BackpanelL1 = root.addOrReplaceChild("BackpanelL1",CubeListBuilder.create().texOffs(96,7).addBox(-2.0f, 2.5f, -2.5f, 2, 2, 5),PartPose.offsetAndRotation(2.0f, 12.0f, 0.0f,0.0f, 0.0f, -0.1396263f));
        var LegL = root.addOrReplaceChild("LegL",CubeListBuilder.create().texOffs(79,19).addBox(-1.5f, 2.5f, -2.0f, 4, 9, 4),PartPose.offsetAndRotation(2.0f, 12.5f, 0.0f,0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void prepareMobModel(EldritchGolem entity,float walk,float amount,float partial){
        float swing=entity.getAttackTimer()>0?-2+1.5f*triangle(entity.getAttackTimer()-partial,10):Float.NaN;
        root.getChild("ArmR").xRot=Float.isNaN(swing)?Mth.cos(walk*.4f+Mth.PI)*amount:swing;
        root.getChild("ArmL").xRot=Float.isNaN(swing)?Mth.cos(walk*.4f)*amount:swing;
    }
    @Override public void setupAnim(EldritchGolem entity,float walk,float amount,float age,float yaw,float pitch){
        var head=root.getChild("Head");var core=root.getChild("Head2");
        head.visible=!entity.isHeadless();core.visible=entity.isHeadless();
        head.yRot=entity.getSpawnTimer()>0?0:yaw/4*Mth.DEG_TO_RAD;
        head.xRot=entity.getSpawnTimer()>0?(entity.getSpawnTimer()/2)*Mth.DEG_TO_RAD:pitch/2*Mth.DEG_TO_RAD;
        core.yRot=yaw*Mth.DEG_TO_RAD;core.xRot=pitch*Mth.DEG_TO_RAD;
        root.getChild("LegR").xRot=Mth.cos(walk*.4662f)*1.4f*amount;
        root.getChild("LegL").xRot=Mth.cos(walk*.4662f+Mth.PI)*1.4f*amount;
        float bend=Math.min(Mth.cos(walk*.44f)*1.4f*amount,Mth.cos(walk*.44f+Mth.PI)*1.4f*amount);
        root.getChild("Frontcloth1").xRot=bend-.1047198f;root.getChild("Frontcloth2").xRot=bend-.3316126f;
        root.getChild("Cloak1").xRot=-bend/3+.1396263f;root.getChild("Cloak2").xRot=-bend/3+.3069452f;root.getChild("Cloak3").xRot=-bend/3+.4465716f;
    }
    private static float triangle(float time,float period){return (Math.abs(time%period-period*.5f)-period*.25f)/(period*.25f);}
}
